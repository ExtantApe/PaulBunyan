package com.extantape.paulbunyan.regrow;

import com.extantape.paulbunyan.BukkitView;
import com.extantape.paulbunyan.PlacedBlocks;
import com.extantape.paulbunyan.Settings;
import com.extantape.paulbunyan.fall.Sprouts;
import com.extantape.paulbunyan.hook.BlockLog;
import com.extantape.paulbunyan.hook.Protection;
import com.extantape.paulbunyan.tree.Cell;
import com.extantape.paulbunyan.tree.Grower;
import com.extantape.paulbunyan.tree.Growth;
import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Scan;
import com.extantape.paulbunyan.tree.Species;
import com.extantape.paulbunyan.tree.TreeScanner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;

/**
 * Stumps that grow back into wild trees, and saplings that sprout along a cleared fall. It keeps its list in
 * {@code regrowth.txt}, and checks it every few seconds on the main thread, only in chunks that are loaded.
 * It never replaces anything but air and small plants, and it logs what it places as the plugin, not a player.
 */
public final class Regrowth {
    /** Ticks in an in-game day. Tests shorten it with -Dpaulbunyan.test.day-ticks. */
    static final long DAY_TICKS = Long.getLong("paulbunyan.test.day-ticks", 24000L);
    /** How often the list is checked, in ticks. */
    public static final long SWEEP_TICKS = 100;
    /** A fall nobody clears is forgotten after this many days. */
    static final int FALL_WAIT_DAYS = 60;
    /** At most this many stumps grow in one check, so a busy list never lags the server. */
    static final int GROWTH_PER_SWEEP = 2;

    private final Path file;
    private final Logger log;
    private final PlacedBlocks placed;
    private final Supplier<Settings> settings;
    private Protection protection;
    private BlockLog blockLog;
    private Store store;
    private boolean dirty;

    public Regrowth(Path dataFolder, Logger log, PlacedBlocks placed, Supplier<Settings> settings, Protection protection, BlockLog blockLog) {
        this.file = dataFolder.resolve("regrowth.txt");
        this.log = log;
        this.placed = placed;
        this.settings = settings;
        this.protection = protection;
        this.blockLog = blockLog;
        try {
            store = Store.load(file);
            if (store.skipped > 0) log.warning(store.skipped + " line(s) of regrowth.txt couldn't be read and were dropped.");
        } catch (IOException | RuntimeException e) {
            log.log(Level.WARNING, "Couldn't read regrowth.txt, so stumps and saplings already listed are forgotten", e);
            store = new Store();
        }
    }

    public void reload(Protection protection, BlockLog blockLog) {
        this.protection = protection;
        this.blockLog = blockLog;
    }

    private Settings.Regrowth rules() {
        return settings.get().regrowth();
    }

    // ---------- what the felling tells it ----------

    /** A high cut left these logs standing. {@code tops} is the highest standing log of each trunk column. */
    public void stumpLeft(World world, Species species, List<Pos> tops) {
        if (tops.isEmpty() || !rules().stumpsIn(world.getName())) return;
        Random r = new Random();
        long seed = r.nextLong();
        double spread = 0.75 + 0.75 * r.nextDouble();
        long due = world.getGameTime() + Math.round(rules().stumpDays() * DAY_TICKS * spread);
        store.stumps.add(new Store.Stump(world.getName(), species.name(), seed, due, false, List.copyOf(tops)));
        dirty = true;
    }

    /** A fall came to rest. {@code logs} are the spots where it landed as blocks. */
    public void fallLanded(World world, Species species, String sapling, List<Pos> logs) {
        if (logs.isEmpty() || sapling == null || !rules().saplingsIn(world.getName())) return;
        store.falls.add(new Store.Fall(world.getName(), species.name(), new Random().nextLong(), world.getGameTime(), sapling, List.copyOf(logs)));
        dirty = true;
    }

    /** A sapling was planted where a tree stood. It stays until it grows or is taken, so Rewild can keep it. */
    public void planted(World world, Pos at, String material) {
        store.saplings.add(new Store.Sapling(world.getName(), at, material, 0));
        dirty = true;
    }

    // ---------- the check ----------

    public void sweep() {
        try {
            sweepStumps();
            sweepFalls();
            sweepSaplings();
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Regrowth check failed; it will try again", e);
        }
        flush();
    }

    /** Writes the list if it changed. Called every check, and when the plugin stops. */
    public void flush() {
        if (!dirty) return;
        try {
            store.save(file);
            dirty = false;
        } catch (IOException e) {
            log.log(Level.WARNING, "Couldn't save regrowth.txt; will try again", e);
        }
    }

    private void sweepStumps() {
        int grew = 0;
        for (Store.Stump s : List.copyOf(store.stumps)) {
            World w = Bukkit.getWorld(s.world());
            if (w == null || !rules().stumpsIn(w.getName())) continue;
            if (w.getGameTime() < s.due()) continue;
            if (grew >= GROWTH_PER_SWEEP) break;
            if (tryGrow(w, s)) grew++;
        }
    }

    /** True if a tree went in. */
    private boolean tryGrow(World w, Store.Stump s) {
        Species species;
        try {
            species = Species.valueOf(s.species());
        } catch (IllegalArgumentException e) {
            store.stumps.remove(s);
            dirty = true;
            return false;
        }
        Grower.Plan plan = Grower.plan(species, s.tops(), s.seed());
        BukkitView view = new BukkitView(w, placed, protection, null);
        Growth.Result r = Growth.check(species, s.tops(), plan, new Growth.Space() {
            @Override public Cell cell(Pos p) { return view.cell(p); }
            @Override public boolean occupied(Pos p) { return view.occupied(p); }
            @Override public boolean claimed(Pos p) { return protection.claimed(new Location(w, p.x(), p.y(), p.z())); }
            @Override public int minY() { return w.getMinHeight(); }
            @Override public int maxY() { return w.getMaxHeight(); }
        });
        switch (r.verdict()) {
            case OK -> { }
            case NOT_LOADED -> { return false; } // look again next check
            case STUMP_GONE -> {
                store.stumps.remove(s);
                dirty = true;
                return false;
            }
            default -> {
                // Something's in the way: leave everything alone and look again in a quarter of a day.
                replace(s, s.due(w.getGameTime() + Math.max(SWEEP_TICKS, DAY_TICKS / 4)));
                return false;
            }
        }
        // The list is written before the first block goes in, so a crash mid-way finishes the same tree next time.
        Store.Stump growing = s.startGrowing();
        replace(s, growing);
        Pos lo = plan.min(), hi = plan.max();
        Pos low = lowestLog(w, s.tops().getFirst());
        int x0 = lo.x(), z0 = lo.z(), x1 = hi.x(), z1 = hi.z();
        for (Pos top : s.tops()) {
            x0 = Math.min(x0, top.x()); x1 = Math.max(x1, top.x());
            z0 = Math.min(z0, top.z()); z1 = Math.max(z1, top.z());
        }
        Store.Grown box = new Store.Grown(w.getName(), new Pos(x0, Math.min(lo.y(), low.y()), z0), new Pos(x1, hi.y(), z1));
        if (!store.grown.contains(box)) store.grown.add(box);
        dirty = true;
        flush();
        for (Grower.Piece p : r.toPlace()) place(w, p);
        store.stumps.remove(growing);
        dirty = true;
        Pos top = s.tops().getFirst();
        w.spawnParticle(Particle.HAPPY_VILLAGER, top.x() + 0.5, top.y() + 1.5, top.z() + 0.5, 12, 0.5, 0.8, 0.5, 0);
        return true;
    }

    private void replace(Store.Stump old, Store.Stump now) {
        int i = store.stumps.indexOf(old);
        if (i >= 0) store.stumps.set(i, now);
        dirty = true;
    }

    private static Pos lowestLog(World w, Pos top) {
        Pos p = top;
        while (p.y() > w.getMinHeight() && Tag.LOGS.isTagged(w.getBlockAt(p.x(), p.y() - 1, p.z()).getType())) p = p.below();
        return p;
    }

    private void place(World w, Grower.Piece p) {
        Block b = w.getBlockAt(p.at().x(), p.at().y(), p.at().z());
        Material m = Material.matchMaterial(p.material());
        if (m == null) return;
        BlockData data = m.createBlockData();
        if (data instanceof Orientable o && p.isLog()) o.setAxis(org.bukkit.Axis.valueOf(String.valueOf(p.axis()).toUpperCase()));
        if (data instanceof org.bukkit.block.data.type.Leaves l) l.setPersistent(false);
        if (!b.getType().isAir()) blockLog.removedByPlugin(b.getLocation(), b.getBlockData());
        b.setBlockData(data, !p.isLog()); // leaves update their distance to the trunk; logs don't need to disturb anything
        blockLog.placedByPlugin(b.getLocation(), data);
    }

    private void sweepFalls() {
        for (Store.Fall f : List.copyOf(store.falls)) {
            World w = Bukkit.getWorld(f.world());
            if (w == null) continue;
            if (!rules().saplingsIn(w.getName()) || w.getGameTime() - f.born() > FALL_WAIT_DAYS * DAY_TICKS) {
                store.falls.remove(f);
                dirty = true;
                continue;
            }
            boolean loaded = true, cleared = true;
            for (Pos p : f.logs()) {
                if (!w.isChunkLoaded(p.x() >> 4, p.z() >> 4)) { loaded = false; break; }
                if (Tag.LOGS.isTagged(w.getBlockAt(p.x(), p.y(), p.z()).getType())) cleared = false;
            }
            if (!loaded || !cleared) continue;
            store.falls.remove(f);
            dirty = true;
            sprout(w, f);
        }
    }

    /** Saplings where the logs lay: on grass or dirt, in the open, outside claims, a few and spread out. */
    private void sprout(World w, Store.Fall f) {
        Species species;
        Material sapling = Material.matchMaterial(f.sapling());
        try {
            species = Species.valueOf(f.species());
        } catch (IllegalArgumentException e) {
            return;
        }
        if (sapling == null) return;
        Set<Pos> columns = new LinkedHashSet<>();
        for (Pos p : f.logs()) {
            columns.add(p);
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) columns.add(p.add(d[0], 0, d[1]));
        }
        Set<Pos> valid = new LinkedHashSet<>();
        BlockData data = sapling.createBlockData();
        BukkitView view = new BukkitView(w, placed, protection, null);
        for (Pos c : columns) {
            for (int dy : new int[] {0, -1, 1}) {
                Pos p = c.add(0, dy, 0);
                if (!w.isChunkLoaded(p.x() >> 4, p.z() >> 4)) continue;
                Block b = w.getBlockAt(p.x(), p.y(), p.z());
                if (!b.getType().isAir()) continue;
                if (!isGround(b.getRelative(0, -1, 0).getType())) continue;
                if (!fits(b, data, species)) continue;
                if (protection.claimed(b.getLocation())) continue;
                if (view.occupied(p)) continue;
                valid.add(p);
                break;
            }
        }
        List<Pos> chosen = Sprouts.choose(new ArrayList<>(valid), valid, rules().saplingMax(), f.seed(), species.needsFour());
        for (Pos p : chosen) {
            Block b = w.getBlockAt(p.x(), p.y(), p.z());
            b.setBlockData(data, true);
            blockLog.placedByPlugin(b.getLocation(), data);
            store.saplings.add(new Store.Sapling(w.getName(), p, sapling.name(), w.getGameTime() + (long) rules().saplingDays() * DAY_TICKS));
        }
        dirty = true;
    }

    private static boolean isGround(Material m) {
        return m == Material.GRASS_BLOCK || Tag.DIRT.isTagged(m) || m == Material.CRIMSON_NYLIUM || m == Material.WARPED_NYLIUM;
    }

    private static boolean fits(Block b, BlockData sapling, Species species) {
        if (!sapling.isSupported(b)) return false;
        return species.nylium() == null || b.getRelative(0, -1, 0).getType().name().equals(species.nylium());
    }

    private void sweepSaplings() {
        for (Store.Sapling s : List.copyOf(store.saplings)) {
            World w = Bukkit.getWorld(s.world());
            if (w == null || !w.isChunkLoaded(s.at().x() >> 4, s.at().z() >> 4)) continue;
            Block b = w.getBlockAt(s.at().x(), s.at().y(), s.at().z());
            Material planted = Material.matchMaterial(s.material());
            if (b.getType() == planted) {
                if (s.expires() > 0 && w.getGameTime() >= s.expires()) {
                    blockLog.removedByPlugin(b.getLocation(), b.getBlockData());
                    b.setType(Material.AIR, false);
                    store.saplings.remove(s);
                    dirty = true;
                }
                continue;
            }
            store.saplings.remove(s);
            dirty = true;
            if (Tag.LOGS.isTagged(b.getType())) keepGrown(w, b);
        }
    }

    /** A sapling of ours grew: the tree is kept, so Rewild leaves it. */
    private void keepGrown(World w, Block base) {
        Pos at = new Pos(base.getX(), base.getY(), base.getZ());
        if (store.isGrown(w.getName(), at)) return;
        Scan scan = new TreeScanner(new BukkitView(w, placed, protection, null), settings.get().limits()).scan(at);
        Pos min, max;
        if (scan.fells()) {
            int x0 = at.x(), y0 = at.y(), z0 = at.z(), x1 = x0, y1 = y0, z1 = z0;
            List<Pos> all = new ArrayList<>(scan.tree().logs());
            all.addAll(scan.tree().leaves());
            for (Pos p : all) {
                x0 = Math.min(x0, p.x()); x1 = Math.max(x1, p.x());
                y0 = Math.min(y0, p.y()); y1 = Math.max(y1, p.y());
                z0 = Math.min(z0, p.z()); z1 = Math.max(z1, p.z());
            }
            min = new Pos(x0, y0, z0);
            max = new Pos(x1, y1, z1);
        } else {
            min = at.add(-3, 0, -3);
            max = at.add(3, 14, 3);
        }
        store.grown.add(new Store.Grown(w.getName(), min, max));
    }

    // ---------- Rewild ----------

    /** Boxes Rewild should keep in a world: saplings we planted and trees that grew from stumps or from those saplings. */
    public List<RewildKeeps.Box> keeps(World w) {
        List<RewildKeeps.Box> out = new ArrayList<>();
        if (!rules().rewildLink()) return out;
        String name = w.getName();
        for (Store.Sapling s : store.saplings)
            if (s.world().equals(name))
                out.add(new RewildKeeps.Box(s.at().x(), s.at().y(), s.at().z(), s.at().x(), s.at().y(), s.at().z(), "a sapling PaulBunyan planted"));
        for (Store.Grown g : store.grown)
            if (g.world().equals(name))
                out.add(new RewildKeeps.Box(g.min().x(), g.min().y(), g.min().z(), g.max().x(), g.max().y(), g.max().z(), "a tree that grew back from a stump"));
        return out;
    }

    public Store store() {
        return store;
    }
}

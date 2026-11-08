package com.extantape.paulbunyan;

import com.extantape.paulbunyan.fall.Animation;
import com.extantape.paulbunyan.fall.Axis3;
import com.extantape.paulbunyan.fall.Direction;
import com.extantape.paulbunyan.fall.FallPlan;
import com.extantape.paulbunyan.fall.Journal;
import com.extantape.paulbunyan.fall.Landing;
import com.extantape.paulbunyan.fall.Replanter;
import com.extantape.paulbunyan.hook.BlockLog;
import com.extantape.paulbunyan.hook.Protection;
import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Scan;
import com.extantape.paulbunyan.tree.Species;
import com.extantape.paulbunyan.tree.Tree;
import com.extantape.paulbunyan.tree.TreeScanner;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Axis;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Runs falls: checks a chopped tree, removes it, animates it and lands it, and puts things right after
 * a crash. Everything happens on the main thread.
 */
public final class Feller {
    /** How long a landed fall's journal is kept: longer than the default five-minute autosave. */
    static final long KEEP_LANDED_MS = 10 * 60 * 1000L;
    /** How long a player who was on or under a falling tree is spared fall damage. */
    static final long FALL_GRACE_MS = 10_000L;

    private final PaulBunyan plugin;
    private Settings settings;
    private final PlacedBlocks placed;
    private Protection protection;
    private BlockLog blockLog;
    private final Journal journal;

    private final Set<String> busy = new HashSet<>();
    private final Map<UUID, Long> fallGrace = new HashMap<>();
    private final Map<UUID, Active> active = new HashMap<>();
    /** Landed fall journals still kept, and where their landed blocks are. */
    private final Map<UUID, Journal.Entry> landed = new HashMap<>();
    private final Map<String, UUID> landedAt = new HashMap<>();

    /** One fall in progress. */
    private final class Active {
        final Journal.Entry entry;
        final World world;
        final FallPlan plan;
        final Animation animation;
        final List<LeafDrop> leafDrops;
        BukkitTask task;
        int tick;

        Active(Journal.Entry entry, World world, FallPlan plan, Animation animation, List<LeafDrop> leafDrops) {
            this.entry = entry;
            this.world = world;
            this.plan = plan;
            this.animation = animation;
            this.leafDrops = leafDrops;
        }
    }

    private record LeafDrop(Pos target, Collection<ItemStack> items) {}

    public Feller(PaulBunyan plugin, Settings settings, PlacedBlocks placed, Protection protection, BlockLog blockLog,
                  Journal journal) {
        this.plugin = plugin;
        this.settings = settings;
        this.placed = placed;
        this.protection = protection;
        this.blockLog = blockLog;
        this.journal = journal;
    }

    public void reload(Settings settings, Protection protection, BlockLog blockLog) {
        this.settings = settings;
        this.protection = protection;
        this.blockLog = blockLog;
    }

    public Settings settings() {
        return settings;
    }

    public PlacedBlocks placed() {
        return placed;
    }

    static String key(World w, Pos p) {
        return w.getUID() + ":" + p;
    }

    static Pos pos(Block b) {
        return new Pos(b.getX(), b.getY(), b.getZ());
    }

    public Scan scan(Block b, UUID player) {
        return new TreeScanner(new BukkitView(b.getWorld(), placed, protection, player), settings.limits()).scan(pos(b));
    }

    public boolean isBusy(Block b) {
        return busy.contains(key(b.getWorld(), pos(b)));
    }

    /** Would this axe survive one point of wear per log, at worst? */
    static boolean axeHolds(ItemStack item, int logs) {
        if (item.hasData(DataComponentTypes.UNBREAKABLE)) return true;
        Integer max = item.getData(DataComponentTypes.MAX_DAMAGE);
        if (max == null) return true;
        int damage = item.getDataOrDefault(DataComponentTypes.DAMAGE, 0);
        return max - damage > logs;
    }

    public enum Outcome { FELLS, VANILLA, AXE_TOO_WORN, PROTECTED, TOUCHES_BUILD, TOO_BIG }

    /**
     * Called while a player's break of {@code chopped} is being allowed. Decides now, while the tree is
     * still whole, and starts the fall next tick, once the chopped log is gone.
     */
    public Outcome chop(Player player, Block chopped, ItemStack axe, boolean wear) {
        Scan scan = scan(chopped, player.getUniqueId());
        if (!scan.fells()) {
            return switch (scan.skip()) {
                case TOUCHES_BUILD -> Outcome.TOUCHES_BUILD;
                case TOO_BIG -> Outcome.TOO_BIG;
                default -> Outcome.VANILLA;
            };
        }
        Tree tree = scan.tree();
        World world = chopped.getWorld();
        if (wear && !axeHolds(axe, tree.logs().size())) return Outcome.AXE_TOO_WORN;
        for (Pos p : tree.logs()) if (busy.contains(key(world, p))) return Outcome.VANILLA;
        for (Pos p : tree.logs()) if (!protection.mayBreak(player, block(world, p))) return Outcome.PROTECTED;
        for (Pos p : tree.leaves()) if (!protection.mayBreak(player, block(world, p))) return Outcome.PROTECTED;

        Pos choppedPos = pos(chopped);
        Map<Pos, BlockData> before = new HashMap<>();
        for (Pos p : tree.logs()) before.put(p, block(world, p).getBlockData());
        for (Pos p : tree.logs()) busy.add(key(world, p));
        Direction dir = Direction.facing(player.getLocation().getYaw());
        int extra = tree.logs().size() - 1;
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean started = false;
            try {
                started = start(player, world, tree, choppedPos, before, dir, wear ? extra : 0);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "A fall failed part-way through starting", e);
            } finally {
                if (!started) for (Pos p : tree.logs()) busy.remove(key(world, p));
            }
        });
        return Outcome.FELLS;
    }

    private static Block block(World w, Pos p) {
        return w.getBlockAt(p.x(), p.y(), p.z());
    }

    private boolean start(Player player, World world, Tree tree, Pos chopped, Map<Pos, BlockData> before, Direction dir, int wear) {
        busy.remove(key(world, chopped));
        // The break went through, and nothing else changed the tree since.
        if (!block(world, chopped).getType().isAir()) return false;
        for (Pos p : tree.logs()) {
            if (p.equals(chopped)) continue;
            if (!block(world, p).getBlockData().equals(before.get(p))) return false;
        }
        if (wear > 0) {
            if (!player.isOnline()) return false;
            ItemStack axe = player.getInventory().getItemInMainHand();
            if (!org.bukkit.Tag.ITEMS_AXES.isTagged(axe.getType()) || !axeHolds(axe, wear)) return false;
            player.damageItemStack(EquipmentSlot.HAND, wear);
        }

        FallPlan plan = new FallPlan(dir, tree.minY(), tree.base());
        List<Journal.Log> logs = new ArrayList<>();
        for (Pos p : tree.logs()) {
            if (p.equals(chopped)) continue;
            BlockData data = before.get(p);
            logs.add(new Journal.Log(p, data.getAsString(), plan.tipped(p), tipped(data, dir).getAsString()));
        }
        String sapling = sapling(world, tree);
        UUID id = UUID.randomUUID();
        Journal.Entry entry = new Journal.Entry(id, world.getName(), player.getUniqueId(), player.getName(),
            settings.landAsItems(), tree.minY(), settings.replant() ? sapling : null, tree.base(), logs, false, 0, List.of());
        try {
            journal.write(entry);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Couldn't write the fall journal, so the tree won't fall", e);
            return false;
        }

        boolean animate = settings.fallTicks() > 0;
        boolean animateLeaves = animate && logs.size() + tree.leaves().size() <= settings.maxAnimated();
        Animation animation = new Animation(plan, settings.fallTicks());
        List<LeafDrop> leafDrops = new ArrayList<>();
        for (Pos p : tree.leaves()) {
            Block b = block(world, p);
            BlockData data = b.getBlockData();
            leafDrops.add(new LeafDrop(plan.tipped(p), b.getDrops()));
            if (animateLeaves) animation.add(world, p, data);
            blockLog.removed(player.getName(), b.getLocation(), data);
            b.setType(Material.AIR, true);
        }
        for (Journal.Log l : logs) {
            Block b = block(world, l.origin());
            if (animate && logs.size() <= settings.maxAnimated()) animation.add(world, l.origin(), b.getBlockData());
            blockLog.removed(player.getName(), b.getLocation(), b.getBlockData());
            b.setType(Material.AIR, true);
        }
        spareFallDamage(world, tree);
        Pos stump = tree.base().getFirst();
        world.playSound(Sound.sound(Key.key("block.wood.break"), Sound.Source.BLOCK, 1.2f, 0.55f), stump.x() + 0.5, stump.y() + 1, stump.z() + 0.5);
        world.playSound(Sound.sound(Key.key("block.grass.break"), Sound.Source.BLOCK, 0.8f, 0.7f), stump.x() + 0.5, stump.y() + 3, stump.z() + 0.5);

        Active a = new Active(entry, world, plan, animation, leafDrops);
        active.put(id, a);
        if (!animate) {
            land(a);
            return true;
        }
        a.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            a.tick++;
            if (a.tick > animation.ticks()) land(a);
            else animation.tick(a.tick);
        }, 1L, 1L);
        return true;
    }

    /** Azalea trees grow oak logs, so they're told apart by their leaves. */
    private static String sapling(World world, Tree tree) {
        if (tree.species() == Species.OAK) {
            int azalea = 0;
            for (Pos p : tree.leaves()) {
                Material m = block(world, p).getType();
                if (m == Material.AZALEA_LEAVES || m == Material.FLOWERING_AZALEA_LEAVES) azalea++;
            }
            if (azalea * 2 > tree.leaves().size()) return "AZALEA";
        }
        return tree.species().sapling;
    }

    static BlockData tipped(BlockData data, Direction dir) {
        BlockData out = data.clone();
        if (out instanceof Orientable o) {
            Axis3 a = Axis3.valueOf(o.getAxis().name()).tipped(dir);
            Axis to = Axis.valueOf(a.name());
            if (o.getAxes().contains(to)) o.setAxis(to);
        }
        return out;
    }

    private void spareFallDamage(World world, Tree tree) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Pos p : tree.logs()) {
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z());
            maxY = Math.max(maxY, p.y());
        }
        for (Pos p : tree.leaves()) {
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z());
            maxY = Math.max(maxY, p.y());
        }
        long until = System.currentTimeMillis() + FALL_GRACE_MS;
        for (Player p : world.getPlayers()) {
            Location l = p.getLocation();
            if (l.getX() >= minX - 1 && l.getX() <= maxX + 2 && l.getZ() >= minZ - 1 && l.getZ() <= maxZ + 2
                && l.getY() >= tree.minY() - 1 && l.getY() <= maxY + 4) fallGrace.put(p.getUniqueId(), until);
        }
    }

    public boolean sparesFallDamage(UUID player) {
        Long until = fallGrace.get(player);
        if (until == null) return false;
        if (until < System.currentTimeMillis()) {
            fallGrace.remove(player);
            return false;
        }
        return true;
    }

    private void land(Active a) {
        if (a.task != null) a.task.cancel();
        a.animation.remove();
        active.remove(a.entry.id());
        try {
            Journal.Entry done = settle(a.world, a.entry, a.plan.minY(), a.leafDrops, null);
            landed.put(done.id(), done);
            for (Journal.Landed b : done.blocks()) landedAt.put(key(a.world, b.at()), done.id());
        } finally {
            for (Journal.Log l : a.entry.logs()) busy.remove(key(a.world, l.origin()));
        }
    }

    /**
     * Replants, lands every log, drops the leaves' items and rewrites the journal as landed.
     * {@code onlyMissing}, during a replay, lists the logs that still need landing.
     */
    private Journal.Entry settle(World world, Journal.Entry e, int minY, List<LeafDrop> leafDrops, Set<Pos> onlyMissing) {
        Player player = Bukkit.getPlayer(e.player());
        String who = e.playerName();
        if (e.sapling() != null) replant(world, e, who);

        BukkitView view = new BukkitView(world, placed, protection, e.player());
        List<Journal.Log> order = new ArrayList<>(e.logs());
        order.sort(Comparator.<Journal.Log>comparingInt(l -> l.target().y())
            .thenComparingInt(l -> Math.abs(l.target().x() - l.origin().x()) + Math.abs(l.target().z() - l.origin().z())));
        List<Journal.Landed> blocks = new ArrayList<>(e.blocks());
        List<ItemStack> toPlayer = new ArrayList<>();
        Location stump = stump(world, e);
        List<Pos> landedAt = new ArrayList<>();
        List<BlockData> landedData = new ArrayList<>();
        for (Journal.Log l : order) {
            if (onlyMissing != null && !onlyMissing.contains(l.origin())) continue;
            world.getChunkAt(l.target().x() >> 4, l.target().z() >> 4); // loads it if it was unloaded mid-fall
            BlockData data = Bukkit.createBlockData(l.tippedData());
            Landing.Result r = Landing.find(view, l.target(), minY, e.asItems());
            ItemStack item = new ItemStack(data.getMaterial());
            switch (r.outcome()) {
                case BLOCK -> {
                    landedAt.add(r.at());
                    landedData.add(data);
                    Block b = view.block(r.at());
                    b.setBlockData(data, true);
                    blockLog.placed(who, b.getLocation(), data);
                    blocks.add(new Journal.Landed(l.origin(), r.at(), data.getAsString()));
                }
                case ITEM -> {
                    landedAt.add(r.at());
                    landedData.add(data);
                    world.dropItem(center(world, r.at()), item, i -> i.setVelocity(new org.bukkit.util.Vector()));
                }
                case TO_PLAYER -> toPlayer.add(item);
            }
        }
        feedback(world, landedAt, landedData);
        if (leafDrops != null) {
            for (LeafDrop d : leafDrops) {
                if (d.items().isEmpty()) continue;
                Landing.Result r = Landing.find(view, d.target(), minY, true);
                Location at = r.outcome() == Landing.Outcome.ITEM ? center(world, r.at()) : stump;
                for (ItemStack i : d.items()) world.dropItem(at, i);
            }
        }
        if (!toPlayer.isEmpty()) {
            if (player != null && player.isOnline()) {
                for (ItemStack left : player.getInventory().addItem(toPlayer.toArray(ItemStack[]::new)).values())
                    player.getWorld().dropItem(player.getLocation(), left);
            } else {
                for (ItemStack i : toPlayer) world.dropItem(stump, i);
            }
        }
        Journal.Entry done = e.withLanded(System.currentTimeMillis(), blocks);
        try {
            journal.write(done);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Couldn't update the fall journal", ex);
        }
        return done;
    }

    /** A thud where the trunk lands, and wood dust on each landed log (the first 64). */
    private static void feedback(World world, List<Pos> at, List<BlockData> data) {
        if (at.isEmpty()) return;
        double x = 0, y = 0, z = 0;
        for (Pos p : at) { x += p.x(); y += p.y(); z += p.z(); }
        int n = at.size();
        world.playSound(Sound.sound(Key.key("block.wood.place"), Sound.Source.BLOCK, 1.5f, 0.5f), x / n + 0.5, y / n + 0.5, z / n + 0.5);
        world.playSound(Sound.sound(Key.key("entity.generic.big_fall"), Sound.Source.BLOCK, 1.0f, 0.6f), x / n + 0.5, y / n + 0.5, z / n + 0.5);
        for (int i = 0; i < Math.min(64, n); i++) {
            Pos p = at.get(i);
            world.spawnParticle(Particle.BLOCK, p.x() + 0.5, p.y() + 0.2, p.z() + 0.5, 10, 0.4, 0.15, 0.4, 0, data.get(i));
        }
    }

    private static Location stump(World world, Journal.Entry e) {
        Pos p = e.replant().isEmpty() ? e.logs().getFirst().origin() : e.replant().getFirst();
        return center(world, p);
    }

    private static Location center(World w, Pos p) {
        return new Location(w, p.x() + 0.5, p.y() + 0.1, p.z() + 0.5);
    }

    private void replant(World world, Journal.Entry e, String who) {
        Species species = Species.ofLog(Bukkit.createBlockData(e.logs().isEmpty() ? "minecraft:air" : e.logs().getFirst().data()).getMaterial().name());
        Material sapling = Material.matchMaterial(e.sapling());
        if (sapling == null || species == null) return;
        BlockData data = sapling.createBlockData();
        List<List<Pos>> options = Replanter.options(species, e.replant());
        if (species == Species.MANGROVE) options = mangroveSpots(world, e.replant(), data);
        for (List<Pos> option : options) {
            List<BlockData> fits = new ArrayList<>();
            for (Pos p : option) {
                BlockData d = fit(block(world, p), data, species);
                if (d == null) break;
                fits.add(d);
            }
            if (fits.size() != option.size()) continue;
            for (int i = 0; i < option.size(); i++) {
                Block b = block(world, option.get(i));
                b.setBlockData(fits.get(i), true);
                blockLog.placed(who, b.getLocation(), fits.get(i));
            }
            return;
        }
    }

    /** The sapling as it would go in here, or null if the spot is taken or the sapling can't grow there. */
    private static BlockData fit(Block b, BlockData sapling, Species species) {
        BlockData d = sapling.clone();
        Material here = b.getType();
        if (here == Material.WATER && d instanceof Waterlogged w && b.getBlockData() instanceof org.bukkit.block.data.Levelled lv && lv.getLevel() == 0) {
            w.setWaterlogged(true);
        } else if (!here.isAir()) {
            return null;
        }
        if (!d.isSupported(b)) return null;
        if (species.nylium() != null && !b.getRelative(0, -1, 0).getType().name().equals(species.nylium())) return null;
        return d;
    }

    /**
     * A mangrove's trunk stands on its roots, where a propagule can't go. The nearest spot to the trunk
     * where one can, within ten blocks across and twelve down (a tall mangrove stands high on its roots), is used instead.
     */
    private static List<List<Pos>> mangroveSpots(World world, List<Pos> base, BlockData propagule) {
        List<Pos> spots = new ArrayList<>();
        Pos c = base.getFirst();
        for (int r = 0; r <= 10; r++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy = 1; dy >= -12; dy--) spots.add(c.add(dx, dy, dz));
                }
        List<List<Pos>> out = new ArrayList<>();
        for (Pos p : spots) out.add(List.of(p));
        return out;
    }

    /** A block was broken or changed at {@code b}: if a fall landed there, forget it. */
    public void landedBlockGone(Block b) {
        UUID id = landedAt.remove(key(b.getWorld(), pos(b)));
        if (id == null) return;
        Journal.Entry e = landed.get(id);
        if (e == null) return;
        Pos p = pos(b);
        List<Journal.Landed> blocks = new ArrayList<>(e.blocks());
        if (!blocks.removeIf(l -> l.at().equals(p))) return;
        Journal.Entry updated = e.withLanded(e.landedAt(), blocks);
        landed.put(id, updated);
        try {
            journal.write(updated);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Couldn't update the fall journal", ex);
        }
    }

    /** Deletes journals of falls that landed long enough ago for the world to have saved them. */
    public void forgetOldFalls() {
        long cutoff = System.currentTimeMillis() - KEEP_LANDED_MS;
        for (Journal.Entry e : List.copyOf(landed.values())) {
            if (e.landedAt() > cutoff) continue;
            landed.remove(e.id());
            World w = Bukkit.getWorld(e.world());
            if (w != null) for (Journal.Landed b : e.blocks()) landedAt.remove(key(w, b.at()));
            journal.delete(e.id());
        }
        fallGrace.values().removeIf(t -> t < System.currentTimeMillis());
    }

    /** Lands every fall still in the air, at once. Called when the plugin stops. */
    public void finishAll() {
        for (Active a : List.copyOf(active.values())) land(a);
    }

    public int activeFalls() {
        return active.size();
    }

    /**
     * After a restart, puts right any fall the journal says wasn't finished, or landed so recently the
     * world may not have saved it. A log is landed again only if it's in neither place: not back at
     * its origin (the removal wasn't saved) and not where it landed.
     */
    public void replay() {
        List<Journal.Entry> entries;
        try {
            entries = journal.load();
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Couldn't read the fall journal", e);
            return;
        }
        for (Journal.Entry e : entries) {
            World world = Bukkit.getWorld(e.world());
            if (world == null) {
                plugin.getLogger().warning("A fall journal names a world that isn't loaded, " + e.world() + "; keeping it for later.");
                continue;
            }
            Set<Pos> missing = new HashSet<>();
            if (!e.landed()) {
                for (Journal.Log l : e.logs()) {
                    world.getChunkAt(l.origin().x() >> 4, l.origin().z() >> 4);
                    Material m = Bukkit.createBlockData(l.data()).getMaterial();
                    if (block(world, l.origin()).getType() != m) missing.add(l.origin()); // else the removal never reached the disk
                }
            } else {
                // Only logs that landed as blocks can be checked. Items and logs given to the player can't.
                for (Journal.Landed b : e.blocks()) {
                    world.getChunkAt(b.at().x() >> 4, b.at().z() >> 4);
                    world.getChunkAt(b.origin().x() >> 4, b.origin().z() >> 4);
                    if (block(world, b.at()).getBlockData().getAsString().equals(b.data())) continue;
                    Material m = Bukkit.createBlockData(b.data()).getMaterial();
                    if (block(world, b.origin()).getType() == m) continue;
                    missing.add(b.origin());
                }
            }
            if (!missing.isEmpty()) {
                plugin.getLogger().info("Finishing a fall the server stopped during: landing " + missing.size() + " log(s) in " + e.world());
                Journal.Entry redo = new Journal.Entry(e.id(), e.world(), e.player(), e.playerName(), e.asItems(), e.minY(),
                    e.landed() ? null : e.sapling(), e.replant(), e.logs(), false, 0, List.of());
                settle(world, redo, e.minY(), null, missing);
            }
            journal.delete(e.id());
        }
    }
}

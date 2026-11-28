package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.Feller;
import com.extantape.paulbunyan.fall.Direction;
import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Scan;
import com.extantape.paulbunyan.tree.Tree;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Felling wedges. Right-click a trunk with one and it's set in that side; the tree then falls away from it, whatever
 * its lean. A small wedge fits a one-block trunk and a large one a 2x2 trunk. The wedge comes back when the tree
 * falls, when its log is broken some other way, when its owner takes it back (right-click the trunk with an empty
 * hand), after ten minutes, or when the plugin stops: to the player who set it if they're online, otherwise as an item
 * where it was.
 */
public final class Wedges implements Listener {
    /** A wedge left unused this long comes back by itself. */
    static final long KEEP_MS = 10 * 60 * 1000;
    private final Plugin plugin;
    private final SpecialAxes axes;
    private Feller feller;
    private final Map<String, Set> set = new HashMap<>();

    /** A wedge in a trunk: the log it's in, the side it's on, and who set it. */
    record Set(World world, Pos log, BlockFace face, Tool tool, UUID owner, ItemDisplay display, long at) {}

    public Wedges(Plugin plugin, SpecialAxes axes) {
        this.plugin = plugin;
        this.axes = axes;
    }

    public void feller(Feller feller) {
        this.feller = feller;
    }

    private static String key(World w, Pos p) {
        return w.getUID() + ":" + p;
    }

    /** The way a tree falls with a wedge in the given side of its trunk: straight away from it. */
    public static Direction away(BlockFace face) {
        return switch (face) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
            default -> null;
        };
    }

    /** Whether a wedge fits a trunk this many logs across at its base: small for one, large for a 2x2. */
    public static boolean fits(Tool wedge, int baseLogs) {
        return wedge == Tool.SMALL_WEDGE ? baseLogs == 1 : wedge == Tool.LARGE_WEDGE && baseLogs == 4;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        Block b = event.getClickedBlock();
        if (b == null) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        Tool t = axes.tool(hand);
        if (hand.getType().isAir()) {
            Set w = set.get(key(b.getWorld(), pos(b)));
            if (w != null && w.owner().equals(p.getUniqueId())) {
                remove(w);
                giveBack(w);
                p.sendActionBar(Component.text("You took your wedge back.", NamedTextColor.GRAY));
            }
            return;
        }
        if (t == null || !t.wedge()) return;
        event.setCancelled(true);
        Direction falls = away(event.getBlockFace());
        if (falls == null) {
            p.sendActionBar(Component.text("Set a wedge in the side of the trunk.", NamedTextColor.YELLOW));
            return;
        }
        Scan scan = feller.scan(b, p.getUniqueId());
        if (!scan.fells()) {
            p.sendActionBar(Component.text("A wedge only goes in a tree that would fall.", NamedTextColor.YELLOW));
            return;
        }
        Tree tree = scan.tree();
        int across = 0;
        for (Pos l : tree.logs()) if (l.y() == b.getY()) across++;
        if (!fits(t, across)) {
            p.sendActionBar(Component.text(t == Tool.SMALL_WEDGE ? "This trunk is too thick for a small wedge. Use a large one." : "A large wedge is for 2x2 trunks. Use a small one here.", NamedTextColor.YELLOW));
            return;
        }
        if (!feller.protection().mayBreak(p, b)) {
            p.sendActionBar(Component.text("This tree is in a claim or a protected spot.", NamedTextColor.YELLOW));
            return;
        }
        for (Set w : set.values())
            if (w.world().equals(b.getWorld()) && tree.logs().contains(w.log())) {
                p.sendActionBar(Component.text("There's already a wedge in this tree.", NamedTextColor.YELLOW));
                return;
            }
        ItemStack one = hand.clone();
        one.setAmount(1);
        hand.setAmount(hand.getAmount() - 1);
        BlockFace face = event.getBlockFace();
        Location at = b.getLocation().add(0.5 + face.getModX() * 0.52, 0.5, 0.5 + face.getModZ() * 0.52);
        ItemDisplay d = b.getWorld().spawn(at, ItemDisplay.class, e -> {
            e.setItemStack(one);
            e.setPersistent(false);
            float yaw = (float) Math.toRadians(switch (face) { case NORTH -> 180; case EAST -> 90; case WEST -> 270; default -> 0; });
            e.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(yaw, 0, 1, 0), new Vector3f(0.45f), new AxisAngle4f()));
        });
        Set w = new Set(b.getWorld(), pos(b), face, t, p.getUniqueId(), d, System.currentTimeMillis());
        set.put(key(b.getWorld(), w.log()), w);
        b.getWorld().playSound(Sound.sound(Key.key("block.anvil.place"), Sound.Source.BLOCK, 0.4f, 1.6f), at.getX(), at.getY(), at.getZ());
        p.sendActionBar(Component.text("Wedge set. This tree will fall " + falls.name().toLowerCase() + ".", NamedTextColor.GREEN));
    }

    /** Called as a tree is cut: the direction its wedge sends it, taking the wedge out to give back once it falls. */
    public Direction take(World world, Tree tree, List<Runnable> afterFall) {
        for (Set w : List.copyOf(set.values())) {
            if (!w.world().equals(world) || !tree.logs().contains(w.log())) continue;
            remove(w);
            afterFall.add(() -> giveBack(w));
            return away(w.face());
        }
        return null;
    }

    /** Something broke a block: a wedge in it comes back. */
    public void blockGone(Block b) {
        Set w = set.get(key(b.getWorld(), pos(b)));
        if (w == null) return;
        remove(w);
        giveBack(w);
    }

    /** Wedges left too long come back. Run every minute. */
    public void sweep() {
        long cutoff = System.currentTimeMillis() - KEEP_MS;
        for (Set w : List.copyOf(set.values()))
            if (w.at() < cutoff) {
                remove(w);
                giveBack(w);
            }
    }

    /** Gives every wedge back. Called when the plugin stops. */
    public void returnAll() {
        for (Set w : List.copyOf(set.values())) {
            remove(w);
            giveBack(w);
        }
    }

    private void remove(Set w) {
        set.remove(key(w.world(), w.log()));
        w.display().remove();
    }

    private void giveBack(Set w) {
        ItemStack item = axes.make(w.tool());
        Player p = Bukkit.getPlayer(w.owner());
        if (p != null && p.isOnline()) {
            for (ItemStack left : p.getInventory().addItem(item).values()) p.getWorld().dropItem(p.getLocation(), left);
        } else {
            w.world().dropItem(new Location(w.world(), w.log().x() + 0.5 + w.face().getModX() * 0.7, w.log().y() + 0.5, w.log().z() + 0.5 + w.face().getModZ() * 0.7), item);
        }
    }

    public int count() {
        return set.size();
    }

    private static Pos pos(Block b) {
        return new Pos(b.getX(), b.getY(), b.getZ());
    }

}

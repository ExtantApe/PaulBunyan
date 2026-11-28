package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.Feller;
import com.extantape.paulbunyan.FallenLogs;
import com.extantape.paulbunyan.tree.Scan;
import io.papermc.paper.datacomponent.DataComponentTypes;
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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The crosscut saw. Alone, hold right-click for three seconds on a fallen log to saw through its section (up to five
 * logs), or twice on a standing trunk to fell it. With two saws on opposite sides of one trunk, the sawyers take turns
 * swinging (left-click), each stroke between a third of a second and a second and a half after the other's; eight good
 * strokes saw through the whole fallen trunk, or fell the standing tree. Every log cut is broken as the player who cut
 * it, so claims, logging and drops work as usual. The saw wears one use per log.
 */
public final class Saw implements Listener {
    public static final float HOLD_SECONDS = 3;
    /** A stroke counts this many ticks after the partner's, and no sooner or later. */
    static final int RHYTHM_MIN = 6, RHYTHM_MAX = 30;
    /** Strokes it takes two sawyers to cut through. */
    static final int STROKES = 8;
    /** The longest trunk two sawyers cut in one go. */
    static final int WHOLE_TRUNK = 32;
    private final SpecialAxes axes;
    private final FallenLogs fallen;
    private final Feller feller;
    private final Map<UUID, Holding> holds = new HashMap<>();
    private final Map<String, Pull> pulls = new HashMap<>();

    private record Holding(Block log, int done) {}

    /** Two sawyers at one trunk: who swung last, when, from where, and how many good strokes so far. */
    static final class Pull {
        UUID last;
        long lastTick;
        double lastX, lastZ;
        int strokes;
    }

    public Saw(SpecialAxes axes, FallenLogs fallen, Feller feller) {
        this.axes = axes;
        this.fallen = fallen;
        this.feller = feller;
    }

    /** Whether a stroke by someone else, {@code gap} ticks after the last one, keeps the rhythm. */
    static boolean inRhythm(boolean otherSawyer, long gap) {
        return otherSawyer && gap >= RHYTHM_MIN && gap <= RHYTHM_MAX;
    }

    /** Whether two sawyers stand on opposite sides of the point (cx, cz), seen from above. */
    static boolean opposite(double ax, double az, double bx, double bz, double cx, double cz) {
        return (ax - cx) * (bx - cx) + (az - cz) * (bz - cz) < 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHoldDone(PlayerItemConsumeEvent event) {
        if (axes.tool(event.getItem()) != Tool.CROSSCUT_SAW) return;
        event.setCancelled(true); // a saw isn't used up
        Player p = event.getPlayer();
        Block b = p.getTargetBlockExact(4);
        if (b == null || !org.bukkit.Tag.LOGS.isTagged(b.getType())) {
            p.sendActionBar(Component.text("Hold the saw on a log.", NamedTextColor.YELLOW));
            return;
        }
        if (fallen.has(b)) {
            holds.remove(p.getUniqueId());
            cut(p, Bucking.section(b, fallen, 5));
            return;
        }
        Holding h = holds.get(p.getUniqueId());
        int done = h != null && h.log().equals(b) ? h.done() + 1 : 1;
        if (done < 2) {
            holds.put(p.getUniqueId(), new Holding(b, done));
            Scan s = feller.scan(b, p.getUniqueId());
            p.sendActionBar(Component.text(s.fells() ? "Halfway through the trunk. Keep sawing." : "This won't fall: " + s.skip().text + ".", s.fells() ? NamedTextColor.GRAY : NamedTextColor.YELLOW));
            return;
        }
        holds.remove(p.getUniqueId());
        fell(p, b);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player p = event.getPlayer();
        if (axes.tool(p.getInventory().getItemInMainHand()) != Tool.CROSSCUT_SAW) return;
        Block b = p.getTargetBlockExact(4);
        if (b == null || !org.bukkit.Tag.LOGS.isTagged(b.getType())) return;
        boolean down = fallen.has(b);
        String trunk = down ? trunkKey(Bucking.section(b, fallen, WHOLE_TRUNK)) : standingKey(p, b);
        if (trunk == null) return;
        long now = p.getWorld().getGameTime();
        Location at = p.getLocation();
        Pull pull = pulls.get(trunk);
        if (pull == null || now - pull.lastTick > RHYTHM_MAX * 2) {
            pull = new Pull();
            pulls.put(trunk, pull);
        } else if (pull.last.equals(p.getUniqueId())) {
            return; // your partner's turn
        } else if (inRhythm(true, now - pull.lastTick) && opposite(at.getX(), at.getZ(), pull.lastX, pull.lastZ, b.getX() + 0.5, b.getZ() + 0.5)) {
            pull.strokes++;
            p.getWorld().playSound(Sound.sound(Key.key("item.axe.strip"), Sound.Source.PLAYER, 0.8f, 0.8f + 0.05f * pull.strokes), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
            Component bar = Component.text("Sawing: " + pull.strokes + " of " + STROKES, NamedTextColor.GRAY);
            p.sendActionBar(bar);
            Player partner = Bukkit.getPlayer(pull.last);
            if (partner != null) partner.sendActionBar(bar);
            if (pull.strokes >= STROKES) {
                pulls.remove(trunk);
                if (down) cut(p, Bucking.section(b, fallen, WHOLE_TRUNK));
                else fell(p, b);
                return;
            }
        } else {
            pull.strokes = 0; // out of rhythm, or on the same side: start again
        }
        pull.last = p.getUniqueId();
        pull.lastTick = now;
        pull.lastX = at.getX();
        pull.lastZ = at.getZ();
        pulls.values().removeIf(x -> now - x.lastTick > 200);
    }

    private static String trunkKey(List<Block> section) {
        Block min = null;
        for (Block b : section) if (min == null || b.getX() + b.getY() + b.getZ() < min.getX() + min.getY() + min.getZ()) min = b;
        return min == null ? null : min.getWorld().getUID() + ":" + min.getX() + "," + min.getY() + "," + min.getZ();
    }

    private String standingKey(Player p, Block b) {
        Scan s = feller.scan(b, p.getUniqueId());
        if (!s.fells()) return null;
        var base = s.tree().base().getFirst();
        return b.getWorld().getUID() + ":tree:" + base;
    }

    /** Saws through fallen logs, one use each, as far as the saw lasts. */
    private void cut(Player p, List<Block> logs) {
        int n = 0;
        for (Block log : logs) {
            if (!uses(p, 1)) break;
            if (p.breakBlock(log)) {
                p.damageItemStack(EquipmentSlot.HAND, 1);
                n++;
            }
        }
        if (n > 0) p.getWorld().playSound(Sound.sound(Key.key("block.wood.break"), Sound.Source.BLOCK, 1f, 0.7f), logs.getFirst().getX() + 0.5, logs.getFirst().getY() + 0.5, logs.getFirst().getZ() + 0.5);
    }

    /** Fells a standing tree from the sawn log, as an axe would, wearing the saw one use per log. */
    private void fell(Player p, Block b) {
        Scan s = feller.scan(b, p.getUniqueId());
        if (!s.fells()) {
            p.sendActionBar(Component.text("This won't fall: " + s.skip().text + ".", NamedTextColor.YELLOW));
            return;
        }
        int logs = 0;
        for (var l : s.tree().logs()) if (l.y() >= b.getY()) logs++;
        if (!uses(p, logs)) {
            p.sendActionBar(Component.text("Your saw is too worn to fell this whole tree.", NamedTextColor.YELLOW));
            return;
        }
        ItemStack saw = p.getInventory().getItemInMainHand();
        Feller.Outcome o = feller.chop(p, b, saw, false);
        if (o == Feller.Outcome.FELLS) {
            p.breakBlock(b);
            p.damageItemStack(EquipmentSlot.HAND, logs);
        } else if (o == Feller.Outcome.PROTECTED) {
            p.sendActionBar(Component.text("Part of this tree is in a claim or a protected spot, so it won't fall.", NamedTextColor.YELLOW));
        }
    }

    /** Whether the saw in hand has this many uses left. */
    private boolean uses(Player p, int n) {
        ItemStack saw = p.getInventory().getItemInMainHand();
        if (axes.tool(saw) != Tool.CROSSCUT_SAW) return false;
        Integer max = saw.getData(DataComponentTypes.MAX_DAMAGE);
        return max == null || max - saw.getDataOrDefault(DataComponentTypes.DAMAGE, 0) > n;
    }
}

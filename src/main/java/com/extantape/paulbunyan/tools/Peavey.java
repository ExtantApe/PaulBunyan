package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.Feller;
import com.extantape.paulbunyan.FallenLogs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;

/**
 * The peavey. Right-click a fallen log and its section (up to five joined logs) rolls one block sideways across its
 * length, away from you, the way a real log rolls. It rolls only into air or plants, never into a person or an animal,
 * never in or out of a claim you can't build in, and never off a drop of more than a few blocks. Rolled off a small
 * step, it drops onto what's below. One use per roll.
 */
public final class Peavey implements Listener {
    /** The furthest a rolled section may drop after it rolls. */
    static final int MAX_DROP = 3;
    static final long COOLDOWN_MS = 400;
    private final SpecialAxes axes;
    private final FallenLogs fallen;
    private final Feller feller;
    private final Map<UUID, Long> last = new HashMap<>();

    public Peavey(SpecialAxes axes, FallenLogs fallen, Feller feller) {
        this.axes = axes;
        this.fallen = fallen;
        this.feller = feller;
    }

    /**
     * Which way a log lying along {@code axis} rolls when pushed from (px, pz): across its length, away from the pusher.
     * Returns {dx, dz}, or null for a log standing upright.
     */
    static int[] roll(char axis, double logX, double logZ, double px, double pz) {
        if (axis == 'x') return new int[] {0, pz <= logZ ? 1 : -1};
        if (axis == 'z') return new int[] {px <= logX ? 1 : -1, 0};
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        if (axes.tool(p.getInventory().getItemInMainHand()) != Tool.PEAVEY) return;
        event.setCancelled(true);
        Block b = event.getClickedBlock();
        if (b == null || !fallen.has(b)) {
            p.sendActionBar(Component.text("A peavey rolls fallen logs.", NamedTextColor.YELLOW));
            return;
        }
        long now = System.currentTimeMillis();
        if (now - last.getOrDefault(p.getUniqueId(), 0L) < COOLDOWN_MS) return;
        last.put(p.getUniqueId(), now);
        int[] d = roll(Bucking.axis(b), b.getX() + 0.5, b.getZ() + 0.5, p.getLocation().getX(), p.getLocation().getZ());
        if (d == null) {
            p.sendActionBar(Component.text("This log is standing on end. Only a log lying down rolls.", NamedTextColor.YELLOW));
            return;
        }
        List<Block> section = Bucking.section(b, fallen, 5);
        String why = blocked(p, section, d[0], d[1]);
        if (why != null) {
            p.sendActionBar(Component.text(why, NamedTextColor.YELLOW));
            return;
        }
        int drop = dropAfter(section, d[0], d[1]);
        if (drop > MAX_DROP) {
            p.sendActionBar(Component.text("It would roll off a drop.", NamedTextColor.YELLOW));
            return;
        }
        move(p, section, d[0], -drop, d[1]);
        p.damageItemStack(EquipmentSlot.HAND, 1);
        Block first = section.getFirst();
        p.getWorld().playSound(Sound.sound(Key.key("block.wood.place"), Sound.Source.BLOCK, 1f, 0.6f), first.getX() + 0.5, first.getY() + 0.5, first.getZ() + 0.5);
    }

    /** Why the section can't roll that way, or null if it can. */
    private String blocked(Player p, List<Block> section, int dx, int dz) {
        for (Block b : section) {
            Block to = b.getRelative(dx, 0, dz);
            if (!to.isReplaceable() || to.isLiquid()) return "Something's in the way.";
            if (!feller.protection().mayBreak(p, b) || !feller.protection().mayBreak(p, to)) return "That's in a claim or a protected spot.";
            for (var e : to.getWorld().getNearbyEntities(BoundingBox.of(to), e -> e instanceof LivingEntity)) return "Someone's in the way.";
        }
        return null;
    }

    /** How far the section drops once it has rolled: until any of its logs rests on something. */
    private static int dropAfter(List<Block> section, int dx, int dz) {
        for (int drop = 0; drop <= MAX_DROP + 1; drop++)
            for (Block b : section) {
                Block under = b.getRelative(dx, -drop - 1, dz);
                if (!under.isReplaceable() || under.isLiquid()) return drop;
            }
        return MAX_DROP + 1;
    }

    private void move(Player p, List<Block> section, int dx, int dy, int dz) {
        List<BlockData> data = new ArrayList<>();
        for (Block b : section) data.add(b.getBlockData());
        for (Block b : section) {
            feller.landedBlockGone(b);
            fallen.clear(b);
            feller.blockLog().removed(p.getName(), b.getLocation(), b.getBlockData());
            b.setType(org.bukkit.Material.AIR, false);
        }
        for (int i = 0; i < section.size(); i++) {
            Block to = section.get(i).getRelative(dx, dy, dz);
            to.setBlockData(data.get(i), true);
            fallen.mark(to);
            feller.blockLog().placed(p.getName(), to.getLocation(), data.get(i));
        }
    }
}

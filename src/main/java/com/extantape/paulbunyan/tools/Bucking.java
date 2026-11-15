package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.FallenLogs;
import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * A special axe bucks several fallen logs with one swing: the one you broke and the next ones along the same trunk,
 * nearest first. Each extra log is broken as the player, the next tick, so claims, CoreProtect, drops and axe wear all
 * work as they do for a log broken by hand.
 */
public final class Bucking {
    private final Plugin plugin;
    private final FallenLogs fallen;
    private final Set<UUID> busy = new HashSet<>();

    public Bucking(Plugin plugin, FallenLogs fallen) {
        this.plugin = plugin;
        this.fallen = fallen;
    }

    /** True while this player's extra logs are being broken, so those breaks don't buck again. */
    public boolean isBucking(UUID player) {
        return busy.contains(player);
    }

    /**
     * The next logs along a fallen trunk from {@code start}, along {@code axis} ('x', 'y' or 'z'), taking turns either side,
     * nearest first, stopping at the first gap on each side. At most {@code extra}.
     */
    public static List<Pos> line(Pos start, char axis, Predicate<Pos> joined, int extra) {
        List<Pos> out = new ArrayList<>();
        int dx = axis == 'x' ? 1 : 0, dy = axis == 'y' ? 1 : 0, dz = axis == 'z' ? 1 : 0;
        boolean up = true, down = true;
        for (int i = 1; out.size() < extra && (up || down); i++) {
            if (up) {
                Pos p = start.add(dx * i, dy * i, dz * i);
                if (joined.test(p)) out.add(p);
                else up = false;
            }
            if (down && out.size() < extra) {
                Pos p = start.add(-dx * i, -dy * i, -dz * i);
                if (joined.test(p)) out.add(p);
                else down = false;
            }
        }
        return out;
    }

    /** Called as {@code broken}, a fallen log, is broken with a special axe. */
    public void buck(Player player, Block broken, int logs) {
        if (logs <= 1 || !(broken.getBlockData() instanceof Orientable o)) return;
        Material type = broken.getType();
        char axis = o.getAxis() == Axis.X ? 'x' : o.getAxis() == Axis.Y ? 'y' : 'z';
        Pos start = new Pos(broken.getX(), broken.getY(), broken.getZ());
        List<Pos> next = line(start, axis, p -> {
            Block b = broken.getWorld().getBlockAt(p.x(), p.y(), p.z());
            return b.getType() == type && fallen.has(b);
        }, logs - 1);
        if (next.isEmpty()) return;
        var world = broken.getWorld();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            busy.add(player.getUniqueId());
            try {
                for (Pos p : next) {
                    Block b = world.getBlockAt(p.x(), p.y(), p.z());
                    if (b.getType() != type || !fallen.has(b)) continue;
                    // The axe can break on its last use: stop then.
                    if (!org.bukkit.Tag.ITEMS_AXES.isTagged(player.getInventory().getItemInMainHand().getType())) break;
                    player.breakBlock(b);
                }
            } finally {
                busy.remove(player.getUniqueId());
            }
        });
    }
}

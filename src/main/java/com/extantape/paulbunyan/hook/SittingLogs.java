package com.extantape.paulbunyan.hook;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * With GSit installed, lets players sit only on logs lying on their side, like a felled trunk, never on upright logs or
 * stairs. GSit's own settings choose which blocks can be sat on at all; this narrows it to sideways logs. GSit's event is
 * reached by name, so PaulBunyan doesn't need GSit.
 */
public final class SittingLogs implements Listener {
    static final String EVENT = "dev.geco.gsit.api.event.PreEntitySitEvent";

    private SittingLogs() {}

    /** True if sitting on this block is allowed: a log, wood or stem lying along x or z. */
    public static boolean allowed(Material type, Axis axis) {
        // By name, the same set as the logs tag plus bamboo blocks, so it works without a running server.
        String n = type.name();
        boolean log = n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE")
                || type == Material.BAMBOO_BLOCK || type == Material.STRIPPED_BAMBOO_BLOCK;
        return log && axis != null && axis != Axis.Y;
    }

    static boolean allowed(Block b) {
        Axis axis = b.getBlockData() instanceof Orientable o ? o.getAxis() : null;
        return allowed(b.getType(), axis);
    }

    @SuppressWarnings("unchecked")
    public static void register(Plugin owner, Logger log) {
        Plugin gsit = Bukkit.getPluginManager().getPlugin("GSit");
        if (gsit == null || !gsit.isEnabled()) return;
        try {
            Class<? extends Event> event = (Class<? extends Event>) Class.forName(EVENT, true, gsit.getClass().getClassLoader());
            var getBlock = event.getMethod("getBlock");
            Bukkit.getPluginManager().registerEvent(event, new SittingLogs(), EventPriority.HIGH, (l, e) -> {
                if (!event.isInstance(e)) return;
                try {
                    Block b = (Block) getBlock.invoke(e);
                    if (b != null && !allowed(b)) ((Cancellable) e).setCancelled(true);
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    // Can't tell what block it is, so no sitting: the safe answer for this rule.
                    ((Cancellable) e).setCancelled(true);
                }
            }, owner, true);
            log.info("GSit: players can sit only on logs lying on their side.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            log.warning("GSit is installed but its sit event couldn't be reached (" + e + "). Sitting isn't limited to sideways logs.");
        }
    }
}

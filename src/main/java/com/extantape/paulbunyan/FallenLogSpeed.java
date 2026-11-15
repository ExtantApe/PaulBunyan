package com.extantape.paulbunyan;

import java.util.function.DoubleSupplier;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;

/**
 * Fallen logs are joined, so they break slower than a loose log. While a player is digging one, their block break
 * speed is scaled down by {@code fallen-log-speed}; the game sends that to the player's client, so the crack and the
 * timing match what the server expects. The change is transient: it's never saved with the player, and it's taken
 * off as soon as they stop digging, break the block, start on another block, leave, or the plugin stops.
 */
public final class FallenLogSpeed implements Listener {
    private final NamespacedKey key;
    private final DoubleSupplier speed;
    private final FallenLogs fallen;

    public FallenLogSpeed(Plugin plugin, DoubleSupplier speed, FallenLogs fallen) {
        this.key = new NamespacedKey(plugin, "fallen-log-speed");
        this.speed = speed;
        this.fallen = fallen;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(BlockDamageEvent event) {
        Player p = event.getPlayer();
        clear(p);
        if (event.isCancelled()) return;
        Block b = event.getBlock();
        double s = speed.getAsDouble();
        if (s >= 1 || !Tag.LOGS.isTagged(b.getType()) || !fallen.has(b)) return;
        AttributeInstance a = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (a == null) return;
        a.addTransientModifier(new AttributeModifier(key, s - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAbort(BlockDamageAbortEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    /** Takes the slowdown off everyone. Called when the plugin stops. */
    public void clearAll(Iterable<? extends Player> players) {
        for (Player p : players) clear(p);
    }

    private void clear(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (a != null && a.getModifier(key) != null) a.removeModifier(key);
    }
}

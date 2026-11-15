package com.extantape.paulbunyan;

import com.extantape.paulbunyan.tools.AxeKind;
import com.extantape.paulbunyan.tree.Species;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * How fast a player breaks a log, while they're digging it. Fallen logs are joined, so they break slower
 * ({@code fallen-log-speed}), and a special axe swings faster on its own handle's wood. The change is to the player's
 * block break speed, which the game sends to their client, so the crack and the timing match what the server expects.
 * It's transient: never saved with the player, and taken off as soon as they stop digging, break the block, start on
 * another block, leave, or the plugin stops.
 */
public final class DigSpeed implements Listener {
    private final NamespacedKey key;
    private final DoubleSupplier fallenSpeed;
    private final FallenLogs fallen;
    private final Function<ItemStack, AxeKind> axes;

    public DigSpeed(Plugin plugin, DoubleSupplier fallenSpeed, FallenLogs fallen, Function<ItemStack, AxeKind> axes) {
        this.key = new NamespacedKey(plugin, "fallen-log-speed");
        this.fallenSpeed = fallenSpeed;
        this.fallen = fallen;
        this.axes = axes;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(BlockDamageEvent event) {
        Player p = event.getPlayer();
        clear(p);
        if (event.isCancelled()) return;
        Block b = event.getBlock();
        if (!Tag.LOGS.isTagged(b.getType())) return;
        AxeKind kind = axes.apply(event.getItemInHand());
        double f = DigSpeedFactor.of(fallen.has(b), fallenSpeed.getAsDouble(), kind != null && kind.ownWood(Species.ofLog(b.getType().name())));
        if (f == 1) return;
        AttributeInstance a = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (a == null) return;
        a.addTransientModifier(new AttributeModifier(key, f - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
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

    /** Takes the change off everyone. Called when the plugin stops. */
    public void clearAll(Iterable<? extends Player> players) {
        for (Player p : players) clear(p);
    }

    private void clear(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (a != null && a.getModifier(key) != null) a.removeModifier(key);
    }
}

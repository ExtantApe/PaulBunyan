package com.extantape.paulbunyan;

import com.extantape.paulbunyan.tree.Species;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

/** Turns axe chops into falls, and keeps track of logs players place. */
public final class ChopListener implements Listener {
    private final Feller feller;

    public ChopListener(Feller feller) {
        this.feller = feller;
    }

    /**
     * Runs last, once protection plugins have had their say: a cancelled break never fells anything.
     * The tree is read here, before the log is gone, and falls on the next tick.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        feller.landedBlockGone(block);
        Species species = Species.ofLog(block.getType().name());
        boolean tracked = species != null || isCap(block.getType());
        boolean wasPlaced = tracked && feller.placed().isPlaced(block);
        if (wasPlaced) feller.placed().clear(block);
        if (species == null || wasPlaced) return;

        Player player = event.getPlayer();
        Settings s = feller.settings();
        if (player.isSneaking()) return;
        if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.ADVENTURE) return;
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        if (creative && !s.creativeFells()) return;
        if (species.nether() && !s.netherFungi()) return;
        if (s.disabledWorlds().contains(block.getWorld().getName())) return;
        if (!player.hasPermission("paulbunyan.fell")) return;
        ItemStack axe = player.getInventory().getItemInMainHand();
        if (!Tag.ITEMS_AXES.isTagged(axe.getType())) return;
        if (feller.isBusy(block)) return;

        Feller.Outcome outcome = feller.chop(player, block, axe, !creative);
        if (outcome == Feller.Outcome.AXE_TOO_WORN) {
            player.sendActionBar(Component.text("Your axe is too worn to fell this whole tree.", NamedTextColor.YELLOW));
        } else if (outcome == Feller.Outcome.PROTECTED) {
            player.sendActionBar(Component.text("Part of this tree is in a claim you can't build in.", NamedTextColor.YELLOW));
        } else if (outcome == Feller.Outcome.TOUCHES_BUILD) {
            player.sendActionBar(Component.text("This tree touches a build, so it won't fall. Chopping it log by log.", NamedTextColor.YELLOW));
        } else if (outcome == Feller.Outcome.TOO_BIG) {
            player.sendActionBar(Component.text("This tree is too big to fall in one go. Chopping it log by log.", NamedTextColor.YELLOW));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        if (Species.ofLog(b.getType().name()) != null || isCap(b.getType())) feller.placed().mark(b);
    }

    /** A tree grown from a sapling is natural, even where a placed log once stood. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(StructureGrowEvent event) {
        for (BlockState s : event.getBlocks()) feller.placed().clear(s.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        forget(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        for (Block b : event.blockList()) forget(b);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        for (Block b : event.blockList()) forget(b);
    }

    private void forget(Block b) {
        feller.landedBlockGone(b);
        if (Species.ofLog(b.getType().name()) != null || isCap(b.getType())) feller.placed().clear(b);
    }

    /** No fall damage for someone who was on or under a tree as it fell. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player p
            && feller.sparesFallDamage(p.getUniqueId())) event.setCancelled(true);
    }

    static boolean isCap(Material m) {
        return m == Material.NETHER_WART_BLOCK || m == Material.WARPED_WART_BLOCK || m == Material.SHROOMLIGHT;
    }
}

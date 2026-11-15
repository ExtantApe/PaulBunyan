package com.extantape.paulbunyan.tools;

import org.bukkit.Keyed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * PaulBunyan's tools only go into PaulBunyan's own recipes. Otherwise a wedge would count as a trial key and two
 * special axes could be "repaired" into a plain iron axe, losing everything that made them special.
 */
public final class RecipeGuard implements Listener {
    private final SpecialAxes axes;

    public RecipeGuard(SpecialAxes axes) {
        this.axes = axes;
    }

    static boolean ourRecipe(Recipe r) {
        return r instanceof Keyed k && k.getKey().getNamespace().equals("paulbunyan");
    }

    private boolean anyOurs(ItemStack[] items) {
        for (ItemStack i : items) if (i != null && axes.ours(i)) return true;
        return false;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!ourRecipe(event.getRecipe()) && anyOurs(event.getInventory().getMatrix())) event.getInventory().setResult(null);
    }

    @EventHandler
    public void onCrafter(CrafterCraftEvent event) {
        if (ourRecipe(event.getRecipe())) return;
        if (event.getBlock().getState() instanceof org.bukkit.block.Crafter c && anyOurs(c.getInventory().getContents())) event.setCancelled(true);
    }
}

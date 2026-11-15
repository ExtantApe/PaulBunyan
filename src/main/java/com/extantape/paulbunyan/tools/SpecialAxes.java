package com.extantape.paulbunyan.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import io.papermc.paper.registry.set.RegistrySet;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Makes and recognises special axes, and registers their recipes. A special axe is an iron axe with its own name,
 * uses, speed, repair material and look; which one it is lives in the item's data as {@code paulbunyan:axe}. Nothing
 * vanilla is changed: the recipes use stripped logs for the handle, which no vanilla axe recipe does.
 */
public final class SpecialAxes {
    /** The item model the resource pack draws, from the two custom model data strings: handle, then head. */
    public static final String MODEL = "paulbunyan:axe";
    private final Plugin plugin;
    private final NamespacedKey key;
    private final List<NamespacedKey> recipes = new ArrayList<>();
    private boolean usePack;

    public SpecialAxes(Plugin plugin, boolean usePack) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "axe");
        this.usePack = usePack;
    }

    public void usePack(boolean usePack) {
        this.usePack = usePack;
    }

    /** Which special axe this is, or null for any other item. */
    public AxeKind of(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE) return null;
        return AxeKind.parse(item.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    @SuppressWarnings("UnstableApiUsage")
    public ItemStack make(AxeKind kind) {
        Head head = kind.head();
        ItemStack item = ItemStack.of(Material.IRON_AXE);
        item.editPersistentDataContainer(pdc -> pdc.set(key, PersistentDataType.STRING, kind.id()));
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(head.title() + " Axe"));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
            plain(kind.handle().title() + " handle", NamedTextColor.GRAY),
            plain(head.perk, NamedTextColor.DARK_AQUA),
            plain("Bucks " + AxeKind.BUCKS + " fallen logs at once, " + AxeKind.BUCKS_OWN_WOOD + " of " + kind.handle().title().toLowerCase() + ".", NamedTextColor.GRAY))));
        item.setData(DataComponentTypes.MAX_DAMAGE, head.uses);
        var blocks = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK);
        Tool.Builder tool = Tool.tool().defaultMiningSpeed(1).damagePerBlock(1);
        if (head.stemSpeed() != head.speed) {
            tool.addRule(Tool.rule(blocks.getTag(BlockTypeTagKeys.CRIMSON_STEMS), head.stemSpeed(), TriState.TRUE));
            tool.addRule(Tool.rule(blocks.getTag(BlockTypeTagKeys.WARPED_STEMS), head.stemSpeed(), TriState.TRUE));
        }
        tool.addRule(Tool.rule(blocks.getTag(BlockTypeTagKeys.MINEABLE_AXE), head.speed, TriState.TRUE));
        item.setData(DataComponentTypes.TOOL, tool.build());
        Material repair = Material.matchMaterial(head.material);
        if (repair != null) item.setData(DataComponentTypes.REPAIRABLE, Repairable.repairable(
            RegistrySet.keySet(RegistryKey.ITEM, TypedKey.create(RegistryKey.ITEM, repair.getKey()))));
        if (head == Head.PRISMARINE) {
            // A player's base submerged mining speed is 0.2: five times that is full speed.
            ItemAttributeModifiers base = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            ItemAttributeModifiers.Builder mods = ItemAttributeModifiers.itemAttributes();
            if (base != null) for (var e : base.modifiers()) mods.addModifier(e.attribute(), e.modifier(), e.getGroup());
            mods.addModifier(Attribute.SUBMERGED_MINING_SPEED, new AttributeModifier(new NamespacedKey(plugin, "prismarine"), 4,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.MAINHAND), EquipmentSlotGroup.MAINHAND);
            item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, mods.build());
        }
        if (usePack) {
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key(MODEL));
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                .addStrings(List.of(kind.handle().id(), head.id())).build());
        }
        return item;
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** One shaped recipe per handle and head: the head material where an axe's iron goes, stripped logs for the stick. */
    public void registerRecipes() {
        for (Handle h : Handle.values()) for (Head d : Head.values()) {
            Material handle = Material.matchMaterial(h.material), head = Material.matchMaterial(d.material);
            if (handle == null || head == null) {
                plugin.getLogger().warning("No " + (handle == null ? h.material : d.material) + " on this server, so there's no " + d.id() + " axe with a " + h.id() + " handle.");
                continue;
            }
            NamespacedKey k = new NamespacedKey(plugin, "axe_" + h.id() + "_" + d.id());
            ShapedRecipe r = new ShapedRecipe(k, make(new AxeKind(h, d)));
            r.shape("HH", "HS", " S");
            r.setIngredient('H', head);
            r.setIngredient('S', handle);
            r.setGroup("paulbunyan_" + d.id() + "_axe");
            Bukkit.removeRecipe(k, false);
            Bukkit.addRecipe(r, false);
            recipes.add(k);
        }
        Bukkit.updateRecipes();
    }

    public List<NamespacedKey> recipes() {
        return recipes;
    }

    public void unregisterRecipes() {
        for (NamespacedKey k : recipes) Bukkit.removeRecipe(k, false);
        recipes.clear();
        Bukkit.updateRecipes();
    }

}

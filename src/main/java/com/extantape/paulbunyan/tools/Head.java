package com.extantape.paulbunyan.tools;

import java.util.Locale;

/**
 * The eleven axe heads (owner, 2026-11-15: "All eleven at once"), each with one small perk. The numbers sit on an iron
 * axe's: 250 uses and mining speed 6.
 */
public enum Head {
    COPPER("COPPER_INGOT", 250, 6, "A plain, honest axe."),
    IRON("IRON_INGOT", 333, 6, "Wears a quarter slower."),
    GOLD("GOLD_INGOT", 125, 12, "The fastest axe, and the quickest to wear out."),
    EMERALD("EMERALD", 250, 6, "Trees it fells drop twice the saplings and fruit."),
    AMETHYST("AMETHYST_SHARD", 250, 6, "Shows which way a tree will fall before you cut it."),
    QUARTZ("QUARTZ", 250, 6, "Twice as fast on crimson and warped stems."),
    LAPIS("LAPIS_LAZULI", 250, 6, "Felling a tree gives experience."),
    REDSTONE("REDSTONE", 250, 6, "Fallen logs it bucks go straight into your inventory."),
    GLOWSTONE("GLOWSTONE_DUST", 250, 6, "Logs it fells glow where they land for half a minute."),
    OBSIDIAN("OBSIDIAN", 500, 6, "Wears half as fast."),
    PRISMARINE("PRISMARINE_SHARD", 250, 6, "Full speed underwater.");

    /** What the head is made of in the recipe, as a material name. */
    public final String material;
    public final int uses;
    public final float speed;
    /** The perk, as the item's description says it. */
    public final String perk;

    Head(String material, int uses, float speed, String perk) {
        this.material = material;
        this.uses = uses;
        this.speed = speed;
        this.perk = perk;
    }

    /** Mining speed on crimson and warped stems. */
    public float stemSpeed() {
        return this == QUARTZ ? speed * 2 : speed;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String title() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }

    public static Head byId(String id) {
        for (Head h : values()) if (h.id().equals(id)) return h;
        return null;
    }
}

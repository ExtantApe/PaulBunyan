package com.extantape.paulbunyan.tree;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Tag;

/**
 * Which blocks can sit next to a natural tree trunk. Anything not listed counts as part of a build,
 * and a tree touching a build doesn't fall. The list errs on the side of "build": an unknown block
 * makes the plugin leave the tree alone.
 */
public final class Palette {
    private Palette() {}

    private static final Set<Material> NATURAL = EnumSet.noneOf(Material.class);

    static {
        for (String name : new String[] {
            "AIR", "CAVE_AIR", "WATER", "BUBBLE_COLUMN", "LAVA",
            "SNOW", "SNOW_BLOCK", "POWDER_SNOW", "ICE", "PACKED_ICE", "BLUE_ICE",
            "DIRT", "GRASS_BLOCK", "PODZOL", "COARSE_DIRT", "ROOTED_DIRT", "MYCELIUM", "MOSS_BLOCK",
            "MUD", "CLAY", "SAND", "RED_SAND", "GRAVEL", "SUSPICIOUS_SAND", "SUSPICIOUS_GRAVEL",
            "SANDSTONE", "RED_SANDSTONE", "CALCITE", "DRIPSTONE_BLOCK", "POINTED_DRIPSTONE", "OBSIDIAN",
            "TERRACOTTA", "WHITE_TERRACOTTA", "ORANGE_TERRACOTTA", "YELLOW_TERRACOTTA", "BROWN_TERRACOTTA",
            "RED_TERRACOTTA", "LIGHT_GRAY_TERRACOTTA",
            "MANGROVE_ROOTS", "MUDDY_MANGROVE_ROOTS", "MANGROVE_PROPAGULE", "MOSS_CARPET",
            "PALE_MOSS_BLOCK", "PALE_MOSS_CARPET", "PALE_HANGING_MOSS", "CREAKING_HEART", "RESIN_CLUMP",
            "SHORT_GRASS", "TALL_GRASS", "FERN", "LARGE_FERN", "DEAD_BUSH", "BUSH", "SHORT_DRY_GRASS",
            "TALL_DRY_GRASS", "FIREFLY_BUSH", "LEAF_LITTER", "WILDFLOWERS", "PINK_PETALS", "CACTUS_FLOWER",
            "VINE", "COCOA", "BEE_NEST", "GLOW_LICHEN", "HANGING_ROOTS", "SPORE_BLOSSOM", "SWEET_BERRY_BUSH",
            "BROWN_MUSHROOM", "RED_MUSHROOM", "BROWN_MUSHROOM_BLOCK", "RED_MUSHROOM_BLOCK", "MUSHROOM_STEM",
            "SUGAR_CANE", "BAMBOO", "BAMBOO_SAPLING", "CACTUS", "LILY_PAD", "SEAGRASS", "TALL_SEAGRASS",
            "KELP", "KELP_PLANT", "AZALEA", "FLOWERING_AZALEA", "BIG_DRIPLEAF", "BIG_DRIPLEAF_STEM",
            "SMALL_DRIPLEAF", "CAVE_VINES", "CAVE_VINES_PLANT", "SCULK", "SCULK_VEIN",
            "CRIMSON_NYLIUM", "WARPED_NYLIUM", "NETHER_WART_BLOCK", "WARPED_WART_BLOCK", "SHROOMLIGHT",
            "WEEPING_VINES", "WEEPING_VINES_PLANT", "TWISTING_VINES", "TWISTING_VINES_PLANT",
            "CRIMSON_ROOTS", "WARPED_ROOTS", "NETHER_SPROUTS", "CRIMSON_FUNGUS", "WARPED_FUNGUS",
            "SOUL_SAND", "SOUL_SOIL", "MAGMA_BLOCK", "GLOWSTONE", "NETHER_GOLD_ORE", "NETHER_QUARTZ_ORE",
        }) {
            Material m = Material.getMaterial(name);
            if (m != null) NATURAL.add(m);
        }
        NATURAL.addAll(Tag.DIRT.getValues());
        NATURAL.addAll(Tag.BASE_STONE_OVERWORLD.getValues());
        NATURAL.addAll(Tag.BASE_STONE_NETHER.getValues());
        NATURAL.addAll(Tag.FLOWERS.getValues());
        NATURAL.addAll(Tag.SAPLINGS.getValues());
        for (Material m : Material.values()) if (!m.isLegacy() && m.isBlock() && m.name().endsWith("_ORE")) NATURAL.add(m);
    }

    public static boolean isLeaves(Material m) {
        return Tag.LEAVES.isTagged(m);
    }

    /** A log or stem that grows on a natural tree. Stripped logs and wood blocks don't. */
    public static boolean isTreeLog(Material m) {
        return Species.ofLog(m.name()) != null;
    }

    /** May this block sit next to a natural trunk without making it part of a build? */
    public static boolean isNaturalNeighbour(Material m) {
        return isLeaves(m) || isTreeLog(m) || NATURAL.contains(m);
    }

    /** Ground a fallen block can't rest on and that destroys an item dropped into it. */
    public static boolean destroysItems(Material m) {
        return m == Material.LAVA || m == Material.FIRE || m == Material.SOUL_FIRE;
    }
}

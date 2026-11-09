package com.extantape.paulbunyan.tree;

import java.util.Set;

/**
 * The trees PaulBunyan fells, each with its log, the blocks that act as its leaves and what it
 * replants. Names are block names, so this class loads without a server (the unit tests rely on that).
 */
public enum Species {
    OAK("OAK_LOG", "OAK_SAPLING"),
    BIRCH("BIRCH_LOG", "BIRCH_SAPLING"),
    SPRUCE("SPRUCE_LOG", "SPRUCE_SAPLING"),
    JUNGLE("JUNGLE_LOG", "JUNGLE_SAPLING"),
    ACACIA("ACACIA_LOG", "ACACIA_SAPLING"),
    DARK_OAK("DARK_OAK_LOG", "DARK_OAK_SAPLING"),
    PALE_OAK("PALE_OAK_LOG", "PALE_OAK_SAPLING"),
    CHERRY("CHERRY_LOG", "CHERRY_SAPLING"),
    MANGROVE("MANGROVE_LOG", "MANGROVE_PROPAGULE"),
    CRIMSON("CRIMSON_STEM", "CRIMSON_FUNGUS", "NETHER_WART_BLOCK", "SHROOMLIGHT"),
    WARPED("WARPED_STEM", "WARPED_FUNGUS", "WARPED_WART_BLOCK", "SHROOMLIGHT");

    public final String log;
    public final String sapling;
    /** For Nether fungi, the blocks of the cap. Overworld trees use any leaves instead. */
    public final Set<String> caps;

    Species(String log, String sapling, String... caps) {
        this.log = log;
        this.sapling = sapling;
        this.caps = Set.of(caps);
    }

    public boolean nether() {
        return !caps.isEmpty();
    }

    /** Dark and pale oak only grow from four saplings in a square. */
    public boolean needsFour() {
        return this == DARK_OAK || this == PALE_OAK;
    }

    /** Spruce and jungle grow giant from four saplings, or small from one. */
    public boolean mayBeFour() {
        return this == SPRUCE || this == JUNGLE;
    }

    /** The nylium a fungus has to stand on to grow, or null for an overworld tree. */
    public String nylium() {
        return this == CRIMSON ? "CRIMSON_NYLIUM" : this == WARPED ? "WARPED_NYLIUM" : null;
    }

    public static Species ofLog(String blockName) {
        for (Species s : values()) if (s.log.equals(blockName)) return s;
        return null;
    }
}

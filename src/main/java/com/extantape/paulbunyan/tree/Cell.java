package com.extantape.paulbunyan.tree;

/**
 * What the scanner needs to know about one block. {@code name} is the block's name, such as
 * OAK_LEAVES. The Bukkit side builds these; the tests make them up.
 */
public record Cell(Kind kind, String name, boolean persistent) {
    public enum Kind {
        /** Air, or water a log can't rest in. */
        EMPTY,
        /** A log or stem of a tree that nobody placed, as far as the plugin knows. */
        LOG,
        /** A log or stem the plugin saw a player place. */
        PLACED_LOG,
        /** Leaves, or a block of a fungus cap. {@code persistent} is true when a player placed it. */
        CAP,
        /** Ground, plants and other blocks that grow next to trees. */
        NATURAL,
        /** Anything else: planks, glass, torches, signs. Part of a build. */
        BUILD,
        /** Not loaded: the scanner can't see it, so it treats it like a build. */
        UNKNOWN
    }

    public static final Cell EMPTY = new Cell(Kind.EMPTY, "AIR", false);

    public Species species() {
        return kind == Kind.LOG || kind == Kind.PLACED_LOG ? Species.ofLog(name) : null;
    }
}

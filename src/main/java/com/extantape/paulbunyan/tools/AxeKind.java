package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.tree.Species;

/** One special axe: a handle wood and a head. Written on the item as {@code handle/head}, such as {@code oak/copper}. */
public record AxeKind(Handle handle, Head head) {
    /** Any special axe bucks this many fallen logs at once. */
    public static final int BUCKS = 3;
    /** On a tree of its handle's wood, it bucks this many. */
    public static final int BUCKS_OWN_WOOD = 5;
    /** And swings this much faster there. */
    public static final double OWN_WOOD_SPEED = 1.5;

    public String id() {
        return handle.id() + "/" + head.id();
    }

    public static AxeKind parse(String id) {
        if (id == null) return null;
        int slash = id.indexOf('/');
        if (slash < 0) return null;
        Handle h = Handle.byId(id.substring(0, slash));
        Head d = Head.byId(id.substring(slash + 1));
        return h == null || d == null ? null : new AxeKind(h, d);
    }

    public boolean ownWood(Species s) {
        return s != null && s == handle.species;
    }

    public int bucks(Species s) {
        return ownWood(s) ? BUCKS_OWN_WOOD : BUCKS;
    }
}

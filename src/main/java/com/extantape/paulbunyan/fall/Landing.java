package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;

/**
 * Finds where a fallen log comes to rest. It never replaces anything: it rises out of whatever is at its spot,
 * drops through air, and then sits on the first block below it. If that block has a full top it
 * becomes a block (in blocks mode); otherwise, or if something is in the way, it becomes an item.
 * Lava, fire, the void and land the player can't build on send the log to the player instead.
 *
 * <p>Real felling (felling-plan.md): a log that ends up inside the ground, rather than on logs that landed first, is
 * crushed into an item. A log that drops further than {@link Drops#itemsAfter} breaks into an item, and further than
 * {@link Drops#sticksAfter} splinters into sticks.
 */
public final class Landing {
    /** How high a log may rise out of a slope before the spot counts as buried. */
    public static final int MAX_RISE = 8;

    public enum Outcome { BLOCK, ITEM, CRUSHED, SPLINTERED, TO_PLAYER }

    /** How far a log may drop and still land whole, and how far before it splinters. In blocks. */
    public record Drops(int itemsAfter, int sticksAfter) {
        public static final Drops DEFAULT = new Drops(4, 12);
        /** The rules before real felling: any drop lands whole. */
        public static final Drops NEVER = new Drops(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public record Result(Outcome outcome, Pos at) {}

    public interface Ground {
        /** Plain air: a log may occupy it. */
        boolean isAir(Pos p);

        /** A full top face that a log can rest on. */
        boolean sturdyTop(Pos p);

        /** Lava or fire, which destroy dropped items. */
        boolean destroysItems(Pos p);

        /** The lowest y the world has. */
        int worldMin();

        /** A mob, player or vehicle is in this block space. */
        boolean occupied(Pos p);

        /** Any log or stem block, such as one that landed a moment ago. Logs land on logs; anything else crushes. */
        default boolean isLog(Pos p) { return false; }

        /** The player may build here. */
        boolean mayBuild(Pos p);
    }

    private Landing() {}

    public static Result find(Ground g, Pos target, int floorY, boolean asItems) {
        return find(g, target, floorY, asItems, Drops.NEVER);
    }

    public static Result find(Ground g, Pos target, int floorY, boolean asItems, Drops drops) {
        Pos p = new Pos(target.x(), Math.max(target.y(), floorY), target.z());
        int rise = 0;
        boolean crushed = false;
        while (!g.isAir(p)) {
            if (++rise > MAX_RISE) return new Result(Outcome.TO_PLAYER, null);
            if (!g.isLog(p)) crushed = true;
            p = p.above();
        }
        int top = p.y();
        while (p.y() > g.worldMin() && g.isAir(p.below())) p = p.below();
        if (p.y() <= g.worldMin()) return new Result(Outcome.TO_PLAYER, null);
        Pos below = p.below();
        if (g.destroysItems(below) || !g.mayBuild(p)) return new Result(Outcome.TO_PLAYER, null);
        int dropped = top - p.y();
        if (crushed) return new Result(Outcome.CRUSHED, p);
        if (dropped > drops.sticksAfter()) return new Result(Outcome.SPLINTERED, p);
        if (dropped > drops.itemsAfter()) return new Result(Outcome.ITEM, p);
        if (!asItems && g.sturdyTop(below) && !g.occupied(p)) return new Result(Outcome.BLOCK, p);
        return new Result(Outcome.ITEM, p);
    }
}

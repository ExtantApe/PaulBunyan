package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;

/**
 * Finds where a fallen log comes to rest. It never replaces anything: it rises out of the ground,
 * drops through air, and then sits on the first block below it. If that block has a full top it
 * becomes a block (in blocks mode); otherwise, or if something is in the way, it becomes an item.
 * Lava, fire, the void and land the player can't build on send the log to the player instead.
 */
public final class Landing {
    /** How high a log may rise out of a slope before the spot counts as buried. */
    public static final int MAX_RISE = 8;

    public enum Outcome { BLOCK, ITEM, TO_PLAYER }

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

        /** The player may build here. */
        boolean mayBuild(Pos p);
    }

    private Landing() {}

    public static Result find(Ground g, Pos target, int floorY, boolean asItems) {
        Pos p = new Pos(target.x(), Math.max(target.y(), floorY), target.z());
        int rise = 0;
        while (!g.isAir(p)) {
            if (++rise > MAX_RISE) return new Result(Outcome.TO_PLAYER, null);
            p = p.above();
        }
        while (p.y() > g.worldMin() && g.isAir(p.below())) p = p.below();
        if (p.y() <= g.worldMin()) return new Result(Outcome.TO_PLAYER, null);
        Pos below = p.below();
        if (g.destroysItems(below) || !g.mayBuild(p)) return new Result(Outcome.TO_PLAYER, null);
        if (!asItems && g.sturdyTop(below) && !g.occupied(p)) return new Result(Outcome.BLOCK, p);
        return new Result(Outcome.ITEM, p);
    }
}

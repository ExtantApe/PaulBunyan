package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;
import java.util.Collection;
import java.util.Set;

/**
 * How far a tree tips before it strikes the ground (felling-plan.md, terrain). On flat ground that's flat, 90 degrees.
 * Uphill it strikes the slope sooner and lands short, and its crown ends up in the hill, where it's crushed. Downhill
 * or over an edge it tips past flat before it strikes anything. Either way it always comes to rest flat on the block
 * grid: only the animation shows the angle.
 */
public final class Terrain {
    /** Degrees between the angles tried. */
    static final int STEP = 3;
    /** The furthest the animation tips, past flat, over a drop. */
    public static final int MAX_DEGREES = 135;
    /** Uphill only counts when the tree strikes before this: a bump just short of flat changes nothing. */
    public static final int UPHILL_BELOW = 81;
    /**
     * Struck sooner than this, the tree is leaning on something right beside it rather than landing on a slope. It
     * lands flat, as on level ground, and whatever ends up inside what it struck is crushed.
     */
    public static final int SHORTEST = 30;

    /** What stops a falling tree. Grass, flowers and anything else a player walks through don't. */
    public interface Solid {
        boolean solid(Pos p);
    }

    private Terrain() {}

    /**
     * The angle, in degrees, the tree reaches just before any log would enter something solid, or {@code maxDegrees}
     * if nothing is struck. {@code ignore} is the tree itself, still standing while this is worked out.
     */
    public static int contact(Solid g, FallPlan plan, Collection<Pos> logs, Set<Pos> ignore, int maxDegrees) {
        int last = 0;
        for (int deg = STEP; deg <= maxDegrees; deg += STEP) {
            double rad = Math.toRadians(deg);
            for (Pos p : logs) {
                Pos at = plan.tippedAt(p, rad);
                if (ignore.contains(at) || !g.solid(at)) continue;
                return last;
            }
            last = deg;
        }
        return maxDegrees;
    }
}

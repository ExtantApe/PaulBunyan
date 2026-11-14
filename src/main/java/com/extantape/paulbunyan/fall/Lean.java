package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;

import java.util.Collection;

/**
 * Which way a cut tree falls: toward its heaviest side, from the shape of its logs and leaves above the cut. A tree that
 * stands nearly straight falls away from whoever cut it.
 */
public final class Lean {
    /** Leaves weigh less than logs. */
    static final double LEAF_WEIGHT = 0.25;
    /** How far, in blocks, the weight has to sit off the cut before it decides the way. */
    static final double MIN_OFFSET = 0.35;

    private Lean() {}

    public static Direction of(Collection<Pos> logs, Collection<Pos> leaves, Collection<Pos> cut, Direction fallback) {
        double cx = 0, cz = 0;
        for (Pos p : cut) {
            cx += p.x() + 0.5;
            cz += p.z() + 0.5;
        }
        cx /= Math.max(1, cut.size());
        cz /= Math.max(1, cut.size());
        double wx = 0, wz = 0, total = 0;
        for (Pos p : logs) {
            wx += p.x() + 0.5 - cx;
            wz += p.z() + 0.5 - cz;
            total += 1;
        }
        for (Pos p : leaves) {
            wx += (p.x() + 0.5 - cx) * LEAF_WEIGHT;
            wz += (p.z() + 0.5 - cz) * LEAF_WEIGHT;
            total += LEAF_WEIGHT;
        }
        if (total == 0) return fallback;
        double ox = wx / total, oz = wz / total;
        if (Math.max(Math.abs(ox), Math.abs(oz)) < MIN_OFFSET) return fallback;
        if (Math.abs(ox) >= Math.abs(oz)) return ox > 0 ? Direction.EAST : Direction.WEST;
        return oz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}

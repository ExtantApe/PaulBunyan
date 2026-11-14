package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Picks where saplings sprout along a cleared fall: a few spots, spread out, the same ones for the same seed.
 * Species that only grow from four saplings in a square get one square or nothing.
 */
public final class Sprouts {
    private Sprouts() {}

    /** No two sprouts closer than this, in blocks, either way. */
    static final int SPACING = 3;

    /**
     * Up to {@code max} spots out of {@code spots} (all of them places a sapling can go), or, when
     * {@code square} is true, one 2x2 square of them, if there is one.
     */
    public static List<Pos> choose(List<Pos> spots, Set<Pos> valid, int max, long seed, boolean square) {
        List<Pos> order = new ArrayList<>(spots);
        order.sort(java.util.Comparator.comparingInt(Pos::x).thenComparingInt(Pos::y).thenComparingInt(Pos::z));
        java.util.Collections.shuffle(order, new Random(seed));
        List<Pos> out = new ArrayList<>();
        if (max <= 0) return out;
        if (square) {
            for (Pos p : order) {
                List<Pos> sq = List.of(p, p.add(1, 0, 0), p.add(0, 0, 1), p.add(1, 0, 1));
                if (valid.containsAll(sq)) return sq;
            }
            return out;
        }
        for (Pos p : order) {
            if (out.size() >= max) break;
            boolean near = false;
            for (Pos q : out) if (Math.abs(p.x() - q.x()) < SPACING && Math.abs(p.z() - q.z()) < SPACING) near = true;
            if (!near) out.add(p);
        }
        return out;
    }
}

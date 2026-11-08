package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Species;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where to replant, before checking the ground. Dark and pale oak need a 2x2 square of saplings or
 * nothing; spruce and jungle get four where their trunk was 2x2, or one if any of the four can't go in.
 * Everything else gets one, at the stump nearest the trunk's middle.
 */
public final class Replanter {
    private Replanter() {}

    /** Ways to replant, best first. Each is a set of spots that must all take a sapling. */
    public static List<List<Pos>> options(Species species, List<Pos> base) {
        List<List<Pos>> out = new ArrayList<>();
        List<Pos> square = square(base);
        if ((species.needsFour() || species.mayBeFour()) && square != null) out.add(square);
        if (species.needsFour()) return out;
        double cx = base.stream().mapToInt(Pos::x).average().orElse(0);
        double cz = base.stream().mapToInt(Pos::z).average().orElse(0);
        List<Pos> sorted = new ArrayList<>(base);
        sorted.sort(Comparator.comparingDouble(p -> Math.hypot(p.x() - cx, p.z() - cz)));
        for (Pos p : sorted) out.add(List.of(p));
        return out;
    }

    /** A 2x2 square inside the base, if there is one. */
    static List<Pos> square(List<Pos> base) {
        for (Pos p : base) {
            List<Pos> sq = List.of(p, p.add(1, 0, 0), p.add(0, 0, 1), p.add(1, 0, 1));
            if (base.containsAll(sq)) return sq;
        }
        return null;
    }
}

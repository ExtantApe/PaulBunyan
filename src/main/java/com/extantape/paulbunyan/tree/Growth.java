package com.extantape.paulbunyan.tree;

import com.extantape.paulbunyan.tree.Cell.Kind;
import com.extantape.paulbunyan.tree.Grower.Piece;
import com.extantape.paulbunyan.tree.Grower.Plan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The rules a wild tree has to pass before it grows from a stump. All or nothing: if any block it would need is
 * taken, in a claim, next to a build, not loaded or has someone standing in it, nothing is placed and the stump
 * waits. The rules never replace anything but air and a few small plants.
 */
public final class Growth {
    private Growth() {}

    /** What the rules need to know about the world. {@link Cell.Kind#UNKNOWN} means not loaded. */
    public interface Space {
        Cell cell(Pos p);

        /** A living thing, a boat or a minecart is standing in this block. */
        boolean occupied(Pos p);

        /** This block is in somebody's claim. */
        boolean claimed(Pos p);

        int minY();

        /** One above the highest block. */
        int maxY();
    }

    public enum Verdict {
        OK("it can grow"),
        STUMP_GONE("the stump isn't there any more"),
        NOT_LOADED("part of it isn't loaded"),
        OUT_OF_BOUNDS("it would go past the top or bottom of the world"),
        OCCUPIED("something is in the way"),
        ENTITY("someone is standing where it would grow"),
        CLAIMED("it's in a claim"),
        TOUCHES_BUILD("it would touch a build");

        public final String text;

        Verdict(String text) {
            this.text = text;
        }
    }

    /** {@code toPlace} lists the blocks to put in, logs first. Empty unless the verdict is OK. */
    public record Result(Verdict verdict, Pos at, List<Piece> toPlace) {
        public boolean ok() {
            return verdict == Verdict.OK;
        }
    }

    /** Small plants a trunk may grow through. Tall ones have a second half, so they count as in the way. */
    private static final Set<String> REPLACEABLE = Set.of("SHORT_GRASS", "FERN", "DEAD_BUSH", "SHORT_DRY_GRASS", "LEAF_LITTER", "SNOW");

    public static boolean replaceable(Cell c) {
        return c.kind() == Kind.NATURAL && REPLACEABLE.contains(c.name());
    }

    private static Result no(Verdict v, Pos at) {
        return new Result(v, at, List.of());
    }

    public static Result check(Species species, List<Pos> stumpTops, Plan plan, Space space) {
        // The stump has to be there, and its chunk loaded.
        for (Pos top : stumpTops) {
            Cell c = space.cell(top);
            if (c.kind() == Kind.UNKNOWN) return no(Verdict.NOT_LOADED, top);
            if (c.kind() != Kind.LOG || !c.name().equals(species.log)) return no(Verdict.STUMP_GONE, top);
        }
        for (Piece p : plan.all()) {
            if (p.at().y() < space.minY() || p.at().y() >= space.maxY()) return no(Verdict.OUT_OF_BOUNDS, p.at());
        }
        List<Piece> place = new ArrayList<>();
        for (Piece p : plan.all()) {
            Cell c = space.cell(p.at());
            if (c.kind() == Kind.UNKNOWN) return no(Verdict.NOT_LOADED, p.at());
            if (alreadyThere(p, c)) continue; // a replay after a crash: this block went in before
            boolean free = c.kind() == Kind.EMPTY || replaceable(c);
            if (!free) {
                // Leaves just don't appear where terrain or another tree is, but a build or a placed block is a refusal.
                boolean skippable = !p.isLog() && (c.kind() == Kind.NATURAL || c.kind() == Kind.LOG || (c.kind() == Kind.CAP && !c.persistent()));
                if (skippable) continue;
                return no(Verdict.OCCUPIED, p.at());
            }
            place.add(p);
        }
        // Nothing in a claim, and nobody inside a block that is about to become solid.
        for (Pos top : stumpTops) if (space.claimed(top)) return no(Verdict.CLAIMED, top);
        for (Piece p : place) {
            if (space.claimed(p.at())) return no(Verdict.CLAIMED, p.at());
            if (space.occupied(p.at())) return no(Verdict.ENTITY, p.at());
        }
        // Nothing new may touch a build: anything that isn't ground, a plant or another tree.
        Set<Pos> mine = new HashSet<>(stumpTops);
        for (Piece p : plan.all()) mine.add(p.at());
        for (Piece p : place) {
            for (Pos n : p.at().around()) {
                if (mine.contains(n)) continue;
                Cell c = space.cell(n);
                if (c.kind() == Kind.UNKNOWN) return no(Verdict.NOT_LOADED, n);
                if (c.kind() == Kind.BUILD || c.kind() == Kind.PLACED_LOG || (c.kind() == Kind.CAP && c.persistent()))
                    return no(Verdict.TOUCHES_BUILD, n);
            }
        }
        place.sort((a, b) -> Boolean.compare(b.isLog(), a.isLog())); // logs first; stable, so each group keeps its order
        return new Result(Verdict.OK, null, place);
    }

    private static boolean alreadyThere(Piece p, Cell c) {
        if (!c.name().equals(p.material())) return false;
        return p.isLog() ? c.kind() == Kind.LOG : c.kind() == Kind.CAP && !c.persistent();
    }
}

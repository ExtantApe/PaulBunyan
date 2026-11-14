package com.extantape.paulbunyan.tree;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Cell.Kind;
import com.extantape.paulbunyan.tree.Grower.Piece;
import com.extantape.paulbunyan.tree.Grower.Plan;
import com.extantape.paulbunyan.tree.Growth.Space;
import com.extantape.paulbunyan.tree.Growth.Verdict;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The rules a stump's tree passes before it grows: every refusal, a positive control, and the never-replace guarantee. */
class GrowthTest {
    static final Pos TOP = new Pos(0, 65, 0); // the stump's top log; the ground is dirt to y=64 in FakeWorld
    static final List<Pos> STUMP = List.of(TOP);

    /** A world with the stump standing in it. */
    static class Rig implements Space {
        final FakeWorld w = new FakeWorld();
        final Set<Pos> claimed = new HashSet<>(), occupied = new HashSet<>();
        { w.log("OAK_LOG", 0, 65, 0); w.log("OAK_LOG", 0, 64, 0); }
        @Override public Cell cell(Pos p) { return w.cell(p); }
        @Override public boolean occupied(Pos p) { return occupied.contains(p); }
        @Override public boolean claimed(Pos p) { return claimed.contains(p); }
        @Override public int minY() { return 0; }
        @Override public int maxY() { return 320; }
    }

    static final Plan PLAN = Grower.plan(Species.OAK, STUMP, 99L);

    static Growth.Result check(Rig r) {
        return Growth.check(Species.OAK, STUMP, PLAN, r);
    }

    @Test void growsInTheOpen() {
        Growth.Result r = check(new Rig());
        assertTrue(r.ok(), r.verdict().text);
        assertEquals(PLAN.all().size(), r.toPlace().size());
        assertTrue(r.toPlace().getFirst().isLog(), "logs go in first");
        boolean seenLeaf = false;
        for (Piece p : r.toPlace()) { if (!p.isLog()) seenLeaf = true; else assertFalse(seenLeaf, "no log after a leaf"); }
    }

    @Test void aBlockInTheTrunksWayRefusesTheWholeTree() {
        Rig r = new Rig();
        r.w.set(Kind.NATURAL, "STONE", 0, 66, 0);
        Growth.Result g = check(r);
        assertEquals(Verdict.OCCUPIED, g.verdict());
        assertEquals(new Pos(0, 66, 0), g.at());
        assertTrue(g.toPlace().isEmpty(), "nothing is placed");
    }

    @Test void aPlacedBuildBlockInTheWayRefuses() {
        Rig r = new Rig();
        r.w.set(Kind.BUILD, "OAK_PLANKS", 0, 66, 0);
        assertEquals(Verdict.OCCUPIED, check(r).verdict());
    }

    @Test void waterLavaAndFlowersAreNotReplaced() {
        for (String n : List.of("WATER", "LAVA", "DANDELION", "TALL_GRASS", "TORCH")) {
            Rig r = new Rig();
            r.w.set(n.equals("TORCH") ? Kind.BUILD : Kind.NATURAL, n, 0, 66, 0);
            assertEquals(Verdict.OCCUPIED, check(r).verdict(), n);
        }
    }

    @Test void smallPlantsAreReplaced() {
        Rig r = new Rig();
        r.w.set(Kind.NATURAL, "SHORT_GRASS", 0, 66, 0);
        Growth.Result g = check(r);
        assertTrue(g.ok());
        assertTrue(g.toPlace().stream().anyMatch(p -> p.at().equals(new Pos(0, 66, 0))));
    }

    @Test void aClaimRefuses() {
        Rig r = new Rig();
        Pos inside = PLAN.logs().get(PLAN.logs().size() - 1).at();
        r.claimed.add(inside);
        Growth.Result g = check(r);
        assertEquals(Verdict.CLAIMED, g.verdict());
        assertTrue(g.toPlace().isEmpty());
    }

    @Test void aClaimOverJustTheStumpRefuses() {
        Rig r = new Rig();
        r.claimed.add(TOP);
        assertEquals(Verdict.CLAIMED, check(r).verdict());
    }

    @Test void aBuildTouchingTheTreeRefuses() {
        Rig r = new Rig();
        Pos leaf = PLAN.leaves().getFirst().at();
        r.w.set(Kind.BUILD, "OAK_PLANKS", leaf.x() + 1, leaf.y() + 1, leaf.z());
        // the planks might themselves be on a planned block; put them beside the first log instead if so
        Growth.Result g = check(r);
        assertTrue(g.verdict() == Verdict.TOUCHES_BUILD || g.verdict() == Verdict.OCCUPIED, g.verdict().name());
        assertTrue(g.toPlace().isEmpty());
    }

    @Test void aBuildBesideTheTrunkRefusesAsTouching() {
        Rig r = new Rig();
        r.w.set(Kind.BUILD, "STONE_BRICKS", 1, 66, 0); // beside the first new log, not in the way
        Growth.Result g = Growth.check(Species.OAK, STUMP, PLAN, r);
        assertEquals(Verdict.TOUCHES_BUILD, g.verdict());
    }

    @Test void aPlacedLogOrPlacedLeafNextToItCountsAsABuild() {
        Rig r = new Rig();
        r.w.placedLog("OAK_LOG", 1, 66, 0);
        assertEquals(Verdict.TOUCHES_BUILD, check(r).verdict());
        Rig q = new Rig();
        q.w.leaf("OAK_LEAVES", true, 1, 66, 0);
        assertEquals(Verdict.TOUCHES_BUILD, check(q).verdict());
    }

    @Test void someoneStandingWhereItWouldGrowRefuses() {
        Rig r = new Rig();
        r.occupied.add(PLAN.logs().getFirst().at());
        assertEquals(Verdict.ENTITY, check(r).verdict());
    }

    @Test void anUnloadedChunkWaits() {
        Rig r = new Rig();
        Pos far = PLAN.leaves().getLast().at();
        r.w.set(Kind.UNKNOWN, "UNLOADED", far.x(), far.y(), far.z());
        assertEquals(Verdict.NOT_LOADED, check(r).verdict());
        Rig s = new Rig();
        s.w.set(Kind.UNKNOWN, "UNLOADED", 0, 65, 0);
        assertEquals(Verdict.NOT_LOADED, check(s).verdict());
    }

    @Test void aStumpThatIsGoneStaysGone() {
        Rig r = new Rig();
        r.w.cells.put(TOP, Cell.EMPTY);
        assertEquals(Verdict.STUMP_GONE, check(r).verdict());
        Rig q = new Rig();
        q.w.log("BIRCH_LOG", 0, 65, 0);
        assertEquals(Verdict.STUMP_GONE, check(q).verdict(), "a different tree isn't this stump");
    }

    @Test void outOfBoundsRefuses() {
        Rig r = new Rig() { @Override public int maxY() { return 70; } };
        assertEquals(Verdict.OUT_OF_BOUNDS, check(r).verdict());
    }

    @Test void leavesSkipTerrainAndOtherTreesButNeverReplaceThem() {
        Rig r = new Rig();
        // a hillside: terrain at a leaf's height, and another tree's leaves beside it
        Pos a = PLAN.leaves().get(0).at(), b = PLAN.leaves().get(1).at();
        r.w.set(Kind.NATURAL, "DIRT", a.x(), a.y(), a.z());
        r.w.leaf("OAK_LEAVES", false, b.x(), b.y(), b.z());
        Growth.Result g = check(r);
        assertTrue(g.ok(), g.verdict() + " at " + g.at());
        assertTrue(g.toPlace().stream().noneMatch(p -> p.at().equals(a) || p.at().equals(b)));
    }

    @Test void aReplayAfterACrashFinishesTheSameTreeWithoutDoublingBlocks() {
        Rig r = new Rig();
        Growth.Result first = check(r);
        // the first half went in before the crash
        int half = first.toPlace().size() / 2;
        for (Piece p : first.toPlace().subList(0, half)) {
            r.w.cells.put(p.at(), p.isLog() ? new Cell(Kind.LOG, p.material(), false) : new Cell(Kind.CAP, p.material(), false));
        }
        Growth.Result second = check(r);
        assertTrue(second.ok(), second.verdict().text);
        assertEquals(first.toPlace().size() - half, second.toPlace().size());
    }

    /** The guarantee itself: over many trees and worlds, whatever the verdict, nothing placed is on a taken block. */
    @Test void nothingPlacedIsEverOnATakenBlock() {
        java.util.Random random = new java.util.Random(5);
        String[] taken = {"STONE", "OAK_PLANKS", "WATER", "DANDELION", "CHEST", "LAVA"};
        int ok = 0, refused = 0;
        for (long seed = 0; seed < 300; seed++) {
            Rig r = new Rig();
            Plan plan = Grower.plan(Species.OAK, STUMP, seed);
            for (Piece p : plan.all()) if (random.nextInt(100) < 3) {
                String n = taken[random.nextInt(taken.length)];
                r.w.set(n.equals("OAK_PLANKS") || n.equals("CHEST") ? Kind.BUILD : Kind.NATURAL, n, p.at().x(), p.at().y(), p.at().z());
            }
            Growth.Result g = Growth.check(Species.OAK, STUMP, plan, r);
            if (!g.ok()) { refused++; assertTrue(g.toPlace().isEmpty()); continue; }
            ok++;
            for (Piece p : g.toPlace()) {
                Cell c = r.w.cell(p.at());
                assertTrue(c.kind() == Kind.EMPTY || Growth.replaceable(c), "would replace " + c.name() + " at " + p.at());
            }
        }
        assertTrue(ok > 0 && refused > 0, ok + " grew, " + refused + " refused");
    }
}

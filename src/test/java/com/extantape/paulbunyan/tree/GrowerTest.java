package com.extantape.paulbunyan.tree;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Grower.Piece;
import com.extantape.paulbunyan.tree.Grower.Plan;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The wild tree made for a stump: always the same for the same seed, always in bounds, never on the stump. */
class GrowerTest {
    static final Pos TOP = new Pos(10, 70, -4);
    static final List<Pos> ONE = List.of(TOP);
    static final List<Pos> FOUR = List.of(TOP, TOP.add(1, 0, 0), TOP.add(0, 0, 1), TOP.add(1, 0, 1));

    @Test void sameSeedSameTree() {
        for (Species s : Species.values()) {
            assertEquals(Grower.plan(s, ONE, 42L), Grower.plan(s, ONE, 42L), s.name());
            assertEquals(Grower.plan(s, FOUR, 7L), Grower.plan(s, FOUR, 7L), s.name() + " 2x2");
        }
    }

    @Test void differentSeedsGiveDifferentTrees() {
        Set<Plan> seen = new HashSet<>();
        for (long seed = 0; seed < 30; seed++) seen.add(Grower.plan(Species.OAK, ONE, seed));
        assertTrue(seen.size() >= 25, "only " + seen.size() + " different trees out of 30");
    }

    @Test void everySpeciesUsesItsOwnWoodAndAllowedLeaves() {
        for (Species s : Species.values()) {
            for (long seed = 0; seed < 20; seed++) {
                Plan p = Grower.plan(s, ONE, seed);
                for (Piece l : p.logs()) assertEquals(s.log, l.material());
                for (Piece l : p.leaves()) assertTrue(Grower.leafBlocks(s).contains(l.material()), s + " " + l.material());
            }
        }
    }

    @Test void neverPlacesOnTheStumpOrTwiceAndLeavesNeverOverlapLogs() {
        for (Species s : Species.values()) {
            for (long seed = 0; seed < 40; seed++) {
                for (List<Pos> tops : List.of(ONE, FOUR)) {
                    Plan p = Grower.plan(s, tops, seed);
                    Set<Pos> all = new HashSet<>(tops);
                    for (Piece x : p.all()) assertTrue(all.add(x.at()), s + " " + seed + " doubled or on the stump at " + x.at());
                }
            }
        }
    }

    @Test void staysInsideItsBounds() {
        for (Species s : Species.values()) {
            for (long seed = 0; seed < 60; seed++) {
                for (List<Pos> tops : List.of(ONE, FOUR)) {
                    Plan p = Grower.plan(s, tops, seed);
                    Pos lo = p.min(), hi = p.max();
                    assertTrue(lo.y() > TOP.y(), "nothing below the stump's top: " + s);
                    assertTrue(hi.y() - TOP.y() <= 20, s + " is " + (hi.y() - TOP.y()) + " tall");
                    assertTrue(TOP.x() - lo.x() <= 12 && hi.x() - TOP.x() <= 13, s + " is too wide in x");
                    assertTrue(TOP.z() - lo.z() <= 12 && hi.z() - TOP.z() <= 13, s + " is too wide in z");
                }
            }
        }
    }

    @Test void everyLogIsJoinedToTheStumpFaceToFace() {
        for (Species s : Species.values()) {
            for (long seed = 0; seed < 40; seed++) {
                Plan p = Grower.plan(s, ONE, seed);
                Set<Pos> logs = new HashSet<>();
                for (Piece l : p.logs()) logs.add(l.at());
                Set<Pos> reached = new HashSet<>(ONE);
                ArrayDeque<Pos> todo = new ArrayDeque<>(ONE);
                while (!todo.isEmpty()) for (Pos n : todo.poll().faces()) if (logs.contains(n) && reached.add(n)) todo.add(n);
                assertEquals(logs.size() + 1, reached.size(), s + " " + seed + " has a log floating free");
            }
        }
    }

    @Test void itLooksLikeATree() {
        for (Species s : Species.values()) {
            for (long seed = 0; seed < 20; seed++) {
                Plan p = Grower.plan(s, ONE, seed);
                assertTrue(p.logs().size() >= 6, s + " has " + p.logs().size() + " logs");
                assertTrue(p.leaves().size() >= 25, s + " has " + p.leaves().size() + " leaves");
                assertTrue(p.logs().stream().anyMatch(l -> l.axis() == 'x' || l.axis() == 'z'), "branches lie on their side");
            }
        }
    }

    @Test void trunksTwist() {
        int leaning = 0;
        for (long seed = 0; seed < 50; seed++) {
            Plan p = Grower.plan(Species.OAK, ONE, seed);
            if (p.logs().stream().filter(l -> l.axis() == 'y').anyMatch(l -> l.at().x() != TOP.x() || l.at().z() != TOP.z())) leaning++;
        }
        assertTrue(leaning > 25, "only " + leaning + " of 50 trunks leaned or had branches rising");
    }

    @Test void aTwoByTwoStumpGrowsATwoByTwoTrunk() {
        Plan p = Grower.plan(Species.DARK_OAK, FOUR, 3L);
        Set<Pos> logs = new HashSet<>();
        for (Piece l : p.logs()) logs.add(l.at());
        for (Pos c : FOUR) assertTrue(logs.contains(c.above()), "trunk column " + c + " continues");
    }

    @Test void mixesInASecondLeafWhereASpeciesHasOne() {
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < 40; seed++) for (Piece l : Grower.plan(Species.OAK, ONE, seed).leaves()) seen.add(l.material());
        assertEquals(Set.of("OAK_LEAVES", "AZALEA_LEAVES"), seen);
    }
}

package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The real-felling rules (docs/felling-plan.md): which way a tree falls, and what it does to someone underneath. */
class FellingRulesTest {
    static List<Pos> trunk(int x, int z, int from, int to) {
        List<Pos> out = new ArrayList<>();
        for (int y = from; y <= to; y++) out.add(new Pos(x, y, z));
        return out;
    }

    @Test void aStraightTreeFallsAwayFromTheCutter() {
        List<Pos> logs = trunk(0, 0, 64, 70);
        List<Pos> leaves = List.of(new Pos(1, 70, 0), new Pos(-1, 70, 0), new Pos(0, 70, 1), new Pos(0, 70, -1));
        assertEquals(Direction.NORTH, Lean.of(logs, leaves, List.of(new Pos(0, 64, 0)), Direction.NORTH));
    }

    @Test void aLeaningTreeFallsTowardItsHeavySide() {
        List<Pos> logs = new ArrayList<>(trunk(0, 0, 64, 68));
        // A branch and a crown out to the east.
        for (int x = 1; x <= 4; x++) logs.add(new Pos(x, 68, 0));
        List<Pos> leaves = new ArrayList<>();
        for (int x = 2; x <= 6; x++) for (int z = -2; z <= 2; z++) leaves.add(new Pos(x, 69, z));
        assertEquals(Direction.EAST, Lean.of(logs, leaves, List.of(new Pos(0, 64, 0)), Direction.WEST), "heavier east, whatever way the cutter faces");
    }

    @Test void atOffNobodyIsEverHurtOnlyThrown() {
        var small = Danger.hit(Danger.Level.OFF, 0, 0, 3);
        var big = Danger.hit(Danger.Level.OFF, 2, 2, 10);
        for (var h : List.of(small, big)) {
            assertEquals(0, h.damage());
            assertFalse(h.kills());
        }
        assertTrue(big.fling() > small.fling(), "a bigger hit throws harder");
        assertEquals(Danger.Hit.NONE, Danger.hit(Danger.Level.OFF, 0, 0, 0));
    }

    @Test void atMaxWoodHurtsAHeadHitHurtsALotAndTheTrunkOnTheBodyKills() {
        var leaves = Danger.hit(Danger.Level.MAX, 0, 0, 6);
        var feet = Danger.hit(Danger.Level.MAX, 1, 0, 0);
        var head = Danger.hit(Danger.Level.MAX, 0, 1, 0);
        var body = Danger.hit(Danger.Level.MAX, 1, 1, 0);
        assertTrue(leaves.damage() > 0 && leaves.damage() < feet.damage());
        assertTrue(head.damage() > feet.damage());
        assertFalse(head.kills());
        assertTrue(body.kills());
    }

    @Test void unknownLevelsAreRefused() {
        assertNull(Danger.Level.parse("hard"));
        assertEquals(Danger.Level.MAX, Danger.Level.parse(" MAX "));
    }
}

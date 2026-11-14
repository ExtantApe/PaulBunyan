package com.extantape.paulbunyan;

import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Species;
import com.extantape.paulbunyan.tree.Tree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Wherever you chop, everything below the cut stays standing as a stump, and everything from the cut up falls. */
class AboveCutTest {
    @Test void choppingHigherLeavesAStump() {
        List<Pos> logs = new ArrayList<>();
        for (int y = 64; y <= 70; y++) logs.add(new Pos(0, y, 0));
        List<Pos> leaves = List.of(new Pos(1, 66, 0), new Pos(1, 70, 0));
        Tree whole = new Tree(Species.OAK, logs, leaves, List.of(new Pos(0, 64, 0)), 64, 0);
        Tree falling = Feller.aboveCut(whole, 67);
        assertEquals(4, falling.logs().size());
        assertEquals(List.of(new Pos(0, 67, 0)), falling.base());
        assertEquals(67, falling.minY());
        assertEquals(List.of(new Pos(1, 70, 0)), falling.leaves());
        assertTrue(Feller.leavesStump(whole, 67));
    }

    @Test void choppingAtTheGroundFellsTheWholeTree() {
        List<Pos> logs = List.of(new Pos(0, 64, 0), new Pos(0, 65, 0));
        Tree whole = new Tree(Species.OAK, logs, List.of(), List.of(new Pos(0, 64, 0)), 64, 0);
        assertSame(whole, Feller.aboveCut(whole, 64));
        assertFalse(Feller.leavesStump(whole, 64));
    }
}

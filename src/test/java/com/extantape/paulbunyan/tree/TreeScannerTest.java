package com.extantape.paulbunyan.tree;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Cell.Kind;
import org.junit.jupiter.api.Test;

class TreeScannerTest {
    private static Scan scan(FakeWorld w, int x, int y, int z) {
        return new TreeScanner(w, TreeScanner.Limits.DEFAULT).scan(new Pos(x, y, z));
    }

    @Test
    void naturalOakFalls() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "OAK_LOG", "OAK_LEAVES", false);
        Scan s = scan(w, 0, 65, 0);
        assertTrue(s.fells(), () -> String.valueOf(s.skip()));
        assertEquals(5, s.tree().logs().size());
        assertEquals(Species.OAK, s.tree().species());
        assertEquals(2 * 24 + 9, s.tree().leaves().size());
        assertEquals(1, s.tree().base().size());
        assertEquals(65, s.tree().minY());
    }

    @Test
    void choppingTheTopFindsTheWholeTree() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "BIRCH_LOG", "BIRCH_LEAVES", false);
        assertEquals(5, scan(w, 0, 69, 0).tree().logs().size());
    }

    @Test
    void placedLeavesMeanABuild() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "OAK_LOG", "OAK_LEAVES", true);
        assertEquals(Scan.Reason.NO_NATURAL_LEAVES, scan(w, 0, 65, 0).skip());
    }

    @Test
    void logCabinWallNeverFalls() {
        FakeWorld w = new FakeWorld();
        for (int x = 0; x < 6; x++) for (int y = 65; y < 69; y++) w.log("SPRUCE_LOG", x, y, 0);
        assertEquals(Scan.Reason.NO_NATURAL_LEAVES, scan(w, 2, 65, 0).skip());
    }

    @Test
    void cabinWithPlanksIsNotATreeRatherThanATreeTouchingABuild() {
        FakeWorld w = new FakeWorld();
        for (int y = 65; y < 69; y++) w.log("SPRUCE_LOG", 0, y, 0).set(Kind.BUILD, "SPRUCE_PLANKS", 1, y, 0);
        Scan s = scan(w, 0, 65, 0);
        assertEquals(Scan.Reason.NO_NATURAL_LEAVES, s.skip(), "a cabin player gets no message; only real trees explain themselves");
    }

    @Test
    void treeTouchingABuildIsLeftAlone() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "OAK_LOG", "OAK_LEAVES", false);
        w.set(Kind.BUILD, "OAK_PLANKS", 1, 66, 0);
        Scan s = scan(w, 0, 65, 0);
        assertEquals(Scan.Reason.TOUCHES_BUILD, s.skip());
        assertEquals("oak_planks at 1,66,0", s.detail());
    }

    @Test
    void unloadedNeighbourCountsAsABuild() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "OAK_LOG", "OAK_LEAVES", false);
        w.set(Kind.UNKNOWN, "?", 0, 65, 1);
        assertEquals(Scan.Reason.TOUCHES_BUILD, scan(w, 0, 65, 0).skip());
    }

    @Test
    void strayPlacedLogStaysWithItsLeaves() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "OAK_LOG", "OAK_LEAVES", false);
        w.placedLog("OAK_LOG", 2, 68, 2); // replaces a leaf at a corner of the canopy
        w.placedLog("OAK_LOG", 1, 66, 0); // and one stuck to the trunk
        Scan s = scan(w, 0, 65, 0);
        assertTrue(s.fells());
        assertEquals(5, s.tree().logs().size());
        assertEquals(1, s.tree().strays());
        assertFalse(s.tree().logs().contains(new Pos(2, 68, 2)));
        assertFalse(s.tree().logs().contains(new Pos(1, 66, 0)));
        // The leaves right next to the stray are as near to it as to the trunk, so they stay.
        assertFalse(s.tree().leaves().contains(new Pos(2, 69, 2)));
        assertTrue(s.tree().leaves().contains(new Pos(1, 68, 0)));
    }

    @Test
    void manyPlacedLogsMeanABuild() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "OAK_LOG", "OAK_LEAVES", false);
        w.placedLog("OAK_LOG", 1, 65, 0).placedLog("OAK_LOG", -1, 65, 0).placedLog("OAK_LOG", 0, 65, 1);
        assertEquals(Scan.Reason.TOUCHES_BUILD, scan(w, 0, 65, 0).skip());
    }

    @Test
    void sharedCanopySplitsByDistance() {
        FakeWorld w = new FakeWorld().oak(0, 0, 5, "OAK_LOG", "OAK_LEAVES", false).oak(4, 0, 5, "OAK_LOG", "OAK_LEAVES", false);
        Scan s = scan(w, 0, 65, 0);
        assertTrue(s.fells());
        assertEquals(5, s.tree().logs().size());
        assertTrue(s.tree().leaves().contains(new Pos(1, 68, 0)));
        assertFalse(s.tree().leaves().contains(new Pos(2, 68, 0)), "equidistant leaves stay with the other tree");
        assertFalse(s.tree().leaves().contains(new Pos(3, 68, 0)));
        // and the other tree is untouched: none of its logs are in ours
        assertFalse(s.tree().logs().contains(new Pos(4, 65, 0)));
    }

    @Test
    void giantTwoByTwoAndBranches() {
        FakeWorld w = new FakeWorld();
        for (int y = 65; y < 85; y++) for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) w.log("SPRUCE_LOG", dx, y, dz);
        w.log("SPRUCE_LOG", 2, 80, 0).log("SPRUCE_LOG", 3, 81, 0); // a diagonal branch
        for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) w.leaf("SPRUCE_LEAVES", false, dx, 85, dz);
        Scan s = scan(w, 1, 65, 1);
        assertTrue(s.fells());
        assertEquals(82, s.tree().logs().size());
        assertEquals(4, s.tree().base().size());
    }

    @Test
    void sizeCap() {
        FakeWorld w = new FakeWorld();
        for (int y = 65; y < 65 + 300; y++) w.log("JUNGLE_LOG", 0, y, 0);
        assertEquals(Scan.Reason.TOO_BIG, scan(w, 0, 65, 0).skip());
    }

    @Test
    void playerPlacedLogIsNotATree() {
        FakeWorld w = new FakeWorld().placedLog("OAK_LOG", 0, 65, 0);
        assertEquals(Scan.Reason.NOT_A_TREE_LOG, scan(w, 0, 65, 0).skip());
    }

    @Test
    void fungusNeedsNyliumAndTakesItsCap() {
        FakeWorld w = new FakeWorld();
        for (int y = 65; y < 70; y++) w.log("CRIMSON_STEM", 0, y, 0);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) w.leaf("NETHER_WART_BLOCK", false, dx, 70, dz);
        w.leaf("SHROOMLIGHT", false, 1, 69, 0);
        assertEquals(Scan.Reason.NOT_ON_NYLIUM, scan(w, 0, 65, 0).skip());
        w.set(Kind.NATURAL, "CRIMSON_NYLIUM", 0, 64, 0);
        Scan s = scan(w, 0, 65, 0);
        assertTrue(s.fells());
        assertEquals(10, s.tree().leaves().size());
    }

    @Test
    void mangroveRootsAndMudAreNatural() {
        FakeWorld w = new FakeWorld().oak(0, 0, 6, "MANGROVE_LOG", "MANGROVE_LEAVES", false);
        w.set(Kind.NATURAL, "MANGROVE_ROOTS", 0, 64, 0).set(Kind.NATURAL, "MUDDY_MANGROVE_ROOTS", 1, 65, 0).set(Kind.NATURAL, "MOSS_CARPET", 0, 66, 1);
        assertTrue(scan(w, 0, 66, 0).fells());
    }
}

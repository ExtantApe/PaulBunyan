package com.extantape.paulbunyan.regrow;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Pos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoreTest {
    static Store sample() {
        Store s = new Store();
        s.stumps.add(new Store.Stump("world one", "OAK", -123456789012L, 99000L, false, List.of(new Pos(1, 64, -2))));
        s.stumps.add(new Store.Stump("world_nether", "DARK_OAK", 5L, 12L, true,
            List.of(new Pos(0, 70, 0), new Pos(1, 70, 0), new Pos(0, 70, 1), new Pos(1, 70, 1))));
        s.falls.add(new Store.Fall("world one", "BIRCH", 77L, 500L, "BIRCH_SAPLING", List.of(new Pos(3, 64, 3), new Pos(4, 64, 3))));
        s.saplings.add(new Store.Sapling("world one", new Pos(9, 63, 9), "OAK_SAPLING", 0));
        s.saplings.add(new Store.Sapling("world one", new Pos(-9, 63, 9), "AZALEA", 720000));
        s.grown.add(new Store.Grown("world one", new Pos(-3, 64, -3), new Pos(5, 80, 5)));
        return s;
    }

    @Test void roundTripsThroughText() {
        Store a = sample();
        Store b = Store.parse(a.toText().lines().toList());
        assertEquals(a.stumps, b.stumps);
        assertEquals(a.falls, b.falls);
        assertEquals(a.saplings, b.saplings);
        assertEquals(a.grown, b.grown);
        assertEquals(0, b.skipped);
    }

    @Test void savesAtomicallyAndLoadsBack(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("regrowth.txt");
        sample().save(f);
        assertTrue(Files.exists(f));
        assertFalse(Files.exists(dir.resolve("regrowth.txt.tmp")), "the temporary file is moved into place");
        Store b = Store.load(f);
        assertEquals(sample().stumps, b.stumps);
        // saving again replaces the file whole
        b.stumps.clear();
        b.save(f);
        assertTrue(Store.load(f).stumps.isEmpty());
        assertEquals(1, Store.load(f).falls.size());
    }

    @Test void aMissingFileIsAnEmptyList(@TempDir Path dir) throws Exception {
        assertTrue(Store.load(dir.resolve("nope.txt")).isEmpty());
    }

    @Test void badLinesAreSkippedAndCountedNotFatal() {
        Store s = Store.parse(List.of("# a comment", "", "stump w OAK 1 2 0 1,2,3", "stump broken", "stump w OAK x 2 0 1,2,3",
            "nonsense here", "sapling w 1,2,3 OAK_SAPLING 5", "fall w OAK 1 2 OAK_SAPLING "));
        assertEquals(1, s.stumps.size());
        assertEquals(1, s.saplings.size());
        assertTrue(s.skipped >= 3);
    }

    @Test void growthBoxesKnowWhatTheyContain() {
        Store.Grown g = new Store.Grown("w", new Pos(0, 0, 0), new Pos(2, 2, 2));
        assertTrue(g.contains(new Pos(1, 1, 1)));
        assertFalse(g.contains(new Pos(3, 1, 1)));
        Store s = new Store();
        s.grown.add(g);
        assertTrue(s.isGrown("w", new Pos(2, 2, 2)));
        assertFalse(s.isGrown("other", new Pos(2, 2, 2)));
    }
}

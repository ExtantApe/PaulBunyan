package com.extantape.paulbunyan.fall;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SproutsTest {
    static List<Pos> row(int n) {
        List<Pos> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new Pos(i, 64, 0));
        return out;
    }

    @Test void aFewSpreadOutAndNeverMoreThanTheMax() {
        List<Pos> spots = row(20);
        List<Pos> got = Sprouts.choose(spots, new HashSet<>(spots), 3, 1L, false);
        assertTrue(got.size() <= 3 && !got.isEmpty());
        for (Pos a : got) for (Pos b : got) if (a != b) assertTrue(Math.abs(a.x() - b.x()) >= Sprouts.SPACING);
        assertTrue(spots.containsAll(got));
    }

    @Test void sameSeedSameSpots() {
        List<Pos> spots = row(30);
        assertEquals(Sprouts.choose(spots, new HashSet<>(spots), 3, 9L, false), Sprouts.choose(spots, new HashSet<>(spots), 3, 9L, false));
        assertNotEquals(Sprouts.choose(spots, new HashSet<>(spots), 3, 9L, false), Sprouts.choose(spots, new HashSet<>(spots), 3, 10L, false));
    }

    @Test void zeroMaxOrNoSpotsGivesNothing() {
        assertTrue(Sprouts.choose(row(5), new HashSet<>(row(5)), 0, 1L, false).isEmpty());
        assertTrue(Sprouts.choose(List.of(), Set.of(), 3, 1L, false).isEmpty());
    }

    @Test void aFourSaplingSpeciesGetsOneFullSquareOrNothing() {
        List<Pos> spots = new ArrayList<>(row(10)); // a single row: no square fits
        assertTrue(Sprouts.choose(spots, new HashSet<>(spots), 3, 1L, true).isEmpty());
        for (int x = 0; x < 3; x++) spots.add(new Pos(x, 64, 1));
        List<Pos> got = Sprouts.choose(spots, new HashSet<>(spots), 3, 1L, true);
        assertEquals(4, got.size());
        Pos p = got.getFirst();
        assertTrue(got.containsAll(List.of(p, p.add(1, 0, 0), p.add(0, 0, 1), p.add(1, 0, 1))));
    }
}

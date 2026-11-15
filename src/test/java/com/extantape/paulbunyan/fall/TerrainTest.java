package com.extantape.paulbunyan.fall;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TerrainTest {
    /** A one-block trunk at x=0, z=0, from y=65 up, falling east. Ground is solid at y<=64 unless set otherwise. */
    static List<Pos> trunk(int h) {
        List<Pos> out = new ArrayList<>();
        for (int y = 65; y < 65 + h; y++) out.add(new Pos(0, y, 0));
        return out;
    }

    static FallPlan east() { return new FallPlan(Direction.EAST, 65, List.of(new Pos(0, 65, 0))); }

    @Test
    void aQuarterTurnIsTheFlatTip() {
        FallPlan plan = east();
        for (Pos p : trunk(8)) assertEquals(plan.tipped(p), plan.tippedAt(p, Math.PI / 2), p.toString());
        for (Direction d : Direction.values()) {
            FallPlan pl = new FallPlan(d, 65, List.of(new Pos(0, 65, 0)));
            for (Pos p : trunk(8)) assertEquals(pl.tipped(p), pl.tippedAt(p, Math.PI / 2), d + " " + p);
        }
    }

    @Test
    void noTurnLeavesItStanding() {
        for (Pos p : trunk(6)) assertEquals(p, east().tippedAt(p, 0));
    }

    @Test
    void onFlatGroundItStrikesAtFlat() {
        Set<Pos> self = new HashSet<>(trunk(8));
        int hit = Terrain.contact(p -> p.y() <= 64, east(), trunk(8).subList(1, 8), self, Terrain.MAX_DEGREES);
        assertTrue(hit >= 90 && hit < 96, "hit at " + hit);
    }

    @Test
    void uphillItStrikesBeforeFlat() {
        // The ground rises one block for every two east, about 27 degrees.
        Terrain.Solid slope = p -> p.y() <= 64 + Math.max(0, p.x()) / 2;
        Set<Pos> self = new HashSet<>(trunk(8));
        int hit = Terrain.contact(slope, east(), trunk(8).subList(1, 8), self, Terrain.MAX_DEGREES);
        assertTrue(hit >= Terrain.SHORTEST && hit < Terrain.UPHILL_BELOW, "hit at " + hit);
    }

    @Test
    void overADropItTipsPastFlat() {
        // A cliff starting one block east: twenty blocks down.
        Terrain.Solid cliff = p -> p.x() >= 1 ? p.y() <= 44 : p.y() <= 64;
        Set<Pos> self = new HashSet<>(trunk(8));
        int hit = Terrain.contact(cliff, east(), trunk(8).subList(1, 8), self, Terrain.MAX_DEGREES);
        assertTrue(hit > 90, "hit at " + hit);
    }

    @Test
    void grassAndFlowersDontStopIt() {
        // Solid only means solid: the test world's "solid" ignores a layer of plants above the ground.
        Set<Pos> plants = new HashSet<>();
        for (int x = 0; x <= 10; x++) plants.add(new Pos(x, 65, 0));
        Terrain.Solid g = p -> p.y() <= 64;
        int hit = Terrain.contact(g, east(), trunk(8).subList(1, 8), new HashSet<>(trunk(8)), Terrain.MAX_DEGREES);
        assertTrue(hit >= 90, "hit at " + hit);
    }

    @Test
    void theTreeItselfIsIgnored() {
        // Its own leaves around the trunk mustn't count as something it strikes.
        Set<Pos> self = new HashSet<>(trunk(8));
        for (int y = 70; y < 73; y++) for (int dx = -2; dx <= 2; dx++) self.add(new Pos(dx, y, 0));
        Terrain.Solid g = p -> p.y() <= 64 || self.contains(p);
        int hit = Terrain.contact(g, east(), trunk(8).subList(1, 8), self, Terrain.MAX_DEGREES);
        assertTrue(hit >= 90, "hit at " + hit);
    }

    @Test
    void aWallRightBesideItIsStruckAtOnce() {
        Terrain.Solid wall = p -> p.y() <= 64 || p.x() == 2;
        int hit = Terrain.contact(wall, east(), trunk(8).subList(1, 8), new HashSet<>(trunk(8)), Terrain.MAX_DEGREES);
        assertTrue(hit < Terrain.SHORTEST, "hit at " + hit + ": leaning on a wall lands flat, crushed into it");
    }
}

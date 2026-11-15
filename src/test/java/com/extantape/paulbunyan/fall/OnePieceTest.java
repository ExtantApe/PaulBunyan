package com.extantape.paulbunyan.fall;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OnePieceTest {
    /** A trunk lying east along x from 1 to n, at y. */
    static List<Pos> trunk(int n, int y) {
        List<Pos> out = new ArrayList<>();
        for (int x = 1; x <= n; x++) out.add(new Pos(x, y, 0));
        return out;
    }

    @Test
    void onFlatGroundItLandsWhereItTipped() {
        LandingTest.G g = new LandingTest.G();
        Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, trunk(6, 65), true, 1);
        for (Pos p : trunk(6, 65)) assertEquals(new OnePiece.Rest(p, 0, true), r.get(p));
    }

    @Test
    void itDropsTogetherUntilItStrikesSomething() {
        LandingTest.G g = new LandingTest.G();
        // Tipped two blocks above flat ground (a stump on a rise): the whole trunk drops two, together.
        Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, trunk(5, 67), true, 1);
        for (Pos p : trunk(5, 67)) assertEquals(new OnePiece.Rest(p.add(0, -2, 0), 2, true), r.get(p));
    }

    @Test
    void acrossTwoBouldersItLiesWholeLikeABridge() {
        LandingTest.G g = new LandingTest.G();
        g.groundY = 60;
        for (int y = 61; y <= 64; y++) { g.blocks.put(new Pos(1, y, 0), "STONE"); g.blocks.put(new Pos(6, y, 0), "STONE"); }
        Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, trunk(6, 70), true, 1);
        for (Pos p : trunk(6, 70)) assertEquals(new OnePiece.Rest(p.add(0, -5, 0), 5, true), r.get(p), p.toString());
    }

    @Test
    void onOneBoulderTheEndsBreakOffPastTheOverhang() {
        LandingTest.G g = new LandingTest.G();
        g.groundY = 60;
        for (int y = 61; y <= 64; y++) g.blocks.put(new Pos(5, y, 0), "STONE");
        Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, trunk(10, 66), true, 1);
        for (int x = 3; x <= 7; x++) assertEquals(new OnePiece.Rest(new Pos(x, 65, 0), 1, true), r.get(new Pos(x, 66, 0)), "x=" + x);
        for (int x : new int[] {1, 2, 8, 9, 10}) assertEquals(61, r.get(new Pos(x, 66, 0)).at().y(), "x=" + x);
    }

    @Test
    void overALedgeTwoLogsStickOutAndTheRestBreaksOff() {
        LandingTest.G g = new LandingTest.G();
        g.groundY = 55;
        for (int x = -5; x <= 3; x++) for (int y = 56; y <= 64; y++) g.blocks.put(new Pos(x, y, 0), "STONE");
        Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, trunk(10, 65), true, 7);
        for (int x = 1; x <= 5; x++) assertEquals(new OnePiece.Rest(new Pos(x, 65, 0), 0, true), r.get(new Pos(x, 65, 0)), "x=" + x);
        for (int x = 6; x <= 10; x++) {
            OnePiece.Rest rest = r.get(new Pos(x, 65, 0));
            assertEquals(new Pos(x, 56, 0), rest.at(), "x=" + x);
            assertTrue(rest.held(), "its section rests on the ground below");
            assertEquals(9, rest.dropped(), "the drop counts toward long-drop rules");
        }
    }

    @Test
    void sectionsAreTwoToFiveLogs() {
        Random random = new Random(3);
        for (int n = 2; n <= 40; n++) {
            List<List<Pos>> s = OnePiece.sections(trunk(n, 65), true, random);
            int total = 0;
            for (List<Pos> section : s) {
                total += section.size();
                assertTrue(section.size() <= OnePiece.MAX_SECTION, "n=" + n);
            }
            assertEquals(n, total);
            // Only a lone log that couldn't join a full section is ever left on its own.
            for (int i = 0; i < s.size(); i++)
                if (s.get(i).size() < OnePiece.MIN_SECTION) {
                    assertEquals(s.size() - 1, i);
                    assertEquals(OnePiece.MAX_SECTION, s.get(i - 1).size());
                }
        }
    }

    @Test
    void theSameFallBreaksTheSameWay() {
        LandingTest.G g = new LandingTest.G();
        g.groundY = 40;
        for (int x = -5; x <= 2; x++) for (int y = 41; y <= 64; y++) g.blocks.put(new Pos(x, y, 0), "STONE");
        // A step part way down, so sections land differently depending on where the breaks are.
        for (int x = 3; x <= 30; x += 3) for (int y = 41; y <= 50; y++) g.blocks.put(new Pos(x, y, 0), "STONE");
        assertEquals(OnePiece.settle(g, trunk(25, 65), true, 42), OnePiece.settle(g, trunk(25, 65), true, 42));
    }

    @Test
    void noTwoLogsEndUpInTheSameBlockAndNoneInTheGround() {
        LandingTest.G g = new LandingTest.G();
        g.groundY = 40;
        for (int x = -5; x <= 2; x++) for (int y = 41; y <= 64; y++) g.blocks.put(new Pos(x, y, 0), "STONE");
        for (int x = 3; x <= 30; x += 4) for (int y = 41; y <= 47; y++) g.blocks.put(new Pos(x, y, 0), "STONE");
        // A branchy crown: a second row above the trunk's far end, and one log off to the side.
        List<Pos> tree = new ArrayList<>(trunk(20, 65));
        for (int x = 14; x <= 20; x++) tree.add(new Pos(x, 66, 0));
        tree.add(new Pos(18, 65, 1));
        for (long seed = 0; seed < 50; seed++) {
            Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, tree, true, seed);
            assertEquals(tree.size(), r.size());
            Set<Pos> seen = new HashSet<>();
            for (OnePiece.Rest rest : r.values()) {
                assertTrue(seen.add(rest.at()), "two logs at " + rest.at() + ", seed " + seed);
                assertTrue(g.isAir(rest.at()), "a log inside the ground at " + rest.at());
            }
        }
    }

    @Test
    void aLogAlreadyInTheGroundHoldsThePieceWhereItIs() {
        LandingTest.G g = new LandingTest.G();
        // Uphill: the trunk's far end tipped into a hill. Landing crushes those logs; this only must not move them.
        for (int x = 5; x <= 8; x++) for (int y = 65; y <= 66; y++) g.blocks.put(new Pos(x, y, 0), "STONE");
        Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, trunk(8, 65), true, 1);
        for (Pos p : trunk(8, 65)) assertEquals(new OnePiece.Rest(p, 0, true), r.get(p));
    }

    @Test
    void overTheVoidItsLeftToLandingLogByLog() {
        LandingTest.G g = new LandingTest.G();
        g.groundY = -100; // below the world's floor: air all the way down
        Map<Pos, OnePiece.Rest> r = OnePiece.settle(g, trunk(4, 65), true, 1);
        for (Pos p : trunk(4, 65)) assertEquals(new OnePiece.Rest(p, 0, false), r.get(p));
    }

    @Test
    void aHeldLogStaysOverAirButStillFollowsTheRules() {
        LandingTest.G g = new LandingTest.G();
        Pos air = new Pos(1, 70, 0);
        assertEquals(new Landing.Result(Landing.Outcome.BLOCK, air), Landing.held(g, air, false, Landing.Drops.DEFAULT, 0));
        assertEquals(Landing.Outcome.ITEM, Landing.held(g, air, true, Landing.Drops.DEFAULT, 0).outcome());
        assertEquals(Landing.Outcome.ITEM, Landing.held(g, air, false, Landing.Drops.DEFAULT, 5).outcome());
        g.mobs.add(air);
        assertEquals(Landing.Outcome.ITEM, Landing.held(g, air, false, Landing.Drops.DEFAULT, 0).outcome(), "never a block inside a player");
        g.denied.add(air);
        assertEquals(Landing.Outcome.TO_PLAYER, Landing.held(g, air, false, Landing.Drops.DEFAULT, 0).outcome(), "never in a claim the player can't build in");
        assertEquals(Landing.Outcome.TO_PLAYER, Landing.held(g, new Pos(1, 60, 0), false, Landing.Drops.DEFAULT, 0).outcome(), "never inside the ground");
    }

    @Test
    void theDropCountsInLanding() {
        LandingTest.G g = new LandingTest.G();
        // Resting on the ground after a piece drop of 5, past items-after 4: it breaks into an item.
        assertEquals(Landing.Outcome.ITEM, Landing.find(g, new Pos(1, 65, 0), 65, false, Landing.Drops.DEFAULT, 5).outcome());
        assertEquals(Landing.Outcome.SPLINTERED, Landing.find(g, new Pos(1, 65, 0), 65, false, Landing.Drops.DEFAULT, 13).outcome());
        assertEquals(Landing.Outcome.BLOCK, Landing.find(g, new Pos(1, 65, 0), 65, false, Landing.Drops.DEFAULT, 3).outcome());
    }
}

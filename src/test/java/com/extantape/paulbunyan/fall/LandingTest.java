package com.extantape.paulbunyan.fall;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.fall.Landing.Outcome;
import com.extantape.paulbunyan.tree.Pos;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LandingTest {
    /** Ground at y=64 everywhere unless a column says otherwise. */
    static final class G implements Landing.Ground {
        final Map<Pos, String> blocks = new HashMap<>();
        final Set<Pos> denied = new HashSet<>();
        final Set<Pos> mobs = new HashSet<>();
        int groundY = 64;

        String at(Pos p) {
            return blocks.getOrDefault(p, p.y() <= groundY ? "STONE" : "AIR");
        }

        public boolean isAir(Pos p) { return at(p).equals("AIR"); }
        public boolean sturdyTop(Pos p) { return Set.of("STONE", "OAK_LOG").contains(at(p)); }
        public boolean destroysItems(Pos p) { return Set.of("LAVA", "FIRE").contains(at(p)); }
        public int worldMin() { return -64; }
        public boolean occupied(Pos p) { return mobs.contains(p); }
        public boolean mayBuild(Pos p) { return !denied.contains(p); }
        public boolean isLog(Pos p) { return at(p).endsWith("_LOG"); }
    }

    @Test
    void flatGround() {
        G g = new G();
        assertEquals(new Landing.Result(Outcome.BLOCK, new Pos(3, 65, 0)), Landing.find(g, new Pos(3, 65, 0), 65, false));
        assertEquals(new Landing.Result(Outcome.ITEM, new Pos(3, 65, 0)), Landing.find(g, new Pos(3, 65, 0), 65, true));
    }

    @Test
    void neverReplacesTorchesFlowersOrWater() {
        G g = new G();
        g.blocks.put(new Pos(3, 65, 0), "TORCH");
        assertEquals(new Landing.Result(Outcome.CRUSHED, new Pos(3, 66, 0)), Landing.find(g, new Pos(3, 65, 0), 65, false));
        g.blocks.put(new Pos(4, 65, 0), "WATER");
        assertEquals(Outcome.CRUSHED, Landing.find(g, new Pos(4, 65, 0), 65, false).outcome());
    }

    @Test
    void lavaVoidAndClaimsGoToThePlayer() {
        G g = new G();
        g.blocks.put(new Pos(3, 64, 0), "LAVA");
        assertEquals(Outcome.TO_PLAYER, Landing.find(g, new Pos(3, 65, 0), 65, false).outcome());
        g.groundY = -1000;
        assertEquals(Outcome.TO_PLAYER, Landing.find(g, new Pos(5, 65, 0), 65, false).outcome());
        g.groundY = 64;
        g.denied.add(new Pos(6, 65, 0));
        assertEquals(Outcome.TO_PLAYER, Landing.find(g, new Pos(6, 65, 0), 65, true).outcome());
    }

    @Test
    void stacksAndClimbsAndFallsOffCliffs() {
        G g = new G();
        g.blocks.put(new Pos(3, 65, 0), "OAK_LOG");
        assertEquals(new Pos(3, 66, 0), Landing.find(g, new Pos(3, 65, 0), 65, false).at());
        for (int y = 65; y < 68; y++) g.blocks.put(new Pos(4, y, 0), "STONE");
        assertEquals(new Pos(4, 68, 0), Landing.find(g, new Pos(4, 60, 0), 65, false).at());
        for (int y = 40; y <= 64; y++) g.blocks.put(new Pos(5, y, 0), "AIR");
        assertEquals(new Pos(5, 40, 0), Landing.find(g, new Pos(5, 65, 0), 65, false).at());
    }

    @Test
    void neverInsideAPlayer() {
        G g = new G();
        g.mobs.add(new Pos(3, 65, 0));
        assertEquals(Outcome.ITEM, Landing.find(g, new Pos(3, 65, 0), 65, false).outcome());
    }

    @Test
    void buriedTooDeepGoesToThePlayer() {
        G g = new G();
        g.groundY = 100;
        assertEquals(Outcome.TO_PLAYER, Landing.find(g, new Pos(3, 65, 0), 65, false).outcome());
    }

    // --- real felling: crushed parts and long drops (felling-plan.md) ---

    @Test
    void aLogOnLogsThatLandedFirstStacksWhole() {
        G g = new G();
        g.blocks.put(new Pos(3, 65, 0), "OAK_LOG");
        assertEquals(new Landing.Result(Outcome.BLOCK, new Pos(3, 66, 0)), Landing.find(g, new Pos(3, 65, 0), 65, false, Landing.Drops.DEFAULT));
    }

    @Test
    void aLogThatEndsUpInsideTheGroundIsCrushedToAnItemOnTop() {
        G g = new G();
        for (int y = 65; y < 68; y++) g.blocks.put(new Pos(4, y, 0), "STONE");
        assertEquals(new Landing.Result(Outcome.CRUSHED, new Pos(4, 68, 0)), Landing.find(g, new Pos(4, 66, 0), 65, false, Landing.Drops.DEFAULT));
    }

    @Test
    void shortDropsLandWholeLongerOnesBreakAndLongOnesSplinter() {
        G g = new G();
        var d = new Landing.Drops(4, 12);
        // ground at 64, so a log at y lands at 65 after dropping y-65 blocks
        assertEquals(Outcome.BLOCK, Landing.find(g, new Pos(0, 69, 0), 65, false, d).outcome(), "4 blocks: whole");
        assertEquals(Outcome.ITEM, Landing.find(g, new Pos(1, 70, 0), 65, false, d).outcome(), "5 blocks: a log item");
        assertEquals(Outcome.ITEM, Landing.find(g, new Pos(2, 77, 0), 65, false, d).outcome(), "12 blocks: still a log item");
        assertEquals(Outcome.SPLINTERED, Landing.find(g, new Pos(3, 78, 0), 65, false, d).outcome(), "13 blocks: sticks");
    }

    @Test
    void overAnEdgeTheDropCountsFromTheLip() {
        G g = new G();
        for (int y = 40; y <= 64; y++) g.blocks.put(new Pos(5, y, 0), "AIR");
        var r = Landing.find(g, new Pos(5, 65, 0), 65, false, Landing.Drops.DEFAULT);
        assertEquals(new Landing.Result(Outcome.SPLINTERED, new Pos(5, 40, 0)), r, "25 blocks down a hole");
    }

    @Test
    void withoutRealFellingAnyDropLandsWhole() {
        G g = new G();
        for (int y = 40; y <= 64; y++) g.blocks.put(new Pos(5, y, 0), "AIR");
        assertEquals(Outcome.BLOCK, Landing.find(g, new Pos(5, 65, 0), 65, false).outcome());
    }

    @Test
    void lavaAndClaimsStillWinOverBreaking() {
        G g = new G();
        g.blocks.put(new Pos(3, 64, 0), "LAVA");
        assertEquals(Outcome.TO_PLAYER, Landing.find(g, new Pos(3, 90, 0), 65, false, Landing.Drops.DEFAULT).outcome());
        g.denied.add(new Pos(6, 65, 0));
        assertEquals(Outcome.TO_PLAYER, Landing.find(g, new Pos(6, 66, 0), 65, false, Landing.Drops.DEFAULT).outcome());
    }
}

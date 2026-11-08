package com.extantape.paulbunyan.fall;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Pos;
import java.util.List;
import org.junit.jupiter.api.Test;

class FallPlanTest {
    @Test
    void trunkLiesAheadOfTheStump() {
        FallPlan plan = new FallPlan(Direction.EAST, 65, List.of(new Pos(0, 65, 0)));
        assertEquals(new Pos(1, 65, 0), plan.tipped(new Pos(0, 65, 0)));
        assertEquals(new Pos(5, 65, 0), plan.tipped(new Pos(0, 69, 0)));
        plan = new FallPlan(Direction.NORTH, 65, List.of(new Pos(0, 65, 0)));
        assertEquals(new Pos(0, 65, -5), plan.tipped(new Pos(0, 69, 0)));
    }

    @Test
    void twoByTwoStacksTwoHigh() {
        List<Pos> base = List.of(new Pos(0, 65, 0), new Pos(1, 65, 0), new Pos(0, 65, 1), new Pos(1, 65, 1));
        FallPlan plan = new FallPlan(Direction.EAST, 65, base);
        assertEquals(new Pos(2, 65, 0), plan.tipped(new Pos(1, 65, 0)));
        assertEquals(new Pos(2, 66, 0), plan.tipped(new Pos(0, 65, 0)));
        plan = new FallPlan(Direction.WEST, 65, base);
        assertEquals(new Pos(-1, 65, 0), plan.tipped(new Pos(0, 65, 0)));
        assertEquals(new Pos(-1, 66, 0), plan.tipped(new Pos(1, 65, 0)));
    }

    @Test
    void axesTurn() {
        assertEquals(Axis3.X, Axis3.Y.tipped(Direction.EAST));
        assertEquals(Axis3.Y, Axis3.X.tipped(Direction.WEST));
        assertEquals(Axis3.Z, Axis3.Z.tipped(Direction.EAST));
        assertEquals(Axis3.Z, Axis3.Y.tipped(Direction.NORTH));
    }

    @Test
    void facing() {
        assertEquals(Direction.SOUTH, Direction.facing(0));
        assertEquals(Direction.WEST, Direction.facing(90));
        assertEquals(Direction.NORTH, Direction.facing(-180));
        assertEquals(Direction.EAST, Direction.facing(-90));
        assertEquals(Direction.EAST, Direction.facing(260));
    }
}

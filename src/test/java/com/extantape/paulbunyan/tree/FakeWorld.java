package com.extantape.paulbunyan.tree;

import com.extantape.paulbunyan.tree.Cell.Kind;
import java.util.HashMap;
import java.util.Map;

/** A made-up world for tests: everything is air above y=64 and dirt at or below it, unless set. */
public final class FakeWorld implements WorldView {
    public final Map<Pos, Cell> cells = new HashMap<>();

    @Override
    public Cell cell(Pos p) {
        Cell c = cells.get(p);
        if (c != null) return c;
        return p.y() <= 64 ? new Cell(Kind.NATURAL, "DIRT", false) : Cell.EMPTY;
    }

    public FakeWorld log(String name, int x, int y, int z) {
        cells.put(new Pos(x, y, z), new Cell(Kind.LOG, name, false));
        return this;
    }

    public FakeWorld placedLog(String name, int x, int y, int z) {
        cells.put(new Pos(x, y, z), new Cell(Kind.PLACED_LOG, name, false));
        return this;
    }

    public FakeWorld leaf(String name, boolean persistent, int x, int y, int z) {
        cells.put(new Pos(x, y, z), new Cell(Kind.CAP, name, persistent));
        return this;
    }

    public FakeWorld set(Kind kind, String name, int x, int y, int z) {
        cells.put(new Pos(x, y, z), new Cell(kind, name, false));
        return this;
    }

    /** A plain tree: a trunk of {@code height} logs from y=65 and a 5x5 layer plus a 3x3 cap of leaves. */
    public FakeWorld oak(int x, int z, int height, String log, String leaves, boolean persistent) {
        for (int y = 65; y < 65 + height; y++) log(log, x, y, z);
        int top = 65 + height - 1;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                for (int y = top - 1; y <= top; y++)
                    if (!(dx == 0 && dz == 0)) leaf(leaves, persistent, x + dx, y, z + dz);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) leaf(leaves, persistent, x + dx, top + 1, z + dz);
        return this;
    }
}

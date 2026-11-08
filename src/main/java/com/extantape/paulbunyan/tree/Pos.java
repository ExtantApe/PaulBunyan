package com.extantape.paulbunyan.tree;

/** A block position. */
public record Pos(int x, int y, int z) {
    public Pos add(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }

    public Pos below() {
        return add(0, -1, 0);
    }

    public Pos above() {
        return add(0, 1, 0);
    }

    /** The six face neighbours. */
    public Pos[] faces() {
        return new Pos[] {add(1, 0, 0), add(-1, 0, 0), add(0, 1, 0), add(0, -1, 0), add(0, 0, 1), add(0, 0, -1)};
    }

    /** The 26 neighbours that share a face, an edge or a corner. */
    public Pos[] around() {
        Pos[] out = new Pos[26];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dy != 0 || dz != 0) out[i++] = add(dx, dy, dz);
        return out;
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}

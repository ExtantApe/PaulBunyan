package com.extantape.paulbunyan.fall;

/** The four ways a tree can fall. */
public enum Direction {
    EAST(1, 0), WEST(-1, 0), SOUTH(0, 1), NORTH(0, -1);

    public final int dx;
    public final int dz;

    Direction(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public boolean alongX() {
        return dx != 0;
    }

    public int sign() {
        return dx + dz;
    }

    /** The way someone facing {@code yaw} degrees is looking, in Minecraft's yaw (0 is south). */
    public static Direction facing(float yaw) {
        int q = Math.floorMod(Math.round(yaw / 90f), 4);
        return switch (q) {
            case 0 -> SOUTH;
            case 1 -> WEST;
            case 2 -> NORTH;
            default -> EAST;
        };
    }
}

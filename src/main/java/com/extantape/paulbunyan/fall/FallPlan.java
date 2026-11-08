package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;

/**
 * Where each block of a tree ends up once it has tipped 90 degrees over in one direction, before
 * gravity and obstacles are taken into account. The tree pivots on the front edge of its base, so the
 * trunk ends up lying on the ground in front of the stump, starting one block ahead of it.
 */
public final class FallPlan {
    private final Direction dir;
    private final int minY;
    /** The base's frontmost coordinate along the fall: its largest x for EAST, smallest for WEST. */
    private final int front;

    public FallPlan(Direction dir, int minY, Iterable<Pos> base) {
        this.dir = dir;
        this.minY = minY;
        int f = dir.sign() > 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Pos p : base) {
            int a = dir.alongX() ? p.x() : p.z();
            f = dir.sign() > 0 ? Math.max(f, a) : Math.min(f, a);
        }
        this.front = f;
    }

    public Direction direction() {
        return dir;
    }

    public int minY() {
        return minY;
    }

    /** The coordinate of the pivot line along the fall, in block units (the front face of the base). */
    public double pivotAlong() {
        return dir.sign() > 0 ? front + 1 : front;
    }

    /** Where {@code p} is once the tree lies flat. */
    public Pos tipped(Pos p) {
        int along = dir.alongX() ? p.x() : p.z();
        int ahead = (along - front) * dir.sign(); // 0 for the front of the base, negative behind it
        int height = p.y() - minY;
        int newAlong = front + dir.sign() * (height + 1);
        int newY = minY - ahead;
        return dir.alongX() ? new Pos(newAlong, newY, p.z()) : new Pos(p.x(), newY, newAlong);
    }
}

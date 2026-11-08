package com.extantape.paulbunyan.fall;

/** A log's axis, without Bukkit, so the plan can be tested. */
public enum Axis3 {
    X, Y, Z;

    /** The axis a log has after tipping 90 degrees over in {@code dir}. */
    public Axis3 tipped(Direction dir) {
        Axis3 along = dir.alongX() ? X : Z;
        if (this == Y) return along;
        if (this == along) return Y;
        return this;
    }
}

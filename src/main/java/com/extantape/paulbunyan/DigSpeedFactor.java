package com.extantape.paulbunyan;

import com.extantape.paulbunyan.tools.AxeKind;

/** How fast a log breaks, as a multiple of normal. */
public final class DigSpeedFactor {
    private DigSpeedFactor() {}

    /** Fallen or not, and whether the axe is a special one of the log's own wood. */
    public static double of(boolean isFallen, double fallenSpeed, boolean ownWood) {
        return (isFallen ? fallenSpeed : 1) * (ownWood ? AxeKind.OWN_WOOD_SPEED : 1);
    }
}

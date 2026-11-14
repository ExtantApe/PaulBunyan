package com.extantape.paulbunyan.fall;

import java.util.Locale;

/**
 * What a falling tree does to someone in its way, set per world like difficulty. At {@code off}, the default, nobody is
 * ever hurt: they're knocked back or flung clear, harder the bigger the hit. At {@code max}, wood hurts, a log on the
 * head hurts a lot, and the trunk landing on someone kills them.
 */
public final class Danger {
    public enum Level {
        OFF, MAX;

        public static Level parse(String s) {
            if (s == null) return null;
            return switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "off", "false" -> OFF; // YAML reads an unquoted off as false
                case "max" -> MAX;
                default -> null;
            };
        }
    }

    /** What happens to one player: how hard they're thrown, how much health they lose, and whether it kills. */
    public record Hit(double fling, double damage, boolean kills) {
        public static final Hit NONE = new Hit(0, 0, false);
    }

    private Danger() {}

    /**
     * @param logsOnFeet logs landing in the block the player's feet are in
     * @param logsOnHead logs landing in the block the player's head is in
     * @param leaves     leaves landing in either block
     */
    public static Hit hit(Level level, int logsOnFeet, int logsOnHead, int leaves) {
        int logs = logsOnFeet + logsOnHead;
        if (logs == 0 && leaves == 0) return Hit.NONE;
        double fling = Math.min(2.0, 0.5 + 0.35 * logs + 0.05 * leaves);
        if (level == Level.OFF) return new Hit(fling, 0, false);
        if (logsOnFeet > 0 && logsOnHead > 0) return new Hit(fling, 0, true);   // the trunk lands on the whole body
        if (logsOnHead > 0) return new Hit(Math.max(fling, 1.4), 12, false);    // a log on the head
        if (logs > 0) return new Hit(fling, 4.0 * logs, false);                 // struck by falling wood
        return new Hit(fling, Math.min(4, Math.max(1, leaves / 3)), false);     // only leaves
    }

    /** Knockback for standing right by the trunk as it goes, at any level. */
    public static final double NEAR_TRUNK_FLING = 0.6;
}

package com.extantape.paulbunyan.hook;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SittingLogsTest {
    @Test void onlyLogsOnTheirSideCanBeSatOn() {
        assertTrue(SittingLogs.allowed(Material.OAK_LOG, Axis.X));
        assertTrue(SittingLogs.allowed(Material.SPRUCE_LOG, Axis.Z));
        assertTrue(SittingLogs.allowed(Material.CRIMSON_STEM, Axis.X));
        assertFalse(SittingLogs.allowed(Material.OAK_LOG, Axis.Y), "an upright log");
        assertFalse(SittingLogs.allowed(Material.OAK_STAIRS, null), "stairs");
        assertFalse(SittingLogs.allowed(Material.OAK_PLANKS, null));
    }
}

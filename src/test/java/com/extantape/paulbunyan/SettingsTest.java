package com.extantape.paulbunyan;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SettingsTest {
    static Settings read(String yaml) throws Exception {
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString(yaml);
        return Settings.read(c, Logger.getAnonymousLogger());
    }

    @Test void regrowthDefaultsToOnWithGentleNumbers() throws Exception {
        Settings.Regrowth r = read("").regrowth();
        assertTrue(r.stumps() && r.saplings() && r.rewildLink());
        assertEquals(3, r.stumpDays());
        assertEquals(3, r.saplingMax());
        assertEquals(30, r.saplingDays());
        assertTrue(r.disabledWorlds().isEmpty());
    }

    @Test void everythingCanBeSwitchedOffAndPerWorld() throws Exception {
        Settings.Regrowth r = read("regrowth:\n  stumps: false\n  saplings: false\n  rewild-link: false\n").regrowth();
        assertFalse(r.stumpsIn("world") || r.saplingsIn("world") || r.rewildLink());
        Settings.Regrowth w = read("regrowth:\n  disabled-worlds: [kids]\n").regrowth();
        assertFalse(w.stumpsIn("kids"));
        assertFalse(w.saplingsIn("kids"));
        assertTrue(w.stumpsIn("main") && w.saplingsIn("main"));
    }

    @Test void sillyNumbersAreClamped() throws Exception {
        Settings.Regrowth r = read("regrowth:\n  stump-days: -4\n  sapling-days: 100000\n  sapling-max: 99\n").regrowth();
        assertEquals(1, r.stumpDays());
        assertEquals(365, r.saplingDays());
        assertEquals(8, r.saplingMax());
    }
}

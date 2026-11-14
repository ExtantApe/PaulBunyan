package com.extantape.paulbunyan;

import com.extantape.paulbunyan.tree.TreeScanner;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/** The settings in config.yml, read once and checked. */
public record Settings(
    boolean landAsItems,
    TreeScanner.Limits limits,
    boolean replant,
    boolean netherFungi,
    boolean creativeFells,
    int fallTicks,
    int maxAnimated,
    Set<String> disabledWorlds,
    int warningTicks,
    com.extantape.paulbunyan.fall.Danger.Level fallDanger,
    java.util.Map<String, com.extantape.paulbunyan.fall.Danger.Level> fallDangerWorlds) {

    /** How dangerous a fall is in this world. */
    public com.extantape.paulbunyan.fall.Danger.Level danger(String world) {
        return fallDangerWorlds.getOrDefault(world, fallDanger);
    }

    public static Settings read(FileConfiguration c, Logger log) {
        String land = c.getString("land-as", "blocks").toLowerCase(Locale.ROOT);
        if (!land.equals("blocks") && !land.equals("items")) {
            log.warning("land-as must be blocks or items, not '" + land + "'. Using blocks.");
            land = "blocks";
        }
        int maxLogs = clamp(c.getInt("max-logs", 250), 1, 2000, "max-logs", log);
        int maxLeaves = clamp(c.getInt("max-leaves", 3000), 0, 20000, "max-leaves", log);
        int fallTicks = clamp(c.getInt("fall-ticks", 24), 0, 200, "fall-ticks", log);
        int maxAnimated = clamp(c.getInt("max-animated-blocks", 600), 0, 5000, "max-animated-blocks", log);
        Set<String> disabled = new HashSet<>(c.getStringList("disabled-worlds"));
        int warning = clamp(c.getInt("warning-ticks", 20), 0, 60, "warning-ticks", log);
        var level = com.extantape.paulbunyan.fall.Danger.Level.parse(c.getString("fall-danger", "off"));
        if (level == null) {
            log.warning("fall-danger must be off or max, not '" + c.getString("fall-danger") + "'. Using off, so nobody is hurt.");
            level = com.extantape.paulbunyan.fall.Danger.Level.OFF;
        }
        java.util.Map<String, com.extantape.paulbunyan.fall.Danger.Level> perWorld = new java.util.HashMap<>();
        var sec = c.getConfigurationSection("fall-danger-worlds");
        if (sec != null) for (String w : sec.getKeys(false)) {
            var l = com.extantape.paulbunyan.fall.Danger.Level.parse(sec.getString(w));
            if (l == null) log.warning("fall-danger-worlds." + w + " must be off or max. Using off there.");
            perWorld.put(w, l == null ? com.extantape.paulbunyan.fall.Danger.Level.OFF : l);
        }
        return new Settings(land.equals("items"),
            new TreeScanner.Limits(maxLogs, maxLeaves, TreeScanner.Limits.DEFAULT.minLeaves(), TreeScanner.Limits.DEFAULT.maxStrays()),
            c.getBoolean("replant", true), c.getBoolean("nether-fungi", true), c.getBoolean("creative-fells", false),
            fallTicks, maxAnimated, disabled, warning, level, perWorld);
    }

    private static int clamp(int v, int min, int max, String key, Logger log) {
        if (v >= min && v <= max) return v;
        int to = Math.max(min, Math.min(max, v));
        log.warning(key + " must be between " + min + " and " + max + ". Using " + to + ".");
        return to;
    }
}

package com.extantape.paulbunyan.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Asks GriefPrevention, when it's installed, whether a player may break or build at a block. It is
 * reached by reflection so the plugin runs without it. If GriefPrevention is there but can't be
 * asked, or the player is offline, the answer is no.
 */
public final class Protection {
    private final Object gp;
    private final Method allowBreak;
    private final Method allowBuild;
    private final Object dataStore;
    private final Method claimAt;
    private final boolean present;

    public Protection(Logger log) {
        this(Bukkit.getPluginManager().getPlugin("GriefPrevention"), log);
    }

    /** With the GriefPrevention plugin found, or null if it isn't installed. Package-private for tests. */
    Protection(Plugin p, Logger log) {
        Object instance = null;
        Method br = null;
        Method bu = null;
        Object store = null;
        Method at = null;
        if (p != null && p.isEnabled()) {
            try {
                Class<?> c = p.getClass();
                Field f = c.getField("instance");
                instance = f.get(null);
                br = c.getMethod("allowBreak", Player.class, Block.class, Location.class);
                bu = c.getMethod("allowBuild", Player.class, Location.class);
                store = c.getField("dataStore").get(instance);
                for (Method m : store.getClass().getMethods())
                    if (m.getName().equals("getClaimAt") && m.getParameterCount() == 3 && m.getParameterTypes()[0] == Location.class) at = m;
                if (at == null) throw new NoSuchMethodException("DataStore.getClaimAt(Location, boolean, Claim)");
            } catch (ReflectiveOperationException | RuntimeException e) {
                log.warning("GriefPrevention is installed but its claim check couldn't be found ("
                    + e + "). Trees won't fall at all until this is fixed.");
                instance = null;
            }
        }
        this.present = p != null;
        this.gp = instance;
        this.allowBreak = br;
        this.allowBuild = bu;
        this.dataStore = instance == null ? null : store;
        this.claimAt = instance == null ? null : at;
        if (gp != null) log.info("Checking GriefPrevention claims before felling.");
    }

    public boolean present() {
        return present;
    }

    public boolean mayBreak(Player player, Block b) {
        if (!present) return true;
        if (gp == null || player == null) return false;
        try {
            return allowBreak.invoke(gp, player, b, b.getLocation()) == null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Is this spot in anyone's claim, at any height? Used where no player is involved, so nothing grows into a claim.
     * If GriefPrevention is there but can't be asked, the answer is yes.
     */
    public boolean claimed(Location at) {
        if (!present) return false;
        if (gp == null || dataStore == null || claimAt == null) return true;
        try {
            return claimAt.invoke(dataStore, at, true, null) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    public boolean mayBuild(UUID uuid, Block b) {
        if (!present) return true;
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
        if (gp == null || player == null) return false;
        try {
            return allowBuild.invoke(gp, player, b.getLocation()) == null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}

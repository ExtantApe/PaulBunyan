package com.extantape.paulbunyan.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

/**
 * Asks GriefPrevention, when it's installed, whether a player may break or build at a block. It is
 * reached by reflection so the plugin runs without it. If GriefPrevention is there but can't be
 * asked, or the player is offline, the answer is no.
 * <p>Then it asks the plugins named in {@code ask-plugins} (Waycave, for its gates), which have no claim API: each of
 * their own break or place listeners gets a test event for the block, and a cancelled event is a no. Only those
 * plugins' listeners see the test events, so logging, skills and other plugins never count them as real. A listener
 * that fails is a no too.
 */
public final class Protection {
    private final Object gp;
    private final Method allowBreak;
    private final Method allowBuild;
    private final Object dataStore;
    private final Method claimAt;
    private final boolean present;
    /** Lowercased names of the other plugins to ask. */
    private final Set<String> askPlugins = new HashSet<>();
    private final Logger log;
    private final Set<String> announced = new HashSet<>();

    public Protection(Logger log, List<String> askPlugins) {
        this(Bukkit.getPluginManager().getPlugin("GriefPrevention"), log, askPlugins);
    }

    /** With the GriefPrevention plugin found, or null if it isn't installed. Package-private for tests. */
    Protection(Plugin p, Logger log) {
        this(p, log, List.of());
    }

    Protection(Plugin p, Logger log, List<String> askPlugins) {
        this.log = log;
        for (String name : askPlugins) if (name != null && !name.isBlank()) this.askPlugins.add(name.trim().toLowerCase(Locale.ROOT));
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
        if (present) {
            if (gp == null || player == null) return false;
            try {
                if (allowBreak.invoke(gp, player, b, b.getLocation()) != null) return false;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }
        if (askPlugins.isEmpty()) return true;
        List<RegisteredListener> asked = listeners(BlockBreakEvent.getHandlerList());
        return asked.isEmpty() || player != null && allowed(asked, new BlockBreakEvent(b, player));
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
        if (!present && askPlugins.isEmpty()) return true;
        return mayBuildAs(uuid == null ? null : Bukkit.getPlayer(uuid), b);
    }

    /** May this player put a block here (where a falling log lands)? */
    boolean mayBuildAs(Player player, Block b) {
        if (present) {
            if (gp == null || player == null) return false;
            try {
                if (allowBuild.invoke(gp, player, b.getLocation()) != null) return false;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }
        if (askPlugins.isEmpty()) return true;
        List<RegisteredListener> asked = listeners(BlockPlaceEvent.getHandlerList());
        if (asked.isEmpty()) return true;
        if (player == null) return false;
        BlockPlaceEvent test;
        try {
            var inventory = player.getInventory();
            test = new BlockPlaceEvent(b, b.getState(), b.getRelative(BlockFace.DOWN), inventory == null ? null : inventory.getItemInMainHand(),
                player, true, EquipmentSlot.HAND);
        } catch (RuntimeException e) {
            return false;
        }
        return allowed(asked, test);
    }

    /** The named plugins' own listeners for this event, read each time so a plugin enabled later is still asked. */
    private List<RegisteredListener> listeners(HandlerList handlers) {
        List<RegisteredListener> out = new ArrayList<>();
        for (RegisteredListener l : handlers.getRegisteredListeners()) {
            Plugin owner = l.getPlugin();
            if (owner == null || !owner.isEnabled() || !askPlugins.contains(owner.getName().toLowerCase(Locale.ROOT))) continue;
            out.add(l);
            if (announced.add(owner.getName())) log.info("Asking " + owner.getName() + " before felling, too.");
        }
        return out;
    }

    /** Hands the test event to each listener in turn. Cancelled, or a listener that fails, is a no. */
    private static <E extends Event & Cancellable> boolean allowed(List<RegisteredListener> asked, E test) {
        for (RegisteredListener l : asked) {
            try {
                l.callEvent(test);
            } catch (Exception | LinkageError e) {
                return false;
            }
            if (test.isCancelled()) return false;
        }
        return true;
    }
}

package com.extantape.paulbunyan.hook;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

/**
 * Records removed and placed blocks in CoreProtect, when it's installed, under the player's name, so a
 * felled tree can be looked up and rolled back like hand-chopped logs. Reached by reflection.
 */
public final class BlockLog {
    private final Object api;
    private final Method removal;
    private final Method placement;

    public BlockLog(Logger log) {
        Object a = null;
        Method r = null;
        Method p = null;
        Plugin cp = Bukkit.getPluginManager().getPlugin("CoreProtect");
        if (cp != null && cp.isEnabled()) {
            try {
                a = cp.getClass().getMethod("getAPI").invoke(cp);
                if (!(Boolean) a.getClass().getMethod("isEnabled").invoke(a)) a = null;
                else {
                    r = a.getClass().getMethod("logRemoval", String.class, Location.class, Material.class, BlockData.class);
                    p = a.getClass().getMethod("logPlacement", String.class, Location.class, Material.class, BlockData.class);
                    log.info("Recording felled trees in CoreProtect.");
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                log.warning("CoreProtect is installed but couldn't be reached (" + e + "). Felled trees won't show in it.");
                a = null;
            }
        }
        this.api = a;
        this.removal = r;
        this.placement = p;
    }

    public void removed(String who, Location at, BlockData data) {
        call(removal, who, at, data);
    }

    public void placed(String who, Location at, BlockData data) {
        call(placement, who, at, data);
    }

    private void call(Method m, String who, Location at, BlockData data) {
        if (api == null) return;
        try {
            m.invoke(api, who, at, data.getMaterial(), data);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A logging failure never stops a fall.
        }
    }
}

package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** The head perks that act when a tree falls: emerald, lapis and glowstone. */
public final class Perks {
    /** How long glowstone's glow lasts on landed logs, in ticks. */
    public static final int GLOW_TICKS = 600;
    /** At most this many landed logs glow per fall. */
    static final int GLOW_MAX = 64;
    private final Plugin plugin;
    private final List<BlockDisplay> glowing = new ArrayList<>();

    public Perks(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Emerald: saplings, propagules and apples from the leaves come twice over. */
    public static boolean doubles(String material) {
        return material.endsWith("_SAPLING") || material.equals("MANGROVE_PROPAGULE") || material.equals("APPLE");
    }

    public static List<ItemStack> moreFruit(Collection<ItemStack> drops) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack i : drops) {
            out.add(i);
            if (doubles(i.getType().name())) out.add(i.clone());
        }
        return out;
    }

    /** Lapis: experience for felling a tree of this many logs, about one point for every three. */
    public static int xp(int logs) {
        return Math.max(1, logs / 3);
    }

    public void experience(World world, Pos at, int logs) {
        world.spawn(new Location(world, at.x() + 0.5, at.y() + 0.5, at.z() + 0.5), ExperienceOrb.class, o -> o.setExperience(xp(logs)));
    }

    /** Glowstone: an outline round each landed log for half a minute, seen through walls, so none get lost. */
    public void glow(World world, Collection<Pos> landed) {
        int n = 0;
        for (Pos p : landed) {
            if (++n > GLOW_MAX) break;
            var data = world.getBlockAt(p.x(), p.y(), p.z()).getBlockData();
            BlockDisplay d = world.spawn(new Location(world, p.x(), p.y(), p.z()), BlockDisplay.class, e -> {
                e.setBlock(data);
                e.setPersistent(false);
                e.setGlowing(true);
                e.setGlowColorOverride(Color.fromRGB(0xFFD86B));
                // A hair larger than the block, so the display's faces don't flicker against it.
                e.setTransformation(new Transformation(new Vector3f(-0.005f), new AxisAngle4f(), new Vector3f(1.01f), new AxisAngle4f()));
            });
            glowing.add(d);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                d.remove();
                glowing.remove(d);
            }, GLOW_TICKS);
        }
    }

    /** Takes every glow away. Called when the plugin stops. */
    public void clear() {
        for (BlockDisplay d : glowing) d.remove();
        glowing.clear();
    }
}

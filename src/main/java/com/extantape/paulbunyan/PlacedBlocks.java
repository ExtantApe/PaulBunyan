package com.extantape.paulbunyan;

import java.util.Arrays;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Remembers logs and fungus caps that players place, in the chunk's own data, so they're saved with
 * the world and travel with it. Placed leaves don't need this: the game marks them itself.
 * Blocks placed before the plugin was installed, or by commands, aren't known.
 */
public final class PlacedBlocks {
    private final NamespacedKey key;

    public PlacedBlocks(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "placed");
    }

    private static int pack(Block b) {
        return ((b.getY() + 4096) << 8) | ((b.getX() & 15) << 4) | (b.getZ() & 15);
    }

    private int[] read(Chunk chunk) {
        int[] a = chunk.getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        return a == null ? new int[0] : a;
    }

    public boolean isPlaced(Block b) {
        int[] a = read(b.getChunk());
        int p = pack(b);
        for (int v : a) if (v == p) return true;
        return false;
    }

    public void mark(Block b) {
        int[] a = read(b.getChunk());
        int p = pack(b);
        for (int v : a) if (v == p) return;
        int[] n = Arrays.copyOf(a, a.length + 1);
        n[a.length] = p;
        b.getChunk().getPersistentDataContainer().set(key, PersistentDataType.INTEGER_ARRAY, n);
    }

    public void clear(Block b) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        int[] a = read(b.getChunk());
        if (a.length == 0) return;
        int p = pack(b);
        int[] n = Arrays.stream(a).filter(v -> v != p).toArray();
        if (n.length == a.length) return;
        if (n.length == 0) pdc.remove(key);
        else pdc.set(key, PersistentDataType.INTEGER_ARRAY, n);
    }
}

package com.extantape.paulbunyan;

import java.util.Arrays;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * A set of marked blocks kept in each chunk's own data, so it's saved with the world and travels with it. One integer
 * per block: its height and its place within the chunk.
 */
public class ChunkMarks {
    private final NamespacedKey key;

    protected ChunkMarks(Plugin plugin, String name) {
        this.key = new NamespacedKey(plugin, name);
    }

    static int pack(int x, int y, int z) {
        return ((y + 4096) << 8) | ((x & 15) << 4) | (z & 15);
    }

    private static int pack(Block b) {
        return pack(b.getX(), b.getY(), b.getZ());
    }

    private int[] read(Chunk chunk) {
        int[] a = chunk.getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        return a == null ? new int[0] : a;
    }

    public boolean has(Block b) {
        int p = pack(b);
        for (int v : read(b.getChunk())) if (v == p) return true;
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
        int[] a = read(b.getChunk());
        if (a.length == 0) return;
        int p = pack(b);
        int[] n = Arrays.stream(a).filter(v -> v != p).toArray();
        if (n.length == a.length) return;
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        if (n.length == 0) pdc.remove(key);
        else pdc.set(key, PersistentDataType.INTEGER_ARRAY, n);
    }
}

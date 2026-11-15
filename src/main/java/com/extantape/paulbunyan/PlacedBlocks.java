package com.extantape.paulbunyan;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

/**
 * Remembers logs and fungus caps that players place, in the chunk's own data. Placed leaves don't need this: the game
 * marks them itself. Blocks placed before the plugin was installed, or by commands, aren't known.
 */
public final class PlacedBlocks extends ChunkMarks {
    public PlacedBlocks(Plugin plugin) {
        super(plugin, "placed");
    }

    public boolean isPlaced(Block b) {
        return has(b);
    }
}

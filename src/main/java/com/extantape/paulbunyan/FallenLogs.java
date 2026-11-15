package com.extantape.paulbunyan;

import org.bukkit.plugin.Plugin;

/**
 * Logs a fall left lying as blocks. They're joined: any tool breaks them one log at a time, slower than a loose log
 * (felling-plan.md, "Bucking and tools"), and they never fall again. Kept in the chunk's own data, so it outlasts the
 * fall journal and a restart. A fallen log a player picks up and places again is an ordinary placed log.
 */
public final class FallenLogs extends ChunkMarks {
    public FallenLogs(Plugin plugin) {
        super(plugin, "fallen");
    }
}

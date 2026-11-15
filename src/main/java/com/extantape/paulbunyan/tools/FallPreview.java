package com.extantape.paulbunyan.tools;

import com.extantape.paulbunyan.Feller;
import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Species;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Amethyst: while a player holds an amethyst axe and looks at a tree that would fall, a line of purple dust, seen
 * only by them, shows where it would land. Checked twice a second; the tree is scanned again at most once a second.
 */
public final class FallPreview implements Runnable {
    public static final long PERIOD = 10;
    private static final Particle.DustOptions DUST = new Particle.DustOptions(Color.fromRGB(0xB98CF0), 1.2f);
    private final Feller feller;
    private final Map<UUID, Cached> cache = new HashMap<>();

    private record Cached(Block log, long at, Feller.Preview preview) {}

    public FallPreview(Feller feller) {
        this.feller = feller;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            AxeKind kind = feller.axeKind(p.getInventory().getItemInMainHand());
            if (kind == null || kind.head() != Head.AMETHYST || p.isSneaking()) {
                cache.remove(p.getUniqueId());
                continue;
            }
            Block b = p.getTargetBlockExact(5);
            if (b == null || Species.ofLog(b.getType().name()) == null) continue;
            Cached c = cache.get(p.getUniqueId());
            if (c == null || !c.log().equals(b) || now - c.at() > 1000) {
                c = new Cached(b, now, feller.preview(p, b));
                cache.put(p.getUniqueId(), c);
            }
            if (c.preview() != null) show(p, b.getY(), c.preview());
        }
        cache.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    private static void show(Player p, int y, Feller.Preview v) {
        double cx = 0, cz = 0;
        for (Pos b : v.base()) {
            cx += b.x() + 0.5;
            cz += b.z() + 0.5;
        }
        cx /= v.base().size();
        cz /= v.base().size();
        for (int i = 1; i <= v.length(); i++)
            p.spawnParticle(Particle.DUST, cx + v.dir().dx * i, y + 0.15, cz + v.dir().dz * i, 2, 0.15, 0.05, 0.15, 0, DUST);
    }
}

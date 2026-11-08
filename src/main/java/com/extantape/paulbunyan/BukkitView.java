package com.extantape.paulbunyan;

import com.extantape.paulbunyan.fall.Landing;
import com.extantape.paulbunyan.hook.Protection;
import com.extantape.paulbunyan.tree.Cell;
import com.extantape.paulbunyan.tree.Cell.Kind;
import com.extantape.paulbunyan.tree.Palette;
import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Species;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.BoundingBox;

/** The scanner's and the landing's view of a real world. Never loads a chunk while scanning. */
public final class BukkitView implements com.extantape.paulbunyan.tree.WorldView, Landing.Ground {
    private final World world;
    private final PlacedBlocks placed;
    private final Protection protection;
    private final UUID player;

    public BukkitView(World world, PlacedBlocks placed, Protection protection, UUID player) {
        this.world = world;
        this.placed = placed;
        this.protection = protection;
        this.player = player;
    }

    public Block block(Pos p) {
        return world.getBlockAt(p.x(), p.y(), p.z());
    }

    @Override
    public Cell cell(Pos p) {
        if (p.y() >= world.getMaxHeight()) return Cell.EMPTY;
        if (p.y() < world.getMinHeight()) return new Cell(Kind.NATURAL, "BEDROCK", false);
        if (!world.isChunkLoaded(p.x() >> 4, p.z() >> 4)) return new Cell(Kind.UNKNOWN, "UNLOADED", false);
        Block b = block(p);
        Material m = b.getType();
        String name = m.name();
        if (m.isAir()) return new Cell(Kind.EMPTY, name, false);
        if (Species.ofLog(name) != null) return new Cell(placed.isPlaced(b) ? Kind.PLACED_LOG : Kind.LOG, name, false);
        if (Palette.isLeaves(m)) {
            BlockData d = b.getBlockData();
            return new Cell(Kind.CAP, name, d instanceof Leaves l && l.isPersistent());
        }
        if (m == Material.NETHER_WART_BLOCK || m == Material.WARPED_WART_BLOCK || m == Material.SHROOMLIGHT) {
            return new Cell(Kind.CAP, name, placed.isPlaced(b));
        }
        return new Cell(Palette.isNaturalNeighbour(m) ? Kind.NATURAL : Kind.BUILD, name, false);
    }

    @Override
    public boolean isAir(Pos p) {
        if (p.y() < world.getMinHeight()) return true;
        if (p.y() >= world.getMaxHeight()) return true;
        return block(p).getType().isAir();
    }

    @Override
    public boolean sturdyTop(Pos p) {
        if (p.y() < world.getMinHeight()) return false;
        return block(p).getBlockData().isFaceSturdy(BlockFace.UP, BlockSupport.FULL);
    }

    @Override
    public boolean destroysItems(Pos p) {
        return p.y() >= world.getMinHeight() && Palette.destroysItems(block(p).getType());
    }

    @Override
    public int worldMin() {
        return world.getMinHeight();
    }

    @Override
    public boolean occupied(Pos p) {
        BoundingBox box = new BoundingBox(p.x(), p.y(), p.z(), p.x() + 1, p.y() + 1, p.z() + 1);
        for (Entity e : world.getNearbyEntities(box)) {
            if (e instanceof LivingEntity || e instanceof Vehicle) return true;
        }
        return false;
    }

    @Override
    public boolean mayBuild(Pos p) {
        return protection.mayBuild(player, block(p));
    }
}

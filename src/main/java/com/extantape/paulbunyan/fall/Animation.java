package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The tree tipping over, drawn with block displays that pivot on the front edge of the stump. The
 * displays are never saved with the world, so a crash or a chunk unload can't leave one behind.
 * Nothing here touches real blocks: the landing does that once the animation ends.
 */
public final class Animation {
    /** Ticks between keyframes. The client interpolates in between. */
    static final int STEP = 3;

    private record Piece(BlockDisplay display, Vector3f offset) {}

    private final FallPlan plan;
    private final int ticks;
    private final List<Piece> pieces = new ArrayList<>();

    public Animation(FallPlan plan, int ticks) {
        this.plan = plan;
        this.ticks = ticks;
    }

    public void add(World world, Pos p, BlockData data) {
        Direction d = plan.direction();
        double pivot = plan.pivotAlong();
        Location at = d.alongX() ? new Location(world, pivot, plan.minY(), p.z()) : new Location(world, p.x(), plan.minY(), pivot);
        Vector3f offset = d.alongX()
            ? new Vector3f((float) (p.x() - pivot), p.y() - plan.minY(), 0)
            : new Vector3f(0, p.y() - plan.minY(), (float) (p.z() - pivot));
        BlockDisplay display = world.spawn(at, BlockDisplay.class, e -> {
            e.setPersistent(false);
            e.setBlock(data);
            e.setTransformation(transform(offset, 0));
        });
        pieces.add(new Piece(display, offset));
    }

    public int ticks() {
        return ticks;
    }

    /** Called every tick from 1 to {@link #ticks()}; sends the next keyframe when one is due. */
    public void tick(int t) {
        if ((t - 1) % STEP != 0) return;
        int to = Math.min(ticks, t - 1 + STEP);
        float progress = (float) to / ticks;
        float angle = (float) (Math.PI / 2 * progress * progress); // speeds up as it goes, like a real fall
        for (Piece p : pieces) {
            if (!p.display().isValid()) continue;
            p.display().setInterpolationDelay(0);
            p.display().setInterpolationDuration(to - (t - 1));
            p.display().setTransformation(transform(p.offset(), angle));
        }
    }

    public void remove() {
        for (Piece p : pieces) if (p.display().isValid()) p.display().remove();
        pieces.clear();
    }

    private Transformation transform(Vector3f offset, float angle) {
        Quaternionf r = switch (plan.direction()) {
            case EAST -> new Quaternionf().rotateZ(-angle);
            case WEST -> new Quaternionf().rotateZ(angle);
            case SOUTH -> new Quaternionf().rotateX(angle);
            case NORTH -> new Quaternionf().rotateX(-angle);
        };
        Vector3f t = r.transform(new Vector3f(offset));
        return new Transformation(t, r, new Vector3f(1, 1, 1), new Quaternionf());
    }
}

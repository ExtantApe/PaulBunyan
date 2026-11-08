package com.extantape.paulbunyan.tree;

import com.extantape.paulbunyan.tree.Cell.Kind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether a chopped log belongs to a natural tree, and which blocks go with it.
 *
 * <p>The rules, in order:
 * <ul>
 *   <li>The tree is every unplaced log of the same kind connected to the chopped one, corners included.
 *   <li>If any of those logs touches a build block on a face, nothing falls. More than
 *       {@code maxStrays} placed logs touching it count as a build too.
 *   <li>Leaves go with the tree only if no player placed them and they're nearer to this tree's logs
 *       than to any other log, counting steps through leaves the way the game does for decay.
 *   <li>A tree needs at least {@code minLeaves} such leaves. Players can't place natural leaves, so a
 *       log cabin or a log fence never qualifies. A fungus has no such marker, so it has to stand on
 *       its own nylium instead.
 * </ul>
 */
public final class TreeScanner {
    /** Leaves further than this from a log decay in the game, so they're never kept alive by one. */
    public static final int LEAF_REACH = 7;

    public record Limits(int maxLogs, int maxLeaves, int minLeaves, int maxStrays) {
        public static final Limits DEFAULT = new Limits(250, 3000, 3, 2);
    }

    private final WorldView view;
    private final Limits limits;

    public TreeScanner(WorldView view, Limits limits) {
        this.view = view;
        this.limits = limits;
    }

    public Scan scan(Pos start) {
        Cell first = view.cell(start);
        Species species = first.kind() == Kind.LOG ? first.species() : null;
        if (species == null) return Scan.skip(Scan.Reason.NOT_A_TREE_LOG);

        // 1. The logs.
        Set<Pos> logs = new LinkedHashSet<>();
        Set<Pos> strays = new HashSet<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        logs.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Pos p = queue.poll();
            for (Pos n : p.around()) {
                if (logs.contains(n)) continue;
                Cell c = view.cell(n);
                if (c.kind() == Kind.LOG && c.name().equals(species.log)) {
                    logs.add(n);
                    if (logs.size() > limits.maxLogs()) return Scan.skip(Scan.Reason.TOO_BIG, "over " + limits.maxLogs() + " logs");
                    queue.add(n);
                } else if (c.kind() == Kind.PLACED_LOG) {
                    strays.add(n);
                }
            }
        }

        // 2. Nothing built may touch it. Noted here, reported after the leaves, so a log cabin is
        // reported as having no natural leaves (it isn't a tree at all) rather than as touching a build.
        String build = null;
        for (Pos p : logs) {
            for (Pos n : p.faces()) {
                Cell c = view.cell(n);
                if (build == null && (c.kind() == Kind.BUILD || c.kind() == Kind.UNKNOWN))
                    build = c.name().toLowerCase(java.util.Locale.ROOT) + " at " + n;
            }
        }
        if (build == null && strays.size() > limits.maxStrays()) build = strays.size() + " placed logs";

        // 3. The leaves: walk out from this tree's logs, noting any other log the leaves reach.
        Map<Pos, Integer> ours = new HashMap<>();
        Set<Pos> foreign = new HashSet<>();
        for (Pos p : logs) queue.add(p);
        Map<Pos, Integer> depth = new HashMap<>();
        for (Pos p : logs) depth.put(p, 0);
        while (!queue.isEmpty()) {
            Pos p = queue.poll();
            int d = depth.get(p);
            for (Pos n : p.faces()) {
                if (depth.containsKey(n) || foreign.contains(n)) continue;
                Cell c = view.cell(n);
                if (isLeafOf(species, c)) {
                    if (d + 1 > LEAF_REACH) continue;
                    depth.put(n, d + 1);
                    ours.put(n, d + 1);
                    if (ours.size() > limits.maxLeaves()) return Scan.skip(Scan.Reason.TOO_BIG, "over " + limits.maxLeaves() + " leaves");
                    queue.add(n);
                } else if (d > 0 && (c.kind() == Kind.LOG || c.kind() == Kind.PLACED_LOG)) {
                    foreign.add(n); // another tree's log, or a placed one, reached through leaves
                }
            }
        }

        // 4. Leaves as near or nearer to another log stay with it.
        if (!foreign.isEmpty()) {
            Map<Pos, Integer> theirs = new HashMap<>();
            for (Pos p : foreign) {
                theirs.put(p, 0);
                queue.add(p);
            }
            while (!queue.isEmpty()) {
                Pos p = queue.poll();
                int d = theirs.get(p);
                if (d >= LEAF_REACH) continue;
                for (Pos n : p.faces()) {
                    if (theirs.containsKey(n) || !ours.containsKey(n)) continue;
                    theirs.put(n, d + 1);
                    queue.add(n);
                }
            }
            ours.entrySet().removeIf(e -> theirs.getOrDefault(e.getKey(), Integer.MAX_VALUE) <= e.getValue());
        }

        // 5. Is it natural?
        int minY = Integer.MAX_VALUE;
        for (Pos p : logs) minY = Math.min(minY, p.y());
        List<Pos> base = new ArrayList<>();
        for (Pos p : logs) if (p.y() == minY) base.add(p);
        if (species.nether()) {
            boolean onNylium = false;
            for (Pos p : base) onNylium |= species.nylium().equals(view.cell(p.below()).name());
            if (!onNylium) return Scan.skip(Scan.Reason.NOT_ON_NYLIUM);
        }
        if (ours.size() < limits.minLeaves()) return Scan.skip(Scan.Reason.NO_NATURAL_LEAVES);
        if (build != null) return Scan.skip(Scan.Reason.TOUCHES_BUILD, build);

        List<Pos> leaves = new ArrayList<>(ours.keySet());
        leaves.sort((a, b) -> a.y() != b.y() ? Integer.compare(a.y(), b.y()) : a.x() != b.x() ? Integer.compare(a.x(), b.x()) : Integer.compare(a.z(), b.z()));
        return Scan.fell(new Tree(species, new ArrayList<>(logs), leaves, base, minY, strays.size()));
    }

    private static boolean isLeafOf(Species species, Cell c) {
        if (c.kind() != Kind.CAP || c.persistent()) return false;
        return species.nether() ? species.caps.contains(c.name()) : c.name().endsWith("_LEAVES");
    }
}

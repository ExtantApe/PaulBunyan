package com.extantape.paulbunyan.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Makes up a wild tree for a stump: a trunk that twists as it climbs, a few odd branches and a ragged crown of mixed
 * leaves. It only draws the shape, as a list of blocks; whether the blocks may go in is {@link Growth}'s call. The
 * same species, stump and seed always give the same tree, so a crash and a replay agree. Block names, not blocks, so
 * this loads without a server.
 */
public final class Grower {
    private Grower() {}

    /** One block of the tree. {@code axis} is x, y or z for a log, and 0 for leaves. */
    public record Piece(Pos at, String material, char axis) {
        public boolean isLog() {
            return axis != 0;
        }
    }

    /** The shape: logs first (the trunk and branches), then leaves. No block is in both lists. */
    public record Plan(List<Piece> logs, List<Piece> leaves) {
        public List<Piece> all() {
            List<Piece> out = new ArrayList<>(logs);
            out.addAll(leaves);
            return out;
        }

        /** The corner of the box around every block, lowest first. */
        public Pos min() {
            return corner(true);
        }

        public Pos max() {
            return corner(false);
        }

        private Pos corner(boolean low) {
            int x = low ? Integer.MAX_VALUE : Integer.MIN_VALUE, y = x, z = x;
            for (Piece p : all()) {
                x = low ? Math.min(x, p.at().x()) : Math.max(x, p.at().x());
                y = low ? Math.min(y, p.at().y()) : Math.max(y, p.at().y());
                z = low ? Math.min(z, p.at().z()) : Math.max(z, p.at().z());
            }
            return new Pos(x, y, z);
        }
    }

    /** Trunk height range, the leaf blocks (and how often the second one turns up) and the crown's radius. */
    private record Look(int minHeight, int maxHeight, String leaf, String other, int otherPercent, int radius) {}

    private static Look look(Species s) {
        return switch (s) {
            case OAK -> new Look(5, 8, "OAK_LEAVES", "AZALEA_LEAVES", 8, 2);
            case BIRCH -> new Look(6, 9, "BIRCH_LEAVES", "OAK_LEAVES", 8, 2);
            case SPRUCE -> new Look(7, 12, "SPRUCE_LEAVES", "SPRUCE_LEAVES", 0, 2);
            case JUNGLE -> new Look(6, 11, "JUNGLE_LEAVES", "OAK_LEAVES", 10, 2);
            case ACACIA -> new Look(5, 7, "ACACIA_LEAVES", "OAK_LEAVES", 6, 2);
            case DARK_OAK -> new Look(6, 8, "DARK_OAK_LEAVES", "OAK_LEAVES", 8, 2);
            case PALE_OAK -> new Look(6, 8, "PALE_OAK_LEAVES", "PALE_OAK_LEAVES", 0, 2);
            case CHERRY -> new Look(5, 7, "CHERRY_LEAVES", "CHERRY_LEAVES", 0, 2);
            case MANGROVE -> new Look(6, 9, "MANGROVE_LEAVES", "MANGROVE_LEAVES", 0, 2);
            case CRIMSON -> new Look(5, 9, "NETHER_WART_BLOCK", "SHROOMLIGHT", 6, 3);
            case WARPED -> new Look(5, 9, "WARPED_WART_BLOCK", "SHROOMLIGHT", 6, 3);
        };
    }

    /** The leaf blocks a species' wild tree may use. */
    public static List<String> leafBlocks(Species s) {
        Look l = look(s);
        return l.otherPercent() == 0 ? List.of(l.leaf()) : List.of(l.leaf(), l.other());
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * The tree for a stump. {@code stumpTops} is the highest standing log of each trunk column (one column, or four
     * for a 2x2 trunk). The tree starts on top of them and never includes them.
     */
    public static Plan plan(Species species, List<Pos> stumpTops, long seed) {
        Look look = look(species);
        Random r = new Random(seed * 31 + species.ordinal());
        boolean big = stumpTops.size() >= 4;
        int height = look.minHeight() + r.nextInt(look.maxHeight() - look.minHeight() + 1) + (big ? 2 : 0);
        String log = species.log;

        Map<Pos, Piece> logs = new LinkedHashMap<>();
        Map<Pos, Piece> leaves = new LinkedHashMap<>();
        Pos ref = stumpTops.getFirst();
        // Where the trunk's reference column is at each height, for branches to start from.
        Pos[] trunk = new Pos[height];
        int ox = 0, oz = 0, lastDx = 0, lastDz = 0;
        for (int i = 0; i < height; i++) {
            for (Pos c : stumpTops) put(logs, new Pos(c.x() + ox, c.y() + 1 + i, c.z() + oz), log, 'y');
            trunk[i] = new Pos(ref.x() + ox, ref.y() + 1 + i, ref.z() + oz);
            // From the third log up it may lean one block sideways, never more than two from the stump.
            if (i >= 2 && i < height - 1 && r.nextInt(100) < 35) {
                int[] d = r.nextInt(100) < 50 && (lastDx != 0 || lastDz != 0) ? new int[] {lastDx, lastDz} : DIRS[r.nextInt(4)];
                int nx = ox + d[0], nz = oz + d[1];
                if (Math.abs(nx) <= 2 && Math.abs(nz) <= 2) {
                    // The log beside, lying toward the lean, so the two are joined face to face.
                    for (Pos c : stumpTops) put(logs, new Pos(c.x() + nx, c.y() + 1 + i, c.z() + nz), log, d[0] != 0 ? 'x' : 'z');
                    ox = nx;
                    oz = nz;
                    lastDx = d[0];
                    lastDz = d[1];
                }
            }
        }

        // Branches: two to four, from the upper half of the trunk, each a few logs long and sometimes turning up.
        int branches = 2 + r.nextInt(3) + (big ? 1 : 0);
        List<Pos> tips = new ArrayList<>();
        for (int b = 0; b < branches; b++) {
            int from = Math.max(2, height / 2) + r.nextInt(Math.max(1, height - 2 - Math.max(2, height / 2) + 1));
            from = Math.min(from, height - 1);
            int[] d = DIRS[r.nextInt(4)];
            int length = 2 + r.nextInt(3) + (big ? 1 : 0);
            Pos at = trunk[from];
            Pos tip = at;
            for (int k = 1; k <= length; k++) {
                Pos p = new Pos(at.x() + d[0] * k, at.y(), at.z() + d[1] * k);
                put(logs, p, log, d[0] != 0 ? 'x' : 'z');
                tip = p;
                if (k == length - 1 && r.nextInt(100) < 40) {
                    at = at.above();
                    Pos up = new Pos(p.x(), at.y(), p.z());
                    put(logs, up, log, 'y');
                    tip = up;
                    // the rest of the branch, if any, carries on from the higher level
                }
            }
            tips.add(tip);
        }

        // The crown, then a smaller tuft on each branch tip.
        int crown = look.radius() + (big ? 1 : 0);
        Pos top = trunk[height - 1];
        blob(leaves, logs, r, look, top, crown);
        blob(leaves, logs, r, look, top.add(0, -2, 0), crown);
        for (Pos tip : tips) blob(leaves, logs, r, look, tip, Math.max(1, look.radius() - (big ? 0 : 1)) + (species.nether() ? 1 : 0));
        return new Plan(new ArrayList<>(logs.values()), new ArrayList<>(leaves.values()));
    }

    private static void put(Map<Pos, Piece> into, Pos at, String material, char axis) {
        into.putIfAbsent(at, new Piece(at, material, axis));
    }

    /** A rounded, ragged ball of leaves, trimmed at the corners and thinner on the outside. */
    private static void blob(Map<Pos, Piece> leaves, Map<Pos, Piece> logs, Random r, Look look, Pos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                for (int dy = -radius; dy <= radius; dy++) {
                    double d2 = dx * dx + dz * dz + dy * dy * 1.5;
                    if (d2 > radius * radius + 0.5) continue;
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius) continue;
                    boolean outer = d2 > (radius - 1) * (radius - 1);
                    boolean gap = outer && r.nextInt(100) < 14;
                    String m = r.nextInt(100) < look.otherPercent() ? look.other() : look.leaf();
                    Pos p = center.add(dx, dy, dz);
                    if (gap || logs.containsKey(p)) continue;
                    leaves.putIfAbsent(p, new Piece(p, m, (char) 0));
                }
    }
}

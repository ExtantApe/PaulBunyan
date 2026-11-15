package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The tree comes down in one piece (felling-plan.md). Once it has tipped flat, the whole tree drops together until
 * some part of it strikes something. On flat ground it lands whole. Across a gap it stays whole too, lying from one
 * side to the other, and a short end may stick out past an edge. Past that, the part hanging over a drop breaks off in
 * sections of two to five logs, which fall on, each in one piece, until they strike something in turn.
 *
 * <p>It works only out where each log comes to rest and how far it dropped on the way. {@link Landing} then places
 * each log and decides, from that drop, whether it lands whole, breaks into an item or splinters. Nothing here changes
 * the world, and the same seed always gives the same breaks, so a fall finished after a crash breaks the same way.
 */
public final class OnePiece {
    /** A piece that would drop further than this is left to {@link Landing}, log by log (it splinters or is lost anyway). */
    public static final int MAX_DROP = 64;
    /** How far a piece may stick out past the last thing it rests on before the end breaks off, in logs. */
    public static final int OVERHANG = 2;
    static final int MIN_SECTION = 2, MAX_SECTION = 5;

    /**
     * Where a log comes to rest, and how far it dropped as part of its piece. {@code held} means the piece holds it
     * there even with air under it, as part of a trunk lying across a gap or sticking out over an edge.
     */
    public record Rest(Pos at, int dropped, boolean held) {}

    private record Piece(List<Pos> logs, int dropped) {}

    private OnePiece() {}

    /**
     * @param tipped where each log lies once the tree has tipped flat (see {@link FallPlan#tipped})
     * @param alongX true when the tree fell along x, so sections are cut across x
     * @param seed the same for every run of one fall
     * @return each tipped position mapped to where that log comes to rest
     */
    public static Map<Pos, Rest> settle(Landing.Ground g, Collection<Pos> tipped, boolean alongX, long seed) {
        Random random = new Random(seed);
        Map<Pos, Pos> origin = new HashMap<>(); // where a log is now -> its tipped position
        for (Pos p : tipped) origin.put(p, p);
        Map<Pos, Rest> out = new HashMap<>();
        Set<Pos> settled = new HashSet<>();
        Deque<Piece> work = new ArrayDeque<>();
        for (List<Pos> c : components(tipped)) work.add(new Piece(c, 0));
        while (!work.isEmpty()) {
            Piece piece = lowestFirst(work);
            Set<Pos> mine = new HashSet<>(piece.logs());
            // Logs that have come to rest, and pieces still waiting their turn, are both in the way.
            Set<Pos> blocked = new HashSet<>(settled);
            for (Piece other : work) blocked.addAll(other.logs());
            int drop = Integer.MAX_VALUE;
            for (Pos p : piece.logs()) if (!mine.contains(p.below())) drop = Math.min(drop, free(g, p, blocked));
            if (drop > MAX_DROP) { // nothing under it worth speaking of: let Landing drop it log by log
                rest(piece.logs(), piece.dropped(), false, origin, out, settled);
                continue;
            }
            List<Pos> moved = new ArrayList<>(piece.logs().size());
            Map<Pos, Pos> movedOrigin = new HashMap<>();
            for (Pos p : piece.logs()) {
                Pos to = p.add(0, -drop, 0);
                moved.add(to);
                movedOrigin.put(to, origin.remove(p));
            }
            origin.putAll(movedOrigin);
            int dropped = piece.dropped() + drop;
            Set<Pos> movedSet = new HashSet<>(moved);
            List<Pos> resting = new ArrayList<>(), hanging = new ArrayList<>();
            for (Pos p : moved) {
                Pos under = p.below();
                if (movedSet.contains(under)) continue;
                if (free(g, p, blocked) == 0) resting.add(p);
                else hanging.add(p);
            }
            if (hanging.isEmpty()) {
                rest(moved, dropped, true, origin, out, settled);
                continue;
            }
            // It struck something with only part of itself. Whatever lies between the things it struck stays, like a log
            // across a gap, and so does a short end sticking out past them. The rest breaks off.
            Set<Pos> held = heldBy(moved, resting, alongX);
            List<Pos> stays = new ArrayList<>(), breaks = new ArrayList<>();
            for (Pos p : moved) (held.contains(p) ? stays : breaks).add(p);
            rest(stays, dropped, true, origin, out, settled);
            blocked.addAll(stays);
            for (List<Pos> c : components(breaks))
                for (List<Pos> s : sections(c, alongX, random))
                    for (List<Pos> part : components(s)) work.add(new Piece(part, dropped));
        }
        return out;
    }

    /** Air under a log before it would strike the ground or a log in the way. 0 if it's in the ground. */
    static int free(Landing.Ground g, Pos p, Set<Pos> blocked) {
        if (!g.isAir(p) || blocked.contains(p)) return 0;
        int n = 0;
        Pos q = p.below();
        while (q.y() > g.worldMin() && g.isAir(q) && !blocked.contains(q)) {
            if (++n > MAX_DROP) return Integer.MAX_VALUE;
            q = q.below();
        }
        return q.y() <= g.worldMin() ? Integer.MAX_VALUE : n;
    }

    /**
     * The logs that stay with the part that struck something: every log between the first and last resting log along
     * the line the tree fell, and up to {@link #OVERHANG} logs past each end.
     */
    static Set<Pos> heldBy(List<Pos> piece, List<Pos> resting, boolean alongX) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (Pos p : resting) {
            int a = alongX ? p.x() : p.z();
            lo = Math.min(lo, a);
            hi = Math.max(hi, a);
        }
        Set<Pos> held = new HashSet<>();
        for (Pos p : piece) {
            int a = alongX ? p.x() : p.z();
            if (a >= lo - OVERHANG && a <= hi + OVERHANG) held.add(p);
        }
        return held;
    }

    /** Cuts a broken-off part into sections of two to five logs, along the line the tree fell. */
    static List<List<Pos>> sections(List<Pos> part, boolean alongX, Random random) {
        List<Pos> order = new ArrayList<>(part);
        order.sort(Comparator.<Pos>comparingInt(p -> alongX ? p.x() : p.z()).thenComparingInt(Pos::y)
            .thenComparingInt(p -> alongX ? p.z() : p.x()));
        List<List<Pos>> out = new ArrayList<>();
        int i = 0;
        while (i < order.size()) {
            int size = MIN_SECTION + random.nextInt(MAX_SECTION - MIN_SECTION + 1);
            int end = Math.min(order.size(), i + size);
            out.add(new ArrayList<>(order.subList(i, end)));
            i = end;
        }
        // A lone log left at the end joins the section before it when that stays within five logs.
        if (out.size() > 1 && out.getLast().size() < MIN_SECTION && out.get(out.size() - 2).size() < MAX_SECTION) {
            List<Pos> last = out.removeLast();
            out.getLast().addAll(last);
        }
        return out;
    }

    /** Groups logs that touch, by a face, an edge or a corner. */
    static List<List<Pos>> components(Collection<Pos> logs) {
        Set<Pos> left = new HashSet<>(logs);
        List<List<Pos>> out = new ArrayList<>();
        List<Pos> ordered = new ArrayList<>(logs);
        ordered.sort(Comparator.comparingInt(Pos::y).thenComparingInt(Pos::x).thenComparingInt(Pos::z));
        for (Pos start : ordered) {
            if (!left.remove(start)) continue;
            List<Pos> c = new ArrayList<>();
            Deque<Pos> q = new ArrayDeque<>();
            q.add(start);
            while (!q.isEmpty()) {
                Pos p = q.poll();
                c.add(p);
                for (Pos n : p.around()) if (left.remove(n)) q.add(n);
            }
            out.add(c);
        }
        return out;
    }

    /** Lower pieces come to rest first, so higher ones can land on them. */
    private static Piece lowestFirst(Deque<Piece> work) {
        Piece best = null;
        int bestY = Integer.MAX_VALUE;
        for (Piece p : work) {
            int y = Integer.MAX_VALUE;
            for (Pos l : p.logs()) y = Math.min(y, l.y());
            if (y < bestY) { bestY = y; best = p; }
        }
        work.remove(best);
        return best;
    }

    private static void rest(List<Pos> logs, int dropped, boolean held, Map<Pos, Pos> origin, Map<Pos, Rest> out, Set<Pos> settled) {
        for (Pos p : logs) {
            out.put(origin.get(p), new Rest(p, dropped, held));
            settled.add(p);
        }
    }
}

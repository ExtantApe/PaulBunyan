package com.extantape.paulbunyan.regrow;

import com.extantape.paulbunyan.tree.Pos;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * What regrowth remembers, in one plain text file, {@code regrowth.txt} in the plugin folder. One fact per line,
 * words separated by spaces, world names written as URL-encoded text. Times are the world's game time in ticks.
 *
 * <pre>
 * stump  world species seed due growing x,y,z;x,y,z   a stump left by a high cut: the top log of each trunk column
 * fall   world species seed born sapling x,y,z;...    logs a fall left lying; saplings sprout once they're all gone
 * sapling world x,y,z material expires                a sapling PaulBunyan planted (expires 0 means it stays)
 * grown  world x,y,z x,y,z                            the box around a tree that grew from a stump or a sapling
 * </pre>
 *
 * It's written to a temporary file and moved into place, so a crash leaves either the old file or the new one.
 */
public final class Store {
    public record Stump(String world, String species, long seed, long due, boolean growing, List<Pos> tops) {
        public Stump due(long due) {
            return new Stump(world, species, seed, due, growing, tops);
        }

        public Stump startGrowing() {
            return new Stump(world, species, seed, due, true, tops);
        }
    }

    public record Fall(String world, String species, long seed, long born, String sapling, List<Pos> logs) {}

    public record Sapling(String world, Pos at, String material, long expires) {}

    public record Grown(String world, Pos min, Pos max) {
        public boolean contains(Pos p) {
            return p.x() >= min.x() && p.x() <= max.x() && p.y() >= min.y() && p.y() <= max.y() && p.z() >= min.z() && p.z() <= max.z();
        }
    }

    public final List<Stump> stumps = new ArrayList<>();
    public final List<Fall> falls = new ArrayList<>();
    public final List<Sapling> saplings = new ArrayList<>();
    public final List<Grown> grown = new ArrayList<>();
    /** Lines that couldn't be read when the file was loaded. They're dropped on the next save. */
    public int skipped;

    private static final String HEADER = """
        # PaulBunyan regrowth list. One fact per line; see the README for what each line means.
        # Plain text. Don't edit it while the server is running.
        """;

    public boolean isEmpty() {
        return stumps.isEmpty() && falls.isEmpty() && saplings.isEmpty() && grown.isEmpty();
    }

    public boolean isGrown(String world, Pos p) {
        for (Grown g : grown) if (g.world().equals(world) && g.contains(p)) return true;
        return false;
    }

    // ---------- text ----------

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String dec(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static String posList(List<Pos> ps) {
        StringBuilder b = new StringBuilder();
        for (Pos p : ps) b.append(b.isEmpty() ? "" : ";").append(p);
        return b.toString();
    }

    static Pos pos(String s) {
        String[] a = s.split(",");
        return new Pos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
    }

    static List<Pos> posList(String s) {
        List<Pos> out = new ArrayList<>();
        for (String p : s.split(";")) if (!p.isEmpty()) out.add(pos(p));
        return out;
    }

    public String toText() {
        StringBuilder s = new StringBuilder(HEADER);
        for (Stump t : stumps)
            s.append("stump ").append(enc(t.world())).append(' ').append(t.species()).append(' ').append(t.seed()).append(' ')
                .append(t.due()).append(' ').append(t.growing() ? 1 : 0).append(' ').append(posList(t.tops())).append('\n');
        for (Fall f : falls)
            s.append("fall ").append(enc(f.world())).append(' ').append(f.species()).append(' ').append(f.seed()).append(' ')
                .append(f.born()).append(' ').append(f.sapling()).append(' ').append(posList(f.logs())).append('\n');
        for (Sapling p : saplings)
            s.append("sapling ").append(enc(p.world())).append(' ').append(p.at()).append(' ').append(p.material()).append(' ')
                .append(p.expires()).append('\n');
        for (Grown g : grown)
            s.append("grown ").append(enc(g.world())).append(' ').append(g.min()).append(' ').append(g.max()).append('\n');
        return s.toString();
    }

    /** Reads what {@link #toText()} wrote. A line that makes no sense is skipped and counted, never fatal. */
    public static Store parse(List<String> lines) {
        Store st = new Store();
        for (String line : lines) {
            String l = line.strip();
            if (l.isEmpty() || l.startsWith("#")) continue;
            String[] w = l.split(" ");
            try {
                switch (w[0]) {
                    case "stump" -> {
                        List<Pos> tops = posList(w[6]);
                        if (tops.isEmpty()) throw new IllegalArgumentException("no stump columns");
                        st.stumps.add(new Stump(dec(w[1]), w[2], Long.parseLong(w[3]), Long.parseLong(w[4]), w[5].equals("1"), tops));
                    }
                    case "fall" -> st.falls.add(new Fall(dec(w[1]), w[2], Long.parseLong(w[3]), Long.parseLong(w[4]), w[5], posList(w[6])));
                    case "sapling" -> st.saplings.add(new Sapling(dec(w[1]), pos(w[2]), w[3], Long.parseLong(w[4])));
                    case "grown" -> st.grown.add(new Grown(dec(w[1]), pos(w[2]), pos(w[3])));
                    default -> st.skipped++;
                }
            } catch (RuntimeException e) {
                st.skipped++;
            }
        }
        return st;
    }

    public static Store load(Path file) throws IOException {
        if (!Files.exists(file)) return new Store();
        return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    public void save(Path file) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, toText(), StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}

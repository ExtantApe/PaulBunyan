package com.extantape.paulbunyan.fall;

import com.extantape.paulbunyan.tree.Pos;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * One small text file per fall, so a crash part-way through doesn't lose logs. It's written before any
 * block is removed, rewritten once the logs have landed, and deleted once the world has had time to
 * save. Each file is plain text, one fact per line.
 */
public final class Journal {
    /** A log on its way down: where it was, what it was, where it's headed and what it becomes. */
    public record Log(Pos origin, String data, Pos target, String tippedData) {}

    /** A log that came to rest as a block: where it came from, where it is and what it is. */
    public record Landed(Pos origin, Pos at, String data) {}

    public record Entry(UUID id, String world, UUID player, String playerName, boolean asItems, int minY,
                        String sapling, List<Pos> replant, List<Log> logs, boolean landed, long landedAt,
                        List<Landed> blocks) {
        public Entry withLanded(long at, List<Landed> blocks) {
            return new Entry(id, world, player, playerName, asItems, minY, sapling, replant, logs, true, at, blocks);
        }
    }

    private final Path dir;

    public Journal(Path dir) throws IOException {
        this.dir = dir;
        Files.createDirectories(dir);
    }

    private Path file(UUID id) {
        return dir.resolve(id + ".txt");
    }

    public void write(Entry e) throws IOException {
        StringBuilder s = new StringBuilder();
        s.append("world ").append(e.world()).append('\n');
        s.append("player ").append(e.player()).append(' ').append(e.playerName()).append('\n');
        s.append("land-as ").append(e.asItems() ? "items" : "blocks").append('\n');
        s.append("min-y ").append(e.minY()).append('\n');
        s.append("sapling ").append(e.sapling() == null ? "-" : e.sapling()).append('\n');
        for (Pos p : e.replant()) s.append("replant ").append(p).append('\n');
        for (Log l : e.logs()) s.append("log ").append(l.origin()).append(' ').append(l.data()).append(' ')
            .append(l.target()).append(' ').append(l.tippedData()).append('\n');
        if (e.landed()) {
            s.append("landed ").append(e.landedAt()).append('\n');
            for (Landed b : e.blocks())
                s.append("block ").append(b.origin()).append(' ').append(b.at()).append(' ').append(b.data()).append('\n');
        }
        Path tmp = dir.resolve(e.id() + ".tmp");
        Files.writeString(tmp, s, StandardCharsets.UTF_8);
        Files.move(tmp, file(e.id()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public void delete(UUID id) {
        try {
            Files.deleteIfExists(file(id));
        } catch (IOException ignored) {
            // Left behind, it's replayed harmlessly: every log is checked before it's touched.
        }
    }

    public List<Entry> load() throws IOException {
        List<Entry> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".txt")).toList()) {
                UUID id = UUID.fromString(f.getFileName().toString().replace(".txt", ""));
                out.add(parse(id, Files.readAllLines(f, StandardCharsets.UTF_8)));
            }
        }
        return out;
    }

    static Pos pos(String s) {
        String[] a = s.split(",");
        return new Pos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
    }

    static Entry parse(UUID id, List<String> lines) {
        String world = null, name = null, sapling = null;
        UUID player = null;
        boolean items = false, landed = false;
        int minY = 0;
        long landedAt = 0;
        List<Pos> replant = new ArrayList<>();
        List<Landed> blocks = new ArrayList<>();
        List<Log> logs = new ArrayList<>();
        for (String line : lines) {
            String[] w = line.split(" ");
            switch (w[0]) {
                case "world" -> world = line.substring(6);
                case "player" -> { player = UUID.fromString(w[1]); name = w.length > 2 ? w[2] : "?"; }
                case "land-as" -> items = w[1].equals("items");
                case "min-y" -> minY = Integer.parseInt(w[1]);
                case "sapling" -> sapling = w[1].equals("-") ? null : w[1];
                case "replant" -> replant.add(pos(w[1]));
                case "log" -> logs.add(new Log(pos(w[1]), w[2], pos(w[3]), w[4]));
                case "landed" -> { landed = true; landedAt = Long.parseLong(w[1]); }
                case "block" -> blocks.add(new Landed(pos(w[1]), pos(w[2]), w[3]));
                default -> { }
            }
        }
        return new Entry(id, world, player, name, items, minY, sapling, replant, logs, landed, landedAt, blocks);
    }
}

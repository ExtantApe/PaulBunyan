package com.extantape.paulbunyan.tools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * PaulBunyan's resource pack: the tool pictures and the item model that layers a handle and a head. Everything is in
 * the {@code paulbunyan} namespace, so it overrides nothing and sits alongside any other pack. The same plugin version
 * always builds the same bytes, so the same SHA-1: entries are sorted and carry a fixed date.
 *
 * <p>Run as a program, it writes the pack to the path given (the release attaches that file).
 */
public final class PackBuilder {
    public static final int MIN_FORMAT = 84, MAX_FORMAT = 999;

    public record Built(byte[] zip, String sha1) {}

    private PackBuilder() {}

    public static void main(String[] args) throws IOException {
        Built b = build();
        Path out = Path.of(args[0]);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.write(out, b.zip());
        System.out.println(out + " " + b.sha1());
    }

    public static Built build() throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        files.put("pack.mcmeta", text("""
            {
              "pack": {
                "description": "PaulBunyan tools",
                "min_format": %d,
                "max_format": %d
              }
            }
            """.formatted(MIN_FORMAT, MAX_FORMAT)));
        for (Handle h : Handle.values()) {
            files.put("assets/paulbunyan/models/item/axe/handle/" + h.id() + ".json", model("item/axe/handle/" + h.id()));
            files.put("assets/paulbunyan/textures/item/axe/handle/" + h.id() + ".png", png(Textures.handle(h)));
        }
        for (Head h : Head.values()) {
            files.put("assets/paulbunyan/models/item/axe/head/" + h.id() + ".json", model("item/axe/head/" + h.id()));
            files.put("assets/paulbunyan/textures/item/axe/head/" + h.id() + ".png", png(Textures.head(h)));
        }
        files.put("assets/paulbunyan/items/axe.json", text(axeItem()));
        byte[] zip = zip(files);
        return new Built(zip, sha1(zip));
    }

    /** The axe: two layers, each picked by one of the item's custom model data strings (0 the handle, 1 the head). */
    static String axeItem() {
        StringBuilder handles = new StringBuilder(), heads = new StringBuilder();
        for (Handle h : Handle.values()) cases(handles, h.id(), "item/axe/handle/" + h.id());
        for (Head h : Head.values()) cases(heads, h.id(), "item/axe/head/" + h.id());
        return """
            {
              "model": {
                "type": "minecraft:composite",
                "models": [
                  %s,
                  %s
                ]
              }
            }
            """.formatted(select(0, handles, "item/axe/handle/oak"), select(1, heads, "item/axe/head/iron"));
    }

    private static String select(int index, StringBuilder cases, String fallback) {
        return """
            {
                    "type": "minecraft:select",
                    "property": "minecraft:custom_model_data",
                    "index": %d,
                    "cases": [
            %s
                    ],
                    "fallback": { "type": "minecraft:model", "model": "paulbunyan:%s" }
                  }""".formatted(index, cases.toString().stripTrailing().replaceAll(",$", ""), fallback);
    }

    private static void cases(StringBuilder sb, String when, String model) {
        sb.append("          { \"when\": \"").append(when).append("\", \"model\": { \"type\": \"minecraft:model\", \"model\": \"paulbunyan:")
            .append(model).append("\" } },\n");
    }

    private static byte[] model(String texture) {
        return text("""
            {
              "parent": "minecraft:item/handheld",
              "textures": { "layer0": "paulbunyan:%s" }
            }
            """.formatted(texture));
    }

    private static byte[] text(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LocalDateTime fixed = LocalDateTime.of(2026, 1, 1, 0, 0);
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (var e : files.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                entry.setTimeLocal(fixed);
                zip.putNextEntry(entry);
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.extantape.paulbunyan.tools;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * The tool pictures, drawn here pixel by pixel so the plugin ships no game art: 16x16, in the vanilla look. A special
 * axe is two layers, a handle and a head, each a mask filled from a palette, so every pairing works without a picture
 * of its own.
 */
public final class Textures {
    private Textures() {}

    /** The handle: a two-pixel stick from the bottom left, light on its upper side and dark below. */
    static final String[] HANDLE = {
        "................",
        "................",
        "..............ld",
        ".............ld.",
        "............ld..",
        "...........ld...",
        "..........ld....",
        ".........ld.....",
        "........ld......",
        ".......ld.......",
        "......ld........",
        ".....ld.........",
        "....ld..........",
        "...ld...........",
        "..od............",
        "................",
    };

    /** The head: a blade on the left of the handle's top, with a socket round the handle. o outline, s shadow, h body, l edge. */
    static final String[] HEAD = {
        "................",
        "..oo............",
        ".olho...........",
        ".olhho....ooo...",
        "olhhhhoooohhso..",
        "olhhhhhhhhhsso..",
        "olhhhhhhhhssso..",
        "olhhhhhsoooooo..",
        ".olhhhso........",
        ".olhhso.........",
        "..olso..........",
        "...o............",
        "................",
        "................",
        "................",
        "................",
    };

    /** Handle colours: outline/dark, then light. */
    static final Map<Handle, int[]> WOOD = Map.ofEntries(
        Map.entry(Handle.OAK, new int[] {0x5E4425, 0xB8985F}),
        Map.entry(Handle.SPRUCE, new int[] {0x4A321A, 0x8C6A40}),
        Map.entry(Handle.BIRCH, new int[] {0x8A7A48, 0xDCCD94}),
        Map.entry(Handle.JUNGLE, new int[] {0x6A4529, 0xB8896A}),
        Map.entry(Handle.ACACIA, new int[] {0x7A3C1B, 0xC8753F}),
        Map.entry(Handle.DARK_OAK, new int[] {0x2E1C0D, 0x5A3F24}),
        Map.entry(Handle.MANGROVE, new int[] {0x4F1B1A, 0x944641}),
        Map.entry(Handle.CHERRY, new int[] {0x9C6159, 0xEFC7BE}),
        Map.entry(Handle.PALE_OAK, new int[] {0x9C918C, 0xECE6E2}),
        Map.entry(Handle.CRIMSON, new int[] {0x421B2C, 0x93486A}),
        Map.entry(Handle.WARPED, new int[] {0x184A46, 0x3B9E94}));

    /** Head colours: outline, shadow, body, edge. */
    static final Map<Head, int[]> METAL = Map.ofEntries(
        Map.entry(Head.COPPER, new int[] {0x4A2418, 0xA0532F, 0xC8714A, 0xF0B08A}),
        Map.entry(Head.IRON, new int[] {0x353535, 0x9A9A9A, 0xD0D0D0, 0xFFFFFF}),
        Map.entry(Head.GOLD, new int[] {0x5C3F08, 0xD0951A, 0xF2CC3D, 0xFFF6B0}),
        Map.entry(Head.EMERALD, new int[] {0x0A3F20, 0x15994A, 0x41D47A, 0xB0F7C8}),
        Map.entry(Head.AMETHYST, new int[] {0x34195A, 0x7A50B5, 0xA57BDB, 0xE2CEF8}),
        Map.entry(Head.QUARTZ, new int[] {0x6E645C, 0xC9C0B6, 0xECE6DF, 0xFFFFFF}),
        Map.entry(Head.LAPIS, new int[] {0x0C1A50, 0x1F45A8, 0x3B6BD9, 0x9CBCFF}),
        Map.entry(Head.REDSTONE, new int[] {0x420404, 0x9E1111, 0xDB2020, 0xFF8A7A}),
        Map.entry(Head.GLOWSTONE, new int[] {0x5E4016, 0xC9963A, 0xF2CF6B, 0xFFF6C8}),
        Map.entry(Head.OBSIDIAN, new int[] {0x07050D, 0x21163A, 0x3B2A5C, 0x7A64B5}),
        Map.entry(Head.PRISMARINE, new int[] {0x1A423B, 0x4F9E8C, 0x6EC2AD, 0xC0F2E6}));

    /**
     * The handle leaves out every pixel the head covers. The two layers lie in the same plane, so pixels drawn by both
     * would flicker.
     */
    public static BufferedImage handle(Handle h) {
        int[] c = WOOD.get(h);
        String[] mask = new String[16];
        for (int y = 0; y < 16; y++) {
            StringBuilder row = new StringBuilder(HANDLE[y]);
            for (int x = 0; x < 16; x++) if (HEAD[y].charAt(x) != '.') row.setCharAt(x, '.');
            mask[y] = row.toString();
        }
        return draw(mask, Map.of('o', c[0], 'd', c[0], 'l', c[1]));
    }

    public static BufferedImage head(Head h) {
        int[] c = METAL.get(h);
        return draw(HEAD, Map.of('o', c[0], 's', c[1], 'h', c[2], 'l', c[3]));
    }

    static BufferedImage draw(String[] mask, Map<Character, Integer> colors) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                Integer c = colors.get(mask[y].charAt(x));
                if (c != null) img.setRGB(x, y, 0xFF000000 | c);
            }
        return img;
    }
}

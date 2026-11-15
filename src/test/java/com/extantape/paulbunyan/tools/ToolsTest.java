package com.extantape.paulbunyan.tools;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.tree.Pos;
import com.extantape.paulbunyan.tree.Species;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ToolsTest {
    @Test
    void elevenHeadsAndElevenHandlesMakeEveryPairing() {
        assertEquals(11, Head.values().length);
        assertEquals(11, Handle.values().length);
        for (Handle h : Handle.values()) for (Head d : Head.values()) {
            AxeKind k = new AxeKind(h, d);
            assertEquals(k, AxeKind.parse(k.id()));
        }
        assertNull(AxeKind.parse("oak"));
        assertNull(AxeKind.parse("oak/diamond"), "only the eleven heads");
        assertNull(AxeKind.parse(null));
    }

    @Test
    void thePerksThatAreNumbers() {
        assertEquals(250, Head.COPPER.uses);
        assertEquals(333, Head.IRON.uses, "a quarter slower to wear");
        assertEquals(500, Head.OBSIDIAN.uses, "half as fast to wear");
        assertTrue(Head.GOLD.speed > Head.COPPER.speed && Head.GOLD.uses < Head.COPPER.uses, "fastest, and quickest to wear out");
        assertEquals(2 * Head.QUARTZ.speed, Head.QUARTZ.stemSpeed());
        assertEquals(Head.IRON.speed, Head.IRON.stemSpeed());
        for (Head h : Head.values()) assertFalse(h.perk.isBlank());
    }

    @Test
    void itBucksThreeAndFiveOnItsOwnWood() {
        AxeKind k = new AxeKind(Handle.BIRCH, Head.COPPER);
        assertEquals(5, k.bucks(Species.BIRCH));
        assertEquals(3, k.bucks(Species.OAK));
        assertEquals(3, k.bucks(null));
    }

    @Test
    void bucksAlongTheTrunkNearestFirstAndStopsAtAGap() {
        // Logs along x from -3 to 4, with a gap at 2.
        Set<Pos> trunk = Set.of(new Pos(-3, 0, 0), new Pos(-2, 0, 0), new Pos(-1, 0, 0), new Pos(1, 0, 0), new Pos(3, 0, 0), new Pos(4, 0, 0));
        assertEquals(List.of(new Pos(1, 0, 0), new Pos(-1, 0, 0)), Bucking.line(new Pos(0, 0, 0), 'x', trunk::contains, 2));
        assertEquals(List.of(new Pos(1, 0, 0), new Pos(-1, 0, 0), new Pos(-2, 0, 0), new Pos(-3, 0, 0)),
            Bucking.line(new Pos(0, 0, 0), 'x', trunk::contains, 4), "past the gap on one side it keeps going on the other");
        assertEquals(List.of(), Bucking.line(new Pos(0, 0, 0), 'z', trunk::contains, 4), "only along the log's own axis");
        assertEquals(List.of(), Bucking.line(new Pos(0, 0, 0), 'x', trunk::contains, 0));
    }

    @Test
    void emeraldDoublesSaplingsAndFruitOnly() {
        assertTrue(Perks.doubles("OAK_SAPLING") && Perks.doubles("MANGROVE_PROPAGULE") && Perks.doubles("APPLE"));
        assertFalse(Perks.doubles("STICK") || Perks.doubles("OAK_LEAVES"));
        assertEquals(1, Perks.xp(1));
        assertEquals(10, Perks.xp(30));
    }

    @Test
    void digSpeedCombinesFallenAndOwnWood() {
        assertEquals(1.0, com.extantape.paulbunyan.DigSpeedFactor.of(false, 0.5, false));
        assertEquals(0.5, com.extantape.paulbunyan.DigSpeedFactor.of(true, 0.5, false));
        assertEquals(1.5, com.extantape.paulbunyan.DigSpeedFactor.of(false, 0.5, true));
        assertEquals(0.75, com.extantape.paulbunyan.DigSpeedFactor.of(true, 0.5, true));
    }

    @Test
    void thePackIsTheSameEveryTimeAndHasEveryLayer() throws Exception {
        PackBuilder.Built a = PackBuilder.build(), b = PackBuilder.build();
        assertArrayEquals(a.zip(), b.zip());
        assertEquals(a.sha1(), b.sha1());
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(a.zip()))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) files.put(e.getName(), z.readAllBytes());
        }
        assertTrue(new String(files.get("pack.mcmeta"), StandardCharsets.UTF_8).contains("\"min_format\": 84"));
        String item = new String(files.get("assets/paulbunyan/items/axe.json"), StandardCharsets.UTF_8);
        for (Handle h : Handle.values()) {
            assertTrue(item.contains("\"when\": \"" + h.id() + "\""), h.id());
            var img = ImageIO.read(new ByteArrayInputStream(files.get("assets/paulbunyan/textures/item/axe/handle/" + h.id() + ".png")));
            assertEquals(16, img.getWidth());
            assertEquals(16, img.getHeight());
            assertNotNull(files.get("assets/paulbunyan/models/item/axe/handle/" + h.id() + ".json"));
        }
        for (Head h : Head.values()) {
            assertTrue(item.contains("\"when\": \"" + h.id() + "\""), h.id());
            assertNotNull(files.get("assets/paulbunyan/textures/item/axe/head/" + h.id() + ".png"));
            assertNotNull(files.get("assets/paulbunyan/models/item/axe/head/" + h.id() + ".json"));
        }
        for (String name : files.keySet()) assertTrue(name.equals("pack.mcmeta") || name.startsWith("assets/paulbunyan/"), "only its own namespace: " + name);
    }

    @Test
    void handleAndHeadNeverDrawTheSamePixel() {
        var handle = Textures.handle(Handle.OAK);
        var head = Textures.head(Head.IRON);
        int handlePixels = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            boolean a = (handle.getRGB(x, y) >>> 24) != 0, b = (head.getRGB(x, y) >>> 24) != 0;
            assertFalse(a && b, "both at " + x + "," + y);
            if (a) handlePixels++;
        }
        assertTrue(handlePixels > 10, "the handle still shows");
    }
}

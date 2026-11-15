package com.extantape.paulbunyan.tools;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.paulbunyan.fall.Direction;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class MoreToolsTest {
    @Test
    void aTreeFallsStraightAwayFromItsWedge() {
        assertEquals(Direction.EAST, Wedges.away(BlockFace.WEST));
        assertEquals(Direction.WEST, Wedges.away(BlockFace.EAST));
        assertEquals(Direction.SOUTH, Wedges.away(BlockFace.NORTH));
        assertEquals(Direction.NORTH, Wedges.away(BlockFace.SOUTH));
        assertNull(Wedges.away(BlockFace.UP), "a wedge goes in the side of a trunk");
    }

    @Test
    void aSmallWedgeFitsOneBlockAndALargeOneATwoByTwo() {
        assertTrue(Wedges.fits(Tool.SMALL_WEDGE, 1));
        assertFalse(Wedges.fits(Tool.SMALL_WEDGE, 4));
        assertTrue(Wedges.fits(Tool.LARGE_WEDGE, 4));
        assertFalse(Wedges.fits(Tool.LARGE_WEDGE, 1));
        assertFalse(Wedges.fits(Tool.LARGE_WEDGE, 2), "an odd-shaped trunk takes neither");
        assertFalse(Wedges.fits(Tool.CROSSCUT_SAW, 1));
    }

    @Test
    void sawStrokesKeepARhythmBetweenTwoSawyers() {
        assertTrue(Saw.inRhythm(true, Saw.RHYTHM_MIN));
        assertTrue(Saw.inRhythm(true, Saw.RHYTHM_MAX));
        assertFalse(Saw.inRhythm(true, Saw.RHYTHM_MIN - 1), "too quick");
        assertFalse(Saw.inRhythm(true, Saw.RHYTHM_MAX + 1), "too slow");
        assertFalse(Saw.inRhythm(false, 10), "the same sawyer twice");
    }

    @Test
    void sawyersMustStandOnOppositeSides() {
        assertTrue(Saw.opposite(0, -2, 0, 2, 0, 0));
        assertTrue(Saw.opposite(-2, 0, 2, 0.5, 0, 0));
        assertFalse(Saw.opposite(1, -2, -1, -2, 0, 0), "both on the north side");
    }

    @Test
    void aLogRollsAcrossItsLengthAwayFromThePusher() {
        assertArrayEquals(new int[] {0, 1}, Peavey.roll('x', 0.5, 0.5, 0.5, -1), "pushed from the north, it rolls south");
        assertArrayEquals(new int[] {0, -1}, Peavey.roll('x', 0.5, 0.5, 0.5, 2));
        assertArrayEquals(new int[] {1, 0}, Peavey.roll('z', 0.5, 0.5, -1, 0.5));
        assertArrayEquals(new int[] {-1, 0}, Peavey.roll('z', 0.5, 0.5, 2, 0.5));
        assertNull(Peavey.roll('y', 0.5, 0.5, 0, 0), "a log on end doesn't roll");
    }

    @Test
    void thePackDrawsEveryTool() throws Exception {
        Set<String> names = new HashSet<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(PackBuilder.build().zip()))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) names.add(e.getName());
        }
        for (Tool t : Tool.values()) {
            assertTrue(names.contains("assets/paulbunyan/items/" + t.id() + ".json"), t.id());
            assertTrue(names.contains("assets/paulbunyan/textures/item/" + t.id() + ".png"), t.id());
            assertEquals(16, Textures.tool(t).getWidth());
        }
    }

    @Test
    void wedgesStackAndTheSawAndPeaveyWear() {
        assertTrue(Tool.SMALL_WEDGE.stack > 1 && Tool.SMALL_WEDGE.uses == 0);
        assertEquals(1, Tool.CROSSCUT_SAW.stack);
        assertTrue(Tool.CROSSCUT_SAW.uses > 0 && Tool.PEAVEY.uses > 0);
        for (Tool t : Tool.values()) assertEquals(t, Tool.byId(t.id()));
    }
}

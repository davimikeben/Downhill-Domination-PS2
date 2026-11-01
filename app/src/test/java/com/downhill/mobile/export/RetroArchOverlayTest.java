package com.downhill.mobile.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.pad.Ps2Button;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Locks the exported RetroArch overlay format down. */
public class RetroArchOverlayTest {

    private static final Pattern DESC = Pattern.compile(
            "^overlay0_desc(\\d+) = \"([0-9.]+),([0-9.]+),(rect|radial),([0-9.]+),([0-9.]+),(\\w+)\"$");

    private static PadGeometry geo() {
        return PadGeometry.compute(PadBitmap.WIDTH, PadBitmap.HEIGHT, 2.0f, true, 1f, null);
    }

    @Test
    public void headerCarriesTheImageAndTheDescCount() {
        PadGeometry g = geo();
        String cfg = RetroArchOverlay.build(g, PadBitmap.WIDTH, PadBitmap.HEIGHT);
        assertTrue(cfg.contains("overlays = 1"));
        assertTrue(cfg.contains("overlay0_overlay = " + RetroArchOverlay.PNG_NAME));
        assertTrue(cfg.contains("overlay0_full_screen = true"));
        assertTrue(cfg.contains("overlay0_descs = " + (g.boxes().size() + 2)));
    }

    @Test
    public void everyDescriptorIsWellFormedAndInsideTheScreen() {
        String cfg = RetroArchOverlay.build(geo(), PadBitmap.WIDTH, PadBitmap.HEIGHT);
        int descs = 0;
        Set<String> mappings = new HashSet<>();
        for (String line : cfg.split("\n")) {
            if (!line.startsWith("overlay0_desc")
                    || line.charAt("overlay0_desc".length()) == 's') {
                continue;
            }
            Matcher m = DESC.matcher(line);
            assertTrue("bad line: " + line, m.matches());
            for (int i = 2; i <= 6; i++) {
                if (i == 4) {
                    continue;
                }
                float v = Float.parseFloat(m.group(i));
                assertTrue("out of range in " + line, v >= 0f && v <= 1f);
            }
            mappings.add(m.group(7));
            descs++;
        }
        assertEquals("descriptor count must match the header", geo().boxes().size() + 2, descs);
        assertEquals("mappings must be unique", descs, mappings.size());
        assertTrue(mappings.contains("analog_left"));
        assertTrue(mappings.contains("analog_right"));
        assertTrue(mappings.contains("btn_start"));
        assertTrue(mappings.contains("btn_select"));
    }

    @Test
    public void dualshockFaceButtonsUseTheRetroPadLayout() {
        Map<Ps2Button, String> m = RetroArchOverlay.mappings();
        assertEquals("btn_b", m.get(Ps2Button.CROSS));   // south = Cross
        assertEquals("btn_a", m.get(Ps2Button.CIRCLE));  // east  = Circle
        assertEquals("btn_y", m.get(Ps2Button.SQUARE));  // west  = Square
        assertEquals("btn_x", m.get(Ps2Button.TRIANGLE)); // north = Triangle
        assertEquals("btn_l", m.get(Ps2Button.L1));
        assertEquals("btn_r", m.get(Ps2Button.R1));
        assertEquals("btn_l2", m.get(Ps2Button.L2));
        assertEquals("btn_r2", m.get(Ps2Button.R2));
        assertEquals("btn_l3", m.get(Ps2Button.L3));
        assertEquals("btn_r3", m.get(Ps2Button.R3));
        assertEquals(21, m.size());
    }

    @Test
    public void utilityKeysAreReportedAsHotkeys() {
        Map<Ps2Button, String> m = RetroArchOverlay.mappings();
        assertTrue(RetroArchOverlay.isHotkey(m.get(Ps2Button.SAVE)));
        assertTrue(RetroArchOverlay.isHotkey(m.get(Ps2Button.FF)));
        assertFalse(RetroArchOverlay.isHotkey(m.get(Ps2Button.CROSS)));
        assertFalse(RetroArchOverlay.isHotkey(m.get(Ps2Button.START)));
    }

    @Test
    public void readmeExplainsEveryKey() {
        String readme = RetroArchOverlay.readme();
        for (Ps2Button b : Ps2Button.values()) {
            assertTrue("missing " + b, readme.contains(b.emoji));
            assertTrue("missing label " + b, readme.contains(b.shortLabel));
        }
        assertTrue(readme.contains("RetroArch"));
    }

    @Test
    public void emptyViewportIsRejected() {
        try {
            RetroArchOverlay.build(geo(), 0f, 100f);
            throw new AssertionError("must reject a zero width");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}

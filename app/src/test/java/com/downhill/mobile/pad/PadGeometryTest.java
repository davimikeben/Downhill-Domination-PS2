package com.downhill.mobile.pad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Locks the touch layout down: whatever the phone size is, every key must be on
 * screen, reachable, and must never steal another key's touch.
 */
public class PadGeometryTest {

    private static final float[][] DEVICES = {
            // widthPx, heightPx, density, landscape(1/0)
            {2400f, 1080f, 2.75f, 1f},   // 1080p phone, landscape
            {2160f, 1080f, 2.0f, 1f},    // 18:9 phone
            {1280f, 720f, 2.0f, 1f},     // small/cheap phone (Mali-G52 class)
            {1600f, 720f, 1.5f, 1f},     // low dpi
            {2560f, 1600f, 2.0f, 1f},    // tablet
            {1080f, 2400f, 2.75f, 0f},   // portrait phone
            {720f, 1600f, 2.0f, 0f},     // portrait cheap phone
            {800f, 480f, 1.0f, 1f},      // worst case
    };

    private static PadGeometry geo(float w, float h, float d, boolean land) {
        return PadGeometry.compute(w, h, d, land, 1f, null);
    }

    @Test
    public void everyKeyExistsAndIsOnScreen() {
        for (float[] dev : DEVICES) {
            boolean land = dev[3] > 0f;
            PadGeometry g = geo(dev[0], dev[1], dev[2], land);
            String where = "device " + dev[0] + "x" + dev[1] + " land=" + land;
            for (Ps2Button b : Ps2Button.values()) {
                PadBox box = g.box(b);
                assertNotNull(where + " missing " + b, box);
                assertTrue(where + " " + b + " off left " + box, box.x >= 0f);
                assertTrue(where + " " + b + " off top " + box, box.y >= 0f);
                assertTrue(where + " " + b + " off right " + box, box.right() <= dev[0] + 0.51f);
                assertTrue(where + " " + b + " off bottom " + box, box.bottom() <= dev[1] + 0.51f);
                assertTrue(where + " " + b + " too small " + box, box.w > 12f && box.h > 12f);
            }
            assertNotNull(g.leftStick());
            assertNotNull(g.rightStick());
            assertTrue(where + " left stick on screen", g.leftStick().cx - g.leftStick().radius >= -0.51f);
            assertTrue(where + " right stick on screen",
                    g.rightStick().cx + g.rightStick().radius <= dev[0] + 0.51f);
        }
    }

    @Test
    public void keysNeverOverlapEachOther() {
        for (float[] dev : DEVICES) {
            boolean land = dev[3] > 0f;
            PadGeometry g = geo(dev[0], dev[1], dev[2], land);
            List<Ps2Button> all = new ArrayList<>(g.boxes().keySet());
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    PadBox a = g.box(all.get(i));
                    PadBox b = g.box(all.get(j));
                    assertTrue("overlap " + all.get(i) + " x " + all.get(j) + " on "
                                    + dev[0] + "x" + dev[1] + " (" + a + " " + b + ")",
                            !a.overlaps(b, 0.51f));
                }
            }
        }
    }

    @Test
    public void sticksDoNotFightWithKeysOrEachOther() {
        for (float[] dev : DEVICES) {
            boolean land = dev[3] > 0f;
            PadGeometry g = geo(dev[0], dev[1], dev[2], land);
            for (Ps2Button b : Ps2Button.values()) {
                assertTrue("stick overlaps " + b + " on " + dev[0] + "x" + dev[1],
                        g.leftStick().apart(g.box(b), 0.5f));
                assertTrue("stick overlaps " + b + " on " + dev[0] + "x" + dev[1],
                        g.rightStick().apart(g.box(b), 0.5f));
            }
            assertTrue("sticks overlap on " + dev[0] + "x" + dev[1],
                    !g.leftStick().overlaps(g.rightStick()));
        }
    }

    @Test
    public void hitTestFindsTheKeyUnderTheFinger() {
        PadGeometry g = geo(2400f, 1080f, 2.75f, true);
        for (Ps2Button b : Ps2Button.values()) {
            PadBox box = g.box(b);
            assertEquals(b, g.hitTest(box.cx(), box.cy()));
            assertEquals(b, g.hitTest(box.x + 1.5f, box.y + 1.5f));
            assertEquals(b, g.hitTest(box.right() - 1.5f, box.bottom() - 1.5f));
        }
        assertNull(g.hitTest(1200f, 40f));
    }

    @Test
    public void landscapeKeepsThumbsOnTheirOwnSide() {
        PadGeometry g = geo(2400f, 1080f, 2.75f, true);
        for (Ps2Button b : Ps2Button.of(Ps2Button.Group.DPAD)) {
            assertTrue(b + " must be on the left half", g.box(b).right() < 2400f * 0.5f);
        }
        for (Ps2Button b : Ps2Button.of(Ps2Button.Group.FACE)) {
            assertTrue(b + " must be on the right half", g.box(b).x > 2400f * 0.5f);
        }
        assertTrue("left shoulder top-left", g.box(Ps2Button.L1).x < 2400f * 0.25f);
        assertTrue("right shoulder top-right", g.box(Ps2Button.R1).right() > 2400f * 0.75f);
        assertTrue("left stick bottom-left", g.leftStick().cx < 2400f * 0.5f);
        assertTrue("right stick bottom-right", g.rightStick().cx > 2400f * 0.5f);
    }

    @Test
    public void stickHitTestAndVectorMath() {
        PadGeometry g = geo(2400f, 1080f, 2.75f, true);
        StickBox left = g.leftStick();
        assertEquals(PadGeometry.STICK_LEFT, g.hitStick(left.cx, left.cy));
        assertEquals(PadGeometry.STICK_RIGHT, g.hitStick(g.rightStick().cx, g.rightStick().cy));
        assertEquals(PadGeometry.STICK_NONE, g.hitStick(1200f, 540f));

        float[] centre = PadGeometry.stickVector(left, left.cx, left.cy, 0.12f);
        assertEquals(0f, centre[0], 0.001f);
        assertEquals(0f, centre[1], 0.001f);

        float[] full = PadGeometry.stickVector(left, left.cx + left.radius, left.cy, 0.12f);
        assertEquals(1f, full[0], 0.001f);
        assertEquals(0f, full[1], 0.001f);

        float[] over = PadGeometry.stickVector(left, left.cx + left.radius * 4f, left.cy, 0.12f);
        assertEquals(1f, over[0], 0.001f);

        float[] half = PadGeometry.stickVector(left, left.cx + left.radius * 0.5f, left.cy, 0.0f);
        assertEquals(0.5f, half[0], 0.001f);
    }

    @Test
    public void userScaleAndOffsetsOnlyMoveKeysInsideTheScreen() {
        Map<Ps2Button, float[]> offsets = new EnumMap<>(Ps2Button.class);
        offsets.put(Ps2Button.CROSS, new float[]{0f, -24f});
        PadGeometry base = PadGeometry.compute(2400f, 1080f, 2.75f, true, 1f, null);
        PadGeometry moved = PadGeometry.compute(2400f, 1080f, 2.75f, true, 1f, offsets);
        assertTrue("offset must lift the key",
                moved.box(Ps2Button.CROSS).cy() < base.box(Ps2Button.CROSS).cy());
        assertEquals(base.box(Ps2Button.TRIANGLE).cy(), moved.box(Ps2Button.TRIANGLE).cy(), 0.001f);

        PadGeometry big = PadGeometry.compute(2400f, 1080f, 2.75f, true, 1.4f, null);
        assertTrue(big.box(Ps2Button.CROSS).w > base.box(Ps2Button.CROSS).w);
        PadGeometry small = PadGeometry.compute(2400f, 1080f, 2.75f, true, 0.7f, null);
        assertTrue(small.box(Ps2Button.CROSS).w < base.box(Ps2Button.CROSS).w);
        // scaling down keeps the pad inside the surface
        assertTrue(small.box(Ps2Button.CROSS).right() <= 2400f);
        assertTrue(small.box(Ps2Button.CROSS).bottom() <= 1080f);
    }
}

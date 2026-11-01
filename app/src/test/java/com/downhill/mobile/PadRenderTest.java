package com.downhill.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.downhill.mobile.export.PadBitmap;
import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.pad.PadPainter;
import com.downhill.mobile.pad.Ps2Button;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Rasterises the pad with real Skia and inspects the pixels, so the drawing code
 * is proven to actually paint keys (not just to run without throwing).
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w2400dp-h1080dp-land-mdpi")
public class PadRenderTest {

    private static final int W = 1920;
    private static final int H = 1080;

    @Test
    public void padPaintsKeysAndSticks() {
        Bitmap bmp = PadBitmap.render();
        assertEquals(W, bmp.getWidth());
        assertEquals(H, bmp.getHeight());

        Set<Integer> colours = new HashSet<>();
        int lit = 0;
        for (int y = 0; y < H; y += 3) {
            for (int x = 0; x < W; x += 3) {
                int c = bmp.getPixel(x, y);
                colours.add(c);
                if ((c >>> 24) > 0x80) {
                    lit++;
                }
            }
        }
        assertTrue("the pad must use many tones, got " + colours.size(), colours.size() > 120);
        assertTrue("something must be painted", lit > 5000);

        // every key box must contain painted pixels
        PadGeometry geo = PadGeometry.compute(W, H, 2.0f, true, 1f, null);
        for (Ps2Button b : Ps2Button.values()) {
            int x = (int) geo.box(b).cx();
            int y = (int) geo.box(b).cy();
            int c = bmp.getPixel(x, y);
            assertTrue(b + " is invisible at its centre", (c >>> 24) > 0x40);
        }

        save(bmp, "build/pad-overlay.png");
        save(PadBitmap.renderPreview(EnumSet.noneOf(Ps2Button.class), null, null),
                "build/pad-preview.png");
    }

    @Test
    public void pressedKeysLookDifferentFromIdleOnes() {
        PadGeometry geo = PadGeometry.compute(W, H, 2.0f, true, 1f, null);
        Bitmap idle = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        new PadPainter().draw(new Canvas(idle), geo, EnumSet.noneOf(Ps2Button.class), null, null);

        Bitmap pressed = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        new PadPainter().draw(new Canvas(pressed), geo, EnumSet.of(Ps2Button.CROSS),
                new float[]{0.7f, -0.4f}, null);

        // compare whole keys, not a single pixel: the glyph in the middle keeps its colour
        assertTrue("the pressed key must change", brightness(pressed, geo.box(Ps2Button.CROSS))
                != brightness(idle, geo.box(Ps2Button.CROSS)));
        assertEquals("only the pressed key may change",
                brightness(idle, geo.box(Ps2Button.TRIANGLE)),
                brightness(pressed, geo.box(Ps2Button.TRIANGLE)));
        save(PadBitmap.renderPreview(EnumSet.of(Ps2Button.CROSS, Ps2Button.DPAD_LEFT),
                        new float[]{0.65f, -0.45f}, new float[]{-0.3f, 0.4f}),
                "build/pad-pressed.png");
    }

    /** Sum of the alpha-weighted brightness of a key box. */
    private static long brightness(Bitmap bmp, com.downhill.mobile.pad.PadBox box) {
        long sum = 0;
        for (int y = (int) box.y; y < (int) box.bottom(); y += 2) {
            for (int x = (int) box.x; x < (int) box.right(); x += 2) {
                int c = bmp.getPixel(x, y);
                sum += ((c >>> 16) & 0xFF) + ((c >>> 8) & 0xFF) + (c & 0xFF) + (c >>> 24);
            }
        }
        return sum;
    }

    private static void save(Bitmap bmp, String path) {
        try {
            File f = new File(path);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            assertTrue(f.isFile() && f.length() > 1000);
        } catch (Exception e) {
            throw new AssertionError("could not save the preview: " + e.getMessage(), e);
        }
    }
}

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

        save(bmp, "build/pad-preview.png");
    }

    @Test
    public void pressedKeysLookDifferentFromIdleOnes() {
        PadGeometry geo = PadGeometry.compute(W, H, 2.0f, true, 1f, null);
        Bitmap idle = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        new PadPainter().draw(new Canvas(idle), geo, EnumSet.noneOf(Ps2Button.class), null, null);

        Bitmap pressed = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        new PadPainter().draw(new Canvas(pressed), geo, EnumSet.of(Ps2Button.CROSS),
                new float[]{0.7f, -0.4f}, null);

        int cross = idle.getPixel((int) geo.box(Ps2Button.CROSS).cx(),
                (int) geo.box(Ps2Button.CROSS).cy());
        int crossDown = pressed.getPixel((int) geo.box(Ps2Button.CROSS).cx(),
                (int) geo.box(Ps2Button.CROSS).cy());
        assertTrue("the pressed key must change colour", cross != crossDown);

        int triangle = idle.getPixel((int) geo.box(Ps2Button.TRIANGLE).cx(),
                (int) geo.box(Ps2Button.TRIANGLE).cy());
        int triangleStill = pressed.getPixel((int) geo.box(Ps2Button.TRIANGLE).cx(),
                (int) geo.box(Ps2Button.TRIANGLE).cy());
        assertEquals("only the pressed key may change", triangle, triangleStill);

        save(pressed, "build/pad-pressed.png");
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

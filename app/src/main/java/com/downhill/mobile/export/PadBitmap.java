package com.downhill.mobile.export;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.pad.PadPainter;
import com.downhill.mobile.pad.Ps2Button;

import java.util.Collections;
import java.util.Set;

/**
 * Renders the pad to the PNG that backs the exported RetroArch overlay, plus a
 * "preview" variant (dark frame instead of transparency) for docs and screenshots.
 */
public final class PadBitmap {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;

    private PadBitmap() {
    }

    /** Transparent-background pad: this is the file the overlay points at. */
    public static Bitmap render() {
        Bitmap bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        PadGeometry geo = geometry();
        PadPainter painter = new PadPainter();
        painter.drawBackdrop(canvas, WIDTH, HEIGHT);
        Set<Ps2Button> none = Collections.emptySet();
        painter.draw(canvas, geo, none, null, null);
        return bmp;
    }

    /** Same pad over a dark frame, so the design can be judged (and shipped as a preview). */
    public static Bitmap renderPreview(Set<Ps2Button> pressed, float[] left, float[] right) {
        Bitmap bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint sky = new Paint();
        sky.setShader(new LinearGradient(0f, 0f, 0f, HEIGHT,
                new int[]{0xFF101A2B, 0xFF0B111B, 0xFF05080D},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, WIDTH, HEIGHT, sky);
        PadGeometry geo = geometry();
        new PadPainter().draw(canvas, geo, pressed, left, right);
        return bmp;
    }

    public static PadGeometry geometry() {
        // Reference density 2.0 -> the landscape mock lands at 1920x1080 exactly.
        return PadGeometry.compute(WIDTH, HEIGHT, 2.0f, true, 1f, null);
    }
}

package com.downhill.mobile.export;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.pad.PadPainter;
import com.downhill.mobile.pad.Ps2Button;

import java.util.Collections;
import java.util.Set;

/** Renders the pad to a PNG that matches the exported overlay coordinates. */
public final class PadBitmap {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;

    private PadBitmap() {
    }

    /**
     * @return a 1920x1080 bitmap with the pad on a transparent-to-dark scrim
     */
    public static Bitmap render() {
        Bitmap bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        // Reference density 2.0 -> the landscape mock lands at 1920x1080 exactly.
        PadGeometry geo = PadGeometry.compute(WIDTH, HEIGHT, 2.0f, true, 1f, null);
        PadPainter painter = new PadPainter();
        painter.drawBackdrop(canvas, WIDTH, HEIGHT);
        Set<Ps2Button> none = Collections.emptySet();
        painter.draw(canvas, geo, none, null, null);
        return bmp;
    }
}

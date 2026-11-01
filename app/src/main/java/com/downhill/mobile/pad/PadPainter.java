package com.downhill.mobile.pad;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Draws the virtual pad: one continuous cross d-pad, two clean analog sticks, the
 * DualShock face diamond, shoulder keys and a row of utility keys.
 *
 * <p>Look: dark glass keys, hairline rims, micro labels in caps and vector icons.
 * Everything is plain {@link Canvas} drawing (no bitmaps, no layout inflation), so
 * a frame stays cheap on a Mali-G52.</p>
 */
public final class PadPainter {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final Path scratch = new Path();

    private final Typeface labelFace = PadTheme.labelTypeface();

    public PadPainter() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        text.setTextAlign(Paint.Align.CENTER);
        glow.setStyle(Paint.Style.FILL);
    }

    /**
     * @param pressed keys currently held (may be empty)
     * @param left    normalized left stick vector or null
     * @param right   normalized right stick vector or null
     */
    public void draw(Canvas c, PadGeometry g, Set<Ps2Button> pressed, float[] left, float[] right) {
        drawDpad(c, g, pressed);
        drawStick(c, g, g.leftStick(), left, pressed != null
                && pressed.contains(Ps2Button.L3));
        drawStick(c, g, g.rightStick(), right, pressed != null
                && pressed.contains(Ps2Button.R3));
        for (Map.Entry<Ps2Button, PadBox> e : g.boxes().entrySet()) {
            Ps2Button b = e.getKey();
            boolean down = pressed != null && pressed.contains(b);
            if (b.group == Ps2Button.Group.FACE) {
                drawFace(c, g, b, e.getValue(), down);
            } else if (b.group != Ps2Button.Group.DPAD) {
                drawKey(c, g, b, e.getValue(), down);
            }
        }
    }

    // --------------------------------------------------------------- keys ---

    /** Rounded key: dark glass body, hairline rim and a micro label (utility keys add an icon). */
    private void drawKey(Canvas c, PadGeometry g, Ps2Button b, PadBox box, boolean down) {
        float radius = Math.min(box.h * 0.40f, g.dp(15f));
        keyBackground(c, g, box, radius, down);

        float labelSize = Math.max(g.dp(6.6f), Math.min(box.h * 0.30f, box.w * 0.19f));
        text.setTypeface(labelFace);
        text.setTextSize(labelSize);
        text.setLetterSpacing(0.07f);
        text.setColor(down ? PadTheme.TEXT : PadTheme.TEXT_DIM);
        float baseline = box.cy() + labelSize * 0.36f;
        String label = b.shortLabel.toUpperCase(Locale.ROOT);

        if (b.group == Ps2Button.Group.UTILITY) {
            // icon only: five pill keys in one row have no room for a word
            drawUtilityIcon(c, b, box.cx(), box.cy(), Math.min(box.h * 0.56f, box.w * 0.52f), down);
            return;
        }
        c.drawText(label, box.cx(), baseline, text);
    }

    /** Dark glass key: vertical gradient, hairline rim and a top lip. */
    private void keyBackground(Canvas c, PadGeometry g, PadBox box, float radius, boolean down) {
        rect.set(box.x, box.y, box.right(), box.bottom());
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0f, rect.top, 0f, rect.bottom,
                down ? PadTheme.KEY_TOP_PRESSED : PadTheme.KEY_TOP,
                down ? PadTheme.KEY_BOTTOM_PRESSED : PadTheme.KEY_BOTTOM,
                Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, radius, radius, fill);
        fill.setShader(null);

        stroke.setColor(down ? PadTheme.RIM_PRESSED : PadTheme.RIM);
        stroke.setStrokeWidth(g.dp(down ? 1.5f : 1f));
        c.drawRoundRect(rect, radius, radius, stroke);

        if (!down) {
            stroke.setColor(0x1FFFFFFF);
            stroke.setStrokeWidth(g.dp(0.8f));
            rect.set(box.x + g.dp(1.6f), box.y + g.dp(1.2f), box.right() - g.dp(1.6f),
                    box.bottom() - g.dp(2.4f));
            c.drawRoundRect(rect, radius * 0.92f, radius * 0.92f, stroke);
        }
    }

    // --------------------------------------------------------------- face ---

    /** The four PlayStation shapes, drawn as vectors so they stay crisp at any size. */
    private void drawFace(Canvas c, PadGeometry g, Ps2Button b, PadBox box, boolean down) {
        float cx = box.cx();
        float cy = box.cy();
        float r = Math.min(box.w, box.h) * 0.5f;

        if (down) {
            glow.setAlpha(255);
            glow.setShader(new RadialGradient(cx, cy, r * 1.35f,
                    new int[]{0x55FFFFFF, 0x14FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r * 1.35f, glow);
            glow.setShader(null);
        }

        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0f, cy - r, 0f, cy + r,
                down ? 0xFF232B3A : 0xFF141A26, 0xFF080B12, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, fill);
        fill.setShader(null);

        stroke.setColor(PadTheme.faceColor(b));
        stroke.setStrokeWidth(g.dp(down ? 2.2f : 1.6f));
        c.drawCircle(cx, cy, r - g.dp(0.8f), stroke);

        drawShape(c, g, b, cx, cy, r * 0.42f, down);
    }

    private void drawShape(Canvas c, PadGeometry g, Ps2Button b, float cx, float cy, float r,
                           boolean down) {
        int colour = PadTheme.faceColor(b);
        fill.setAlpha(255);
        fill.setColor(colour);

        switch (b) {
            case TRIANGLE:
                path.reset();
                path.moveTo(cx, cy - r);
                path.lineTo(cx + r * 0.90f, cy + r * 0.70f);
                path.lineTo(cx - r * 0.90f, cy + r * 0.70f);
                path.close();
                c.drawPath(path, fill);
                break;
            case CIRCLE:
                stroke.setColor(colour);
                stroke.setStrokeWidth(g.dp(3.4f));
                c.drawCircle(cx, cy, r * 0.80f, stroke);
                break;
            case SQUARE:
                rect.set(cx - r * 0.76f, cy - r * 0.76f, cx + r * 0.76f, cy + r * 0.76f);
                c.drawRoundRect(rect, g.dp(2.2f), g.dp(2.2f), fill);
                break;
            default:
                crossPath(cx, cy, r * 0.94f, r * 0.36f);
                c.drawPath(path, fill);
                break;
        }
    }

    /** Thick X built from one closed path (two crossing bars would show the seam). */
    private void crossPath(float cx, float cy, float arm, float thick) {
        float t = thick * 0.5f;
        path.reset();
        path.moveTo(cx - arm, cy - arm + thick);
        path.lineTo(cx - arm + thick, cy - arm);
        path.lineTo(cx, cy - t);
        path.lineTo(cx + arm - thick, cy - arm);
        path.lineTo(cx + arm, cy - arm + thick);
        path.lineTo(cx + t, cy);
        path.lineTo(cx + arm, cy + arm - thick);
        path.lineTo(cx + arm - thick, cy + arm);
        path.lineTo(cx, cy + t);
        path.lineTo(cx - arm + thick, cy + arm);
        path.lineTo(cx - arm, cy + arm - thick);
        path.lineTo(cx - t, cy);
        path.close();
    }

    // -------------------------------------------------------------- d-pad ---

    /** One continuous cross instead of four loose squares: how a real d-pad looks. */
    private void drawDpad(Canvas c, PadGeometry g, Set<Ps2Button> pressed) {
        PadBox up = g.box(Ps2Button.DPAD_UP);
        PadBox down = g.box(Ps2Button.DPAD_DOWN);
        PadBox left = g.box(Ps2Button.DPAD_LEFT);
        PadBox right = g.box(Ps2Button.DPAD_RIGHT);
        if (up == null || down == null || left == null || right == null) {
            return;
        }
        boolean any = pressed != null && (pressed.contains(Ps2Button.DPAD_UP)
                || pressed.contains(Ps2Button.DPAD_DOWN) || pressed.contains(Ps2Button.DPAD_LEFT)
                || pressed.contains(Ps2Button.DPAD_RIGHT));

        float radius = g.dp(11f);
        rect.set(left.x, left.y, right.right(), left.bottom());
        scratch.reset();
        scratch.addRoundRect(rect, radius, radius, Path.Direction.CW);
        rect.set(up.x, up.y, up.right(), down.bottom());
        path.reset();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        path.op(scratch, Path.Op.UNION);

        rect.set(left.x, up.y, right.right(), down.bottom());
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0f, rect.top, 0f, rect.bottom,
                any ? 0xFF1D2532 : 0xFF141A26, 0xFF080B12, Shader.TileMode.CLAMP));
        c.drawPath(path, fill);
        fill.setShader(null);
        stroke.setColor(any ? PadTheme.RIM_PRESSED : PadTheme.RIM);
        stroke.setStrokeWidth(g.dp(any ? 1.4f : 1f));
        c.drawPath(path, stroke);

        float cx = up.cx();
        float cy = left.cy();
        fill.setColor(0x26000000);
        c.drawCircle(cx, cy, up.w * 0.30f, fill);
        stroke.setColor(0x14FFFFFF);
        stroke.setStrokeWidth(g.dp(0.8f));
        c.drawCircle(cx, cy, up.w * 0.30f, stroke);

        chevron(c, up, 0f, -1f, pressed != null && pressed.contains(Ps2Button.DPAD_UP));
        chevron(c, down, 0f, 1f, pressed != null && pressed.contains(Ps2Button.DPAD_DOWN));
        chevron(c, left, -1f, 0f, pressed != null && pressed.contains(Ps2Button.DPAD_LEFT));
        chevron(c, right, 1f, 0f, pressed != null && pressed.contains(Ps2Button.DPAD_RIGHT));
    }

    private void chevron(Canvas c, PadBox box, float dx, float dy, boolean down) {
        float cx = box.cx();
        float cy = box.cy();
        float size = Math.min(box.w, box.h) * 0.19f;
        float wide = size * 1.10f;
        float depth = size * 0.90f;

        path.reset();
        if (dx == 0f) {
            path.moveTo(cx, cy + dy * depth);
            path.lineTo(cx + wide, cy - dy * depth * 0.50f);
            path.lineTo(cx - wide, cy - dy * depth * 0.50f);
        } else {
            path.moveTo(cx + dx * depth, cy);
            path.lineTo(cx - dx * depth * 0.50f, cy + wide);
            path.lineTo(cx - dx * depth * 0.50f, cy - wide);
        }
        path.close();
        fill.setColor(down ? PadTheme.ACCENT : 0xFFDAE4F0);
        c.drawPath(path, fill);
    }

    // ------------------------------------------------------------- sticks ---

    private void drawStick(Canvas c, PadGeometry g, StickBox s, float[] vec, boolean clicking) {
        glow.setAlpha(255);
        glow.setShader(new RadialGradient(s.cx, s.cy, s.radius,
                new int[]{0x3A000000, 0x14000000, 0x00000000},
                new float[]{0.55f, 0.85f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(s.cx, s.cy, s.radius, glow);
        glow.setShader(null);

        stroke.setColor(clicking ? PadTheme.RIM_PRESSED : 0x33FFFFFF);
        stroke.setStrokeWidth(g.dp(clicking ? 1.6f : 1f));
        c.drawCircle(s.cx, s.cy, s.radius, stroke);

        // four notches: the neutral position is obvious without any text
        stroke.setColor(0x22FFFFFF);
        stroke.setStrokeWidth(g.dp(1f));
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            float x1 = s.cx + (float) Math.cos(a) * s.radius * 0.86f;
            float y1 = s.cy + (float) Math.sin(a) * s.radius * 0.86f;
            float x2 = s.cx + (float) Math.cos(a) * s.radius;
            float y2 = s.cy + (float) Math.sin(a) * s.radius;
            c.drawLine(x1, y1, x2, y2, stroke);
        }

        float kx = s.cx + (vec == null ? 0f : vec[0]) * (s.radius - s.knobRadius) * 0.94f;
        float ky = s.cy + (vec == null ? 0f : vec[1]) * (s.radius - s.knobRadius) * 0.94f;

        glow.setAlpha(255);
        glow.setShader(new RadialGradient(kx, ky - s.knobRadius * 0.45f, s.knobRadius * 1.35f,
                new int[]{0xFF47546F, 0xFF232C3D, 0xFF0D1119},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(kx, ky, s.knobRadius, glow);
        glow.setShader(null);

        stroke.setColor(clicking ? PadTheme.RIM_PRESSED : 0x3DFFFFFF);
        stroke.setStrokeWidth(g.dp(1.1f));
        c.drawCircle(kx, ky, s.knobRadius, stroke);

        fill.setColor(0x12FFFFFF);
        c.drawCircle(kx, ky - s.knobRadius * 0.12f, s.knobRadius * 0.52f, fill);
    }

    // -------------------------------------------------------------- icons ---

    /** Small vector icon per utility key (an emoji sticker would look out of place). */
    private void drawUtilityIcon(Canvas c, Ps2Button b, float cx, float cy, float size,
                                 boolean down) {
        int colour = down ? PadTheme.ACCENT : 0xFFC6D2E4;
        fill.setColor(colour);
        stroke.setColor(colour);
        stroke.setStrokeWidth(Math.max(1f, size * 0.10f));
        float r = size * 0.5f;

        switch (b) {
            case TURBO:
                path.reset();
                path.moveTo(cx + r * 0.30f, cy - r);
                path.lineTo(cx - r * 0.55f, cy + r * 0.12f);
                path.lineTo(cx - r * 0.05f, cy + r * 0.12f);
                path.lineTo(cx - r * 0.30f, cy + r);
                path.lineTo(cx + r * 0.55f, cy - r * 0.16f);
                path.lineTo(cx + r * 0.05f, cy - r * 0.16f);
                path.close();
                c.drawPath(path, fill);
                break;
            case SAVE:
                rect.set(cx - r * 0.80f, cy - r * 0.80f, cx + r * 0.80f, cy + r * 0.80f);
                c.drawRoundRect(rect, r * 0.2f, r * 0.2f, stroke);
                rect.set(cx - r * 0.38f, cy - r * 0.74f, cx + r * 0.38f, cy - r * 0.12f);
                c.drawRect(rect, fill);
                rect.set(cx - r * 0.52f, cy + r * 0.14f, cx + r * 0.52f, cy + r * 0.74f);
                c.drawRect(rect, fill);
                break;
            case LOAD:
                path.reset();
                path.moveTo(cx - r * 0.85f, cy - r * 0.45f);
                path.lineTo(cx - r * 0.20f, cy - r * 0.45f);
                path.lineTo(cx - r * 0.02f, cy - r * 0.70f);
                path.lineTo(cx + r * 0.85f, cy - r * 0.70f);
                path.lineTo(cx + r * 0.85f, cy + r * 0.62f);
                path.lineTo(cx - r * 0.85f, cy + r * 0.62f);
                path.close();
                fill.setColor(0x00000000);
                c.drawPath(path, stroke);
                break;
            case FF:
                path.reset();
                for (int i = 0; i < 2; i++) {
                    float ox = cx - r * 0.62f + i * r * 0.78f;
                    path.moveTo(ox, cy - r * 0.68f);
                    path.lineTo(ox + r * 0.60f, cy);
                    path.lineTo(ox, cy + r * 0.68f);
                    path.close();
                }
                c.drawPath(path, fill);
                break;
            default:
                stroke.setStrokeWidth(Math.max(1.4f, size * 0.13f));
                for (int i = 0; i < 3; i++) {
                    float y = cy - r * 0.55f + i * r * 0.55f;
                    c.drawLine(cx - r * 0.85f, y, cx + r * 0.85f, y, stroke);
                    float knobX = cx + ((i == 1) ? r * 0.38f : -r * 0.42f);
                    fill.setColor(colour);
                    c.drawCircle(knobX, y, Math.max(1.6f, size * 0.15f), fill);
                }
                break;
        }
    }

    /** Soft vertical scrim behind the pad, used by the PNG export. */
    public void drawBackdrop(Canvas c, float width, float height) {
        fill.setAlpha(255);
        fill.setShader(new LinearGradient(0f, height * 0.55f, 0f, height,
                new int[]{0x00060A12, 0x1A060A12, 0x4D060A12},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, width, height, fill);
        fill.setShader(null);
    }
}

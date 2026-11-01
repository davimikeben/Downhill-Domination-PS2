package com.downhill.mobile.pad;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.Map;
import java.util.Set;

/**
 * Draws the whole virtual pad. One painter is shared by the live view and by the
 * PNG exporter, so what the player sees is exactly what gets exported to the
 * emulator overlay.
 *
 * <p>Everything is drawn with plain {@link Canvas} primitives (no bitmaps, no
 * layout inflation), which keeps the frame cheap on a Mali-G52: only keys that
 * change state are redrawn.</p>
 */
public final class PadPainter {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    private final Typeface labelFace = PadTheme.labelTypeface();
    private final Typeface iconFace = PadTheme.emojiTypeface();

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
        for (Map.Entry<Ps2Button, PadBox> e : g.boxes().entrySet()) {
            Ps2Button b = e.getKey();
            boolean down = pressed != null && pressed.contains(b);
            if (b.group == Ps2Button.Group.FACE) {
                drawFace(c, g, b, e.getValue(), down);
            } else if (b.group == Ps2Button.Group.UTILITY) {
                drawPill(c, g, b, e.getValue(), down, 0.5f);
            } else {
                drawPill(c, g, b, e.getValue(), down, 0.34f);
            }
        }
        drawStick(c, g, g.leftStick(), left, Ps2Button.L3,
                pressed != null && pressed.contains(Ps2Button.L3));
        drawStick(c, g, g.rightStick(), right, Ps2Button.R3,
                pressed != null && pressed.contains(Ps2Button.R3));
    }

    // ---------------------------------------------------------------- keys --

    private void drawPill(Canvas c, PadGeometry g, Ps2Button b, PadBox box, boolean down,
                          float radiusFactor) {
        float r = Math.min(box.w, box.h) * radiusFactor;
        rect.set(box.x, box.y, box.right(), box.bottom());

        int top = down ? PadTheme.KEY_FILL_TOP_PRESSED : PadTheme.KEY_FILL_TOP;
        int bottom = down ? PadTheme.KEY_FILL_BOTTOM_PRESSED : PadTheme.KEY_FILL_BOTTOM;
        fill.setShader(new LinearGradient(0f, rect.top, 0f, rect.bottom, top, bottom,
                Shader.TileMode.CLAMP));
        fill.setAlpha(down ? 255 : 225);
        c.drawRoundRect(rect, r, r, fill);
        fill.setShader(null);

        int rim = down ? PadTheme.KEY_STROKE_PRESSED : PadTheme.faceColor(b);
        stroke.setColor(rim);
        stroke.setAlpha(down ? 255 : 200);
        stroke.setStrokeWidth(g.dp(down ? 1.9f : 1.2f));
        c.drawRoundRect(rect, r, r, stroke);

        if (!down) {
            stroke.setColor(0x40FFFFFF);
            stroke.setStrokeWidth(g.dp(0.7f));
            rect.set(box.x + g.dp(2.2f), box.y + g.dp(2.2f), box.right() - g.dp(2.2f),
                    box.bottom() - g.dp(2.2f));
            c.drawRoundRect(rect, r * 0.85f, r * 0.85f, stroke);
            rect.set(box.x, box.y, box.right(), box.bottom());
        }
        drawGlyphs(c, g, b, box, down);
    }

    private void drawFace(Canvas c, PadGeometry g, Ps2Button b, PadBox box, boolean down) {
        float cx = box.cx();
        float cy = box.cy();
        float r = Math.min(box.w, box.h) * 0.5f;

        if (down) {
            glow.setShader(new RadialGradient(cx, cy, r * 1.30f,
                    new int[]{0x77FF6B1A, 0x1EFF6B1A, 0x00FF6B1A},
                    new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r * 1.30f, glow);
            glow.setShader(null);
        }

        fill.setShader(new LinearGradient(0f, cy - r, 0f, cy + r,
                down ? 0xFF2A3242 : 0xFF1A2130, 0xFF0A0E16, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, fill);
        fill.setShader(null);

        stroke.setColor(PadTheme.faceColor(b));
        stroke.setStrokeWidth(g.dp(down ? 2.6f : 2.0f));
        stroke.setAlpha(down ? 255 : 230);
        c.drawCircle(cx, cy, r - g.dp(0.9f), stroke);

        drawFaceSymbol(c, g, b, cx, cy - r * 0.08f, r * 0.46f, down);

        // The name has to fit inside the circle: measure it and shrink to the chord
        // available at the baseline height instead of letting it spill over the rim.
        text.setTypeface(labelFace);
        text.setColor(down ? PadTheme.TEXT_PRIMARY : PadTheme.TEXT_SECONDARY);
        float wanted = Math.max(g.dp(5.5f), r * 0.26f);
        float baseline = cy + r * 0.58f;
        float chord = 2f * (float) Math.sqrt(Math.max(1f,
                r * r - (baseline - cy) * (baseline - cy)));
        text.setTextSize(wanted);
        float width = text.measureText(b.shortLabel);
        if (width > chord * 0.92f) {
            text.setTextSize(wanted * (chord * 0.92f) / width);
        }
        c.drawText(b.shortLabel, cx, baseline, text);
    }

    /**
     * Emoji on top, short name underneath. The emoji is what makes an unfamiliar
     * pad readable at a glance and the label removes the last doubt.
     */
    private void drawGlyphs(Canvas c, PadGeometry g, Ps2Button b, PadBox box, boolean down) {
        if (b.group == Ps2Button.Group.DPAD) {
            drawDpadArrow(c, g, b, box, down);
            return;
        }
        boolean roomForBoth = box.h > g.dp(34f) && box.w > g.dp(30f);
        float iconSize = roomForBoth ? Math.min(box.h * 0.42f, box.w * 0.38f)
                : Math.min(box.h * 0.62f, box.w * 0.55f);
        float cx = box.cx();
        float cy = box.cy();

        text.setTypeface(iconFace);
        text.setColor(PadTheme.TEXT_PRIMARY);
        text.setTextSize(iconSize);
        float emojiY = roomForBoth ? cy - box.h * 0.06f : cy + iconSize * 0.35f;
        c.drawText(b.emoji, cx, emojiY, text);

        if (roomForBoth) {
            text.setTypeface(labelFace);
            text.setColor(down ? PadTheme.TEXT_PRIMARY : PadTheme.TEXT_SECONDARY);
            text.setTextSize(Math.max(g.dp(6.4f), Math.min(box.h * 0.22f, box.w * 0.17f)));
            c.drawText(b.shortLabel, cx, cy + box.h * 0.30f, text);
        }
    }

    /** The four PlayStation shapes, drawn as vectors so they stay crisp at any size. */
    private void drawFaceSymbol(Canvas c, PadGeometry g, Ps2Button b, float cx, float cy,
                                float r, boolean down) {
        int colour = PadTheme.faceColor(b);
        fill.setColor(colour);
        fill.setAlpha(down ? 255 : 235);
        stroke.setColor(0x55000000);
        stroke.setStrokeWidth(g.dp(0.9f));

        switch (b) {
            case TRIANGLE:
                path.reset();
                path.moveTo(cx, cy - r);
                path.lineTo(cx + r * 0.92f, cy + r * 0.72f);
                path.lineTo(cx - r * 0.92f, cy + r * 0.72f);
                path.close();
                c.drawPath(path, fill);
                c.drawPath(path, stroke);
                break;
            case CIRCLE:
                if (down) {
                    c.drawCircle(cx, cy, r * 0.92f, fill);
                } else {
                    stroke.setColor(colour);
                    stroke.setStrokeWidth(g.dp(4.2f));
                    c.drawCircle(cx, cy, r * 0.72f, stroke);
                }
                break;
            case SQUARE:
                rect.set(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f);
                c.drawRoundRect(rect, g.dp(2.4f), g.dp(2.4f), fill);
                c.drawRoundRect(rect, g.dp(2.4f), g.dp(2.4f), stroke);
                rect.set(0f, 0f, 0f, 0f);
                break;
            default: {
                float arm = r * 0.86f;
                float t = r * 0.36f;
                path.reset();
                path.moveTo(cx - arm, cy - arm + t);
                path.lineTo(cx - arm + t, cy - arm);
                path.lineTo(cx, cy - t);
                path.lineTo(cx + arm - t, cy - arm);
                path.lineTo(cx + arm, cy - arm + t);
                path.lineTo(cx + t, cy);
                path.lineTo(cx + arm, cy + arm - t);
                path.lineTo(cx + arm - t, cy + arm);
                path.lineTo(cx, cy + t);
                path.lineTo(cx - arm + t, cy + arm);
                path.lineTo(cx - arm, cy + arm - t);
                path.lineTo(cx - t, cy);
                path.close();
                c.drawPath(path, fill);
                c.drawPath(path, stroke);
                break;
            }
        }
    }

    /** Crisp vector chevron for the d-pad (an emoji arrow looks out of place here). */
    private void drawDpadArrow(Canvas c, PadGeometry g, Ps2Button b, PadBox box, boolean down) {
        float cx = box.cx();
        float cy = box.cy() - box.h * 0.14f;
        float size = Math.min(box.w, box.h) * 0.26f;
        float half = size * 0.62f;
        float thick = Math.max(g.dp(2.6f), size * 0.42f);

        path.reset();
        switch (b) {
            case DPAD_UP:
                path.moveTo(cx, cy - size);
                path.lineTo(cx + half, cy + size * 0.55f);
                path.lineTo(cx - half, cy + size * 0.55f);
                break;
            case DPAD_DOWN:
                path.moveTo(cx, cy + size);
                path.lineTo(cx + half, cy - size * 0.55f);
                path.lineTo(cx - half, cy - size * 0.55f);
                break;
            case DPAD_LEFT:
                path.moveTo(cx - size, cy);
                path.lineTo(cx + size * 0.55f, cy - half);
                path.lineTo(cx + size * 0.55f, cy + half);
                break;
            default:
                path.moveTo(cx + size, cy);
                path.lineTo(cx - size * 0.55f, cy - half);
                path.lineTo(cx - size * 0.55f, cy + half);
                break;
        }
        path.close();
        fill.setColor(down ? PadTheme.KEY_STROKE_PRESSED : 0xFFBFEFE7);
        fill.setAlpha(down ? 255 : 235);
        c.drawPath(path, fill);

        stroke.setColor(0x6625E0C4);
        stroke.setStrokeWidth(Math.min(thick, g.dp(1.6f)));
        c.drawPath(path, stroke);

        text.setTypeface(labelFace);
        text.setColor(down ? PadTheme.TEXT_PRIMARY : PadTheme.TEXT_SECONDARY);
        text.setTextSize(Math.max(g.dp(5.6f), box.h * 0.17f));
        c.drawText(b.shortLabel, cx, box.bottom() - box.h * 0.09f, text);
    }

    // -------------------------------------------------------------- sticks --

    private void drawStick(Canvas c, PadGeometry g, StickBox s, float[] vec, Ps2Button click,
                           boolean clicking) {
        glow.setShader(new RadialGradient(s.cx, s.cy, s.radius * 1.25f,
                new int[]{clicking ? 0x66FF6B1A : 0x5525E0C4, 0x14000000, 0x00000000},
                new float[]{0f, 0.70f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(s.cx, s.cy, s.radius * 1.25f, glow);
        glow.setShader(null);

        fill.setColor(0xC0111826);
        fill.setAlpha(210);
        c.drawCircle(s.cx, s.cy, s.radius, fill);

        stroke.setColor(clicking ? PadTheme.KEY_STROKE_PRESSED : PadTheme.STICK_RING);
        stroke.setStrokeWidth(g.dp(1.4f));
        c.drawCircle(s.cx, s.cy, s.radius, stroke);

        stroke.setColor(0x33FFFFFF);
        stroke.setStrokeWidth(g.dp(0.8f));
        float tick = s.radius * 0.16f;
        c.drawLine(s.cx - tick, s.cy, s.cx + tick, s.cy, stroke);
        c.drawLine(s.cx, s.cy - tick, s.cx, s.cy + tick, stroke);

        float kx = s.cx + (vec == null ? 0f : vec[0]) * (s.radius - s.knobRadius) * 0.92f;
        float ky = s.cy + (vec == null ? 0f : vec[1]) * (s.radius - s.knobRadius) * 0.92f;

        glow.setShader(new RadialGradient(kx, ky - s.knobRadius * 0.4f, s.knobRadius * 1.4f,
                new int[]{0xFF4A5A78, 0xFF1B2436, 0xFF0C111A},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(kx, ky, s.knobRadius, glow);
        glow.setShader(null);

        stroke.setColor(0xAA25E0C4);
        stroke.setStrokeWidth(g.dp(1.2f));
        c.drawCircle(kx, ky, s.knobRadius, stroke);

        text.setTypeface(labelFace);
        text.setColor(PadTheme.TEXT_SECONDARY);
        text.setTextSize(Math.max(g.dp(6f), s.knobRadius * 0.62f));
        c.drawText(click.shortLabel, kx, ky + s.knobRadius * 0.24f, text);
    }

    /** Soft vertical scrim behind the pad, used by the PNG export. */
    public void drawBackdrop(Canvas c, float width, float height) {
        fill.setShader(new LinearGradient(0f, height * 0.50f, 0f, height,
                new int[]{0x000A0D14, 0x2E0A0D14, 0x690A0D14},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, width, height, fill);
        fill.setShader(null);
        fill.setAlpha(Color.alpha(PadTheme.GLOW));
    }
}

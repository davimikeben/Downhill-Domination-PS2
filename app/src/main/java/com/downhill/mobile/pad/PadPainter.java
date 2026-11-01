package com.downhill.mobile.pad;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
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
            glow.setShader(new RadialGradient(cx, cy, r * 1.55f,
                    new int[]{0x88FF6B1A, 0x22FF6B1A, 0x00FF6B1A},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r * 1.55f, glow);
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

        text.setTypeface(iconFace);
        text.setColor(PadTheme.TEXT_PRIMARY);
        text.setTextSize(r * (down ? 1.02f : 0.95f));
        c.drawText(b.emoji, cx, cy + r * 0.34f, text);

        text.setTypeface(labelFace);
        text.setColor(PadTheme.TEXT_SECONDARY);
        text.setTextSize(Math.max(g.dp(6f), r * 0.26f));
        c.drawText(b.shortLabel, cx, cy + r * 0.80f, text);
    }

    /**
     * Emoji on top, short name underneath. The emoji is what makes an unfamiliar
     * pad readable at a glance and the label removes the last doubt.
     */
    private void drawGlyphs(Canvas c, PadGeometry g, Ps2Button b, PadBox box, boolean down) {
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

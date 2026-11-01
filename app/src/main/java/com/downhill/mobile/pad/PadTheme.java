package com.downhill.mobile.pad;

import android.graphics.Typeface;

/**
 * Colours, radii and typefaces shared by the on-screen pad and the exported PNG,
 * so the two never drift apart.
 */
public final class PadTheme {

    public static final int KEY_STROKE = 0xFF25E0C4;
    public static final int KEY_STROKE_PRESSED = 0xFFFFFFFF;
    public static final int KEY_FILL_TOP = 0xFF20283A;
    public static final int KEY_FILL_BOTTOM = 0xFF0E1320;
    public static final int KEY_FILL_TOP_PRESSED = 0xFFFF6B1A;
    public static final int KEY_FILL_BOTTOM_PRESSED = 0xFFB23A00;
    public static final int DPAD_FILL_TOP = 0xFF1B2130;
    public static final int DPAD_FILL_BOTTOM = 0xFF0B0F18;
    public static final int FACE_TRIANGLE = 0xFF34D399;
    public static final int FACE_CIRCLE = 0xFFF87171;
    public static final int FACE_CROSS = 0xFF60A5FA;
    public static final int FACE_SQUARE = 0xFFF472B6;
    public static final int STICK_RING = 0x5525E0C4;
    public static final int STICK_BASE = 0xFF151B28;
    public static final int STICK_KNOB_TOP = 0xFF3B4A66;
    public static final int STICK_KNOB_BOTTOM = 0xFF151B28;
    public static final int TEXT_PRIMARY = 0xFFF5F7FB;
    public static final int TEXT_SECONDARY = 0xFF9AA6BF;
    public static final int GLOW = 0x33FF6B1A;

    private PadTheme() {
    }

    /** Colour of the rim of a face button (PS2 button colours). */
    public static int faceColor(Ps2Button b) {
        switch (b) {
            case TRIANGLE:
                return FACE_TRIANGLE;
            case CIRCLE:
                return FACE_CIRCLE;
            case CROSS:
                return FACE_CROSS;
            case SQUARE:
                return FACE_SQUARE;
            default:
                return KEY_STROKE;
        }
    }

    public static Typeface labelTypeface() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static Typeface emojiTypeface() {
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }
}

package com.downhill.mobile.pad;

import android.graphics.Typeface;

/**
 * Colours, typefaces and shared geometry of the on-screen pad. One palette for
 * the live view and the exported PNG so the two never drift apart.
 */
public final class PadTheme {

    public static final int KEY_TOP = 0xFF161C28;
    public static final int KEY_BOTTOM = 0xFF0B0F17;
    public static final int KEY_TOP_PRESSED = 0xFF2C3648;
    public static final int KEY_BOTTOM_PRESSED = 0xFF12161F;
    public static final int RIM = 0x47FFFFFF;
    public static final int RIM_PRESSED = 0xFF25E0C4;
    public static final int ACCENT = 0xFF25E0C4;
    public static final int ACCENT_SOFT = 0x5525E0C4;
    public static final int WARM = 0xFFFF6B1A;
    public static final int TEXT = 0xFFF2F5FA;
    public static final int TEXT_DIM = 0xFFA9B4C7;

    public static final int FACE_TRIANGLE = 0xFF34D399;
    public static final int FACE_CIRCLE = 0xFFF87171;
    public static final int FACE_CROSS = 0xFF60A5FA;
    public static final int FACE_SQUARE = 0xFFF472B6;

    private PadTheme() {
    }

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
                return ACCENT;
        }
    }

    public static Typeface labelTypeface() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    /** Font with colour emoji fallback, used for the small icon next to a label. */
    public static Typeface emojiTypeface() {
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }
}

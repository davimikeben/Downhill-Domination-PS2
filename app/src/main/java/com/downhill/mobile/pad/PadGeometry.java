package com.downhill.mobile.pad;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Turns an arbitrary viewport into pixel hit-boxes for every key.
 *
 * <p>Layouts are authored in a density independent reference canvas (like a design
 * mock at 1dp = 1 unit) and then scaled/anchored to the real surface. Two mocks
 * exist: landscape (870x390) and portrait (420x500). The layout is bottom anchored
 * so the thumbs always reach the keys and the top of the screen stays free for the
 * game image.</p>
 *
 * <p>Pure Java, no Android imports: the exact placement can be asserted by tests on
 * any device size without an emulator.</p>
 */
public final class PadGeometry {

    public static final float REF_LANDSCAPE_W = 870f;
    public static final float REF_LANDSCAPE_H = 390f;
    public static final float REF_PORTRAIT_W = 420f;
    public static final float REF_PORTRAIT_H = 500f;

    public static final int STICK_NONE = 0;
    public static final int STICK_LEFT = 1;
    public static final int STICK_RIGHT = 2;

    private final EnumMap<Ps2Button, PadBox> boxes = new EnumMap<>(Ps2Button.class);
    private final StickBox leftStick;
    private final StickBox rightStick;
    private final float unit;
    private final boolean landscape;
    private final float widthPx;
    private final float heightPx;

    private PadGeometry(float widthPx, float heightPx, boolean landscape, float unit,
                        float ox, float oy, Map<Ps2Button, float[]> offsetsDp) {
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.landscape = landscape;
        this.unit = unit;

        for (Ps2Button b : Ps2Button.values()) {
            float[] r = reference(b, landscape);
            if (r == null) {
                continue;
            }
            float dx = 0f;
            float dy = 0f;
            float[] off = offsetsDp == null ? null : offsetsDp.get(b);
            if (off != null) {
                dx = off[0];
                dy = off[1];
            }
            boxes.put(b, new PadBox(
                    ox + (r[0] + dx) * unit,
                    oy + (r[1] + dy) * unit,
                    r[2] * unit,
                    r[3] * unit));
        }

        float[] ls = landscape ? LEFT_STICK_LAND : LEFT_STICK_PORT;
        float[] rs = landscape ? RIGHT_STICK_LAND : RIGHT_STICK_PORT;
        float[] lo = offsetsDp == null ? null : offsetsDp.get(STICK_LEFT_KEY);
        float[] ro = offsetsDp == null ? null : offsetsDp.get(STICK_RIGHT_KEY);
        leftStick = new StickBox(
                ox + (ls[0] + (lo == null ? 0f : lo[0])) * unit,
                oy + (ls[1] + (lo == null ? 0f : lo[1])) * unit,
                ls[2] * unit, ls[3] * unit);
        rightStick = new StickBox(
                ox + (rs[0] + (ro == null ? 0f : ro[0])) * unit,
                oy + (rs[1] + (ro == null ? 0f : ro[1])) * unit,
                rs[2] * unit, rs[3] * unit);
    }

    /**
     * Builds the layout for a surface of {@code widthPx} x {@code heightPx}.
     *
     * @param density   display density (pixels per dp), usually {@code getResources().getDisplayMetrics().density}
     * @param landscape true for the wide mock, false for the tall one
     * @param userScale 1.0 = design size, clamped to 0.6 .. 1.6
     * @param offsetsDp per-key tweaks in dp (from the drag-to-place editor), may be null
     */
    public static PadGeometry compute(float widthPx, float heightPx, float density,
                                      boolean landscape, float userScale,
                                      Map<Ps2Button, float[]> offsetsDp) {
        float d = density <= 0f ? 1f : density;
        float refW = landscape ? REF_LANDSCAPE_W : REF_PORTRAIT_W;
        float refH = landscape ? REF_LANDSCAPE_H : REF_PORTRAIT_H;
        float fit = Math.min(widthPx / (refW * d), heightPx / (refH * d));
        fit = clamp(fit, 0.40f, 2.40f);
        float s = fit * clamp(userScale, 0.60f, 1.60f);
        float unit = d * s;
        float ox = (widthPx - refW * unit) * 0.5f;
        float oy = heightPx - refH * unit;
        return new PadGeometry(widthPx, heightPx, landscape, unit, ox, oy, offsetsDp);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public PadBox box(Ps2Button b) {
        return boxes.get(b);
    }

    public Map<Ps2Button, PadBox> boxes() {
        return Collections.unmodifiableMap(boxes);
    }

    public StickBox leftStick() {
        return leftStick;
    }

    public StickBox rightStick() {
        return rightStick;
    }

    public StickBox stick(int which) {
        return which == STICK_LEFT ? leftStick : (which == STICK_RIGHT ? rightStick : null);
    }

    public float unit() {
        return unit;
    }

    public boolean isLandscape() {
        return landscape;
    }

    /** dp value converted to pixels for the current scale (used for text sizes and strokes). */
    public float dp(float dp) {
        return dp * unit;
    }

    /** Returns the key under the finger, or null. */
    public Ps2Button hitTest(float px, float py) {
        for (Map.Entry<Ps2Button, PadBox> e : boxes.entrySet()) {
            if (e.getValue().contains(px, py)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Same as {@link #hitTest} but the box is grown by {@code slopPx} (finger wobble). */
    public Ps2Button hitTest(float px, float py, float slopPx) {
        Ps2Button exact = hitTest(px, py);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<Ps2Button, PadBox> e : boxes.entrySet()) {
            if (e.getValue().grow(slopPx).contains(px, py)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Returns {@link #STICK_LEFT} / {@link #STICK_RIGHT} / {@link #STICK_NONE}. */
    public int hitStick(float px, float py) {
        if (leftStick.contains(px, py)) {
            return STICK_LEFT;
        }
        if (rightStick.contains(px, py)) {
            return STICK_RIGHT;
        }
        return STICK_NONE;
    }

    /**
     * Normalized stick vector (-1..1 on both axes) for a touch position, with a
     * radial dead-zone so a resting thumb never drifts.
     */
    public static float[] stickVector(StickBox box, float px, float py, float deadZone) {
        float dx = (px - box.cx) / box.radius;
        float dy = (py - box.cy) / box.radius;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= deadZone) {
            return new float[]{0f, 0f};
        }
        if (len > 1f) {
            dx /= len;
            dy /= len;
        } else {
            float norm = (len - deadZone) / (1f - deadZone) / len;
            dx *= norm;
            dy *= norm;
        }
        return new float[]{dx, dy};
    }

    // ---------------------------------------------------------------------
    // Design reference tables, authored in dp on the mock canvases.
    // (x, y, width, height)                                         
    // ---------------------------------------------------------------------

    /** Pseudo key used only as an offset-table key for the two analog sticks. */
    public static final Ps2Button STICK_LEFT_KEY = Ps2Button.L3;
    /** Pseudo key used only as an offset-table key for the two analog sticks. */
    public static final Ps2Button STICK_RIGHT_KEY = Ps2Button.R3;

    private static final float[] LEFT_STICK_LAND = {204f, 72f, 58f, 32f};
    private static final float[] RIGHT_STICK_LAND = {666f, 72f, 58f, 32f};
    private static final float[] LEFT_STICK_PORT = {105f, 450f, 48f, 28f};
    private static final float[] RIGHT_STICK_PORT = {315f, 450f, 48f, 28f};

    private static float[] reference(Ps2Button b, boolean land) {
        if (land) {
            switch (b) {
                case L1: return new float[]{16f, 16f, 120f, 46f};
                case L2: return new float[]{16f, 68f, 120f, 42f};
                case R1: return new float[]{734f, 16f, 120f, 46f};
                case R2: return new float[]{734f, 68f, 120f, 42f};
                case SELECT: return new float[]{343f, 16f, 92f, 46f};
                case START: return new float[]{435f, 16f, 92f, 46f};
                case L3: return new float[]{16f, 330f, 74f, 44f};
                case R3: return new float[]{780f, 330f, 74f, 44f};
                case DPAD_UP: return new float[]{119f, 130f, 62f, 62f};
                case DPAD_DOWN: return new float[]{119f, 262f, 62f, 62f};
                case DPAD_LEFT: return new float[]{53f, 196f, 62f, 62f};
                case DPAD_RIGHT: return new float[]{185f, 196f, 62f, 62f};
                case TRIANGLE: return new float[]{688f, 131f, 64f, 64f};
                case CIRCLE: return new float[]{752f, 195f, 64f, 64f};
                case CROSS: return new float[]{688f, 259f, 64f, 64f};
                case SQUARE: return new float[]{624f, 195f, 64f, 64f};
                case TURBO: return new float[]{234f, 330f, 74f, 44f};
                case SAVE: return new float[]{316f, 330f, 74f, 44f};
                case LOAD: return new float[]{398f, 330f, 74f, 44f};
                case FF: return new float[]{480f, 330f, 74f, 44f};
                case MENU: return new float[]{562f, 330f, 74f, 44f};
                default: return null;
            }
        }
        switch (b) {
            case L1: return new float[]{10f, 10f, 96f, 40f};
            case L2: return new float[]{10f, 54f, 96f, 38f};
            case R1: return new float[]{314f, 10f, 96f, 40f};
            case R2: return new float[]{314f, 54f, 96f, 38f};
            case TURBO: return new float[]{14f, 110f, 74f, 40f};
            case SAVE: return new float[]{96f, 110f, 74f, 40f};
            case LOAD: return new float[]{178f, 110f, 74f, 40f};
            case FF: return new float[]{260f, 110f, 74f, 40f};
            case MENU: return new float[]{342f, 110f, 64f, 40f};
            case L3: return new float[]{14f, 160f, 88f, 40f};
            case SELECT: return new float[]{110f, 160f, 90f, 40f};
            case START: return new float[]{208f, 160f, 90f, 40f};
            case R3: return new float[]{306f, 160f, 88f, 40f};
            case DPAD_UP: return new float[]{77f, 212f, 56f, 56f};
            case DPAD_DOWN: return new float[]{77f, 332f, 56f, 56f};
            case DPAD_LEFT: return new float[]{17f, 272f, 56f, 56f};
            case DPAD_RIGHT: return new float[]{109f, 272f, 56f, 56f};
            case TRIANGLE: return new float[]{286f, 213f, 58f, 58f};
            case CIRCLE: return new float[]{344f, 271f, 58f, 58f};
            case CROSS: return new float[]{286f, 329f, 58f, 58f};
            case SQUARE: return new float[]{228f, 271f, 58f, 58f};
            default: return null;
        }
    }
}

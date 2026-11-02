package com.downhill.mobile.pad;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The virtual gamepad: glossy multi-touch keys, two analog sticks and every
 * DualShock 2 control, drawn on a single {@link Canvas}.
 *
 * <p>Input handling is pointer based: each finger owns the key (or the stick) it
 * landed on until it lifts, so sliding a thumb never swaps keys - the classic
 * reason on-screen pads feel wrong on cheap phones.</p>
 */
public class VirtualPadView extends View {

    /** Receives the pad events (pad lab, overlay injector, macros). */
    public interface Listener {
        void onKeyDown(Ps2Button button);

        void onKeyUp(Ps2Button button);

        void onStick(int which, float x, float y);
    }

    private static final float SLOP_DP = 1.5f;

    private final PadPainter painter = new PadPainter();
    private final SparseArray<Target> targets = new SparseArray<>();
    private final EnumSet<Ps2Button> pressed = EnumSet.noneOf(Ps2Button.class);
    private final float[] leftVec = {0f, 0f};
    private final float[] rightVec = {0f, 0f};

    private PadGeometry geometry;
    private float userScale = 1f;
    private float padAlpha = 1f;
    private boolean haptics = true;
    private Listener listener;
    private Vibrator vibrator;

    public VirtualPadView(Context context) {
        super(context);
        setFocusable(true);
        setClickable(true);
        Object svc = getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator = svc instanceof Vibrator ? (Vibrator) svc : null;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** 0.6 .. 1.6, the same range the geometry clamps to. */
    public void setUserScale(float scale) {
        this.userScale = Math.max(0.6f, Math.min(1.6f, scale));
        geometry = null;
        invalidate();
    }

    public float getUserScale() {
        return userScale;
    }

    /** 0.3 .. 1.0 - how strongly the pad covers the game image. */
    public void setPadAlpha(float alpha) {
        this.padAlpha = Math.max(0.3f, Math.min(1f, alpha));
        invalidate();
    }

    public float getPadAlpha() {
        return padAlpha;
    }

    public void setHaptics(boolean on) {
        this.haptics = on;
    }

    public boolean isHaptics() {
        return haptics;
    }

    /** Current geometry, rebuilt lazily after a resize or a scale change. */
    public PadGeometry geometry() {
        if (geometry == null) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0f || h <= 0f) {
                return null;
            }
            float density = getResources().getDisplayMetrics().density;
            geometry = PadGeometry.compute(w, h, density, w >= h, userScale,
                    PadOffsets.load(getContext()));
        }
        return geometry;
    }

    public Set<Ps2Button> pressedButtons() {
        return Collections.unmodifiableSet(pressed);
    }

    public float[] stickVector(int which) {
        return which == PadGeometry.STICK_LEFT ? leftVec.clone() : rightVec.clone();
    }

    /** Releases everything (called when the app goes to the background). */
    public void releaseAll() {
        for (Ps2Button b : EnumSet.copyOf(pressed)) {
            fireUp(b);
        }
        pressed.clear();
        targets.clear();
        leftVec[0] = leftVec[1] = 0f;
        rightVec[0] = rightVec[1] = 0f;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        geometry = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        PadGeometry g = geometry();
        if (g == null) {
            return;
        }
        int layer = canvas.saveLayerAlpha(0f, 0f, getWidth(), getHeight(),
                (int) (255 * padAlpha), Canvas.ALL_SAVE_FLAG);
        painter.draw(canvas, g, pressed, leftVec, rightVec);
        canvas.restoreToCount(layer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        PadGeometry g = geometry();
        if (g == null) {
            return false;
        }
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int index = ev.getActionIndex();
            assign(ev.getPointerId(index), ev.getX(index), ev.getY(index), g);
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < ev.getPointerCount(); i++) {
                move(ev.getPointerId(i), ev.getX(i), ev.getY(i), g);
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            targets.clear();
            leftVec[0] = leftVec[1] = 0f;
            rightVec[0] = rightVec[1] = 0f;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            targets.remove(ev.getPointerId(ev.getActionIndex()));
            if (targets.size() == 0) {
                leftVec[0] = leftVec[1] = 0f;
                rightVec[0] = rightVec[1] = 0f;
            }
        }
        syncPressed();
        invalidate();
        return true;
    }

    private void assign(int id, float px, float py, PadGeometry g) {
        int stick = g.hitStick(px, py);
        if (stick != PadGeometry.STICK_NONE && !stickTaken(stick)) {
            targets.put(id, new Target(stick, null));
            updateStick(stick, px, py, g);
            return;
        }
        Ps2Button b = g.hitTest(px, py, g.dp(SLOP_DP));
        if (b != null) {
            targets.put(id, new Target(PadGeometry.STICK_NONE, b));
        }
    }

    private void move(int id, float px, float py, PadGeometry g) {
        Target t = targets.get(id);
        if (t != null && t.stick != PadGeometry.STICK_NONE) {
            updateStick(t.stick, px, py, g);
        }
    }

    private boolean stickTaken(int which) {
        for (int i = 0; i < targets.size(); i++) {
            if (targets.valueAt(i).stick == which) {
                return true;
            }
        }
        return false;
    }

    private void updateStick(int which, float px, float py, PadGeometry g) {
        StickBox box = g.stick(which);
        if (box == null) {
            return;
        }
        float[] v = PadGeometry.stickVector(box, px, py, 0.10f);
        if (which == PadGeometry.STICK_LEFT) {
            leftVec[0] = v[0];
            leftVec[1] = v[1];
        } else {
            rightVec[0] = v[0];
            rightVec[1] = v[1];
        }
        if (listener != null) {
            listener.onStick(which, v[0], v[1]);
        }
    }

    /** Recomputes the held set from the live pointers and fires the diffs. */
    private void syncPressed() {
        EnumSet<Ps2Button> now = EnumSet.noneOf(Ps2Button.class);
        for (int i = 0; i < targets.size(); i++) {
            Ps2Button b = targets.valueAt(i).button;
            if (b != null) {
                now.add(b);
            }
        }
        for (Ps2Button b : EnumSet.copyOf(pressed)) {
            if (!now.contains(b)) {
                fireUp(b);
            }
        }
        for (Ps2Button b : now) {
            if (!pressed.contains(b)) {
                pressed.add(b);
                hapticTap();
                if (listener != null) {
                    listener.onKeyDown(b);
                }
            }
        }
        pressed.retainAll(now);
    }

    private void fireUp(Ps2Button b) {
        if (listener != null) {
            listener.onKeyUp(b);
        }
    }

    private void hapticTap() {
        if (!haptics) {
            return;
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(12);
            }
        } else {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private static final class Target {
        final int stick;
        final Ps2Button button;

        Target(int stick, Ps2Button button) {
            this.stick = stick;
            this.button = button;
        }
    }
}

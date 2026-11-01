package com.downhill.mobile;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.pad.Ps2Button;
import com.downhill.mobile.pad.VirtualPadView;
import com.downhill.mobile.ui.Ui;

import java.util.Locale;

/**
 * Full-screen pad test bench: the game area is a drawn downhill track, the pad is
 * the real one, and the HUD shows every key/axis as it is pressed. It also prints
 * the live frame rate, which is how the "runs on a weak GPU" claim is checked.
 */
public class PadLabActivity extends Activity implements VirtualPadView.Listener {

    private VirtualPadView pad;
    private TextView hud;
    private TrackBackdrop backdrop;
    private int frames;
    private long windowStart;
    private int fps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setKeepScreenOn();
        setContentView(build());
        loop();
    }

    private void setKeepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pad != null) {
            pad.releaseAll();
        }
    }

    private View build() {
        FrameLayout root = new FrameLayout(this);

        backdrop = new TrackBackdrop();
        root.addView(backdrop, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        pad = new VirtualPadView(this);
        pad.setListener(this);
        root.addView(pad, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        root.addView(buildHud(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));
        return root;
    }

    private View buildHud() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 8));

        LinearLayout bar = Ui.row(this);
        TextView back = Ui.secondaryButton(this, "‹  Voltar");
        back.setOnClickListener(v -> finish());
        bar.addView(back);
        bar.addView(spacer());

        bar.addView(chip("A-", () -> pad.setPadAlpha(pad.getPadAlpha() - 0.1f)));
        bar.addView(chip("A+", () -> pad.setPadAlpha(pad.getPadAlpha() + 0.1f)));
        bar.addView(chip("S-", () -> pad.setUserScale(pad.getUserScale() - 0.1f)));
        bar.addView(chip("S+", () -> pad.setUserScale(pad.getUserScale() + 0.1f)));
        bar.addView(chip("📳", () -> pad.setHaptics(!pad.isHaptics())));
        View reset = chip("⭘", () -> com.downhill.mobile.pad.PadOffsets.clear(this));
        bar.addView(reset);
        box.addView(bar);

        hud = Ui.label(this, "toque no pad", 12f, 0xFFB7C2D8, false);
        hud.setBackground(Ui.rounded(0xCC0A0D14, 0xCC0A0D14, 10f, this, 0x3325E0C4, 1f));
        hud.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        Ui.marginTop(hud, this, 8);
        box.addView(hud);
        return box;
    }

    private View spacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    private View chip(String label, Runnable action) {
        TextView t = Ui.secondaryButton(this, label);
        t.setTextSize(12f);
        t.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        t.setOnClickListener(v -> {
            action.run();
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(90).start())
                    .start();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 6);
        t.setLayoutParams(lp);
        return t;
    }

    // ---------------------------------------------------------------- input --

    @Override
    public void onKeyDown(Ps2Button button) {
        hud.setText(summary());
    }

    @Override
    public void onKeyUp(Ps2Button button) {
        hud.setText(summary());
    }

    @Override
    public void onStick(int which, float x, float y) {
        hud.setText(summary());
    }

    private String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(fps).append(" fps  ·  ");
        if (pad.pressedButtons().isEmpty()) {
            sb.append("nenhuma tecla");
        } else {
            for (Ps2Button b : pad.pressedButtons()) {
                sb.append(b.emoji).append(' ');
            }
            for (Ps2Button b : pad.pressedButtons()) {
                sb.append('\n').append(b.emoji).append(' ').append(b.shortLabel)
                        .append(" — ").append(b.hint);
            }
        }
        float[] l = pad.stickVector(PadGeometry.STICK_LEFT);
        float[] r = pad.stickVector(PadGeometry.STICK_RIGHT);
        sb.append(String.format(Locale.US, "\n🕹️ esq %.2f/%.2f   🎮 dir %.2f/%.2f", l[0], l[1], r[0], r[1]));
        return sb.toString();
    }

    /** Counts real drawn frames so the pad's cost is visible. */
    private void loop() {
        windowStart = System.nanoTime();
        frames = 0;
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                frames++;
                long elapsed = frameTimeNanos - windowStart;
                if (elapsed >= 500_000_000L) {
                    fps = (int) (frames * 1_000_000_000L / elapsed);
                    windowStart = frameTimeNanos;
                    frames = 0;
                    hud.setText(summary());
                }
                backdrop.invalidate();
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    /** Cheap fake game frame: gradient sky, rolling hill and the track. */
    private final class TrackBackdrop extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private float phase;

        TrackBackdrop() {
            super(PadLabActivity.this);
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth();
            int h = getHeight();
            p.setShader(new LinearGradient(0f, 0f, 0f, h * 0.62f,
                    new int[]{0xFF14243F, 0xFF2E4A6B, 0xFF7A5C42}, new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h * 0.62f, p);
            p.setShader(null);

            // sun
            p.setColor(0x33FF9A4D);
            c.drawCircle(w * 0.78f, h * 0.20f, h * 0.16f, p);

            // hills
            phase += 0.9f;
            path.reset();
            path.moveTo(0, h * 0.62f);
            for (int i = 0; i <= 12; i++) {
                float x = w * i / 12f;
                float y = h * 0.62f - (float) (Math.sin(i * 0.8f + phase * 0.01f) * h * 0.05f);
                path.lineTo(x, y);
            }
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            p.setColor(0xFF243A2C);
            c.drawPath(path, p);

            // track
            p.setColor(0xFF3B3226);
            path.reset();
            path.moveTo(w * 0.30f, h * 0.62f);
            path.lineTo(w * 0.56f, h * 0.62f);
            path.lineTo(w * 0.98f, h);
            path.lineTo(0, h);
            path.close();
            c.drawPath(path, p);

            p.setColor(0x22FFFFFF);
            for (int i = 0; i < 6; i++) {
                float t = ((i * 0.18f) + (phase * 0.002f) % 0.18f);
                float y = h * 0.62f + (h * 0.38f) * t;
                float halfW = w * (0.13f + 0.35f * t);
                c.drawRect(w * 0.5f - halfW * 0.08f, y, w * 0.5f + halfW * 0.08f, y + h * 0.012f, p);
            }
        }
    }
}

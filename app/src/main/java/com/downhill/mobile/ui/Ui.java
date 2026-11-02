package com.downhill.mobile.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tiny styling helpers - the whole UI is built in code, no inflated layouts. */
public final class Ui {

    public static final int BG_DEEP = 0xFF0A0D14;
    public static final int BG_SURFACE = 0xFF141926;
    public static final int BG_SURFACE_2 = 0xFF1B2231;
    public static final int ACCENT = 0xFFFF6B1A;
    public static final int ACCENT_2 = 0xFF25E0C4;
    public static final int TEXT = 0xFFF5F7FB;
    public static final int TEXT_DIM = 0xFF9AA6BF;

    private Ui() {
    }

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int colorTop, int colorBottom, float radiusDp,
                                           Context c, int strokeColor, float strokeDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{colorTop, colorBottom});
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeColor != 0) {
            g.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        }
        return g;
    }

    public static TextView label(Context c, String text, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return t;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 16));
        l.setBackground(rounded(BG_SURFACE, 0xFF101520, 18f, c, 0x1AFFFFFF, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 14);
        l.setLayoutParams(lp);
        return l;
    }

    /** Grey uppercase heading used inside the cards. */
    public static TextView cardHeader(Context c, String text) {
        TextView t = label(c, text.toUpperCase(java.util.Locale.ROOT), 11.5f, ACCENT_2, true);
        t.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 10);
        t.setLayoutParams(lp);
        return t;
    }

    /** Filled action button (orange gradient, ripple). */
    public static TextView primaryButton(Context c, String text) {
        TextView t = label(c, text, 15f, 0xFF10131C, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        GradientDrawable fill = rounded(0xFFFF8A3D, ACCENT, 14f, c, 0, 0f);
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x66FFFFFF), fill, null));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /** Outlined action button. */
    public static TextView secondaryButton(Context c, String text) {
        TextView t = label(c, text, 14f, TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 16), dp(c, 11), dp(c, 16), dp(c, 11));
        GradientDrawable fill = rounded(BG_SURFACE_2, 0xFF151B28, 13f, c, 0x3325E0C4, 1.2f);
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), fill, null));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /** Horizontal container with even spacing between children. */
    public static LinearLayout row(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    public static void marginBottom(View v, Context c, float dp) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        if (lp == null) {
            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        lp.bottomMargin = dp(c, dp);
        v.setLayoutParams(lp);
    }

    public static void marginTop(View v, Context c, float dp) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        if (lp == null) {
            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        lp.topMargin = dp(c, dp);
        v.setLayoutParams(lp);
    }

    /** Root background: deep navy with a warm glow at the top. */
    public static GradientDrawable screenBackground(Context c) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF141B2B, BG_DEEP, 0xFF05070C});
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return g;
    }
}

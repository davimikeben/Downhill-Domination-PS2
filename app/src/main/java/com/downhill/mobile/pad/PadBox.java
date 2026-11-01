package com.downhill.mobile.pad;

/** Immutable axis-aligned rectangle in pixels, used for every pad hit-box. */
public final class PadBox {

    public final float x;
    public final float y;
    public final float w;
    public final float h;

    public PadBox(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public float cx() {
        return x + w * 0.5f;
    }

    public float cy() {
        return y + h * 0.5f;
    }

    public float right() {
        return x + w;
    }

    public float bottom() {
        return y + h;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    /** True when the two boxes share surface (a {@code tolerance} of 0 means touching is fine). */
    public boolean overlaps(PadBox o, float tolerance) {
        return x < o.right() - tolerance && o.x < right() - tolerance
                && y < o.bottom() - tolerance && o.y < bottom() - tolerance;
    }

    /** Squared distance from an arbitrary point to this box (0 when inside). */
    public float distanceSq(float px, float py) {
        float dx = Math.max(Math.max(x - px, 0f), px - right());
        float dy = Math.max(Math.max(y - py, 0f), py - bottom());
        return dx * dx + dy * dy;
    }

    public PadBox grow(float by) {
        return new PadBox(x - by, y - by, w + by * 2f, h + by * 2f);
    }

    @Override
    public String toString() {
        return "PadBox{" + x + "," + y + " " + w + "x" + h + "}";
    }
}

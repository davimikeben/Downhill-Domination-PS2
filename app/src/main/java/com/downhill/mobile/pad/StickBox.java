package com.downhill.mobile.pad;

/** Circular analog stick area in pixels. */
public final class StickBox {

    public final float cx;
    public final float cy;
    public final float radius;
    public final float knobRadius;

    public StickBox(float cx, float cy, float radius, float knobRadius) {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.knobRadius = knobRadius;
    }

    public boolean contains(float px, float py) {
        float dx = px - cx;
        float dy = py - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    /** Smallest distance between the circle edge and a box (negative = overlapping). */
    public float distanceTo(PadBox box) {
        return (float) Math.sqrt(box.distanceSq(cx, cy)) - radius;
    }

    public boolean apart(PadBox box, float tolerance) {
        return distanceTo(box) > tolerance;
    }

    public boolean overlaps(StickBox other) {
        float dx = cx - other.cx;
        float dy = cy - other.cy;
        float rr = radius + other.radius;
        return dx * dx + dy * dy < rr * rr;
    }

    @Override
    public String toString() {
        return "StickBox{" + cx + "," + cy + " r=" + radius + "}";
    }
}

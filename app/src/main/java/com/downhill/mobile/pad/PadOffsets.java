package com.downhill.mobile.pad;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-key drag offsets saved by the layout editor, in dp on the design canvas.
 * Pure data holder so the geometry stays testable.
 */
public final class PadOffsets {

    private static final String PREFIX = "off_";

    private PadOffsets() {
    }

    /** @return offsets in dp, or {@code null} when the player never moved a key */
    public static Map<Ps2Button, float[]> load(Context context) {
        SharedPreferences p = context.getSharedPreferences("downhill_pad", Context.MODE_PRIVATE);
        Map<Ps2Button, float[]> out = new EnumMap<>(Ps2Button.class);
        for (Ps2Button b : Ps2Button.values()) {
            String raw = p.getString(PREFIX + b.name(), null);
            if (raw == null) {
                continue;
            }
            int comma = raw.indexOf(',');
            if (comma <= 0) {
                continue;
            }
            try {
                float dx = Float.parseFloat(raw.substring(0, comma));
                float dy = Float.parseFloat(raw.substring(comma + 1));
                if (dx != 0f || dy != 0f) {
                    out.put(b, new float[]{dx, dy});
                }
            } catch (NumberFormatException ignored) {
                // a corrupted entry must never break the pad
            }
        }
        return out.isEmpty() ? null : out;
    }

    public static void save(Context context, Ps2Button button, float dxDp, float dyDp) {
        context.getSharedPreferences("downhill_pad", Context.MODE_PRIVATE)
                .edit()
                .putString(PREFIX + button.name(), dxDp + "," + dyDp)
                .apply();
    }

    public static void clear(Context context) {
        SharedPreferences p = context.getSharedPreferences("downhill_pad", Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        for (Ps2Button b : Ps2Button.values()) {
            e.remove(PREFIX + b.name());
        }
        e.apply();
    }
}

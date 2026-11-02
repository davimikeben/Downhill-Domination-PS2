package com.downhill.mobile.export;

import com.downhill.mobile.pad.PadBox;
import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.pad.Ps2Button;
import com.downhill.mobile.pad.StickBox;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Writes a RetroArch touch overlay ({@code .cfg}) that reproduces this app's pad
 * inside RetroArch with the LRPS2 core.
 *
 * <p>RetroArch overlays are plain text: coordinates are fractions of the screen
 * and every box carries the input name it triggers, so the generated file can be
 * asserted byte for byte in unit tests. The matching PNG is produced by
 * {@link PadBitmap}.</p>
 */
public final class RetroArchOverlay {

    public static final String CFG_NAME = "downhill-pad.cfg";
    public static final String PNG_NAME = "downhill-pad.png";

    private RetroArchOverlay() {
    }

    /**
     * DualShock 2 -&gt; RetroPad mapping.
     *
     * <p>The PS2 face buttons sit in the "Nintendo" layout for RetroArch, so
     * Cross is {@code B} (south), Circle is {@code A} (east), Square is {@code Y}
     * (west) and Triangle is {@code X} (north).</p>
     */
    public static Map<Ps2Button, String> mappings() {
        Map<Ps2Button, String> m = new LinkedHashMap<>();
        m.put(Ps2Button.DPAD_UP, "btn_up");
        m.put(Ps2Button.DPAD_DOWN, "btn_down");
        m.put(Ps2Button.DPAD_LEFT, "btn_left");
        m.put(Ps2Button.DPAD_RIGHT, "btn_right");
        m.put(Ps2Button.TRIANGLE, "btn_x");
        m.put(Ps2Button.CIRCLE, "btn_a");
        m.put(Ps2Button.CROSS, "btn_b");
        m.put(Ps2Button.SQUARE, "btn_y");
        m.put(Ps2Button.L1, "btn_l");
        m.put(Ps2Button.R1, "btn_r");
        m.put(Ps2Button.L2, "btn_l2");
        m.put(Ps2Button.R2, "btn_r2");
        m.put(Ps2Button.L3, "btn_l3");
        m.put(Ps2Button.R3, "btn_r3");
        m.put(Ps2Button.SELECT, "btn_select");
        m.put(Ps2Button.START, "btn_start");
        // Extra keys of this build, mapped to RetroArch hotkeys.
        m.put(Ps2Button.TURBO, "turbo");
        m.put(Ps2Button.SAVE, "save_state");
        m.put(Ps2Button.LOAD, "load_state");
        m.put(Ps2Button.FF, "fast_forward");
        m.put(Ps2Button.MENU, "menu_toggle");
        return m;
    }

    /** Hotkey-style entries, documented in the exported README. */
    public static boolean isHotkey(String mapping) {
        return !mapping.startsWith("btn_") && !mapping.startsWith("analog_");
    }

    private static String f(float v) {
        float c = v < 0f ? 0f : (v > 1f ? 1f : v);
        return String.format(Locale.US, "%.6f", c);
    }

    /**
     * Builds the whole {@code .cfg} for a pad laid out on a
     * {@code widthPx x heightPx} surface.
     */
    public static String build(PadGeometry geo, float widthPx, float heightPx) {
        if (geo == null || widthPx <= 0f || heightPx <= 0f) {
            throw new IllegalArgumentException("geometry/viewport required");
        }
        Map<Ps2Button, String> map = mappings();
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Downhill Domination Mobile - RetroArch touch overlay\n");
        sb.append("# Copie este arquivo e ").append(PNG_NAME)
                .append(" para a pasta de overlays do RetroArch\n");
        sb.append("# (Android/data/com.retroarch/files/overlays/downhill/).\n\n");
        sb.append("overlays = 1\n\n");
        sb.append("overlay0_overlay = ").append(PNG_NAME).append('\n');
        sb.append("overlay0_full_screen = true\n");
        int descs = geo.boxes().size() + 2;
        sb.append("overlay0_descs = ").append(descs).append('\n');
        sb.append("overlay0_descs_mouse_scale = 0.400000\n\n");

        int index = 0;
        for (Map.Entry<Ps2Button, PadBox> e : geo.boxes().entrySet()) {
            PadBox b = e.getValue();
            String key = map.get(e.getKey());
            if (key == null) {
                continue;
            }
            sb.append("overlay0_desc").append(index).append(" = \"")
                    .append(f(b.cx() / widthPx)).append(',')
                    .append(f(b.cy() / heightPx)).append(",rect,")
                    .append(f(b.w / widthPx)).append(',')
                    .append(f(b.h / heightPx)).append(',')
                    .append(key).append("\"\n");
            index++;
        }
        StickBox left = geo.leftStick();
        StickBox right = geo.rightStick();
        sb.append("overlay0_desc").append(index++).append(" = \"")
                .append(f(left.cx / widthPx)).append(',').append(f(left.cy / heightPx))
                .append(",radial,").append(f(left.radius / widthPx)).append(',')
                .append(f(left.radius / heightPx)).append(",analog_left\"\n");
        sb.append("overlay0_desc").append(index).append(" = \"")
                .append(f(right.cx / widthPx)).append(',').append(f(right.cy / heightPx))
                .append(",radial,").append(f(right.radius / widthPx)).append(',')
                .append(f(right.radius / heightPx)).append(",analog_right\"\n");
        return sb.toString();
    }

    /** Small text file explaining where the files go and what each key does. */
    public static String readme() {
        StringBuilder sb = new StringBuilder();
        sb.append("PAD DO DOWNHILL DOMINATION - GUIA DE INSTALACAO\n");
        sb.append("=============================================\n\n");
        sb.append("1. Instale o RetroArch no celular.\n");
        sb.append("2. Copie os dois arquivos desta pasta para:\n");
        sb.append("   Android/data/com.retroarch/files/overlays/downhill/\n");
        sb.append("3. RetroArch > Settings > On-Screen Display > On-Screen Overlay\n");
        sb.append("   > Overlay Preset > downhill-pad.cfg\n");
        sb.append("4. Em Settings > Input, deixe o Port 1 como RetroPad.\n");
        sb.append("5. Carregue o jogo com o nucleo LRPS2.\n\n");
        sb.append("Mapa das teclas (RetroPad):\n");
        for (Map.Entry<Ps2Button, String> e : mappings().entrySet()) {
            sb.append("  ").append(e.getKey().emoji).append(' ')
                    .append(e.getKey().shortLabel).append(" -> ").append(e.getValue())
                    .append("  ").append(e.getKey().hint).append('\n');
        }
        return sb.toString();
    }
}

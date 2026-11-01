package com.downhill.mobile.emu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Render settings tuned for a low-end phone GPU (Mali-G52 / PowerVR 832 class).
 *
 * <p>Every row is a concrete "open this menu, set this value" instruction, because
 * the emulators keep their preferences in private storage: the app can not write
 * them, but it can show exactly what to tap. Pure Java so the table can be
 * asserted in tests.</p>
 */
public final class TuningProfile {

    public static final class Setting {
        public final String where;
        public final String what;
        public final String value;
        public final String why;

        public Setting(String where, String what, String value, String why) {
            this.where = where;
            this.what = what;
            this.value = value;
            this.why = why;
        }

        @Override
        public String toString() {
            return where + " > " + what + " = " + value;
        }
    }

    private TuningProfile() {
    }

    /** Settings that apply to the AetherSX2 family (ARMSX2 / NetherSX2 share the UI). */
    public static List<Setting> aetherFamily() {
        List<Setting> s = new ArrayList<>();
        s.add(new Setting("Graphics", "Renderer", "OpenGL",
                "Em Mali-G52 o OpenGL costuma ser mais rápido e sem artefatos que o Vulkan."));
        s.add(new Setting("Graphics", "GPU Palette Conversion", "Desligado",
                "Economiza banda de memória."));
        s.add(new Setting("Graphics", "Internal Resolution", "1x (Native)",
                "2x derruba os FPS pela metade nessa GPU."));
        s.add(new Setting("Graphics", "Texture Filtering", "Nearest",
                "Filtro bilinear custa caro e some com o visual do PS2."));
        s.add(new Setting("Graphics", "Anisotropic Filtering", "Desligado",
                "Ganho visual pequeno, custo real."));
        s.add(new Setting("Graphics", "Mipmapping", "Automático",
                "Evita serrilhado em pistas longas."));
        s.add(new Setting("Graphics", "FXAA / Shade Boost", "Desligado",
                "Pós-processamento é o primeiro a cair em GPU fraca."));
        s.add(new Setting("Graphics", "Blending Accuracy", "Básico",
                "Precisão alta só importa em poucos jogos."));
        s.add(new Setting("Emulation", "EE Cycle Rate", "100% (normal)",
                "Nunca aumente: dobra o trabalho da CPU."));
        s.add(new Setting("Emulation", "EE Cycle Skip", "1 (leve)",
                "Ganha FPS em trechos pesados com pouca perda visual."));
        s.add(new Setting("Emulation", "Multi-Threaded VU1 (MTVU)", "Ligado",
                "Usa os 8 núcleos; é o maior ganho grátis."));
        s.add(new Setting("Emulation", "Instant DMA", "Ligado",
                "Reduz travadas de carregamento de textura."));
        s.add(new Setting("Emulation", "Fast Boot", "Ligado",
                "Pula a BIOS e entra direto no jogo."));
        s.add(new Setting("Audio", "Backend", "Cubeb", "Menor latência que SDL nesta classe."));
        s.add(new Setting("Audio", "Buffer Size", "2048 samples",
                "Buffer maior evita estalos quando o FPS oscila."));
        s.add(new Setting("System", "Sustained Performance Mode", "Ligado",
                "Mantém o clock estável e evita throttling térmico."));
        return s;
    }

    /** RetroArch / LRPS2 equivalent. */
    public static List<Setting> retroArch() {
        List<Setting> s = new ArrayList<>();
        s.add(new Setting("Settings > Video", "Video Driver", "gl",
                "GL é mais previsível que Vulkan em Mali-G52."));
        s.add(new Setting("Settings > Video > Output", "Threaded Video", "Ligado",
                "Tira o desenho do encadeamento principal."));
        s.add(new Setting("Settings > Video > Synchronization", "Hard GPU Sync", "Ligado",
                "Reduz o input lag mesmo com FPS baixo."));
        s.add(new Setting("Settings > Video > Synchronization", "Frame Delay", "0",
                "Frame delay alto piora a suavidade aqui."));
        s.add(new Setting("Settings > Video > Scaling", "Integer Scale", "Ligado",
                "Escala sem custo de filtro."));
        s.add(new Setting("Settings > Latency", "Audio Latency", "64 ms",
                "Menos buffer = menos atraso no toque."));
        s.add(new Setting("Core > LRPS2", "Renderer", "Hardware (GL)",
                "Hardware deixa o upscale leve."));
        s.add(new Setting("Core > LRPS2", "Internal Resolution", "1x",
                "Igual ao nativo, sem upscale."));
        s.add(new Setting("Core > LRPS2", "Fast Boot", "Ligado",
                "Pula a BIOS."));
        return s;
    }

    /** Rows for the given emulator id; unknown ids get the AetherSX2 family table. */
    public static List<Setting> forTarget(String emulatorId) {
        if (emulatorId != null && emulatorId.toLowerCase(Locale.ROOT).contains("retroarch")) {
            return Collections.unmodifiableList(retroArch());
        }
        return Collections.unmodifiableList(aetherFamily());
    }

    /** Plain-text dump used by the "copiar ajustes" button and the README. */
    public static String asText(String emulatorId, List<Setting> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Downhill Domination Mobile - ajustes para GPU fraca (Mali-G52)\n");
        sb.append("Emulador: ").append(emulatorId).append("\n\n");
        for (Setting r : rows) {
            sb.append("- ").append(r.where).append(" > ").append(r.what)
                    .append(" = ").append(r.value).append("  (").append(r.why).append(")\n");
        }
        return sb.toString();
    }
}

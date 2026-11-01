package com.downhill.mobile.pad;

import java.util.ArrayList;
import java.util.List;

/**
 * Every control of the PS2 joypad plus the utility keys this build adds.
 *
 * <p>The class is plain Java on purpose: the Android layer (view, overlay, injectors)
 * only consumes it, so the whole key model can be verified by JVM unit tests.</p>
 *
 * <p>{@code gamepadCode} values are Android {@code KeyEvent} key codes of a real
 * joypad (KEYCODE_BUTTON_A = 96 ...), which is what emulators expect.
 * {@code keyboardCode} is the fallback used when the emulator only listens to a
 * physical keyboard.</p>
 */
public enum Ps2Button {

    // name          short label        emoji      gamepad  keyboard  group           hint (what it does)
    DPAD_UP("Cima", "⬆️", Codes.KEY_DPAD_UP, Codes.KEY_W, Group.DPAD,
            "Equilíbrio / empinar a bike 🚵"),
    DPAD_DOWN("Baixo", "⬇️", Codes.KEY_DPAD_DOWN, Codes.KEY_S, Group.DPAD,
            "Agachar na bike para pegar velocidade 💨"),
    DPAD_LEFT("Esq.", "⬅️", Codes.KEY_DPAD_LEFT, Codes.KEY_A, Group.DPAD,
            "Inclinar / curvar para a esquerda ↰"),
    DPAD_RIGHT("Dir.", "➡️", Codes.KEY_DPAD_RIGHT, Codes.KEY_D, Group.DPAD,
            "Inclinar / curvar para a direita ↱"),

    TRIANGLE("Triângulo", "🔺", Codes.KEY_BUTTON_Y, Codes.KEY_I, Group.FACE,
            "Salto e manobras aéreas 🤸"),
    CIRCLE("Círculo", "🔴", Codes.KEY_BUTTON_B, Codes.KEY_L, Group.FACE,
            "Soco / ataque contra os rivais 👊"),
    CROSS("Xis", "✖️", Codes.KEY_BUTTON_A, Codes.KEY_K, Group.FACE,
            "Pedalar / acelerar 🚴"),
    SQUARE("Quadrado", "🟪", Codes.KEY_BUTTON_X, Codes.KEY_J, Group.FACE,
            "Freio: segura a bike nas curvas 🛑"),

    L1("L1", "🎪", Codes.KEY_BUTTON_L1, Codes.KEY_Q, Group.SHOULDER,
            "Manobra/truque com a mão esquerda 🎪"),
    L2("L2", "👈", Codes.KEY_BUTTON_L2, Codes.KEY_1, Group.SHOULDER,
            "Golpe lateral esquerdo 👈"),
    R1("R1", "🎯", Codes.KEY_BUTTON_R1, Codes.KEY_E, Group.SHOULDER,
            "Manobra/truque com a mão direita 🎯"),
    R2("R2", "👉", Codes.KEY_BUTTON_R2, Codes.KEY_3, Group.SHOULDER,
            "Golpe lateral direito 👉"),

    L3("L3", "🕹️", Codes.KEY_BUTTON_THUMBL, Codes.KEY_U, Group.SYSTEM,
            "Clique do analógico esquerdo: buzininha/reposicionar 🕹️"),
    R3("R3", "🎮", Codes.KEY_BUTTON_THUMBR, Codes.KEY_O, Group.SYSTEM,
            "Clique do analógico direito: câmera de ré 🎥"),
    SELECT("Select", "🗺️", Codes.KEY_BUTTON_SELECT, Codes.KEY_TAB, Group.SYSTEM,
            "Mapa / placar da pista 🗺️"),
    START("Start", "⏸️", Codes.KEY_BUTTON_START, Codes.KEY_ENTER, Group.SYSTEM,
            "Pausa e menu do jogo ⏸️"),

    TURBO("Turbo", "⚡", Codes.KEY_NONE, Codes.KEY_NONE, Group.UTILITY,
            "Turbo automático: repete o ✖️ rapidamente ⚡"),
    SAVE("Salvar", "💾", Codes.KEY_NONE, Codes.KEY_NONE, Group.UTILITY,
            "Salvar estado rápido do emulador 💾"),
    LOAD("Carregar", "📂", Codes.KEY_NONE, Codes.KEY_NONE, Group.UTILITY,
            "Carregar estado rápido 📂"),
    FF("Avançar", "⏩", Codes.KEY_NONE, Codes.KEY_NONE, Group.UTILITY,
            "Acelerar o tempo do emulador (fast-forward) ⏩"),
    MENU("Menu", "⚙️", Codes.KEY_NONE, Codes.KEY_NONE, Group.UTILITY,
            "Abrir os ajustes do teclado virtual ⚙️");

    /** Visual grouping, used by the legend screen and by the layout builder. */
    public enum Group {
        DPAD, FACE, SHOULDER, SYSTEM, UTILITY
    }

    public final String shortLabel;
    public final String emoji;
    public final int gamepadCode;
    public final int keyboardCode;
    public final Group group;
    public final String hint;

    Ps2Button(String shortLabel, String emoji, int gamepadCode, int keyboardCode,
              Group group, String hint) {
        this.shortLabel = shortLabel;
        this.emoji = emoji;
        this.gamepadCode = gamepadCode;
        this.keyboardCode = keyboardCode;
        this.group = group;
        this.hint = hint;
    }

    /** True for the five extra keys that this port adds on top of the PS2 pad. */
    public boolean isUtility() {
        return group == Group.UTILITY;
    }

    /** True for keys a real PS2 joypad has (i.e. everything that gets injected). */
    public boolean isHardwareKey() {
        return gamepadCode != Codes.KEY_NONE;
    }

    public String displayName() {
        return shortLabel;
    }

    public static List<Ps2Button> of(Group group) {
        List<Ps2Button> out = new ArrayList<>();
        for (Ps2Button b : values()) {
            if (b.group == group) {
                out.add(b);
            }
        }
        return out;
    }

    public static Ps2Button byGamepadCode(int code) {
        for (Ps2Button b : values()) {
            if (b.gamepadCode == code) {
                return b;
            }
        }
        return null;
    }

    /**
     * Android {@code KeyEvent} constants, duplicated here as plain ints so that this
     * model stays free of Android imports and can run in plain JVM tests.
     */
    public static final class Codes {
        private Codes() {
        }

        public static final int KEY_NONE = 0;
        public static final int KEY_W = 51;
        public static final int KEY_1 = 8;
        public static final int KEY_3 = 10;
        public static final int KEY_A = 29;
        public static final int KEY_D = 32;
        public static final int KEY_E = 33;
        public static final int KEY_I = 37;
        public static final int KEY_J = 38;
        public static final int KEY_K = 39;
        public static final int KEY_L = 40;
        public static final int KEY_O = 43;
        public static final int KEY_Q = 45;
        public static final int KEY_S = 47;
        public static final int KEY_U = 49;
        public static final int KEY_DPAD_UP = 19;
        public static final int KEY_DPAD_DOWN = 20;
        public static final int KEY_DPAD_LEFT = 21;
        public static final int KEY_DPAD_RIGHT = 22;
        public static final int KEY_ENTER = 66;
        public static final int KEY_TAB = 61;
        public static final int KEY_BUTTON_A = 96;
        public static final int KEY_BUTTON_B = 97;
        public static final int KEY_BUTTON_C = 98;
        public static final int KEY_BUTTON_X = 99;
        public static final int KEY_BUTTON_Y = 100;
        public static final int KEY_BUTTON_Z = 101;
        public static final int KEY_BUTTON_L1 = 102;
        public static final int KEY_BUTTON_R1 = 103;
        public static final int KEY_BUTTON_L2 = 104;
        public static final int KEY_BUTTON_R2 = 105;
        public static final int KEY_BUTTON_THUMBL = 106;
        public static final int KEY_BUTTON_THUMBR = 107;
        public static final int KEY_BUTTON_START = 108;
        public static final int KEY_BUTTON_SELECT = 109;
    }
}

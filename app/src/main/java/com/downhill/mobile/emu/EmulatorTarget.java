package com.downhill.mobile.emu;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The PS2 cores this front end can hand the disc image to, in the order they are
 * tried. Pure Java: the Android layer only asks the {@code PackageManager}
 * whether one of {@link #packages} is installed.
 */
public final class EmulatorTarget {

    public enum Kind {
        /** {@code ACTION_VIEW} with a {@code content://} URI of the .iso. */
        VIEW_URI,
        /** RetroArch: explicit launch with a core hint extra. */
        RETROARCH,
        /** Nothing installed. */
        NONE
    }

    public final String id;
    public final String label;
    public final Set<String> packages;
    public final Kind kind;
    public final String tuningNote;

    private EmulatorTarget(String id, String label, Kind kind, String tuningNote,
                           String... packages) {
        this.id = id;
        this.label = label;
        this.kind = kind;
        this.tuningNote = tuningNote;
        this.packages = new LinkedHashSet<>();
        for (String p : packages) {
            this.packages.add(p);
        }
    }

    /** Ordered preference list: the ARMSX2/AetherSX2 family first (fastest on Mali). */
    public static final List<EmulatorTarget> ALL = new ArrayList<>();

    static {
        ALL.add(new EmulatorTarget("armsx2", "ARMSX2", Kind.VIEW_URI,
                "Melhor opção para Mali-G52: núcleo PCSX2 otimizado para ARM.",
                "com.armsx2.android", "org.armsx2.android", "xyz.armsx2.android"));
        ALL.add(new EmulatorTarget("aethersx2", "AetherSX2", Kind.VIEW_URI,
                "Estável e leve, ótimo com renderizador OpenGL em Mali-G52.",
                "xyz.aethersx2.android", "xyz.aethersx2.android.pro"));
        ALL.add(new EmulatorTarget("nethersx2", "NetherSX2", Kind.VIEW_URI,
                "Fork do AetherSX2 com patches: use Vulkan se o driver aguentar.",
                "com.nethersx2.android", "org.nethersx2.android"));
        ALL.add(new EmulatorTarget("retroarch", "RetroArch (núcleo LRPS2)", Kind.RETROARCH,
                "Use o núcleo LRPS2, vídeo GL e sincronização 'hard GPU'.",
                "com.retroarch", "com.retroarch.aarch64"));
    }

    public static EmulatorTarget byId(String id) {
        for (EmulatorTarget t : ALL) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** True when one of this target's package names is in {@code installed}. */
    public boolean isInstalled(Set<String> installed) {
        for (String p : packages) {
            if (installed.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /** First installed target, honouring the preference order, or {@code null}. */
    public static EmulatorTarget firstInstalled(Set<String> installed) {
        for (EmulatorTarget t : ALL) {
            if (t.isInstalled(installed)) {
                return t;
            }
        }
        return null;
    }

    /** Every package name the manifest has to declare in {@code <queries>}. */
    public static Set<String> allPackages() {
        Set<String> out = new LinkedHashSet<>();
        for (EmulatorTarget t : ALL) {
            out.addAll(t.packages);
        }
        return out;
    }

    @Override
    public String toString() {
        return label;
    }
}

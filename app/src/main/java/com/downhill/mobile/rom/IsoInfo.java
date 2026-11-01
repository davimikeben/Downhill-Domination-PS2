package com.downhill.mobile.rom;

import java.util.Locale;

/** What the importer learned about the disc image. */
public final class IsoInfo {

    /** Serial of "Downhill Domination (Europe)". */
    public static final String SERIAL_EU = "SLES-53828";
    /** Serial of "Downhill Domination (USA)". */
    public static final String SERIAL_US = "SLUS-20705";

    public final String volumeLabel;
    public final String boot2;
    public final String bootFile;
    public final String serial;
    public final String region;
    public final boolean downhillDomination;

    private IsoInfo(String volumeLabel, String boot2, String bootFile, String serial,
                    String region, boolean downhillDomination) {
        this.volumeLabel = volumeLabel;
        this.boot2 = boot2;
        this.bootFile = bootFile;
        this.serial = serial;
        this.region = region;
        this.downhillDomination = downhillDomination;
    }

    static IsoInfo of(String volumeLabel, String boot2) {
        String file = boot2;
        int colon = file.indexOf(':');
        if (colon >= 0) {
            file = file.substring(colon + 1);
        }
        file = file.replace('\\', '/');
        if (file.startsWith("/")) {
            file = file.substring(1);
        }
        int semi = file.indexOf(';');
        if (semi > 0) {
            file = file.substring(0, semi);
        }
        String serial = serialOf(file);
        String region = regionOf(serial);
        boolean yes = SERIAL_EU.equals(serial) || SERIAL_US.equals(serial);
        return new IsoInfo(volumeLabel == null ? "" : volumeLabel.trim(), boot2.trim(), file,
                serial, region, yes);
    }

    /** {@code SLES_538.28} -&gt; {@code SLES-53828}. Returns "" when the name has no id. */
    static String serialOf(String bootFile) {
        String base = bootFile.toUpperCase(Locale.ROOT);
        int dot = base.lastIndexOf('.');
        String head = dot > 0 ? base.substring(0, dot) : base;
        String tail = dot > 0 ? base.substring(dot + 1) : "";
        int underscore = head.indexOf('_');
        if (underscore <= 0) {
            return "";
        }
        String prefix = head.substring(0, underscore).replaceAll("[^A-Z]", "");
        String number = head.substring(underscore + 1).replaceAll("[^0-9]", "");
        String tailDigits = tail.replaceAll("[^0-9]", "");
        if (prefix.isEmpty() || number.isEmpty()) {
            return "";
        }
        return prefix + "-" + number + tailDigits;
    }

    public static String regionOf(String serial) {
        if (serial == null || serial.length() < 4) {
            return "?";
        }
        String p = serial.substring(0, 4);
        switch (p) {
            case "SLES":
            case "SCES":
            case "SLED":
            case "SCED":
                return "PAL";
            case "SLUS":
            case "SCUS":
                return "NTSC-U";
            case "SLPS":
            case "SCPS":
            case "SLPM":
            case "SCAJ":
                return "NTSC-J";
            default:
                return "?";
        }
    }

    @Override
    public String toString() {
        return "IsoInfo{" + serial + " " + region + " boot=" + bootFile
                + " label=" + volumeLabel + " downhill=" + downhillDomination + "}";
    }
}

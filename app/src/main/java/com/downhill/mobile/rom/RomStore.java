package com.downhill.mobile.rom;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StatFs;

import java.io.File;

/**
 * Where the disc image lives and whether it is usable.
 *
 * <p>The archive from the user's link is a 2.4 GB zip holding one 2.77 GB ISO, so
 * the APK can not carry it: the player imports it once and this class remembers
 * where it went, how big it is and which PS2 serial was found inside.</p>
 */
public final class RomStore {

    public static final String ISO_NAME = "downhill.iso";
    public static final long EXPECTED_ISO_BYTES = 2771025920L;

    private static final String PREF_SERIAL = "rom_serial";
    private static final String PREF_LABEL = "rom_label";

    private final Context app;

    public RomStore(Context context) {
        this.app = context.getApplicationContext();
    }

    public File dir() {
        File d = new File(app.getFilesDir(), "rom");
        if (!d.exists() && !d.mkdirs()) {
            // nothing else to do: the import will fail with a clear message
            return d;
        }
        return d;
    }

    public File isoFile() {
        return new File(dir(), ISO_NAME);
    }

    public File partFile() {
        return new File(dir(), ISO_NAME + ".part");
    }

    /** True when a complete image is on disk. */
    public boolean isReady() {
        File f = isoFile();
        return f.isFile() && f.length() > 512L * 1024 * 1024;
    }

    public long size() {
        File f = isoFile();
        return f.isFile() ? f.length() : 0L;
    }

    public long freeBytes() {
        try {
            File d = app.getFilesDir();
            StatFs s = new StatFs(d.getAbsolutePath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return s.getAvailableBytes();
            }
            return (long) s.getAvailableBlocks() * s.getBlockSize();
        } catch (RuntimeException e) {
            return -1L;
        }
    }

    /**
     * Bytes still missing before the import can start, 0 when there is room.
     *
     * <p>A reading of 0 (or a failure) means the filesystem does not report free
     * space - some custom ROMs and virtual/storage providers do that - and in that
     * case the import is allowed to try instead of being blocked by a guess.</p>
     */
    public long missingSpace() {
        long free = freeBytes();
        if (free <= 0) {
            return 0L;
        }
        long need = EXPECTED_ISO_BYTES + 256L * 1024 * 1024;
        return Math.max(0L, need - free);
    }

    public void rememberInfo(IsoInfo info) {
        SharedPreferences p = app.getSharedPreferences("downhill_pad", Context.MODE_PRIVATE);
        p.edit()
                .putString(PREF_SERIAL, info == null ? "" : info.serial)
                .putString(PREF_LABEL, info == null ? "" : info.volumeLabel)
                .apply();
    }

    public String serial() {
        return app.getSharedPreferences("downhill_pad", Context.MODE_PRIVATE)
                .getString(PREF_SERIAL, "");
    }

    public String label() {
        return app.getSharedPreferences("downhill_pad", Context.MODE_PRIVATE)
                .getString(PREF_LABEL, "");
    }

    /** Reads the serial from the image that is already on disk. */
    public IsoInfo readInfo(IsoParser.BlockReader reader) {
        if (!isReady()) {
            return null;
        }
        try {
            IsoInfo info = IsoParser.parse(reader);
            rememberInfo(info);
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    /** Deletes the image and the stale metadata. */
    public void clear() {
        File iso = isoFile();
        if (iso.exists()) {
            iso.delete();
        }
        File part = partFile();
        if (part.exists()) {
            part.delete();
        }
        rememberInfo(null);
    }
}

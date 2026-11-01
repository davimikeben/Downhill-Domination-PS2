package com.downhill.mobile.rom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Joins the split parts of the CSO that ship with the GitHub release
 * (2.499.512.984 bytes would be over the 2 GB per-asset limit, so it goes out as
 * {@code .000} + {@code .001}).
 *
 * <p>Plain Java: the ordering and the byte-exact concatenation are checked by
 * JVM tests, the Android layer only opens the documents.</p>
 */
public final class PartJoiner {

    public interface Progress {
        void update(long done, long total);
    }

    public interface CancelCheck {
        boolean isCancelled();
    }

    private static final int BUFFER = 1 << 20;
    private static final long REPORT_EVERY = 32L * 1024 * 1024;

    private PartJoiner() {
    }

    /**
     * Sorts {@code .000, .001, ...} names the way a human expects, ignoring case.
     * Anything without a numeric suffix keeps its relative order at the end.
     */
    public static List<String> sortedParts(List<String> names) {
        List<String> copy = new ArrayList<>(names);
        Collections.sort(copy, (a, b) -> {
            long na = suffix(a);
            long nb = suffix(b);
            if (na >= 0 && nb >= 0) {
                return Long.compare(na, nb);
            }
            if (na >= 0) {
                return -1;
            }
            if (nb >= 0) {
                return 1;
            }
            return a.compareToIgnoreCase(b);
        });
        return copy;
    }

    /** Numeric extension of a split part ({@code game.cso.001} -> 1), or -1. */
    public static long suffix(String name) {
        if (name == null) {
            return -1L;
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return -1L;
        }
        String ext = name.substring(dot + 1);
        if (ext.length() < 2 || ext.length() > 4) {
            return -1L;
        }
        for (int i = 0; i < ext.length(); i++) {
            if (!Character.isDigit(ext.charAt(i))) {
                return -1L;
            }
        }
        return Long.parseLong(ext);
    }

    /**
     * Concatenates every stream, in the given order, into {@code out}.
     *
     * @return bytes written
     */
    public static long join(List<InputStream> parts, OutputStream out, Progress progress,
                            CancelCheck cancel) throws IOException {
        byte[] buf = new byte[BUFFER];
        long done = 0L;
        long lastReport = 0L;
        for (InputStream in : parts) {
            if (in == null) {
                continue;
            }
            int n;
            while ((n = in.read(buf)) > 0) {
                if (cancel != null && cancel.isCancelled()) {
                    throw new IOException("Junção cancelada.");
                }
                out.write(buf, 0, n);
                done += n;
                if (progress != null && done - lastReport >= REPORT_EVERY) {
                    lastReport = done;
                    progress.update(done, -1L);
                }
            }
        }
        if (progress != null) {
            progress.update(done, done);
        }
        return done;
    }

    /** Name of the joined file: the parts' common prefix plus {@code .cso}. */
    public static String joinedName(List<String> partNames) {
        if (partNames.isEmpty()) {
            return RomStore.CSO_NAME;
        }
        String first = partNames.get(0);
        int dot = first.lastIndexOf('.');
        String base = dot > 0 ? first.substring(0, dot) : first;
        base = base.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (base.isEmpty()) {
            return RomStore.CSO_NAME;
        }
        return base.toLowerCase(Locale.ROOT).endsWith(".cso") ? base : RomStore.CSO_NAME;
    }
}

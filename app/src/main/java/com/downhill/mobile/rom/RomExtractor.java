package com.downhill.mobile.rom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The pure-Java core of the import: copies the first disc image found in a
 * stream into a target stream, no Android in sight.
 *
 * <p>Handles both shapes the user can pick: a plain {@code .iso} (raw copy) and a
 * {@code .zip} holding the image (deflate). {@link ZipInputStream} checks the CRC
 * at the end of the entry, so a truncated download fails loudly instead of
 * producing a corrupt disc.</p>
 */
public final class RomExtractor {

    /** Called with the bytes copied so far and the expected total (-1 if unknown). */
    public interface Progress {
        void update(long done, long total, String stage);
    }

    /** Polled before every buffer; returning true aborts the copy. */
    public interface CancelCheck {
        boolean isCancelled();
    }

    private static final int BUFFER = 1 << 20;
    /** Progress is pushed to the UI at most every 48 MB. */
    private static final long REPORT_EVERY = 48L * 1024 * 1024;

    private RomExtractor() {
    }

    /** A raw disc image we copy as-is (uncompressed ISO, or an already compressed CSO). */
    public static boolean looksLikeIso(String displayName) {
        if (displayName == null) {
            return false;
        }
        String n = displayName.toLowerCase(Locale.ROOT);
        return n.endsWith(".iso") || n.endsWith(".img") || n.endsWith(".bin")
                || n.endsWith(".cso");
    }

    /**
     * @param displayName file name from the picker, used to tell raw images from archives
     * @return number of bytes written
     */
    public static long extract(InputStream source, String displayName, OutputStream target,
                               Progress progress, CancelCheck cancel) throws IOException {
        if (looksLikeIso(displayName)) {
            return pump(source, target, -1L, "Copiando a imagem...", progress, cancel);
        }
        ZipInputStream zip = new ZipInputStream(source);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".iso") || name.endsWith(".bin") || name.endsWith(".img")
                    || name.endsWith(".cso")) {
                return pump(zip, target, entry.getSize(), "Extraindo a imagem PS2...", progress,
                        cancel);
            }
        }
        throw new IOException("O arquivo não tem nenhuma imagem .iso dentro.");
    }

    private static long pump(InputStream in, OutputStream out, long expected, String stage,
                             Progress progress, CancelCheck cancel) throws IOException {
        byte[] buf = new byte[BUFFER];
        long done = 0L;
        long lastReport = 0L;
        int n;
        while ((n = in.read(buf)) > 0) {
            if (cancel != null && cancel.isCancelled()) {
                throw new IOException("Importação cancelada.");
            }
            out.write(buf, 0, n);
            done += n;
            if (progress != null && done - lastReport >= REPORT_EVERY) {
                lastReport = done;
                progress.update(done, expected, stage);
            }
        }
        if (progress != null) {
            progress.update(done, expected > 0 ? expected : done, stage);
        }
        if (expected > 0 && done != expected) {
            throw new IOException("A cópia terminou incompleta: " + done + " de " + expected
                    + " bytes.");
        }
        return done;
    }
}

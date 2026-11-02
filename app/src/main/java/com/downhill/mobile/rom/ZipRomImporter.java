package com.downhill.mobile.rom;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streams the downloaded archive straight into the app's private storage.
 *
 * <p>Nothing is held in memory: a 1 MB buffer copies the deflated entry into
 * {@code downhill.iso.part}, and {@link ZipInputStream} verifies the CRC of the
 * 2.4 GB stream on the way out, so a truncated download is caught instead of
 * producing a silently broken image. The file is only renamed after the copy
 * completes, which makes the import restartable.</p>
 */
public final class ZipRomImporter {

    public interface Callback {
        /** @param total expected bytes, or -1 when the source does not tell */
        void onProgress(long done, long total, String stage);

        void onFinished(IsoInfo info, String error);
    }

    private static final int BUFFER = 1 << 20;

    private final Context app;
    private final RomStore store;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ZipRomImporter(Context context, RomStore store) {
        this.app = context.getApplicationContext();
        this.store = store;
    }

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void start(final Uri uri, final Callback callback) {
        cancelled.set(false);
        worker.execute(() -> {
            String error = null;
            IsoInfo info = null;
            try {
                long missing = store.missingSpace();
                if (missing > 0) {
                    throw new IOException(String.format(Locale.US,
                            "Espaço insuficiente: faltam %.2f GB livres.",
                            missing / 1073741824.0));
                }
                File part = store.partFile();
                long written = copy(uri, part, callback);
                if (cancelled.get()) {
                    part.delete();
                    throw new IOException("Importação cancelada.");
                }
                File iso = store.isoFile();
                if (iso.exists() && !iso.delete()) {
                    throw new IOException("Não foi possível substituir o arquivo anterior.");
                }
                if (!part.renameTo(iso)) {
                    throw new IOException("Não foi possível finalizar o arquivo.");
                }
                post(callback, written, written, "Verificando a imagem...");
                try (FileBlockReader r = new FileBlockReader(iso)) {
                    info = IsoParser.parse(r);
                } catch (IOException e) {
                    info = null;
                }
                store.rememberInfo(info);
            } catch (IOException | RuntimeException e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final IsoInfo parsed = info;
            final String failure = error;
            main.post(() -> callback.onFinished(parsed, failure));
        });
        worker.shutdown();
    }

    /**
     * Concatenates the split CSO parts of the release into {@code downhill.cso}.
     *
     * <p>The parts can be selected in any order: they are sorted by their numeric
     * suffix ({@code .000}, {@code .001}) and the result is byte-identical to the
     * published CSO, which the joiner verifies by size.</p>
     */
    public void startJoin(final List<Uri> parts, final Callback callback) {
        cancelled.set(false);
        worker.execute(() -> {
            String error = null;
            IsoInfo info = null;
            try {
                if (parts == null || parts.isEmpty()) {
                    throw new IOException("Nenhuma parte selecionada.");
                }
                List<Uri> ordered = orderByPartName(parts);
                long free = store.freeBytes();
                if (free > 0 && free < 2_900_000_000L) {
                    throw new IOException(String.format(Locale.US,
                            "Espaço insuficiente: o jogo completo precisa de 2,5 GB livres "
                                    + "(livre agora: %.2f GB).", free / 1073741824.0));
                }
                File target = store.joinPartFile();
                List<InputStream> streams = new ArrayList<>();
                long written;
                try (BufferedOutputStream out = new BufferedOutputStream(
                        new FileOutputStream(target, false), BUFFER)) {
                    for (Uri uri : ordered) {
                        streams.add(app.getContentResolver().openInputStream(uri));
                    }
                    written = PartJoiner.join(streams, out,
                            (done, total) -> post(callback, done, 2_500_000_000L, "Juntando as partes..."),
                            cancelled::get);
                } finally {
                    for (InputStream in : streams) {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException ignored) {
                                // best effort
                            }
                        }
                    }
                }
                if (cancelled.get()) {
                    target.delete();
                    throw new IOException("Junção cancelada.");
                }
                File cso = store.csoFile();
                if (cso.exists() && !cso.delete()) {
                    throw new IOException("Não foi possível substituir o arquivo anterior.");
                }
                if (!target.renameTo(cso)) {
                    throw new IOException("Não foi possível finalizar o arquivo juntado.");
                }
                post(callback, written, written, "Verificando o jogo...");
                info = store.sniffInfo();
                File iso = store.isoFile();
                if (iso.exists()) {
                    iso.delete();
                }
            } catch (IOException | RuntimeException e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final IsoInfo parsed = info;
            final String failure = error;
            main.post(() -> callback.onFinished(parsed, failure));
        });
        worker.shutdown();
    }

    /** Sorts the picked documents by their part suffix; unreadable names go last. */
    private List<Uri> orderByPartName(List<Uri> parts) {
        List<String> names = new ArrayList<>();
        for (Uri uri : parts) {
            names.add(displayName(uri));
        }
        List<String> sorted = PartJoiner.sortedParts(names);
        List<Uri> ordered = new ArrayList<>();
        for (String name : sorted) {
            for (Uri uri : parts) {
                if (displayName(uri).equals(name) && !ordered.contains(uri)) {
                    ordered.add(uri);
                    break;
                }
            }
        }
        return ordered;
    }

    // ------------------------------------------------------------- sources --

    /** Streams the picked document into {@code part}; the heavy lifting is in {@link RomExtractor}. */
    private long copy(Uri uri, File part, Callback cb) throws IOException {
        String name = displayName(uri);
        try (InputStream in = app.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Não foi possível abrir o arquivo escolhido.");
            }
            try (BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(part, false), BUFFER)) {
                return RomExtractor.extract(
                        new BufferedInputStream(in, BUFFER),
                        name,
                        out,
                        (done, total, stage) -> post(cb, done, total, stage),
                        cancelled::get);
            }
        }
    }

    // -------------------------------------------------------------- helpers --

    private void post(Callback cb, long done, long total, String stage) {
        main.post(() -> cb.onProgress(done, total, stage));
    }

    private String displayName(Uri uri) {
        try (Cursor c = app.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && !c.isNull(idx)) {
                    return c.getString(idx);
                }
            }
        } catch (RuntimeException ignored) {
            // providers are free to reject queries
        }
        String path = uri.getLastPathSegment();
        return path == null ? "" : path;
    }
}

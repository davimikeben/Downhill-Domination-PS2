package com.downhill.mobile.rom;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Read-only provider that hands the imported ISO (and the exported pad kit) to
 * the emulator, which needs a {@code content://} URI because apps can no longer
 * read each other's files directly.
 */
public class AppFilesProvider extends ContentProvider {

    public static final String AUTHORITY_SUFFIX = ".files";

    public static Uri uriFor(Context context, File file) {
        File base = context.getApplicationContext().getFilesDir();
        String rel = file.getAbsolutePath().substring(base.getAbsolutePath().length());
        if (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        return Uri.parse("content://" + context.getPackageName() + AUTHORITY_SUFFIX + "/" + rel);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        Context ctx = getContext();
        if (ctx == null || uri.getPath() == null) {
            throw new FileNotFoundException("sem contexto");
        }
        String rel = uri.getPath();
        if (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        if (rel.contains("..")) {
            throw new FileNotFoundException("caminho inválido");
        }
        File f = new File(ctx.getFilesDir(), rel);
        if (!f.isFile()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (mode != null && mode.contains("w")) {
            throw new FileNotFoundException("provedor somente leitura");
        }
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        File f;
        try {
            f = resolve(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] cols = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) {
                row[i] = f.getName();
            } else if (OpenableColumns.SIZE.equals(cols[i])) {
                row[i] = f.length();
            } else {
                row[i] = null;
            }
        }
        c.addRow(row);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".cfg") || path.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("somente leitura");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("somente leitura");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("somente leitura");
    }
}

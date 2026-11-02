package com.downhill.mobile.export;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.rom.AppFilesProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Writes the "pad kit" (overlay cfg + matching PNG + instructions) into the app
 * folder and offers it through the share sheet, so the exact same pad can be
 * dropped into RetroArch.
 */
public final class OverlayExporter {

    private OverlayExporter() {
    }

    public static File dir(Context context) {
        File d = new File(context.getFilesDir(), "export");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    /**
     * @return {cfg, png, readme}
     */
    public static File[] export(Context context) throws IOException {
        File d = dir(context);
        File cfg = new File(d, RetroArchOverlay.CFG_NAME);
        File png = new File(d, RetroArchOverlay.PNG_NAME);
        File txt = new File(d, "LEIA-ME.txt");

        PadGeometry geo = PadGeometry.compute(PadBitmap.WIDTH, PadBitmap.HEIGHT, 2.0f, true, 1f,
                null);
        write(cfg, RetroArchOverlay.build(geo, PadBitmap.WIDTH, PadBitmap.HEIGHT));
        write(txt, RetroArchOverlay.readme());

        Bitmap bmp = PadBitmap.render();
        try (FileOutputStream out = new FileOutputStream(png)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            bmp.recycle();
        }
        return new File[]{cfg, png, txt};
    }

    private static void write(File f, String text) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(text);
        }
    }

    /** Multi-file share intent for the exported kit. */
    public static Intent shareIntent(Context context, File[] files) {
        Uri[] uris = new Uri[files.length];
        for (int i = 0; i < files.length; i++) {
            uris[i] = AppFilesProvider.uriFor(context, files[i]);
        }
        Intent send = new Intent(Intent.ACTION_SEND_MULTIPLE);
        send.setType("*/*");
        java.util.ArrayList<Uri> list = new java.util.ArrayList<>();
        java.util.Collections.addAll(list, uris);
        send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(send, "Enviar pad para o RetroArch");
    }
}

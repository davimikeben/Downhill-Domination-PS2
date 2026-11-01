package com.downhill.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.downhill.mobile.export.OverlayExporter;
import com.downhill.mobile.export.RetroArchOverlay;
import com.downhill.mobile.legend.LegendActivity;
import com.downhill.mobile.pad.PadGeometry;
import com.downhill.mobile.pad.Ps2Button;
import com.downhill.mobile.pad.VirtualPadView;
import com.downhill.mobile.rom.AppFilesProvider;
import com.downhill.mobile.rom.RomStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Boots the real activities on the JVM: proves the screens build, the pad draws
 * into a bitmap, the exporter writes the three files and the content provider
 * serves the image. This is the closest thing to "installed it and opened it"
 * that a headless CI can do.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w2400dp-h1080dp-land-hdpi")
public class AppSmokeTest {

    @Test
    public void mainActivityStartsAndShowsTheFourCards() {
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertNotNull(activity);
        ViewGroup content = activity.findViewById(android.R.id.content);
        assertEquals(1, content.getChildCount());
        String texts = allText(content.getChildAt(0));
        assertTrue(texts, texts.contains("Importar ROM"));
        assertTrue(texts, texts.contains("JOGAR"));
        assertTrue(texts, texts.contains("Testar teclado"));
        assertTrue(texts, texts.contains("Ver ajustes"));
        assertTrue(texts, texts.contains("Nenhuma imagem importada") || texts.contains("Faltam"));
    }

    @Test
    public void padLabDrawsTheWholePadWithoutCrashing() {
        Activity activity = Robolectric.buildActivity(PadLabActivity.class).setup().get();
        View root = activity.findViewById(android.R.id.content);
        int w = 2400;
        int h = 1080;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);

        VirtualPadView pad = findPad(root);
        assertNotNull("the pad view must be part of the lab", pad);
        PadGeometry geo = pad.geometry();
        assertNotNull(geo);
        assertEquals(21, geo.boxes().size());

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        root.draw(canvas);
        pad.draw(canvas);
        assertTrue(bitmap.getWidth() == w);
    }

    @Test
    public void legendListsEveryKeyWithItsEmoji() {
        Activity activity = Robolectric.buildActivity(LegendActivity.class).setup().get();
        String texts = allText(activity.findViewById(android.R.id.content));
        for (Ps2Button b : Ps2Button.values()) {
            assertTrue("legend is missing " + b, texts.contains(b.emoji));
            assertTrue("legend is missing the hint of " + b, texts.contains(b.hint.split(" ")[0]));
        }
    }

    @Test
    public void exporterWritesCfgPngAndReadme() throws IOException {
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        File[] files = OverlayExporter.export(activity);
        assertEquals(3, files.length);
        for (File f : files) {
            assertTrue(f.getName() + " was not written", f.isFile());
        }
        String cfg = read(files[0]);
        assertTrue(cfg.contains("overlay0_overlay = " + RetroArchOverlay.PNG_NAME));
        assertTrue(cfg.contains("overlay0_desc22"));
        assertTrue(files[1].length() > 1024);
    }

    @Test
    public void providerServesTheImportedImageReadOnly() throws IOException {
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        RomStore store = new RomStore(activity);
        File iso = store.isoFile();
        try (FileOutputStream out = new FileOutputStream(iso)) {
            out.write(new byte[4096]);
        }
        assertTrue(iso.isFile());

        AppFilesProvider provider = Robolectric.setupContentProvider(AppFilesProvider.class);
        Uri uri = AppFilesProvider.uriFor(activity, iso);
        assertEquals("application/octet-stream", provider.getType(uri));
        ParcelFileDescriptor fd = provider.openFile(uri, "r");
        assertNotNull(fd);
        fd.close();
        try {
            provider.openFile(uri, "w");
            throw new AssertionError("the provider must be read only");
        } catch (IOException expected) {
            // ok
        }
        assertTrue(store.size() >= 4096);
    }

    @Test
    public void importerEndToEndThroughTheContentResolver() throws Exception {
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        RomStore store = new RomStore(activity);
        store.clear();

        // a small zip that carries a synthetic ISO with a PS2 boot id
        File zip = new File(activity.getCacheDir(), "downhill.zip");
        byte[] iso = SyntheticIso.build("BOOT2 = cdrom0:\\SLES_538.28;1\r\n");
        try (java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(
                new FileOutputStream(zip))) {
            z.putNextEntry(new java.util.zip.ZipEntry("Downhill Domination (Europe).iso"));
            z.write(iso);
            z.closeEntry();
        }

        final Object[] result = new Object[2];
        com.downhill.mobile.rom.ZipRomImporter importer =
                new com.downhill.mobile.rom.ZipRomImporter(activity, store);
        importer.start(Uri.fromFile(zip), new com.downhill.mobile.rom.ZipRomImporter.Callback() {
            @Override
            public void onProgress(long done, long total, String stage) {
                // progress is exercised, values are noise for the assertion
            }

            @Override
            public void onFinished(com.downhill.mobile.rom.IsoInfo info, String error) {
                result[0] = info;
                result[1] = error;
            }
        });
        ShadowLooper.idleMainLooper();
        for (int i = 0; i < 200 && result[0] == null && result[1] == null; i++) {
            Thread.sleep(25);
            ShadowLooper.idleMainLooper();
        }
        assertEquals("import failed: " + result[1], null, result[1]);
        assertTrue(store.isoFile().isFile());
        assertEquals(iso.length, store.isoFile().length());
        com.downhill.mobile.rom.IsoInfo info =
                (com.downhill.mobile.rom.IsoInfo) result[0];
        assertNotNull(info);
        assertEquals("SLES-53828", info.serial);
        assertTrue(info.downhillDomination);
    }

    // --------------------------------------------------------------- helpers --

    private static VirtualPadView findPad(View v) {
        if (v instanceof VirtualPadView) {
            return (VirtualPadView) v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                VirtualPadView found = findPad(g.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String allText(View v) {
        StringBuilder sb = new StringBuilder();
        if (v instanceof TextView) {
            sb.append(((TextView) v).getText()).append('\n');
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                sb.append(allText(g.getChildAt(i)));
            }
        }
        return sb.toString();
    }

    private static String read(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            int read = 0;
            while (read < b.length) {
                int n = in.read(b, read, b.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
        }
        return new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }
}

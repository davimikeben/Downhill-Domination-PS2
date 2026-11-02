package com.downhill.mobile.rom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exercises the real import path used for the user's 2.4 GB zip: a deflated ISO
 * inside, a raw ISO, a zip without an image, a truncated entry and a cancel.
 */
public class RomExtractorTest {

    private static byte[] isoPayload(int bytes) {
        byte[] b = new byte[bytes];
        new Random(42).nextBytes(b);
        return b;
    }

    private static byte[] zipWith(String entryName, byte[] payload) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(payload);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    public void extractsTheIsoFromTheZip() throws IOException {
        byte[] payload = isoPayload(300_000);
        byte[] zip = zipWith("Downhill Domination (Europe) (En,Fr,De,Es,It).iso", payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String> stages = new ArrayList<>();
        long written = RomExtractor.extract(new ByteArrayInputStream(zip), "rom.zip", out,
                (done, total, stage) -> stages.add(stage), () -> false);

        assertEquals(payload.length, written);
        assertTrue(java.util.Arrays.equals(payload, out.toByteArray()));
        assertFalse(stages.isEmpty());
    }

    @Test
    public void copiesARawIsoWithoutUnzipping() throws IOException {
        byte[] payload = isoPayload(120_000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long written = RomExtractor.extract(new ByteArrayInputStream(payload),
                "Downhill.Domination.iso", out, null, () -> false);
        assertEquals(payload.length, written);
        assertTrue(java.util.Arrays.equals(payload, out.toByteArray()));
    }

    @Test
    public void detectsTheImageByExtensionCaseInsensitively() {
        assertTrue(RomExtractor.looksLikeIso("JOGO.ISO"));
        assertTrue(RomExtractor.looksLikeIso("jogo.img"));
        assertFalse(RomExtractor.looksLikeIso("jogo.zip"));
        assertFalse(RomExtractor.looksLikeIso(null));
    }

    @Test
    public void azipWithoutAnImageFails() throws IOException {
        byte[] zip = zipWith("leiame.txt", "nada aqui".getBytes(StandardCharsets.UTF_8));
        try {
            RomExtractor.extract(new ByteArrayInputStream(zip), "rom.zip",
                    new ByteArrayOutputStream(), null, () -> false);
            fail("must reject a zip without an image");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(".iso"));
        }
    }

    @Test
    public void truncatedEntryIsRejected() throws IOException {
        byte[] payload = isoPayload(200_000);
        byte[] zip = zipWith("game.iso", payload);
        byte[] cut = new byte[zip.length / 2];
        System.arraycopy(zip, 0, cut, 0, cut.length);
        try {
            RomExtractor.extract(new ByteArrayInputStream(cut), "rom.zip",
                    new ByteArrayOutputStream(), null, () -> false);
            fail("a half downloaded zip must not pass");
        } catch (IOException expected) {
            // ZipInputStream raises "unexpected end of ZLIB input stream"
            assertTrue(expected.getMessage() != null);
        }
    }

    @Test
    public void cancelStopsTheCopy() {
        byte[] payload = isoPayload(200_000);
        try {
            RomExtractor.extract(new ByteArrayInputStream(payload), "game.iso",
                    new ByteArrayOutputStream(), null, () -> true);
            fail("cancel must abort");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("cancelada"));
        }
    }
}

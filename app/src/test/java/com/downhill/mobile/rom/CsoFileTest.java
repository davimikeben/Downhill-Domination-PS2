package com.downhill.mobile.rom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.Deflater;

/** Round-trips the CISO reader that lets the app sniff a serial out of the big CSO. */
public class CsoFileTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final int BLOCK = 2048;

    private static File writeCso(File target, byte[] data) throws IOException {
        return writeCso(target, data, 0);
    }

    /**
     * Writes a CISO v1 file from {@code data}, mixing deflated and plain blocks.
     *
     * @param align offset shift stored in the header (2 is what the release uses for >2 GB)
     */
    private static File writeCso(File target, byte[] data, int align) throws IOException {
        int step = 1 << align;
        int blocks = (data.length + BLOCK - 1) / BLOCK;
        int[] index = new int[blocks];
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(9);
        for (int i = 0; i < blocks; i++) {
            int off = i * BLOCK;
            int len = Math.min(BLOCK, data.length - off);
            byte[] block = new byte[len];
            System.arraycopy(data, off, block, 0, len);
            deflater.reset();
            deflater.setInput(block);
            deflater.finish();
            byte[] out = new byte[block.length + 64];
            int n = deflater.deflate(out);
            int position = CsoFile.MAGIC_SIZE + blocks * 4 + body.size();
            byte[] payload = n >= block.length ? block : java.util.Arrays.copyOf(out, n);
            boolean plain = n >= block.length;
            index[i] = (position >> align) | (plain ? 0x80000000 : 0);
            body.write(payload);
            int pad = (-payload.length) % step;
            for (int k = 0; k < pad; k++) {
                body.write(0);
            }
        }
        deflater.end();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            head.write(new byte[]{'C', 'I', 'S', 'O'});
            head.write(le32(CsoFile.MAGIC_SIZE));
            head.write(le64(data.length));
            head.write(le32(BLOCK));
            head.write(1);
            head.write(align);
            head.write(new byte[]{0, 0});
            for (int entry : index) {
                head.write(le32(entry));
            }
            fos.write(head.toByteArray());
            fos.write(body.toByteArray());
        }
        return target;
    }

    private static byte[] le32(long v) {
        return new byte[]{(byte) (v & 0xFF), (byte) ((v >> 8) & 0xFF),
                (byte) ((v >> 16) & 0xFF), (byte) ((v >> 24) & 0xFF)};
    }

    private static byte[] le64(long v) {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) ((v >>> (8 * i)) & 0xFF);
        }
        return b;
    }

    @Test
    public void headerAndSectorsSurviveTheRoundTrip() throws IOException {
        byte[] data = new byte[BLOCK * 40];
        new Random(7).nextBytes(data);
        File cso = writeCso(tmp.newFile("game.cso"), data);

        CsoFile.Header header = CsoFile.readHeader(cso);
        assertEquals(data.length, header.totalBytes);
        assertEquals(BLOCK, header.blockSize);
        assertEquals(40, header.blocks);

        byte[] first = CsoFile.readFirstSectors(cso, 8);
        assertNotNull(first);
        assertEquals(8 * BLOCK, first.length);
        byte[] expected = new byte[8 * BLOCK];
        System.arraycopy(data, 0, expected, 0, expected.length);
        assertArrayEquals(expected, first);

        byte[] middle = CsoFile.readSectors(cso, 17, 3);
        assertNotNull(middle);
        byte[] wanted = new byte[3 * BLOCK];
        System.arraycopy(data, 17 * BLOCK, wanted, 0, wanted.length);
        assertArrayEquals(wanted, middle);
    }

    @Test
    public void sniffsTheSerialOutOfACompressedIso() throws IOException {
        byte[] iso = SyntheticIsoData.build("BOOT2 = cdrom0:\\SLES_522.02;1\r\n");
        File cso = writeCso(tmp.newFile("downhill.cso"), iso);

        byte[] head = CsoFile.readFirstSectors(cso, 24);
        assertNotNull(head);
        IsoInfo info = IsoParser.parse((offset, dst, len) -> {
            System.arraycopy(head, (int) offset, dst, 0, len);
        });
        assertNotNull(info);
        assertEquals("SLES-52202", info.serial);
        assertTrue(info.downhillDomination);
    }

    @Test
    public void alignedIndexIsReadBackExactly() throws IOException {
        // the published CSO uses align=2 because its offsets do not fit in 31 bits
        byte[] data = new byte[BLOCK * 30];
        new Random(11).nextBytes(data);
        File cso = writeCso(tmp.newFile("aligned.cso"), data, 2);

        CsoFile.Header header = CsoFile.readHeader(cso);
        assertEquals(2, header.align);

        byte[] all = CsoFile.readSectors(cso, 0, 30);
        assertArrayEquals(data, all);
        byte[] tail = CsoFile.readSectors(cso, 27, 3);
        byte[] wanted = new byte[3 * BLOCK];
        System.arraycopy(data, 27 * BLOCK, wanted, 0, wanted.length);
        assertArrayEquals(wanted, tail);
    }

    @Test
    public void plainBlocksAndGarbageAreHandled() throws IOException {
        byte[] random = new byte[BLOCK * 4];
        new Random(3).nextBytes(random);
        File cso = writeCso(tmp.newFile("plain.cso"), random);
        byte[] first = CsoFile.readFirstSectors(cso, 4);
        assertArrayEquals(random, first);

        File notCso = tmp.newFile("not.cso");
        try (FileOutputStream out = new FileOutputStream(notCso)) {
            out.write("hello".getBytes("UTF-8"));
        }
        assertFalse(CsoFile.hasMagic(java.nio.file.Files.readAllBytes(notCso.toPath())));
        org.junit.Assert.assertNull(CsoFile.readFirstSectors(notCso, 4));
    }
}

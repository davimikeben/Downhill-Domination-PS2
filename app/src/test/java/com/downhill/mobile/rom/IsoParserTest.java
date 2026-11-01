package com.downhill.mobile.rom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Builds a miniature ISO 9660 image in memory and checks that the parser finds
 * the PS2 boot id the same way it does on the real 2.77 GB disc.
 */
public class IsoParserTest {

    private static final int SECTOR = 2048;

    private static void putLe32(byte[] b, int off, long v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >> 8) & 0xFF);
        b[off + 2] = (byte) ((v >> 16) & 0xFF);
        b[off + 3] = (byte) ((v >> 24) & 0xFF);
    }

    private static void putBe32(byte[] b, int off, long v) {
        b[off] = (byte) ((v >> 24) & 0xFF);
        b[off + 1] = (byte) ((v >> 16) & 0xFF);
        b[off + 2] = (byte) ((v >> 8) & 0xFF);
        b[off + 3] = (byte) (v & 0xFF);
    }

    /** Writes one ISO 9660 directory record and returns its length. */
    private static int dirRecord(byte[] target, int off, int lba, int size, boolean directory,
                                 String name, boolean selfOrParent) {
        int nameLen = selfOrParent ? 1 : name.length();
        int len = 33 + nameLen + ((nameLen % 2 == 0) ? 1 : 0);
        target[off] = (byte) len;
        target[off + 1] = 0;
        putLe32(target, off + 2, lba);
        putBe32(target, off + 6, lba);
        putLe32(target, off + 10, size);
        putBe32(target, off + 14, size);
        target[off + 25] = (byte) (directory ? 0x02 : 0x00);
        target[off + 26] = 0;
        target[off + 27] = 0;
        target[off + 28] = 1;
        target[off + 29] = 0;
        target[off + 30] = 1;
        target[off + 31] = 0;
        target[off + 32] = (byte) nameLen;
        if (selfOrParent) {
            target[off + 33] = 0;
        } else {
            byte[] n = name.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(n, 0, target, off + 33, n.length);
        }
        return len;
    }

    /** 32 sectors: PVD at 16, root dir at 20, SYSTEM.CNF content at 21. */
    private static byte[] syntheticIso(String bootLine) {
        byte[] iso = new byte[34 * SECTOR];

        int pvd = 16 * SECTOR;
        iso[pvd] = 1;
        byte[] magic = "CD001".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, iso, pvd + 1, 5);
        iso[pvd + 6] = 1;
        byte[] label = "DOWNHILL_DOMINATION".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(label, 0, iso, pvd + 40, label.length);
        dirRecord(iso, pvd + 156, 20, SECTOR, true, "", true);

        int root = 20 * SECTOR;
        int off = root;
        off += dirRecord(iso, off, 20, SECTOR, true, "", true);
        off += dirRecord(iso, off, 20, SECTOR, true, "", true);
        byte[] cnfText = bootLine.getBytes(StandardCharsets.US_ASCII);
        dirRecord(iso, off, 21, cnfText.length, false, "SYSTEM.CNF;1", false);

        System.arraycopy(cnfText, 0, iso, 21 * SECTOR, cnfText.length);
        return iso;
    }

    private static IsoParser.BlockReader reader(byte[] iso) {
        return (offset, dst, len) -> {
            if (offset + len > iso.length) {
                throw new IOException("read past end: " + offset + "+" + len);
            }
            System.arraycopy(iso, (int) offset, dst, 0, len);
        };
    }

    @Test
    public void readsTheEuropeanBootId() throws IOException {
        byte[] iso = syntheticIso("BOOT2 = cdrom0:\\SLES_538.28;1\r\nVER = 1.00\r\nVMODE = NTSC\r\n");
        IsoInfo info = IsoParser.parse(reader(iso));
        assertNotNull(info);
        assertEquals("DOWNHILL_DOMINATION", info.volumeLabel);
        assertEquals("SLES_538.28", info.bootFile);
        assertEquals("SLES-53828", info.serial);
        assertEquals("PAL", info.region);
        assertTrue("must be recognised as Downhill Domination", info.downhillDomination);
    }

    @Test
    public void readsTheUsBootId() throws IOException {
        byte[] iso = syntheticIso("BOOT2 = cdrom0:\\SLUS_207.05;1\r\nVER = 1.00\r\n");
        IsoInfo info = IsoParser.parse(reader(iso));
        assertNotNull(info);
        assertEquals("SLUS-20705", info.serial);
        assertEquals("NTSC-U", info.region);
        assertTrue(info.downhillDomination);
    }

    @Test
    public void anotherGameIsImportedButNotClaimedAsDownhill() throws IOException {
        byte[] iso = syntheticIso("BOOT2 = cdrom0:\\SLES_500.00;1\r\n");
        IsoInfo info = IsoParser.parse(reader(iso));
        assertNotNull(info);
        assertEquals("SLES-50000", info.serial);
        assertFalse(info.downhillDomination);
    }

    @Test
    public void junkImageReturnsNull() throws IOException {
        byte[] zeros = new byte[34 * SECTOR];
        assertNull(IsoParser.parse(reader(zeros)));
    }

    @Test
    public void serialAndRegionHelpers() {
        assertEquals("SLES-53828", IsoInfo.serialOf("SLES_538.28"));
        assertEquals("SLUS-20705", IsoInfo.serialOf("SLUS_207.05"));
        assertEquals("", IsoInfo.serialOf("NOIDEOFGAME"));
        assertEquals("PAL", IsoInfo.regionOf("SLES-53828"));
        assertEquals("NTSC-U", IsoInfo.regionOf("SLUS-20705"));
        assertEquals("NTSC-J", IsoInfo.regionOf("SLPS-25888"));
        assertEquals("?", IsoInfo.regionOf("XX"));
        assertEquals("?", IsoInfo.regionOf(null));
    }
}

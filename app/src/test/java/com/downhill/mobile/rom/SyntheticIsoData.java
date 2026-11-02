package com.downhill.mobile.rom;

import java.nio.charset.StandardCharsets;

/** Minimal ISO 9660 image with a SYSTEM.CNF, shared by the smoke tests. */
final class SyntheticIsoData {

    private static final int SECTOR = 2048;

    private SyntheticIsoData() {
    }

    static byte[] build(String bootLine) {
        byte[] iso = new byte[34 * SECTOR];
        int pvd = 16 * SECTOR;
        iso[pvd] = 1;
        byte[] magic = "CD001".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, iso, pvd + 1, 5);
        iso[pvd + 6] = 1;
        byte[] label = "DOWNHILL_DOMINATION".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(label, 0, iso, pvd + 40, label.length);
        dirRecord(iso, pvd + 156, 20, SECTOR, true, "", true);

        int off = 20 * SECTOR;
        off += dirRecord(iso, off, 20, SECTOR, true, "", true);
        off += dirRecord(iso, off, 20, SECTOR, true, "", true);
        byte[] cnf = bootLine.getBytes(StandardCharsets.US_ASCII);
        dirRecord(iso, off, 21, cnf.length, false, "SYSTEM.CNF;1", false);
        System.arraycopy(cnf, 0, iso, 21 * SECTOR, cnf.length);
        return iso;
    }

    private static int dirRecord(byte[] t, int off, int lba, int size, boolean dir,
                                 String name, boolean self) {
        int nameLen = self ? 1 : name.length();
        int len = 33 + nameLen + ((nameLen % 2 == 0) ? 1 : 0);
        t[off] = (byte) len;
        le32(t, off + 2, lba);
        le32(t, off + 10, size);
        t[off + 25] = (byte) (dir ? 0x02 : 0x00);
        t[off + 32] = (byte) nameLen;
        if (self) {
            t[off + 33] = 0;
        } else {
            byte[] n = name.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(n, 0, t, off + 33, n.length);
        }
        return len;
    }

    private static void le32(byte[] b, int off, long v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >> 8) & 0xFF);
        b[off + 2] = (byte) ((v >> 16) & 0xFF);
        b[off + 3] = (byte) ((v >> 24) & 0xFF);
    }
}

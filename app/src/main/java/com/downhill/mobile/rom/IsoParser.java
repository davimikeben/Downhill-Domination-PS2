package com.downhill.mobile.rom;

import java.io.IOException;

/**
 * Reads the PlayStation 2 boot id out of an ISO 9660 image.
 *
 * <p>A PS2 disc stores the executable name in {@code SYSTEM.CNF}:</p>
 *
 * <pre>
 * BOOT2 = cdrom0:\SLES_538.28;1
 * VER   = 1.00
 * </pre>
 *
 * <p>The parser only needs a few hundred bytes from three sectors, so it can be
 * run right after the import to prove the archive really contains Downhill
 * Domination - without loading 2.7 GB into memory. Plain Java on purpose: the
 * whole path is covered by JVM unit tests.</p>
 */
public final class IsoParser {

    public static final int SECTOR = 2048;

    /** Seekable byte source (a file, a {@code FileChannel}, or a test array). */
    public interface BlockReader {
        /** Fills {@code dst} with {@code len} bytes starting at {@code offset}. */
        void read(long offset, byte[] dst, int len) throws IOException;
    }

    private IsoParser() {
    }

    /**
     * @return the parsed info, or {@code null} when the image has no ISO 9660
     *         primary volume descriptor (nothing readable was found)
     */
    public static IsoInfo parse(BlockReader reader) throws IOException {
        byte[] pvd = new byte[SECTOR];
        reader.read(16L * SECTOR, pvd, SECTOR);
        if (!"CD001".equals(ascii(pvd, 1, 5)) || pvd[0] != 1) {
            return null;
        }

        String label = ascii(pvd, 40, 32).trim();
        int rootLen = u8(pvd, 156);
        if (rootLen < 34) {
            return null;
        }
        long rootLba = u32le(pvd, 156 + 2);
        long rootSize = u32le(pvd, 156 + 10);

        byte[] cnf = null;
        for (Entry e : listDirectory(reader, rootLba, rootSize)) {
            if (e.name.equalsIgnoreCase("SYSTEM.CNF;1") || e.name.equalsIgnoreCase("SYSTEM.CNF")) {
                cnf = readFile(reader, e);
                break;
            }
        }
        if (cnf == null) {
            return null;
        }
        String text = new String(cnf, java.nio.charset.StandardCharsets.ISO_8859_1);
        String boot2 = null;
        for (String line : text.split("\r?\n")) {
            String t = line.trim();
            if (t.toUpperCase(java.util.Locale.ROOT).startsWith("BOOT2")) {
                int eq = t.indexOf('=');
                if (eq > 0) {
                    boot2 = t.substring(eq + 1).trim();
                    break;
                }
            }
        }
        if (boot2 == null || boot2.isEmpty()) {
            return null;
        }
        return IsoInfo.of(label, boot2);
    }

    /** Reads a whole (small) file such as SYSTEM.CNF. */
    private static byte[] readFile(BlockReader reader, Entry e) throws IOException {
        if (e.size <= 0 || e.size > 1 << 20) {
            return null;
        }
        int sectors = (int) ((e.size + SECTOR - 1) / SECTOR);
        byte[] buf = new byte[sectors * SECTOR];
        reader.read(e.lba * SECTOR, buf, buf.length);
        byte[] out = new byte[(int) e.size];
        System.arraycopy(buf, 0, out, 0, out.length);
        return out;
    }

    private static java.util.List<Entry> listDirectory(BlockReader reader, long lba, long size)
            throws IOException {
        java.util.List<Entry> out = new java.util.ArrayList<>();
        int sectors = (int) Math.min(64, (size + SECTOR - 1) / SECTOR);
        for (int s = 0; s < sectors; s++) {
            byte[] sector = new byte[SECTOR];
            reader.read((lba + s) * SECTOR, sector, SECTOR);
            int pos = 0;
            while (pos < SECTOR) {
                int len = u8(sector, pos);
                if (len == 0) {
                    break;
                }
                if (pos + len > SECTOR) {
                    break;
                }
                int nameLen = u8(sector, pos + 32);
                boolean special = (u8(sector, pos + 25) & 0x02) != 0;
                if (nameLen > 0 && nameLen <= 222 && !special) {
                    String name = ascii(sector, pos + 33, nameLen);
                    if (!".".equals(name) && !"..".equals(name)) {
                        out.add(new Entry(name, u32le(sector, pos + 2), u32le(sector, pos + 10)));
                    }
                }
                pos += len;
            }
        }
        return out;
    }

    private static String ascii(byte[] b, int off, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len && off + i < b.length; i++) {
            int c = b[off + i] & 0xFF;
            sb.append(c == 0 ? ' ' : (char) c);
        }
        return sb.toString();
    }

    private static int u8(byte[] b, int off) {
        return off < b.length ? b[off] & 0xFF : 0;
    }

    private static long u32le(byte[] b, int off) {
        return (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8)
                | ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24);
    }

    private static final class Entry {
        final String name;
        final long lba;
        final long size;

        Entry(String name, long lba, long size) {
            this.name = name;
            this.lba = lba;
            this.size = size;
        }
    }
}

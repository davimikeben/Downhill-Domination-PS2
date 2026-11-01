package com.downhill.mobile.rom;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Reads CISO ("CSO") images - the compressed PS2 disc format that PCSX2 based
 * emulators accept.
 *
 * <p>Layout: {@code 'CISO'} magic, header size, total uncompressed bytes, block
 * size, version, alignment, then one 32-bit index entry per block. Bit 31 of an
 * entry marks a block stored as-is; the low 31 bits are the byte offset divided
 * by the alignment.</p>
 *
 * <p>Plain Java, so the reader that sniffs the disc serial out of the CSO is
 * covered by JVM tests - and it only inflates the sectors it needs instead of
 * unpacking 2.7 GB.</p>
 */
public final class CsoFile {

    public static final int MAGIC_SIZE = 24;
    private static final int DEFAULT_BLOCK = 2048;

    public static final class Header {
        public final long totalBytes;
        public final int blockSize;
        public final int version;
        public final int align;
        public final int blocks;

        Header(long totalBytes, int blockSize, int version, int align) {
            this.totalBytes = totalBytes;
            this.blockSize = blockSize;
            this.version = version;
            this.align = align;
            this.blocks = (int) ((totalBytes + blockSize - 1) / blockSize);
        }

        @Override
        public String toString() {
            return "CISO{total=" + totalBytes + " block=" + blockSize + " v=" + version
                    + " align=" + align + "}";
        }
    }

    private CsoFile() {
    }

    public static boolean hasMagic(byte[] firstBytes) {
        return firstBytes != null && firstBytes.length >= 4
                && firstBytes[0] == 'C' && firstBytes[1] == 'I'
                && firstBytes[2] == 'S' && firstBytes[3] == 'O';
    }

    public static Header readHeader(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            return readHeader(raf);
        }
    }

    private static Header readHeader(RandomAccessFile raf) throws IOException {
        byte[] head = new byte[MAGIC_SIZE];
        raf.seek(0);
        raf.readFully(head);
        if (!hasMagic(head)) {
            throw new IOException("não é um arquivo CISO");
        }
        int headerSize = (int) u32(head, 4);
        long total = u64(head, 8);
        int block = (int) u32(head, 16);
        int version = head[20] & 0xFF;
        int align = head[21] & 0xFF;
        if (headerSize < MAGIC_SIZE || block <= 0 || total <= 0) {
            throw new IOException("cabeçalho CISO inválido");
        }
        return new Header(total, block > 0 ? block : DEFAULT_BLOCK, version, align);
    }

    /**
     * Inflates the first {@code count} raw sectors (2048 bytes each) of the image,
     * which is what {@link IsoParser} needs to find SYSTEM.CNF.
     *
     * @return the bytes, or {@code null} when the file is not a CISO image
     */
    public static byte[] readFirstSectors(File file, int count) throws IOException {
        return readSectors(file, 0, count);
    }

    /** Reads {@code count} sectors starting at {@code firstSector}. */
    public static byte[] readSectors(File file, int firstSector, int count) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            Header h;
            try {
                h = readHeader(raf);
            } catch (IOException e) {
                return null;
            }
            int[] index = readIndex(raf, h);
            int sectorSize = IsoParser.SECTOR;
            int perBlock = Math.max(1, h.blockSize / sectorSize);
            int firstBlock = firstSector / perBlock;
            int lastBlock = Math.min(index.length - 1,
                    (firstSector + count - 1) / perBlock);
            byte[] out = new byte[count * sectorSize];
            Inflater inflater = new Inflater();
            try {
                for (int b = firstBlock; b <= lastBlock; b++) {
                    byte[] block = readBlock(raf, h, index, b, inflater);
                    if (block == null) {
                        break;
                    }
                    long blockStartSector = (long) b * perBlock;
                    int copyFrom = (int) Math.max(0, (firstSector - blockStartSector) * sectorSize);
                    int copyTo = (int) Math.min(block.length,
                            (firstSector + count - blockStartSector) * sectorSize);
                    int destPos = (int) ((blockStartSector + copyFrom / sectorSize - firstSector)
                            * sectorSize);
                    if (destPos < 0 || copyTo <= copyFrom) {
                        continue;
                    }
                    int len = Math.min(copyTo - copyFrom, out.length - destPos);
                    System.arraycopy(block, copyFrom, out, destPos, len);
                }
            } finally {
                inflater.end();
            }
            return out;
        }
    }

    /**
     * Random access to the uncompressed image, block by block.
     *
     * <p>This is what {@link IsoParser} uses: it may ask for the volume descriptor
     * and then for whatever sector the root directory lives in, so the reader has to
     * inflate on demand instead of pre-buffering a fixed window.</p>
     */
    public static final class Reader implements IsoParser.BlockReader, AutoCloseable {

        private final RandomAccessFile raf;
        private final Header header;
        private final int[] index;
        private final Inflater inflater = new Inflater();
        private final int perBlock;

        private int cachedBlock = -1;
        private byte[] cached;

        Reader(File file) throws IOException {
            this.raf = new RandomAccessFile(file, "r");
            this.header = readHeader(raf);
            this.index = readIndex(raf, header);
            this.perBlock = Math.max(1, header.blockSize / IsoParser.SECTOR);
        }

        public Header header() {
            return header;
        }

        @Override
        public void read(long offset, byte[] dst, int len) throws IOException {
            int written = 0;
            while (written < len) {
                long position = offset + written;
                int block = (int) (position / header.blockSize);
                int inside = (int) (position % header.blockSize);
                byte[] data = blockBytes(block);
                if (data == null) {
                    throw new EOFException("bloco " + block + " fora do CSO");
                }
                int n = Math.min(len - written, Math.min(data.length - inside, dst.length - written));
                if (n <= 0) {
                    throw new EOFException("bloco " + block + " vazio");
                }
                System.arraycopy(data, inside, dst, written, n);
                written += n;
            }
        }

        private byte[] blockBytes(int block) throws IOException {
            if (block == cachedBlock) {
                return cached;
            }
            if (block < 0 || block >= index.length) {
                return null;
            }
            cached = readBlock(raf, header, index, block, inflater);
            cachedBlock = block;
            return cached;
        }

        @Override
        public void close() {
            inflater.end();
            try {
                raf.close();
            } catch (IOException ignored) {
                // read only
            }
        }
    }

    /** Opens the image for random reads; the caller closes it. */
    public static Reader blockReader(File file) throws IOException {
        return new Reader(file);
    }

    private static int[] readIndex(RandomAccessFile raf, Header h) throws IOException {
        int[] index = new int[h.blocks];
        byte[] buf = new byte[Math.min(h.blocks, 4096) * 4];
        raf.seek(MAGIC_SIZE);
        int done = 0;
        while (done < h.blocks) {
            int chunk = Math.min(index.length - done, buf.length / 4);
            int bytes = chunk * 4;
            int read = 0;
            while (read < bytes) {
                int n = raf.read(buf, read, bytes - read);
                if (n < 0) {
                    throw new EOFException("índice CISO truncado");
                }
                read += n;
            }
            for (int i = 0; i < chunk; i++) {
                index[done + i] = (int) u32(buf, i * 4);
            }
            done += chunk;
        }
        return index;
    }

    private static byte[] readBlock(RandomAccessFile raf, Header h, int[] index, int block,
                                    Inflater inflater) throws IOException {
        int entry = index[block];
        boolean plain = (entry & 0x80000000) != 0;
        long offset = ((long) (entry & 0x7FFFFFFF)) << h.align;
        long next;
        if (block + 1 < index.length) {
            next = ((long) (index[block + 1] & 0x7FFFFFFF)) << h.align;
        } else {
            next = raf.length();
        }
        int length = (int) Math.max(0, next - offset);
        if (length <= 0) {
            return null;
        }
        byte[] raw = new byte[length];
        raf.seek(offset);
        raf.readFully(raw);
        if (plain) {
            return raw;
        }
        inflater.reset();
        inflater.setInput(raw);
        byte[] out = new byte[h.blockSize];
        try {
            int written = 0;
            while (written < out.length && !inflater.finished()) {
                int n = inflater.inflate(out, written, out.length - written);
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                written += n;
            }
            if (written == 0) {
                return null;
            }
            if (written == out.length) {
                return out;
            }
            byte[] trimmed = new byte[written];
            System.arraycopy(out, 0, trimmed, 0, written);
            return trimmed;
        } catch (DataFormatException e) {
            throw new IOException("bloco CSO corrompido em " + block, e);
        }
    }

    private static long u32(byte[] b, int off) {
        return (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8)
                | ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24);
    }

    private static long u64(byte[] b, int off) {
        return u32(b, off) | (u32(b, off + 4) << 32);
    }
}

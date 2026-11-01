package com.downhill.mobile.rom;

import java.io.IOException;

/** Adapter that lets {@link IsoParser} read from a {@code RandomAccessFile}. */
public final class FileBlockReader implements IsoParser.BlockReader, AutoCloseable {

    private final java.io.RandomAccessFile file;

    public FileBlockReader(java.io.File f) throws IOException {
        this.file = new java.io.RandomAccessFile(f, "r");
    }

    @Override
    public void read(long offset, byte[] dst, int len) throws IOException {
        file.seek(offset);
        int read = 0;
        while (read < len) {
            int n = file.read(dst, read, len - read);
            if (n < 0) {
                throw new IOException("unexpected end of image at " + (offset + read));
            }
            read += n;
        }
    }

    @Override
    public void close() {
        try {
            file.close();
        } catch (IOException ignored) {
            // read-only handle
        }
    }
}

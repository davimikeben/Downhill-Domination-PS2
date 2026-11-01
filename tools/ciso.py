#!/usr/bin/env python3
"""CISO (CSO) encoder/decoder for PS2 images, zlib, block 2048.

Format (v1): header 'CISO', header_size u32, total_bytes u64, block_size u32,
ver u8, align u8, reserved u16; then (total/block) u32 index entries where the
MSB marks a plain (uncompressed) block and the rest is the offset / align.
"""
import struct
import sys
import zlib

BLOCK = 2048

HEADER = struct.Struct("<4sIQIBBH")


def encode(src, dst, level=9, progress=None):
    """Writes CISO v1.

    The index entry keeps the offset in 31 bits, so for images over 2 GB the offset
    has to be divided: align=2 stores (offset >> 2) and pads every block to 4 bytes,
    which is exactly what PCSX2/AetherSX2 expect for large CSOs.
    """
    size = src.seek(0, 2)
    blocks = (size + BLOCK - 1) // BLOCK
    align = 2
    step = 1 << align
    index = [0] * blocks
    with open(src.name, "rb") as fin, open(dst, "wb") as out:
        out.write(HEADER.pack(b"CISO", HEADER.size, size, BLOCK, 1, align, 0))
        out.write(b"\x00" * (blocks * 4))
        fin.seek(0)
        for i in range(blocks):
            raw = fin.read(BLOCK)
            packed = zlib.compress(raw, level)
            payload = packed if len(packed) < len(raw) else raw
            plain = len(packed) >= len(raw)
            pad = (-len(payload)) % step
            offset = out.tell()
            assert offset % step == 0, offset
            assert (offset >> align) < 0x80000000, offset
            index[i] = (offset >> align) | (0x80000000 if plain else 0)
            out.write(payload)
            if pad:
                out.write(b"\x00" * pad)
            if progress and i % 20000 == 0:
                progress(i, blocks)
        end = out.tell()
        out.seek(HEADER.size)
        out.write(struct.pack("<%dI" % blocks, *index))
    return size, end


def read_blocks(name, first, count):
    """Decompress blocks [first, first+count) - used to sniff the ISO header."""
    with open(name, "rb") as f:
        head = f.read(HEADER.size)
        magic, hsize, total, block, ver, align, _ = HEADER.unpack(head)
        assert magic == b"CISO", "not a CISO file"
        f.seek(hsize)
        index = struct.unpack("<%dI" % ((total + block - 1) // block),
                              f.read(4 * ((total + block - 1) // block)))
        out = bytearray()
        for i in range(first, min(first + count, len(index))):
            entry = index[i]
            plain = entry & 0x80000000
            off = (entry & 0x7FFFFFFF) << align
            f.seek(off)
            if plain:
                out += f.read(block)
            else:
                nxt = index[i + 1] & 0x7FFFFFFF if i + 1 < len(index) else None
                nxt = (nxt << align) if nxt is not None else None
                chunk = f.read((nxt - off) if nxt else block)
                out += zlib.decompress(chunk)
        return bytes(out), total


if __name__ == "__main__":
    if sys.argv[1] == "encode":
        with open(sys.argv[2], "rb") as src:
            a, b = encode(src, sys.argv[3],
                          progress=lambda i, n: print(f"  {i*BLOCK/2**30:.2f} / {n*BLOCK/2**30:.2f} GB",
                                                      flush=True))
        print(f"in={a} out={b} ratio={a/b:.2f}x")
    elif sys.argv[1] == "test":
        data, total = read_blocks(sys.argv[2], 0, 24)
        print("first sector magic:", data[16*2048+1:16*2048+6], "total:", total)

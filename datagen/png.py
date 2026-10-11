"""Minimal stdlib PNG codec: reads 8-bit-or-less gray/RGB/palette/RGBA PNGs, writes 8-bit RGBA."""
import struct
import zlib

SIG = b"\x89PNG\r\n\x1a\n"


class Image:
    def __init__(self, width, height, pixels):
        self.width = width
        self.height = height
        self.pixels = pixels  # list of (r, g, b, a) tuples, row-major

    def copy(self):
        return Image(self.width, self.height, list(self.pixels))


def _chunks(data):
    pos = len(SIG)
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        kind = data[pos + 4:pos + 8]
        yield kind, data[pos + 8:pos + 8 + length]
        pos += 12 + length


def _paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def read(path_or_bytes):
    data = path_or_bytes if isinstance(path_or_bytes, (bytes, bytearray)) else open(path_or_bytes, "rb").read()
    path = "<bytes>" if isinstance(path_or_bytes, (bytes, bytearray)) else path_or_bytes
    if data[:8] != SIG:
        raise ValueError(f"not a PNG: {path}")
    idat = b""
    palette, trns = None, None
    for kind, body in _chunks(data):
        if kind == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if interlace:
                raise ValueError(f"interlaced PNG not supported: {path}")
        elif kind == b"PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == b"tRNS":
            trns = body
        elif kind == b"IDAT":
            idat += body
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    bits_pp = channels * depth
    stride = (w * bits_pp + 7) // 8
    bpp = max(1, bits_pp // 8)
    raw = zlib.decompress(idat)
    rows, prev, pos = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[pos]
        line = bytearray(raw[pos + 1:pos + 1 + stride])
        pos += 1 + stride
        for i in range(stride):
            left = line[i - bpp] if i >= bpp else 0
            up = prev[i]
            ul = prev[i - bpp] if i >= bpp else 0
            if f == 1:
                line[i] = (line[i] + left) & 255
            elif f == 2:
                line[i] = (line[i] + up) & 255
            elif f == 3:
                line[i] = (line[i] + ((left + up) >> 1)) & 255
            elif f == 4:
                line[i] = (line[i] + _paeth(left, up, ul)) & 255
        rows.append(line)
        prev = line
    pixels = []
    for line in rows:
        if depth < 8:
            vals = []
            for byte in line:
                for shift in range(8 - depth, -1, -depth):
                    vals.append((byte >> shift) & ((1 << depth) - 1))
            vals = vals[:w]
        else:
            vals = list(line)
        for x in range(w):
            if ctype == 3:
                idx = vals[x]
                r, g, b = palette[idx]
                a = trns[idx] if trns is not None and idx < len(trns) else 255
            elif ctype == 6:
                r, g, b, a = vals[x * 4:x * 4 + 4]
            elif ctype == 2:
                r, g, b = vals[x * 3:x * 3 + 3]
                a = 255
            elif ctype == 4:
                r = g = b = vals[x * 2]
                a = vals[x * 2 + 1]
            else:  # gray
                v = vals[x]
                if depth < 8:
                    v = v * 255 // ((1 << depth) - 1)
                r = g = b = v
                a = 255
            pixels.append((r, g, b, a))
    return Image(w, h, pixels)


def _chunk(kind, body):
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)


def write(path, img):
    raw = bytearray()
    for y in range(img.height):
        raw.append(0)
        for r, g, b, a in img.pixels[y * img.width:(y + 1) * img.width]:
            raw += bytes((r, g, b, a))
    ihdr = struct.pack(">IIBBBBB", img.width, img.height, 8, 6, 0, 0, 0)
    with open(path, "wb") as f:
        f.write(SIG + _chunk(b"IHDR", ihdr) + _chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + _chunk(b"IEND", b""))

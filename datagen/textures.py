#!/usr/bin/env python3
"""
Sprocket Dyes texture generator.

Every texture for a new color is recolored from the same texture of a vanilla color, in OKLCH:
hue is rotated and lightness shifted by the difference between the two dye colors, chroma is scaled by
their ratio. Per-pixel shading, highlights and noise from Mojang's hand-made texture survive.

  * most textures use the color's `source` (nearest vanilla color) from palette/colors.json
  * glazed terracotta uses its assigned `glazed` pattern color instead

Reads vanilla textures straight from the <SOURCE_VERSION> client jar in Loom's cache.
Writes into src/main/resources/assets/sprocket_dyes/textures (shared by every version) and a contact sheet
into build/textures-contact.png.

Run:  python3 datagen/textures.py
"""
import json
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import png  # noqa: E402
from vanilla import VanillaJar  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SOURCE_VERSION = "26.3"
VANILLA_TEX = "assets/minecraft/textures"
ASSETS = ROOT / "src/main/resources/assets"

PALETTE = json.loads((ROOT / "palette/colors.json").read_text())
ALL = {c["id"]: c for c in PALETTE["key"] + PALETTE["colors"]}
NEW = [c for c in PALETTE["colors"] if not c["vanilla"]]

# (vanilla path with {c}, output path with {c}, palette field picking the source color, output namespace, source version)
# Textures vanilla code looks up by color name (entity beds/shulkers/cushions, llama carpets) go in minecraft's
# namespace; everything our own models reference goes in ours.
NS = "sprocket_dyes"
MC = "minecraft"
TEXTURES = [
    ("item/{c}_dye.png", "item/{c}_dye.png", "dye_source", NS, SOURCE_VERSION),
    ("block/{c}_wool.png", "block/{c}_wool.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_terracotta.png", "block/{c}_terracotta.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_glazed_terracotta.png", "block/{c}_glazed_terracotta.png", "glazed", NS, SOURCE_VERSION),
    ("block/{c}_concrete.png", "block/{c}_concrete.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_concrete_powder.png", "block/{c}_concrete_powder.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_stained_glass.png", "block/{c}_stained_glass.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_stained_glass_pane_top.png", "block/{c}_stained_glass_pane_top.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_candle.png", "block/{c}_candle.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_candle_lit.png", "block/{c}_candle_lit.png", "source", NS, SOURCE_VERSION),
    ("item/{c}_candle.png", "item/{c}_candle.png", "source", NS, SOURCE_VERSION),
    ("block/{c}_shulker_box.png", "block/{c}_shulker_box.png", "source", NS, SOURCE_VERSION),
    ("entity/shulker/shulker_{c}.png", "entity/shulker/shulker_{c}.png", "source", MC, SOURCE_VERSION),
    # Beds: an entity texture up to 26.1, per-face block textures from 26.2.
    ("entity/bed/{c}.png", "entity/bed/{c}.png", "source", MC, "26.1.2"),
] + [
    (f"block/{{c}}_bed_{part}.png", f"block/{{c}}_bed_{part}.png", "source", NS, SOURCE_VERSION)
    for part in ("head_east", "head_up", "head_west", "foot_east", "foot_south", "foot_up", "foot_west")
] + [
    # Llama carpets: a decor texture in 1.21.1, an equipment layer from 1.21.2.
    ("entity/llama/decor/{c}.png", "entity/llama/decor/{c}.png", "source", MC, "1.21.1"),
    ("entity/equipment/llama_body/{c}.png", "entity/equipment/llama_body/{c}.png", "source", MC, SOURCE_VERSION),
    ("item/{c}_bundle.png", "item/{c}_bundle.png", "source", NS, SOURCE_VERSION),
    ("item/{c}_bundle_open_back.png", "item/{c}_bundle_open_back.png", "source", NS, SOURCE_VERSION),
    ("item/{c}_bundle_open_front.png", "item/{c}_bundle_open_front.png", "source", NS, SOURCE_VERSION),
    ("item/{c}_harness.png", "item/{c}_harness.png", "source", NS, SOURCE_VERSION),
    ("entity/equipment/happy_ghast_body/{c}_harness.png", "entity/equipment/happy_ghast_body/{c}_harness.png", "source", NS, SOURCE_VERSION),
    ("item/{c}_cushion.png", "item/{c}_cushion.png", "source", NS, SOURCE_VERSION),
    ("entity/cushion/{c}_cushion.png", "entity/cushion/{c}_cushion.png", "source", MC, SOURCE_VERSION),
    ("map/decorations/{c}_banner.png", "map/decorations/{c}_banner.png", "source", NS, SOURCE_VERSION),
]
# Contact sheet shows the 16x16 item/block kinds only.
CONTACT_KINDS = 12


# ---------------------------------------------------------------------------------------------
# OKLab
# ---------------------------------------------------------------------------------------------

def _lin(c):
    c /= 255
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gam(c):
    c = max(0.0, min(1.0, c))
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055


def rgb_to_lch(r, g, b):
    r, g, b = _lin(r), _lin(g), _lin(b)
    l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
    m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
    s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
    L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    A = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    B = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return L, math.hypot(A, B), math.degrees(math.atan2(B, A)) % 360


def _lab_to_linear(L, A, B):
    l = (L + 0.3963377774 * A + 0.2158037573 * B) ** 3
    m = (L - 0.1055613458 * A - 0.0638541728 * B) ** 3
    s = (L - 0.0894841775 * A - 1.2914855480 * B) ** 3
    return (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)


def lch_to_rgb(L, C, h):
    """OKLCH -> sRGB bytes, reducing chroma (not lightness) until in gamut."""
    L = max(0.0, min(1.0, L))
    hr = math.radians(h)
    for _ in range(24):
        lin = _lab_to_linear(L, C * math.cos(hr), C * math.sin(hr))
        if all(-1e-4 <= v <= 1 + 1e-4 for v in lin):
            break
        C *= 0.9
    return tuple(round(_gam(v) * 255) for v in lin)


def hex_lch(h):
    return rgb_to_lch(int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


# ---------------------------------------------------------------------------------------------
# Recolor
# ---------------------------------------------------------------------------------------------

def color_mask(variants):
    """
    Which pixels change with color, judged from vanilla's 16 variants of one texture. Pixels identical in every
    variant (a bed's wooden frame and pillow, the shulker's head, metal buckles) are left untouched.
    """
    base = variants[0]
    n = len(base.pixels)
    if any(v.width != base.width or v.height != base.height for v in variants):
        return [True] * n
    return [any(v.pixels[i] != base.pixels[i] for v in variants[1:]) for i in range(n)]


def recolor(img, src_hex, dst_hex, mask=None):
    sL, sC, sh = hex_lch(src_hex)
    dL, dC, dh = hex_lch(dst_hex)
    neutral_src = sC < 0.03
    dhue = dh - sh
    cratio = dC / sC if not neutral_src else 1.0
    out = img.copy()
    cache = {}
    for i, (r, g, b, a) in enumerate(img.pixels):
        if a == 0 or (mask is not None and not mask[i]):
            continue
        key = (r, g, b)
        if key not in cache:
            L, C, h = rgb_to_lch(r, g, b)
            L2 = L + (dL - sL)
            if neutral_src:
                C2, h2 = C + dC, dh  # a gray source carries no hue: tint it
            else:
                C2, h2 = C * cratio, h + dhue
            cache[key] = lch_to_rgb(L2, C2, h2)
        out.pixels[i] = (*cache[key], a)
    return out


def generate():
    jars = {}
    written = 0
    blocks = []
    for vpath, opath, field, ns, version in TEXTURES:
        jar = jars.setdefault(version, VanillaJar(version))
        cells = {}
        vanilla_ids = [c["id"] for c in PALETTE["key"] + PALETTE["colors"] if c["vanilla"]]
        mask = color_mask([png.read(jar.read_bytes(f"{VANILLA_TEX}/{vpath.format(c=v)}")) for v in vanilla_ids])
        for c in PALETTE["colors"]:
            if c["vanilla"]:
                cells[c["id"]] = png.read(jar.read_bytes(f"{VANILLA_TEX}/{vpath.format(c=c['id'])}"))
                continue
            src_id = c[field]
            src = f"{VANILLA_TEX}/{vpath.format(c=src_id)}"
            out = recolor(png.read(jar.read_bytes(src)), ALL[src_id]["hex"], c["hex"], mask)
            dest = ASSETS / ns / "textures" / opath.format(c=c["id"])
            dest.parent.mkdir(parents=True, exist_ok=True)
            png.write(dest, out)
            if jar.exists(src + ".mcmeta"):
                Path(str(dest) + ".mcmeta").write_text(jar.read_text(src + ".mcmeta"))
            cells[c["id"]] = out
            written += 1
        if len(blocks) < CONTACT_KINDS:
            blocks.append([cells[c["id"]] for c in PALETTE["colors"]])
    contact_sheet(blocks)
    print(f"wrote {written} textures")


def contact_sheet(blocks, scale=3, gap=2, block_gap=12):
    """Each texture kind is a 16-hue x 8 block (palette grid layout, vanilla anchors included); first frame only."""
    cell = 16 * scale
    bw = 8 * (cell + gap) + gap
    bh = 16 * (cell + gap) + gap
    W, H = len(blocks) * (bw + block_gap), bh
    px = [(40, 40, 46, 255)] * (W * H)
    for bi, imgs in enumerate(blocks):
        for i, img in enumerate(imgs):
            row, col = divmod(i, 8)
            ox, oy = bi * (bw + block_gap) + gap + col * (cell + gap), gap + row * (cell + gap)
            for y in range(cell):
                for x in range(cell):
                    sx, sy = x * img.width // cell, y * img.width // cell
                    p = img.pixels[sy * img.width + sx]
                    if p[3]:
                        px[(oy + y) * W + ox + x] = p
    (ROOT / "build").mkdir(exist_ok=True)
    png.write(ROOT / "build/textures-contact.png", png.Image(W, H, px))


if __name__ == "__main__":
    generate()

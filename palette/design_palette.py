#!/usr/bin/env python3
"""
Designs the Sprocket Dyes palette and writes `palette/colors.json`, the single source of truth
for every downstream generator (Java palette table, lang, textures, creative sections).

Stdlib only. Run from anywhere:  python3 palette/design_palette.py

The grid is 16 hues x 2 saturations x 4 values = 128 points, laid out in OKLCH.
The 12 colored vanilla dyes are pinned to grid points and keep their exact vanilla hex;
every other point is computed from per-hue lightness/chroma curves fitted to those anchors.
Hand edits belong in the CONFIG section below (names, anchors, hue angles), not in the JSON.
"""
import json
import math
import random
from pathlib import Path

# ---------------------------------------------------------------------------------------------
# CONFIG
# ---------------------------------------------------------------------------------------------

VALUES = ["bright", "medium", "deep", "dark"]
SATS = ["standard", "muted"]

# Base OKLab lightness per value level, before the per-hue offset.
BASE_L = {"bright": 0.765, "medium": 0.640, "deep": 0.515, "dark": 0.390}
# Minimum lightness gap between consecutive value levels within one hue.
MIN_L_GAP = 0.075
# Standard points use this fraction of the in-gamut chroma maximum (per value), capped at MAX_CHROMA.
# Darker values get closer to the gamut edge, since the gamut is narrow down there.
STANDARD_REL_CHROMA = {"bright": 0.86, "medium": 0.88, "deep": 0.93, "dark": 0.97}
MAX_CHROMA = 0.205
# Muted = "only slightly washed out": less chroma and a touch lighter. The gamut squeezes dark
# colors together, so the darker values are washed a little harder to stay distinguishable.
MUTED_CHROMA = {"bright": 0.70, "medium": 0.66, "deep": 0.60, "dark": 0.52}
MUTED_L_SHIFT = {"bright": 0.010, "medium": 0.015, "deep": 0.022, "dark": 0.030}

# Hue wheel. `angle: None` means interpolate between the anchored neighbours.
HUES = [
    ("red",        "Red",        None),
    ("orange",     "Orange",     None),
    ("amber",      "Amber",      None),
    ("yellow",     "Yellow",     None),
    ("chartreuse", "Chartreuse", None),
    ("lime",       "Lime",       None),
    ("green",      "Green",      148.0),
    ("teal",       "Teal",       170.0),
    ("cyan",       "Cyan",       None),
    ("sky",        "Sky",        None),
    ("azure",      "Azure",      250.0),
    ("blue",       "Blue",       None),
    ("indigo",     "Indigo",     291.0),
    ("purple",     "Purple",     None),
    ("magenta",    "Magenta",    None),
    ("rose",       "Rose",       None),
]

# vanilla dye -> (hue, value). All anchors are Standard saturation.
ANCHORS = {
    "red":        ("red",     "deep"),
    "orange":     ("orange",  "bright"),
    "brown":      ("orange",  "deep"),
    "yellow":     ("yellow",  "bright"),
    "lime":       ("lime",    "bright"),
    "green":      ("lime",    "deep"),
    "cyan":       ("cyan",    "medium"),
    "light_blue": ("sky",     "bright"),
    "blue":       ("blue",    "deep"),
    "purple":     ("purple",  "deep"),
    "magenta":    ("magenta", "medium"),
    "pink":       ("rose",    "bright"),
}

# Display names: hue -> [std bright, std medium, std deep, std dark, muted bright, muted medium, muted deep, muted dark]
# `None` = vanilla anchor (keeps its vanilla name).
NAMES = {
    "red":        ["Coral", "Scarlet", None, "Oxblood", "Salmon", "Brick", "Garnet", "Maroon"],
    "orange":     [None, "Rust", None, "Umber", "Peach", "Cinnamon", "Cocoa", "Walnut"],
    "amber":      ["Marigold", "Amber", "Bronze", "Coffee", "Butterscotch", "Caramel", "Tawny", "Sepia"],
    "yellow":     [None, "Gold", "Ochre", "Olive", "Straw", "Mustard", "Brass", "Bistre"],
    "chartreuse": ["Chartreuse", "Pear", "Avocado", "Moss", "Pistachio", "Lichen", "Artichoke", "Swamp"],
    "lime":       [None, "Leaf", None, "Ivy", "Celadon", "Fern", "Laurel", "Thyme"],
    "green":      ["Spring", "Emerald", "Forest", "Pine", "Mint", "Jade", "Spruce", "Cypress"],
    "teal":       ["Aquamarine", "Viridian", "Teal", "Kelp", "Seafoam", "Verdigris", "Eucalyptus", "Bayou"],
    "cyan":       ["Turquoise", None, "Peacock", "Petrol", "Robin", "Tidepool", "Harbor", "Fathom"],
    "sky":        [None, "Cerulean", "Ocean", "Abyss", "Powder", "Denim", "Steel", "Slate"],
    "azure":      ["Cornflower", "Azure", "Cobalt", "Navy", "Hydrangea", "Chambray", "Dusk", "Midnight"],
    "blue":       ["Periwinkle", "Ultramarine", None, "Ink", "Bluebell", "Delft", "Storm", "Nightfall"],
    "indigo":     ["Iris", "Violet", "Indigo", "Nocturne", "Lavender", "Wisteria", "Gloaming", "Raven"],
    "purple":     ["Orchid", "Amethyst", None, "Eggplant", "Lilac", "Heather", "Plum", "Elderberry"],
    "magenta":    ["Fuchsia", None, "Byzantium", "Mulberry", "Mauve", "Peony", "Bramble", "Wine"],
    "rose":       [None, "Rose", "Crimson", "Burgundy", "Blush", "Rouge", "Rhubarb", "Claret"],
}

GLAZED_SEED = 132

# ---------------------------------------------------------------------------------------------
# Vanilla reference data (DyeColor constructor arguments, 1.21 - 26.3)
# ---------------------------------------------------------------------------------------------

# id, textureDiffuseColor, mapColor, fireworkColor, textColor
VANILLA = [
    ("white",      "F9FFFE", "SNOW",              "F0F0F0", "FFFFFF"),
    ("orange",     "F9801D", "COLOR_ORANGE",      "EB8844", "FF681F"),
    ("magenta",    "C74EBD", "COLOR_MAGENTA",     "C354CD", "FF00FF"),
    ("light_blue", "3AB3DA", "COLOR_LIGHT_BLUE",  "6689D3", "9AC0CD"),
    ("yellow",     "FED83D", "COLOR_YELLOW",      "DECF2A", "FFFF00"),
    ("lime",       "80C71F", "COLOR_LIGHT_GREEN", "41CD34", "BFFF00"),
    ("pink",       "F38BAA", "COLOR_PINK",        "D88198", "FF69B4"),
    ("gray",       "474F52", "COLOR_GRAY",        "434343", "808080"),
    ("light_gray", "9D9D97", "COLOR_LIGHT_GRAY",  "ABABAB", "D3D3D3"),
    ("cyan",       "169C9C", "COLOR_CYAN",        "287697", "00FFFF"),
    ("purple",     "8932B8", "COLOR_PURPLE",      "7B2FBE", "A020F0"),
    ("blue",       "3C44AA", "COLOR_BLUE",        "253192", "0000FF"),
    ("brown",      "835432", "COLOR_BROWN",       "51301A", "8B4513"),
    ("green",      "5E7C16", "COLOR_GREEN",       "3B511A", "00FF00"),
    ("red",        "B02E26", "COLOR_RED",         "B3312C", "FF0000"),
    ("black",      "1D1D21", "COLOR_BLACK",       "1E1B1B", "000000"),
]
VANILLA_BY_ID = {v[0]: v for v in VANILLA}
KEY = ["white", "light_gray", "gray", "black"]
VANILLA_NAMES = {
    "white": "White", "orange": "Orange", "magenta": "Magenta", "light_blue": "Light Blue",
    "yellow": "Yellow", "lime": "Lime", "pink": "Pink", "gray": "Gray", "light_gray": "Light Gray",
    "cyan": "Cyan", "purple": "Purple", "blue": "Blue", "brown": "Brown", "green": "Green",
    "red": "Red", "black": "Black",
}

# Vanilla terracotta map color per dye (TERRACOTTA_<DYE>).
def terracotta_map(dye):
    return "TERRACOTTA_" + dye.upper()

# Every vanilla MapColor (name, rgb) a dye color may resolve to.
MAP_COLORS = [
    ("GRASS", "7FB238"), ("SAND", "F7E9A3"), ("WOOL", "C7C7C7"), ("FIRE", "FF0000"), ("ICE", "A0A0FF"),
    ("METAL", "A7A7A7"), ("PLANT", "007C00"), ("SNOW", "FFFFFF"), ("CLAY", "A4A8B8"), ("DIRT", "976D4D"),
    ("STONE", "707070"), ("WATER", "4040FF"), ("WOOD", "8F7748"), ("QUARTZ", "FFFCF5"),
    ("COLOR_ORANGE", "D87F33"), ("COLOR_MAGENTA", "B24CD8"), ("COLOR_LIGHT_BLUE", "6699D8"),
    ("COLOR_YELLOW", "E5E533"), ("COLOR_LIGHT_GREEN", "7FCC19"), ("COLOR_PINK", "F27FA5"),
    ("COLOR_GRAY", "4C4C4C"), ("COLOR_LIGHT_GRAY", "999999"), ("COLOR_CYAN", "4C7F99"),
    ("COLOR_PURPLE", "7F3FB2"), ("COLOR_BLUE", "334CB2"), ("COLOR_BROWN", "664C33"),
    ("COLOR_GREEN", "667F33"), ("COLOR_RED", "993333"), ("COLOR_BLACK", "191919"), ("GOLD", "FAEE4D"),
    ("DIAMOND", "5CDBD5"), ("LAPIS", "4A80FF"), ("EMERALD", "00D93A"), ("PODZOL", "815631"),
    ("NETHER", "700200"), ("CRIMSON_NYLIUM", "BD3031"), ("CRIMSON_STEM", "943F61"),
    ("CRIMSON_HYPHAE", "5C191D"), ("WARPED_NYLIUM", "167E86"), ("WARPED_STEM", "3A8E8C"),
    ("WARPED_HYPHAE", "562C3E"), ("WARPED_WART_BLOCK", "14B485"), ("GLOW_LICHEN", "7FA796"),
]

# ---------------------------------------------------------------------------------------------
# OKLab / OKLCH
# ---------------------------------------------------------------------------------------------

def _lin(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4

def _gam(c):
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055

def hex_to_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))

def rgb_to_hex(rgb):
    return "".join(f"{round(max(0.0, min(1.0, c)) * 255):02X}" for c in rgb)

def rgb_to_oklab(rgb):
    r, g, b = (_lin(c) for c in rgb)
    l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    l, m, s = (math.copysign(abs(x) ** (1 / 3), x) for x in (l, m, s))
    return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)

def oklab_to_linear(lab):
    L, a, b = lab
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)

def lch_to_lab(L, C, h):
    return (L, C * math.cos(math.radians(h)), C * math.sin(math.radians(h)))

def lab_to_lch(lab):
    L, a, b = lab
    return L, math.hypot(a, b), math.degrees(math.atan2(b, a)) % 360

def in_gamut(lab, eps=1e-4):
    return all(-eps <= c <= 1 + eps for c in oklab_to_linear(lab))

def lab_to_rgb(lab):
    return tuple(_gam(max(0.0, min(1.0, c))) for c in oklab_to_linear(lab))

def max_chroma(L, h):
    lo, hi = 0.0, 0.4
    for _ in range(40):
        mid = (lo + hi) / 2
        if in_gamut(lch_to_lab(L, mid, h)):
            lo = mid
        else:
            hi = mid
    return lo

def hex_lch(h):
    return lab_to_lch(rgb_to_oklab(hex_to_rgb(h)))

def dist(h1, h2):
    a, b = rgb_to_oklab(hex_to_rgb(h1)), rgb_to_oklab(hex_to_rgb(h2))
    return math.dist(a, b)

def lerp_angle(a, b, t):
    d = ((b - a + 540) % 360) - 180
    return (a + d * t) % 360

# ---------------------------------------------------------------------------------------------
# Build the grid
# ---------------------------------------------------------------------------------------------

def circular_fill(known, n):
    """Linear interpolation around a ring of n slots; `known` maps index -> value."""
    out = {}
    idx = sorted(known)
    for i in range(n):
        if i in known:
            out[i] = known[i]
            continue
        prev = max([k for k in idx if k < i], default=idx[-1])
        nxt = min([k for k in idx if k > i], default=idx[0])
        span = (nxt - prev) % n
        t = ((i - prev) % n) / span
        out[i] = (prev, nxt, t)
    return out

def build():
    hue_ids = [h[0] for h in HUES]
    anchor_at = {}  # (hue, value) -> vanilla id
    for dye, (hue, val) in ANCHORS.items():
        anchor_at[(hue, val)] = dye

    # Hue angles: explicit > anchor average > interpolated.
    angles = {}
    for i, (hid, _, ang) in enumerate(HUES):
        if ang is not None:
            angles[i] = ang
        else:
            hs = [hex_lch(VANILLA_BY_ID[d][1])[2] for (h, _), d in anchor_at.items() if h == hid]
            if hs:
                # average on the circle
                x = sum(math.cos(math.radians(a)) for a in hs); y = sum(math.sin(math.radians(a)) for a in hs)
                angles[i] = math.degrees(math.atan2(y, x)) % 360
    fill = circular_fill(angles, 16)
    for i, v in fill.items():
        if isinstance(v, tuple):
            a, b, t = v
            angles[i] = lerp_angle(angles[a], angles[b], t)

    # Per-hue lightness offset from anchors, interpolated around the wheel.
    offsets = {}
    for i, hid in enumerate(hue_ids):
        offs = [hex_lch(VANILLA_BY_ID[d][1])[0] - BASE_L[val] for (h, val), d in anchor_at.items() if h == hid]
        if offs:
            offsets[i] = sum(offs) / len(offs)
    fill = circular_fill(offsets, 16)
    for i, v in fill.items():
        if isinstance(v, tuple):
            a, b, t = v
            offsets[i] = offsets[a] + (offsets[b] - offsets[a]) * t

    colors = []
    for i, (hid, hname, _) in enumerate(HUES):
        h = angles[i]
        # Standard lightness per value: anchors pinned, the rest from base + offset, then de-crowded.
        Ls = {}
        for val in VALUES:
            if (hid, val) in anchor_at:
                Ls[val] = hex_lch(VANILLA_BY_ID[anchor_at[(hid, val)]][1])[0]
            else:
                Ls[val] = BASE_L[val] + offsets[i]
        for k in range(1, 4):  # enforce a minimum gap going darker (anchors win ties)
            a, b = VALUES[k - 1], VALUES[k]
            if Ls[a] - Ls[b] < MIN_L_GAP:
                if (hid, b) in anchor_at and (hid, a) not in anchor_at:
                    Ls[a] = Ls[b] + MIN_L_GAP
                else:
                    Ls[b] = Ls[a] - MIN_L_GAP
        Ls = {k: max(0.24, min(0.93, v)) for k, v in Ls.items()}

        for s_i, sat in enumerate(SATS):
            for v_i, val in enumerate(VALUES):
                anchor = anchor_at.get((hid, val)) if sat == "standard" else None
                if anchor:
                    hexv = VANILLA_BY_ID[anchor][1]
                else:
                    L = Ls[val] + (MUTED_L_SHIFT[val] if sat == "muted" else 0)
                    if (hid, val) in anchor_at:
                        # muted twin of an anchor: anchor's own hue/chroma, washed
                        aL, aC, ah = hex_lch(VANILLA_BY_ID[anchor_at[(hid, val)]][1])
                        C, hh = aC * MUTED_CHROMA[val], ah
                    else:
                        C = min(max_chroma(L, h) * STANDARD_REL_CHROMA[val], MAX_CHROMA)
                        if sat == "muted":
                            # chroma of the standard twin at its own lightness, then washed
                            C = min(max_chroma(Ls[val], h) * STANDARD_REL_CHROMA[val], MAX_CHROMA) * MUTED_CHROMA[val]
                        hh = h
                    C = min(C, max_chroma(L, hh) * 0.995)
                    hexv = rgb_to_hex(lab_to_rgb(lch_to_lab(L, C, hh)))
                name = NAMES[hid][s_i * 4 + v_i]
                cid = anchor if anchor else (("muted_" if sat == "muted" else "") + f"{val}_{hid}")
                colors.append({
                    "id": cid,
                    "name": VANILLA_NAMES[anchor] if anchor else name,
                    "hue": hid,
                    "sat": sat,
                    "value": val,
                    "hex": hexv,
                    "vanilla": anchor is not None,
                })
    return colors, {hue_ids[i]: round(angles[i], 1) for i in range(16)}

def nearest_vanilla(hexv, pool):
    return min(pool, key=lambda d: dist(hexv, VANILLA_BY_ID[d][1]))

def transfer(hexv, src_from, src_to):
    """Applies the OKLab delta (src_from -> src_to) to hexv, clamped into gamut."""
    a = rgb_to_oklab(hex_to_rgb(hexv)); f = rgb_to_oklab(hex_to_rgb(src_from)); t = rgb_to_oklab(hex_to_rgb(src_to))
    lab = tuple(a[i] + t[i] - f[i] for i in range(3))
    L, C, h = lab_to_lch(lab)
    L = max(0.0, min(1.0, L))
    C = min(C, max_chroma(L, h))
    return rgb_to_hex(lab_to_rgb(lch_to_lab(L, C, h)))

def main():
    root = Path(__file__).resolve().parent
    grid, angles = build()

    names = [c["name"] for c in grid] + [VANILLA_NAMES[k] for k in KEY]
    dupes = {n for n in names if names.count(n) > 1}
    assert not dupes, f"duplicate names: {dupes}"
    assert sum(1 for c in grid if not c["vanilla"]) == 116

    # Texture source = nearest vanilla color (any of the 16) - textures are recolored from it.
    colored_vanilla = [v[0] for v in VANILLA if v[0] not in KEY]
    for c in grid:
        if c["vanilla"]:
            c["source"] = c["id"]
        else:
            # Prefer a chromatic source for chromatic targets; grays only for near-neutral targets.
            C = hex_lch(c["hex"])[1]
            c["source"] = nearest_vanilla(c["hex"], colored_vanilla if C > 0.035 else [v[0] for v in VANILLA])
        src = VANILLA_BY_ID[c["source"]]
        mc = min(MAP_COLORS, key=lambda m: dist(c["hex"], m[1]))
        c["map_color"] = src[2] if c["vanilla"] else mc[0]
        c["terracotta_map_color"] = terracotta_map(c["source"])
        c["firework"] = src[3] if c["vanilla"] else transfer(c["hex"], src[1], src[3])
        c["text"] = src[4] if c["vanilla"] else transfer(c["hex"], src[1], src[4])

    # Glazed patterns: each new color borrows one of the 16 vanilla patterns. Within a hue section
    # all 8 patterns are distinct, and every pattern ends up used 8-9 times overall.
    rng = random.Random(GLAZED_SEED)
    patterns = [v[0] for v in VANILLA]
    usage = {p: 1 for p in patterns}  # each vanilla color uses its own pattern once
    for hid, _, _ in HUES:
        section = [c for c in grid if c["hue"] == hid]
        used = {c["id"] for c in section if c["vanilla"]}
        for c in section:
            if c["vanilla"]:
                c["glazed"] = c["id"]
        for c in sorted([c for c in section if not c["vanilla"]], key=lambda _: rng.random()):
            low = min(usage[p] for p in patterns if p not in used)
            choices = [p for p in patterns if p not in used and usage[p] == low]
            p = rng.choice(choices)
            c["glazed"] = p
            used.add(p)
            usage[p] += 1
    assert max(usage.values()) - min(usage.values()) <= 1, usage

    key = []
    for k in KEY:
        v = VANILLA_BY_ID[k]
        key.append({"id": k, "name": VANILLA_NAMES[k], "hue": "key", "sat": None, "value": None, "hex": v[1],
                    "vanilla": True, "source": k, "map_color": v[2], "terracotta_map_color": terracotta_map(k),
                    "firework": v[3], "text": v[4], "glazed": k})

    # Similarity warnings (OKLab distance) - anything under this is likely hard to tell apart.
    allc = key + grid
    warn = []
    for i in range(len(allc)):
        for j in range(i + 1, len(allc)):
            d = dist(allc[i]["hex"], allc[j]["hex"])
            if d < 0.045:
                warn.append({"a": allc[i]["id"], "b": allc[j]["id"], "distance": round(d, 4)})
    warn.sort(key=lambda w: w["distance"])

    out = {
        "_comment": "GENERATED by palette/design_palette.py - edit the script's CONFIG, not this file.",
        "values": VALUES,
        "sats": SATS,
        "hues": [{"id": h[0], "name": h[1], "angle": angles[h[0]]} for h in HUES],
        "key": key,
        "colors": grid,
        "glazed_usage": usage,
        "similar_pairs": warn,
    }
    (root / "colors.json").write_text(json.dumps(out, indent=2) + "\n")
    print(f"wrote {root / 'colors.json'}: {len(grid)} grid points ({sum(c['vanilla'] for c in grid)} vanilla), "
          f"{len(warn)} similar pairs")
    for w in warn[:15]:
        print("  similar:", w)

if __name__ == "__main__":
    main()

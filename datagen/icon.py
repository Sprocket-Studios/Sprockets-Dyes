#!/usr/bin/env python3
"""
Renders the mod icon (src/main/resources/assets/sprocket_dyes/icon.png): the 16 hues' dye sprites in a
4x4 grid at 2x scale (128x128), in the given value/saturation (default: standard medium).

  python3 datagen/icon.py [value] [sat]
"""
import json
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import png  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/sprocket_dyes/icon.png"
OURS = ROOT / "src/main/resources/assets/sprocket_dyes/textures/item"
JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
SCALE = 2


def sprite(color):
    if color["vanilla"]:
        with zipfile.ZipFile(JAR) as jar:
            return png.read(jar.read(f"assets/minecraft/textures/item/{color['id']}_dye.png"))
    return png.read((OURS / f"{color['id']}_dye.png").read_bytes())


def main(value="medium", sat="standard"):
    palette = json.loads((ROOT / "palette/colors.json").read_text())
    by_hue = {c["hue"]: c for c in palette["colors"] if c["value"] == value and c["sat"] == sat}
    cell = 16 * SCALE
    icon = png.Image(4 * cell, 4 * cell, [(0, 0, 0, 0)] * (16 * cell * cell))
    for i, hue in enumerate(h["id"] for h in palette["hues"]):
        img = sprite(by_hue[hue])
        ox, oy = (i % 4) * cell, (i // 4) * cell
        for y in range(16):
            for x in range(16):
                px = img.pixels[y * img.width + x]
                if px[3] == 0:
                    continue
                for dy in range(SCALE):
                    for dx in range(SCALE):
                        icon.pixels[(oy + y * SCALE + dy) * icon.width + ox + x * SCALE + dx] = px
    png.write(OUT, icon)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main(*sys.argv[1:])

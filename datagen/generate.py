#!/usr/bin/env python3
"""
Sprocket Dyes data generator.

Writes src/generated/<mc version>/resources for every target version by *retargeting vanilla's own files*
for that version: a vanilla magenta file (recipe, loot table, blockstate, model, item definition, equipment
asset, ...) is copied with "magenta" replaced by the target color and ids moved into our namespace where the
referenced thing is ours. Formats are therefore exactly right per version without per-version code.

Shapes vanilla has no colored version of (walls everywhere; wool/concrete slabs & stairs before 26.3;
terracotta/glazed/glass shapes) are templated from vanilla's stone brick family of the same version.

Also writes the shared lang file (src/main/resources/.../lang/en_us.json; block names merged in).

Reads vanilla files straight from the client jar Loom caches (~/.gradle/caches/fabric-loom/<version>).

  python3 datagen/generate.py data <mc version> <out dir>   # what each Gradle node runs before processResources
  python3 datagen/generate.py lang                          # rewrites the shared (committed) lang file
"""
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANG = ROOT / "src/main/resources/assets/sprocket_dyes/lang/en_us.json"
NS = "sprocket_dyes"
T = "magenta"  # template color
VERSIONS = ["1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2", "26.3"]

sys.path.insert(0, str(Path(__file__).resolve().parent))
import glazed  # noqa: E402
from mixing import mixing_recipes  # noqa: E402
from vanilla import VanillaJar  # noqa: E402

PALETTE = json.loads((ROOT / "palette/colors.json").read_text())
ALL_COLORS = PALETTE["key"] + PALETTE["colors"]  # display order
COLOR_IDS = [c["id"] for c in ALL_COLORS]
NEW_IDS = [c["id"] for c in PALETTE["colors"] if not c["vanilla"]]
VANILLA_IDS = [c["id"] for c in ALL_COLORS if c["vanilla"]]
NAMES = {c["id"]: c["name"] for c in ALL_COLORS}


def vkey(v):
    return tuple(int(x) for x in v.split("."))


# ---------------------------------------------------------------------------------------------
# Products
# ---------------------------------------------------------------------------------------------

class Product:
    """
    A per-color registry entry: `{color}_{suffix}`.

    vanilla_since: version from which vanilla registers it for its own 16 colors (None: always, "never": never)
    since:         version from which it exists at all
    shape:         for slab/stairs/wall products, (base suffix, shape)
    undyed:        for shapes, the undyed base that also gets them ("glass", "terracotta")
    stonecut:      shapes also get stonecutter recipes
    redye:         shape is re-dyeable like its base (wool family)
    """

    def __init__(self, suffix, vanilla_since=None, since=None, shape=None, undyed=None, stonecut=False, redye=False):
        self.suffix = suffix
        self.vanilla_since = vanilla_since
        self.since = since
        self.shape = shape
        self.undyed = undyed
        self.stonecut = stonecut
        self.redye = redye

    def exists(self, v):
        return self.since is None or vkey(v) >= vkey(self.since)

    def vanilla_has(self, v):
        if self.vanilla_since == "never":
            return False
        return self.vanilla_since is None or vkey(v) >= vkey(self.vanilla_since)

    def ours(self, v, color):
        """Whether we register this for `color` (a color id, or None for the undyed variant) on version v."""
        if not self.exists(v):
            return False
        if color is None:
            return self.undyed is not None
        return color in NEW_IDS or not self.vanilla_has(v)

    def path(self, color):
        if color is None:
            return f"{self.undyed}_{self.shape[1]}"
        return f"{color}_{self.suffix}"


DYE = Product("dye")
KINDS = [Product(s) for s in (
    "wool", "carpet", "terracotta", "concrete", "concrete_powder", "glazed_terracotta", "stained_glass", "stained_glass_pane",
    "bed", "banner", "wall_banner", "shulker_box", "candle", "candle_cake",
)] + [
    Product("bundle", since="1.21.2"),
    Product("harness", since="1.21.6"),
    Product("cushion", since="26.3"),
]
# Products that are items only (lang key item.<ns>.<path>).
ITEM_ONLY = {"dye", "bundle", "harness", "cushion"}
# Blocks without an item.
BLOCK_ONLY = {"wall_banner", "candle_cake"}
SHAPE_BASES = {
    # base suffix: (undyed base, stonecutter recipes, re-dyeable, vanilla_since for slab/stairs)
    "wool": (None, False, True, "26.3"),
    "terracotta": ("terracotta", True, False, "never"),
    "glazed_terracotta": (None, True, False, "never"),
    "concrete": (None, True, False, "26.3"),
    "stained_glass": ("glass", False, False, "never"),
}
SHAPES = []
for base, (undyed, stonecut, redye, vsince) in SHAPE_BASES.items():
    for shape in ("slab", "stairs", "wall"):
        SHAPES.append(Product(f"{base}_{shape}", vanilla_since=vsince if shape != "wall" else "never",
                              shape=(base, shape), undyed=undyed, stonecut=stonecut, redye=redye))

PRODUCTS = [DYE] + KINDS + SHAPES
BY_SUFFIX = {p.suffix: p for p in PRODUCTS}

# Vanilla tags that describe a block's material and should carry over from a base block to its shapes.
MATERIAL_TAGS = re.compile(r"^minecraft:(mineable/.*|needs_.*_tool|dampens_vibrations|occludes_vibration_signals|impermeable|shears_major_breaking_speed)$")
SHAPE_TAGS = {"slab": "minecraft:slabs", "stairs": "minecraft:stairs", "wall": "minecraft:walls"}


# ---------------------------------------------------------------------------------------------
# Context
# ---------------------------------------------------------------------------------------------

TOKEN = re.compile(r"(?<![a-z0-9])" + T + r"(?![a-z0-9])")
NAMESPACED = re.compile(r"minecraft:([a-z0-9_./-]+)")
BARE_MODEL_PATH = re.compile(r'"((?:block|item)/[a-z0-9_./-]+)"')
ASSET_DIRS = ["blockstates", "models/block", "models/item", "items", "equipment", "models/equipment"]
# Vanilla code builds these ids from the color name in its own namespace (EquipmentAssets/EquipmentModels),
# so ours have to live there too.
MINECRAFT_NAMESPACED_DIRS = {"equipment", "models/equipment"}
DATA_DIRS = ["loot_table/blocks", "recipe"]


class Ctx:
    def __init__(self, version, out):
        self.version = version
        self.vj = VanillaJar(version)
        self.out = Path(out)
        self.ours = set()
        for p in PRODUCTS:
            for c in COLOR_IDS + [None]:
                if p.ours(version, c):
                    self.ours.add(p.path(c))
        self.tags = {}
        self.tag_index = self._index_tags()
        self.tags_by_entry = {}  # (registry, entry) -> [tag ids]
        for (reg, tid), vals in self.tag_index.items():
            for v in vals:
                self.tags_by_entry.setdefault((reg, v), []).append(tid)
        self.lang = {}

    def _index_tags(self):
        idx = {}
        for reg in ("item", "block"):
            base = f"data/minecraft/tags/{reg}/"
            for f in self.vj.rglob(base, "*.json"):
                idx[(reg, "minecraft:" + str(f)[len(base):-5])] = json.loads(self.vj.read_text(f)).get("values", [])
        return idx

    def is_ours(self, path):
        seg = path.rsplit("/", 1)[-1]
        if seg.startswith("dye_"):
            seg = seg[4:]  # re-dye recipe ids: dye_<name>
        if seg in self.ours:
            return True
        # model/texture variants of our blocks: <name>_top, <name>_inner, <name>_post, ...
        parts = seg.split("_")
        return any("_".join(parts[:i]) in self.ours for i in range(len(parts) - 1, 1, -1))

    def fix_namespaces(self, text):
        text = NAMESPACED.sub(lambda m: f"{NS}:{m.group(1)}" if self.is_ours(m.group(1)) else m.group(0), text)
        # Some models refer to textures/parents without a namespace ("item/magenta_bundle_open_front").
        return BARE_MODEL_PATH.sub(lambda m: f'"{NS}:{m.group(1)}"' if self.is_ours(m.group(1)) else m.group(0), text)

    def ref(self, product, color):
        return f"{NS if product.ours(self.version, color) else 'minecraft'}:{product.path(color)}"

    def write(self, side, ns, rel, obj):
        p = self.out / side / ns / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(obj if isinstance(obj, str) else json.dumps(obj, indent=2) + "\n")

    def tag(self, registry, tag_id, entry):
        self.tags.setdefault((registry, tag_id), set()).add(entry)

    def vanilla_tags_of(self, registry, entry):
        return self.tags_by_entry.get((registry, entry), [])


# ---------------------------------------------------------------------------------------------
# Template discovery
# ---------------------------------------------------------------------------------------------

def magenta_templates(ctx):
    """product suffix -> list of (side, dir, stem) for vanilla magenta files of that product."""
    suffixes = sorted(BY_SUFFIX, key=len, reverse=True)
    found = {}
    for side, dirs in (("assets", ASSET_DIRS), ("data", DATA_DIRS)):
        for d in dirs:
            for f in ctx.vj.glob(f"{side}/minecraft/{d}", "*.json"):
                stem = f.stem
                core = stem[4:] if stem.startswith("dye_" + T + "_") else stem
                if not core.startswith(T + "_"):
                    continue
                rest = core[len(T) + 1:]
                for s in suffixes:
                    if rest == s or rest.startswith(s + "_"):
                        if s == "dye" and rest != "dye":
                            break  # flower -> dye recipes
                        found.setdefault(s, []).append((side, d, stem))
                        break
    return found


FAMILY = "stone_brick"
FAMILY_BASE = "stone_bricks"


def family_templates(ctx, shape, stonecut):
    out = []
    for side, dirs in (("assets", ASSET_DIRS), ("data", DATA_DIRS)):
        for d in dirs:
            for f in ctx.vj.glob(f"{side}/minecraft/{d}", f"{FAMILY}_{shape}*.json"):
                stem = f.stem
                rest = stem[len(f"{FAMILY}_{shape}"):]
                if "stonecutting" in rest:
                    if not stonecut or rest != f"_from_{FAMILY_BASE}_stonecutting":
                        continue
                elif rest and d == "recipe":
                    continue
                out.append((side, d, stem))
    return out


# ---------------------------------------------------------------------------------------------
# Writing products
# ---------------------------------------------------------------------------------------------

def retarget_color(ctx, text, color):
    return ctx.fix_namespaces(TOKEN.sub(color, text))


def retarget_family(ctx, text, base_id, shape):
    # 26.3 refers to the stone bricks family through a #stone_bricks tag; our bases are single blocks.
    text = text.replace(f"#minecraft:{FAMILY_BASE}", f"minecraft:{FAMILY_BASE}")
    text = re.sub(rf"(?<![a-z0-9]){FAMILY}_({shape})(?![a-z0-9])", lambda m: f"{base_id}_{m.group(1)}", text)
    text = re.sub(rf"(?<![a-z0-9]){FAMILY_BASE}(?![a-z0-9])", base_id, text)
    return ctx.fix_namespaces(text)


def write_advancement(ctx, recipe_stem_template, out_stem, transform):
    for adv in ctx.vj.rglob("data/minecraft/advancement/recipes", f"{recipe_stem_template}.json"):
        ctx.write("data", NS, f"advancement/recipes/{adv.parent.name}/{out_stem}.json", transform(ctx.vj.read_text(adv)))


def generate_colored(ctx, product, templates):
    for color in COLOR_IDS:
        if not product.ours(ctx.version, color):
            continue
        for side, d, stem in templates:
            out_stem = stem.replace(T, color)
            text = retarget_color(ctx, ctx.vj.read_text(f"{side}/minecraft/{d}/{stem}.json"), color)
            obj = json.loads(text)
            if d == "recipe":
                obj = rebuild_color_lists(ctx, obj, color)
                write_advancement(ctx, stem, out_stem, lambda t, c=color: retarget_color(ctx, t, c))
            ctx.write(side, "minecraft" if d in MINECRAFT_NAMESPACED_DIRS else NS, f"{d}/{out_stem}.json", obj)
        mirror_tags(ctx, product, color, product.path(T))
        name_colored(ctx, product, color)


def generate_shape(ctx, product, own_templates):
    base_suffix, shape = product.shape
    for color in COLOR_IDS + [None]:
        if not product.ours(ctx.version, color):
            continue
        base_id = product.undyed if color is None else f"{color}_{base_suffix}"
        own = bool(own_templates) and color is not None
        templates = own_templates if own else family_templates(ctx, shape, product.stonecut)
        for side, d, stem in templates:
            if base_suffix == "glazed_terracotta" and d in ("blockstates", "models/block"):
                continue  # see glazed_shape_assets
            src = ctx.vj.read_text(f"{side}/minecraft/{d}/{stem}.json")
            if own:
                text, out_stem = retarget_color(ctx, src, color), stem.replace(T, color)
                fix = lambda t, c=color: retarget_color(ctx, t, c)
            else:
                text = retarget_family(ctx, src, base_id, shape)
                out_stem = re.sub(rf"^{FAMILY}_{shape}", f"{base_id}_{shape}", stem).replace(FAMILY_BASE, base_id)
                fix = lambda t, b=base_id: retarget_family(ctx, t, b, shape)
            obj = json.loads(text)
            if d == "recipe":
                obj = rebuild_color_lists(ctx, obj, color)
                if not own:
                    obj = style_shape_recipe(obj, base_suffix, shape)
                write_advancement(ctx, stem, out_stem, fix)
            ctx.write(side, NS, f"{d}/{out_stem}.json", obj)
        if base_suffix == "glazed_terracotta":
            glazed_shape_assets(ctx, product, color)
        if product.redye and color is not None and not own:
            derived_redye(ctx, product, color)
        shape_tags(ctx, product, color, base_id, own)
        name_shape(ctx, product, color, base_id)


def glazed_shape_assets(ctx, product, color):
    """Glazed shapes face like the full block and show its pattern, cut to shape (datagen/glazed.py)."""
    full = ctx.ref(BY_SUFFIX["glazed_terracotta"], color)
    ns, path = full.split(":")
    glazed.write_shape(lambda d, p, obj: ctx.write("assets", NS, f"{d}/{p}.json", obj),
                       NS, product.shape[1], product.path(color), f"{ns}:block/{path}", f"{ns}:block/{path}")


def style_shape_recipe(obj, base_suffix, shape):
    """Recipes templated from stone bricks: give them the group vanilla uses for its 26.3 wool shapes."""
    if obj.get("type", "").endswith("crafting_shaped") and base_suffix == "wool":
        obj["group"] = {"slab": "woolen_slab", "stairs": "woolen_stairs", "wall": "woolen_wall"}[shape]
    return obj


def derived_redye(ctx, product, color):
    """Re-dye recipe for a wool shape where vanilla has none to copy: derived from dye_<c>_wool."""
    _, shape = product.shape
    stem = f"dye_{T}_wool"
    path = f"data/minecraft/recipe/{stem}.json"
    if not ctx.vj.exists(path):
        return

    def derive(t):
        t = re.sub(r'_wool(?=["/])', f"_wool_{shape}", t)
        return retarget_color(ctx, t, color)

    obj = rebuild_color_lists(ctx, json.loads(derive(ctx.vj.read_text(path))), color)
    obj["group"] = f"wool_{shape}s" if shape != "stairs" else "wool_stairs"
    out_stem = f"dye_{color}_wool_{shape}"
    ctx.write("data", NS, f"recipe/{out_stem}.json", obj)
    write_advancement(ctx, stem, out_stem, derive)


# ---------------------------------------------------------------------------------------------
# "Every other color" ingredient lists
# ---------------------------------------------------------------------------------------------

COLOR_REF = re.compile(r"^(minecraft|" + NS + r"):(" + "|".join(sorted(COLOR_IDS, key=len, reverse=True)) + r")_([a-z_]+)$")


def _ids_of(alt):
    """The item ids of an alternatives ingredient, plus a function rebuilding one in the same format."""
    if isinstance(alt, list) and alt and all(isinstance(x, str) for x in alt):
        return alt, lambda ids: ids
    if isinstance(alt, list) and alt and all(isinstance(x, dict) and "item" in x for x in alt):
        return [x["item"] for x in alt], lambda ids: [{"item": i} for i in ids]
    return None, None


def rebuild_color_lists(ctx, recipe, color=None):
    """
    Vanilla re-dye recipes list every *other* color's variant as one ingredient. Rebuild such lists so they
    cover all 132 colors (minus the result's own color) with each entry in its proper namespace.
    """
    def fix(alt):
        ids, make = _ids_of(alt)
        if not ids or len(ids) < 8:
            return alt
        matches = [COLOR_REF.match(i) for i in ids]
        if not all(matches):
            return alt
        suffixes = {m.group(3) for m in matches}
        if len(suffixes) != 1:
            return alt
        product = BY_SUFFIX.get(suffixes.pop())
        if product is None or not product.exists(ctx.version):
            return alt
        full = [ctx.ref(product, c) for c in COLOR_IDS
                if c != color and (product.ours(ctx.version, c) or product.vanilla_has(ctx.version))]
        return make(full)

    if "ingredients" in recipe:
        recipe["ingredients"] = [fix(i) for i in recipe["ingredients"]]
    if "ingredient" in recipe:
        recipe["ingredient"] = fix(recipe["ingredient"])
    if "key" in recipe:
        recipe["key"] = {k: fix(v) for k, v in recipe["key"].items()}
    for k in ("target", "input"):
        if k in recipe:
            recipe[k] = fix(recipe[k])
    return recipe


def override_vanilla_redyes(ctx):
    """Vanilla's own dye_<color>_<x> recipes only accept vanilla variants; override them to accept ours too."""
    for f in ctx.vj.glob("data/minecraft/recipe", "dye_*.json"):
        m = re.match(r"^dye_(" + "|".join(VANILLA_IDS) + r")_([a-z_]+)$", f.stem)
        if not m:
            continue
        color, suffix = m.groups()
        if suffix not in BY_SUFFIX:
            continue  # beds, harnesses, ... are handled with their products
        obj = json.loads(ctx.vj.read_text(f))
        before = json.dumps(obj)
        obj = rebuild_color_lists(ctx, obj, color)
        if json.dumps(obj) != before:
            ctx.write("data", "minecraft", f"recipe/{f.stem}.json", obj)


# ---------------------------------------------------------------------------------------------
# Tags
# ---------------------------------------------------------------------------------------------

def mirror_tags(ctx, product, color, template_path):
    needle = f"minecraft:{template_path}"
    ref = ctx.ref(product, color)
    for reg in ("item", "block"):
        for tid in ctx.vanilla_tags_of(reg, needle):
            ctx.tag(reg, tid, ref)
    if color in NEW_IDS and product is not DYE:
        registries = ("item",) if product.suffix in ITEM_ONLY else ("block",) if product.suffix in BLOCK_ONLY else ("item", "block")
        for reg in registries:
            ctx.tag(reg, f"c:dyed/{color}", ref)


def shape_tags(ctx, product, color, base_id, own):
    base_suffix, shape = product.shape
    ref = ctx.ref(product, color)
    if own:
        mirror_tags(ctx, product, color, product.path(T))
        return
    for reg in ("block", "item"):
        ctx.tag(reg, SHAPE_TAGS[shape], ref)
    # Material tags (tools, vibrations, ...) come from the base block; magenta's stands in for new colors.
    template_base = f"minecraft:{base_id}" if color is None or color in VANILLA_IDS else f"minecraft:{T}_{base_suffix}"
    for tid in ctx.vanilla_tags_of("block", template_base):
        if MATERIAL_TAGS.match(tid):
            ctx.tag("block", tid, ref)
    if color in NEW_IDS:
        for reg in ("item", "block"):
            ctx.tag(reg, f"c:dyed/{color}", ref)


def conventional_tags(ctx):
    for c in NEW_IDS:
        ctx.tag("item", "c:dyes", ctx.ref(DYE, c))
        ctx.tag("item", f"c:dyes/{c}", ctx.ref(DYE, c))
        for reg in ("item", "block"):
            ctx.tag(reg, "c:dyed", f"#c:dyed/{c}")
    groups = {"stained_glass": "c:glass_blocks", "stained_glass_pane": "c:glass_panes", "concrete": "c:concretes",
              "concrete_powder": "c:concrete_powders", "glazed_terracotta": "c:glazed_terracottas"}
    for suffix, tid in groups.items():
        for c in NEW_IDS:
            for reg in ("item", "block"):
                ctx.tag(reg, tid, ctx.ref(BY_SUFFIX[suffix], c))


def write_tags(ctx):
    for (reg, tid), entries in sorted(ctx.tags.items()):
        ns, path = tid.split(":", 1)
        ctx.write("data", ns, f"tags/{reg}/{path}.json", {"replace": False, "values": sorted(entries)})


# ---------------------------------------------------------------------------------------------
# Lang
# ---------------------------------------------------------------------------------------------

_vanilla_lang = {}


def vanilla_lang(ctx):
    if ctx.version not in _vanilla_lang:
        _vanilla_lang[ctx.version] = json.loads(ctx.vj.read_text("assets/minecraft/lang/en_us.json"))
    return _vanilla_lang[ctx.version]


def block_name(ctx, path):
    """English name of a block id (ours or vanilla) on this version."""
    return ctx.lang.get(f"block.{NS}.{path}") or vanilla_lang(ctx).get(f"block.minecraft.{path}")


def name_colored(ctx, product, color):
    if product is DYE:
        return  # dye names come from palette/generate_code.py
    kind = "item" if product.suffix in ITEM_ONLY else "block"
    vname = vanilla_lang(ctx).get(f"{kind}.minecraft.{T}_{product.suffix}")
    if vname:
        ctx.lang[f"{kind}.{NS}.{product.path(color)}"] = vname.replace("Magenta", NAMES[color])


def color_lang(ctx):
    """Vanilla keys built from a color's name: banner pattern layers, shields."""
    for key, value in vanilla_lang(ctx).items():
        if key.startswith("block.minecraft.banner.") and key.endswith(f".{T}") or key == f"item.minecraft.shield.{T}":
            for c in NEW_IDS:
                ctx.lang[key[: -len(T)] + c] = value.replace("Magenta", NAMES[c])


def name_shape(ctx, product, color, base_id):
    _, shape = product.shape
    base = block_name(ctx, base_id) or base_id.replace("_", " ").title()
    ctx.lang[f"block.{NS}.{product.path(color)}"] = f"{base} {shape.title()}"


# ---------------------------------------------------------------------------------------------
# Mixing recipes, trades
# ---------------------------------------------------------------------------------------------

def generate_mixing(ctx):
    template = json.loads(ctx.vj.read_text("data/minecraft/recipe/magenta_dye_from_purple_and_pink.json"))
    adv_template = ctx.vj.read_text("data/minecraft/advancement/recipes/misc/magenta_dye_from_purple_and_pink.json")
    recipes, unreachable = mixing_recipes(PALETTE, ctx.vj)
    if unreachable:
        raise SystemExit(f"{ctx.version}: colors unreachable through mixing: {unreachable}")
    for r in recipes:
        a, b, result = r["a"], r["b"], r["result"]
        rid = f"{result}_dye_from_{a}_and_{b}"
        recipe = json.loads(json.dumps(template))
        recipe["group"] = f"{result}_dye"
        ing = [ctx.ref(DYE, a), ctx.ref(DYE, b)]
        recipe["ingredients"] = [{"item": i} for i in ing] if isinstance(template["ingredients"][0], dict) else ing
        recipe["result"]["id"] = ctx.ref(DYE, result)
        recipe["result"]["count"] = 2
        ctx.write("data", NS, f"recipe/{rid}.json", recipe)
        adv = json.loads(adv_template)
        crit_items = json.loads(adv_template)["criteria"]["has_pink_dye"]
        adv["criteria"] = {f"has_{a}_dye": _with_item(crit_items, ctx.ref(DYE, a)),
                           f"has_{b}_dye": _with_item(crit_items, ctx.ref(DYE, b))}
        unlock = json.loads(adv_template)["criteria"]["has_the_recipe"]
        key = "recipe" if "recipe" in unlock["conditions"] else "recipes"
        unlock["conditions"][key] = f"{NS}:{rid}"
        adv["criteria"]["has_the_recipe"] = unlock
        adv["requirements"] = [["has_the_recipe", f"has_{a}_dye", f"has_{b}_dye"]]
        adv["rewards"]["recipes"] = [f"{NS}:{rid}"]
        ctx.write("data", NS, f"advancement/recipes/misc/{rid}.json", adv)
    return len(recipes)


def _with_item(crit, item_id):
    crit = json.loads(json.dumps(crit))
    p = crit["conditions"]["items"][0]
    p["items"] = item_id if isinstance(p["items"], str) else [item_id]
    return crit


def sheep_loot(ctx):
    """
    Per-color sheep loot tables (death; shearing from 1.21.2) live in vanilla's namespace, where its code looks
    them up by color name. The 1.21.2+ root shearing table picks one per color, so it gets all 132.
    """
    for kind in ("entities/sheep", "shearing/sheep"):
        path = f"data/minecraft/loot_table/{kind}/{T}.json"
        if not ctx.vj.exists(path):
            continue
        for c in NEW_IDS:
            ctx.write("data", "minecraft", f"loot_table/{kind}/{c}.json", retarget_color(ctx, ctx.vj.read_text(path), c))
    root = "data/minecraft/loot_table/shearing/sheep.json"
    if ctx.vj.exists(root):
        table = json.loads(ctx.vj.read_text(root))
        children = table["pools"][0]["entries"][0]["children"]
        template = next(ch for ch in children if f"/{T}\"" in json.dumps(ch) or f"shearing/sheep/{T}" in json.dumps(ch))
        for c in NEW_IDS:
            children.append(json.loads(TOKEN.sub(c, json.dumps(template))))
        ctx.write("data", "minecraft", "loot_table/shearing/sheep.json", table)


def wandering_trader(ctx):
    """26.1+: data-driven trades. Four 'random new dye' offers in the common pool (see ModTrades)."""
    path = "data/minecraft/villager_trade/wandering_trader/emerald_magenta_dye.json"
    if not ctx.vj.exists(path):
        return  # code-registered on this version
    for i in range(1, 5):
        trade = json.loads(ctx.vj.read_text(path))
        # 26.1-26.2: a list of {"function": ...}; 26.3: a single function keyed by "type".
        if vkey(ctx.version) >= vkey("26.3"):
            trade["given_item_modifier"] = {"type": f"{NS}:random_new_dye"}
        else:
            trade["given_item_modifiers"] = [{"function": f"{NS}:random_new_dye"}]
        name = f"wandering_trader/emerald_random_dye_{i}"
        ctx.write("data", NS, f"villager_trade/{name}.json", trade)
        ctx.tag("villager_trade", "minecraft:wandering_trader/common", f"{NS}:{name}")


# ---------------------------------------------------------------------------------------------

def generate(version, out, write_files=True):
    ctx = Ctx(version, out)
    if write_files and ctx.out.exists():
        shutil.rmtree(ctx.out)
    if not write_files:
        ctx.write = lambda *a, **k: None
    templates = magenta_templates(ctx)
    for p in [DYE] + KINDS:
        generate_colored(ctx, p, templates.get(p.suffix, []))
    for p in SHAPES:
        generate_shape(ctx, p, templates.get(p.suffix, []))
    override_vanilla_redyes(ctx)
    sheep_loot(ctx)
    color_lang(ctx)
    n_mix = generate_mixing(ctx)
    conventional_tags(ctx)
    wandering_trader(ctx)
    write_tags(ctx)
    if write_files:
        files = sum(1 for _ in ctx.out.rglob("*.json"))
        print(f"{version}: {files} files ({n_mix} mixing recipes) -> {ctx.out}")
    return ctx.lang


def write_lang(langs):
    merged = {}
    for lang in langs:
        merged.update(lang)
    out = json.loads(LANG.read_text()) if LANG.exists() else {}
    new_suffixes = tuple(f".{c}" for c in NEW_IDS)
    out = {k: v for k, v in out.items()
           if not k.startswith(f"block.{NS}.")
           and not (k.startswith(f"item.{NS}.") and not k.endswith("_dye"))
           and not ((k.startswith("block.minecraft.banner.") or k.startswith("item.minecraft.shield.")) and k.endswith(new_suffixes))}
    out.update(merged)
    LANG.write_text(json.dumps(dict(sorted(out.items())), indent=2, ensure_ascii=False) + "\n")
    print(f"wrote {LANG} ({len(out)} keys)")


def main():
    args = sys.argv[1:]
    if args[:1] == ["data"] and len(args) == 3:
        generate(args[1], args[2])
    elif args == ["lang"]:
        write_lang([generate(v, "/dev/null", write_files=False) for v in VERSIONS])
    else:
        raise SystemExit(__doc__)


if __name__ == "__main__":
    main()

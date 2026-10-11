"""
Block models and blockstates for glazed terracotta slabs, stairs and walls.

Each shape is cut out of the full glazed terracotta block: every face shows exactly the part of the pattern
the full block would show there, so a slab or stair continues the pattern of the block it's placed like.
Like the full block (and unlike stone-like shapes), the blockstates rotate models *without* uvlock so the
pattern turns with the block.

Models are written in a fixed "pattern frame" and turned into place by the blockstate's y rotation:
  slabs, walls: the pattern of a full block facing south (the full block's own unrotated model);
                the block's `facing` property picks the rotation, exactly as for the full block
  stairs:       the stairs' own frame (facing east, like vanilla's unrotated stairs models), where the
                pattern faces west: the way a full block placed by the same player would face

Face UVs follow FaceBakery: a face's vertex k (in default-UV corner order) gets UV corner (k + rotation) % 4,
with corners ordered (u0,v0), (u0,v1), (u1,v1), (u1,v0).
"""

DIRS = ("down", "up", "north", "south", "west", "east")
HORIZONTAL = ("north", "east", "south", "west")
# Blockstate y rotation of the full glazed block per facing; vanilla stairs' rotation per facing.
GLAZED_Y = {"south": 0, "west": 90, "north": 180, "east": 270}
STAIRS_Y = {"east": 0, "south": 90, "west": 180, "north": 270}
# template_glazed_terracotta: every face is uv [0,0,16,16] with these rotations.
TEMPLATE_ROTATION = {"down": 0, "up": 0, "north": 90, "south": 270, "west": 0, "east": 180}
CORNERS = ((0, 0), (0, 1), (1, 1), (1, 0))  # (u index, v index) into (min, max) per vertex


def rot_y(p, deg):
    """Rotate a point about the block's vertical center line, clockwise seen from above (MC's y rotation)."""
    x, y, z = p
    for _ in range((deg // 90) % 4):
        x, z = 16 - z, x
    return x, y, z


def rot_dir(d, deg):
    if d in ("up", "down"):
        return d
    return HORIZONTAL[(HORIZONTAL.index(d) + (deg // 90)) % 4]


def default_uv(frm, to, d):
    """FaceBakery.defaultFaceUV: (minU, minV, maxU, maxV)."""
    fx, fy, fz = frm
    tx, ty, tz = to
    return {
        "down": (fx, 16 - tz, tx, 16 - fz),
        "up": (fx, fz, tx, tz),
        "north": (16 - tx, 16 - ty, 16 - fx, 16 - fy),
        "south": (fx, 16 - ty, tx, 16 - fy),
        "west": (fz, 16 - ty, tz, 16 - fy),
        "east": (16 - tz, 16 - ty, 16 - fz, 16 - fy),
    }[d]


def point_of(u, v, d, plane):
    """The point on face plane `plane` (coordinate along d's axis) with default UV (u, v)."""
    return {
        "down": (u, plane, 16 - v),
        "up": (u, plane, v),
        "north": (16 - u, 16 - v, plane),
        "south": (u, 16 - v, plane),
        "west": (plane, 16 - v, u),
        "east": (plane, 16 - v, 16 - u),
    }[d]


def uv_of(p, d):
    x, y, z = p
    return {
        "down": (x, 16 - z),
        "up": (x, z),
        "north": (16 - x, 16 - y),
        "south": (x, 16 - y),
        "west": (z, 16 - y),
        "east": (16 - z, 16 - y),
    }[d]


def template_tex(d, u, v):
    """Texture coordinate the full (south-facing) glazed block shows at default UV (u, v) of face d."""
    s = TEMPLATE_ROTATION[d] // 90
    a, b = u / 16, v / 16
    # Vertex k at default corner k gets texture corner (k + s) % 4 of [0,0,16,16]; interpolate between them.
    tex = [tuple(16 * c for c in CORNERS[(k + s) % 4]) for k in range(4)]
    w = ((1 - a) * (1 - b), (1 - a) * b, a * b, a * (1 - b))
    return (sum(w[k] * tex[k][0] for k in range(4)), sum(w[k] * tex[k][1] for k in range(4)))


def face_uv(frm, to, d, frame_y):
    """
    (uv, rotation) for face d of box frm..to in a model whose pattern is the full block turned by frame_y.
    """
    plane = (frm if d in ("down", "north", "west") else to)[{"down": 1, "up": 1, "north": 2, "south": 2, "west": 0, "east": 0}[d]]
    mn_u, mn_v, mx_u, mx_v = default_uv(frm, to, d)
    corners = [(mn_u if cu == 0 else mx_u, mn_v if cv == 0 else mx_v) for cu, cv in CORNERS]
    tex = []
    for u, v in corners:
        p = rot_y(point_of(u, v, d, plane), -frame_y % 360)
        td = rot_dir(d, -frame_y % 360)
        tex.append(tuple(round(c, 4) for c in template_tex(td, *uv_of(p, td))))
    # Find the rotation that makes the corners a (non-mirrored) UV rectangle.
    best = None
    for s in range(4):
        uvc = [tex[(j - s) % 4] for j in range(4)]  # UV corner j = texture at vertex (j - s)
        if uvc[0][0] == uvc[1][0] and uvc[2][0] == uvc[3][0] and uvc[0][1] == uvc[3][1] and uvc[1][1] == uvc[2][1]:
            cand = ([uvc[0][0], uvc[0][1], uvc[2][0], uvc[2][1]], s * 90)
            if cand[0][0] <= cand[0][2] and cand[0][1] <= cand[0][3]:
                return cand
            best = best or cand
    if best is None:
        raise ValueError(f"no UV rectangle for {d} {frm}..{to}")
    return best


def _num(x):
    return int(x) if float(x).is_integer() else x


def _covered(box, d, boxes):
    """Whether face d of box is entirely pressed against another box of the same model."""
    frm, to = box
    axis = {"down": 1, "up": 1, "north": 2, "south": 2, "west": 0, "east": 0}[d]
    positive = d in ("up", "south", "east")
    plane = (to if positive else frm)[axis]
    others = [i for i in range(3) if i != axis]
    for of, ot in boxes:
        if (of, ot) == box:
            continue
        if (of if positive else ot)[axis] != plane:
            continue
        if all(of[i] <= frm[i] and ot[i] >= to[i] for i in others):
            return True
    return False


def model(boxes, frame_y, texture, parent=None, skip=None):
    """A model of `boxes` ((from, to) pairs) textured as glazed terracotta seen in pattern frame frame_y."""
    elements = []
    for i, (frm, to) in enumerate(boxes):
        faces = {}
        for d in DIRS:
            if skip and d in skip.get(i, ()):
                continue
            if _covered((frm, to), d, boxes):
                continue
            uv, rotation = face_uv(frm, to, d, frame_y)
            face = {"uv": [_num(c) for c in uv], "texture": "#pattern"}
            if rotation:
                face["rotation"] = rotation
            axis = {"down": 1, "up": 1, "north": 2, "south": 2, "west": 0, "east": 0}[d]
            if (to[axis] == 16) if d in ("up", "south", "east") else (frm[axis] == 0):
                face["cullface"] = d
            faces[d] = face
        elements.append({"from": [_num(c) for c in frm], "to": [_num(c) for c in to], "faces": faces})
    out = {}
    if parent:
        out["parent"] = parent
    out["textures"] = {"pattern": texture, "particle": texture}
    out["elements"] = elements
    return out


def flip(boxes):
    """Upside-down copy (y -> 16 - y)."""
    return [((f[0], 16 - t[1], f[2]), (t[0], 16 - f[1], t[2])) for f, t in boxes]


def rotate_box(box, deg):
    a, b = rot_y(box[0], deg), rot_y(box[1], deg)
    return (tuple(min(a[i], b[i]) for i in range(3)), tuple(max(a[i], b[i]) for i in range(3)))


# Geometry in the stairs' own frame (facing east; "right" of east is south).
_LOW = ((0, 0, 0), (16, 8, 16))
_BACK = ((8, 8, 0), (16, 16, 16))
STAIRS = {
    "straight": [_LOW, _BACK],
    "inner_right": [_LOW, _BACK, ((0, 8, 8), (8, 16, 16))],
    "inner_left": [_LOW, _BACK, ((0, 8, 0), (8, 16, 8))],
    "outer_right": [_LOW, ((8, 8, 8), (16, 16, 16))],
    "outer_left": [_LOW, ((8, 8, 0), (16, 16, 8))],
}
SLAB = {"bottom": [((0, 0, 0), (16, 8, 16))], "top": [((0, 8, 0), (16, 16, 16))]}
WALL_POST = [((4, 0, 4), (12, 16, 12))]
WALL_SIDE = {"low": ((5, 0, 0), (11, 14, 8)), "tall": ((5, 0, 0), (11, 16, 8))}  # north side
WALL_INVENTORY = [((4, 0, 4), (12, 16, 12)), ((5, 0, 0), (11, 13, 16))]


def stairs_model_name(half, shape):
    return ("" if half == "bottom" and shape == "straight" else "_" + "_".join(
        p for p in (("top" if half == "top" else ""), ("" if shape == "straight" else shape)) if p))


def write_shape(write, ns, shape, block_id, texture, full_model):
    """
    Writes blockstate + block models for one glazed shape block.

    write(dir, path, obj) writes assets/<ns>/<dir>/<path>.json; block_id is the shape's path
    ("lime_glazed_terracotta_slab"); texture / full_model are the full block's texture and model ids.
    """
    def m(name):
        return f"{ns}:block/{block_id}{name}"

    if shape == "stairs":
        frame = GLAZED_Y["west"]  # the pattern faces west in the stairs' (east-facing) frame
        variants = {}
        for half in ("bottom", "top"):
            for kind, boxes in STAIRS.items():
                name = stairs_model_name(half, kind)
                write("models/block", block_id + name,
                      model(boxes if half == "bottom" else flip(boxes), frame, texture, parent="minecraft:block/stairs"))
                for facing in HORIZONTAL:
                    v = {"model": m(name)}
                    if STAIRS_Y[facing]:
                        v["y"] = STAIRS_Y[facing]
                    variants[f"facing={facing},half={half},shape={kind}"] = v
        write("blockstates", block_id, {"variants": dict(sorted(variants.items()))})

    elif shape == "slab":
        write("models/block", block_id, model(SLAB["bottom"], 0, texture, parent="minecraft:block/slab"))
        write("models/block", block_id + "_top", model(SLAB["top"], 0, texture, parent="minecraft:block/slab_top"))
        variants = {}
        for facing in HORIZONTAL:
            for kind, ref in (("bottom", m("")), ("top", m("_top")), ("double", full_model)):
                v = {"model": ref}
                if GLAZED_Y[facing]:
                    v["y"] = GLAZED_Y[facing]
                variants[f"facing={facing},type={kind}"] = v
        write("blockstates", block_id, {"variants": dict(sorted(variants.items()))})

    elif shape == "wall":
        write("models/block", block_id + "_post", model(WALL_POST, 0, texture))
        for height, box in WALL_SIDE.items():
            for side in HORIZONTAL:
                deg = HORIZONTAL.index(side) * 90
                # The face toward the post is never seen.
                write("models/block", f"{block_id}_side_{'tall_' if height == 'tall' else ''}{side}",
                      model([rotate_box(box, deg)], 0, texture, skip={0: (rot_dir("south", deg),)}))
        write("models/block", block_id + "_inventory", model(WALL_INVENTORY, 0, texture, parent="minecraft:block/wall_inventory"))
        parts = []
        for facing in HORIZONTAL:
            y = GLAZED_Y[facing]
            post = {"model": m("_post")}
            if y:
                post["y"] = y
            parts.append({"when": {"facing": facing, "up": "true"}, "apply": post})
            for side in HORIZONTAL:
                local = rot_dir(side, -y % 360)  # the side in the pattern frame
                for height in ("low", "tall"):
                    v = {"model": m(f"_side_{'tall_' if height == 'tall' else ''}{local}")}
                    if y:
                        v["y"] = y
                    parts.append({"when": {"facing": facing, side: height}, "apply": v})
        write("blockstates", block_id, {"multipart": parts})

"""
The dye mixing grid. Every recipe is shapeless, takes two dyes and makes two, like vanilla's mixing recipes.

  dye + white dye          -> one value step brighter   (same hue, same saturation)
  dye + black dye          -> one value step darker
  standard dye + gray dye  -> the muted version
  two dyes 2 hues apart    -> the hue between them      (same saturation and value)
  bridge: where no vanilla dye sits close enough for that (Lime -> Cyan has two hues between them),
          the neighbor on one side + the dye two hues away on the other side makes the hue (Lime + Cyan -> Green)

A recipe whose ingredients match a vanilla shapeless recipe is dropped (vanilla wins: red + white stays pink).
Every color must be reachable from the 16 vanilla dyes; `mixing_recipes` reports any that aren't.
"""
import json


def mixing_recipes(palette, vanilla):
    grid = {(c["hue"], c["sat"], c["value"]): c["id"] for c in palette["colors"]}
    hues = [h["id"] for h in palette["hues"]]
    values = palette["values"]
    sats = palette["sats"]
    vanilla_ids = [c["id"] for c in palette["key"]] + [c["id"] for c in palette["colors"] if c["vanilla"]]

    taken = vanilla_pairs(vanilla)
    recipes, seen = [], set()

    def add(a, b, result, kind):
        pair = frozenset((a, b))
        if pair in taken or pair in seen or a == b:
            return
        seen.add(pair)
        recipes.append({"a": a, "b": b, "result": result, "kind": kind})

    for hi, h in enumerate(hues):
        for s in sats:
            for vi, v in enumerate(values):
                me = grid[(h, s, v)]
                if vi > 0:
                    add(me, "white", grid[(h, s, values[vi - 1])], "brighter")
                if vi < len(values) - 1:
                    add(me, "black", grid[(h, s, values[vi + 1])], "darker")
                if s == "standard":
                    add(me, "gray", grid[(h, "muted", v)], "muted")
                left = grid[(hues[(hi - 1) % len(hues)], s, v)]
                right = grid[(hues[(hi + 1) % len(hues)], s, v)]
                add(left, right, me, "blend")

    def closure():
        reachable = set(vanilla_ids)
        changed = True
        while changed:
            changed = False
            for r in recipes:
                if r["a"] in reachable and r["b"] in reachable and r["result"] not in reachable:
                    reachable.add(r["result"])
                    changed = True
        return reachable

    # Bridges, only for hues the rules above can't reach.
    reachable = closure()
    for hi, h in enumerate(hues):
        for s in sats:
            for v in values:
                me = grid[(h, s, v)]
                if me in reachable:
                    continue
                near = grid[(hues[(hi - 1) % len(hues)], s, v)]
                far = grid[(hues[(hi + 2) % len(hues)], s, v)]
                add(near, far, me, "bridge")
    unreachable = sorted(set(grid.values()) - closure())
    return recipes, unreachable


def vanilla_pairs(vanilla):
    """Ingredient pairs of every two-ingredient vanilla shapeless recipe, as dye ids (namespace stripped)."""
    pairs = set()
    for f in vanilla.glob("data/minecraft/recipe", "*.json"):
        r = json.loads(vanilla.read_text(f))
        if r.get("type") != "minecraft:crafting_shapeless" or len(r.get("ingredients", [])) != 2:
            continue
        ids = []
        for ing in r["ingredients"]:
            i = ing.get("item") if isinstance(ing, dict) else ing
            if not isinstance(i, str) or not i.endswith("_dye"):
                break
            ids.append(i.split(":", 1)[1][: -len("_dye")])
        else:
            pairs.add(frozenset(ids))
    return pairs

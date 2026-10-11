# Sprocket Dyes — handoff

Notes for whoever picks this up next. **Feature-complete on all 20 version/loader nodes**; server self-test
passes everywhere (see *Status*).

## What the mod is

132 dyes: vanilla's 4 Key dyes (white, light gray, gray, black) + a 16-hue × 2-saturation (standard,
muted) × 4-value (bright, medium, deep, dark) grid. The 12 colored vanilla dyes sit on grid points, so
**116 colors are new**. They are real `DyeColor` enum entries with full vanilla parity, plus:

- every vanilla colored block for each new color (wool, carpet, terracotta, concrete, powder, glazed
  terracotta, stained glass + pane, bed, banner, shulker box, candle + cake) and bundles (1.21.2+),
  harnesses (1.21.6+), cushions (26.3)
- slab/stairs/wall of wool, terracotta, glazed terracotta, concrete, stained glass for **all 132 colors**
  (vanilla's included) and for undyed terracotta and glass — except the wool/concrete slabs & stairs vanilla
  itself has from 26.3
- a mixing grid of 397 two-dye recipes (every color reachable from vanilla dyes), random new dyes from the
  Wandering Trader, a "Hue · Saturation · Value" tooltip on every colored item
- glazed terracotta slabs/walls face like the full block (`facing` property, `GlazedShapes`); stairs follow their
  own facing. Their models are cut from the full block's texture mapping (`datagen/glazed.py`), no uvlock
- the creative "Colored Blocks" tab split into collapsible sections (Key, 16 hues, Other)

Display names are evocative ("Eucalyptus"); registry ids are systematic and stable (`muted_deep_teal`).

## Layout & toolchain

- **Stonecutter** multiloader (template: the official `stonecutter-template-multiloader`, same as
  `../BetweenMod`): one shared `src/`, nodes `versions/<mc>-<loader>`, `build.fabric.gradle.kts`
  (Fabric Loom via `loom-back-compat`) / `build.neoforge.gradle.kts` (ModDevGradle). Per-node versions in
  `stonecutter.properties.toml`. **Active node is `26.3-fabric`**: `src/` is written in 26.3 dialect;
  inactive branches of `//? if` blocks must be commented out in `src/`.
- 10 MC nodes × 2 loaders: 1.21.1 (1.21–1.21.1), 1.21.3 (1.21.2–3), 1.21.4, 1.21.5, 1.21.8 (1.21.6–8),
  1.21.10 (1.21.9–10), 1.21.11, 26.1.2 (26.1.x), 26.2, 26.3. Java 21 below 26.1, 25 from 26.1.
- **Architectury API** is a runtime dependency (registries, events, networking, trades). Not Architectury Loom.
- **Python 3 (stdlib only)** is required to build: each node's `generateData` task runs
  `datagen/generate.py data <mc> <dir>` before `processResources`. It reads vanilla templates from
  `~/.gradle/caches/fabric-loom/<mc>/minecraft-client.jar`, which exists once Gradle has configured the
  Fabric nodes (any `./gradlew` invocation does).

| Path | What |
|---|---|
| `palette/design_palette.py` → `palette/colors.json` | The palette (single source of truth). Edit CONFIG in the script, not the JSON. |
| `palette/generate_code.py` | `colors.json` → `SprocketPalette.java`, `NewDyeColorDefinitions.java`, dye lang keys. |
| `palette/render_preview.py` | Palette review page (published artifact). |
| `datagen/icon.py` | Renders `assets/sprocket_dyes/icon.png` (16 hue dyes, 4x4) - committed output. |
| `datagen/glazed.py` | Glazed shape models/blockstates (slices of the full block, per FaceBakery's UV rotation rules). |
| `datagen/generate.py` | Per-version data (recipes, models, blockstates, loot, tags, equipment, sheep loot) **by retargeting vanilla's own magenta files** for that version; stone-brick family as template for shapes vanilla lacks. `lang` subcommand rewrites the committed lang file. Not committed output. |
| `datagen/mixing.py` | Mixing-grid rules + reachability check. |
| `datagen/textures.py` | Recolors vanilla textures in OKLCH from each color's nearest vanilla color (glazed: assigned pattern; dyes: per-hue sprite). Only pixels that differ across vanilla's 16 variants are recolored (keeps bed wood, shulker heads, buckles). Output **is committed** in `src/main/resources/assets/{sprocket_dyes,minecraft}/textures`. |
| `src/main/java/.../mixin/` | See *Vanilla sites* below. `SprocketMixinPlugin` turns mixins off per version. |
| `src/main/java/.../registry/` | `ModBlocks`, `ModItems` (component copying), `ColorCollections` (26.2+ & switch sites), `ModMapDecorations`, `ModTrades`, `ModLoot`, `Aliases`, `Vanilla` (id lookups). |
| `src/main/java/.../client/` | Tooltip, `CreativeSections` (tab layout model), `SectionHeaderRenderer`. |
| `src/main/java/.../debug/` | `SelfTest` (server) and `ClientTest` (client) dev-only checks. |

## Commands

```bash
./gradlew :26.3-neoforge:buildAndCollect          # one jar -> build/libs/0.1.0/
./gradlew chiseledBuild                           # all 20 jars
scripts/selftest-all.sh [node ...]                # server self-test (fails on any data-load error too)
scripts/clienttest.sh [node ...]                  # client: missing models/textures check (opens a window)
scripts/clientshots.sh 26.3-neoforge              # creative tab screenshots + section toggle scroll check -> run/screenshots/ (needs that node's selftest world)
python3 palette/design_palette.py && python3 palette/generate_code.py   # after palette edits
python3 datagen/textures.py                       # after palette edits
python3 datagen/generate.py lang                  # after product/palette changes (all 10 versions)
python3 scripts/publish_modrinth.py sprocket-dyes [--publish]   # upload build/libs jars (dry run without --publish; token in ~/.config/modrinth/token)
```

Run Gradle invocations **one at a time** — overlapping runs collide on the server port and give false failures.

## How the core works

- **DyeColor extension** (`DyeColorMixin`): appends 116 constants right after `$VALUES` in `<clinit>`
  (before `BY_ID`/codecs are built); ids 16–131. Constructor signature differs from 26.2 (`terracottaColor`).
  `LEGACY_ID_CODEC` (1.21.5+) is replaced with an unsigned-byte version (ids > 127). Firework colors must be
  unique (`BY_FIREWORK_COLOR` uses `toMap`).
- **Vanilla 16-color sites** go through `ColorExtensions` (site → color → value, falling back to the
  color's vanilla source color): `ShulkerBoxBlock.getBlockByColor`, `BundleItem.getByColor` (≤1.21.11),
  `MapBanner.getDecoration`, 1.21.1 sheep loot switch, and 26.2+ `ColorCollection.pick` (keyed by the
  collection instance).
- Many things need **no code** because vanilla builds them from `DyeColor.values()` by name after our
  colors exist: bed/shulker sprite tables (1.21.4+), cushion textures, llama/harness equipment asset ids,
  sheep loot keys (1.21.2+). Their files live in the **`minecraft:` namespace** on purpose.
- **Sheep**: vanilla packs color into 4 bits. The full id is a plain field (`SheepMixin`), synced to clients
  by `SheepColorPayload` (Architectury typed packet) on change and on start-tracking. Do **not** add synced
  entity data — NeoForge rejects it in dev and warns in prod.
- **Items** copy their vanilla counterpart's components up to 1.21.11; from 26.1 components bind late, so
  `ModItems.copied()` spells out per-kind properties.
- **Creative tab** (all versions): `CreativeModeInventoryScreenMixin` replaces the Colored Blocks list
  after `selectTab`/`refreshCurrentTabContents` with `CreativeSections.apply()` (header row = 9 empty slots),
  toggles on header clicks (keeping the header in view; opening scrolls just enough to show the section), and
  `AbstractContainerScreenMixin` draws headers after the slots: `extractContents` TAIL (26.1+),
  `renderContents` TAIL (1.21.6-1.21.11), before `renderLabels` in `render` (<=1.21.5, pose is translated).
  Left click is `InputConstants.MOUSE_BUTTON_LEFT` (0, but 1 on 26.3). Only the visible list changes; tab
  contents/search/JEI are untouched.
- **26.3 aliases** (`Aliases`): old `sprocket_dyes:<vanilla color>_{wool,concrete}_{slab,stairs}` →
  vanilla ids. NeoForge `addAlias` for blocks and items; Fabric `MappedRegistryMixin` resolves them in the same
  `MappedRegistry` lookups NeoForge patches.

## Status

2026-10-11, all 20 nodes: server self-test **PASS** (incl. glazed shape facings, aliases on both 26.3
loaders) and client asset test **PASS**. Creative tab screenshots + section toggle scroll check pass on
1.21.1-fabric, 1.21.5-neoforge, 1.21.8-fabric, 26.1.2-neoforge, 26.3-neoforge (one per draw hook / click
signature). In-game checks by the owner on 26.3 NeoForge: glazed shape facing, toggle scroll.
The 1.21.8 node also covers 1.21.6-7; its `renderContents` hook was only checked against the 1.21.8 jar.

License: LGPL-3.0-only (`COPYING`, `COPYING.LESSER`, both bundled into jars). Icon: `datagen/icon.py`.

## Remaining work

1. Check Sodium/other popular mods (a Sodium alpha crashed on NeoForge 26.3.0.65-beta on its own - not ours).
2. **Deliberately not done**: shepherd villagers selling beds/banners in new colors (would crowd vanilla's
   trade pools), colored shulker mobs.

## Gotchas learned the hard way

- Exhaustive `switch` over `DyeColor` throws `MatchException` for new colors — every such site needs a guard.
- `Sheep` has a static `getColor(DyeColor)` before 1.21.5 — target `getColor()Lnet/minecraft/world/item/DyeColor;`.
- `Properties.ofFullCopy` copies `drops`/`descriptionId`; fine for id-derived defaults, but wall banners
  must `overrideLootTable`/`overrideDescription` (≤1.21.1: `dropsLike`).
- `ServerChunkCache.broadcast` became `sendToTrackingPlayers` in 1.21.9.
- 26.3 loot functions use `"type"`, 26.1–26.2 villager trades use `given_item_modifiers` (list).
- NeoForge 1.21.x dev runs don't start MixinExtras (breaks Architectury's `@Local`) — `SprocketMixinPlugin`
  bootstraps it; the NeoForge build adds `mixinextras-neoforge` to the dev classpath.
- Data-gen: `\b` treats `_` as a word char; template tokens need `(?<![a-z0-9])` lookarounds. Some vanilla
  models use namespace-less paths (`item/x`).
- Vanilla's re-dye recipes list "every other color" explicitly; the generator rebuilds those lists for 132
  colors and overrides vanilla's 16 re-dye recipes so they accept our items too.
- Before 1.21.5 `WallBlock` caches shapes in a map keyed by states built from `defaultBlockState()`, so a wall
  with an extra property (glazed `facing`) must look shapes up with that property at its default value.
- First client launch shows the accessibility onboarding screen, which blocks quick play
  (`clientshots.sh` sets `onboardAccessibility:false`).

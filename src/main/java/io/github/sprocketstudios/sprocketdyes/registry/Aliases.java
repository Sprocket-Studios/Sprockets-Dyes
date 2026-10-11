package io.github.sprocketstudios.sprocketdyes.registry;

import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import io.github.sprocketstudios.sprocketdyes.palette.SprocketPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Before 26.3 this mod adds wool and concrete slabs/stairs in vanilla's 16 colors
 * ({@code sprocket_dyes:red_wool_slab}). 26.3 added those blocks itself, so there the mod stops registering
 * them and points the old ids at vanilla's, keeping them in worlds upgraded from earlier versions.
 */
public final class Aliases {
    private Aliases() {}

    /** (our old id, vanilla id) pairs that apply on this version. */
    public static List<Identifier[]> pairs() {
        List<Identifier[]> pairs = new ArrayList<>();
        //? if >=26.3 {
        for (SprocketPalette.Entry entry : SprocketPalette.ALL) {
            if (!entry.vanilla()) continue;
            for (String base : List.of("wool", "concrete")) {
                for (String shape : List.of("slab", "stairs")) {
                    String path = entry.id() + "_" + base + "_" + shape;
                    pairs.add(new Identifier[]{SprocketDyes.id(path), Identifier.withDefaultNamespace(path)});
                }
            }
        }
        //?}
        return pairs;
    }

    public static void init() {
        //? if neoforge {
        /*for (Identifier[] pair : pairs()) {
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.addAlias(pair[0], pair[1]);
            net.minecraft.core.registries.BuiltInRegistries.ITEM.addAlias(pair[0], pair[1]);
        }
        *///?}
        // Fabric: no registry alias API; see HANDOFF.md.
    }
}

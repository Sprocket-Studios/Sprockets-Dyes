package io.github.sprocketstudios.sprocketdyes.registry;

import io.github.sprocketstudios.sprocketdyes.color.ColorExtensions;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.loot.LootTable;
//? if >=26.2 {
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
//?}

/**
 * Tells vanilla's hard-coded 16-color sites about our colors (see {@link ColorExtensions}):
 * static switches (shulker box block, bundle item, sheep loot before 1.21.2) and, from 26.2, every
 * {@code ColorCollection} vanilla resolves with {@code pick(color)}.
 */
public final class ColorCollections {
    private ColorCollections() {}

    static void extend() {
        for (DyeColor color : SprocketColors.newColors()) {
            var kinds = ModBlocks.COLORED.get(color);
            String c = color.getName();
            ResourceKey<LootTable> sheepLoot = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("entities/sheep/" + c));

            ColorExtensions.register(ColorExtensions.Site.SHULKER_BOX_BLOCK, color, kinds.get(ModBlocks.Kind.SHULKER_BOX));
            if (ModItems.BUNDLES.containsKey(color)) {
                ColorExtensions.register(ColorExtensions.Site.BUNDLE_ITEM, color, ModItems.BUNDLES.get(color));
            }
            ColorExtensions.register(ColorExtensions.Site.SHEEP_LOOT_TABLE, color, () -> sheepLoot);

            //? if >=26.2 {
            both(Blocks.WOOL, Items.WOOL, kinds.get(ModBlocks.Kind.WOOL), c + "_wool", color);
            both(Blocks.CARPET, Items.CARPET, kinds.get(ModBlocks.Kind.CARPET), c + "_carpet", color);
            both(Blocks.DYED_TERRACOTTA, Items.DYED_TERRACOTTA, kinds.get(ModBlocks.Kind.TERRACOTTA), c + "_terracotta", color);
            both(Blocks.GLAZED_TERRACOTTA, Items.GLAZED_TERRACOTTA, kinds.get(ModBlocks.Kind.GLAZED_TERRACOTTA), c + "_glazed_terracotta", color);
            both(Blocks.CONCRETE, Items.CONCRETE, kinds.get(ModBlocks.Kind.CONCRETE), c + "_concrete", color);
            both(Blocks.CONCRETE_POWDER, Items.CONCRETE_POWDER, kinds.get(ModBlocks.Kind.CONCRETE_POWDER), c + "_concrete_powder", color);
            both(Blocks.STAINED_GLASS, Items.STAINED_GLASS, kinds.get(ModBlocks.Kind.STAINED_GLASS), c + "_stained_glass", color);
            both(Blocks.STAINED_GLASS_PANE, Items.STAINED_GLASS_PANE, kinds.get(ModBlocks.Kind.STAINED_GLASS_PANE), c + "_stained_glass_pane", color);
            both(Blocks.BED, Items.BED, kinds.get(ModBlocks.Kind.BED), c + "_bed", color);
            both(Blocks.BANNER, Items.BANNER, kinds.get(ModBlocks.Kind.BANNER), c + "_banner", color);
            ColorExtensions.register(Blocks.WALL_BANNER, color, kinds.get(ModBlocks.Kind.WALL_BANNER));
            both(Blocks.DYED_SHULKER_BOX, Items.DYED_SHULKER_BOX, kinds.get(ModBlocks.Kind.SHULKER_BOX), c + "_shulker_box", color);
            both(Blocks.DYED_CANDLE, Items.DYED_CANDLE, kinds.get(ModBlocks.Kind.CANDLE), c + "_candle", color);
            ColorExtensions.register(Blocks.DYED_CANDLE_CAKE, color, kinds.get(ModBlocks.Kind.CANDLE_CAKE));
            ColorExtensions.register(Items.DYE, color, ModItems.DYES.get(color));
            ColorExtensions.register(Items.DYED_BUNDLE, color, ModItems.BUNDLES.get(color));
            ColorExtensions.register(Items.HARNESS, color, ModItems.HARNESSES.get(color));
            ColorExtensions.register(BuiltInLootTables.SHEEP, color, () -> sheepLoot);
            //? if >=26.3 {
            ColorExtensions.register(Items.CUSHION, color, ModItems.CUSHIONS.get(color));
            var wool = ModBlocks.SHAPES.get(c + "_wool");
            var concrete = ModBlocks.SHAPES.get(c + "_concrete");
            both(Blocks.WOOL_SLAB, Items.WOOL_SLAB, wool.get(ModBlocks.Shape.SLAB), c + "_wool_slab", color);
            both(Blocks.WOOL_STAIRS, Items.WOOL_STAIRS, wool.get(ModBlocks.Shape.STAIRS), c + "_wool_stairs", color);
            both(Blocks.CONCRETE_SLAB, Items.CONCRETE_SLAB, concrete.get(ModBlocks.Shape.SLAB), c + "_concrete_slab", color);
            both(Blocks.CONCRETE_STAIRS, Items.CONCRETE_STAIRS, concrete.get(ModBlocks.Shape.STAIRS), c + "_concrete_stairs", color);
            //?}
            //?}
        }
    }

    //? if >=26.2 {
    private static void both(Object blocks, Object items, java.util.function.Supplier<?> block, String name, DyeColor color) {
        ColorExtensions.register(blocks, color, block);
        ColorExtensions.register(items, color, ModBlocks.ITEM_BY_NAME.get(name));
    }
    //?}
}

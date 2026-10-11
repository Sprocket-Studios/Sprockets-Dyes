package io.github.sprocketstudios.sprocketdyes.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import io.github.sprocketstudios.sprocketdyes.block.GlassShapes;
import io.github.sprocketstudios.sprocketdyes.block.GlazedShapes;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Colored building blocks.
 *
 * <p>Every new color gets the 8 vanilla colored blocks ({@link Kind}). Every color - vanilla's 16 included -
 * plus undyed terracotta and glass gets slab, stairs and wall shapes ({@link Shape}) of wool, terracotta,
 * glazed terracotta, concrete and stained glass, except the ones vanilla itself has (wool and concrete
 * slabs/stairs for its 16 colors from 26.3).
 *
 * <p>Properties are copied from the vanilla block of the color's source color, so strength, sounds, tools,
 * flammability etc. always match the running version.
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(SprocketDyes.MOD_ID, Registries.BLOCK);

    /** The vanilla colored blocks, recreated for every new color. */
    public enum Kind {
        WOOL("wool"),
        CARPET("carpet"),
        TERRACOTTA("terracotta"),
        CONCRETE("concrete"),
        CONCRETE_POWDER("concrete_powder"),
        GLAZED_TERRACOTTA("glazed_terracotta"),
        STAINED_GLASS("stained_glass"),
        STAINED_GLASS_PANE("stained_glass_pane"),
        BED("bed"),
        BANNER("banner"),
        WALL_BANNER("wall_banner"),
        SHULKER_BOX("shulker_box"),
        CANDLE("candle"),
        CANDLE_CAKE("candle_cake");

        public final String suffix;

        Kind(String suffix) {
            this.suffix = suffix;
        }
    }

    /** Blocks that get slab/stairs/wall shapes. */
    public enum ShapeBase {
        WOOL("wool", null),
        TERRACOTTA("terracotta", "terracotta"),
        GLAZED_TERRACOTTA("glazed_terracotta", null),
        CONCRETE("concrete", null),
        STAINED_GLASS("stained_glass", "glass");

        public final String suffix;
        /** Id of the undyed block that also gets shapes, or null. */
        public final String undyed;

        ShapeBase(String suffix, String undyed) {
            this.suffix = suffix;
            this.undyed = undyed;
        }

        public boolean isGlass() {
            return this == STAINED_GLASS;
        }

        /** Whether vanilla has this shape for its own 16 colors on the running version. */
        boolean vanillaHas(Shape shape) {
            //? if >=26.3 {
            return (this == WOOL || this == CONCRETE) && shape != Shape.WALL;
            //?} else {
            /*return false;
            *///?}
        }
    }

    public enum Shape {
        SLAB("slab"),
        STAIRS("stairs"),
        WALL("wall");

        public final String suffix;

        Shape(String suffix) {
            this.suffix = suffix;
        }
    }

    public static final Map<DyeColor, Map<Kind, RegistrySupplier<Block>>> COLORED = new LinkedHashMap<>();
    /** Shapes by base block id (e.g. "red_wool", "muted_deep_teal_concrete", "glass") then shape. */
    public static final Map<String, Map<Shape, RegistrySupplier<Block>>> SHAPES = new LinkedHashMap<>();
    /** Every block item we register, in registration order. */
    public static final List<RegistrySupplier<Item>> BLOCK_ITEMS = new ArrayList<>();
    /** Block items by registry path. */
    public static final Map<String, RegistrySupplier<Item>> ITEM_BY_NAME = new LinkedHashMap<>();
    /** Glass-like blocks that need the translucent render layer before 26.1. */
    public static final List<RegistrySupplier<Block>> TRANSLUCENT = new ArrayList<>();

    static {
        for (DyeColor color : SprocketColors.newColors()) {
            Map<Kind, RegistrySupplier<Block>> kinds = new EnumMap<>(Kind.class);
            COLORED.put(color, kinds);
            String c = color.getName();
            String src = SprocketColors.entry(color).source();

            kinds.put(Kind.WOOL, colored(color, Kind.WOOL, Block::new, copy(src + "_wool").mapColor(color.getMapColor())));
            kinds.put(Kind.CARPET, colored(color, Kind.CARPET, p -> new WoolCarpetBlock(color, p), copy(src + "_carpet").mapColor(color.getMapColor())));
            kinds.put(Kind.TERRACOTTA, colored(color, Kind.TERRACOTTA, Block::new, copy(src + "_terracotta")));
            RegistrySupplier<Block> concrete = colored(color, Kind.CONCRETE, Block::new, copy(src + "_concrete").mapColor(color.getMapColor()));
            kinds.put(Kind.CONCRETE, concrete);
            kinds.put(Kind.CONCRETE_POWDER, colored(color, Kind.CONCRETE_POWDER, p -> new ConcretePowderBlock(concrete.get(), p), copy(src + "_concrete_powder").mapColor(color.getMapColor())));
            kinds.put(Kind.GLAZED_TERRACOTTA, colored(color, Kind.GLAZED_TERRACOTTA, GlazedTerracottaBlock::new, copy(src + "_glazed_terracotta").mapColor(color.getMapColor())));
            RegistrySupplier<Block> glass = colored(color, Kind.STAINED_GLASS, p -> new StainedGlassBlock(color, p), copy(src + "_stained_glass").mapColor(color.getMapColor()));
            kinds.put(Kind.STAINED_GLASS, glass);
            TRANSLUCENT.add(glass);
            RegistrySupplier<Block> pane = colored(color, Kind.STAINED_GLASS_PANE, p -> new StainedGlassPaneBlock(color, p), copy(src + "_stained_glass_pane"));
            kinds.put(Kind.STAINED_GLASS_PANE, pane);
            TRANSLUCENT.add(pane);

            //? if >=26.3 {
            kinds.put(Kind.BED, block(c + "_bed", p -> new BedBlock(color, p), copy(src + "_bed"), src + "_bed", color, BlockItem::new));
            //?} else {
            /*kinds.put(Kind.BED, block(c + "_bed", p -> new BedBlock(color, p), copy(src + "_bed"), src + "_bed", color, net.minecraft.world.item.BedItem::new));
            *///?}
            RegistrySupplier<Block> banner = blockNoItem(c + "_banner", p -> new BannerBlock(color, p), copy(src + "_banner"));
            RegistrySupplier<Block> wallBanner = blockNoItem(c + "_wall_banner", p -> new WallBannerBlock(color, p), wallVariant(src + "_wall_banner", banner));
            item(c + "_banner", (b, p) -> new BannerItem(b, wallBanner.get(), p), banner, src + "_banner", color);
            kinds.put(Kind.BANNER, banner);
            kinds.put(Kind.WALL_BANNER, wallBanner);
            kinds.put(Kind.SHULKER_BOX, block(c + "_shulker_box", p -> new ShulkerBoxBlock(color, p), copy(src + "_shulker_box"), src + "_shulker_box", color, BlockItem::new));
            RegistrySupplier<Block> candle = colored(color, Kind.CANDLE, CandleBlock::new, copy(src + "_candle").mapColor(color.getMapColor()));
            kinds.put(Kind.CANDLE, candle);
            kinds.put(Kind.CANDLE_CAKE, blockNoItem(c + "_candle_cake", p -> new CandleCakeBlock(candle.get(), p), copy(src + "_candle_cake")));
        }

        for (DyeColor color : SprocketColors.displayOrder()) {
            boolean vanillaColor = !SprocketColors.isNew(color);
            for (ShapeBase base : ShapeBase.values()) {
                String baseId = color.getName() + "_" + base.suffix;
                Supplier<Block> baseBlock = vanillaColor ? () -> Vanilla.block(baseId) : COLORED.get(color).get(Kind.valueOf(base.name()));
                shapes(baseId, baseBlock, base, color, vanillaColor);
            }
        }
        for (ShapeBase base : ShapeBase.values()) {
            if (base.undyed != null) {
                shapes(base.undyed, () -> Vanilla.block(base.undyed), base, null, false);
            }
        }
    }

    private static void shapes(String baseId, Supplier<Block> baseBlock, ShapeBase base, DyeColor color, boolean vanillaColor) {
        Map<Shape, RegistrySupplier<Block>> shapes = new EnumMap<>(Shape.class);
        for (Shape shape : Shape.values()) {
            if (vanillaColor && base.vanillaHas(shape)) continue;
            String id = baseId + "_" + shape.suffix;
            RegistrySupplier<Block> block = block(id, p -> shape(shape, base, baseBlock.get(), color, p), copyOf(baseBlock, shape));
            shapes.put(shape, block);
            if (base.isGlass()) TRANSLUCENT.add(block);
        }
        SHAPES.put(baseId, shapes);
    }

    private static Block shape(Shape shape, ShapeBase base, Block baseBlock, DyeColor color, BlockBehaviour.Properties p) {
        if (base.isGlass()) {
            return switch (shape) {
                case SLAB -> new GlassShapes.Slab(baseBlock, color, p);
                case STAIRS -> new GlassShapes.Stairs(baseBlock, color, p);
                case WALL -> new GlassShapes.Wall(baseBlock, color, p);
            };
        }
        if (base == ShapeBase.GLAZED_TERRACOTTA) {
            return switch (shape) {
                case SLAB -> new GlazedShapes.Slab(p);
                case STAIRS -> new StairBlock(baseBlock.defaultBlockState(), p);
                case WALL -> new GlazedShapes.Wall(p);
            };
        }
        return switch (shape) {
            case SLAB -> new SlabBlock(p);
            case STAIRS -> new StairBlock(baseBlock.defaultBlockState(), p);
            case WALL -> new WallBlock(p);
        };
    }

    /** Properties of a vanilla block, resolved lazily (registration suppliers run after vanilla bootstrap). */
    private static LazyProps copy(String vanillaId) {
        return () -> BlockBehaviour.Properties.ofFullCopy(Vanilla.block(vanillaId));
    }

    private static LazyProps copyOf(Supplier<Block> base, Shape shape) {
        return () -> {
            BlockBehaviour.Properties p = BlockBehaviour.Properties.ofFullCopy(base.get());
            // Vanilla walls are forced solid so things can stand on their posts.
            return shape == Shape.WALL ? p.forceSolidOn() : p;
        };
    }

    /** Fluent helper so {@code copy(...).mapColor(...)} stays lazy. */
    private interface LazyProps extends Supplier<BlockBehaviour.Properties> {
        default LazyProps mapColor(net.minecraft.world.level.material.MapColor color) {
            return () -> get().mapColor(color);
        }
    }

    /**
     * A wall variant (wall banner) shares its standing block's loot table and name, as vanilla's do.
     * {@code ofFullCopy} would otherwise carry over the vanilla source's.
     */
    private static LazyProps wallVariant(String vanillaWall, Supplier<Block> standing) {
        return () -> {
            BlockBehaviour.Properties p = BlockBehaviour.Properties.ofFullCopy(Vanilla.block(vanillaWall));
            //? if >=1.21.2 {
            return p.overrideLootTable(standing.get().getLootTable()).overrideDescription(standing.get().getDescriptionId());
            //?} else {
            /*return p.dropsLike(standing.get());
            *///?}
        };
    }

    @FunctionalInterface
    private interface ItemFactory {
        Item create(Block block, Item.Properties properties);
    }

    /** A new-color block of a vanilla kind, with a plain block item copying the vanilla item's components. */
    private static <T extends Block> RegistrySupplier<Block> colored(DyeColor color, Kind kind, Function<BlockBehaviour.Properties, T> factory, LazyProps properties) {
        String src = SprocketColors.entry(color).source();
        return block(color.getName() + "_" + kind.suffix, factory, properties, src + "_" + kind.suffix, color, BlockItem::new);
    }

    private static <T extends Block> RegistrySupplier<Block> block(String name, Function<BlockBehaviour.Properties, T> factory, LazyProps properties) {
        return block(name, factory, properties, null, null, BlockItem::new);
    }

    private static <T extends Block> RegistrySupplier<Block> block(String name, Function<BlockBehaviour.Properties, T> factory, LazyProps properties,
                                                                   String vanillaItem, DyeColor color, ItemFactory itemFactory) {
        RegistrySupplier<Block> block = blockNoItem(name, factory, properties);
        item(name, itemFactory, block, vanillaItem, color);
        return block;
    }

    private static <T extends Block> RegistrySupplier<Block> blockNoItem(String name, Function<BlockBehaviour.Properties, T> factory, LazyProps properties) {
        return BLOCKS.register(name, () -> factory.apply(withId(name, properties.get())));
    }

    private static void item(String name, ItemFactory factory, Supplier<Block> block, String vanillaItem, DyeColor color) {
        RegistrySupplier<Item> item = ModItems.registerLazy(name, p -> factory.create(block.get(), p),
            () -> vanillaItem == null ? blockItemProperties() : ModItems.copied(blockItemProperties(), vanillaItem, color));
        BLOCK_ITEMS.add(item);
        ITEM_BY_NAME.put(name, item);
    }

    static BlockBehaviour.Properties withId(String name, BlockBehaviour.Properties properties) {
        //? if >=1.21.2 {
        return properties.setId(ResourceKey.create(Registries.BLOCK, SprocketDyes.id(name)));
        //?} else {
        /*return properties;
        *///?}
    }

    private static Item.Properties blockItemProperties() {
        //? if >=1.21.2 {
        return new Item.Properties().useBlockDescriptionPrefix();
        //?} else {
        /*return new Item.Properties();
        *///?}
    }

    public static void init() {
        BLOCKS.register();
        ColorCollections.extend();
    }
}

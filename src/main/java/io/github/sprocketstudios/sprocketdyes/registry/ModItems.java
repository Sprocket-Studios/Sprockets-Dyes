package io.github.sprocketstudios.sprocketdyes.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
//? if >=26.1 {
import net.minecraft.core.component.DataComponents;
//?}

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(SprocketDyes.MOD_ID, Registries.ITEM);

    /** One dye item per new color, keyed by color, in id order. */
    public static final Map<DyeColor, RegistrySupplier<Item>> DYES = new LinkedHashMap<>();
    /** Colored bundles (1.21.2+), harnesses (1.21.6+) and cushions (26.3+) for the new colors. */
    public static final Map<DyeColor, RegistrySupplier<Item>> BUNDLES = new LinkedHashMap<>();
    public static final Map<DyeColor, RegistrySupplier<Item>> HARNESSES = new LinkedHashMap<>();
    public static final Map<DyeColor, RegistrySupplier<Item>> CUSHIONS = new LinkedHashMap<>();

    static {
        for (DyeColor color : SprocketColors.newColors()) {
            String c = color.getName();
            String src = SprocketColors.entry(color).source();
            //? if >=26.1 {
            DYES.put(color, register(c + "_dye", DyeItem::new, new Item.Properties().component(DataComponents.DYE, color)));
            //?} else {
            /*DYES.put(color, register(c + "_dye", props -> new DyeItem(color, props), new Item.Properties()));
            *///?}
            //? if >=1.21.4 {
            BUNDLES.put(color, registerLazy(c + "_bundle", net.minecraft.world.item.BundleItem::new, () -> copied(new Item.Properties(), src + "_bundle", color)));
            //?} else if >=1.21.2 {
            /*BUNDLES.put(color, registerLazy(c + "_bundle",
                p -> new net.minecraft.world.item.BundleItem(SprocketDyes.id(c + "_bundle_open_front"), SprocketDyes.id(c + "_bundle_open_back"), p),
                () -> copied(new Item.Properties(), src + "_bundle", color)));
            *///?}
            //? if >=1.21.6 {
            HARNESSES.put(color, registerLazy(c + "_harness", Item::new, () -> copied(new Item.Properties(), src + "_harness", color)));
            //?}
            //? if >=26.3 {
            CUSHIONS.put(color, registerLazy(c + "_cushion", net.minecraft.world.item.CushionItem::new, () -> copied(new Item.Properties(), src + "_cushion", color)));
            //?}
        }
    }

    /** Components that identify the item or its color; everything else is copied from the vanilla counterpart. */
    private static final Set<String> NOT_COPIED = Set.of(
        "minecraft:item_name", "minecraft:item_model", "minecraft:equippable", "minecraft:cushion/color", "minecraft:dye"
    );

    /**
     * Gives {@code properties} what the vanilla counterpart has (stack size, container, banner patterns, fuel,
     * equippable, ...), with the color-bound parts made for {@code color}.
     *
     * <p>Up to 1.21.11 this copies the vanilla item's default components. From 26.1 components are only bound
     * after registries freeze, so the properties are spelled out per kind, mirroring vanilla's Items.
     */
    static Item.Properties copied(Item.Properties properties, String vanillaItem, DyeColor color) {
        //? if >=26.1 {
        String kind = vanillaItem.substring(vanillaItem.lastIndexOf('_') + 1);
        if (vanillaItem.endsWith("_shulker_box")) kind = "shulker_box";
        switch (kind) {
            case "carpet" -> properties.component(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.llamaSwag(color));
            case "bed" -> properties.stacksTo(1);
            case "banner" -> properties.stacksTo(16).component(DataComponents.BANNER_PATTERNS, net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY);
            case "shulker_box" -> properties.stacksTo(1).component(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
            case "bundle" -> properties.stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, net.minecraft.world.item.component.BundleContents.EMPTY);
            case "harness" -> properties.stacksTo(1).component(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.harness(color));
            default -> {}
        }
        //? if >=26.3 {
        switch (kind) {
            case "wool" -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOL);
            case "carpet" -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOL_CARPETS);
            case "banner" -> properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_BLOCKS);
            case "cushion" -> properties.stacksTo(16).component(DataComponents.CUSHION_COLOR, color)
                .cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOL_SLABS);
            default -> {}
        }
        //?}
        return properties;
        //?} else {
        /*for (TypedDataComponent<?> component : Vanilla.item(vanillaItem).components()) {
            if (!NOT_COPIED.contains(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type())))) {
                copyOne(properties, component);
            }
        }
        //? if >=1.21.2 {
        if (vanillaItem.endsWith("_carpet")) {
            properties.component(net.minecraft.core.component.DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.llamaSwag(color));
        }
        //?}
        //? if >=1.21.6 {
        if (vanillaItem.endsWith("_harness")) {
            properties.component(net.minecraft.core.component.DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.harness(color));
        }
        //?}
        return properties;
        *///?}
    }

    private static <T> void copyOne(Item.Properties properties, TypedDataComponent<T> component) {
        properties.component(component.type(), component.value());
    }

    /** Registers an item, applying the registry id to its properties where the version requires it (1.21.2+). */
    public static <T extends Item> RegistrySupplier<T> register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        return ITEMS.register(name, () -> factory.apply(withId(name, properties)));
    }

    /** Like {@link #register}, but builds the properties at registration time (vanilla items exist by then). */
    public static <T extends Item> RegistrySupplier<T> registerLazy(String name, Function<Item.Properties, T> factory, Supplier<Item.Properties> properties) {
        return ITEMS.register(name, () -> factory.apply(withId(name, properties.get())));
    }

    static Item.Properties withId(String name, Item.Properties properties) {
        //? if >=1.21.2 {
        return properties.setId(ResourceKey.create(Registries.ITEM, SprocketDyes.id(name)));
        //?} else {
        /*return properties;
        *///?}
    }

    /** Call after {@link ModBlocks#init()} so block items are queued before the item register fires. */
    public static void init() {
        ITEMS.register();
        CreativeTabRegistry.append(CreativeModeTabs.INGREDIENTS, DYES.values().toArray(RegistrySupplier[]::new));
        CreativeTabRegistry.append(CreativeModeTabs.COLORED_BLOCKS, ModBlocks.BLOCK_ITEMS.toArray(RegistrySupplier[]::new));
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, BUNDLES.values().toArray(RegistrySupplier[]::new));
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, HARNESSES.values().toArray(RegistrySupplier[]::new));
        // Vanilla keeps cushions with the colored blocks.
        CreativeTabRegistry.append(CreativeModeTabs.COLORED_BLOCKS, CUSHIONS.values().toArray(RegistrySupplier[]::new));
    }

    /** The dye color an item represents as a dye, or null. */
    public static DyeColor dyeColorOf(Item item) {
        //? if >=26.1 {
        return item.components().get(DataComponents.DYE);
        //?} else {
        /*return item instanceof DyeItem dye ? dye.getDyeColor() : null;
        *///?}
    }
}

package io.github.sprocketstudios.sprocketdyes.debug;

import com.mojang.serialization.JsonOps;
import dev.architectury.event.events.common.LifecycleEvent;
import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import io.github.sprocketstudios.sprocketdyes.registry.ModBlocks;
import io.github.sprocketstudios.sprocketdyes.registry.ModItems;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import io.github.sprocketstudios.sprocketdyes.block.entity.BedHomes;
import io.github.sprocketstudios.sprocketdyes.registry.ModMapDecorations;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.5 {
import net.minecraft.world.entity.animal.sheep.Sheep;
//?} else {
/*import net.minecraft.world.entity.animal.Sheep;
*///?}
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.saveddata.maps.MapBanner;
//? if >=26.2 {
import net.minecraft.world.level.block.Blocks;
//?}

/**
 * Dev-only checks, enabled with {@code -Dsprocket_dyes.selftest=true}. Runs once the server has started,
 * logs every failure, then stops the JVM with exit code 0 (pass) or 1 (fail).
 */
public final class SelfTest {
    private SelfTest() {}

    public static final boolean ENABLED = Boolean.getBoolean("sprocket_dyes.selftest");

    public static void init() {
        if (ENABLED) LifecycleEvent.SERVER_STARTED.register(SelfTest::run);
    }

    private static void run(MinecraftServer server) {
        List<String> failures = new ArrayList<>();
        try {
            check(failures, DyeColor.values().length == 132, "DyeColor.values().length = " + DyeColor.values().length);
            check(failures, ModBlocks.COLORED.size() == 116, "colored block sets: " + ModBlocks.COLORED.size());
            for (var kinds : ModBlocks.COLORED.values()) {
                check(failures, kinds.size() == ModBlocks.Kind.values().length, "kinds per color: " + kinds.size());
                kinds.forEach((kind, block) -> check(failures, block.isPresent(), "block registered: " + block.getId()));
            }
            int shapes = ModBlocks.SHAPES.values().stream().mapToInt(java.util.Map::size).sum();
            // 132 colors x 5 bases x 3 shapes + 2 undyed bases x 3, minus vanilla's own wool/concrete slabs & stairs on 26.3
            //? if >=26.3 {
            int expectedShapes = 132 * 15 + 6 - 16 * 4;
            //?} else {
            /*int expectedShapes = 132 * 15 + 6;
            *///?}
            check(failures, shapes == expectedShapes, "shape blocks: " + shapes + " (expected " + expectedShapes + ")");
            for (var item : ModBlocks.BLOCK_ITEMS) check(failures, item.isPresent(), "block item registered: " + item.getId());
            phase5(failures);
            glazedShapes(failures);
            sheep(server, failures);
            aliases(server, failures);
            for (DyeColor color : DyeColor.values()) {
                check(failures, DyeColor.byId(color.getId()) == color, "byId round trip: " + color);
                check(failures, DyeColor.byName(color.getName(), null) == color, "byName round trip: " + color);
                var json = DyeColor.CODEC.encodeStart(JsonOps.INSTANCE, color).getOrThrow();
                check(failures, DyeColor.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow() == color, "codec round trip: " + color);
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                DyeColor.STREAM_CODEC.encode(buf, color);
                check(failures, DyeColor.STREAM_CODEC.decode(buf) == color, "stream codec round trip: " + color);
                check(failures, color == DyeColor.byFireworkColor(color.getFireworkColor()), "firework color lookup: " + color);
                //? if >=1.21.5 {
                var legacy = DyeColor.LEGACY_ID_CODEC.encodeStart(JsonOps.INSTANCE, color).getOrThrow();
                check(failures, DyeColor.LEGACY_ID_CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow() == color, "legacy id codec round trip: " + color);
                //?}
                check(failures, SprocketColors.entry(color) != null, "palette entry: " + color);
                if (SprocketColors.isNew(color)) {
                    var dye = ModItems.DYES.get(color);
                    check(failures, dye != null && dye.isPresent() && ModItems.dyeColorOf(dye.get()) == color, "dye item: " + color);
                }
            }
        } catch (Throwable t) {
            SprocketDyes.LOGGER.error("[selftest] crashed", t);
            failures.add("crash: " + t);
        }
        failures.forEach(f -> SprocketDyes.LOGGER.error("[selftest] FAIL {}", f));
        SprocketDyes.LOGGER.info("[selftest] {} on Minecraft {}: {}", failures.isEmpty() ? "PASSED" : "FAILED", SprocketDyes.MINECRAFT,
            failures.isEmpty() ? "all checks passed" : failures.size() + " failures");
        Runtime.getRuntime().halt(failures.isEmpty() ? 0 : 1);
    }

    /** Beds, banners, shulker boxes, candles, map markers, villager homes and vanilla's per-color lookups. */
    private static void phase5(List<String> failures) {
        var bannerType = blockEntityType("banner");
        var shulkerType = blockEntityType("shulker_box");
        var bedType = blockEntityType("bed");  // gone in 26.3: beds are plain blocks
        for (DyeColor color : SprocketColors.newColors()) {
            var kinds = ModBlocks.COLORED.get(color);
            Block bed = kinds.get(ModBlocks.Kind.BED).get();
            Block banner = kinds.get(ModBlocks.Kind.BANNER).get();
            Block wallBanner = kinds.get(ModBlocks.Kind.WALL_BANNER).get();
            Block shulker = kinds.get(ModBlocks.Kind.SHULKER_BOX).get();
            check(failures, bannerType.isValid(banner.defaultBlockState()) && bannerType.isValid(wallBanner.defaultBlockState()), "banner block entity accepts " + color);
            check(failures, shulkerType.isValid(shulker.defaultBlockState()), "shulker block entity accepts " + color);
            if (bedType != null) check(failures, bedType.isValid(bed.defaultBlockState()), "bed block entity accepts " + color);
            BlockState head = bed.defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD);
            check(failures, PoiTypes.forState(head).map(h -> h.is(PoiTypes.HOME)).orElse(false) && BedHomes.home().value().is(head), "villager home: " + color);
            var marker = new MapBanner(BlockPos.ZERO, color, Optional.empty()).getDecoration();
            check(failures, marker.value() == ModMapDecorations.BANNERS.get(color).get(), "map banner marker: " + color);
            //? if <26.1 {
            /*check(failures, ShulkerBoxBlock.getBlockByColor(color) == shulker, "ShulkerBoxBlock.getBlockByColor: " + color);
            *///?}
            //? if >=26.2 {
            check(failures, Blocks.BED.pick(color) == bed && Blocks.DYED_SHULKER_BOX.pick(color) == shulker && Blocks.WOOL.pick(color) == kinds.get(ModBlocks.Kind.WOOL).get(), "ColorCollection.pick: " + color);
            //?}
            check(failures, wallBanner.getLootTable().equals(banner.getLootTable()), "wall banner drops banner: " + color);
        }
    }

    /** Glazed slabs and walls face like the full block; every facing has the same shape. */
    private static void glazedShapes(List<String> failures) {
        var facing = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
        for (DyeColor color : DyeColor.values()) {
            var shapes = ModBlocks.SHAPES.get(color.getName() + "_glazed_terracotta");
            Block slab = shapes.get(ModBlocks.Shape.SLAB).get();
            Block wall = shapes.get(ModBlocks.Shape.WALL).get();
            check(failures, slab.defaultBlockState().hasProperty(facing) && wall.defaultBlockState().hasProperty(facing), "glazed shapes have facing: " + color);
            BlockState side = wall.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH_WALL, net.minecraft.world.level.block.state.properties.WallSide.TALL);
            var expected = side.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
            for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var shape = side.setValue(facing, dir).getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                var collision = side.setValue(facing, dir).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                check(failures, shape != null && collision != null && !collision.isEmpty() && shape.bounds().equals(expected), "glazed wall shape facing " + dir + ": " + color);
            }
        }
    }

    /** A sheep keeps every color (vanilla's byte only holds 16) and its sheared flag independently. */
    private static void sheep(MinecraftServer server, List<String> failures) {
        var id = net.minecraft.resources.Identifier.withDefaultNamespace("sheep");
        //? if >=1.21.2 {
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        var sheep = (Sheep) type.create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        //?} else {
        /*var type = BuiltInRegistries.ENTITY_TYPE.get(id);
        var sheep = (Sheep) type.create(server.overworld());
        *///?}
        for (DyeColor color : DyeColor.values()) {
            sheep.setColor(color);
            sheep.setSheared(true);
            check(failures, sheep.getColor() == color && sheep.isSheared(), "sheep color (sheared): " + color);
            sheep.setSheared(false);
            check(failures, sheep.getColor() == color && !sheep.isSheared(), "sheep color: " + color);
        }
    }

    /** Old ids resolve to vanilla's 26.3 blocks/items, including when read back from saved data. */
    private static void aliases(MinecraftServer server, List<String> failures) {
        //? if >=26.3 {
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        for (var pair : io.github.sprocketstudios.sprocketdyes.registry.Aliases.pairs()) {
            Block vanilla = BuiltInRegistries.BLOCK.getValue(pair[1]);
            check(failures, BuiltInRegistries.BLOCK.getValue(pair[0]) == vanilla, "block alias: " + pair[0]);
            check(failures, BuiltInRegistries.ITEM.getValue(pair[0]) == vanilla.asItem(), "item alias: " + pair[0]);
            // A saved block state keeps its properties (both shapes use vanilla's property names).
            // Encode a non-default vanilla state, swap in our old id, decode: what loading saved data does.
            BlockState sample = vanilla.defaultBlockState().getProperties().stream().findFirst()
                .map(p -> vanilla.defaultBlockState().cycle(p)).orElse(vanilla.defaultBlockState());
            String encoded = BlockState.CODEC.encodeStart(ops, sample).getOrThrow().toString();
            var stateJson = com.google.gson.JsonParser.parseString(encoded.replace(pair[1].toString(), pair[0].toString()));
            var parsed = BlockState.CODEC.parse(ops, stateJson);
            var state = parsed.result();
            check(failures, state.isPresent() && state.get().equals(sample) && !sample.equals(vanilla.defaultBlockState()),
                "saved block state alias: " + pair[0] + " -> " + state.map(Object::toString).orElseGet(() -> parsed.error().map(e -> e.message()).orElse("?")));
            var stack = ItemStack.CODEC.parse(ops, com.google.gson.JsonParser.parseString("{\"id\":\"" + pair[0] + "\",\"count\":3}")).result();
            check(failures, stack.isPresent() && stack.get().getItem() == vanilla.asItem() && stack.get().getCount() == 3, "saved item stack alias: " + pair[0]);
        }
        check(failures, io.github.sprocketstudios.sprocketdyes.registry.Aliases.pairs().size() == 64, "alias count");
        //?}
    }

    private static BlockEntityType<?> blockEntityType(String id) {
        var key = net.minecraft.resources.Identifier.withDefaultNamespace(id);
        if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(key)) return null;
        //? if >=1.21.2 {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(key);
        //?} else {
        /*return BuiltInRegistries.BLOCK_ENTITY_TYPE.get(key);
        *///?}
    }

    private static void check(List<String> failures, boolean ok, String what) {
        if (!ok) failures.add(what);
    }
}

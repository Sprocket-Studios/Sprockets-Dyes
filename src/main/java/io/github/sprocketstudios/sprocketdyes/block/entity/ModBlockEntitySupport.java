package io.github.sprocketstudios.sprocketdyes.block.entity;

import io.github.sprocketstudios.sprocketdyes.registry.ModBlocks;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Vanilla's bed, banner and shulker box block entity types only accept the blocks they were built with.
 * {@code BlockEntityTypeMixin} asks here whether one of our blocks belongs to a type.
 */
public final class ModBlockEntitySupport {
    private ModBlockEntitySupport() {}

    private static final Map<String, ModBlocks.Kind[]> KINDS_BY_TYPE = Map.of(
        "minecraft:bed", new ModBlocks.Kind[]{ModBlocks.Kind.BED},
        "minecraft:banner", new ModBlocks.Kind[]{ModBlocks.Kind.BANNER, ModBlocks.Kind.WALL_BANNER},
        "minecraft:shulker_box", new ModBlocks.Kind[]{ModBlocks.Kind.SHULKER_BOX}
    );

    private static volatile Map<BlockEntityType<?>, Set<Block>> cache;

    public static boolean supports(BlockEntityType<?> type, Block block) {
        Map<BlockEntityType<?>, Set<Block>> map = cache;
        if (map == null) {
            map = build();
            if (map == null) return false;  // our blocks aren't registered yet
            cache = map;
        }
        return map.getOrDefault(type, Collections.emptySet()).contains(block);
    }

    private static Map<BlockEntityType<?>, Set<Block>> build() {
        Map<BlockEntityType<?>, Set<Block>> map = new IdentityHashMap<>();
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            ModBlocks.Kind[] kinds = KINDS_BY_TYPE.get(String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)));
            if (kinds == null) continue;
            Set<Block> blocks = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var byKind : ModBlocks.COLORED.values()) {
                for (ModBlocks.Kind kind : kinds) {
                    var supplier = byKind.get(kind);
                    if (supplier == null || !supplier.isPresent()) return null;
                    blocks.add(supplier.get());
                }
            }
            map.put(type, blocks);
        }
        return map;
    }
}

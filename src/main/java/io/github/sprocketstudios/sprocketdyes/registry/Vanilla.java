package io.github.sprocketstudios.sprocketdyes.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;

/** Looks vanilla content up by id, which is stable across versions (field names like MAGENTA_WOOL are not). */
public final class Vanilla {
    private Vanilla() {}

    public static Block block(String path) {
        Identifier id = Identifier.withDefaultNamespace(path);
        //? if >=1.21.2 {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        //?} else {
        /*Block block = BuiltInRegistries.BLOCK.get(id);
        *///?}
        if (block == Blocks.AIR && !path.equals("air")) throw new IllegalArgumentException("No vanilla block minecraft:" + path);
        return block;
    }

    public static Item item(String path) {
        Identifier id = Identifier.withDefaultNamespace(path);
        //? if >=1.21.2 {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        //?} else {
        /*Item item = BuiltInRegistries.ITEM.get(id);
        *///?}
        if (item == Items.AIR && !path.equals("air")) throw new IllegalArgumentException("No vanilla item minecraft:" + path);
        return item;
    }

    public static boolean hasBlock(String path) {
        return BuiltInRegistries.BLOCK.containsKey(Identifier.withDefaultNamespace(path));
    }
}

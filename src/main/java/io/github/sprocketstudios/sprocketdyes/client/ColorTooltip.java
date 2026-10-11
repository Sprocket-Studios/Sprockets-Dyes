package io.github.sprocketstudios.sprocketdyes.client;

import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import io.github.sprocketstudios.sprocketdyes.palette.SprocketPalette;
import io.github.sprocketstudios.sprocketdyes.registry.ModItems;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Adds the systematic "Hue · Saturation · Value" line under every dye's name. */
final class ColorTooltip {
    private ColorTooltip() {}

    /** Every colored item (ours and vanilla's) -> its palette entry, built on first use. */
    private static Map<Item, SprocketPalette.Entry> byItem;

    private static final List<String> EXTRA_KINDS = List.of("bundle", "harness");

    static void append(ItemStack stack, List<Component> lines, Item.TooltipContext context, TooltipFlag flag) {
        SprocketPalette.Entry entry = entryOf(stack.getItem());
        if (entry == null) return;
        int at = Math.min(1, lines.size());
        lines.add(at, describe(entry).withStyle(ChatFormatting.GRAY));
    }

    private static SprocketPalette.Entry entryOf(Item item) {
        DyeColor dye = ModItems.dyeColorOf(item);
        if (dye != null) return SprocketColors.entry(dye);
        if (byItem == null) {
            Map<Item, SprocketPalette.Entry> map = new IdentityHashMap<>();
            for (SprocketPalette.Entry entry : SprocketPalette.ALL) {
                for (String kind : CreativeSections.KINDS) put(map, entry, kind);
                for (String kind : EXTRA_KINDS) put(map, entry, kind);
            }
            byItem = map;
        }
        return byItem.get(item);
    }

    private static void put(Map<Item, SprocketPalette.Entry> map, SprocketPalette.Entry entry, String kind) {
        Item item = CreativeSections.coloredItem(entry.id(), kind);
        if (item != null) map.putIfAbsent(item, entry);
    }

    static net.minecraft.network.chat.MutableComponent describe(SprocketPalette.Entry entry) {
        if (entry.isKey()) {
            return Component.translatable("sprocket_dyes.hue.key");
        }
        return Component.translatable(
            "sprocket_dyes.tooltip.position",
            Component.translatable("sprocket_dyes.hue." + SprocketPalette.HUES.get(entry.hue()).id()),
            Component.translatable("sprocket_dyes.sat." + SprocketPalette.SATURATIONS.get(entry.saturation())),
            Component.translatable("sprocket_dyes.value." + SprocketPalette.VALUES.get(entry.value()))
        );
    }
}

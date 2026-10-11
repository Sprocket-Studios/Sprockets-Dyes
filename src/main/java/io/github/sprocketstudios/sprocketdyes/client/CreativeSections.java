package io.github.sprocketstudios.sprocketdyes.client;

import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import io.github.sprocketstudios.sprocketdyes.palette.SprocketPalette;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The "Colored Blocks" creative tab, regrouped into collapsible sections: Key first, then one section per hue,
 * then anything else the tab holds (other mods' items). Each section starts with a header row - nine empty
 * slots the screen draws a bar over - followed, when open, by one row per block kind with the section's colors
 * in fixed columns (standard bright to dark, then muted bright to dark).
 *
 * <p>Only the screen's visible list is rearranged; the tab's real contents (search, JEI/EMI, recipe book) are
 * untouched.
 */
public final class CreativeSections {
    private CreativeSections() {}

    public static final int COLUMNS = 9;

    /** Block kinds in display order (vanilla's Colored Blocks order, with our shapes next to their base). */
    static final List<String> KINDS = List.of(
        "dye",
        "wool", "wool_stairs", "wool_slab", "wool_wall",
        "carpet",
        "terracotta", "terracotta_stairs", "terracotta_slab", "terracotta_wall",
        "concrete", "concrete_stairs", "concrete_slab", "concrete_wall",
        "concrete_powder",
        "glazed_terracotta", "glazed_terracotta_stairs", "glazed_terracotta_slab", "glazed_terracotta_wall",
        "stained_glass", "stained_glass_stairs", "stained_glass_slab", "stained_glass_wall",
        "stained_glass_pane",
        "shulker_box", "bed", "cushion", "candle", "banner"
    );

    /** Undyed blocks shown at the end of the Key section. */
    private static final List<String> UNDYED = List.of(
        "minecraft:terracotta", "sprocket_dyes:terracotta_stairs", "sprocket_dyes:terracotta_slab", "sprocket_dyes:terracotta_wall",
        "minecraft:glass", "sprocket_dyes:glass_stairs", "sprocket_dyes:glass_slab", "sprocket_dyes:glass_wall",
        "minecraft:tinted_glass", "minecraft:glass_pane", "minecraft:shulker_box", "minecraft:candle"
    );

    /** A collapsible group. {@code chips} are the swatch colors drawn on its header (RGB). */
    public record Section(String id, Component title, int[] chips, int itemCount) {}

    private static final Set<String> EXPANDED = new HashSet<>();
    /** Header section per row of the current layout, or null for content rows. */
    private static List<Section> rowHeaders = List.of();
    private static boolean active;

    public static boolean isActive() {
        return active;
    }

    public static void deactivate() {
        active = false;
        rowHeaders = List.of();
    }

    public static boolean isExpanded(Section section) {
        return EXPANDED.contains(section.id());
    }

    public static void setExpanded(String id, boolean expanded) {
        if (expanded) EXPANDED.add(id); else EXPANDED.remove(id);
    }

    public static void toggle(Section section) {
        if (!EXPANDED.remove(section.id())) EXPANDED.add(section.id());
    }

    /** The section whose header is at {@code row} of the current layout, or null. */
    public static Section headerAt(int row) {
        return row >= 0 && row < rowHeaders.size() ? rowHeaders.get(row) : null;
    }

    /**
     * Rearranges {@code items} (the tab's display items, as vanilla put them in the screen's list) into the
     * sectioned layout.
     */
    public static void apply(List<ItemStack> items) {
        List<ItemStack> tab = new ArrayList<>(items);
        Map<Item, ItemStack> byItem = new IdentityHashMap<>();
        for (ItemStack stack : tab) byItem.putIfAbsent(stack.getItem(), stack);
        Set<Item> claimed = Collections.newSetFromMap(new IdentityHashMap<>());

        List<ItemStack> out = new ArrayList<>();
        List<Section> headers = new ArrayList<>();

        // Key: the four neutrals, two kinds per row (4 + 4), then the undyed blocks.
        List<String> key = SprocketPalette.ALL.stream().filter(SprocketPalette.Entry::isKey).map(SprocketPalette.Entry::id).toList();
        List<ItemStack[]> keyRows = colorRows(key, byItem, claimed, 4);
        List<ItemStack> undyed = new ArrayList<>();
        for (String id : UNDYED) {
            Item item = item(id);
            if (item != null && byItem.containsKey(item) && claimed.add(item)) undyed.add(byItem.get(item));
        }
        List<ItemStack[]> keyLayout = pairUp(keyRows);
        keyLayout.addAll(pack(undyed));
        addSection(out, headers, new Section("key", Component.translatable("sprocket_dyes.hue.key"), chips(key), count(keyLayout)), keyLayout);

        // One section per hue, one row per kind.
        for (int h = 0; h < SprocketPalette.HUES.size(); h++) {
            int hue = h;
            List<String> colors = SprocketPalette.ALL.stream().filter(e -> e.hue() == hue).map(SprocketPalette.Entry::id).toList();
            List<ItemStack[]> rows = colorRows(colors, byItem, claimed, colors.size());
            SprocketPalette.Hue info = SprocketPalette.HUES.get(h);
            addSection(out, headers, new Section(info.id(), Component.translatable("sprocket_dyes.hue." + info.id()), chips(colors), count(rows)), rows);
        }

        // Whatever else is in the tab stays reachable.
        List<ItemStack> other = new ArrayList<>();
        for (ItemStack stack : tab) {
            if (!claimed.contains(stack.getItem())) other.add(stack);
        }
        if (!other.isEmpty()) {
            List<ItemStack[]> rows = pack(other);
            addSection(out, headers, new Section("other", Component.translatable("sprocket_dyes.section.other"), new int[0], count(rows)), rows);
        }

        items.clear();
        items.addAll(out);
        rowHeaders = headers;
        active = true;
    }

    private static void addSection(List<ItemStack> out, List<Section> headers, Section section, List<ItemStack[]> rows) {
        if (rows.isEmpty()) return;
        for (int i = 0; i < COLUMNS; i++) out.add(ItemStack.EMPTY);
        headers.add(section);
        if (!isExpanded(section)) return;
        for (ItemStack[] row : rows) {
            for (ItemStack stack : row) out.add(stack == null ? ItemStack.EMPTY : stack);
            headers.add(null);
        }
    }

    /** One row per kind that has at least one item for these colors; `width` slots used, rest left empty. */
    private static List<ItemStack[]> colorRows(List<String> colors, Map<Item, ItemStack> byItem, Set<Item> claimed, int width) {
        List<ItemStack[]> rows = new ArrayList<>();
        for (String kind : KINDS) {
            ItemStack[] row = new ItemStack[COLUMNS];
            boolean any = false;
            for (int i = 0; i < colors.size() && i < width; i++) {
                Item item = coloredItem(colors.get(i), kind);
                if (item == null) continue;
                ItemStack stack = byItem.get(item);
                if (stack == null && kind.equals("dye")) stack = new ItemStack(item);  // dyes live in Ingredients
                if (stack == null) continue;
                claimed.add(item);
                row[i] = stack;
                any = true;
            }
            if (any) rows.add(row);
        }
        return rows;
    }

    /** Key rows hold four colors; put two kinds side by side (slots 0-3 and 4-7). */
    private static List<ItemStack[]> pairUp(List<ItemStack[]> rows) {
        List<ItemStack[]> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += 2) {
            ItemStack[] row = new ItemStack[COLUMNS];
            System.arraycopy(rows.get(i), 0, row, 0, 4);
            if (i + 1 < rows.size()) System.arraycopy(rows.get(i + 1), 0, row, 4, 4);
            out.add(row);
        }
        return out;
    }

    private static List<ItemStack[]> pack(List<ItemStack> stacks) {
        List<ItemStack[]> out = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i += COLUMNS) {
            ItemStack[] row = new ItemStack[COLUMNS];
            for (int j = 0; j < COLUMNS && i + j < stacks.size(); j++) row[j] = stacks.get(i + j);
            out.add(row);
        }
        return out;
    }

    private static int count(List<ItemStack[]> rows) {
        int n = 0;
        for (ItemStack[] row : rows) for (ItemStack s : row) if (s != null && !s.isEmpty()) n++;
        return n;
    }

    private static int[] chips(List<String> colors) {
        Map<String, Integer> rgb = new LinkedHashMap<>();
        for (SprocketPalette.Entry e : SprocketPalette.ALL) rgb.put(e.id(), e.rgb());
        return colors.stream().mapToInt(rgb::get).toArray();
    }

    static Item coloredItem(String color, String kind) {
        Item ours = item(SprocketDyes.MOD_ID + ":" + color + "_" + kind);
        return ours != null ? ours : item("minecraft:" + color + "_" + kind);
    }

    private static Item item(String id) {
        Identifier key = Identifier.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) return null;
        //? if >=1.21.2 {
        Item item = BuiltInRegistries.ITEM.getValue(key);
        //?} else {
        /*Item item = BuiltInRegistries.ITEM.get(key);
        *///?}
        return item == Items.AIR ? null : item;
    }
}

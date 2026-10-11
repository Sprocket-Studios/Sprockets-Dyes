package io.github.sprocketstudios.sprocketdyes.color;

import io.github.sprocketstudios.sprocketdyes.palette.SprocketPalette;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

/** Lookups between {@link DyeColor}s and palette entries. Safe to use once {@code DyeColor} is initialized. */
public final class SprocketColors {
    private SprocketColors() {}

    public static final int VANILLA_COUNT = 16;

    private static final Map<DyeColor, SprocketPalette.Entry> ENTRY_BY_COLOR = new HashMap<>();
    private static final List<DyeColor> DISPLAY_ORDER;
    private static final List<DyeColor> NEW_COLORS;

    static {
        List<DyeColor> display = new ArrayList<>();
        for (SprocketPalette.Entry entry : SprocketPalette.ALL) {
            DyeColor color = DyeColor.byName(entry.id(), null);
            if (color == null) {
                throw new IllegalStateException("Sprocket Dyes: DyeColor '" + entry.id() + "' is missing - was DyeColorMixin applied?");
            }
            ENTRY_BY_COLOR.put(color, entry);
            display.add(color);
        }
        DISPLAY_ORDER = Collections.unmodifiableList(display);
        List<DyeColor> added = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            if (isNew(color)) added.add(color);
        }
        NEW_COLORS = Collections.unmodifiableList(added);
        if (NEW_COLORS.size() != SprocketPalette.NEW_COLOR_COUNT) {
            throw new IllegalStateException("Sprocket Dyes: expected " + SprocketPalette.NEW_COLOR_COUNT + " new DyeColors, found " + NEW_COLORS.size());
        }
    }

    /** True for the colors this mod adds (everything past vanilla's 16). */
    public static boolean isNew(DyeColor color) {
        return color.getId() >= VANILLA_COUNT;
    }

    public static SprocketPalette.Entry entry(DyeColor color) {
        return ENTRY_BY_COLOR.get(color);
    }

    /** All 132 colors: Key first, then each hue's 8 points. */
    public static List<DyeColor> displayOrder() {
        return DISPLAY_ORDER;
    }

    /** The 116 added colors, in id order. */
    public static List<DyeColor> newColors() {
        return NEW_COLORS;
    }
}

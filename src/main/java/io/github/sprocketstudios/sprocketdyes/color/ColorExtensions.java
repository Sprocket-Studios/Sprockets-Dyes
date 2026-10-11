package io.github.sprocketstudios.sprocketdyes.color;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.DyeColor;

/**
 * Answers for vanilla code that only knows the 16 vanilla colors.
 *
 * <p>Vanilla hard-codes per-color objects in exhaustive switches and 16-slot containers. Mixins at those
 * sites ask here first: a <em>site</em> is either a {@link Site} constant (for static switches) or the
 * container instance itself (26.2+ {@code ColorCollection}s). When nothing is registered for a new color,
 * the site falls back to that color's vanilla source color instead of crashing.
 */
public final class ColorExtensions {
    private ColorExtensions() {}

    /** Hard-coded switch sites that aren't backed by a container instance. */
    public enum Site {
        SHULKER_BOX_BLOCK,
        BUNDLE_ITEM,
        MAP_BANNER_DECORATION,
        SHEEP_LOOT_TABLE,
    }

    private static final Map<Object, Map<DyeColor, Supplier<?>>> VALUES = new IdentityHashMap<>();

    public static synchronized <T> void register(Object site, DyeColor color, Supplier<? extends T> value) {
        VALUES.computeIfAbsent(site, s -> new IdentityHashMap<>()).put(color, value);
    }

    /** The registered value for a new color at a site, or null. */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object site, DyeColor color) {
        Map<DyeColor, Supplier<?>> bySite = VALUES.get(site);
        if (bySite == null) return null;
        Supplier<?> value = bySite.get(color);
        return value == null ? null : (T) value.get();
    }

    /** The vanilla color whose objects stand in for {@code color} when it has none of its own. */
    public static DyeColor fallback(DyeColor color) {
        var entry = SprocketColors.entry(color);
        DyeColor source = entry == null ? null : DyeColor.byName(entry.source(), null);
        return source == null || SprocketColors.isNew(source) ? DyeColor.WHITE : source;
    }
}

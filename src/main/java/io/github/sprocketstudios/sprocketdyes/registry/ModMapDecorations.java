package io.github.sprocketstudios.sprocketdyes.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import io.github.sprocketstudios.sprocketdyes.color.ColorExtensions;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/** Banner markers on maps, one per new color (vanilla registers banner_<color> for its 16). */
public final class ModMapDecorations {
    private ModMapDecorations() {}

    public static final DeferredRegister<MapDecorationType> TYPES = DeferredRegister.create(SprocketDyes.MOD_ID, Registries.MAP_DECORATION_TYPE);
    public static final Map<DyeColor, RegistrySupplier<MapDecorationType>> BANNERS = new LinkedHashMap<>();

    static {
        for (DyeColor color : SprocketColors.newColors()) {
            // Same flags as vanilla's banner markers: shown in item frames, counted for map tracking.
            //? if >=26.3 {
            BANNERS.put(color, TYPES.register("banner_" + color.getName(), () -> new MapDecorationType(SprocketDyes.id(color.getName() + "_banner"), true, true)));
            //?} else {
            /*BANNERS.put(color, TYPES.register("banner_" + color.getName(), () -> new MapDecorationType(SprocketDyes.id(color.getName() + "_banner"), true, MapDecorationType.NO_MAP_COLOR, false, true)));
            *///?}
        }
    }

    public static void init() {
        TYPES.register();
        BANNERS.forEach((color, type) ->
            ColorExtensions.register(ColorExtensions.Site.MAP_BANNER_DECORATION, color, () -> BuiltInRegistries.MAP_DECORATION_TYPE.wrapAsHolder(type.get())));
    }
}

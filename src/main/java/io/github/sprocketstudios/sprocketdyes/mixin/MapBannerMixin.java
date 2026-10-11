package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.color.ColorExtensions;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code getDecoration} is an exhaustive switch over the vanilla 16 banner marker types. */
@Mixin(MapBanner.class)
public abstract class MapBannerMixin {
    @Shadow @Final private BlockPos pos;
    @Shadow @Final private DyeColor color;
    @Shadow @Final private Optional<Component> name;

    @Inject(method = "getDecoration", at = @At("HEAD"), cancellable = true)
    private void sprocketDyes$newColors(CallbackInfoReturnable<Holder<MapDecorationType>> cir) {
        if (!SprocketColors.isNew(this.color)) return;
        Holder<MapDecorationType> own = ColorExtensions.get(ColorExtensions.Site.MAP_BANNER_DECORATION, this.color);
        cir.setReturnValue(own != null ? own : new MapBanner(this.pos, ColorExtensions.fallback(this.color), this.name).getDecoration());
    }
}

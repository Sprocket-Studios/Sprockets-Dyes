package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.color.ColorExtensions;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2+ {@code ColorCollection.pick} is an exhaustive switch over 16 record components. New colors resolve
 * through {@link ColorExtensions} with the collection instance as the site.
 */
@Mixin(targets = "net.minecraft.world.level.block.ColorCollection")
public abstract class ColorCollectionMixin {
    @Shadow(remap = false)
    public abstract Object pick(DyeColor color);

    @Inject(method = "pick", at = @At("HEAD"), cancellable = true, remap = false)
    private void sprocketDyes$newColors(DyeColor color, CallbackInfoReturnable<Object> cir) {
        if (!SprocketColors.isNew(color)) return;
        Object own = ColorExtensions.get(this, color);
        cir.setReturnValue(own != null ? own : this.pick(ColorExtensions.fallback(color)));
    }
}

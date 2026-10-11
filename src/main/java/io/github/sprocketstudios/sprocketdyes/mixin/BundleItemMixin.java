package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.color.ColorExtensions;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code BundleItem.getByColor} is an exhaustive switch over the vanilla 16 (1.21.2 - 1.21.11). */
@Mixin(targets = "net.minecraft.world.item.BundleItem")
public abstract class BundleItemMixin {
    @Inject(method = "getByColor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sprocketDyes$newColors(DyeColor color, CallbackInfoReturnable<Item> cir) {
        if (color == null || !SprocketColors.isNew(color)) return;
        Item item = ColorExtensions.get(ColorExtensions.Site.BUNDLE_ITEM, color);
        //? if >=1.21.2 && <26.1 {
        /*cir.setReturnValue(item != null ? item : net.minecraft.world.item.BundleItem.getByColor(ColorExtensions.fallback(color)));
        *///?}
    }
}

package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.color.ColorExtensions;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code getBlockByColor} is an exhaustive switch over the vanilla 16 (1.21 - 1.21.11). */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
    @Inject(method = "getBlockByColor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sprocketDyes$newColors(DyeColor color, CallbackInfoReturnable<Block> cir) {
        if (color == null || !SprocketColors.isNew(color)) return;
        Block block = ColorExtensions.get(ColorExtensions.Site.SHULKER_BOX_BLOCK, color);
        //? if <26.1 {
        /*cir.setReturnValue(block != null ? block : ShulkerBoxBlock.getBlockByColor(ColorExtensions.fallback(color)));
        *///?}
    }
}

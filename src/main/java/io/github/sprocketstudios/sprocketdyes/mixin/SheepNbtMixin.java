package io.github.sprocketstudios.sprocketdyes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 1.21 - 1.21.4 read the saved color as a signed byte; ids 128-131 must come back unsigned. */
@Mixin(targets = "net.minecraft.world.entity.animal.Sheep")
public abstract class SheepNbtMixin {
    @ModifyArg(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/DyeColor;byId(I)Lnet/minecraft/world/item/DyeColor;"), require = 0)
    private int sprocketDyes$unsigned(int id) {
        return id & 0xFF;
    }
}

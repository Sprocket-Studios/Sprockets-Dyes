package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.block.entity.ModBlockEntitySupport;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets vanilla's bed, banner and shulker box block entities live in our blocks. */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void sprocketDyes$ourBlocks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ModBlockEntitySupport.supports((BlockEntityType<?>) (Object) this, state.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}

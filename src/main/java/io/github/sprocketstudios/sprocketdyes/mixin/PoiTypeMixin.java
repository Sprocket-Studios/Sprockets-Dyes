package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.block.entity.BedHomes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** HOME's state check (used when validating claimed beds) accepts our bed heads. */
@Mixin(PoiType.class)
public abstract class PoiTypeMixin {
    @Inject(method = "is", at = @At("HEAD"), cancellable = true)
    private void sprocketDyes$is(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (BedHomes.isOurBedHead(state) && (Object) this == BedHomes.home().value()) cir.setReturnValue(true);
    }
}

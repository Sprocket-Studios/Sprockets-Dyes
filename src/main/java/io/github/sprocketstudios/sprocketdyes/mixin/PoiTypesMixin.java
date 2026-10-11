package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.block.entity.BedHomes;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Our beds are villager homes too. */
@Mixin(PoiTypes.class)
public abstract class PoiTypesMixin {
    @Inject(method = "forState", at = @At("HEAD"), cancellable = true)
    private static void sprocketDyes$forState(BlockState state, CallbackInfoReturnable<Optional<Holder<PoiType>>> cir) {
        if (BedHomes.isOurBedHead(state)) cir.setReturnValue(Optional.of(BedHomes.home()));
    }

    @Inject(method = "hasPoi", at = @At("HEAD"), cancellable = true)
    private static void sprocketDyes$hasPoi(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (BedHomes.isOurBedHead(state)) cir.setReturnValue(true);
    }
}

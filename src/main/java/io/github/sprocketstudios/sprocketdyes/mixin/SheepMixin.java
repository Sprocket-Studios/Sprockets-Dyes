package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.network.SheepColorPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.5 {
import net.minecraft.world.entity.animal.sheep.Sheep;
//?} else {
/*import net.minecraft.world.entity.animal.Sheep;
*///?}

/**
 * Vanilla packs a sheep's color into the low 4 bits of a synced byte (bit 16 is "sheared"), which can't hold
 * ids past 15. The full id is kept in a plain field; clients learn it through {@link SheepColorPayload}.
 * Until a client hears otherwise it falls back to vanilla's byte, which is exact for vanilla colors.
 */
@Mixin(Sheep.class)
public abstract class SheepMixin implements SheepColorPayload.SheepColorHolder {
    @Unique
    private int sprocketDyes$colorId = -1;

    @Override
    public int sprocketDyes$colorId() {
        return this.sprocketDyes$colorId;
    }

    @Override
    public void sprocketDyes$setColorId(int id) {
        this.sprocketDyes$colorId = id;
    }

    @Inject(method = "getColor()Lnet/minecraft/world/item/DyeColor;", at = @At("HEAD"), cancellable = true)
    private void sprocketDyes$getColor(CallbackInfoReturnable<DyeColor> cir) {
        if (this.sprocketDyes$colorId >= 0) cir.setReturnValue(DyeColor.byId(this.sprocketDyes$colorId));
    }

    @Inject(method = "setColor", at = @At("HEAD"))
    private void sprocketDyes$setColor(DyeColor color, CallbackInfo ci) {
        this.sprocketDyes$colorId = color.getId();
        SheepColorPayload.broadcast((Entity) (Object) this, color.getId());
    }
}

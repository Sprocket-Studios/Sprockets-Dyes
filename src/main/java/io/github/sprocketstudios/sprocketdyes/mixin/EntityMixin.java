package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import io.github.sprocketstudios.sprocketdyes.network.SheepColorPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player who starts seeing a sheep in one of our colors learns its full color id (after vanilla's spawn data). */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "startSeenByPlayer", at = @At("TAIL"))
    private void sprocketDyes$sheepColor(ServerPlayer player, CallbackInfo ci) {
        if (this instanceof SheepColorPayload.SheepColorHolder sheep && sheep.sprocketDyes$colorId() >= SprocketColors.VANILLA_COUNT) {
            SheepColorPayload.send(player, (Entity) (Object) this, sheep.sprocketDyes$colorId());
        }
    }
}

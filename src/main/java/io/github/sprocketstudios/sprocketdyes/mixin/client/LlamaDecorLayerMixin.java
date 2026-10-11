package io.github.sprocketstudios.sprocketdyes.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.2 {
/*import java.util.Arrays;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * 1.21 - 1.21.1 index llama carpet textures in a 16-entry array by color id; extend it to every color.
 * From 1.21.2 carpets render through equipment assets keyed by color name.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.layers.LlamaDecorLayer")
public abstract class LlamaDecorLayerMixin {
    //? if <1.21.2 {
    /*@Shadow @Final @Mutable private static ResourceLocation[] TEXTURE_LOCATION;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sprocketDyes$decor(CallbackInfo ci) {
        DyeColor[] colors = DyeColor.values();
        ResourceLocation[] all = Arrays.copyOf(TEXTURE_LOCATION, colors.length);
        for (DyeColor color : colors) {
            if (all[color.getId()] == null) {
                all[color.getId()] = ResourceLocation.withDefaultNamespace("textures/entity/llama/decor/" + color.getName() + ".png");
            }
        }
        TEXTURE_LOCATION = all;
    }
    *///?}
}

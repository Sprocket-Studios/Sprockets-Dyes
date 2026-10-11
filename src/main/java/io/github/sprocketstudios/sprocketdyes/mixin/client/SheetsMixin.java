package io.github.sprocketstudios.sprocketdyes.mixin.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.4 {
/*import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
*///?}

/**
 * 1.21 - 1.21.3 list the 16 shulker box textures by name; append the new colors (indexed by color id) so
 * {@code SHULKER_TEXTURE_LOCATION.get(color.getId())} resolves. 1.21.4+ builds the list from DyeColor itself.
 */
@Mixin(Sheets.class)
public abstract class SheetsMixin {
    //? if <1.21.4 {
    /*@Shadow @Final @Mutable public static List<Material> SHULKER_TEXTURE_LOCATION;
    @Shadow @Final public static ResourceLocation SHULKER_SHEET;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sprocketDyes$shulkers(CallbackInfo ci) {
        List<Material> all = new ArrayList<>(SHULKER_TEXTURE_LOCATION);
        for (DyeColor color : DyeColor.values()) {
            if (color.getId() >= all.size()) {
                all.add(new Material(SHULKER_SHEET, ResourceLocation.withDefaultNamespace("entity/shulker/shulker_" + color.getName())));
            }
        }
        SHULKER_TEXTURE_LOCATION = List.copyOf(all);
    }
    *///?}
}

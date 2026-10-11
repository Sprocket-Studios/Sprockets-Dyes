package io.github.sprocketstudios.sprocketdyes.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.3 {
import io.github.sprocketstudios.sprocketdyes.client.SectionedCreativeScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}

/** Draws the creative tab's section headers after the slots, before the carried item and tooltips. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    //? if >=26.3 {
    @Inject(method = "extractContents", at = @At("TAIL"))
    private void sprocketDyes$sectionHeaders(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object) this instanceof SectionedCreativeScreen creative) {
            creative.sprocketDyes$drawHeaders(graphics, mouseX, mouseY);
        }
    }
    //?}
}

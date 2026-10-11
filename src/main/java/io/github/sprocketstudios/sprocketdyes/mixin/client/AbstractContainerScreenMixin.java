package io.github.sprocketstudios.sprocketdyes.mixin.client;

import io.github.sprocketstudios.sprocketdyes.client.SectionedCreativeScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.6 {
/*import org.spongepowered.asm.mixin.Shadow;
*///?}

/** Draws the creative tab's section headers after the slots, before the carried item and tooltips. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    //? if >=26.1 {
    @Inject(method = "extractContents", at = @At("TAIL"))
    private void sprocketDyes$sectionHeaders(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object) this instanceof SectionedCreativeScreen creative) {
            creative.sprocketDyes$drawHeaders(graphics, mouseX, mouseY);
        }
    }
    //?} else if >=1.21.6 {
    /*@Inject(method = "renderContents", at = @At("TAIL"))
    private void sprocketDyes$sectionHeaders(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object) this instanceof SectionedCreativeScreen creative) {
            creative.sprocketDyes$drawHeaders(graphics, mouseX, mouseY);
        }
    }
    *///?} else {
    /*@Shadow protected int leftPos;
    @Shadow protected int topPos;

    // A single render method: draw right before the labels, while the pose is moved to the screen's corner.
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void sprocketDyes$sectionHeaders(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object) this instanceof SectionedCreativeScreen creative) {
            graphics.pose().pushPose();
            graphics.pose().translate(-this.leftPos, -this.topPos, 0.0F);
            creative.sprocketDyes$drawHeaders(graphics, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }
    *///?}
}

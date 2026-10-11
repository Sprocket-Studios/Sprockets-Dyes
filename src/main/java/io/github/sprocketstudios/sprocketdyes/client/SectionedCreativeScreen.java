package io.github.sprocketstudios.sprocketdyes.client;

//? if >=26.3 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}

/** Implemented on the creative screen by CreativeModeInventoryScreenMixin. */
public interface SectionedCreativeScreen {
    //? if >=26.3 {
    void sprocketDyes$drawHeaders(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    /** Shows the Colored Blocks tab and re-lays it out from the current expanded state. */
    void sprocketDyes$showColoredBlocks();

    /** Left-clicks the middle of a visible grid row through the screen's normal click handling (tests). */
    void sprocketDyes$clickVisibleRow(int visibleRow);
    //?}
}

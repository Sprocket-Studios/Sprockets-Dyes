package io.github.sprocketstudios.sprocketdyes.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Implemented on the creative screen by CreativeModeInventoryScreenMixin. */
public interface SectionedCreativeScreen {
    void sprocketDyes$drawHeaders(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    /** Shows the Colored Blocks tab and re-lays it out from the current expanded state. */
    void sprocketDyes$showColoredBlocks();

    /** Left-clicks the middle of a visible grid row through the screen's normal click handling (tests). */
    void sprocketDyes$clickVisibleRow(int visibleRow);

    /** The list row shown at the top of the grid (tests). */
    int sprocketDyes$firstRow();

    /** Scrolls so {@code row} is at the top of the grid (tests). */
    void sprocketDyes$scrollToRow(int row);
}

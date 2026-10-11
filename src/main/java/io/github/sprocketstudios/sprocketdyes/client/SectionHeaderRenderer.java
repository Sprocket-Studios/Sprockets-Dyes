package io.github.sprocketstudios.sprocketdyes.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws a section header across one row of the creative grid: a raised bar in the inventory's own greys
 * with a chevron, the section name, and a strip of the section's colors.
 */
public final class SectionHeaderRenderer {
    private SectionHeaderRenderer() {}

    public static final int WIDTH = 9 * 18;
    public static final int HEIGHT = 18;

    private static final int FILL = 0xFFB4B4B4;
    private static final int FILL_OPEN = 0xFFA4A4A4;
    private static final int FILL_HOVER = 0xFFC4C4D0;
    private static final int HIGHLIGHT = 0xFFEDEDED;
    private static final int SHADOW = 0xFF5B5B5B;
    private static final int INK = 0xFF3B3B3B;
    private static final int COUNT_INK = 0xFF6B6B6B;
    private static final int CHIP_EDGE = 0xFF2E2E2E;

    public static void draw(GuiGraphicsExtractor g, Font font, CreativeSections.Section section, int x, int y, boolean hovered) {
        boolean open = CreativeSections.isExpanded(section);
        int x1 = x + WIDTH, y1 = y + HEIGHT;

        // Raised bar: light top/left edge, dark bottom/right edge, like vanilla buttons and slots inverted.
        g.fill(x, y, x1, y1, hovered ? FILL_HOVER : open ? FILL_OPEN : FILL);
        g.fill(x, y, x1 - 1, y + 1, HIGHLIGHT);
        g.fill(x, y, x + 1, y1 - 1, HIGHLIGHT);
        g.fill(x + 1, y1 - 1, x1, y1, SHADOW);
        g.fill(x1 - 1, y + 1, x1, y1, SHADOW);

        chevron(g, x + 5, y + 4, open);
        g.text(font, section.title(), x + 17, y + 5, INK, false);

        // Color strip, right-aligned: one chip per color.
        int[] chips = section.chips();
        int chipW = chips.length > 4 ? 6 : 9, gap = 1;
        int stripW = chips.length * chipW + Math.max(0, chips.length - 1) * gap;
        int cx = x1 - 5 - stripW, cy = y + 4;
        if (chips.length > 0) {
            g.fill(cx - 1, cy - 1, cx + stripW + 1, cy + 11, CHIP_EDGE);
            for (int chip : chips) {
                g.fill(cx, cy, cx + chipW, cy + 10, 0xFF000000 | chip);
                cx += chipW + gap;
            }
        }

        // Item count, just left of the strip.
        String count = String.valueOf(section.itemCount());
        int countX = (chips.length > 0 ? x1 - 5 - stripW - 5 : x1 - 6) - font.width(count);
        g.text(font, count, countX, y + 5, COUNT_INK, false);
    }

    /** A pixel-art wedge: pointing right (5 wide, 9 tall) when closed, down (9 wide, 5 tall) when open. */
    private static void chevron(GuiGraphicsExtractor g, int x, int y, boolean open) {
        for (int i = 0; i < 5; i++) {
            if (open) {
                g.fill(x - 1 + i, y + 2 + i, x + 8 - i, y + 3 + i, INK);
            } else {
                g.fill(x + 2 + i, y + i, x + 3 + i, y + 9 - i, INK);
            }
        }
    }
}

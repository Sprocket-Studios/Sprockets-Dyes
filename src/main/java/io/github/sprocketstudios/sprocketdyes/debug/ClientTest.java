package io.github.sprocketstudios.sprocketdyes.debug;

import dev.architectury.event.events.client.ClientTickEvent;
import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import net.minecraft.client.Minecraft;
import io.github.sprocketstudios.sprocketdyes.client.CreativeSections;
import io.github.sprocketstudios.sprocketdyes.client.SectionedCreativeScreen;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.level.GameType;

/**
 * Dev-only client checks.
 * <ul>
 *   <li>{@code -Dsprocket_dyes.clienttest=true}: once resources are loaded (models baked, atlases stitched),
 *       log a marker and exit; scripts/clienttest.sh scans the log for missing assets.</li>
 *   <li>{@code -Dsprocket_dyes.clientshots=true} (with quick play into a world): switch to creative, open the
 *       Colored Blocks tab, screenshot it collapsed and with sections open, then exit.</li>
 * </ul>
 */
public final class ClientTest {
    private ClientTest() {}

    public static final boolean ENABLED = Boolean.getBoolean("sprocket_dyes.clienttest");
    public static final boolean SHOTS = Boolean.getBoolean("sprocket_dyes.clientshots");
    private static int quietTicks;
    private static int step;
    private static int wait;

    public static void init() {
        if (ENABLED || SHOTS) ClientTickEvent.CLIENT_POST.register(ClientTest::tick);
    }

    private static void tick(Minecraft minecraft) {
        //? if >=26.2 {
        boolean loading = minecraft.gui.overlay() != null;
        //?} else {
        /*boolean loading = minecraft.getOverlay() != null;
        *///?}
        if (loading) {
            quietTicks = 0;
            return;
        }
        if (++quietTicks < 40) return;
        if (!SHOTS) {
            SprocketDyes.LOGGER.info("[clienttest] resources loaded on Minecraft {}", SprocketDyes.MINECRAFT);
            Runtime.getRuntime().halt(0);
        }
        shots(minecraft);
    }

    private static void shots(Minecraft minecraft) {
        if (wait > 0) {
            wait--;
            return;
        }
        switch (step) {
            case 0 -> {  // in a world, no loading screen
                if (minecraft.player == null || minecraft.level == null || screen(minecraft) != null || minecraft.getSingleplayerServer() == null) return;
                var server = minecraft.getSingleplayerServer();
                var id = minecraft.player.getUUID();
                server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.CREATIVE));
                next(40);
            }
            case 1 -> {
                var screen = new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), true);
                setScreen(minecraft, screen);
                ((SectionedCreativeScreen) screen).sprocketDyes$showColoredBlocks();
                next(20);
            }
            case 2 -> {
                shot(minecraft, "collapsed");
                click(minecraft, 0);  // Key header, through the real click handler
                next(20);
            }
            case 3 -> {
                shot(minecraft, "key_open");
                click(minecraft, 0);  // close Key again
                click(minecraft, 1);  // Red header
                next(20);
            }
            case 4 -> {
                shot(minecraft, "red_open");
                clickListRow(minecraft, 1);  // close Red (opening it scrolled its header to the top): all collapsed
                // Open a section from the middle of the list: its header must stay in view.
                if (screen(minecraft) instanceof SectionedCreativeScreen screen) {
                    screen.sprocketDyes$scrollToRow(6);
                    int first = screen.sprocketDyes$firstRow();
                    var section = CreativeSections.headerAt(first + 2);
                    click(minecraft, 2);
                    int header = CreativeSections.headerRow(section), after = screen.sprocketDyes$firstRow();
                    boolean ok = CreativeSections.isExpanded(section) && header >= after && header < after + 5;
                    SprocketDyes.LOGGER.info("[clienttest] {} toggle scroll: opened {} at row {} (top row {} -> {})",
                        ok ? "PASS" : "FAIL", section.id(), header, first, after);
                    click(minecraft, header - after);  // and close it again
                    after = screen.sprocketDyes$firstRow();
                    header = CreativeSections.headerRow(section);
                    ok = !CreativeSections.isExpanded(section) && header >= after && header < after + 5;
                    SprocketDyes.LOGGER.info("[clienttest] {} toggle scroll: closed {} at row {} (top row {})",
                        ok ? "PASS" : "FAIL", section.id(), header, after);
                }
                next(20);
            }
            case 5 -> {
                shot(minecraft, "scroll_toggle");
                next(60);  // screenshots are written on an IO thread
            }
            default -> {
                SprocketDyes.LOGGER.info("[clienttest] screenshots done");
                Runtime.getRuntime().halt(0);
            }
        }
    }

    private static void next(int ticks) {
        step++;
        wait = ticks;
    }

    private static void click(Minecraft minecraft, int visibleRow) {
        if (screen(minecraft) instanceof SectionedCreativeScreen screen) screen.sprocketDyes$clickVisibleRow(visibleRow);
    }

    /** Clicks row {@code row} of the whole list (not of the visible grid). */
    private static void clickListRow(Minecraft minecraft, int row) {
        if (screen(minecraft) instanceof SectionedCreativeScreen screen) click(minecraft, row - screen.sprocketDyes$firstRow());
    }

    private static void shot(Minecraft minecraft, String name) {
        String file = "sprocket_dyes_" + name + ".png";
        //? if >=26.2 {
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.gameRenderer.mainRenderTarget(), 1, message -> {});
        //?} else if >=1.21.6 {
        /*Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), 1, message -> {});
        *///?} else {
        /*Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message -> {});
        *///?}
        SprocketDyes.LOGGER.info("[clienttest] screenshot {}", name);
    }

    private static Screen screen(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.screen();
        //?} else {
        /*return minecraft.screen;
        *///?}
    }

    private static void setScreen(Minecraft minecraft, Screen screen) {
        //? if >=26.2 {
        minecraft.gui.setScreen(screen);
        //?} else {
        /*minecraft.setScreen(screen);
        *///?}
    }
}

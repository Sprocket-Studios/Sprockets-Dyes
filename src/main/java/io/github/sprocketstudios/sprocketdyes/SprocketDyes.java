package io.github.sprocketstudios.sprocketdyes;

import dev.architectury.platform.Platform;
import io.github.sprocketstudios.sprocketdyes.debug.SelfTest;
import io.github.sprocketstudios.sprocketdyes.registry.ModBlocks;
import io.github.sprocketstudios.sprocketdyes.registry.ModItems;
import io.github.sprocketstudios.sprocketdyes.registry.ModLoot;
import io.github.sprocketstudios.sprocketdyes.registry.ModMapDecorations;
import io.github.sprocketstudios.sprocketdyes.registry.ModTrades;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SprocketDyes {
    public static final String MOD_ID = "sprocket_dyes";
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sprocket Dyes");

    private SprocketDyes() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Common (both sides, both loaders) initialization. */
    public static void init() {
        LOGGER.info("Sprocket Dyes {} for Minecraft {} on {}", VERSION, MINECRAFT, Platform.isFabric() ? "Fabric" : "NeoForge");
        ModBlocks.init();
        ModItems.init();
        ModMapDecorations.init();
        io.github.sprocketstudios.sprocketdyes.registry.Aliases.init();
        io.github.sprocketstudios.sprocketdyes.network.SheepColorPayload.init();
        ModLoot.init();
        ModTrades.init();
        SelfTest.init();
    }
}

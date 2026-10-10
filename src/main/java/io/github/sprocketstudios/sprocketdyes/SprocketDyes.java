package io.github.sprocketstudios.sprocketdyes;

import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SprocketDyes {
    public static final String MOD_ID = "sprocket_dyes";
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sprocket Dyes");

    private SprocketDyes() {}

    /** Common (both sides, both loaders) initialization. */
    public static void init() {
        LOGGER.info("Sprocket Dyes {} for Minecraft {} on {}", VERSION, MINECRAFT, Platform.isFabric() ? "Fabric" : "NeoForge");
    }
}

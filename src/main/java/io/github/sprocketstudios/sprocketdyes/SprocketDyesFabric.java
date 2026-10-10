package io.github.sprocketstudios.sprocketdyes;

//? if fabric {
import net.fabricmc.api.ModInitializer;

public final class SprocketDyesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SprocketDyes.init();
    }
}
//?}

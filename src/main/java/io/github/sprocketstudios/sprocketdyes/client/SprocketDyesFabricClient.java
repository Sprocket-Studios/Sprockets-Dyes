package io.github.sprocketstudios.sprocketdyes.client;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;

public final class SprocketDyesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SprocketDyesClient.init();
    }
}
//?}

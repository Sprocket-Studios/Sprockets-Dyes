package io.github.sprocketstudios.sprocketdyes;

//? if neoforge {
/*import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.github.sprocketstudios.sprocketdyes.client.SprocketDyesClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(SprocketDyes.MOD_ID)
public final class SprocketDyesNeoForge {
    public SprocketDyesNeoForge(IEventBus modBus) {
        SprocketDyes.init();
        if (Platform.getEnvironment() == Env.CLIENT) {
            SprocketDyesClient.init();
        }
    }
}
*///?}

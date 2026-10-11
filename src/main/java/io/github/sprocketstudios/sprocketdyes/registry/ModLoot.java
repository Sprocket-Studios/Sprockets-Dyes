package io.github.sprocketstudios.sprocketdyes.registry;

import dev.architectury.registry.registries.DeferredRegister;
import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import net.minecraft.core.registries.Registries;
//? if >=26.1 {
import io.github.sprocketstudios.sprocketdyes.loot.RandomNewDyeFunction;
//?}

public final class ModLoot {
    private ModLoot() {}

    public static void init() {
        //? if >=26.1 {
        var functions = DeferredRegister.create(SprocketDyes.MOD_ID, Registries.LOOT_FUNCTION_TYPE);
        functions.register("random_new_dye", () -> RandomNewDyeFunction.CODEC);
        functions.register();
        //?}
    }
}

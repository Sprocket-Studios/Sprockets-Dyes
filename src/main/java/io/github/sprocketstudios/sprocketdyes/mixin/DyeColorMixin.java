package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.palette.NewDyeColorDefinitions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.5 {
import com.mojang.serialization.Codec;
//?}

/**
 * Appends the 116 Sprocket Dyes colors to the {@link DyeColor} enum.
 *
 * <p>The new constants are added right after {@code $VALUES} is assigned in {@code <clinit>}, i.e. before
 * {@code BY_ID}, {@code BY_FIREWORK_COLOR}, {@code CODEC} and {@code STREAM_CODEC} are built, so every vanilla
 * lookup sees all 132 colors. Ids continue from 16, as {@code ByIdMap.continuous} requires.
 */
@Mixin(DyeColor.class)
public abstract class DyeColorMixin {
    @Shadow
    @Final
    @Mutable
    private static DyeColor[] $VALUES;

    //? if >=1.21.5 {
    @Shadow
    @Final
    @Mutable
    public static Codec<DyeColor> LEGACY_ID_CODEC;
    //?}

    //? if >=26.2 {
    @Invoker("<init>")
    private static DyeColor sprocketDyes$create(String constant, int ordinal, int id, String name, int textureDiffuseColor, MapColor mapColor, MapColor terracottaColor, int fireworkColor, int textColor) {
        throw new AssertionError();
    }
    //?} else {
    /*@Invoker("<init>")
    private static DyeColor sprocketDyes$create(String constant, int ordinal, int id, String name, int textureDiffuseColor, MapColor mapColor, int fireworkColor, int textColor) {
        throw new AssertionError();
    }
    *///?}

    @Inject(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/DyeColor;$VALUES:[Lnet/minecraft/world/item/DyeColor;", shift = At.Shift.AFTER))
    private static void sprocketDyes$appendColors(CallbackInfo ci) {
        List<DyeColor> colors = new ArrayList<>(Arrays.asList($VALUES));
        NewDyeColorDefinitions.define((constant, id, name, diffuse, mapColor, terracottaColor, firework, text) -> {
            if (id != colors.size()) {
                throw new IllegalStateException("Sprocket Dyes: DyeColor " + name + " has id " + id + " but would be ordinal " + colors.size());
            }
            //? if >=26.2 {
            colors.add(sprocketDyes$create(constant, colors.size(), id, name, diffuse, mapColor, terracottaColor, firework, text));
            //?} else {
            /*colors.add(sprocketDyes$create(constant, colors.size(), id, name, diffuse, mapColor, firework, text));
            *///?}
        });
        $VALUES = colors.toArray(DyeColor[]::new);
    }

    //? if >=1.21.5 {
    /** Vanilla stores the id as a signed byte, which wraps ids 128-131; read it back unsigned. */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sprocketDyes$unsignedLegacyCodec(CallbackInfo ci) {
        LEGACY_ID_CODEC = Codec.BYTE.xmap(b -> DyeColor.byId(b & 0xFF), color -> (byte) color.getId());
    }
    //?}
}

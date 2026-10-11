package io.github.sprocketstudios.sprocketdyes.mixin;

import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
//? if fabric && >=26.3 {
import io.github.sprocketstudios.sprocketdyes.registry.Aliases;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//?}

/**
 * Fabric has no registry alias API: resolve our old 26.3-replaced ids ({@link
 * io.github.sprocketstudios.sprocketdyes.registry.Aliases}) in the same lookups NeoForge's {@code addAlias} covers.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {
    //? if fabric && >=26.3 {
    @Shadow public abstract ResourceKey<? extends Registry<T>> key();

    private boolean sprocketDyes$aliased() {
        Object key = this.key();
        return key == Registries.BLOCK || key == Registries.ITEM;
    }

    private Identifier sprocketDyes$resolve(Identifier id) {
        return id != null && this.sprocketDyes$aliased() ? Aliases.resolve(id) : id;
    }

    private ResourceKey<T> sprocketDyes$resolve(ResourceKey<T> key) {
        if (key == null || !this.sprocketDyes$aliased()) return key;
        Identifier target = Aliases.resolve(key.identifier());
        return target == key.identifier() ? key : ResourceKey.create(this.key(), target);
    }

    @ModifyVariable(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
    private Identifier sprocketDyes$getValueById(Identifier id) {
        return this.sprocketDyes$resolve(id);
    }

    @ModifyVariable(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
    private Identifier sprocketDyes$getById(Identifier id) {
        return this.sprocketDyes$resolve(id);
    }

    @ModifyVariable(method = "containsKey(Lnet/minecraft/resources/Identifier;)Z", at = @At("HEAD"), argsOnly = true)
    private Identifier sprocketDyes$containsId(Identifier id) {
        return this.sprocketDyes$resolve(id);
    }

    @ModifyVariable(method = "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
    private ResourceKey<T> sprocketDyes$getValueByKey(ResourceKey<T> key) {
        return this.sprocketDyes$resolve(key);
    }

    @ModifyVariable(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
    private ResourceKey<T> sprocketDyes$getByKey(ResourceKey<T> key) {
        return this.sprocketDyes$resolve(key);
    }
    //?}
}

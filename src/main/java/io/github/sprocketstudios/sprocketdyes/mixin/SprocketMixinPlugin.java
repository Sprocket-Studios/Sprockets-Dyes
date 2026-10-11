package io.github.sprocketstudios.sprocketdyes.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Turns off mixins whose target doesn't exist (or no longer needs patching) on this node's MC version.
 * Mixins use string targets where the class is version-specific, so every mixin compiles on every node.
 */
public final class SprocketMixinPlugin implements IMixinConfigPlugin {
    private static final String PKG = "io.github.sprocketstudios.sprocketdyes.mixin.";

    private static final Set<String> DISABLED = Set.of(
        //? if >=26.1 {
        // 26.1 removed ShulkerBoxBlock#getBlockByColor and BundleItem#getByColor.
        "ShulkerBoxBlockMixin", "BundleItemMixin",
        //?}
        //? if <1.21.2 {
        /*"BundleItemMixin",
        *///?}
        //? if >=1.21.2 {
        // Sheep wool/loot use per-color loot tables from 1.21.2; llama carpets render via equipment assets.
        "SheepLegacyMixin", "client.LlamaDecorLayerMixin",
        //?}
        //? if >=1.21.4 {
        // Shulker textures are built from DyeColor itself from 1.21.4.
        "client.SheetsMixin",
        //?}
        //? if >=1.21.5 {
        // Sheep moved to animal.sheep and save their color through DyeColor.LEGACY_ID_CODEC (fixed in DyeColorMixin).
        "SheepNbtMixin",
        //?}
        //? if <26.2 {
        /*"ColorCollectionMixin",
        *///?}
        //? if neoforge || <26.3 {
        /*// Old-id aliases: NeoForge has an API; nothing to alias before 26.3.
        "MappedRegistryMixin",
        *///?}
        "<none>"
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = mixinClassName.startsWith(PKG) ? mixinClassName.substring(PKG.length()) : mixinClassName;
        return !DISABLED.contains(simple);
    }

    @Override
    public void onLoad(String mixinPackage) {
        // Fabric Loader and NeoForge production start MixinExtras themselves; NeoForge 1.21.x dev runs don't.
        // Idempotent, so always safe to call.
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

package io.github.sprocketstudios.sprocketdyes.mixin;

import io.github.sprocketstudios.sprocketdyes.color.ColorExtensions;
import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import io.github.sprocketstudios.sprocketdyes.registry.ModBlocks;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21 - 1.21.1 only: shearing drops wool from a hard-coded map and the death loot table comes from an
 * exhaustive switch. (1.21.2+ use per-color loot tables keyed by color name.)
 */
@Mixin(targets = "net.minecraft.world.entity.animal.Sheep")
public abstract class SheepLegacyMixin {
    @Shadow @Final private static Map<DyeColor, ItemLike> ITEM_BY_DYE;

    @Shadow public abstract boolean isSheared();

    @Shadow public abstract DyeColor getColor();

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sprocketDyes$wool(CallbackInfo ci) {
        for (DyeColor color : DyeColor.values()) {
            if (SprocketColors.isNew(color)) {
                // Lazy: our blocks register long after Sheep is initialized.
                ITEM_BY_DYE.put(color, () -> ModBlocks.COLORED.get(color).get(ModBlocks.Kind.WOOL).get().asItem());
            }
        }
    }

    @Inject(method = "getDefaultLootTable", at = @At("HEAD"), cancellable = true)
    private void sprocketDyes$loot(CallbackInfoReturnable<ResourceKey<LootTable>> cir) {
        if (!this.isSheared() && SprocketColors.isNew(this.getColor())) {
            cir.setReturnValue(ColorExtensions.get(ColorExtensions.Site.SHEEP_LOOT_TABLE, this.getColor()));
        }
    }
}

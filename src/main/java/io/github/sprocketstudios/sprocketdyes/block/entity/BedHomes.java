package io.github.sprocketstudios.sprocketdyes.block.entity;

import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Vanilla builds the villager HOME point of interest from its own 16 beds when PoiTypes loads, long before
 * ours exist. The PoiTypes/PoiType mixins route our bed heads to HOME through here.
 */
public final class BedHomes {
    private BedHomes() {}

    public static boolean isOurBedHead(BlockState state) {
        return state.getBlock() instanceof BedBlock bed && SprocketColors.isNew(bed.getColor()) && state.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    public static Holder<PoiType> home() {
        //? if >=1.21.2 {
        return BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.HOME);
        //?} else {
        /*return BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.HOME);
        *///?}
    }
}

package io.github.sprocketstudios.sprocketdyes.loot;

//? if >=26.1 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.sprocketstudios.sprocketdyes.registry.ModTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
//? if >=26.3 {
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
//?} else {
/*import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
*///?}

/** {@code sprocket_dyes:random_new_dye}: replaces the item with a random new dye, keeping the count. */
public final class RandomNewDyeFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomNewDyeFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, RandomNewDyeFunction::new));

    //? if >=26.3 {
    private RandomNewDyeFunction(Optional<Holder<LootItemCondition>> condition) {
        super(condition);
    }
    //?} else {
    /*private RandomNewDyeFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }
    *///?}

    @Override
    public MapCodec<RandomNewDyeFunction> codec() {
        return CODEC;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        return ModTrades.randomNewDye(context.getRandom(), stack.getCount());
    }
}
//?}

package io.github.sprocketstudios.sprocketdyes.registry;

import io.github.sprocketstudios.sprocketdyes.color.SprocketColors;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
//? if <26.1 {
/*import dev.architectury.registry.level.entity.trade.TradeRegistry;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
*///?}
//? if >=1.21.11 && <26.1 {
/*import net.minecraft.world.entity.npc.villager.VillagerTrades;
*///?} else if <1.21.11 {
/*import net.minecraft.world.entity.npc.VillagerTrades;
*///?}

/**
 * The Wandering Trader occasionally sells a random new dye: 3 for an emerald, like vanilla's dye trades.
 * Four such offers join the common pool. On 26.1+ trades are data-driven; see
 * {@code data/sprocket_dyes/villager_trade/wandering_trader/} and {@link io.github.sprocketstudios.sprocketdyes.loot.RandomNewDyeFunction}.
 */
public final class ModTrades {
    private ModTrades() {}

    public static final int OFFERS = 4;

    public static ItemStack randomNewDye(RandomSource random, int count) {
        List<DyeColor> colors = SprocketColors.newColors();
        DyeColor color = colors.get(random.nextInt(colors.size()));
        return new ItemStack(ModItems.DYES.get(color).get(), count);
    }

    public static void init() {
        //? if <26.1 {
        /*VillagerTrades.ItemListing[] listings = new VillagerTrades.ItemListing[OFFERS];
        for (int i = 0; i < OFFERS; i++) {
            //? if >=1.21.11 {
            /^listings[i] = (level, trader, random) -> offer(random);
            ^///?} else {
            listings[i] = (trader, random) -> offer(random);
            //?}
        }
        //? if fabric && >=1.21.5 {
        // Architectury's Fabric wandering-trader hook re-registers Fabric API's common pool on 1.21.5+ and crashes.
        net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.registerWanderingTraderOffers(builder ->
            builder.addOffersToPool(net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL, listings));
        //?} else if >=1.21.5 {
        /^TradeRegistry.registerTradeForWanderingTrader(TradeRegistry.WandererTradeType.GENERIC_TRADES, listings);
        ^///?} else {
        /^TradeRegistry.registerTradeForWanderingTrader(false, listings);
        ^///?}
        *///?}
    }

    //? if <26.1 {
    /*private static MerchantOffer offer(RandomSource random) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, 1), randomNewDye(random, 3), 12, 1, 0.05F);
    }
    *///?}
}

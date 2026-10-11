package io.github.sprocketstudios.sprocketdyes.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
//? if <26.1 {
/*import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import io.github.sprocketstudios.sprocketdyes.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
*///?}

public final class SprocketDyesClient {
    private SprocketDyesClient() {}

    /** Client-only initialization, called from each loader's client entrypoint. */
    public static void init() {
        ClientTooltipEvent.ITEM.register(ColorTooltip::append);
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> renderLayers());
        io.github.sprocketstudios.sprocketdyes.debug.ClientTest.init();
    }

    /** Glass needs the translucent layer before 26.1; from 26.1 the texture's alpha decides. */
    private static void renderLayers() {
        //? if <26.1 {
        /*Block[] glass = ModBlocks.TRANSLUCENT.stream().map(s -> s.get()).toArray(Block[]::new);
        //? if >=1.21.6 {
        RenderTypeRegistry.register(net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT, glass);
        //?} else {
        /^RenderTypeRegistry.register(net.minecraft.client.renderer.RenderType.translucent(), glass);
        ^///?}
        *///?}
    }
}

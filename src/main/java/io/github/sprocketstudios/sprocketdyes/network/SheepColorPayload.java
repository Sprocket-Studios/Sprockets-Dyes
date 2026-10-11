package io.github.sprocketstudios.sprocketdyes.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.github.sprocketstudios.sprocketdyes.SprocketDyes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * A sheep's full color id, server to client. Vanilla syncs the color in 4 bits of a byte (16 colors); rather
 * than adding synced entity data to a vanilla entity (fragile, and refused by NeoForge in dev), the full id
 * travels in this packet whenever it changes and whenever a player starts seeing the sheep.
 */
public record SheepColorPayload(int entityId, int colorId) implements CustomPacketPayload {
    public static final Type<SheepColorPayload> TYPE = new Type<>(SprocketDyes.id("sheep_color"));
    public static final StreamCodec<ByteBuf, SheepColorPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SheepColorPayload::entityId,
        ByteBufCodecs.VAR_INT, SheepColorPayload::colorId,
        SheepColorPayload::new
    );

    @Override
    public Type<SheepColorPayload> type() {
        return TYPE;
    }

    public static void init() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(TYPE, CODEC);
        } else {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, TYPE, CODEC, (payload, context) -> context.queue(() -> {
                Entity entity = context.getPlayer().level().getEntity(payload.entityId());
                if (entity instanceof SheepColorHolder sheep) sheep.sprocketDyes$setColorId(payload.colorId());
            }));
        }
    }

    /** Tells everyone tracking the sheep about its color. */
    public static void broadcast(Entity sheep, int colorId) {
        if (sheep.level() instanceof ServerLevel level) {
            var packet = new ClientboundCustomPayloadPacket(new SheepColorPayload(sheep.getId(), colorId));
            //? if >=1.21.9 {
            level.getChunkSource().sendToTrackingPlayers(sheep, packet);
            //?} else {
            /*level.getChunkSource().broadcast(sheep, packet);
            *///?}
        }
    }

    public static void send(ServerPlayer player, Entity sheep, int colorId) {
        NetworkManager.sendToPlayer(player, new SheepColorPayload(sheep.getId(), colorId));
    }

    /** Implemented on Sheep by SheepMixin. */
    public interface SheepColorHolder {
        int sprocketDyes$colorId();

        void sprocketDyes$setColorId(int id);
    }
}

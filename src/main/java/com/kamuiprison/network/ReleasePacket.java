package com.kamuiprison.network;

import com.kamuiprison.prison.PrisonManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Cliente -> servidor: libertar o prisioneiro escolhido na interface. */
public final class ReleasePacket {
    private final UUID id;

    public ReleasePacket(UUID id) {
        this.id = id;
    }

    public static void encode(ReleasePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.id);
    }

    public static ReleasePacket decode(FriendlyByteBuf buf) {
        return new ReleasePacket(buf.readUUID());
    }

    public static void handle(ReleasePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PrisonManager.handleReleaseRequest(player, msg.id);
            }
        });
        context.setPacketHandled(true);
    }
}

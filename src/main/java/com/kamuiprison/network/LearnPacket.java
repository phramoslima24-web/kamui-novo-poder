package com.kamuiprison.network;

import com.kamuiprison.jutsu.KamuiPrisonJutsu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: botao "Kamui: Prisao" da aba Dojutsu. */
public final class LearnPacket {
    public LearnPacket() {
    }

    public static void encode(LearnPacket msg, FriendlyByteBuf buf) {
    }

    public static LearnPacket decode(FriendlyByteBuf buf) {
        return new LearnPacket();
    }

    public static void handle(LearnPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                KamuiPrisonJutsu.learnOrBind(player);
            }
        });
        context.setPacketHandled(true);
    }
}

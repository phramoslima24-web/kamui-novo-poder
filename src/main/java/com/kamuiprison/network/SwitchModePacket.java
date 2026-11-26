package com.kamuiprison.network;

import com.kamuiprison.jutsu.KamuiPrisonJutsu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: a seta (mesma tecla do Kamui original) foi pressionada com Kamui: Prisao selecionado. */
public final class SwitchModePacket {
    public SwitchModePacket() {
    }

    public static void encode(SwitchModePacket msg, FriendlyByteBuf buf) {
    }

    public static SwitchModePacket decode(FriendlyByteBuf buf) {
        return new SwitchModePacket();
    }

    public static void handle(SwitchModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                KamuiPrisonJutsu.toggleMode(player);
            }
        });
        context.setPacketHandled(true);
    }
}

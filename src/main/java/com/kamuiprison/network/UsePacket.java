package com.kamuiprison.network;

import com.kamuiprison.jutsu.KamuiPrisonJutsu;
import com.kamuiprison.compat.NarutoBridge;
import com.kamuiprison.prison.PrisonManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: tecla de usar o jutsu (a mesma do Narutoto) solta com o modo Parcial selecionado. */
public final class UsePacket {
    public UsePacket() {
    }

    public static void encode(UsePacket msg, FriendlyByteBuf buf) {
    }

    public static UsePacket decode(FriendlyByteBuf buf) {
        return new UsePacket();
    }

    public static void handle(UsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !KamuiPrisonJutsu.isPartialSelected(player)) {
                return;
            }
            NarutoBridge.Vars vars = NarutoBridge.of(player);
            if (vars == null || vars.getDouble("ninja") <= 0.0) {
                return;
            }
            PrisonManager.openList(player);
        });
        context.setPacketHandled(true);
    }
}

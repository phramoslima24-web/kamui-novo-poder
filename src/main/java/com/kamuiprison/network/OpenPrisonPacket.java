package com.kamuiprison.network;

import com.kamuiprison.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> cliente: abre (ou atualiza) a interface com a lista de prisioneiros. */
public final class OpenPrisonPacket {
    private final List<PrisonerView> prisoners;

    public OpenPrisonPacket(List<PrisonerView> prisoners) {
        this.prisoners = prisoners;
    }

    public static void encode(OpenPrisonPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.prisoners.size());
        for (PrisonerView p : msg.prisoners) {
            buf.writeUUID(p.id());
            buf.writeUtf(p.name(), 256);
            buf.writeUtf(p.typeName(), 256);
            buf.writeBoolean(p.player());
        }
    }

    public static OpenPrisonPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<PrisonerView> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new PrisonerView(buf.readUUID(), buf.readUtf(256), buf.readUtf(256), buf.readBoolean()));
        }
        return new OpenPrisonPacket(list);
    }

    public static void handle(OpenPrisonPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openPrisonScreen(msg.prisoners)));
        context.setPacketHandled(true);
    }
}

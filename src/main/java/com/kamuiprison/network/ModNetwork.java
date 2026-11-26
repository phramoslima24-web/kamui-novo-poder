package com.kamuiprison.network;

import com.kamuiprison.KamuiPrisonMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de rede proprio do addon (nao interfere no canal do Narutoto). */
public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KamuiPrisonMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId = 0;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, SwitchModePacket.class, SwitchModePacket::encode,
                SwitchModePacket::decode, SwitchModePacket::handle);
        CHANNEL.registerMessage(nextId++, UsePacket.class, UsePacket::encode,
                UsePacket::decode, UsePacket::handle);
        CHANNEL.registerMessage(nextId++, LearnPacket.class, LearnPacket::encode,
                LearnPacket::decode, LearnPacket::handle);
        CHANNEL.registerMessage(nextId++, ReleasePacket.class, ReleasePacket::encode,
                ReleasePacket::decode, ReleasePacket::handle);
        CHANNEL.registerMessage(nextId++, OpenPrisonPacket.class, OpenPrisonPacket::encode,
                OpenPrisonPacket::decode, OpenPrisonPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}

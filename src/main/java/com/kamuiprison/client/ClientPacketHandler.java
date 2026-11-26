package com.kamuiprison.client;

import com.kamuiprison.network.PrisonerView;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Codigo exclusivo do cliente chamado pelos pacotes. */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void openPrisonScreen(List<PrisonerView> prisoners) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof KamuiPrisonScreen screen) {
            screen.setPrisoners(prisoners);
        } else {
            mc.setScreen(new KamuiPrisonScreen(prisoners));
        }
    }
}

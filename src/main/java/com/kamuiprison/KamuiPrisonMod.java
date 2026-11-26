package com.kamuiprison;

import com.kamuiprison.config.PrisonConfig;
import com.kamuiprison.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Addon do Narutoto: Kamui: Prisao.
 * Os eventos (Forge bus) ficam em {@link com.kamuiprison.events.CommonEvents} e
 * {@link com.kamuiprison.client.ClientEvents}.
 */
@Mod(KamuiPrisonMod.MOD_ID)
public class KamuiPrisonMod {
    public static final String MOD_ID = "kamuiprison";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KamuiPrisonMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PrisonConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}

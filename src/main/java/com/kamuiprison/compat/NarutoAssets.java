package com.kamuiprison.compat;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Busca sons, particulas e efeitos do Narutoto pelo registro (sem dependencia de compilacao).
 * Todos os metodos retornam null se o item nao existir, e quem chama usa um fallback do vanilla.
 */
public final class NarutoAssets {
    private NarutoAssets() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(NarutoBridge.NARUTOTO_ID, path);
    }

    public static SoundEvent sound(String path) {
        return ForgeRegistries.SOUND_EVENTS.getValue(id(path));
    }

    public static SimpleParticleType particle(String path) {
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(id(path));
        return type instanceof SimpleParticleType simple ? simple : null;
    }

    public static MobEffect effect(String path) {
        return ForgeRegistries.MOB_EFFECTS.getValue(id(path));
    }
}

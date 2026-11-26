package com.kamuiprison.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracao comum (config/kamuiprison-common.toml). */
public final class PrisonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue CAPTURE_CHAKRA_COST;
    public static final ForgeConfigSpec.IntValue CAPTURE_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue CAPTURE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue LEARN_JUTSU_POINTS;
    public static final ForgeConfigSpec.IntValue LEARN_DOJUTSU_SKILLED;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_OBITO_MANGEKYO;
    public static final ForgeConfigSpec.BooleanValue SHARED_PRISON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("capture");
        CAPTURE_CHAKRA_COST = builder
                .comment("Custo de chakra para prender um alvo (modo Total). Nao e cobrado em creativo.")
                .defineInRange("chakraCost", 100, 0, 100000);
        CAPTURE_DELAY_TICKS = builder
                .comment("Duracao (ticks) da animacao da espiral do Kamui antes de o alvo ser levado para a prisao.")
                .defineInRange("delayTicks", 20, 1, 200);
        CAPTURE_COOLDOWN_TICKS = builder
                .comment("Tempo de recarga (ticks) entre duas capturas do mesmo usuario.")
                .defineInRange("cooldownTicks", 20, 0, 12000);
        builder.pop();

        builder.push("learn");
        LEARN_JUTSU_POINTS = builder
                .comment("Pontos de jutsu (JP) gastos para aprender Kamui: Prisao.")
                .defineInRange("jutsuPoints", 8, 0, 100000);
        LEARN_DOJUTSU_SKILLED = builder
                .comment("Habilidade em Dojutsu (DS) minima para aprender Kamui: Prisao.")
                .defineInRange("dojutsuSkilled", 150, 0, 100000);
        REQUIRE_OBITO_MANGEKYO = builder
                .comment("Exigir o Mangekyo de Obito (mesma condicao do Kamui Transferencia original) para aprender.")
                .define("requireObitoMangekyo", true);
        builder.pop();

        builder.push("prison");
        SHARED_PRISON = builder
                .comment("false = cada usuario do Kamui ve e liberta apenas os proprios prisioneiros.",
                        "true = todos os usuarios compartilham a mesma lista de prisioneiros.")
                .define("sharedPrison", false);
        builder.pop();

        SPEC = builder.build();
    }

    private PrisonConfig() {
    }
}

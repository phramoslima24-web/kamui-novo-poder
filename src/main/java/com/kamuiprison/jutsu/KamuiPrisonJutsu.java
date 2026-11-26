package com.kamuiprison.jutsu;

import com.kamuiprison.compat.NarutoAssets;
import com.kamuiprison.compat.NarutoBridge;
import com.kamuiprison.config.PrisonConfig;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * Kamui: Prisao, integrado ao sistema de jutsus do Narutoto.
 *
 * O Narutoto identifica o jutsu selecionado pelo NOME gravado em {@code PlayerVariables.jutsu_choosed_name}
 * (e nos slots {@code jutsu_key1..20} / NBT {@code jutsuN}). Aqui seguimos o mesmo padrao:
 * dois nomes (modos Total e Parcial) que se alternam com as mesmas setas do Kamui original.
 */
public final class KamuiPrisonJutsu {
    /** Chave gravada em learned_jutsu (o original grava "jutsu.kamui_transfer_part"). */
    public static final String KEY_TOTAL = "jutsu.kamuiprison.total";
    public static final String KEY_PARTIAL = "jutsu.kamuiprison.partial";

    private static final String EN_TOTAL = "Kamui: Prison (Total \u00b7 Capture)";
    private static final String EN_PARTIAL = "Kamui: Prison (Partial \u00b7 Release)";
    private static final String PT_TOTAL = "Kamui: Pris\u00e3o (Total \u00b7 Prender)";
    private static final String PT_PARTIAL = "Kamui: Pris\u00e3o (Parcial \u00b7 Sair)";

    private static final int MAX_SLOT = 20;

    private KamuiPrisonJutsu() {
    }

    // ------------------------------------------------------------------ nomes

    private static String localized(String key, String fallback) {
        String text = Component.translatable(key).getString();
        return key.equals(text) ? fallback : text;
    }

    public static String nameTotal() {
        return localized(KEY_TOTAL, EN_TOTAL);
    }

    public static String namePartial() {
        return localized(KEY_PARTIAL, EN_PARTIAL);
    }

    public static boolean isTotalName(String name) {
        return name != null && (name.equals(KEY_TOTAL) || name.equals(EN_TOTAL) || name.equals(PT_TOTAL)
                || name.equals(nameTotal()));
    }

    public static boolean isPartialName(String name) {
        return name != null && (name.equals(KEY_PARTIAL) || name.equals(EN_PARTIAL) || name.equals(PT_PARTIAL)
                || name.equals(namePartial()));
    }

    private static String chosenName(Entity entity) {
        NarutoBridge.Vars vars = NarutoBridge.of(entity);
        return vars == null ? "" : vars.getString("jutsu_choosed_name");
    }

    public static boolean isTotalSelected(Entity entity) {
        return isTotalName(chosenName(entity));
    }

    public static boolean isPartialSelected(Entity entity) {
        return isPartialName(chosenName(entity));
    }

    public static boolean isSelected(Entity entity) {
        String name = chosenName(entity);
        return isTotalName(name) || isPartialName(name);
    }

    // ------------------------------------------------------------------ custo

    public static int totalChakraCost() {
        return PrisonConfig.CAPTURE_CHAKRA_COST.get();
    }

    /**
     * O Narutoto atualiza chakra_cost a cada troca de jutsu apenas para os nomes que ele conhece.
     * Chamado periodicamente no servidor para manter o custo exibido coerente com o modo ativo.
     */
    public static void refreshChakraCost(ServerPlayer player) {
        NarutoBridge.Vars vars = NarutoBridge.of(player);
        if (vars == null) {
            return;
        }
        String name = vars.getString("jutsu_choosed_name");
        double expected;
        if (isTotalName(name)) {
            expected = totalChakraCost();
        } else if (isPartialName(name)) {
            expected = 0.0;
        } else {
            return;
        }
        if (vars.getDouble("chakra_cost") != expected) {
            vars.setDouble("chakra_cost", expected);
            vars.sync();
        }
    }

    // ------------------------------------------------------------------ slot

    private static int currentSlot(NarutoBridge.Vars vars) {
        int slot = (int) Math.round(vars.getDouble("jutsu_choosed"));
        return slot < 1 || slot > MAX_SLOT ? 1 : slot;
    }

    /** Grava o nome no slot atual exatamente como o Narutoto faz (NBT "jutsuN", jutsu_keyN e nome escolhido). */
    private static void writeToCurrentSlot(ServerPlayer player, NarutoBridge.Vars vars, String name, double cost) {
        int slot = currentSlot(vars);
        player.getPersistentData().putString("jutsu" + slot, name);
        String keyField = "jutsu_key" + slot;
        if (vars.has(keyField)) {
            vars.setString(keyField, name);
        }
        vars.setString("jutsu_choosed_name", name);
        vars.setDouble("chakra_cost", cost);
        vars.sync();
    }

    // ------------------------------------------------------------------ alternar modo (setas)

    /** Alterna Total <-> Parcial. Chamado pelo pacote enviado quando a seta e pressionada. */
    public static void toggleMode(ServerPlayer player) {
        NarutoBridge.Vars vars = NarutoBridge.of(player);
        if (vars == null) {
            return;
        }
        String current = vars.getString("jutsu_choosed_name");
        if (isTotalName(current)) {
            writeToCurrentSlot(player, vars, namePartial(), 0.0);
            player.displayClientMessage(Component.translatable("message.kamuiprison.mode_partial"), true);
        } else if (isPartialName(current)) {
            writeToCurrentSlot(player, vars, nameTotal(), totalChakraCost());
            player.displayClientMessage(Component.translatable("message.kamuiprison.mode_total"), true);
        }
    }

    // ------------------------------------------------------------------ aprender / equipar

    private static boolean isTailsForm(String name) {
        return name.equals(Component.translatable("jutsu.tails_cloak").getString())
                || name.equals(Component.translatable("jutsu.tails_half").getString())
                || name.equals(Component.translatable("jutsu.tails_nine").getString())
                || name.equals(Component.translatable("jutsu.tails_full").getString());
    }

    private static boolean meetsMangekyoCondition(ServerPlayer player, NarutoBridge.Vars vars) {
        if (!PrisonConfig.REQUIRE_OBITO_MANGEKYO.get()) {
            return true;
        }
        if (!"obito".equals(vars.getString("mangekyo_owner"))) {
            return false;
        }
        Advancement advancement = player.server.getAdvancements()
                .getAdvancement(new ResourceLocation(NarutoBridge.NARUTOTO_ID, "learn_mangekyo_sasuke"));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void playLearnSound(ServerPlayer player) {
        SoundEvent sound = NarutoAssets.sound("learn_jutsu");
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound,
                    SoundSource.PLAYERS, 2.0F, 1.0F);
        }
    }

    /**
     * Botao "Kamui: Prisao" da aba Dojutsu. Mesma regra do botao "Kamui Transferencia" original:
     * se ja aprendeu, apenas equipa no slot selecionado; senao aprende (gasta JP) e equipa.
     */
    public static void learnOrBind(ServerPlayer player) {
        NarutoBridge.Vars vars = NarutoBridge.of(player);
        if (vars == null) {
            return;
        }
        if (vars.getDouble("ninja") <= 0.0) {
            return;
        }
        if (isTailsForm(vars.getString("jutsu_choosed_name"))) {
            return;
        }

        if (player.isCreative()) {
            bind(player, vars);
            return;
        }
        if (!meetsMangekyoCondition(player, vars)) {
            player.sendSystemMessage(Component.translatable("text.can.not.learn"));
            return;
        }
        if (vars.getString("learned_jutsu").contains(KEY_TOTAL)) {
            bind(player, vars);
            return;
        }

        double jp = vars.getDouble("jutsu_point");
        double skilled = vars.getDouble("dojutsu_skilled");
        int jpCost = PrisonConfig.LEARN_JUTSU_POINTS.get();
        if (jp >= jpCost && skilled >= PrisonConfig.LEARN_DOJUTSU_SKILLED.get()) {
            vars.setDouble("jutsu_point", jp - jpCost);
            vars.setString("learned_jutsu", vars.getString("learned_jutsu") + KEY_TOTAL);
            vars.sync();
            player.displayClientMessage(Component.translatable("message.kamuiprison.learned"), true);
            bind(player, vars);
        } else {
            player.sendSystemMessage(Component.translatable("text.can.not.learn"));
        }
    }

    private static void bind(ServerPlayer player, NarutoBridge.Vars vars) {
        writeToCurrentSlot(player, vars, nameTotal(), totalChakraCost());
        playLearnSound(player);
    }
}

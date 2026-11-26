package com.kamuiprison.events;

import com.kamuiprison.KamuiPrisonMod;
import com.kamuiprison.command.PrisonCommand;
import com.kamuiprison.compat.NarutoBridge;
import com.kamuiprison.jutsu.KamuiPrisonJutsu;
import com.kamuiprison.prison.PrisonData;
import com.kamuiprison.prison.PrisonManager;
import com.kamuiprison.prison.ServerTasks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Eventos comuns (cliente e servidor) do Forge bus. */
@Mod.EventBusSubscriber(modid = KamuiPrisonMod.MOD_ID)
public final class CommonEvents {
    private CommonEvents() {
    }

    /**
     * Modo Total: clique esquerdo em um mob ou jogador captura o alvo.
     * O ataque normal e cancelado (nos dois lados) enquanto o modo Total estiver selecionado.
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        Entity target = event.getTarget();
        if (target instanceof PartEntity<?> part) {
            target = part.getParent();
        }
        if (!(target instanceof Mob) && !(target instanceof Player)) {
            return;
        }
        if (!NarutoBridge.isLoaded() || !KamuiPrisonJutsu.isTotalSelected(attacker)) {
            return;
        }
        event.setCanceled(true);
        if (attacker.level().isClientSide()) {
            return;
        }
        if (attacker instanceof ServerPlayer serverPlayer) {
            PrisonManager.startCapture(serverPlayer, target);
        }
    }

    /** Impede copias fantasma: um mob preso nunca pode voltar ao mundo por outro caminho que nao a libertacao. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity() instanceof Player) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null || server.overworld() == null) {
            return;
        }
        if (PrisonData.get(server).isMobPrisoner(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ServerTasks.tick();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        if (server.getTickCount() % 10 == 0) {
            PrisonManager.tickEnforcement(server);
            if (NarutoBridge.isLoaded()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    KamuiPrisonJutsu.refreshChakraCost(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PrisonManager.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PrisonManager.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PrisonCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ServerTasks.clear();
    }
}

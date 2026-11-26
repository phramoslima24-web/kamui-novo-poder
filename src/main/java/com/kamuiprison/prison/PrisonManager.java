package com.kamuiprison.prison;

import com.kamuiprison.KamuiPrisonMod;
import com.kamuiprison.compat.NarutoAssets;
import com.kamuiprison.compat.NarutoBridge;
import com.kamuiprison.config.PrisonConfig;
import com.kamuiprison.jutsu.KamuiPrisonJutsu;
import com.kamuiprison.network.ModNetwork;
import com.kamuiprison.network.OpenPrisonPacket;
import com.kamuiprison.network.PrisonerView;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Captura, armazenamento e libertacao de prisioneiros do Kamui: Prisao.
 *
 * Garantias contra perda/duplicacao:
 *  - O registro (NBT completo para mobs) e gravado em {@link PrisonData} ANTES de a entidade ser removida.
 *  - Na libertacao o registro e removido ANTES de a entidade ser adicionada; se a adicao falhar, o registro volta.
 *  - Copias antigas de um mob preso (ex.: chunk que nao salvou a remocao) sao bloqueadas ao carregar
 *    (ver CommonEvents#onEntityJoin).
 *  - Jogadores ficam fisicamente na dimensao de prisao e sao trazidos de volta caso saiam.
 */
public final class PrisonManager {
    public enum ReleaseResult {
        OK, PENDING, NOT_FOUND, FAILED
    }

    private static final Set<UUID> ANIMATING = new HashSet<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private PrisonManager() {
    }

    // =====================================================================================
    // Captura (modo Total)
    // =====================================================================================

    public static void startCapture(ServerPlayer caster, Entity target) {
        if (!(target instanceof LivingEntity living) || target == caster) {
            return;
        }
        MinecraftServer server = caster.server;
        if (target instanceof ServerPlayer targetPlayer && targetPlayer.isSpectator()) {
            return;
        }

        NarutoBridge.Vars vars = NarutoBridge.of(caster);
        if (vars == null || vars.getDouble("ninja") <= 0.0) {
            return;
        }
        if (NarutoBridge.isInDimension(caster, "narutoto:ninetails_cage_dimension")) {
            caster.displayClientMessage(Component.translatable("text.nosky"), true);
            return;
        }
        if (isUnderGenjutsu(caster)) {
            return;
        }

        long now = server.overworld().getGameTime();
        Long until = COOLDOWNS.get(caster.getUUID());
        if (until != null && until > now) {
            caster.displayClientMessage(Component.translatable("text.cooldown"), true);
            return;
        }
        if (ANIMATING.contains(target.getUUID()) || PrisonData.get(server).contains(target.getUUID())) {
            return;
        }
        if (!(target instanceof ServerPlayer) && !living.getType().canSerialize()) {
            caster.displayClientMessage(Component.translatable("message.kamuiprison.cannot_seal"), true);
            return;
        }
        boolean free = caster.isCreative();
        if (!free && vars.getDouble("if_sharigan") != 2.0) {
            caster.displayClientMessage(Component.translatable("text.needtoopenmangekyo"), true);
            return;
        }
        if (!free && vars.getDouble("chakra") < KamuiPrisonJutsu.totalChakraCost()) {
            caster.displayClientMessage(Component.translatable("text.notenoughchakra"), true);
            return;
        }

        UUID casterId = caster.getUUID();
        UUID targetId = target.getUUID();
        ANIMATING.add(targetId);
        COOLDOWNS.put(casterId, now + PrisonConfig.CAPTURE_COOLDOWN_TICKS.get() + PrisonConfig.CAPTURE_DELAY_TICKS.get());

        int delay = PrisonConfig.CAPTURE_DELAY_TICKS.get();
        playSound((ServerLevel) target.level(), target.getX(), target.getY(), target.getZ(), "kamui_suck");
        if (living instanceof Mob || living instanceof ServerPlayer) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, delay + 10, 9, false, false));
        }

        // Espiral do Kamui envolvendo o alvo (mesma particula do Kamui original), repetida durante a animacao.
        for (int t = 1; t < delay; t += 4) {
            ServerTasks.schedule(t, () -> {
                Entity current = findEntity(server, targetId);
                if (current != null && current.isAlive()) {
                    spiral((ServerLevel) current.level(), current);
                }
            });
        }
        ServerTasks.schedule(delay, () -> {
            try {
                finishCapture(server, casterId, targetId);
            } finally {
                ANIMATING.remove(targetId);
            }
        });
    }

    private static void finishCapture(MinecraftServer server, UUID casterId, UUID targetId) {
        ServerPlayer caster = server.getPlayerList().getPlayer(casterId);
        Entity target = findEntity(server, targetId);
        if (caster == null || target == null || !target.isAlive() || !(target instanceof LivingEntity)) {
            return;
        }
        PrisonData data = PrisonData.get(server);
        if (data.contains(targetId)) {
            return;
        }
        NarutoBridge.Vars vars = NarutoBridge.of(caster);
        boolean free = caster.isCreative() || caster.isSpectator();
        int cost = KamuiPrisonJutsu.totalChakraCost();
        if (vars == null || (!free && vars.getDouble("chakra") < cost)) {
            caster.displayClientMessage(Component.translatable("text.notenoughchakra"), true);
            return;
        }

        boolean ok = target instanceof ServerPlayer targetPlayer
                ? imprisonPlayer(server, data, caster, targetPlayer)
                : imprisonMob(data, caster, target);
        if (!ok) {
            return;
        }
        if (!free) {
            vars.setDouble("chakra", vars.getDouble("chakra") - cost);
            vars.sync();
        }
        caster.displayClientMessage(Component.translatable("message.kamuiprison.captured",
                target.getName().getString()), true);
    }

    private static boolean imprisonMob(PrisonData data, ServerPlayer caster, Entity mob) {
        mob.stopRiding();
        mob.ejectPassengers();
        if (mob instanceof Mob m && m.isLeashed()) {
            m.dropLeash(true, true);
        }
        CompoundTag tag = new CompoundTag();
        if (!mob.save(tag)) {
            caster.displayClientMessage(Component.translatable("message.kamuiprison.cannot_seal"), true);
            return false;
        }
        Prisoner prisoner = new Prisoner(mob.getUUID(), false);
        prisoner.name = mob.getName().getString();
        prisoner.typeName = mob.getType().getDescription().getString();
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        prisoner.typeId = typeKey == null ? "" : typeKey.toString();
        prisoner.entityData = tag;
        prisoner.owner = caster.getUUID();
        prisoner.ownerName = caster.getGameProfile().getName();
        prisoner.capturedAt = caster.server.overworld().getGameTime();

        // 1) grava no armazenamento persistente, 2) so entao remove do mundo.
        data.add(prisoner);
        mob.discard();
        return true;
    }

    private static boolean imprisonPlayer(MinecraftServer server, PrisonData data, ServerPlayer caster,
                                          ServerPlayer target) {
        ServerLevel prison = PrisonCell.level(server);
        if (prison == null) {
            caster.displayClientMessage(Component.translatable("message.kamuiprison.dimension_missing"), true);
            return false;
        }
        Prisoner prisoner = new Prisoner(target.getUUID(), true);
        prisoner.name = target.getGameProfile().getName();
        prisoner.typeName = "";
        prisoner.typeId = "minecraft:player";
        prisoner.owner = caster.getUUID();
        prisoner.ownerName = caster.getGameProfile().getName();
        prisoner.capturedAt = server.overworld().getGameTime();
        prisoner.gameMode = target.gameMode.getGameModeForPlayer().getId();
        data.add(prisoner);
        sendToPrison(server, target);
        return true;
    }

    private static void sendToPrison(MinecraftServer server, ServerPlayer player) {
        ServerLevel prison = PrisonCell.level(server);
        if (prison == null) {
            return;
        }
        PrisonCell.ensureBuilt(prison);
        player.stopRiding();
        player.teleportTo(prison, PrisonCell.SPAWN_X, PrisonCell.SPAWN_Y, PrisonCell.SPAWN_Z, player.getYRot(), 0.0F);
        player.setGameMode(GameType.ADVENTURE);
        player.fallDistance = 0.0F;
    }

    private static boolean isUnderGenjutsu(ServerPlayer player) {
        for (String id : new String[]{"genjutsu", "genjutsu_2"}) {
            MobEffect effect = NarutoAssets.effect(id);
            if (effect != null && player.hasEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================
    // Libertacao (modo Parcial)
    // =====================================================================================

    /** Liberta no local atual do usuario (dimensao e posicao onde ele esta agora). */
    public static ReleaseResult releaseToPlayer(ServerPlayer releaser, UUID id) {
        Vec3 look = releaser.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        Vec3 forward = horizontal.lengthSqr() > 1.0E-4 ? horizontal.normalize() : Vec3.ZERO;
        return releaseAt(releaser.server, id, releaser.serverLevel(), releaser.position(),
                forward, releaser.getYRot() + 180.0F);
    }

    public static ReleaseResult releaseAt(MinecraftServer server, UUID id, ServerLevel level, Vec3 pos,
                                          Vec3 forward, float yaw) {
        PrisonData data = PrisonData.get(server);
        Prisoner prisoner = data.get(id);
        if (prisoner == null) {
            return ReleaseResult.NOT_FOUND;
        }
        return prisoner.player
                ? releasePlayer(server, data, prisoner, level, pos, yaw)
                : releaseMob(data, prisoner, level, pos, forward, yaw);
    }

    private static ReleaseResult releaseMob(PrisonData data, Prisoner prisoner, ServerLevel level, Vec3 pos,
                                            Vec3 forward, float yaw) {
        if (prisoner.entityData == null) {
            return ReleaseResult.FAILED;
        }
        Optional<Entity> created = EntityType.create(prisoner.entityData.copy(), level);
        if (created.isEmpty()) {
            KamuiPrisonMod.LOGGER.warn("Nao foi possivel recriar o prisioneiro {} ({})", prisoner.name, prisoner.typeId);
            return ReleaseResult.FAILED;
        }
        Entity entity = created.get();

        Vec3 spot = pos.add(forward.scale(1.5));
        entity.setPos(spot.x, spot.y, spot.z);
        if (!level.noCollision(entity)) {
            spot = pos;
        }
        entity.moveTo(spot.x, spot.y, spot.z, yaw, 0.0F);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;

        // Remove o registro antes de adicionar (para o bloqueio anti-duplicacao nao barrar a propria entidade).
        data.remove(prisoner.id);
        boolean added = level.addFreshEntity(entity);
        if (!added) {
            entity.setUUID(UUID.randomUUID());
            added = level.addFreshEntity(entity);
        }
        if (!added) {
            data.add(prisoner);
            return ReleaseResult.FAILED;
        }
        spiral(level, entity);
        playSound(level, spot.x, spot.y, spot.z, "kamui_suck");
        return ReleaseResult.OK;
    }

    private static ReleaseResult releasePlayer(MinecraftServer server, PrisonData data, Prisoner prisoner,
                                               ServerLevel level, Vec3 pos, float yaw) {
        prisoner.pendingRelease = true;
        prisoner.pendingDim = level.dimension().location().toString();
        prisoner.pendingX = pos.x;
        prisoner.pendingY = pos.y;
        prisoner.pendingZ = pos.z;
        prisoner.pendingYaw = yaw;
        prisoner.pendingPitch = 0.0F;
        data.setDirty();

        ServerPlayer online = server.getPlayerList().getPlayer(prisoner.id);
        if (online == null) {
            return ReleaseResult.PENDING;
        }
        finishPendingRelease(server, data, prisoner, online);
        return ReleaseResult.OK;
    }

    private static void finishPendingRelease(MinecraftServer server, PrisonData data, Prisoner prisoner,
                                             ServerPlayer player) {
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(prisoner.pendingDim));
        ServerLevel dest = server.getLevel(dimKey);
        if (dest == null) {
            dest = server.overworld();
        }
        data.remove(prisoner.id);
        player.teleportTo(dest, prisoner.pendingX, prisoner.pendingY, prisoner.pendingZ,
                prisoner.pendingYaw, prisoner.pendingPitch);
        player.setGameMode(GameType.byId(prisoner.gameMode));
        player.fallDistance = 0.0F;
        spiral(dest, player);
        playSound(dest, prisoner.pendingX, prisoner.pendingY, prisoner.pendingZ, "kamui_suck");
        player.displayClientMessage(Component.translatable("message.kamuiprison.you_were_released"), true);
    }

    // =====================================================================================
    // Lista / interface
    // =====================================================================================

    public static List<PrisonerView> viewsFor(ServerPlayer viewer) {
        boolean shared = PrisonConfig.SHARED_PRISON.get();
        List<PrisonerView> views = new ArrayList<>();
        for (Prisoner p : PrisonData.get(viewer.server).all()) {
            if (p.pendingRelease) {
                continue;
            }
            if (!shared && (p.owner == null || !p.owner.equals(viewer.getUUID()))) {
                continue;
            }
            views.add(new PrisonerView(p.id, p.name, p.typeName, p.player));
            if (views.size() >= 500) {
                break;
            }
        }
        return views;
    }

    /** Abre (ou atualiza) a interface de libertacao no cliente. */
    public static void openList(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new OpenPrisonPacket(viewsFor(player)));
    }

    /** Valida e executa um pedido de libertacao vindo da interface. */
    public static void handleReleaseRequest(ServerPlayer player, UUID id) {
        if (!KamuiPrisonJutsu.isPartialSelected(player)) {
            return;
        }
        boolean shared = PrisonConfig.SHARED_PRISON.get();
        Prisoner prisoner = PrisonData.get(player.server).get(id);
        if (prisoner != null && !shared && (prisoner.owner == null || !prisoner.owner.equals(player.getUUID()))) {
            prisoner = null;
        }
        if (prisoner == null || prisoner.pendingRelease) {
            player.displayClientMessage(Component.translatable("message.kamuiprison.not_found"), true);
            openList(player);
            return;
        }
        String name = prisoner.name;
        ReleaseResult result = releaseToPlayer(player, id);
        switch (result) {
            case OK -> player.displayClientMessage(Component.translatable("message.kamuiprison.released", name), true);
            case PENDING -> player.displayClientMessage(Component.translatable("message.kamuiprison.release_pending", name), true);
            case NOT_FOUND -> player.displayClientMessage(Component.translatable("message.kamuiprison.not_found"), true);
            case FAILED -> player.displayClientMessage(Component.translatable("message.kamuiprison.release_failed"), true);
        }
        openList(player);
    }

    // =====================================================================================
    // Manutencao (tick / login)
    // =====================================================================================

    /** Mantem jogadores presos na prisao e conclui libertacoes pendentes. Chamado periodicamente. */
    public static void tickEnforcement(MinecraftServer server) {
        PrisonData data = PrisonData.get(server);
        if (data.isEmpty()) {
            return;
        }
        for (Prisoner prisoner : data.all()) {
            if (!prisoner.player) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(prisoner.id);
            if (player == null || !player.isAlive()) {
                continue;
            }
            if (prisoner.pendingRelease) {
                finishPendingRelease(server, data, prisoner, player);
            } else if (player.level().dimension() != PrisonCell.DIMENSION) {
                sendToPrison(server, player);
            }
        }
    }

    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.server;
        PrisonData data = PrisonData.get(server);
        Prisoner prisoner = data.get(player.getUUID());
        if (prisoner == null || !prisoner.player) {
            return;
        }
        if (prisoner.pendingRelease) {
            finishPendingRelease(server, data, prisoner, player);
        } else if (player.level().dimension() != PrisonCell.DIMENSION) {
            sendToPrison(server, player);
        }
    }

    // =====================================================================================
    // Utilidades
    // =====================================================================================

    public static Entity findEntity(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            return player;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void spiral(ServerLevel level, Entity target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        SimpleParticleType kamui = NarutoAssets.particle("kamui_particle_2");
        if (kamui != null) {
            for (ServerPlayer viewer : level.players()) {
                if (viewer.distanceToSqr(x, y, z) <= 64.0 * 64.0) {
                    level.sendParticles(viewer, kamui, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
        double w = Math.max(0.3, target.getBbWidth() * 0.5);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 16, w, target.getBbHeight() * 0.4, w, 0.05);
    }

    private static void playSound(ServerLevel level, double x, double y, double z, String narutoSound) {
        SoundEvent sound = NarutoAssets.sound(narutoSound);
        if (sound == null) {
            sound = SoundEvents.ENDERMAN_TELEPORT;
        }
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, 2.0F, 1.0F);
    }
}

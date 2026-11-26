package com.kamuiprison.prison;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Registro persistente de um prisioneiro.
 * Mobs: o NBT completo da entidade fica guardado aqui e a entidade e removida do mundo.
 * Jogadores: o jogador fica fisicamente na dimensao de prisao; aqui ficam o modo de jogo anterior
 * e, se foi libertado estando offline, o destino da libertacao.
 */
public final class Prisoner {
    public final UUID id;
    public final boolean player;
    public String name;
    public String typeName;
    public String typeId;
    public CompoundTag entityData;
    public UUID owner;
    public String ownerName;
    public long capturedAt;

    public int gameMode;

    public boolean pendingRelease;
    public String pendingDim = "minecraft:overworld";
    public double pendingX;
    public double pendingY;
    public double pendingZ;
    public float pendingYaw;
    public float pendingPitch;

    public Prisoner(UUID id, boolean player) {
        this.id = id;
        this.player = player;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putBoolean("Player", player);
        tag.putString("Name", name == null ? "" : name);
        tag.putString("TypeName", typeName == null ? "" : typeName);
        tag.putString("TypeId", typeId == null ? "" : typeId);
        if (entityData != null) {
            tag.put("EntityData", entityData.copy());
        }
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName == null ? "" : ownerName);
        tag.putLong("CapturedAt", capturedAt);
        tag.putInt("GameMode", gameMode);
        tag.putBoolean("PendingRelease", pendingRelease);
        tag.putString("PendingDim", pendingDim);
        tag.putDouble("PendingX", pendingX);
        tag.putDouble("PendingY", pendingY);
        tag.putDouble("PendingZ", pendingZ);
        tag.putFloat("PendingYaw", pendingYaw);
        tag.putFloat("PendingPitch", pendingPitch);
        return tag;
    }

    public static Prisoner load(CompoundTag tag) {
        Prisoner p = new Prisoner(tag.getUUID("Id"), tag.getBoolean("Player"));
        p.name = tag.getString("Name");
        p.typeName = tag.getString("TypeName");
        p.typeId = tag.getString("TypeId");
        if (tag.contains("EntityData", 10)) {
            p.entityData = tag.getCompound("EntityData");
        }
        if (tag.hasUUID("Owner")) {
            p.owner = tag.getUUID("Owner");
        }
        p.ownerName = tag.getString("OwnerName");
        p.capturedAt = tag.getLong("CapturedAt");
        p.gameMode = tag.getInt("GameMode");
        p.pendingRelease = tag.getBoolean("PendingRelease");
        p.pendingDim = tag.getString("PendingDim");
        if (p.pendingDim.isEmpty()) {
            p.pendingDim = "minecraft:overworld";
        }
        p.pendingX = tag.getDouble("PendingX");
        p.pendingY = tag.getDouble("PendingY");
        p.pendingZ = tag.getDouble("PendingZ");
        p.pendingYaw = tag.getFloat("PendingYaw");
        p.pendingPitch = tag.getFloat("PendingPitch");
        return p;
    }
}

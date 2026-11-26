package com.kamuiprison.prison;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Armazenamento global dos prisioneiros. Fica no DimensionDataStorage do overworld
 * (data/kamuiprison_prisoners.dat), portanto e unico por mundo e independe da dimensao
 * onde o alvo foi capturado ou onde o usuario esta.
 */
public final class PrisonData extends SavedData {
    private static final String NAME = "kamuiprison_prisoners";

    private final Map<UUID, Prisoner> prisoners = new LinkedHashMap<>();

    public static PrisonData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PrisonData::load, PrisonData::new, NAME);
    }

    public static PrisonData load(CompoundTag tag) {
        PrisonData data = new PrisonData();
        ListTag list = tag.getList("Prisoners", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Prisoner p = Prisoner.load(list.getCompound(i));
            data.prisoners.put(p.id, p);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Prisoner p : prisoners.values()) {
            list.add(p.save());
        }
        tag.put("Prisoners", list);
        return tag;
    }

    public Prisoner get(UUID id) {
        return prisoners.get(id);
    }

    public boolean contains(UUID id) {
        return prisoners.containsKey(id);
    }

    /** True se o UUID pertence a um MOB preso (usado para impedir copias fantasma ao carregar chunks). */
    public boolean isMobPrisoner(UUID id) {
        Prisoner p = prisoners.get(id);
        return p != null && !p.player;
    }

    public void add(Prisoner prisoner) {
        prisoners.put(prisoner.id, prisoner);
        setDirty();
    }

    public Prisoner remove(UUID id) {
        Prisoner removed = prisoners.remove(id);
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    public boolean isEmpty() {
        return prisoners.isEmpty();
    }

    /** Copia da lista (seguro para modificar o mapa durante a iteracao). */
    public List<Prisoner> all() {
        return new ArrayList<>(prisoners.values());
    }
}

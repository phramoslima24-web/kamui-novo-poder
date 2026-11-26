package com.kamuiprison.prison;

import com.kamuiprison.KamuiPrisonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Dimensao de prisao do Kamui (kamuiprison:prison, definida em data/kamuiprison/dimension).
 * E um vazio com uma cela fechada construida automaticamente no primeiro uso.
 */
public final class PrisonCell {
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(KamuiPrisonMod.MOD_ID, "prison"));

    private static final int HALF = 8;
    private static final int FLOOR_Y = 99;
    private static final int CEILING_Y = 108;
    public static final double SPAWN_X = 0.5;
    public static final double SPAWN_Y = FLOOR_Y + 1;
    public static final double SPAWN_Z = 0.5;

    private PrisonCell() {
    }

    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIMENSION);
    }

    private static Block floorBlock(int x, int z) {
        int ring = Math.max(Math.abs(x), Math.abs(z));
        return ring % 2 == 0 ? Blocks.POLISHED_BLACKSTONE : Blocks.OBSIDIAN;
    }

    /** Constroi a cela se ainda nao existir (idempotente). */
    public static void ensureBuilt(ServerLevel level) {
        for (int cx = -1; cx <= 0; cx++) {
            for (int cz = -1; cz <= 0; cz++) {
                level.getChunk(cx, cz);
            }
        }
        BlockPos sentinel = new BlockPos(0, FLOOR_Y, 0);
        if (level.getBlockState(sentinel).is(floorBlock(0, 0))
                && level.getBlockState(new BlockPos(HALF, FLOOR_Y + 1, 0)).is(Blocks.BLACKSTONE)) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                boolean wall = Math.abs(x) == HALF || Math.abs(z) == HALF;
                level.setBlock(pos.set(x, FLOOR_Y, z), floorBlock(x, z).defaultBlockState(), 2);
                for (int y = FLOOR_Y + 1; y < CEILING_Y; y++) {
                    level.setBlock(pos.set(x, y, z),
                            wall ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
                boolean lamp = x % 4 == 0 && z % 4 == 0 && !wall;
                level.setBlock(pos.set(x, CEILING_Y, z),
                        (lamp ? Blocks.SEA_LANTERN : Blocks.BLACKSTONE).defaultBlockState(), 2);
            }
        }
    }
}

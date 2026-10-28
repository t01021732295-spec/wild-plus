package com.snolred.wildplus.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/** 귀환 목적지 계산: 리스폰 지점이 있으면 그곳, 없으면 월드 스폰 */
final class RecallTarget {
    private RecallTarget() {}

    static @Nullable ServerLevel level(ServerPlayer player) {
        return player.level().getServer().overworld();
    }

    static @Nullable BlockPos find(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        BlockPos spawn = overworld.getRespawnData().pos();
        return overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
    }
}

package com.snolred.wildplus.event;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 마을 회복: 플레이어 근처 마을에 빈 침대가 주민보다 많으면 2분마다 주민 한 명이 이사 온다.
 * 새 주민은 무직으로 오고, 근처의 빈 작업 블록(퇴비통·용광로 등)을 차지하면 직업을 얻는다.
 * (몬스터에게 주민이 다 잡아먹힌 바닐라 마을도 다시 살아난다)
 */
public final class VillageLife {
    private static final int RADIUS = 48;

    private VillageLife() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 2400 != 1200) return;
        ServerLevel level = event.getServer().overworld();
        for (ServerPlayer p : level.players()) {
            if (p.level().dimension() != Level.OVERWORLD) continue;
            PoiManager poi = level.getPoiManager();
            List<BlockPos> beds = poi.getInRange(h -> h.is(PoiTypes.HOME), p.blockPosition(), RADIUS, PoiManager.Occupancy.ANY)
                    .map(PoiRecord::getPos).toList();
            if (beds.size() < 2) continue;
            int villagers = level.getEntitiesOfClass(Villager.class, new AABB(p.blockPosition()).inflate(RADIUS + 16)).size();
            if (villagers >= beds.size()) continue;
            BlockPos bed = beds.get(level.getRandom().nextInt(beds.size()));
            BlockPos at = bed.above();
            if (!level.getBlockState(at).isAir() && !level.getBlockState(at).canBeReplaced()) at = bed.above(2);
            Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
            if (v == null) continue;
            v.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
            v.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(v);
        }
    }
}

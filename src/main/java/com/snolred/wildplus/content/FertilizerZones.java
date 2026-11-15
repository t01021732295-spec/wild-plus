package com.snolred.wildplus.content;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.0.8 비료 효과 구역: 비료 하나를 쓰면 1분 동안 9×9(위아래 1칸) 안의 모든 작물 · 묘목이 빨리 자란다.
 * 1초마다 구역의 작물마다 비료 등급만큼 확률로 "자라기 틱"을 한 번 더 준다 (계절 작물은 계절 규칙 그대로).
 * 시작할 때 구역의 경작지 전부에 영양(품질)을 남긴다. 저장하지 않음 (서버를 껐다 켜면 남은 시간은 사라짐).
 */
public final class FertilizerZones {
    private FertilizerZones() {}

    public static final int RADIUS = 4, SECONDS = 60, MAX_ZONES = 64;
    /** 등급별 1초마다 작물 한 칸이 추가로 자랄 기회 (일반 · 고급 · 황금) */
    private static final float[] CHANCE = {0F, 0.30F, 0.45F, 0.60F};

    private record Zone(ServerLevel level, BlockPos center, int tier, long until) {}

    private static final List<Zone> ZONES = new ArrayList<>();

    /** 비료를 쓴 자리: 구역 시작 + 경작지 영양. 효과가 생겼으면 true */
    public static boolean start(ServerLevel level, BlockPos center, int tier) {
        boolean any = false;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS), center.offset(RADIUS, 1, RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.FARMLAND)) {
                Soil.fertilize(level, p.immutable(), tier);
                any = true;
            } else if (s.is(BlockTags.CROPS) || s.is(BlockTags.SAPLINGS)) {
                any = true;
            }
        }
        if (!any) return false;
        if (ZONES.size() >= MAX_ZONES) ZONES.remove(0);
        ZONES.add(new Zone(level, center.immutable(), Math.max(1, Math.min(3, tier)), level.getGameTime() + SECONDS * 20L));
        ring(level, center);
        return true;
    }

    private static void ring(ServerLevel level, BlockPos c) {
        for (int i = 0; i <= RADIUS * 2; i += 2) {
            for (int[] d : new int[][] {{i - RADIUS, -RADIUS}, {i - RADIUS, RADIUS}, {-RADIUS, i - RADIUS}, {RADIUS, i - RADIUS}}) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.getX() + d[0] + 0.5, c.getY() + 1.1, c.getZ() + d[1] + 0.5, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (ZONES.isEmpty() || event.getServer().getTickCount() % 20 != 9) return;
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            ServerLevel lv = z.level();
            long now = lv.getGameTime();
            if (now >= z.until() || !lv.isLoaded(z.center())) {
                it.remove();
                continue;
            }
            var r = lv.getRandom();
            for (BlockPos p : BlockPos.betweenClosed(z.center().offset(-RADIUS, -1, -RADIUS), z.center().offset(RADIUS, 1, RADIUS))) {
                BlockState s = lv.getBlockState(p);
                if (!(s.is(BlockTags.CROPS) || s.is(BlockTags.SAPLINGS)) || r.nextFloat() >= CHANCE[z.tier()]) continue;
                s.randomTick(lv, p, r);
                if (r.nextInt(4) == 0) lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 1, 0.25, 0.2, 0.25, 0);
            }
            if ((z.until() - now) / 20 % 10 == 0) ring(lv, z.center());
        }
    }
}

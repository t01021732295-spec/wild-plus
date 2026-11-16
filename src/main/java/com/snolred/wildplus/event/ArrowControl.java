package com.snolred.wildplus.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.2.1 화살 렉 줄이기
 * · 회수할 수 없는 화살(다중 발사 여분 · 몬스터 화살 · 무한)이 땅에 꽂혀 멈추면 2~4초 뒤 사라짐 (바닐라는 1분)
 */
public final class ArrowControl {
    private ArrowControl() {}

    private static final String POS = "wp_arrow_pos";

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 40 != 0) return;
        for (ServerLevel lv : server.getAllLevels()) {
            for (var e : lv.getAllEntities()) {
                if (!(e instanceof AbstractArrow a) || a.pickup == AbstractArrow.Pickup.ALLOWED || a.tickCount < 20) continue;
                var tag = a.getPersistentData();
                Vec3 pos = a.position();
                long key = Double.doubleToLongBits(pos.x) * 31 + Double.doubleToLongBits(pos.y) * 17 + Double.doubleToLongBits(pos.z);
                if (tag.getLongOr(POS, 0L) == key && a.getDeltaMovement().lengthSqr() < 0.01) a.discard();
                else tag.putLong(POS, key);
            }
        }
    }
}

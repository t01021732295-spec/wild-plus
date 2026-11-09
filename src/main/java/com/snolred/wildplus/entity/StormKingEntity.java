package com.snolred.wildplus.entity;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 4.7 탑 보스 폭풍의 기사왕: 벼락 창 · 검의 윤무 · 폭풍 소용돌이 · 천상의 심판(초록칸만 안전) */
public class StormKingEntity extends FxBoss {
    private int pull;
    private Vec3 safe;
    private int safeLeft;

    public StormKingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 900.0).add(Attributes.ATTACK_DAMAGE, 14.0).add(Attributes.ARMOR, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void skills(ServerLevel lv) {
        List<ServerPlayer> all = near(lv, 48);
        if (all.isEmpty()) return;
        var r = getRandom();
        int t = tickCount;
        float base = 11F;
        if (t % 140 == 0) {
            ServerPlayer p = all.get(r.nextInt(all.size()));
            Vec3 dir = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            for (int i = 1; i <= 8; i++) {
                Vec3 q = position().add(dir.scale(i * 3.0));
                warn(lv, q.x, q.y, q.z, 2.4, base, 16 + i * 4, BOLT);
            }
            lv.playSound(null, blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 1.5F, 1.1F);
        }
        if (t % 260 == 120) warn(lv, getX(), getY(), getZ(), 10.0, base * 1.2F, 40, RING);
        if (t % 400 == 300) pull = 50;
        if (pull > 0) {
            pull--;
            if (pull % 5 == 0) for (ServerPlayer p : near(lv, 26)) {
                p.setDeltaMovement(position().subtract(p.position()).normalize().scale(0.55).add(0, 0.05, 0));
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
            for (int i = 0; i < 6; i++) {
                double a = tickCount * 0.3 + i, d = 3 + (50 - pull) * 0.2;
                lv.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * d, getY() + i * 2.2, getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0);
            }
            if (pull == 0) warn(lv, getX(), getY(), getZ(), 6.0, base * 1.5F, 6, BOOM);
        }
        if (t % 600 == 500 && getHealth() / getMaxHealth() < 0.6F) {
            int sx = r.nextInt(3), sz = r.nextInt(3);
            for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) {
                double x = getX() + (a - 1) * 8, z = getZ() + (b - 1) * 8;
                if (a == sx && b == sz) {
                    safe = new Vec3(x, ground(lv, x, getY(), z), z);
                    safeLeft = 60;
                } else {
                    warn(lv, x, getY(), z, 4.4, base * 2F, 60, BOLT);
                }
            }
            for (ServerPlayer p : all) p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("천상의 심판! 초록빛 칸으로!").withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
        }
        if (safeLeft > 0) {
            safeLeft--;
            if (safeLeft % 3 == 0) {
                ring(lv, safe, 4.0, ParticleTypes.HAPPY_VILLAGER, 0.3);
                lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, safe.x, safe.y + 0.4, safe.z, 8, 2.5, 0.1, 2.5, 0);
            }
        }
    }
}

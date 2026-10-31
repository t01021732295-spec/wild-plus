package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModEffects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 블러드문 클리어 보상: 블러드문이 시작된 뒤 한 번도 죽지 않고 정예 몹을 5마리 이상 잡은 채 아침을 맞으면
 * 30분 동안 흡혈(준 피해의 20% 회복) + 재생 I + 힘 II.
 */
public final class BloodMoonReward {
    public static final int NEEDED = 5;
    public static final int DURATION = 20 * 60 * 30;
    private static final Set<UUID> DIED = new HashSet<>();
    private static final Map<UUID, Integer> KILLS = new HashMap<>();

    private BloodMoonReward() {}

    static void reset() {
        DIED.clear();
        KILLS.clear();
    }

    static void reward(ServerLevel level) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            int kills = KILLS.getOrDefault(p.getUUID(), 0);
            if (!DIED.contains(p.getUUID()) && kills >= NEEDED) {
                p.addEffect(new MobEffectInstance(ModEffects.LIFESTEAL, DURATION, 0));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION, 0));
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, DURATION, 1));
                p.sendSystemMessage(Component.translatable("message.wildplus.blood_moon.clear", kills).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else if (kills > 0 || DIED.contains(p.getUUID())) {
                p.sendSystemMessage(Component.translatable(DIED.contains(p.getUUID()) ? "message.wildplus.blood_moon.fail_died"
                        : "message.wildplus.blood_moon.fail_kills", kills, NEEDED).withStyle(ChatFormatting.GRAY));
            }
        }
        reset();
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !BloodMoon.isActive(level)) return;
        if (dead instanceof ServerPlayer p) {
            DIED.add(p.getUUID());
            return;
        }
        if (EliteMobs.isElite(dead) && event.getSource().getEntity() instanceof ServerPlayer killer) {
            int n = KILLS.merge(killer.getUUID(), 1, Integer::sum);
            BloodLord.onEliteKilled(level, killer);
            killer.sendOverlayMessage(Component.translatable("message.wildplus.blood_moon.progress", n, NEEDED)
                    .withStyle(n >= NEEDED ? ChatFormatting.GOLD : ChatFormatting.RED));
        }
    }

    /** 흡혈 효과: 직접 때린 피해의 20%만큼 회복 */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p
                && p.hasEffect(ModEffects.LIFESTEAL) && p.getHealth() < p.getMaxHealth()) {
            float heal = Math.min(event.getHealthDamage() * 0.2F, 4.0F);
            if (heal > 0) p.heal(heal);
        }
    }
}

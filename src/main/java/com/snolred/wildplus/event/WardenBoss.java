package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModItems25;
import com.snolred.wildplus.registry.ModItems37;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 5.2.1 워든 보스화 (고대 도시): 모습은 바닐라 워든 그대로, 체력 2배 · 보스바 · 패턴 추가.
 * · 음파 고리: 바깥으로 퍼지는 고리 3겹 (예고 후 피해 12)
 * · 지면 균열: 가장 가까운 사람 쪽으로 이어지는 폭발 6개 (피해 16)
 * · 어둠의 포효: 주변 모두 어둠 + 워든 쪽으로 끌어당김
 * · 분노(체력 50% 이하): 빨라지고 패턴 간격 짧아짐, 고리 피해 18
 * 보상: 근처 모두에게 고대 각인석 1개 + 메아리 조각, 30% 확률로 각인 확장석
 */
public final class WardenBoss {
    private WardenBoss() {}

    private static final Identifier HP = Identifier.fromNamespaceAndPath("wildplus", "warden_boss_hp"), SPD = Identifier.fromNamespaceAndPath("wildplus", "warden_boss_rage");
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<UUID, Warden> LIVE = new HashMap<>();

    /** 예고 장판: 고리(r 근처만) 또는 원 */
    private record Warn(ServerLevel lv, Vec3 at, double r, boolean ring, float dmg, long fire) {}

    private static final List<Warn> WARNS = new ArrayList<>();

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Warden w) || !(event.getLevel() instanceof ServerLevel)) return;
        var hp = w.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(HP) == null) {
            hp.addPermanentModifier(new AttributeModifier(HP, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if (!event.loadedFromDisk()) w.setHealth(w.getMaxHealth());
        }
        LIVE.put(w.getUUID(), w);
    }

    private static List<ServerPlayer> near(Warden w, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : ((ServerLevel) w.level()).players())
            if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(w) < r * r) out.add(p);
        return out;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        for (Iterator<Map.Entry<UUID, Warden>> it = LIVE.entrySet().iterator(); it.hasNext(); ) {
            Warden w = it.next().getValue();
            if (w.isRemoved() || !w.isAlive()) {
                it.remove();
                ServerBossEvent b = BARS.remove(w.getUUID());
                if (b != null) b.removeAllPlayers();
                continue;
            }
            tickBoss(w, now);
        }
        tickWarns(now);
    }

    private static void tickBoss(Warden w, long now) {
        ServerLevel lv = (ServerLevel) w.level();
        List<ServerPlayer> all = near(w, 40);
        ServerBossEvent bar = BARS.computeIfAbsent(w.getUUID(), k -> new ServerBossEvent(UUID.randomUUID(),
                Component.literal("고대의 워든").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10));
        bar.setProgress(Mth.clamp(w.getHealth() / w.getMaxHealth(), 0F, 1F));
        if (now % 10 == 0) {
            List<ServerPlayer> show = near(w, 56);
            for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (!show.contains(p)) bar.removePlayer(p);
            for (ServerPlayer p : show) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        }
        if (all.isEmpty() || w.getTarget() == null) return;
        boolean rage = w.getHealth() < w.getMaxHealth() * 0.5F;
        var spd = w.getAttribute(Attributes.MOVEMENT_SPEED);
        if (rage && spd != null && spd.getModifier(SPD) == null) {
            spd.addPermanentModifier(new AttributeModifier(SPD, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            lv.playSound(null, w.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4F, 0.6F);
            for (ServerPlayer p : all) p.sendSystemMessage(Component.literal("🔊 고대의 워든이 분노합니다!").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        }
        int t = w.tickCount;
        int k = rage ? 7 : 10; // 분노하면 간격 70%
        // 음파 고리
        if (t % (12 * k) == 0) {
            for (int i = 0; i < 3; i++) WARNS.add(new Warn(lv, w.position(), 5 + i * 4, true, rage ? 18F : 12F, now + 30 + i * 10));
            lv.playSound(null, w.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3F, 0.8F);
        }
        // 지면 균열
        if (t % (20 * k) == 7 * k) {
            ServerPlayer p = all.get(0);
            for (ServerPlayer q : all) if (q.distanceToSqr(w) < p.distanceToSqr(w)) p = q;
            Vec3 dir = new Vec3(p.getX() - w.getX(), 0, p.getZ() - w.getZ()).normalize();
            for (int i = 1; i <= 6; i++) WARNS.add(new Warn(lv, w.position().add(dir.scale(i * 2.5)), 2.2, false, 16F, now + 20 + i * 3L));
            lv.playSound(null, w.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 3F, 0.6F);
        }
        // 어둠의 포효: 어둠 + 끌어당김
        if (t % (30 * k) == 15 * k) {
            lv.playSound(null, w.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4F, 0.5F);
            lv.sendParticles(ParticleTypes.SCULK_SOUL, w.getX(), w.getY() + 2, w.getZ(), 80, 4, 1.5, 4, 0.05);
            for (ServerPlayer p : all) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                Vec3 pull = w.position().subtract(p.position()).normalize().scale(1.1);
                p.push(pull.x, 0.35, pull.z);
                p.hurtMarked = true;
            }
        }
    }

    private static void tickWarns(long now) {
        for (Iterator<Warn> it = WARNS.iterator(); it.hasNext(); ) {
            Warn a = it.next();
            if (now % 4 == 0) {
                int n = (int) Math.max(10, a.r() * 8);
                for (int i = 0; i < n; i++) {
                    double ang = Math.PI * 2 * i / n;
                    a.lv().sendParticles(ParticleTypes.SCULK_CHARGE_POP, a.at().x + Math.cos(ang) * a.r(), a.at().y + 0.15, a.at().z + Math.sin(ang) * a.r(), 1, 0, 0, 0, 0);
                }
            }
            if (now < a.fire()) continue;
            it.remove();
            a.lv().sendParticles(a.ring() ? ParticleTypes.SONIC_BOOM : ParticleTypes.EXPLOSION, a.at().x, a.at().y + 0.5, a.at().z, a.ring() ? 3 : 1, a.r() * 0.5, 0.2, a.r() * 0.5, 0);
            for (ServerPlayer p : a.lv().players()) {
                if (p.isSpectator() || p.isCreative() || Math.abs(p.getY() - a.at().y) > 3) continue;
                double d = Math.hypot(p.getX() - a.at().x, p.getZ() - a.at().z);
                boolean in = a.ring() ? d > a.r() - 1.6 && d < a.r() + 0.8 && p.onGround() : d < a.r();
                if (in) p.hurtServer(a.lv(), a.lv().damageSources().sonicBoom(p), a.dmg());
            }
        }
        if (WARNS.size() > 300) WARNS.clear();
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Warden w) || !(w.level() instanceof ServerLevel lv)) return;
        ServerBossEvent b = BARS.remove(w.getUUID());
        if (b != null) b.removeAllPlayers();
        LIVE.remove(w.getUUID());
        String[] ks = {"EXECUTE", "SONIC", "BULWARK", "ECHO"};
        var r = lv.getRandom();
        List<ServerPlayer> all = near(w, 48);
        for (ServerPlayer p : all) {
            drop(lv, w.position(), new ItemStack(ModItems25.STONES.get(ks[r.nextInt(ks.length)]).get()));
            if (r.nextFloat() < 0.3F) drop(lv, w.position(), new ItemStack(ModItems37.ENGRAVE_EXPANDER.get()));
            p.sendSystemMessage(Component.literal("✦ 고대의 워든을 쓰러뜨렸어요! 고대 각인석을 얻었어요").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        }
        drop(lv, w.position(), new ItemStack(Items.ECHO_SHARD, 4 + r.nextInt(5)));
    }

    private static void drop(ServerLevel lv, Vec3 at, ItemStack s) {
        ItemEntity e = new ItemEntity(lv, at.x, at.y + 1, at.z, s);
        e.setUnlimitedLifetime();
        lv.addFreshEntity(e);
    }
}

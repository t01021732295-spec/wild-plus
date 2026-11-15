package com.snolred.wildplus.entity;

import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModItems34;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 5.1 겁화의 군주 (성채 본성 보스).
 * 1단계: 향로의 불비(발밑 장판) · 대검 내려찍기(앞으로 이어지는 폭발)
 * 2단계(66%↓): + 겁화 파동(퍼져 나가는 고리 — 뛰어넘기) · 잿불 사냥개 소환
 * 3단계(33%↓): 태양 핵 개방 — 3초 뒤 본성 전체 폭발, 기둥 뒤에 숨어야 산다 · 받는 피해 +30% · 빨라짐
 * 보상: 근처 모두에게 군주의 뿔 1개 + 겁화석 · 잿불 파편
 */
public class KalpaLordEntity extends FxBoss {
    private static final double BASE_HP = 1200.0;
    private final ServerBossEvent bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.literal("겁화의 군주").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
    private int core = -1;
    private int lastPhase = 1;

    public KalpaLordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, BASE_HP).add(Attributes.ATTACK_DAMAGE, 18.0).add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    public static void summon(ServerLevel level, BlockPos altar) {
        KalpaLordEntity b = ModEntities.KALPA_LORD.get().create(level, EntitySpawnReason.EVENT);
        if (b == null) return;
        int n = Math.max(1, level.players().stream().filter(p -> p.blockPosition().closerThan(altar, 48) && !p.isSpectator()).toList().size());
        double want = BASE_HP * (1.0 + 0.35 * (n - 1));
        var hp = b.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(want);
        b.setHealth(b.getMaxHealth());
        b.getPersistentData().putFloat("wp_lord_scale", (float) Math.min(1.0, b.getMaxHealth() / want));
        b.snapTo(altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 180F, 0F);
        b.setCustomName(Component.literal("겁화의 군주").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        level.addFreshEntity(b);
        level.playSound(null, altar, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.FLAME, altar.getX() + 0.5, altar.getY() + 2.0, altar.getZ() + 0.5, 300, 2.5, 3.0, 2.5, 0.2);
        level.sendParticles(ParticleTypes.LAVA, altar.getX() + 0.5, altar.getY() + 1.0, altar.getZ() + 0.5, 80, 3.0, 0.5, 3.0, 0.2);
        for (ServerPlayer p : level.players())
            if (p.blockPosition().closerThan(altar, 64))
                p.sendSystemMessage(Component.literal("🔥 겁화의 군주가 깨어났습니다!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    public float damageScale() {
        float s = getPersistentData().getFloatOr("wp_lord_scale", 1F);
        return phase() == 3 ? s * 1.3F : s;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(double d, float m, DamageSource s) {
        return false;
    }

    private int phase() {
        float f = getHealth() / getMaxHealth();
        return f > 0.66F ? 1 : f > 0.33F ? 2 : 3;
    }

    @Override
    protected void skills(ServerLevel lv) {
        tickBar(lv);
        List<ServerPlayer> all = near(lv, 40);
        if (all.isEmpty()) return;
        int ph = phase();
        if (ph != lastPhase) {
            lastPhase = ph;
            lv.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
            String msg = ph == 2 ? "군주의 날개가 타오릅니다 — 겁화 파동을 뛰어넘으세요!" : "태양 핵이 열렸습니다! 핵이 터지기 전에 기둥 뒤로!";
            for (ServerPlayer p : all) p.sendSystemMessage(Component.literal("🔥 " + msg).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            if (ph == 3) {
                var sp = getAttribute(Attributes.MOVEMENT_SPEED);
                if (sp != null) sp.setBaseValue(0.32);
            }
        }
        int t = tickCount;
        var r = getRandom();
        // 향로의 불비: 모두의 발밑
        if (t % (ph == 1 ? 110 : 90) == 0) {
            for (ServerPlayer p : all) warn(lv, p.getX(), p.getY(), p.getZ(), 3.2, 11F, 34, LAVA);
            lv.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        // 대검 내려찍기: 가장 가까운 사람 쪽으로 6칸 연속 폭발
        if (t % 150 == 70) {
            ServerPlayer p = all.get(0);
            for (ServerPlayer q : all) if (q.distanceToSqr(this) < p.distanceToSqr(this)) p = q;
            Vec3 dir = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ()).normalize();
            for (int i = 1; i <= 6; i++) {
                Vec3 at = position().add(dir.scale(i * 2.6));
                warn(lv, at.x, getY(), at.z, 2.2, 16F, 22 + i * 3, ROCK);
            }
            lv.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        if (ph >= 2) {
            // 겁화 파동: 퍼져 나가는 고리 3겹
            if (t % 200 == 130) {
                for (int i = 0; i < 3; i++) warn(lv, getX(), getY(), getZ(), 4.0 + i * 4.5, 10F, 30 + i * 12, RING);
                lv.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 0.7F);
            }
            // 잿불 사냥개 소환 (최대 4마리)
            if (t % 320 == 250 && lv.getEntitiesOfClass(NetherMobs.Hound.class, new AABB(blockPosition()).inflate(24)).size() < 4) {
                for (int i = 0; i < 2; i++) {
                    var h = ModEntities.EMBER_HOUND.get().create(lv, EntitySpawnReason.MOB_SUMMONED);
                    if (h == null) continue;
                    double a = r.nextDouble() * Math.PI * 2;
                    h.snapTo(getX() + Math.cos(a) * 4, getY(), getZ() + Math.sin(a) * 4, r.nextFloat() * 360F, 0F);
                    lv.addFreshEntity(h);
                    lv.sendParticles(ParticleTypes.FLAME, h.getX(), h.getY() + 0.5, h.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
                }
            }
        }
        if (ph == 3) {
            if (core < 0 && t % 300 == 200) {
                core = 60;
                lv.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
                for (ServerPlayer p : all) p.sendSystemMessage(Component.literal("☀ 태양 핵이 달아오릅니다 — 3초 뒤 폭발! 기둥 뒤로 숨으세요").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            }
            if (core >= 0) coreTick(lv, all);
        }
    }

    /** 태양 핵: 3초 차오른 뒤, 군주가 보이는 사람에게 큰 피해 (블록에 가려지면 안전) */
    private void coreTick(ServerLevel lv, List<ServerPlayer> all) {
        Vec3 eye = position().add(0, 3.0, 0);
        if (core % 5 == 0) {
            lv.sendParticles(ParticleTypes.END_ROD, eye.x, eye.y, eye.z, 12, 0.6, 0.6, 0.6, 0.05);
            ring(lv, position(), 3.0 + (60 - core) / 6.0, ParticleTypes.FLAME, 3.0);
        }
        if (core-- > 0) return;
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, eye.x, eye.y, eye.z, 4, 3, 2, 3, 0);
        lv.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.5F);
        for (ServerPlayer p : all) {
            var hitRes = lv.clip(new ClipContext(eye, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hitRes.getType() == HitResult.Type.BLOCK) {
                p.sendOverlayMessage(Component.literal("기둥이 폭발을 막아 줬어요!").withStyle(ChatFormatting.GREEN));
                continue;
            }
            hit(lv, p, 34F);
            p.igniteForSeconds(6);
        }
        core = -1;
    }

    private void tickBar(ServerLevel lv) {
        bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0F, 1F));
        if (tickCount % 10 != 0) return;
        List<ServerPlayer> near = near(lv, 56);
        for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (!near.contains(p)) bar.removePlayer(p);
        for (ServerPlayer p : near) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bar.removeAllPlayers();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bar.removeAllPlayers();
        if (!(level() instanceof ServerLevel lv)) return;
        var r = getRandom();
        for (NetherMobs.Hound h : lv.getEntitiesOfClass(NetherMobs.Hound.class, new AABB(blockPosition()).inflate(32))) h.discard();
        List<ServerPlayer> all = near(lv, 64);
        for (ServerPlayer p : all) {
            drop(lv, new ItemStack(ModItems34.KALPA_HORN.get()));
            p.sendSystemMessage(Component.literal("✦ 겁화의 군주를 쓰러뜨렸어요! 군주의 뿔을 얻었어요").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            com.snolred.wildplus.content.Codex.add(p, "kalpa_lord", 1);
        }
        if (all.isEmpty()) drop(lv, new ItemStack(ModItems34.KALPA_HORN.get()));
        // 5.1.1 겁화 각인석: 한 개는 반드시, 사람마다 35% 더
        String[] ks = {"INFERNO", "PYRE", "CINDER", "PHOENIX"};
        int stones = 1;
        for (int k = 0; k < all.size(); k++) if (r.nextFloat() < 0.35F) stones++;
        for (int k = 0; k < stones; k++) drop(lv, new ItemStack(com.snolred.wildplus.registry.ModItems25.STONES.get(ks[r.nextInt(ks.length)]).get()));
        drop(lv, new ItemStack(ModItems34.KALPA_STONE.get(), 3 + r.nextInt(4)));
        drop(lv, new ItemStack(ModItems34.EMBER_SHARD.get(), 16 + r.nextInt(17)));
        drop(lv, new ItemStack(ModItems34.RAW_KALPA.get(), 4 + r.nextInt(5)));
    }

    private void drop(ServerLevel lv, ItemStack s) {
        ItemEntity e = new ItemEntity(lv, getX(), getY() + 1.0, getZ(), s);
        e.setUnlimitedLifetime();
        lv.addFreshEntity(e);
    }
}

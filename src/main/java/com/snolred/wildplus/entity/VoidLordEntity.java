package com.snolred.wildplus.entity;

import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModItems22;
import com.snolred.wildplus.registry.ModItems25;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 4.4 공허 군주: 1단계 수정 회복 · 예고 장판 / 2단계 끌어당겨 폭발 · 진드기 소환 / 3단계 조여 오는 공허 */
public class VoidLordEntity extends Monster {
    private static final double BASE_HP = 1500.0;
    private final ServerBossEvent bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.literal("공허 군주").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);
    private final List<EndCrystal> crystals = new ArrayList<>();
    private final List<Warn> warns = new ArrayList<>();
    private Vec3 home;
    private double ring = 34.0;
    private int pullBoom;
    private boolean crystalsMade;

    private static final class Warn {
        final Vec3 at;
        final double r;
        final float dmg;
        int left;

        Warn(Vec3 at, double r, float dmg, int left) {
            this.at = at;
            this.r = r;
            this.dmg = dmg;
            this.left = left;
        }
    }

    public VoidLordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, BASE_HP).add(Attributes.ATTACK_DAMAGE, 16.0).add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    /** 제단에서 소환 */
    public static void summon(ServerLevel level, BlockPos altar) {
        VoidLordEntity b = ModEntities.VOID_LORD.get().create(level, EntitySpawnReason.EVENT);
        if (b == null) return;
        int n = Math.max(1, level.players().stream().filter(p -> p.blockPosition().closerThan(altar, 64) && !p.isSpectator()).toList().size());
        var hp = b.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(BASE_HP * (1.0 + 0.3 * (n - 1)));
        b.setHealth(b.getMaxHealth());
        b.snapTo(altar.getX() + 0.5, altar.getY() + 4.0, altar.getZ() + 0.5, 0F, 0F);
        b.setCustomName(Component.literal("공허 군주").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        level.addFreshEntity(b);
        level.playSound(null, altar, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, altar.getX() + 0.5, altar.getY() + 3.0, altar.getZ() + 0.5, 400, 3.0, 3.0, 3.0, 0.3);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double d, float m, DamageSource s) {
        return false;
    }

    private int phase() {
        float f = getHealth() / getMaxHealth();
        return f > 0.66F ? 1 : f > 0.33F ? 2 : 3;
    }

    private List<ServerPlayer> near(ServerLevel level, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && !p.getAbilities().instabuild && p.distanceToSqr(this) < r * r) out.add(p);
        return out;
    }

    private void magic(ServerLevel level, ServerPlayer p, float amount) {
        int inv = p.getInvulnerableTime();
        p.setInvulnerableTime(0);
        p.hurtServer(level, level.damageSources().indirectMagic(this, this), amount);
        p.setInvulnerableTime(inv);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (home == null) home = position();
        tickBar(level);
        tickWarns(level);
        List<ServerPlayer> all = near(level, 64.0);
        if (all.isEmpty()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        ServerPlayer t = all.get(0);
        for (ServerPlayer p : all) if (p.distanceToSqr(this) < t.distanceToSqr(this)) t = p;
        setTarget(t);
        move(t);
        if (tickCount % 30 == 0 && distanceToSqr(t) < 36.0) {
            t.hurtServer(level, level.damageSources().mobAttack(this), 16.0F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 1.0, t.getZ(), 2, 0.5, 0.3, 0.5, 0);
        }
        int ph = phase();
        if (ph == 1) phaseOne(level, all);
        else phaseTwo(level, all, ph);
        if (ph == 3) closing(level, all);
    }

    private void move(ServerPlayer t) {
        Vec3 want = t.position().add(0, 3.0, 0);
        Vec3 d = want.subtract(position());
        double len = d.length();
        Vec3 v = len < 5.0 ? new Vec3(0, d.y * 0.05, 0) : d.normalize().scale(phase() == 3 ? 0.32 : 0.22);
        setDeltaMovement(v);
        setYRot((float) (Mth.atan2(t.getZ() - getZ(), t.getX() - getX()) * Mth.RAD_TO_DEG) - 90F);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void phaseOne(ServerLevel level, List<ServerPlayer> all) {
        if (!crystalsMade) {
            crystalsMade = true;
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i;
                EndCrystal c = EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.EVENT);
                if (c == null) continue;
                c.setPos(home.x + Math.cos(a) * 12, home.y + 2, home.z + Math.sin(a) * 12);
                c.setShowBottom(false);
                level.addFreshEntity(c);
                crystals.add(c);
            }
        }
        crystals.removeIf(c -> !c.isAlive());
        if (!crystals.isEmpty() && tickCount % 20 == 0) {
            heal(6.0F * crystals.size());
            for (EndCrystal c : crystals) {
                c.setBeamTarget(blockPosition());
            }
        }
        if (tickCount % 100 == 0) for (ServerPlayer p : all) warns.add(new Warn(p.position(), 4.0, 12.0F, 40));
    }

    private void phaseTwo(ServerLevel level, List<ServerPlayer> all, int ph) {
        for (EndCrystal c : crystals) if (c.isAlive()) c.discard();
        crystals.clear();
        int every = ph == 3 ? 90 : 120;
        if (tickCount % every == 0) {
            pullBoom = 30;
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 0.8F);
            for (ServerPlayer p : near(level, 24.0)) {
                Vec3 v = position().subtract(p.position()).normalize().scale(1.5);
                p.setDeltaMovement(v);
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
        }
        if (pullBoom > 0 && --pullBoom == 0) {
            warns.add(new Warn(position().subtract(0, getY() - groundY(level), 0), 7.0, 20.0F, 1));
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5F, 0.8F);
        }
        if (tickCount % 200 == 0 && level.getEntitiesOfClass(EndMobs.Mite.class, new AABB(blockPosition()).inflate(32.0)).size() < 8) {
            for (int i = 0; i < 3; i++) {
                Mob m = ModEntities.RIFT_MITE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (m == null) continue;
                m.setPos(getX() + getRandom().nextDouble() * 6 - 3, groundY(level) + 1, getZ() + getRandom().nextDouble() * 6 - 3);
                level.addFreshEntity(m);
            }
        }
    }

    private double groundY(ServerLevel level) {
        BlockPos p = blockPosition();
        for (int i = 0; i < 40; i++) {
            if (!level.getBlockState(p).isAir()) return p.getY() + 1.0;
            p = p.below();
        }
        return getY();
    }

    /** 3단계: 바깥부터 공허가 좁혀 든다 */
    private void closing(ServerLevel level, List<ServerPlayer> all) {
        ring = Math.max(9.0, ring - 0.03);
        if (tickCount % 10 == 0) {
            int n = (int) (ring * 3);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, home.x + Math.cos(a) * ring, home.y, home.z + Math.sin(a) * ring, 1, 0, 1.2, 0, 0);
            }
        }
        if (tickCount % 20 == 0) {
            for (ServerPlayer p : all) if (Math.hypot(p.getX() - home.x, p.getZ() - home.z) > ring) {
                magic(level, p, 6.0F);
                p.sendOverlayMessage(Component.literal("공허가 몸을 삼키고 있어요! 안쪽으로!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }
        }
    }

    private void tickWarns(ServerLevel level) {
        for (Iterator<Warn> it = warns.iterator(); it.hasNext(); ) {
            Warn w = it.next();
            if (w.left % 4 == 0) {
                int n = (int) (w.r * 12);
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    level.sendParticles(ParticleTypes.WITCH, w.at.x + Math.cos(a) * w.r, w.at.y + 0.2, w.at.z + Math.sin(a) * w.r, 1, 0, 0, 0, 0);
                }
            }
            if (--w.left > 0) continue;
            it.remove();
            for (ServerPlayer p : near(level, 96.0)) if (p.position().distanceToSqr(w.at) < w.r * w.r) magic(level, p, w.dmg);
            level.sendParticles(ParticleTypes.SONIC_BOOM, w.at.x, w.at.y + 0.5, w.at.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, w.at.x, w.at.y + 0.5, w.at.z, 60, w.r / 2, 0.4, w.r / 2, 0.15);
        }
    }

    private void tickBar(ServerLevel level) {
        bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0F, 1F));
        if (tickCount % 10 != 0) return;
        List<ServerPlayer> near = near(level, 72.0);
        for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (!near.contains(p)) bar.removePlayer(p);
        for (ServerPlayer p : near) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bar.removeAllPlayers();
        if (!(level() instanceof ServerLevel level)) return;
        for (EndCrystal c : crystals) if (c.isAlive()) c.discard();
        String[] v = {"ABYSS", "DEVOUR", "CHAIN", "WARP", "HEART"}, c = {"HARVEST", "FURY", "GUARD", "VIGOR", "RUSH"};
        var r = getRandom();
        for (ServerPlayer p : near(level, 72.0)) {
            drop(level, new ItemStack(ModItems25.STONES.get(v[r.nextInt(v.length)]).get()));
            p.sendSystemMessage(Component.literal("✦ 공허 군주를 쓰러뜨렸어요!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        drop(level, new ItemStack(ModItems25.STONES.get(c[r.nextInt(c.length)]).get()));
        drop(level, new ItemStack(ModItems25.LORD_SHARD.get(), 2 + r.nextInt(3)));
        drop(level, new ItemStack(ModItems22.VOID_HEART.get()));
        drop(level, new ItemStack(ModItems22.VOID_SHARD.get(), 16 + r.nextInt(17)));
    }

    private void drop(ServerLevel level, ItemStack s) {
        ItemEntity e = new ItemEntity(level, getX(), groundY(level) + 0.5, getZ(), s);
        e.setUnlimitedLifetime();
        level.addFreshEntity(e);
    }
}

package com.snolred.wildplus.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 4.7 보스 공용: 바닥 예고 → 발동. 종류별 이펙트는 fx 에서 그린다 */
public abstract class FxBoss extends Monster {
    public static final int BOLT = 1, LAVA = 2, ROCK = 3, ERUPT = 4, RING = 5, BOOM = 6;

    protected static final class Warn {
        final Vec3 at;
        final double r;
        final float dmg;
        final int kind;
        int left;

        Warn(Vec3 at, double r, float dmg, int left, int kind) {
            this.at = at;
            this.r = r;
            this.dmg = dmg;
            this.left = left;
            this.kind = kind;
        }
    }

    protected final List<Warn> warns = new ArrayList<>();

    private final boolean keep;

    protected FxBoss(EntityType<? extends Monster> type, Level level) {
        this(type, level, true);
    }

    /** keep=false: 자연 스폰 몹 (멀어지면 사라짐) */
    protected FxBoss(EntityType<? extends Monster> type, Level level, boolean keep) {
        super(type, level);
        this.keep = keep;
        if (keep) setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return !keep;
    }

    protected List<ServerPlayer> near(ServerLevel lv, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : lv.players()) if (!p.isSpectator() && !p.getAbilities().instabuild && p.distanceToSqr(this) < r * r) out.add(p);
        return out;
    }

    protected void hit(ServerLevel lv, ServerPlayer p, float amount) {
        int inv = p.getInvulnerableTime();
        p.setInvulnerableTime(0);
        p.hurtServer(lv, lv.damageSources().indirectMagic(this, this), amount);
        p.setInvulnerableTime(inv);
    }

    protected double ground(ServerLevel lv, double x, double y, double z) {
        BlockPos p = BlockPos.containing(x, y + 2, z);
        for (int i = 0; i < 16; i++) {
            if (!lv.getBlockState(p.below()).isAir()) return p.getY();
            p = p.below();
        }
        return y;
    }

    protected Warn warn(ServerLevel lv, double x, double y, double z, double r, float dmg, int left, int kind) {
        Warn w = new Warn(new Vec3(x, ground(lv, x, y, z), z), r, dmg, left, kind);
        warns.add(w);
        return w;
    }

    protected static void ring(ServerLevel lv, Vec3 at, double r, ParticleOptions p, double y) {
        int n = (int) Math.max(12, r * 12);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            lv.sendParticles(p, at.x + Math.cos(a) * r, at.y + y, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    protected static void column(ServerLevel lv, Vec3 at, double h, ParticleOptions a, ParticleOptions b) {
        for (double y = 0; y < h; y += 0.7) {
            lv.sendParticles(a, at.x, at.y + y, at.z, 3, 0.3, 0.2, 0.3, 0.01);
            lv.sendParticles(b, at.x, at.y + y, at.z, 1, 0.5, 0.2, 0.5, 0.02);
        }
    }

    protected static void bolt(ServerLevel lv, Vec3 at) {
        LightningBolt b = EntityTypes.LIGHTNING_BOLT.create(lv, EntitySpawnReason.EVENT);
        if (b != null) {
            b.setPos(at);
            b.setVisualOnly(true);
            lv.addFreshEntity(b);
        }
        column(lv, at, 14, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD);
    }

    private void fire(ServerLevel lv, Warn w) {
        switch (w.kind) {
            case BOLT -> bolt(lv, w.at);
            case LAVA -> {
                lv.sendParticles(ParticleTypes.LAVA, w.at.x, w.at.y + 0.3, w.at.z, 40, w.r * 0.6, 0.2, w.r * 0.6, 0.1);
                column(lv, w.at, 4, ParticleTypes.FLAME, ParticleTypes.LAVA);
            }
            case ROCK -> {
                lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, w.at.x, w.at.y + 0.5, w.at.z, 1, 0, 0, 0, 0);
                lv.sendParticles(ParticleTypes.LAVA, w.at.x, w.at.y + 0.5, w.at.z, 30, 1.2, 0.4, 1.2, 0.2);
            }
            case ERUPT -> column(lv, w.at, 24, ParticleTypes.FLAME, ParticleTypes.LAVA);
            case RING -> ring(lv, w.at, w.r, ParticleTypes.SONIC_BOOM, 1.0);
            default -> lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, w.at.x, w.at.y + 1, w.at.z, 2, 1, 0.5, 1, 0);
        }
    }

    private boolean inside(Warn w, ServerPlayer p) {
        double d = Math.hypot(p.getX() - w.at.x, p.getZ() - w.at.z);
        return w.kind == RING ? Math.abs(d - w.r) < 2.0 && Math.abs(p.getY() - w.at.y) < 4 : d < w.r && Math.abs(p.getY() - w.at.y) < 6;
    }

    protected void tickWarns(ServerLevel lv) {
        for (Iterator<Warn> it = new ArrayList<>(warns).iterator(); it.hasNext(); ) {
            Warn w = it.next();
            if (w.left % 4 == 0) ring(lv, w.at, w.r, w.kind == LAVA || w.kind == ROCK || w.kind == ERUPT ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK, 0.2);
            if (--w.left > 0) continue;
            warns.remove(w);
            fire(lv, w);
            for (ServerPlayer p : near(lv, 96.0)) if (inside(w, p)) hit(lv, p, w.dmg);
            afterFire(lv, w);
        }
    }

    /** 발동 후 추가 처리 */
    protected void afterFire(ServerLevel lv, Warn w) {}

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel lv && !isDeadOrDying()) {
            tickWarns(lv);
            skills(lv);
        }
    }

    protected abstract void skills(ServerLevel lv);
}

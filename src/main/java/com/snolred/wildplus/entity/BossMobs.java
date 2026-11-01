package com.snolred.wildplus.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;

/**
 * 레이드 · 하늘의 탑 전용 보스 몹. 바닐라 몹의 움직임(AI)과 모델을 그대로 쓰고,
 * 전용 텍스처(ModRenderers)와 몸 주변 이펙트로 생김새를 바꾼다. 고유 패턴은 Raid · Tower(BossSkills)에서.
 */
public final class BossMobs {
    private BossMobs() {}

    /** 몸 주변에 맴도는 이펙트 (서버에서 뿌려서 모두에게 보임) */
    static void aura(LivingEntity e, ParticleOptions a, ParticleOptions b, int every) {
        if (!(e.level() instanceof ServerLevel sl) || e.tickCount % every != 0 || !e.isAlive()) return;
        double w = e.getBbWidth() * 0.6, h = e.getBbHeight();
        sl.sendParticles(a, e.getX(), e.getY() + h * 0.5, e.getZ(), 3, w, h * 0.35, w, 0.01);
        if (b != null) sl.sendParticles(b, e.getX(), e.getY() + 0.1, e.getZ(), 2, w, 0.05, w, 0.0);
    }

    // ───────── 레이드 ─────────
    /** 대지의 거신: 용암이 갈라진 현무암 피부 */
    public static class Titan extends Ravager {
        public Titan(EntityType<? extends Ravager> type, Level level) {
            super(type, level);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.LAVA, ParticleTypes.LARGE_SMOKE, 6);
        }
    }

    /** 망령 군주: 검보라 로브 + 빛나는 해골 얼굴 */
    public static class Lich extends Evoker {
        public Lich(EntityType<? extends Evoker> type, Level level) {
            super(type, level);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.SOUL, ParticleTypes.SOUL_FIRE_FLAME, 4);
        }
    }

    /** 서리 여왕: 얼음빛 몸 + 얼음 왕관 */
    public static class FrostQueen extends Stray {
        public FrostQueen(EntityType<? extends Stray> type, Level level) {
            super(type, level);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.SNOWFLAKE, ParticleTypes.ITEM_SNOWBALL, 3);
        }
    }

    // ───────── 하늘의 탑 (테마마다 하나) ─────────
    /** 이끼 낀 회랑: 덩굴 집행자 */
    public static class VineExecutioner extends Vindicator {
        public VineExecutioner(EntityType<? extends Vindicator> type, Level level) {
            super(type, level);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.COMPOSTER, 5);
        }
    }

    /** 사막 신전: 모래 파라오 */
    public static class SandPharaoh extends Husk {
        public SandPharaoh(EntityType<? extends Husk> type, Level level) {
            super(type, level);
        }

        @Override
        protected boolean convertsInWater() {
            return false;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.WHITE_ASH, ParticleTypes.CRIT, 4);
        }
    }

    /** 지옥 요새: 화염 군주 */
    public static class FlameLord extends WitherSkeleton {
        public FlameLord(EntityType<? extends WitherSkeleton> type, Level level) {
            super(type, level);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.FLAME, ParticleTypes.LAVA, 3);
        }
    }

    /** 해저 궁전: 심해 수호자 */
    public static class AbyssWarden extends Drowned {
        public AbyssWarden(EntityType<? extends Drowned> type, Level level) {
            super(type, level);
        }

        @Override
        protected boolean convertsInWater() {
            return false;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.BUBBLE_POP, ParticleTypes.GLOW, 4);
        }
    }

    /** 끝의 도시: 공허의 기사 */
    public static class VoidKnight extends Vindicator {
        public VoidKnight(EntityType<? extends Vindicator> type, Level level) {
            super(type, level);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL, 3);
        }
    }

    /** 황금 왕좌: 황금 왕 */
    public static class GoldenKing extends Vindicator {
        public GoldenKing(EntityType<? extends Vindicator> type, Level level) {
            super(type, level);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            aura(this, ParticleTypes.WAX_ON, ParticleTypes.END_ROD, 5);
        }
    }
}

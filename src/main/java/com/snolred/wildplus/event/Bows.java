package com.snolred.wildplus.event;

import com.snolred.wildplus.item.WildBowItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 활 · 화살.
 * 활: 미스릴(당김 1.5배 · 피해 1.2배) · 루비(불화살 · 피해 1.35배, 불에 안 탐) ·
 *     스텔라 루비(당김 2배 · 피해 1.6배 · 화살 3발 · 맞으면 고정 피해 3 · 내구도 무한)
 * 화살: 폭발(맞은 곳 폭발, 블록은 안 부숨) · 유도(가까운 몬스터를 따라감) · 번개(맞은 곳에 번개) · 서리(구속 III · 얼림)
 */
public final class Bows {
    public static final String BOW_TAG = "wildplus_bow", ARROW_TAG = "wildplus_arrow", EXTRA = "wildplus_extra_arrow";
    /** 5.0.4 활 약탈: 화살에 새긴 단계 · 맞은 몹에 남기는 단계와 시각 */
    private static final String LOOT_TAG = "wildplus_bow_loot", MARK_LV = "wildplus_bl_lv", MARK_T = "wildplus_bl_t";
    private static final long MARK_TICKS = 200L;
    private static final List<UUID> HOMING = new ArrayList<>();
    private static final List<ServerLevel> HOMING_LEVEL = new ArrayList<>();

    private Bows() {}

    @SubscribeEvent
    public static void onLoose(ArrowLooseEvent event) {
        if (event.getBow().getItem() instanceof WildBowItem bow && bow.drawSpeed != 1.0F) {
            event.setCharge(Math.round(event.getCharge() * bow.drawSpeed));
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        var data = arrow.getPersistentData();
        // 5.0.4 활 약탈: 쏘는 순간 든 활의 단계를 화살에 새김 (스텔라 루비 곁화살도 주인이 같아서 같이 새겨짐)
        if (!data.contains(LOOT_TAG) && arrow.getOwner() instanceof net.minecraft.world.entity.player.Player shooter) {
            int lv = bowLooting(shooter);
            if (lv > 0) data.putInt(LOOT_TAG, lv);
        }
        String bow = data.getStringOr(BOW_TAG, "");
        if (bow.equals("ruby") || bow.equals("star_ruby")) arrow.igniteForSeconds(100);
        // 스카이 사파이어 활: 화살이 더 빠르고 곧게 날아감
        if (bow.equals("sky_sapphire") && !data.getBooleanOr("wildplus_fast", false)) {
            data.putBoolean("wildplus_fast", true);
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.35));
        }
        if (data.getStringOr(ARROW_TAG, "").equals("homing") && !HOMING.contains(arrow.getUUID())) {
            HOMING.add(arrow.getUUID());
            HOMING_LEVEL.add(level);
        }
        // 스텔라 루비 활: 양옆으로 화살 2발 더 (주울 수 없음)
        if (bow.equals("star_ruby") && !data.getBooleanOr(EXTRA, false) && arrow.getOwner() instanceof LivingEntity owner) {
            Vec3 v = arrow.getDeltaMovement();
            for (int side : new int[]{-1, 1}) {
                AbstractArrow extra = EntityTypes.ARROW.create(level, EntitySpawnReason.TRIGGERED);
                if (extra == null) continue;
                double ang = Math.toRadians(8 * side), cos = Math.cos(ang), sin = Math.sin(ang);
                extra.setOwner(owner);
                extra.snapTo(arrow.getX(), arrow.getY(), arrow.getZ(), arrow.getYRot(), arrow.getXRot());
                extra.setDeltaMovement(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
                extra.setBaseDamage(2.0 * 1.6);
                extra.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                extra.setCritArrow(arrow.isCritArrow());
                extra.getPersistentData().putString(BOW_TAG, "star_ruby");
                extra.getPersistentData().putBoolean(EXTRA, true);
                level.addFreshEntity(extra);
            }
        }
    }

    /** 유도 화살: 16칸 안의 가장 가까운 몬스터 쪽으로 방향을 조금씩 튼다 */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        for (int i = HOMING.size() - 1; i >= 0; i--) {
            ServerLevel level = HOMING_LEVEL.get(i);
            Entity e = level.getEntity(HOMING.get(i));
            if (!(e instanceof AbstractArrow a) || a.isRemoved() || a.tickCount > 200 || a.getDeltaMovement().lengthSqr() < 0.04) {
                HOMING.remove(i);
                HOMING_LEVEL.remove(i);
                continue;
            }
            if (a.tickCount < 3) continue;
            LivingEntity best = null;
            double bd = 16 * 16;
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(16))) {
                if (!(t instanceof Enemy) || !t.isAlive() || t == a.getOwner()) continue;
                double d = t.distanceToSqr(a);
                if (d < bd) {
                    bd = d;
                    best = t;
                }
            }
            if (best == null) continue;
            Vec3 v = a.getDeltaMovement();
            Vec3 to = best.position().add(0, best.getBbHeight() * 0.6, 0).subtract(a.position()).normalize().scale(v.length());
            a.setDeltaMovement(v.scale(0.75).add(to.scale(0.25)));
            if (a.tickCount % 2 == 0) level.sendParticles(ParticleTypes.WITCH, a.getX(), a.getY(), a.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow a) || !(a.level() instanceof ServerLevel level)) return;
        String kind = a.getPersistentData().getStringOr(ARROW_TAG, "");
        if (kind.isEmpty() || a.getPersistentData().getBooleanOr("wildplus_used", false)) return;
        Vec3 at = event.getRayTraceResult().getLocation();
        switch (kind) {
            case "explosive" -> {
                a.getPersistentData().putBoolean("wildplus_used", true);
                level.explode(a, at.x, at.y, at.z, 2.2F, Level.ExplosionInteraction.NONE);
                a.discard();
                event.setCanceled(true);
            }
            case "lightning" -> {
                a.getPersistentData().putBoolean("wildplus_used", true);
                LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.snapTo(at.x, at.y, at.z, 0F, 0F);
                    level.addFreshEntity(bolt);
                }
            }
            case "frost" -> {
                a.getPersistentData().putBoolean("wildplus_used", true);
                if (event.getRayTraceResult() instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity t) {
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
                    t.setTicksFrozen(t.getTicksRequiredToFreeze() + 100);
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 30, 0.5, 0.5, 0.5, 0.05);
                level.playSound(null, a.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            default -> {}
        }
    }

    /** 사파이어 활 화살: 구속 II · 얼림 / 스카이 사파이어: 빙결 폭발 (FrostBurst) */
    @SubscribeEvent
    public static void onSapphireHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow a)) return;
        String bow = a.getPersistentData().getStringOr(BOW_TAG, "");
        if (!bow.equals("sapphire") && !bow.equals("sky_sapphire")) return;
        LivingEntity t = event.getEntity();
        if (!(t.level() instanceof ServerLevel level)) return;
        if (bow.equals("sky_sapphire")) { // 3.9.8: 빙결 폭발 확정 (검보다 지속 짧음)
            if (a.getOwner() instanceof net.minecraft.world.entity.player.Player owner) FrostBurst.burst(level, owner, t, false);
            return;
        }
        t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 40));
        level.sendParticles(ParticleTypes.SNOWFLAKE, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 10, 0.4, 0.4, 0.4, 0.03);
    }

    /** 스텔라 루비 활 화살: 맞으면 방어 무시 고정 피해 3 */
    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow a) || !"star_ruby".equals(a.getPersistentData().getStringOr(BOW_TAG, ""))) return;
        LivingEntity t = event.getEntity();
        if (!t.isAlive() || !(t.level() instanceof ServerLevel level)) return;
        float left = t.getHealth() - 3.0F;
        if (left > 0) {
            t.setHealth(left);
        } else {
            t.setHealth(0.0F);
            t.die(event.getSource());
        }
        level.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.7, t.getZ(), 5, 0.2, 0.2, 0.2, 0.2);
    }

    // ───────── 3.9 활 약탈 (활 전용 약탈 인챈트) ─────────
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> BOW_LOOTING =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, com.snolred.wildplus.WildPlus.id("bow_looting"));

    /** 활 약탈 단계 (활을 든 손 기준, 없으면 0) */
    public static int bowLooting(net.minecraft.world.entity.player.Player p) {
        var reg = p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var h = reg.get(BOW_LOOTING);
        if (h.isEmpty()) return 0;
        int lv = 0;
        for (net.minecraft.world.item.ItemStack s : new net.minecraft.world.item.ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof net.minecraft.world.item.BowItem) {
                lv = Math.max(lv, net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h.get(), s));
            }
        }
        return lv;
    }

    /** 활 약탈이 새겨진 화살에 맞은 몹: 10초 동안 기록 (화살 뒤에 폭발 · 번개 · 서리 · 겁화 폭발로 죽어도 적용되게) */
    @SubscribeEvent
    public static void onLootMark(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow a)) return;
        int lv = a.getPersistentData().getIntOr(LOOT_TAG, 0);
        LivingEntity t = event.getEntity();
        if (lv <= 0 || t instanceof net.minecraft.world.entity.player.Player) return;
        var d = t.getPersistentData();
        long now = t.level().getGameTime();
        boolean fresh = now - d.getLongOr(MARK_T, -MARK_TICKS - 1) <= MARK_TICKS;
        d.putInt(MARK_LV, fresh ? Math.max(lv, d.getIntOr(MARK_LV, 0)) : lv);
        d.putLong(MARK_T, now);
    }

    /** 활 약탈 단계: 화살로 직접 잡았으면 그 화살에 새긴 단계, 아니면 10초 안에 활 약탈 화살에 맞은 기록 */
    private static int lootLevel(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        int lv = 0;
        if (event.getSource().getDirectEntity() instanceof AbstractArrow a) {
            lv = a.getPersistentData().getIntOr(LOOT_TAG, 0);
            if (lv <= 0 && !a.getPersistentData().contains(LOOT_TAG) && event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player p) lv = bowLooting(p);
        }
        var d = dead.getPersistentData();
        if (dead.level().getGameTime() - d.getLongOr(MARK_T, -MARK_TICKS - 1) <= MARK_TICKS) lv = Math.max(lv, d.getIntOr(MARK_LV, 0));
        return lv;
    }

    /** 활 약탈: 떨군 물건마다 0~단계 개 더 (바닐라 약탈과 같은 방식, 장비는 제외) */
    @SubscribeEvent
    public static void onArrowKillDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) return;
        int lv = lootLevel(event);
        if (lv <= 0) return;
        var rnd = event.getEntity().getRandom();
        for (net.minecraft.world.entity.item.ItemEntity it : event.getDrops()) {
            net.minecraft.world.item.ItemStack s = it.getItem();
            if (s.isDamageableItem() || s.getMaxStackSize() <= 1) continue;
            int add = rnd.nextInt(lv + 1);
            if (add > 0) s.setCount(Math.min(s.getMaxStackSize(), s.getCount() + add));
        }
    }
}

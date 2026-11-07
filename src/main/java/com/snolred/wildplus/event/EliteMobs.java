package com.snolred.wildplus.event;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 정예 몹 (3.6 개편): 자연 스폰된 몬스터가 난이도에 따라 정예가 된다 — 쉬움 10% · 보통 15% · 어려움 25%
 * (동굴처럼 하늘이 안 보이는 곳은 절반 · 블러드문은 2배).
 * 체력 3배 · 공격력 2배 · 방어 +6 · 강도 +2 · 밀침 저항 · 이동 +10% · 금색 이름표.
 * 특수 능력 1개 (블러드문 2개): 공용 능력 + 몬스터별 고유 능력 (고유 능력이 나올 확률 50%).
 *  공용: 화염 · 흡혈 · 가시 · 질풍 · 거인 · 맹독 · 소환사 · 철벽 · 폭발 · 빙결 · 광폭 · 재생 · 순간이동
 *  좀비 계열: 무리 대장 · 감염 / 활 · 쇠뇌: 명사수 · 연사 / 크리퍼: 대폭발 · 그림자 / 거미: 거미줄 · 도약
 *  엔더맨: 공간 왜곡 / 마녀: 저주 / 전사(변명자 · 위더 스켈레톤 · 피글린 야수): 회전 베기 · 갑옷 파괴
 * 원거리 · 폭발 · 물약 피해에도 공격 능력(화염 · 흡혈 · 맹독 · 빙결)이 적용된다.
 */
public final class EliteMobs {
    public static final String ELITE_TAG = "wildplus_elite";
    public static final String BLOOD_TAG = "wildplus_blood_moon";
    public static final String AFFIX_TAG = "wildplus_affix";
    private static final String SUMMON_STAGE = "wildplus_summon_stage";
    private static final String RAGED = "wildplus_raged";
    private static final String HORDE_PENDING = "wildplus_horde";
    private static final String LAST_HURT = "wildplus_last_hurt";
    private static final String VOLLEY_CHILD = "wildplus_volley";

    public enum Group { ANY, ZOMBIE, ARCHER, CREEPER, SPIDER, ENDERMAN, WITCH, WARRIOR }

    /** 특수 능력 (group = 그 몬스터 계열만) */
    public enum Affix {
        BURNING, VAMPIRIC, THORNS, SWIFT, GIANT, VENOM, SUMMONER, IRONCLAD, VOLATILE,
        FROST, BERSERK, REGENERATING, BLINK,
        HORDE(Group.ZOMBIE), INFECT(Group.ZOMBIE),
        SNIPER(Group.ARCHER), VOLLEY(Group.ARCHER),
        MEGA_BLAST(Group.CREEPER), STEALTH(Group.CREEPER),
        WEBBER(Group.SPIDER), LEAPER(Group.SPIDER),
        WARP(Group.ENDERMAN),
        HEXER(Group.WITCH),
        CLEAVER(Group.WARRIOR), SUNDER(Group.WARRIOR);

        public final Group group;

        Affix() {
            this(Group.ANY);
        }

        Affix(Group g) {
            this.group = g;
        }

        public String key() {
            return "affix.wildplus." + name().toLowerCase(Locale.ROOT);
        }
    }

    private EliteMobs() {}

    public static boolean isElite(LivingEntity entity) {
        return entity.getPersistentData().getBooleanOr(ELITE_TAG, false);
    }

    public static boolean hasAffix(LivingEntity e, Affix a) {
        return isElite(e) && ("," + e.getPersistentData().getStringOr(AFFIX_TAG, "")).contains("," + a.name() + ",");
    }

    public static Group groupOf(Entity e) {
        EntityType<?> t = e.getType();
        if (t == EntityType.ZOMBIE || t == EntityType.HUSK || t == EntityType.DROWNED || t == EntityType.ZOMBIE_VILLAGER
                || t == EntityType.ZOMBIFIED_PIGLIN) return Group.ZOMBIE;
        if (t == EntityType.SKELETON || t == EntityType.STRAY || t == EntityType.BOGGED || t == EntityType.PILLAGER) return Group.ARCHER;
        if (t == EntityType.CREEPER) return Group.CREEPER;
        if (t == EntityType.SPIDER || t == EntityType.CAVE_SPIDER) return Group.SPIDER;
        if (t == EntityType.ENDERMAN) return Group.ENDERMAN;
        if (t == EntityType.WITCH) return Group.WITCH;
        if (t == EntityType.VINDICATOR || t == EntityType.WITHER_SKELETON || t == EntityType.PIGLIN_BRUTE) return Group.WARRIOR;
        return Group.ANY;
    }

    /** 이 몬스터에게 의미 없는 공용 능력 */
    private static Set<Affix> useless(Group g) {
        return switch (g) {
            // 크리퍼는 스스로 터지므로 흡혈 · 소환 · 죽을 때 폭발 · 광폭이 소용없음
            case CREEPER -> EnumSet.of(Affix.VAMPIRIC, Affix.SUMMONER, Affix.VOLATILE, Affix.BERSERK);
            case ENDERMAN -> EnumSet.of(Affix.BLINK);
            default -> EnumSet.noneOf(Affix.class);
        };
    }

    /** 난이도별 정예 확률 */
    public static double baseChance(Difficulty d) {
        return switch (d) {
            case PEACEFUL -> 0.0;
            case EASY -> 0.10;
            case NORMAL -> 0.15;
            case HARD -> 0.25;
        };
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Monster) || event.isSpawnCancelled()) return;
        if (!(event.getLevel().getLevel() instanceof ServerLevel level)) return;
        EntitySpawnReason reason = event.getSpawnType();
        boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.EVENT
                || reason == EntitySpawnReason.REINFORCEMENT || reason == EntitySpawnReason.PATROL;
        boolean bloodMoon = BloodMoon.isActive(level);
        if (bloodMoon && natural) {
            mob.getPersistentData().putBoolean(BLOOD_TAG, true);
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60 * 10, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 10, 0));
        }
        double chance = baseChance(level.getDifficulty());
        if (!level.canSeeSky(mob.blockPosition())) chance *= 0.5;
        if (bloodMoon) chance = Math.min(0.6, chance * 2.0);
        if (natural && level.getRandom().nextDouble() < chance) makeElite(mob, bloodMoon ? 2 : 1);
    }

    public static void makeElite(Mob mob) {
        makeElite(mob, 1);
    }

    public static void makeElite(Mob mob, int affixes) {
        mob.getPersistentData().putBoolean(ELITE_TAG, true);
        addModifier(mob.getAttribute(Attributes.MAX_HEALTH), "elite_health", 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), "elite_damage", 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(mob.getAttribute(Attributes.ARMOR), "elite_armor", 6.0, AttributeModifier.Operation.ADD_VALUE);
        addModifier(mob.getAttribute(Attributes.ARMOR_TOUGHNESS), "elite_toughness", 2.0, AttributeModifier.Operation.ADD_VALUE);
        addModifier(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), "elite_knockback", 0.6, AttributeModifier.Operation.ADD_VALUE);
        addModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), "elite_speed", 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(mob.getAttribute(Attributes.FOLLOW_RANGE), "elite_follow", 16.0, AttributeModifier.Operation.ADD_VALUE);

        RandomSource rnd = mob.getRandom();
        Group g = groupOf(mob);
        Set<Affix> bad = useless(g);
        List<Affix> common = new ArrayList<>(), own = new ArrayList<>();
        for (Affix a : Affix.values()) {
            if (a.group == Group.ANY && !bad.contains(a)) common.add(a);
            else if (a.group == g && g != Group.ANY) own.add(a);
        }
        StringBuilder names = new StringBuilder();
        MutableComponent label = Component.empty();
        for (int i = 0; i < affixes && (!common.isEmpty() || !own.isEmpty()); i++) {
            List<Affix> from = !own.isEmpty() && (common.isEmpty() || rnd.nextBoolean()) ? own : common;
            Affix a = from.remove(rnd.nextInt(from.size()));
            names.append(a.name()).append(',');
            if (i > 0) label.append(Component.literal("·"));
            label.append(Component.translatable(a.key()));
            applyOnSpawn(mob, a);
        }
        mob.getPersistentData().putString(AFFIX_TAG, names.toString());
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.literal("[").append(label).append("] ")
                .append(Component.translatable("entity.wildplus.elite_prefix", mob.getType().getDescription()))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
    }

    private static void applyOnSpawn(Mob mob, Affix a) {
        switch (a) {
            case SWIFT -> addModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), "elite_swift", 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case GIANT -> {
                addModifier(mob.getAttribute(Attributes.SCALE), "elite_giant", 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                addModifier(mob.getAttribute(Attributes.MAX_HEALTH), "elite_giant_health", 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                addModifier(mob.getAttribute(Attributes.ATTACK_KNOCKBACK), "elite_giant_kb", 1.0, AttributeModifier.Operation.ADD_VALUE);
            }
            case IRONCLAD -> {
                addModifier(mob.getAttribute(Attributes.ARMOR), "elite_iron", 8.0, AttributeModifier.Operation.ADD_VALUE);
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            }
            case BURNING -> mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            case SNIPER -> addModifier(mob.getAttribute(Attributes.FOLLOW_RANGE), "elite_sniper_range", 16.0, AttributeModifier.Operation.ADD_VALUE);
            case HORDE -> mob.getPersistentData().putBoolean(HORDE_PENDING, true);
            case MEGA_BLAST -> setCreeperRadius(mob, 6);
            case STEALTH -> mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            case WEBBER, LEAPER -> addModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), "elite_spider", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            default -> {}
        }
    }

    /** 크리퍼 폭발 범위 (기본 3) */
    private static void setCreeperRadius(Mob mob, int r) {
        try {
            for (Class<?> c = mob.getClass(); c != null; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (f.getName().equals("explosionRadius") && f.getType() == int.class) {
                        f.setAccessible(true);
                        f.setInt(mob, r);
                        return;
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void addModifier(AttributeInstance attribute, String name, double amount, AttributeModifier.Operation op) {
        if (attribute == null) return;
        AttributeModifier modifier = new AttributeModifier(WildPlus.id(name), amount, op);
        if (!attribute.hasModifier(modifier.id())) attribute.addPermanentModifier(modifier);
    }

    // ───────── 피해 배율 (명사수 · 갑옷 파괴) ─────────
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !isElite(attacker)) return;
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof AbstractArrow && hasAffix(attacker, Affix.SNIPER)) event.setAmount(event.getAmount() * 1.6F);
        if (direct == attacker && hasAffix(attacker, Affix.SUNDER) && event.getEntity() instanceof Player) event.setAmount(event.getAmount() * 1.3F);
    }

    // ───────── 특수 능력 ─────────
    private static boolean cleaving;

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        float dmg = event.getHealthDamage();
        // 정예가 때렸을 때 (근접 · 화살 · 폭발 · 물약 모두)
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != target && isElite(attacker) && dmg > 0) {
            boolean melee = event.getSource().getDirectEntity() == attacker;
            if (hasAffix(attacker, Affix.BURNING)) target.igniteForSeconds(5.0F);
            if (hasAffix(attacker, Affix.VENOM)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 6, 1));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 3, 0));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 8, 1));
            }
            if (hasAffix(attacker, Affix.VAMPIRIC)) {
                attacker.heal(dmg + 2.0F);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, attacker.getX(), attacker.getY() + attacker.getBbHeight(), attacker.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
            }
            if (hasAffix(attacker, Affix.FROST)) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 3, 2));
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 100));
                level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            }
            if (hasAffix(attacker, Affix.INFECT)) {
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 10, 2));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 0));
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 4, 0));
            }
            if (hasAffix(attacker, Affix.SNIPER) && !melee) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 6, 0));
            if (hasAffix(attacker, Affix.WEBBER)) web(level, target.blockPosition());
            if (hasAffix(attacker, Affix.WARP) && target instanceof Player p) {
                for (int k = 0; k < 8; k++) {
                    double x = p.getX() + (p.getRandom().nextDouble() - 0.5) * 12, z = p.getZ() + (p.getRandom().nextDouble() - 0.5) * 12;
                    if (p.randomTeleport(x, p.getY() + p.getRandom().nextInt(5) - 2, z, true)) break;
                }
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            if (hasAffix(attacker, Affix.HEXER)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20 * 8, 0));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 4, 0));
                level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
            }
            if (hasAffix(attacker, Affix.CLEAVER) && melee && !cleaving) {
                cleaving = true;
                try {
                    for (Player other : level.getEntitiesOfClass(Player.class, target.getBoundingBox().inflate(3.0))) {
                        if (other != target && other.isAlive()) other.hurtServer(level, attacker.damageSources().mobAttack(attacker), dmg * 0.6F);
                    }
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 3, 1.0, 0.2, 1.0, 0.0);
                } finally {
                    cleaving = false;
                }
            }
            if (hasAffix(attacker, Affix.SUNDER) && melee && target instanceof Player p) {
                for (net.minecraft.world.entity.EquipmentSlot s : new net.minecraft.world.entity.EquipmentSlot[]{
                        net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                        net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                    ItemStack armor = p.getItemBySlot(s);
                    if (!armor.isEmpty() && armor.isDamageableItem()) armor.hurtAndBreak(4, p, s);
                }
            }
        }
        // 정예가 맞았을 때
        if (isElite(target) && target instanceof Mob mob) {
            mob.getPersistentData().putLong(LAST_HURT, level.getGameTime());
            if (hasAffix(target, Affix.THORNS) && event.getSource().getDirectEntity() instanceof LivingEntity hitter
                    && hitter != target && !event.getSource().is(net.minecraft.tags.DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
                hitter.hurtServer(level, target.damageSources().thorns(target), Math.max(2.0F, dmg * 0.4F));
            }
            float hp = target.getHealth() / target.getMaxHealth();
            if (hasAffix(target, Affix.SUMMONER)) {
                int stage = mob.getPersistentData().getIntOr(SUMMON_STAGE, 0);
                if ((stage == 0 && hp < 0.75F) || (stage == 1 && hp < 0.35F)) {
                    mob.getPersistentData().putInt(SUMMON_STAGE, stage + 1);
                    summon(level, mob, 2);
                }
            }
            if (hasAffix(target, Affix.BERSERK) && hp < 0.5F && !mob.getPersistentData().getBooleanOr(RAGED, false)) {
                mob.getPersistentData().putBoolean(RAGED, true);
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, MobEffectInstance.INFINITE_DURATION, 1));
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, 1));
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
                level.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8F, 1.4F);
            }
            if (hasAffix(target, Affix.BLINK) && event.getSource().getEntity() instanceof Player p && mob.getRandom().nextFloat() < 0.3F) {
                Vec3 behind = p.position().subtract(p.getLookAngle().multiply(2.0, 0, 2.0));
                level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.3, 0.6, 0.3, 0.2);
                if (mob.randomTeleport(behind.x, p.getY(), behind.z, true)) {
                    level.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.3F);
                }
            }
        }
    }

    private static void summon(ServerLevel level, Mob mob, int n) {
        for (int i = 0; i < n; i++) {
            Entity made = mob.getType().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (!(made instanceof Mob minion)) continue;
            minion.snapTo(mob.getX() + (i == 0 ? 1.5 : -1.5), mob.getY(), mob.getZ(), mob.getYRot(), 0.0F);
            minion.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            minion.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0));
            if (mob.getTarget() != null) minion.setTarget(mob.getTarget());
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 0.5, minion.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    // ───────── 거미줄 (5초 뒤 사라짐) ─────────
    private record Web(ServerLevel level, BlockPos pos, long until) {}

    private static final List<Web> WEBS = new ArrayList<>();

    private static void web(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return;
        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
        WEBS.add(new Web(level, pos.immutable(), level.getGameTime() + 100));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Web> it = WEBS.iterator(); it.hasNext(); ) {
            Web w = it.next();
            if (w.level.getGameTime() < w.until) continue;
            if (w.level.getBlockState(w.pos).is(Blocks.COBWEB)) w.level.removeBlock(w.pos, false);
            it.remove();
        }
    }

    // ───────── 매 초 능력 (재생 · 그림자 · 도약 · 무리 대장) ─────────
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.tickCount % 20 != 7 || !(mob.level() instanceof ServerLevel level) || !isElite(mob)) return;
        var tag = mob.getPersistentData();
        if (tag.getBooleanOr(HORDE_PENDING, false)) {
            tag.putBoolean(HORDE_PENDING, false);
            summon(level, mob, 3);
        }
        if (hasAffix(mob, Affix.REGENERATING) && level.getGameTime() - tag.getLongOr(LAST_HURT, 0L) > 60 && mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(mob.getMaxHealth() * 0.04F);
            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        }
        if (hasAffix(mob, Affix.STEALTH)) {
            Player near = level.getNearestPlayer(mob, 6.0);
            if (near != null) mob.removeEffect(MobEffects.INVISIBILITY);
            else if (!mob.hasEffect(MobEffects.INVISIBILITY)) mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
        if (hasAffix(mob, Affix.LEAPER) && mob.tickCount % 60 == 7 && mob.getTarget() != null && mob.onGround()) {
            LivingEntity t = mob.getTarget();
            double d = mob.distanceTo(t);
            if (d > 3 && d < 12) {
                Vec3 v = t.position().subtract(mob.position()).normalize();
                mob.setDeltaMovement(v.x * 1.2, 0.55, v.z * 1.2);
                mob.hurtMarked = true;
                level.playSound(null, mob.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.7F);
            }
        }
    }

    // ───────── 연사 · 화염 화살 ─────────
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!(arrow.getOwner() instanceof Mob owner) || !isElite(owner) || arrow.getPersistentData().getBooleanOr(VOLLEY_CHILD, false)) return;
        if (hasAffix(owner, Affix.BURNING)) arrow.igniteForSeconds(100);
        if (!hasAffix(owner, Affix.VOLLEY)) return;
        Vec3 v = arrow.getDeltaMovement();
        for (int side : new int[]{-1, 1}) {
            Entity made = arrow.getType().create(level, EntitySpawnReason.TRIGGERED);
            if (!(made instanceof AbstractArrow extra)) continue;
            double ang = Math.toRadians(9 * side);
            Vec3 r = new Vec3(v.x * Math.cos(ang) - v.z * Math.sin(ang), v.y, v.x * Math.sin(ang) + v.z * Math.cos(ang));
            extra.setOwner(owner);
            extra.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
            extra.setDeltaMovement(r);
            extra.setBaseDamage(arrow.getBaseDamage());
            extra.pickup = AbstractArrow.Pickup.DISALLOWED;
            extra.getPersistentData().putBoolean(VOLLEY_CHILD, true);
            if (hasAffix(owner, Affix.BURNING)) extra.igniteForSeconds(100);
            level.addFreshEntity(extra);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level() instanceof ServerLevel level && hasAffix(e, Affix.VOLATILE)) {
            level.explode(e, e.getX(), e.getY() + 0.5, e.getZ(), 3.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        }
    }

    // ───────── 보상 ─────────
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster) || !(entity.level() instanceof ServerLevel level)) return;
        if (!(event.getSource().getEntity() instanceof Player)) return;
        RandomSource random = level.getRandom();
        Collection<ItemEntity> drops = event.getDrops();
        if (isElite(entity)) {
            boolean blood = BloodMoon.isActive(level) || entity.getPersistentData().getBooleanOr(BLOOD_TAG, false);
            add(drops, entity, new ItemStack(ModItems.BLOOD_SHARD.get(), 2 + random.nextInt(3)));
            add(drops, entity, new ItemStack(com.snolred.wildplus.registry.ModItems3.SILVER_COIN.get(), 1 + random.nextInt(3)));
            if (random.nextFloat() < 0.45F) add(drops, entity, new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(3)));
            if (random.nextFloat() < 0.18F) add(drops, entity, new ItemStack(Items.DIAMOND));
            if (random.nextFloat() < 0.08F) add(drops, entity, new ItemStack(ModItems.RECALL_SCROLL.get()));
            if (random.nextFloat() < 0.02F) add(drops, entity, new ItemStack(ModItems.LUCKY_CHARM.get()));
            // 3.6: 생명의 정수 (로열 에메랄드 재료) — 정예 3% · 블러드문 정예 6%
            if (random.nextFloat() < (blood ? 0.06F : 0.03F)) {
                add(drops, entity, new ItemStack(com.snolred.wildplus.registry.ModItems19.LIFE_ESSENCE.get()));
            }
        } else if (BloodMoon.isActive(level) || entity.getPersistentData().getBooleanOr(BLOOD_TAG, false)) {
            if (random.nextFloat() < 0.30F) add(drops, entity, new ItemStack(ModItems.BLOOD_SHARD.get()));
        }
    }

    private static void add(Collection<ItemEntity> drops, LivingEntity entity, ItemStack stack) {
        ItemEntity item = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        drops.add(item);
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (isElite(entity)) event.setDroppedExperience(event.getDroppedExperience() * 4);
        else if (BloodMoon.isActive(entity.level())) event.setDroppedExperience(event.getDroppedExperience() * 2);
    }
}

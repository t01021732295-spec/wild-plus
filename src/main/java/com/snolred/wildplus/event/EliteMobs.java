package com.snolred.wildplus.event;

import com.snolred.wildplus.WildConfig;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModItems;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/**
 * 정예 몹: 자연 스폰된 몬스터가 일정 확률로 강화된다. (9차 강화)
 * 체력 3배, 공격력 2배, 방어력 +6 · 방어 강도 +2, 밀쳐내기 저항, 이동 속도 +10%, 금색 이름표.
 * 특수 능력(접두어) 1개, 블러드문 중에는 2개:
 *  화염 · 흡혈 · 가시 · 질풍 · 거인 · 맹독 · 소환사 · 철벽 · 폭발
 * 처치 시 핏빛 파편 · 동전 · 금 · 다이아 등 보상.
 */
public final class EliteMobs {
    public static final String ELITE_TAG = "wildplus_elite";
    public static final String BLOOD_TAG = "wildplus_blood_moon";
    public static final String AFFIX_TAG = "wildplus_affix";
    private static final String SUMMONED_TAG = "wildplus_summoned";

    /** 특수 능력 */
    public enum Affix { BURNING, VAMPIRIC, THORNS, SWIFT, GIANT, VENOM, SUMMONER, IRONCLAD, VOLATILE;
        public String key() { return "affix.wildplus." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static boolean hasAffix(LivingEntity e, Affix a) {
        return isElite(e) && e.getPersistentData().getStringOr(AFFIX_TAG, "").contains(a.name());
    }

    private EliteMobs() {}

    public static boolean isElite(LivingEntity entity) {
        return entity.getPersistentData().getBooleanOr(ELITE_TAG, false);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Monster) || event.isSpawnCancelled()) {
            return;
        }
        if (!(event.getLevel().getLevel() instanceof ServerLevel level)) {
            return;
        }
        EntitySpawnReason reason = event.getSpawnType();
        boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.EVENT
                || reason == EntitySpawnReason.REINFORCEMENT || reason == EntitySpawnReason.PATROL;
        boolean bloodMoon = BloodMoon.isActive(level);

        if (bloodMoon && natural) {
            mob.getPersistentData().putBoolean(BLOOD_TAG, true);
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60 * 10, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 10, 0));
        }

        double chance = WildConfig.ELITE_CHANCE.get() * (bloodMoon ? 3.0 : 1.0);
        if (natural && level.getRandom().nextDouble() < chance) {
            makeElite(mob, bloodMoon ? 2 : 1);
        }
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
        // 특수 능력
        RandomSource rnd = mob.getRandom();
        java.util.List<Affix> pool = new java.util.ArrayList<>(java.util.List.of(Affix.values()));
        StringBuilder names = new StringBuilder();
        net.minecraft.network.chat.MutableComponent label = Component.empty();
        for (int i = 0; i < affixes && !pool.isEmpty(); i++) {
            Affix a = pool.remove(rnd.nextInt(pool.size()));
            names.append(a.name()).append(',');
            if (i > 0) label.append(Component.literal("·"));
            label.append(Component.translatable(a.key()));
            switch (a) {
                case SWIFT -> addModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), "elite_swift", 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                case GIANT -> {
                    addModifier(mob.getAttribute(Attributes.SCALE), "elite_giant", 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                    addModifier(mob.getAttribute(Attributes.MAX_HEALTH), "elite_giant_health", 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                }
                case IRONCLAD -> {
                    addModifier(mob.getAttribute(Attributes.ARMOR), "elite_iron", 8.0, AttributeModifier.Operation.ADD_VALUE);
                    mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
                }
                case BURNING -> mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
                default -> {}
            }
        }
        mob.getPersistentData().putString(AFFIX_TAG, names.toString());
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.literal("[").append(label).append("] ")
                .append(Component.translatable("entity.wildplus.elite_prefix", mob.getType().getDescription()))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
    }

    private static void addModifier(AttributeInstance attribute, String name, double amount, AttributeModifier.Operation op) {
        if (attribute == null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(WildPlus.id(name), amount, op);
        if (!attribute.hasModifier(modifier.id())) {
            attribute.addPermanentModifier(modifier);
        }
    }

    // ───────── 특수 능력 ─────────

    @SubscribeEvent
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        float dmg = event.getHealthDamage();
        // 정예가 때렸을 때
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isElite(attacker) && event.getSource().getDirectEntity() == attacker) {
            if (hasAffix(attacker, Affix.BURNING)) target.igniteForSeconds(4.0F);
            if (hasAffix(attacker, Affix.VENOM)) target.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 5, 1));
            if (hasAffix(attacker, Affix.VAMPIRIC)) attacker.heal(dmg * 0.5F);
        }
        // 정예가 맞았을 때
        if (isElite(target)) {
            if (hasAffix(target, Affix.THORNS) && event.getSource().getDirectEntity() instanceof LivingEntity hitter
                    && hitter != target && !event.getSource().is(net.minecraft.tags.DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
                hitter.hurtServer(level, target.damageSources().thorns(target), Math.max(1.0F, dmg * 0.3F));
            }
            if (hasAffix(target, Affix.SUMMONER) && target instanceof Mob mob && target.getHealth() < target.getMaxHealth() * 0.5F
                    && !target.getPersistentData().getBooleanOr(SUMMONED_TAG, false)) {
                target.getPersistentData().putBoolean(SUMMONED_TAG, true);
                for (int i = 0; i < 2; i++) {
                    Mob minion = (Mob) mob.getType().create(level, EntitySpawnReason.MOB_SUMMONED);
                    if (minion == null) continue;
                    minion.snapTo(mob.getX() + (i == 0 ? 1.5 : -1.5), mob.getY(), mob.getZ(), mob.getYRot(), 0.0F);
                    minion.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
                    if (mob.getTarget() != null) minion.setTarget(mob.getTarget());
                    level.addFreshEntity(minion);
                }
                level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_SUMMON,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level() instanceof ServerLevel level && hasAffix(e, Affix.VOLATILE)) {
            level.explode(e, e.getX(), e.getY() + 0.5, e.getZ(), 2.5F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        }
    }

    // ───────── 보상 ─────────

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        RandomSource random = level.getRandom();
        Collection<ItemEntity> drops = event.getDrops();

        if (isElite(entity)) {
            add(drops, entity, new ItemStack(ModItems.BLOOD_SHARD.get(), 2 + random.nextInt(3)));
            add(drops, entity, new ItemStack(com.snolred.wildplus.registry.ModItems3.SILVER_COIN.get(), 1 + random.nextInt(3)));
            if (random.nextFloat() < 0.45F) add(drops, entity, new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(3)));
            if (random.nextFloat() < 0.18F) add(drops, entity, new ItemStack(Items.DIAMOND));
            if (random.nextFloat() < 0.08F) add(drops, entity, new ItemStack(ModItems.RECALL_SCROLL.get()));
            if (random.nextFloat() < 0.02F) add(drops, entity, new ItemStack(ModItems.LUCKY_CHARM.get()));
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
        if (isElite(entity)) {
            event.setDroppedExperience(event.getDroppedExperience() * 4);
        } else if (BloodMoon.isActive(entity.level())) {
            event.setDroppedExperience(event.getDroppedExperience() * 2);
        }
    }
}

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
 * 정예 몹: 자연 스폰된 몬스터가 일정 확률로 강화된다.
 * 체력 2배, 공격력 +50%, 방어력 +4, 금색 이름표. 처치 시 핏빛 파편과 추가 보상.
 * 블러드문 중 나타난 몬스터는 힘·신속 버프를 받는다.
 */
public final class EliteMobs {
    public static final String ELITE_TAG = "wildplus_elite";
    public static final String BLOOD_TAG = "wildplus_blood_moon";

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
            makeElite(mob);
        }
    }

    public static void makeElite(Mob mob) {
        mob.getPersistentData().putBoolean(ELITE_TAG, true);
        addModifier(mob.getAttribute(Attributes.MAX_HEALTH), "elite_health", 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), "elite_damage", 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(mob.getAttribute(Attributes.ARMOR), "elite_armor", 4.0, AttributeModifier.Operation.ADD_VALUE);
        addModifier(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), "elite_knockback", 0.5, AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.translatable("entity.wildplus.elite_prefix", mob.getType().getDescription())
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
            add(drops, entity, new ItemStack(ModItems.BLOOD_SHARD.get(), 1 + random.nextInt(3)));
            if (random.nextFloat() < 0.35F) add(drops, entity, new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(3)));
            if (random.nextFloat() < 0.12F) add(drops, entity, new ItemStack(Items.DIAMOND));
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
            event.setDroppedExperience(event.getDroppedExperience() * 3);
        } else if (BloodMoon.isActive(entity.level())) {
            event.setDroppedExperience(event.getDroppedExperience() * 2);
        }
    }
}

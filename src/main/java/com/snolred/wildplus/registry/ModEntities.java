package com.snolred.wildplus.registry;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.entity.BanditEntity;
import com.snolred.wildplus.entity.GoldenSlimeEntity;
import com.snolred.wildplus.entity.MerchantEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(WildPlus.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MerchantEntity>> MERCHANT = ENTITIES.registerEntityType("merchant",
            (type, level) -> new MerchantEntity(type, level, MerchantEntity.Kind.GENERAL), MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MerchantEntity>> BLACKSMITH_MERCHANT = ENTITIES.registerEntityType("blacksmith_merchant",
            (type, level) -> new MerchantEntity(type, level, MerchantEntity.Kind.BLACKSMITH), MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MerchantEntity>> FARMER_MERCHANT = ENTITIES.registerEntityType("farmer_merchant",
            (type, level) -> new MerchantEntity(type, level, MerchantEntity.Kind.FARMER), MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MerchantEntity>> FISHER_MERCHANT = ENTITIES.registerEntityType("fisher_merchant",
            (type, level) -> new MerchantEntity(type, level, MerchantEntity.Kind.FISHERMAN), MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MerchantEntity>> BANKER_MERCHANT = ENTITIES.registerEntityType("banker_merchant",
            (type, level) -> new MerchantEntity(type, level, MerchantEntity.Kind.BANKER), MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<BanditEntity>> BANDIT = ENTITIES.registerEntityType("bandit",
            BanditEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<GoldenSlimeEntity>> GOLDEN_SLIME = ENTITIES.registerEntityType("golden_slime",
            GoldenSlimeEntity::new, MobCategory.MONSTER, b -> b.sized(0.52F, 0.52F).eyeHeight(0.325F).clientTrackingRange(10));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        var merchant = Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.MAX_HEALTH, 30.0).build();
        event.put(MERCHANT.get(), merchant);
        event.put(BLACKSMITH_MERCHANT.get(), merchant);
        event.put(FARMER_MERCHANT.get(), merchant);
        event.put(FISHER_MERCHANT.get(), merchant);
        event.put(BANKER_MERCHANT.get(), merchant);
        event.put(BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(GOLDEN_SLIME.get(), Monster.createMonsterAttributes().build());
    }

    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(BANDIT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GOLDEN_SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() < 10
                        && Monster.isDarkEnoughToSpawn(level, pos, random)
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEntities() {}
}

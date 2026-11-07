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

    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.CustomerEntity>> CUSTOMER = ENTITIES.registerEntityType("customer",
            com.snolred.wildplus.entity.CustomerEntity::new, MobCategory.MISC, b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.SeatEntity>> SEAT = ENTITIES.registerEntityType("seat",
            com.snolred.wildplus.entity.SeatEntity::new, MobCategory.MISC, b -> b.sized(0.01F, 0.01F).noSummon().clientTrackingRange(8));

    // ───────── 3.8 흉조: 우민 용병 · 우민 밀수꾼 ─────────
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.MercenaryEntity>> MERCENARY = ENTITIES.registerEntityType("illager_mercenary",
            com.snolred.wildplus.entity.MercenaryEntity::new, MobCategory.MISC, b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.SmugglerEntity>> SMUGGLER = ENTITIES.registerEntityType("illager_smuggler",
            com.snolred.wildplus.entity.SmugglerEntity::new, MobCategory.MISC, b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));

    // ───────── 보스 (레이드 3 · 하늘의 탑 6) ─────────
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.Titan>> TITAN_BOSS = ENTITIES.registerEntityType("titan_boss",
            com.snolred.wildplus.entity.BossMobs.Titan::new, MobCategory.MONSTER, b -> b.sized(1.95F, 2.2F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.Lich>> LICH_BOSS = ENTITIES.registerEntityType("lich_boss",
            com.snolred.wildplus.entity.BossMobs.Lich::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.FrostQueen>> FROST_BOSS = ENTITIES.registerEntityType("frost_boss",
            com.snolred.wildplus.entity.BossMobs.FrostQueen::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.99F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.VineExecutioner>> VINE_BOSS = ENTITIES.registerEntityType("vine_boss",
            com.snolred.wildplus.entity.BossMobs.VineExecutioner::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.SandPharaoh>> PHARAOH_BOSS = ENTITIES.registerEntityType("pharaoh_boss",
            com.snolred.wildplus.entity.BossMobs.SandPharaoh::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.FlameLord>> FLAME_BOSS = ENTITIES.registerEntityType("flame_boss",
            com.snolred.wildplus.entity.BossMobs.FlameLord::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.4F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.AbyssWarden>> ABYSS_BOSS = ENTITIES.registerEntityType("abyss_boss",
            com.snolred.wildplus.entity.BossMobs.AbyssWarden::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.VoidKnight>> VOID_BOSS = ENTITIES.registerEntityType("void_boss",
            com.snolred.wildplus.entity.BossMobs.VoidKnight::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.snolred.wildplus.entity.BossMobs.GoldenKing>> GOLD_BOSS = ENTITIES.registerEntityType("gold_boss",
            com.snolred.wildplus.entity.BossMobs.GoldenKing::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(10));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TITAN_BOSS.get(), net.minecraft.world.entity.monster.Ravager.createAttributes().build());
        event.put(LICH_BOSS.get(), net.minecraft.world.entity.monster.illager.Evoker.createAttributes().build());
        event.put(FROST_BOSS.get(), net.minecraft.world.entity.monster.skeleton.AbstractSkeleton.createAttributes().build());
        event.put(VINE_BOSS.get(), net.minecraft.world.entity.monster.illager.Vindicator.createAttributes().build());
        event.put(PHARAOH_BOSS.get(), net.minecraft.world.entity.monster.zombie.Husk.createAttributes().build());
        event.put(FLAME_BOSS.get(), net.minecraft.world.entity.monster.skeleton.AbstractSkeleton.createAttributes().build());
        event.put(ABYSS_BOSS.get(), net.minecraft.world.entity.monster.zombie.Drowned.createAttributes().build());
        event.put(VOID_BOSS.get(), net.minecraft.world.entity.monster.illager.Vindicator.createAttributes().build());
        event.put(GOLD_BOSS.get(), net.minecraft.world.entity.monster.illager.Vindicator.createAttributes().build());
        var merchant = Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.MAX_HEALTH, 30.0).build();
        event.put(MERCHANT.get(), merchant);
        event.put(BLACKSMITH_MERCHANT.get(), merchant);
        event.put(FARMER_MERCHANT.get(), merchant);
        event.put(FISHER_MERCHANT.get(), merchant);
        event.put(BANKER_MERCHANT.get(), merchant);
        event.put(BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(MERCENARY.get(), com.snolred.wildplus.entity.MercenaryEntity.createAttributes().build());
        event.put(SMUGGLER.get(), com.snolred.wildplus.entity.SmugglerEntity.createAttributes().build());
        event.put(GOLDEN_SLIME.get(), Monster.createMonsterAttributes().build());
        event.put(CUSTOMER.get(), Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 32.0).build());
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

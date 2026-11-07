package com.snolred.wildplus.event;

import com.snolred.wildplus.WildConfig;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.registry.ModEntities;
import com.snolred.wildplus.registry.ModItems3;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 랜덤 이벤트 32종. 평균 15분(설정)마다 하나가 일어나 몇 분 동안 이어진다. 보스바에 이름과 남은 시간.
 * 좋은 이벤트가 대부분이고, 습격·역병 같은 위험한 이벤트도 섞여 있다.
 */
public final class RandomEvents {
    public enum Type {
        GOLD_RUSH(6000, BossEvent.BossBarColor.YELLOW),
        METEOR_SHOWER(4800, BossEvent.BossBarColor.RED),
        HARVEST_FESTIVAL(6000, BossEvent.BossBarColor.GREEN),
        FISH_FRENZY(6000, BossEvent.BossBarColor.BLUE),
        MONSTER_RAID(3600, BossEvent.BossBarColor.RED),
        LUCKY_DAY(6000, BossEvent.BossBarColor.GREEN),
        MERCHANT_CARAVAN(6000, BossEvent.BossBarColor.YELLOW),
        SUPPLY_DROP(2400, BossEvent.BossBarColor.WHITE),
        SLIME_RAIN(3600, BossEvent.BossBarColor.YELLOW),
        XP_FESTIVAL(6000, BossEvent.BossBarColor.GREEN),
        MINING_FEVER(6000, BossEvent.BossBarColor.YELLOW),
        STORM(4800, BossEvent.BossBarColor.BLUE),
        PLAGUE(3600, BossEvent.BossBarColor.PURPLE),
        TAX_HOLIDAY(6000, BossEvent.BossBarColor.GREEN),
        BANK_BONUS(400, BossEvent.BossBarColor.YELLOW),
        TREASURE_HUNT(6000, BossEvent.BossBarColor.WHITE),
        ELITE_INVASION(3600, BossEvent.BossBarColor.RED),
        BLESSING(4800, BossEvent.BossBarColor.PINK),
        LOW_GRAVITY(3600, BossEvent.BossBarColor.WHITE),
        SWIFT_WIND(4800, BossEvent.BossBarColor.BLUE),
        COIN_RAIN(2400, BossEvent.BossBarColor.YELLOW),
        ANIMAL_MIGRATION(3600, BossEvent.BossBarColor.GREEN),
        ORE_BLESSING(6000, BossEvent.BossBarColor.PURPLE),
        WANTED(6000, BossEvent.BossBarColor.RED),
        // 13차 추가
        TREASURE_GOBLIN(3600, BossEvent.BossBarColor.YELLOW),
        ORE_RAIN(2400, BossEvent.BossBarColor.PURPLE),
        BLOOM(4800, BossEvent.BossBarColor.PINK),
        FROST_WIND(3600, BossEvent.BossBarColor.WHITE),
        HERO_TIME(4800, BossEvent.BossBarColor.RED),
        DOUBLE_DROPS(6000, BossEvent.BossBarColor.GREEN),
        FULL_MOON(6000, BossEvent.BossBarColor.PURPLE),
        VAMPIRE_NIGHT(4800, BossEvent.BossBarColor.RED),
        // 3.8 후반 이벤트 (누군가 통장 300만동 이상일 때부터 40% 확률로)
        OMEN_STORM(6000, BossEvent.BossBarColor.PURPLE),
        ILLAGER_ARMADA(6000, BossEvent.BossBarColor.RED),
        STARFALL(6000, BossEvent.BossBarColor.PURPLE),
        GOLDEN_VAULT(6000, BossEvent.BossBarColor.YELLOW),
        MARKET_BOOM(4800, BossEvent.BossBarColor.GREEN);

        public final int duration;
        public final BossEvent.BossBarColor color;

        Type(int duration, BossEvent.BossBarColor color) {
            this.duration = duration;
            this.color = color;
        }

        public String key() {
            return "event.wildplus." + name().toLowerCase(Locale.ROOT);
        }

        public boolean late() {
            return ordinal() >= OMEN_STORM.ordinal();
        }
    }

    private static final String TAG = "wildplus_event_mob", WANTED_TAG = "wildplus_wanted", GOBLIN_TAG = "wildplus_treasure_rabbit";

    /** 이벤트 투척병 상점에서 이름을 골라 살 수 있는 이벤트 (돈이 바로 생기는 이벤트·위험한 이벤트 일부는 제외) */
    public static final Type[] BOTTLE_SHOP = {Type.GOLD_RUSH, Type.HARVEST_FESTIVAL, Type.FISH_FRENZY, Type.LUCKY_DAY, Type.XP_FESTIVAL,
            Type.MINING_FEVER, Type.ORE_BLESSING, Type.BLESSING, Type.LOW_GRAVITY, Type.SWIFT_WIND, Type.METEOR_SHOWER, Type.MONSTER_RAID,
            Type.ELITE_INVASION, Type.STORM, Type.SLIME_RAIN, Type.TREASURE_GOBLIN, Type.ORE_RAIN, Type.BLOOM, Type.HERO_TIME,
            Type.DOUBLE_DROPS, Type.FULL_MOON, Type.VAMPIRE_NIGHT, Type.ANIMAL_MIGRATION, Type.MERCHANT_CARAVAN};

    public static Type random(RandomSource rnd) {
        List<Type> early = new ArrayList<>();
        for (Type t : Type.values()) if (!t.late()) early.add(t);
        return early.get(rnd.nextInt(early.size()));
    }

    /** 자동 이벤트 고르기: 후반(통장 300만동 이상인 사람이 있음)이면 40% 확률로 후반 이벤트 */
    private static Type pick(MinecraftServer server, RandomSource rnd) {
        if (com.snolred.wildplus.omen.Omen.late(server) && rnd.nextFloat() < 0.4F) {
            List<Type> late = new ArrayList<>();
            for (Type t : Type.values()) if (t.late()) late.add(t);
            return late.get(rnd.nextInt(late.size()));
        }
        return random(rnd);
    }

    private static final String ARMADA_TAG = "wildplus_armada";
    private static final List<UUID> ARMADA = new ArrayList<>();
    private static Type active;
    private static long endsAt, nextAt = -1;
    private static ServerBossEvent bar;
    private static final List<UUID> SPAWNED = new ArrayList<>();
    private static BlockPos target;

    private RandomEvents() {}

    public static boolean isActive(Type t) {
        return active == t;
    }

    public static Type active() {
        return active;
    }

    // ───────── 시작 · 끝 ─────────
    public static void start(MinecraftServer server, Type type) {
        if (active != null) stop(server);
        ServerLevel level = server.overworld();
        active = type;
        endsAt = level.getGameTime() + type.duration;
        target = null;
        bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable(type.key()).withStyle(ChatFormatting.BOLD),
                type.color, BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) bar.addPlayer(p);
        server.getPlayerList().broadcastSystemMessage(Component.literal("✦ ").append(Component.translatable(type.key()))
                .append(" · ").append(Component.translatable(type.key() + ".desc")).withStyle(ChatFormatting.AQUA), false);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        RandomSource rnd = level.getRandom();
        List<ServerPlayer> players = survivalPlayers(server);
        switch (type) {
            case STORM -> command(server, "weather thunder " + type.duration / 20);
            case BANK_BONUS -> {
                WildData data = WildData.get(server);
                for (var e : new ArrayList<>(data.bank.entrySet())) {
                    long bonus = Math.min(30_000, e.getValue() * 2 / 100); // 3.8: 상한 300 → 3만동
                    if (bonus <= 0) continue;
                    data.addBalance(e.getKey(), bonus);
                    ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                    if (p != null) p.sendSystemMessage(Component.translatable("message.wildplus.event.bank_bonus", bonus).withStyle(ChatFormatting.GOLD));
                }
            }
            case SUPPLY_DROP, TREASURE_HUNT -> {
                if (players.isEmpty()) break;
                ServerPlayer p = players.get(rnd.nextInt(players.size()));
                boolean buried = type == Type.TREASURE_HUNT;
                BlockPos pos = surface(level, p, buried ? 40 : 15, buried ? 90 : 35);
                if (pos == null) break;
                if (buried) pos = pos.below(3);
                level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                    chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, WildPlus.id(buried ? "gameplay/treasure_hunt" : "gameplay/supply_drop")),
                            rnd.nextLong());
                }
                if (!buried) level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
                target = pos;
                String where = buried ? String.format("%d, ?, %d (±8)", pos.getX() + rnd.nextInt(17) - 8, pos.getZ() + rnd.nextInt(17) - 8)
                        : String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
                server.getPlayerList().broadcastSystemMessage(Component.translatable(buried ? "message.wildplus.event.treasure"
                        : "message.wildplus.event.supply", where).withStyle(ChatFormatting.YELLOW), false);
            }
            case MERCHANT_CARAVAN -> {
                List<EntityType<? extends Mob>> kinds = List.of(ModEntities.MERCHANT.get(), ModEntities.BLACKSMITH_MERCHANT.get(),
                        ModEntities.FARMER_MERCHANT.get(), ModEntities.FISHER_MERCHANT.get(), ModEntities.BANKER_MERCHANT.get());
                for (ServerPlayer p : players) spawnNear(level, p, kinds.get(rnd.nextInt(kinds.size())), 4, 8, false);
            }
            case TREASURE_GOBLIN -> {
                for (ServerPlayer p : players) {
                    for (int i = 0; i < 2; i++) {
                        Mob m = spawnNear(level, p, EntityTypes.RABBIT, 8, 20, false);
                        if (m == null) continue;
                        m.getPersistentData().putBoolean(GOBLIN_TAG, true);
                        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, type.duration, 0, false, false));
                        m.addEffect(new MobEffectInstance(MobEffects.SPEED, type.duration, 2, false, false));
                        m.setCustomName(Component.translatable("entity.wildplus.treasure_rabbit").withStyle(ChatFormatting.GOLD));
                    }
                }
            }
            case ANIMAL_MIGRATION -> {
                List<EntityType<? extends Mob>> kinds = List.of(EntityTypes.COW, EntityTypes.SHEEP, EntityTypes.PIG, EntityTypes.CHICKEN,
                        EntityTypes.HORSE, EntityTypes.RABBIT);
                for (ServerPlayer p : players) for (int i = 0; i < 6; i++) spawnNear(level, p, kinds.get(rnd.nextInt(kinds.size())), 8, 20, false);
            }
            case WANTED -> {
                if (players.isEmpty()) break;
                ServerPlayer p = players.get(rnd.nextInt(players.size()));
                Mob m = spawnNear(level, p, ModEntities.BANDIT.get(), 15, 30, true);
                if (m != null) {
                    EliteMobs.makeElite(m, 2);
                    m.getPersistentData().putBoolean(WANTED_TAG, true);
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, type.duration, 0, false, false));
                    m.setCustomName(Component.translatable("entity.wildplus.wanted").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    server.getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.event.wanted",
                            m.getBlockX(), m.getBlockZ()).withStyle(ChatFormatting.RED), false);
                }
            }
            case OMEN_STORM -> {
                for (ServerPlayer p : players) {
                    if (p.hasEffect(MobEffects.BAD_OMEN) || p.hasEffect(MobEffects.RAID_OMEN)) continue;
                    p.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 120000, rnd.nextInt(3)));
                }
                server.getPlayerList().broadcastSystemMessage(Component.literal("☠ 모두에게 흉조가 걸렸어요! 이벤트 동안 흉조 습격 보상 ×1.5 · 밀수꾼 3배")
                        .withStyle(ChatFormatting.DARK_PURPLE), false);
            }
            case ILLAGER_ARMADA -> {
                if (players.isEmpty()) break;
                ServerPlayer p = players.get(rnd.nextInt(players.size()));
                BlockPos at = surface(level, p, 30, 45);
                if (at == null) break;
                ARMADA.clear();
                List<EntityType<? extends Mob>> kinds = new ArrayList<>();
                for (int i = 0; i < 7; i++) kinds.add(EntityTypes.PILLAGER);
                for (int i = 0; i < 5; i++) kinds.add(EntityTypes.VINDICATOR);
                kinds.add(EntityTypes.EVOKER);
                kinds.add(EntityTypes.EVOKER);
                kinds.add(EntityTypes.WITCH);
                kinds.add(EntityTypes.RAVAGER);
                for (EntityType<? extends Mob> k : kinds) {
                    Mob m = spawnAt(level, at, k, 6);
                    if (m == null) continue;
                    m.getPersistentData().putBoolean(ARMADA_TAG, true);
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, type.duration, 0, false, false));
                    if (rnd.nextFloat() < 0.4F && k != EntityTypes.RAVAGER) EliteMobs.makeElite(m, 1);
                    m.setTarget(p);
                    ARMADA.add(m.getUUID());
                }
                target = at;
                server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("⚔ 우민 대군 %d명이 %d, %d 에 나타났어요! 처치마다 3,000동 · 전멸시키면 모두 10만동 + 흉조 병",
                        ARMADA.size(), at.getX(), at.getZ())).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            }
            case STARFALL -> {
                if (players.isEmpty()) break;
                ServerPlayer p = players.get(rnd.nextInt(players.size()));
                BlockPos at = surface(level, p, 50, 90);
                if (at == null) break;
                meteor(level, at, rnd);
                target = at;
                for (int i = 0; i < 4; i++) {
                    Mob m = spawnAt(level, at, i % 2 == 0 ? EntityTypes.BLAZE : EntityTypes.WITHER_SKELETON, 5);
                    if (m != null) EliteMobs.makeElite(m, 1 + rnd.nextInt(2));
                }
                server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("☄ 거대한 별똥별이 %d, %d, %d 에 떨어졌어요! 루비 · 사파이어 · 미스릴 광석 (정예 수호자 주의)",
                        at.getX(), at.getY(), at.getZ())).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
            }
            case GOLDEN_VAULT -> {
                if (players.isEmpty()) break;
                ServerPlayer p = players.get(rnd.nextInt(players.size()));
                BlockPos at = surface(level, p, 40, 70);
                if (at == null) break;
                level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
                level.setBlock(at.below(), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
                    ItemStack[] loot = {new ItemStack(ModItems3.DIAMOND_COIN.get(), 30 + rnd.nextInt(31)),
                            new ItemStack(com.snolred.wildplus.registry.ModItems13.EMERALD_COIN.get(), 2 + rnd.nextInt(4)),
                            new ItemStack(Items.GOLD_BLOCK, 2 + rnd.nextInt(4)), new ItemStack(Items.DIAMOND, 3 + rnd.nextInt(5)),
                            rnd.nextFloat() < 0.4F ? new ItemStack(com.snolred.wildplus.registry.ModItems19.LIFE_ESSENCE.get()) : new ItemStack(Items.EMERALD, 16)};
                    for (int i = 0; i < loot.length; i++) chest.setItem(i * 5 + rnd.nextInt(4), loot[i]);
                }
                for (int i = 0; i < 6; i++) {
                    Mob m = spawnAt(level, at, rnd.nextBoolean() ? EntityTypes.ZOMBIE : EntityTypes.HUSK, 5);
                    if (m == null) continue;
                    m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                    m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                    m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                    EliteMobs.makeElite(m, 1);
                }
                target = at;
                server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("💰 황금 금고가 %d, %d, %d 에 나타났어요! (다이아 동전 30~60 · 에메랄드 동전 · 황금 정예 6마리가 지킴)",
                        at.getX(), at.getY(), at.getZ())).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            }
            case MARKET_BOOM -> server.getPlayerList().broadcastSystemMessage(Component.literal("📈 시장 호황! 이벤트 동안 출하 상자가 20초마다 바로 팔리고 값은 1.5배")
                    .withStyle(ChatFormatting.GREEN), false);
            default -> {}
        }
        WildPlus.LOGGER.info("Random event started: {}", type);
    }

    public static void stop(MinecraftServer server) {
        if (active == null) return;
        ServerLevel level = server.overworld();
        if (active == Type.STORM) command(server, "weather clear");
        for (UUID id : SPAWNED) {
            Entity e = level.getEntity(id);
            if (e != null && e.getPersistentData().getBooleanOr(TAG, false)) e.discard();
        }
        SPAWNED.clear();
        if (bar != null) bar.removeAllPlayers();
        bar = null;
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.event.end", Component.translatable(active.key()))
                .withStyle(ChatFormatting.GRAY), false);
        active = null;
    }

    // ───────── 틱 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        if (nextAt < 0) nextAt = now + interval(level.getRandom());
        if (active == null) {
            if (now >= nextAt && WildConfig.EVENTS_ENABLED.getAsBoolean() && !server.getPlayerList().getPlayers().isEmpty()) {
                nextAt = now + interval(level.getRandom());
                start(server, pick(server, level.getRandom()));
            }
            return;
        }
        if (now >= endsAt) {
            stop(server);
            return;
        }
        if (bar != null && now % 20 == 0) {
            bar.setProgress(Mth.clamp((endsAt - now) / (float) active.duration, 0F, 1F));
            long sec = (endsAt - now) / 20;
            bar.setName(Component.translatable(active.key()).append(String.format("  %d:%02d", sec / 60, sec % 60)).withStyle(ChatFormatting.BOLD));
            for (ServerPlayer p : server.getPlayerList().getPlayers()) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        }
        tickEvent(server, level, now);
    }

    private static long interval(RandomSource rnd) {
        long base = WildConfig.EVENT_INTERVAL.getAsInt() * 1200L;
        return base / 2 + (long) (rnd.nextDouble() * base);
    }

    private static void tickEvent(MinecraftServer server, ServerLevel level, long now) {
        RandomSource rnd = level.getRandom();
        List<ServerPlayer> players = survivalPlayers(server);
        switch (active) {
            case LUCKY_DAY -> effects(players, now, MobEffects.LUCK, 1);
            case MINING_FEVER -> effects(players, now, MobEffects.HASTE, 1);
            case SWIFT_WIND -> effects(players, now, MobEffects.SPEED, 1);
            case FISH_FRENZY -> {
                effects(players, now, MobEffects.LUCK, 2);
                effects(players, now, MobEffects.DOLPHINS_GRACE, 0);
            }
            case BLESSING -> {
                effects(players, now, MobEffects.REGENERATION, 0);
                effects(players, now, MobEffects.RESISTANCE, 0);
            }
            case LOW_GRAVITY -> {
                effects(players, now, MobEffects.JUMP_BOOST, 2);
                effects(players, now, MobEffects.SLOW_FALLING, 0);
            }
            case HARVEST_FESTIVAL -> {
                if (now % 40 != 0) break;
                for (ServerPlayer p : players) {
                    for (int i = 0; i < 40; i++) {
                        BlockPos pos = p.blockPosition().offset(rnd.nextInt(33) - 16, rnd.nextInt(7) - 3, rnd.nextInt(33) - 16);
                        BlockState s = level.getBlockState(pos);
                        if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s)) {
                            crop.growCrops(level, pos, s);
                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
                        }
                    }
                }
            }
            case METEOR_SHOWER -> {
                if (now % 200 != 0) break;
                for (ServerPlayer p : players) {
                    BlockPos pos = surface(level, p, 10, 30);
                    if (pos == null) continue;
                    level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 6, pos.getZ() + 0.5, 60, 0.4, 4, 0.4, 0.02);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 30, 0.8, 0.5, 0.8, 0.02);
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.NONE);
                    ItemStack[] loot = {new ItemStack(Items.RAW_IRON, 2 + rnd.nextInt(4)), new ItemStack(Items.RAW_GOLD, 1 + rnd.nextInt(3)),
                            new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD, 1 + rnd.nextInt(2)),
                            new ItemStack(com.snolred.wildplus.registry.ModItems2.RAW_MYTHRIL.get(), 1 + rnd.nextInt(2)), new ItemStack(Items.RAW_COPPER, 4)};
                    drop(level, pos.above(), loot[rnd.nextInt(loot.length)]);
                    if (rnd.nextFloat() < 0.3F) drop(level, pos.above(), loot[rnd.nextInt(loot.length)]);
                }
            }
            case MONSTER_RAID -> {
                if (now % 600 != 0) break;
                List<EntityType<? extends Mob>> kinds = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER, EntityTypes.HUSK);
                for (ServerPlayer p : players) for (int i = 0; i < 4; i++) spawnNear(level, p, kinds.get(rnd.nextInt(kinds.size())), 12, 24, true);
            }
            case ELITE_INVASION -> {
                if (now % 1200 != 0) break;
                List<EntityType<? extends Mob>> kinds = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.VINDICATOR);
                for (ServerPlayer p : players) {
                    for (int i = 0; i < 2; i++) {
                        Mob m = spawnNear(level, p, kinds.get(rnd.nextInt(kinds.size())), 14, 26, true);
                        if (m != null) EliteMobs.makeElite(m, 1);
                    }
                }
            }
            case SLIME_RAIN -> {
                if (now % 400 != 0) break;
                for (ServerPlayer p : players) spawnNear(level, p, ModEntities.GOLDEN_SLIME.get(), 6, 16, true);
            }
            case COIN_RAIN -> {
                if (now % 60 != 0) break;
                for (ServerPlayer p : players) {
                    for (int i = 0; i < 3; i++) {
                        float f = rnd.nextFloat(); // 3.8: 동화 · 은화 → 은화 · 금화 · 가끔 다이아 동전
                        ItemStack coin = f < 0.05F ? new ItemStack(ModItems3.DIAMOND_COIN.get())
                                : f < 0.35F ? new ItemStack(ModItems3.GOLD_COIN.get(), 1 + rnd.nextInt(2))
                                : new ItemStack(ModItems3.SILVER_COIN.get(), 2 + rnd.nextInt(4));
                        BlockPos pos = p.blockPosition().offset(rnd.nextInt(13) - 6, 12, rnd.nextInt(13) - 6);
                        drop(level, pos, coin);
                    }
                }
            }
            case STORM -> {
                if (now % 100 != 0) break;
                for (ServerPlayer p : players) {
                    if (rnd.nextFloat() > 0.5F) continue;
                    BlockPos pos = surface(level, p, 12, 40);
                    if (pos == null) continue;
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
                    if (bolt == null) continue;
                    bolt.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
            case PLAGUE -> {
                if (now % 600 != 0) break;
                for (ServerPlayer p : players) {
                    if (rnd.nextFloat() < 0.4F) {
                        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
                        p.sendOverlayMessage(Component.translatable("message.wildplus.event.plague_hit").withStyle(ChatFormatting.DARK_GREEN));
                    }
                }
            }
            case HERO_TIME -> {
                effects(players, now, MobEffects.STRENGTH, 1);
                effects(players, now, MobEffects.SPEED, 0);
            }
            case FROST_WIND -> {
                if (now % 40 != 0) break;
                for (ServerPlayer p : players) {
                    if (!p.hasEffect(MobEffects.FIRE_RESISTANCE)) p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, false, true));
                    level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.5, p.getZ(), 12, 3, 1, 3, 0.02);
                    for (int i = 0; i < 12; i++) {
                        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                                p.blockPosition().offset(rnd.nextInt(25) - 12, 0, rnd.nextInt(25) - 12)).below();
                        if (level.getBlockState(top).is(Blocks.WATER) && level.getFluidState(top).isSource()) {
                            level.setBlock(top, Blocks.ICE.defaultBlockState(), 3);
                        }
                    }
                }
            }
            case BLOOM -> {
                if (now % 40 == 0) {
                    BlockState[] flowers = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                            Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(),
                            Blocks.PINK_TULIP.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()};
                    for (ServerPlayer p : players) {
                        for (int i = 0; i < 6; i++) {
                            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    p.blockPosition().offset(rnd.nextInt(25) - 12, 0, rnd.nextInt(25) - 12));
                            if (level.getBlockState(top).isAir() && level.getBlockState(top.below()).is(Blocks.GRASS_BLOCK)) {
                                level.setBlock(top, flowers[rnd.nextInt(flowers.length)], 3);
                                level.sendParticles(ParticleTypes.CHERRY_LEAVES, top.getX() + 0.5, top.getY() + 0.8, top.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
                            }
                        }
                    }
                }
                if (now % 600 == 0) {
                    for (ServerPlayer p : players) {
                        for (net.minecraft.world.entity.animal.Animal a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                                p.getBoundingBox().inflate(16))) {
                            if (a.canFallInLove() && a.getAge() == 0) a.setInLove(p);
                        }
                    }
                }
            }
            case ORE_RAIN -> {
                if (now % 60 != 0) break;
                for (ServerPlayer p : players) {
                    for (int i = 0; i < 3; i++) {
                        float f = rnd.nextFloat();
                        ItemStack ore = f < 0.002F ? new ItemStack(com.snolred.wildplus.registry.ModItems7.RUBY.get())
                                : f < 0.02F ? new ItemStack(Items.DIAMOND)
                                : f < 0.06F ? new ItemStack(Items.EMERALD)
                                : f < 0.16F ? new ItemStack(Items.RAW_GOLD, 1 + rnd.nextInt(2))
                                : f < 0.32F ? new ItemStack(Items.RAW_IRON, 1 + rnd.nextInt(2))
                                : f < 0.48F ? new ItemStack(Items.REDSTONE, 2 + rnd.nextInt(4))
                                : f < 0.62F ? new ItemStack(Items.LAPIS_LAZULI, 2 + rnd.nextInt(3))
                                : f < 0.80F ? new ItemStack(Items.RAW_COPPER, 2 + rnd.nextInt(3))
                                : new ItemStack(Items.COAL, 2 + rnd.nextInt(3));
                        BlockPos pos = p.blockPosition().offset(rnd.nextInt(13) - 6, 12, rnd.nextInt(13) - 6);
                        drop(level, pos, ore);
                    }
                }
            }
            case ILLAGER_ARMADA -> {
                if (now % 20 != 0 || ARMADA.isEmpty()) break;
                ARMADA.removeIf(u -> {
                    Entity e = level.getEntity(u);
                    return e == null || !e.isAlive();
                });
                if (bar != null) bar.setName(Component.translatable(active.key()).append(" · 남은 우민 " + ARMADA.size()).withStyle(ChatFormatting.BOLD));
                if (ARMADA.isEmpty()) {
                    WildData wd = WildData.get(server);
                    for (ServerPlayer p : players) {
                        wd.addBalance(p.getUUID(), 100_000);
                        com.snolred.wildplus.omen.Omen.give(p, com.snolred.wildplus.omen.Omen.withAmp(new ItemStack(Items.OMINOUS_BOTTLE, 2), rnd.nextInt(3)));
                        p.sendSystemMessage(Component.literal("🏆 우민 대군 전멸! 100,000동 통장 입금 + 흉조 병 2").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    }
                    wd.setDirty();
                    stop(server);
                }
            }
            case STARFALL, GOLDEN_VAULT -> {
                if (target != null && now % 20 == 0) {
                    level.sendParticles(active == Type.STARFALL ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON, target.getX() + 0.5, target.getY() + 2,
                            target.getZ() + 0.5, 6, 0.4, 2, 0.4, 0.02);
                }
            }
            case SUPPLY_DROP -> {
                if (target != null && now % 20 == 0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, 2, 0.1, 0.5, 0.1, 0.01);
                }
            }
            default -> {}
        }
    }

    private static void effects(List<ServerPlayer> players, long now, Holder<MobEffect> effect, int amp) {
        if (now % 100 != 0) return;
        for (ServerPlayer p : players) p.addEffect(new MobEffectInstance(effect, 140, amp, true, false, true));
    }

    // ───────── 게임 규칙에 끼어드는 이벤트 ─────────
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player killer) || dead instanceof Player) return;
        if (dead.getPersistentData().getBooleanOr(GOBLIN_TAG, false)) {
            RandomSource r = killer.getRandom();
            ItemStack[] loot = {new ItemStack(ModItems3.GOLD_COIN.get(), 5 + r.nextInt(6)), new ItemStack(Items.DIAMOND, 1 + r.nextInt(3)),
                    new ItemStack(Items.EMERALD, 2 + r.nextInt(4)), new ItemStack(ModItems3.DIAMOND_COIN.get(), 3 + r.nextInt(4))};
            for (ItemStack s : loot) event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY() + 0.5, dead.getZ(), s));
            if (dead.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, dead.getX(), dead.getY() + 0.5, dead.getZ(), 30, 0.4, 0.4, 0.4, 0.3);
            }
        }
        if (active == Type.DOUBLE_DROPS && !dead.getPersistentData().getBooleanOr(TAG, false)) {
            List<ItemEntity> extra = new ArrayList<>();
            for (ItemEntity it : event.getDrops()) extra.add(new ItemEntity(dead.level(), it.getX(), it.getY(), it.getZ(), it.getItem().copy()));
            event.getDrops().addAll(extra);
        }
        if (!(dead instanceof Monster m)) return;
        Player p = killer;
        if (active == Type.FULL_MOON && p.getRandom().nextFloat() < 0.25F) {
            event.getDrops().add(new ItemEntity(m.level(), m.getX(), m.getY() + 0.5, m.getZ(), new ItemStack(ModItems3.GOLD_COIN.get())));
        }
        if (active == Type.GOLD_RUSH) {
            ItemEntity it = new ItemEntity(m.level(), m.getX(), m.getY() + 0.5, m.getZ(),
                    p.getRandom().nextFloat() < 0.1F ? new ItemStack(ModItems3.GOLD_COIN.get()) : new ItemStack(ModItems3.SILVER_COIN.get(), 2 + p.getRandom().nextInt(5)));
            event.getDrops().add(it);
        }
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        if (active == Type.XP_FESTIVAL) event.setDroppedExperience(event.getDroppedExperience() * 2);
        if (active == Type.FULL_MOON && event.getEntity() instanceof Monster) event.setDroppedExperience(event.getDroppedExperience() * 3);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (active != Type.ORE_BLESSING || !event.getState().is(Tags.Blocks.ORES) || event.getDrops().isEmpty()) return;
        // 광석 블록 자체(섬세한 손길)는 복사하지 않음 — 놓고 다시 캐는 무한 복사 방지
        if (event.getDrops().get(0).getItem().is(event.getState().getBlock().asItem())) return;
        if (event.getLevel().getRandom().nextFloat() < 0.5F) {
            ItemEntity first = event.getDrops().get(0);
            event.getDrops().add(new ItemEntity(event.getLevel(), first.getX(), first.getY(), first.getZ(), first.getItem().copy()));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.getPersistentData().getBooleanOr(WANTED_TAG, false) && event.getSource().getEntity() instanceof ServerPlayer killer) {
            Coins.give(killer, 30_000); // 3.8: 500 → 3만동
            killer.level().getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.event.wanted_done",
                    killer.getName()).withStyle(ChatFormatting.GOLD), false);
            if (active == Type.WANTED) stop(killer.level().getServer());
        }
    }

    /** 3.8 우민 대군: 처치마다 3,000동 */
    @SubscribeEvent
    public static void onArmadaKill(LivingDeathEvent event) {
        if (!event.getEntity().getPersistentData().getBooleanOr(ARMADA_TAG, false) || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        WildData.get(p.level().getServer()).addBalance(p.getUUID(), 3_000);
        p.sendOverlayMessage(Component.literal("⚔ 우민 대군 처치 +3,000동").withStyle(ChatFormatting.GOLD));
    }

    /** 흡혈의 밤: 플레이어가 준 근접 피해의 25% 회복 */
    @SubscribeEvent
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (active != Type.VAMPIRE_NIGHT || !(event.getSource().getEntity() instanceof Player p) || event.getSource().getDirectEntity() != p
                || p.level().isClientSide()) {
            return;
        }
        float heal = Math.min(event.getHealthDamage() * 0.25F, 4.0F);
        if (heal > 0 && p.getHealth() < p.getMaxHealth()) {
            p.heal(heal);
            if (p.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, event.getEntity().getX(), event.getEntity().getY() + 1, event.getEntity().getZ(), 3, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    /** 이벤트 투척병이 깨지면 이벤트 시작 */
    @SubscribeEvent
    public static void onImpact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile proj)
                || !proj.getItem().is(com.snolred.wildplus.registry.ModItems7.EVENT_BOTTLE.get())
                || !(proj.level() instanceof ServerLevel level) || proj.isRemoved()) {
            return;
        }
        Type type = com.snolred.wildplus.item.EventBottleItem.typeOf(proj.getItem());
        if (type == null) type = random(level.getRandom());
        level.sendParticles(ParticleTypes.END_ROD, proj.getX(), proj.getY(), proj.getZ(), 40, 0.6, 0.6, 0.6, 0.15);
        level.sendParticles(ParticleTypes.FIREWORK, proj.getX(), proj.getY(), proj.getZ(), 30, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, proj.blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        Component who = proj.getOwner() != null ? proj.getOwner().getName() : Component.literal("누군가");
        MinecraftServer server = level.getServer();
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.event_bottle.used", who)
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        start(server, type);
        nextAt = server.overworld().getGameTime() + type.duration + interval(level.getRandom());
        proj.discard();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        if (bar != null) bar.removeAllPlayers();
        bar = null;
        active = null;
        nextAt = -1;
        SPAWNED.clear();
    }

    // ───────── 도우미 ─────────
    private static void command(MinecraftServer server, String cmd) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
    }

    private static List<ServerPlayer> survivalPlayers(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator() && p.level().dimension() == Level.OVERWORLD) out.add(p);
        }
        return out;
    }

    /** 플레이어 주변 지표면의 빈 자리 */
    static BlockPos surface(ServerLevel level, Player p, int min, int max) {
        RandomSource rnd = level.getRandom();
        for (int i = 0; i < 10; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = min + rnd.nextDouble() * (max - min);
            int x = Mth.floor(p.getX() + Math.cos(ang) * d), z = Mth.floor(p.getZ() + Math.sin(ang) * d);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (level.getBlockState(top).isAir() && level.getBlockState(top.below()).isSolid() && level.getFluidState(top).isEmpty()) return top;
        }
        return null;
    }

    private static Mob spawnNear(ServerLevel level, Player p, EntityType<? extends Mob> type, int min, int max, boolean persist) {
        BlockPos pos = surface(level, p, min, max);
        if (pos == null) return null;
        Mob m = type.create(level, EntitySpawnReason.EVENT);
        if (m == null) return null;
        m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
        if (!persist) {
            m.getPersistentData().putBoolean(TAG, true);
            SPAWNED.add(m.getUUID());
        }
        level.addFreshEntityWithPassengers(m);
        return m;
    }

    /** 한 자리 주변에 몹 (이벤트가 끝나면 사라짐) */
    private static Mob spawnAt(ServerLevel level, BlockPos center, EntityType<? extends Mob> type, int spread) {
        RandomSource rnd = level.getRandom();
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                center.offset(rnd.nextInt(spread * 2 + 1) - spread, 0, rnd.nextInt(spread * 2 + 1) - spread));
        Mob m = type.create(level, EntitySpawnReason.EVENT);
        if (m == null) return null;
        m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd.nextFloat() * 360F, 0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
        if (m instanceof net.minecraft.world.entity.raid.Raider r) r.setCanJoinRaid(false);
        m.getPersistentData().putBoolean(TAG, true);
        SPAWNED.add(m.getUUID());
        level.addFreshEntityWithPassengers(m);
        return m;
    }

    /** 별똥별: 땅 위에 불탄 운석 덩어리 (광석이 박힘) */
    private static void meteor(ServerLevel level, BlockPos at, RandomSource rnd) {
        level.explode(null, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 3.0F, Level.ExplosionInteraction.NONE);
        BlockState[] ores = {com.snolred.wildplus.registry.ModBlocks2.RUBY_ORE.get().defaultBlockState(),
                com.snolred.wildplus.registry.ModBlocks2.SAPPHIRE_ORE.get().defaultBlockState(),
                com.snolred.wildplus.registry.ModBlocks.MYTHRIL_ORE.get().defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState(),
                Blocks.EMERALD_ORE.defaultBlockState(), Blocks.GOLD_ORE.defaultBlockState()};
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, 0, -2), at.offset(2, 3, 2))) {
            double d = p.distSqr(at.above(1));
            if (d > 6.5 || !level.getBlockState(p).canBeReplaced()) continue;
            float f = rnd.nextFloat();
            BlockState s = f < 0.30F ? ores[rnd.nextInt(ores.length)] : f < 0.55F ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : f < 0.8F ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
            level.setBlock(p.immutable(), s, 3);
        }
        level.sendParticles(ParticleTypes.LAVA, at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5, 40, 1.5, 1, 1.5, 0.1);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 3, at.getZ() + 0.5, 60, 1.5, 2, 1.5, 0.05);
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity it = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        it.setDefaultPickUpDelay();
        level.addFreshEntity(it);
    }
}

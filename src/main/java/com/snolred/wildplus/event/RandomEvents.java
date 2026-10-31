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
 * 랜덤 이벤트 24종. 평균 15분(설정)마다 하나가 일어나 몇 분 동안 이어진다. 보스바에 이름과 남은 시간.
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
        WANTED(6000, BossEvent.BossBarColor.RED);

        public final int duration;
        public final BossEvent.BossBarColor color;

        Type(int duration, BossEvent.BossBarColor color) {
            this.duration = duration;
            this.color = color;
        }

        public String key() {
            return "event.wildplus." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final String TAG = "wildplus_event_mob", WANTED_TAG = "wildplus_wanted";
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
            case STORM -> level.setWeatherParameters(0, type.duration, true, true);
            case BANK_BONUS -> {
                WildData data = WildData.get(server);
                for (var e : new ArrayList<>(data.bank.entrySet())) {
                    long bonus = Math.min(300, e.getValue() * 2 / 100);
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
            default -> {}
        }
        WildPlus.LOGGER.info("Random event started: {}", type);
    }

    public static void stop(MinecraftServer server) {
        if (active == null) return;
        ServerLevel level = server.overworld();
        if (active == Type.STORM) level.setWeatherParameters(6000, 0, false, false);
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
                Type[] all = Type.values();
                start(server, all[level.getRandom().nextInt(all.length)]);
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
                        ItemStack coin = new ItemStack(rnd.nextFloat() < 0.15F ? ModItems3.SILVER_COIN.get() : ModItems3.COPPER_COIN.get(), 1 + rnd.nextInt(3));
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
        if (!(event.getEntity() instanceof Monster m) || !(event.getSource().getEntity() instanceof Player p)) return;
        if (active == Type.GOLD_RUSH) {
            ItemEntity it = new ItemEntity(m.level(), m.getX(), m.getY() + 0.5, m.getZ(),
                    new ItemStack(ModItems3.COPPER_COIN.get(), 3 + p.getRandom().nextInt(8)));
            event.getDrops().add(it);
        }
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        if (active == Type.XP_FESTIVAL) event.setDroppedExperience(event.getDroppedExperience() * 2);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (active != Type.ORE_BLESSING || !event.getState().is(Tags.Blocks.ORES) || event.getDrops().isEmpty()) return;
        if (event.getLevel().getRandom().nextFloat() < 0.5F) {
            ItemEntity first = event.getDrops().get(0);
            event.getDrops().add(new ItemEntity(event.getLevel(), first.getX(), first.getY(), first.getZ(), first.getItem().copy()));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.getPersistentData().getBooleanOr(WANTED_TAG, false) && event.getSource().getEntity() instanceof ServerPlayer killer) {
            Coins.give(killer, 500);
            killer.level().getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.wildplus.event.wanted_done",
                    killer.getName()).withStyle(ChatFormatting.GOLD), false);
            if (active == Type.WANTED) stop(killer.level().getServer());
        }
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

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity it = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        it.setDefaultPickUpDelay();
        level.addFreshEntity(it);
    }
}

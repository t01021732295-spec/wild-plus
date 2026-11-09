package com.snolred.wildplus.content;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.event.EliteMobs;
import com.snolred.wildplus.item.EventBottleItem;
import com.snolred.wildplus.registry.ModItems7;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 하늘의 탑: 탑 입구 근처의 친구들이 함께 하늘 위 투기장으로 올라가 층마다 몰려오는 몬스터를 물리친다.
 * - 층을 깰 때마다 각자 동전(층 × 10동), 5층마다 보너스 아이템. 10층마다 경치(테마)가 바뀐다.
 * - 5층마다 정예 보스, 10층마다 큰 보스.
 * - 쓰러지면 아이템을 잃지 않고 탑 입구로 돌아온다 (탈락). 층 사이 쉬는 시간에 금빛 발판을 밟으면 스스로 내려갈 수 있다.
 * - 모두 탈락하면 도전 끝. 최고 기록은 탑 입구 화면과 명예 기록에 남는다.
 */
public final class Tower {
    public static final BlockPos CENTER = new BlockPos(1_000_000, 300, 1_000_000);
    public static final int R = 12, H = 8;
    public static final String TAG = "wildplus_tower_mob";
    private static final BlockPos EXIT = CENTER.offset(0, 0, -R + 1);

    private enum Phase { ARRIVE, FIGHT, REST }

    private static boolean running;
    private static Phase phase;
    private static int floor;
    private static long timer, floorStart;
    private static BlockPos gate;
    private static final Set<UUID> ACTIVE = new LinkedHashSet<>();
    private static final Map<UUID, String> NAMES = new LinkedHashMap<>();
    /** 이번 도전에서 각자 몬스터에게 준 피해 합계 */
    private static final Map<UUID, Float> DMG = new LinkedHashMap<>();
    private static final Map<UUID, Integer> BEST_FLOOR = new LinkedHashMap<>();
    private static final List<UUID> MOBS = new ArrayList<>();
    private static ServerBossEvent bar;

    private Tower() {}

    public static boolean running() {
        return running;
    }

    public static int floor() {
        return floor;
    }

    public static CompoundTag state(MinecraftServer server) {
        return ContentData.get(server).section("tower");
    }

    // ───────── 테마 (10층마다) ─────────
    private record Theme(String name, Block floor, Block accent, Block wall, Block pillar, Block light) {}

    private static final Theme[] THEMES = {
            new Theme("이끼 낀 회랑", Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.SEA_LANTERN),
            new Theme("사막 신전", Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.GLOWSTONE),
            new Theme("지옥 요새", Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.SHROOMLIGHT),
            new Theme("해저 궁전", Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN),
            new Theme("끝의 도시", Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.END_ROD),
            new Theme("황금 왕좌", Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.SHROOMLIGHT),
            // 4.7 61층부터
            new Theme("천공 성채", Blocks.QUARTZ_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.SEA_LANTERN),
            new Theme("공허의 회랑", Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.BLACKSTONE, Blocks.PURPUR_PILLAR, Blocks.END_ROD),
            new Theme("화염 전장", Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK, Blocks.BASALT, Blocks.POLISHED_BASALT, Blocks.SHROOMLIGHT),
            new Theme("심판의 정상", Blocks.PURPUR_BLOCK, Blocks.GOLD_BLOCK, Blocks.QUARTZ_BRICKS, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.SEA_LANTERN),
    };

    /** 1~60층은 6테마, 61층부터는 천공 · 공허 · 전장 · 정상이 10층씩 돈다 */
    private static int themeIndex(int f) {
        int x = Math.max(1, f);
        return x <= 60 ? (x - 1) / 10 : 6 + ((x - 61) / 10) % 4;
    }

    public static String themeName(int f) {
        return THEMES[themeIndex(f)].name();
    }

    // ───────── 시작 ─────────
    public static void start(ServerPlayer opener, BlockPos gatePos) {
        if (running) {
            opener.sendSystemMessage(Component.literal("이미 누군가 탑을 오르고 있어요! (" + floor + "층)").withStyle(ChatFormatting.RED));
            return;
        }
        if (opener.level().dimension() != Level.OVERWORLD) {
            opener.sendSystemMessage(Component.literal("탑 입구는 오버월드에서만 열려요").withStyle(ChatFormatting.RED));
            return;
        }
        ServerLevel lv = (ServerLevel) opener.level();
        List<ServerPlayer> party = Raid.nearby(lv, gatePos, 8);
        if (party.isEmpty()) party.add(opener);
        running = true;
        gate = gatePos.immutable();
        floor = 1;
        ACTIVE.clear();
        NAMES.clear();
        DMG.clear();
        BEST_FLOOR.clear();
        MOBS.clear();
        CompoundTag st = state(lv.getServer());
        st.putLong("gate", gate.asLong());
        ContentData.get(lv.getServer()).dirty();
        for (int cx = (CENTER.getX() - R - 2) >> 4; cx <= (CENTER.getX() + R + 2) >> 4; cx++) {
            for (int cz = (CENTER.getZ() - R - 2) >> 4; cz <= (CENTER.getZ() + R + 2) >> 4; cz++) lv.setChunkForced(cx, cz, true);
        }
        build(lv, floor);
        bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("하늘의 탑"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer p : party) {
            ACTIVE.add(p.getUUID());
            NAMES.put(p.getUUID(), p.getGameProfile().name());
            bar.addPlayer(p);
        }
        arrive(lv);
        lv.getServer().getPlayerList().broadcastSystemMessage(Component.literal("🗼 " + String.join(", ", NAMES.values())
                + " 님이 하늘의 탑에 올랐어요!").withStyle(ChatFormatting.AQUA), false);
    }

    /** 모두를 투기장 가운데로 데려오고 잠시 뒤 몬스터 등장 */
    private static void arrive(ServerLevel lv) {
        int i = 0;
        for (UUID id : ACTIVE) {
            ServerPlayer p = lv.getServer().getPlayerList().getPlayer(id);
            if (p == null) continue;
            double a = i++ * Math.PI * 2 / Math.max(1, ACTIVE.size());
            p.teleportTo(lv, CENTER.getX() + 0.5 + Math.cos(a) * 2, CENTER.getY() + 1, CENTER.getZ() + 0.5 + Math.sin(a) * 2, Set.of(), p.getYRot(), 0F, true);
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(floor + "층").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
            p.sendOverlayMessage(Component.literal(themeName(floor) + (floor % 10 == 0 ? " · 큰 보스!" : floor % 5 == 0 ? " · 정예 보스" : ""))
                    .withStyle(ChatFormatting.GRAY));
        }
        lv.playSound(null, CENTER.above(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
        phase = Phase.ARRIVE;
        timer = lv.getGameTime() + 60;
    }

    // ───────── 투기장 짓기 ─────────
    private static void build(ServerLevel lv, int f) {
        Theme t = THEMES[themeIndex(f)];
        BlockState air = Blocks.AIR.defaultBlockState(), barrier = Blocks.BARRIER.defaultBlockState();
        int cx = CENTER.getX(), cy = CENTER.getY(), cz = CENTER.getZ();
        for (int dx = -R - 1; dx <= R + 1; dx++) {
            for (int dz = -R - 1; dz <= R + 1; dz++) {
                boolean edge = Math.abs(dx) == R + 1 || Math.abs(dz) == R + 1;
                boolean corner = Math.abs(dx) >= R - 1 && Math.abs(dz) >= R - 1;
                // 바닥: 체크 무늬 + 6칸마다 조명 + 가운데 원
                Block fl = (Math.floorMod(dx, 6) == 0 && Math.floorMod(dz, 6) == 0) ? t.light()
                        : (dx * dx + dz * dz <= 9) ? t.pillar() : ((dx + dz) % 2 == 0 ? t.floor() : t.accent());
                lv.setBlock(new BlockPos(cx + dx, cy, cz + dz), edge ? t.wall().defaultBlockState() : fl.defaultBlockState(), 2);
                lv.setBlock(new BlockPos(cx + dx, cy - 1, cz + dz), t.wall().defaultBlockState(), 2);
                for (int y = 1; y <= H; y++) {
                    BlockState s;
                    if (edge) s = y <= 3 || (Math.floorMod(dx + dz, 4) == 0 && y <= H - 2) ? t.wall().defaultBlockState() : (y == H ? barrier : air);
                    else if (corner && y <= H - 2) s = t.pillar().defaultBlockState();
                    else s = y == H ? barrier : air;
                    lv.setBlock(new BlockPos(cx + dx, cy + y, cz + dz), s, 2);
                }
                lv.setBlock(new BlockPos(cx + dx, cy + H + 1, cz + dz), barrier, 2);
            }
        }
        // 기둥 꼭대기 조명 · 출구 발판
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            lv.setBlock(new BlockPos(cx + sx * R, cy + H - 1, cz + sz * R), t.light().defaultBlockState(), 2);
        }
        lv.setBlock(EXIT, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        lv.setBlock(EXIT.above(), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState(), 2);
    }

    // ───────── 몬스터 ─────────
    private static List<EntityType<? extends Mob>> pool(int f) {
        if (f > 60) {
            return switch ((f - 61) / 10 % 4) {
                case 0 -> List.of(com.snolred.wildplus.registry.ModEntities.CLOUD_SENTINEL.get(), com.snolred.wildplus.registry.ModEntities.THUNDER_HAWK.get(), com.snolred.wildplus.registry.ModEntities.CLOUD_PRIEST.get());
                case 1 -> List.of(com.snolred.wildplus.registry.ModEntities.VOID_STALKER.get(), com.snolred.wildplus.registry.ModEntities.RIFT_MITE.get(), com.snolred.wildplus.registry.ModEntities.VOID_WRAITH.get());
                case 2 -> List.of(EntityTypes.WITHER_SKELETON, EntityTypes.BLAZE, EntityTypes.RAVAGER, EntityTypes.PIGLIN_BRUTE, EntityTypes.EVOKER);
                default -> List.of(com.snolred.wildplus.registry.ModEntities.CLOUD_SENTINEL.get(), com.snolred.wildplus.registry.ModEntities.CLOUD_PRIEST.get(), com.snolred.wildplus.registry.ModEntities.VOID_STALKER.get(), EntityTypes.WITHER_SKELETON, EntityTypes.RAVAGER);
            };
        }
        List<EntityType<? extends Mob>> p = new ArrayList<>(List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER));
        if (f >= 4) p.addAll(List.of(EntityTypes.HUSK, EntityTypes.STRAY, EntityTypes.CAVE_SPIDER));
        if (f >= 8) p.addAll(List.of(EntityTypes.VINDICATOR, EntityTypes.WITCH, EntityTypes.PILLAGER));
        if (f >= 15) p.addAll(List.of(EntityTypes.WITHER_SKELETON, EntityTypes.BLAZE, EntityTypes.PIGLIN_BRUTE));
        if (f >= 25) p.addAll(List.of(EntityTypes.EVOKER, EntityTypes.RAVAGER));
        return p;
    }

    private static UUID bossId;
    private static int bossKind;

    private static EntityType<? extends Mob> bossType(int kind) {
        return switch (kind) {
            case 0 -> com.snolred.wildplus.registry.ModEntities.VINE_BOSS.get();
            case 1 -> com.snolred.wildplus.registry.ModEntities.PHARAOH_BOSS.get();
            case 2 -> com.snolred.wildplus.registry.ModEntities.FLAME_BOSS.get();
            case 3 -> com.snolred.wildplus.registry.ModEntities.ABYSS_BOSS.get();
            case 4 -> com.snolred.wildplus.registry.ModEntities.VOID_BOSS.get();
            default -> com.snolred.wildplus.registry.ModEntities.GOLD_BOSS.get();
        };
    }

    private static void set(Mob mob, Holder<Attribute> attr, double mult) {
        AttributeInstance a = mob.getAttribute(attr);
        if (a != null) a.setBaseValue(a.getBaseValue() * mult);
    }

    private static Mob spawn(ServerLevel lv, EntityType<? extends Mob> type, RandomSource r) {
        Mob m = type.create(lv, EntitySpawnReason.EVENT);
        if (m == null) return null;
        double a = r.nextDouble() * Math.PI * 2, d = 6 + r.nextDouble() * (R - 7);
        m.snapTo(CENTER.getX() + 0.5 + Math.cos(a) * d, CENTER.getY() + 1, CENTER.getZ() + 0.5 + Math.sin(a) * d, r.nextFloat() * 360F, 0F);
        m.finalizeSpawn(lv, lv.getCurrentDifficultyAt(CENTER), EntitySpawnReason.EVENT, null);
        set(m, Attributes.MAX_HEALTH, 1.0 + 0.05 * floor);
        set(m, Attributes.ATTACK_DAMAGE, 1.0 + 0.03 * floor);
        AttributeInstance fr = m.getAttribute(Attributes.FOLLOW_RANGE);
        if (fr != null) fr.setBaseValue(48);
        m.setHealth(m.getMaxHealth());
        m.setPersistenceRequired();
        m.getPersistentData().putBoolean(TAG, true);
        List<UUID> act = new ArrayList<>(ACTIVE);
        if (!act.isEmpty()) {
            ServerPlayer p = lv.getServer().getPlayerList().getPlayer(act.get(r.nextInt(act.size())));
            if (p != null) m.setTarget(p);
        }
        MOBS.add(m.getUUID());
        lv.addFreshEntityWithPassengers(m);
        lv.sendParticles(ParticleTypes.PORTAL, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.6, 0.3, 0.3);
        return m;
    }

    private static void wave(ServerLevel lv) {
        RandomSource r = lv.getRandom();
        List<EntityType<? extends Mob>> pool = pool(floor);
        int n = Math.min(12, 2 + floor / 3 + ACTIVE.size());
        if (floor % 5 == 0) n = Math.max(2, n / 2);
        for (int i = 0; i < n; i++) spawn(lv, pool.get(r.nextInt(pool.size())), r);
        if (floor > 60 && floor % 10 == 0) {
            highBoss(lv, r);
        } else if (floor % 10 == 0) {
            // 테마마다 전용 보스 (고유 생김새 + 고유 패턴)
            int kind = ((floor - 1) / 10) % THEMES.length;
            Mob big = spawn(lv, bossType(kind), r);
            if (big != null) {
                set(big, Attributes.MAX_HEALTH, 4.0 + ACTIVE.size() * 1.0);
                set(big, Attributes.SCALE, 1.6);
                AttributeInstance kb = big.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kb != null) kb.setBaseValue(0.8);
                big.setHealth(big.getMaxHealth());
                big.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
                switch (kind) {
                    case 0, 4 -> big.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));
                    case 1 -> big.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                    case 2 -> big.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                    case 3 -> big.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
                    default -> big.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
                }
                for (net.minecraft.world.entity.EquipmentSlot sl : net.minecraft.world.entity.EquipmentSlot.values()) big.setDropChance(sl, 0F);
                big.setCustomName(Component.literal("👑 " + BossSkills.TOWER_NAMES[kind] + " · " + floor + "층").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                big.setCustomNameVisible(true);
                bossId = big.getUUID();
                bossKind = kind;
                for (UUID id : ACTIVE) {
                    ServerPlayer p = lv.getServer().getPlayerList().getPlayer(id);
                    if (p == null) continue;
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("👑 " + BossSkills.TOWER_NAMES[kind])
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                    p.sendSystemMessage(Component.literal("👑 " + BossSkills.TOWER_NAMES[kind] + " 등장! 패턴: " + BossSkills.TOWER_SKILLS[kind])
                            .withStyle(ChatFormatting.GOLD));
                }
                lv.playSound(null, CENTER.above(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 1.1F);
            }
        } else if (floor % 5 == 0) {
            Mob elite = spawn(lv, pool.get(r.nextInt(pool.size())), r);
            if (elite != null) {
                EliteMobs.makeElite(elite, 2);
                set(elite, Attributes.MAX_HEALTH, 1.5 + ACTIVE.size() * 0.3);
                elite.setHealth(elite.getMaxHealth());
            }
        }
        floorStart = lv.getGameTime();
    }

    /** 4.7 70층 기사왕 · 80층 문지기 · 90층 화산 거신 · 100층 공허 군주, 그 뒤로는 반복 */
    private static void highBoss(ServerLevel lv, RandomSource r) {
        int slot = (floor / 10 - 7) % 4;
        EntityType<? extends Mob> type;
        String name;
        switch (slot) {
            case 0 -> { type = com.snolred.wildplus.registry.ModEntities.STORM_KING.get(); name = "폭풍의 기사왕"; }
            case 1 -> { type = com.snolred.wildplus.registry.ModEntities.VOID_BOSS.get(); name = "공허의 문지기"; }
            case 2 -> { type = com.snolred.wildplus.registry.ModEntities.VOLCANO_BOSS.get(); name = "화산 거신"; }
            default -> { type = com.snolred.wildplus.registry.ModEntities.VOID_LORD.get(); name = "공허 군주"; }
        }
        Mob big = spawn(lv, type, r);
        if (big == null) return;
        if (big instanceof com.snolred.wildplus.entity.VoidLordEntity) {
            AttributeInstance a = big.getAttribute(Attributes.MAX_HEALTH);
            if (a != null) a.setBaseValue(1500.0);
            big.setHealth(big.getMaxHealth());
            big.getPersistentData().putFloat("wp_lord_scale", 0.35F); // 체력 상한 1024 를 넘는 몫만큼 받는 피해를 줄임
        } else {
            set(big, Attributes.MAX_HEALTH, 3.0 + ACTIVE.size() * 0.6);
            big.setHealth(big.getMaxHealth());
        }
        big.setCustomName(Component.literal("👑 " + name + " · " + floor + "층").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        big.setCustomNameVisible(true);
        for (UUID id : ACTIVE) {
            ServerPlayer p = lv.getServer().getPlayerList().getPlayer(id);
            if (p != null) p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("👑 " + name).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        }
        lv.playSound(null, CENTER.above(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 0.9F);
    }

    // ───────── 틱 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!running) return;
        ServerLevel lv = event.getServer().overworld();
        long now = lv.getGameTime();
        if (now % 10 != 0) return;
        // 투기장을 벗어나거나 나간 사람은 탈락
        for (UUID id : new ArrayList<>(ACTIVE)) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
            if (p == null || p.level() != lv || !inArena(p)) eliminate(event.getServer(), id, p, p == null ? "접속 종료" : "투기장 이탈");
        }
        if (!running) return;
        MOBS.removeIf(id -> {
            Entity e = lv.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (bar != null) {
            bar.setName(Component.literal(String.format("🗼 하늘의 탑 %d층 · %s · 남은 몬스터 %d", floor, themeName(floor), MOBS.size())));
            bar.setProgress(phase == Phase.FIGHT ? Mth.clamp(MOBS.size() / (float) Math.max(1, Math.min(12, 2 + floor / 3 + ACTIVE.size())), 0F, 1F) : 1F);
        }
        switch (phase) {
            case ARRIVE -> {
                if (now >= timer) {
                    phase = Phase.FIGHT;
                    wave(lv);
                }
            }
            case FIGHT -> {
                // 3분이 지나도 안 끝나면 남은 몬스터를 빛나게
                if (now - floorStart > 3600 && now % 200 == 0) {
                    for (UUID id : MOBS) if (lv.getEntity(id) instanceof Mob m) m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 220, 0));
                }
                // 몬스터가 바닥 아래로 떨어지거나 끼면 가운데로
                for (UUID id : MOBS) {
                    if (lv.getEntity(id) instanceof Mob m && (m.getY() < CENTER.getY() - 2 || Math.abs(m.getX() - CENTER.getX()) > R + 3
                            || Math.abs(m.getZ() - CENTER.getZ()) > R + 3)) {
                        m.teleportTo(CENTER.getX() + 0.5, CENTER.getY() + 1, CENTER.getZ() + 0.5);
                    }
                }
                // 보스 고유 패턴
                if (bossId != null && lv.getEntity(bossId) instanceof Mob boss && boss.isAlive()) {
                    List<ServerPlayer> ps = new ArrayList<>();
                    for (UUID id : ACTIVE) {
                        ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
                        if (p != null && p.isAlive()) ps.add(p);
                    }
                    BossSkills.tower(bossKind, lv, boss, ps, now - floorStart, 4.0F + floor * 0.2F);
                }
                if (MOBS.isEmpty()) cleared(lv);
            }
            case REST -> {
                for (UUID id : new ArrayList<>(ACTIVE)) {
                    ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
                    if (p != null && p.blockPosition().equals(EXIT.above())) eliminate(event.getServer(), id, p, "스스로 내려감");
                }
                if (!running) return;
                // 대기 건너뛰기: 누군가 웅크리면 바로 다음 층
                for (UUID id : ACTIVE) {
                    ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
                    if (p != null && p.isShiftKeyDown() && timer - now > 10) {
                        timer = now;
                        for (UUID o : ACTIVE) {
                            ServerPlayer op = event.getServer().getPlayerList().getPlayer(o);
                            if (op != null) op.sendOverlayMessage(Component.literal("⏩ " + p.getGameProfile().name() + " 님이 대기를 건너뛰었어요")
                                    .withStyle(ChatFormatting.AQUA));
                        }
                        break;
                    }
                }
                long left = (timer - now) / 20;
                if (left > 0 && now % 20 == 0) {
                    for (UUID id : ACTIVE) {
                        ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
                        if (p != null) p.sendOverlayMessage(Component.literal(left + "초 뒤 " + (floor + 1) + "층 · 웅크리면 바로 시작 · 금빛 발판 = 탈출")
                                .withStyle(ChatFormatting.YELLOW));
                    }
                }
                if (now >= timer) {
                    floor++;
                    if ((floor - 1) % 10 == 0) build(lv, floor);
                    arrive(lv);
                }
            }
        }
    }

    private static boolean inArena(Player p) {
        return Math.abs(p.getX() - CENTER.getX() - 0.5) <= R + 3 && Math.abs(p.getZ() - CENTER.getZ() - 0.5) <= R + 3
                && p.getY() > CENTER.getY() - 3 && p.getY() < CENTER.getY() + H + 3;
    }

    /** 층을 깨면 투기장에 남은 소환수(벡스 등)·떠돌이 몹을 모두 정리 (길들인 동물은 남김) */
    private static void sweep(ServerLevel lv) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(CENTER).inflate(R + 4, 0, R + 4).expandTowards(0, H + 6, 0).move(0, -3, 0);
        for (Mob m : lv.getEntitiesOfClass(Mob.class, box)) {
            if (m instanceof net.minecraft.world.entity.OwnableEntity o && o.getOwner() != null) continue;
            lv.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            m.discard();
        }
    }

    private static void cleared(ServerLevel lv) {
        bossId = null;
        BossSkills.clear();
        sweep(lv);
        MinecraftServer server = lv.getServer();
        long coins = 10L * floor;
        for (UUID id : ACTIVE) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;
            BEST_FLOOR.put(id, floor);
            Coins.give(p, coins);
            List<ItemStack> loot = new ArrayList<>();
            if (floor % 5 == 0) {
                loot.add(new ItemStack(Items.DIAMOND, 1 + floor / 10));
                loot.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 8));
                if (floor >= 10) loot.add(new ItemStack(ModItems7.RUBY.get(), floor / 10));
                if (floor % 15 == 0) loot.add(EventBottleItem.of(null));
                if (floor >= 30 && floor % 10 == 0) loot.add(new ItemStack(Items.NETHERITE_SCRAP));
                if (floor > 60) {
                    loot.add(new ItemStack(com.snolred.wildplus.registry.ModItems22.VOID_SHARD.get(), floor / 10));
                    if (floor % 10 == 0) {
                        String[] pool = floor >= 90 ? new String[] {"ABYSS", "DEVOUR", "CHAIN", "WARP", "HEART"} : new String[] {"HARVEST", "FURY", "GUARD", "VIGOR", "RUSH"};
                        loot.add(new ItemStack(com.snolred.wildplus.registry.ModItems25.STONES.get(pool[server.overworld().getRandom().nextInt(pool.length)]).get()));
                    }
                }
            }
            for (ItemStack s : loot) if (!p.getInventory().add(s) && !s.isEmpty()) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
            p.heal(p.getMaxHealth() * 0.3F);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(floor + "층 돌파!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            p.sendSystemMessage(Component.literal(String.format("🗼 %d층 돌파 · %,d동%s", floor, coins, floor % 5 == 0 ? " + 보너스 아이템" : ""))
                    .withStyle(ChatFormatting.GOLD));
        }
        WeeklyGoals.add(server, WeeklyGoals.Kind.TOWER_FLOOR, 1);
        lv.playSound(null, CENTER.above(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, CENTER.getX() + 0.5, CENTER.getY() + 2, CENTER.getZ() + 0.5, 60, 5, 1, 5, 0);
        lv.sendParticles(ParticleTypes.END_ROD, EXIT.getX() + 0.5, EXIT.getY() + 1.5, EXIT.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.02);
        phase = Phase.REST;
        timer = lv.getGameTime() + 200;
    }

    // ───────── 탈락 · 끝 ─────────
    public static void eliminate(MinecraftServer server, UUID id, ServerPlayer p, String why) {
        if (!ACTIVE.remove(id)) return;
        int reached = BEST_FLOOR.getOrDefault(id, 0);
        CompoundTag me = ContentData.get(server).player("tower_p", id);
        if (reached > me.getIntOr("best", 0)) me.putInt("best", reached);
        ContentData.get(server).dirty();
        if (p != null) {
            if (bar != null) bar.removePlayer(p);
            sendHome(server, p);
            p.sendSystemMessage(Component.literal(String.format("🗼 탑에서 내려왔어요 (%s) · 이번 기록 %d층", why, reached)).withStyle(ChatFormatting.AQUA));
            if (reached > 0) WeeklyGoals.addPoints(p, reached * 4);
        }
        if (ACTIVE.isEmpty()) end(server);
    }

    private static void sendHome(MinecraftServer server, ServerPlayer p) {
        ServerLevel ow = server.overworld();
        BlockPos g = gate != null ? gate : BlockPos.of(state(server).getLongOr("gate", 0L));
        BlockPos to = ow.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g.offset(0, 0, 3));
        p.teleportTo(ow, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, Set.of(), p.getYRot(), 0F, true);
        p.clearFire();
        p.fallDistance = 0;
    }

    private static void end(MinecraftServer server) {
        ServerLevel lv = server.overworld();
        int team = 0;
        for (int v : BEST_FLOOR.values()) team = Math.max(team, v);
        CompoundTag st = state(server);
        String names = String.join(", ", NAMES.values());
        if (team > st.getIntOr("best", 0)) {
            st.putInt("best", team);
            st.putString("best_names", names);
            server.getPlayerList().broadcastSystemMessage(Component.literal("★ 하늘의 탑 신기록! " + team + "층 (" + names + ")")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        } else {
            server.getPlayerList().broadcastSystemMessage(Component.literal("🗼 하늘의 탑 도전 끝: " + team + "층 (" + names + ")")
                    .withStyle(ChatFormatting.AQUA), false);
        }
        server.getPlayerList().broadcastSystemMessage(DamageBoard.of("하늘의 탑 피해 순위 (" + team + "층까지)", DMG, NAMES), false);
        // 최근 기록 5개
        ListTag runs = st.getListOrEmpty("runs");
        CompoundTag run = new CompoundTag();
        run.putInt("floor", team);
        run.putString("names", names);
        runs.add(0, run);
        while (runs.size() > 5) runs.remove(runs.size() - 1);
        st.put("runs", runs);
        ContentData.get(server).dirty();
        for (UUID id : MOBS) {
            Entity e = lv.getEntity(id);
            if (e != null) e.discard();
        }
        MOBS.clear();
        bossId = null;
        BossSkills.clear();
        sweep(lv);
        if (bar != null) bar.removeAllPlayers();
        bar = null;
        for (int cx = (CENTER.getX() - R - 2) >> 4; cx <= (CENTER.getX() + R + 2) >> 4; cx++) {
            for (int cz = (CENTER.getZ() - R - 2) >> 4; cz <= (CENTER.getZ() + R + 2) >> 4; cz++) lv.setChunkForced(cx, cz, false);
        }
        running = false;
    }

    /** 탑 투기장 안 몬스터에게 준 피해 기록 (화살 등 원거리 포함) */
    @SubscribeEvent
    public static void onDamageDealt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!running || event.getEntity() instanceof Player) return;
        if (event.getSource().getEntity() instanceof ServerPlayer p && ACTIVE.contains(p.getUUID()) && inArena(p)) {
            DMG.merge(p.getUUID(), event.getHealthDamage(), Float::sum);
        }
    }

    /** 탑에서 쓰러지면 죽지 않고 탈락 (아이템 유지) */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!running || !(event.getEntity() instanceof ServerPlayer p) || !ACTIVE.contains(p.getUUID())) return;
        event.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        p.removeAllEffects();
        p.getFoodData().setFoodLevel(20);
        eliminate(p.level().getServer(), p.getUUID(), p, "쓰러짐");
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBooleanOr(TAG, false)) {
            // 탑 몬스터는 장비를 떨구지 않음 (보상은 층마다 따로)
            event.getDrops().removeIf(it -> !it.getItem().is(Items.ROTTEN_FLESH) && !it.getItem().is(Items.BONE) && !it.getItem().is(Items.STRING));
        }
    }

    /** 탑 안에서 접속이 끊겼다 다시 들어오면 입구로 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.level().dimension() == Level.OVERWORLD && inArena(p) && !ACTIVE.contains(p.getUUID())) {
            sendHome(p.level().getServer(), p);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().getPersistentData().getBooleanOr(TAG, false)
                && !MOBS.contains(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        running = false;
        ACTIVE.clear();
        MOBS.clear();
        if (bar != null) bar.removeAllPlayers();
        bar = null;
    }

    public static boolean isIn(UUID id) {
        return running && ACTIVE.contains(id);
    }

    public static String status() {
        return running ? String.format("진행 중: %d층 · %s · %d명", floor, String.join(", ", activeNames()), ACTIVE.size()) : "";
    }

    private static List<String> activeNames() {
        List<String> out = new ArrayList<>();
        for (UUID id : ACTIVE) out.add(NAMES.getOrDefault(id, "?"));
        return out;
    }
}

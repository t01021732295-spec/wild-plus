package com.snolred.wildplus.content;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModItems8;
import com.snolred.wildplus.registry.ModItems7;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 주말 레이드: 레이드 제단에서 시작하는 협동 보스전. 보스는 게임 속 7일마다 바뀌고, 깰 때마다 단계가 오른다 (최대 10단계).
 * 체력은 참가 인원과 단계에 따라 늘어난다. 3페이즈(66% · 33%)마다 패턴이 바뀌고, 바닥에 표시되는 범위 공격을 피해야 한다.
 * 클리어하면 참가자 모두 각자 보상 (이번 주 첫 클리어는 전부, 그 뒤로는 일부). 피해량 1등은 MVP 보너스.
 */
public final class Raid {
    public enum Boss {
        TITAN("대지의 거신", "땅 내리치기 · 지진 돌진 / 낙석 / 광폭화 + 약탈자 호위", 700, 16, 2.0F, BossEvent.BossBarColor.YELLOW),
        LICH("망령 군주", "송곳니 · 영혼 사슬 / 순간이동 · 번개 / 위더 스켈레톤 · 암흑", 520, 10, 1.8F, BossEvent.BossBarColor.PURPLE),
        FROST("서리 여왕", "얼음 화살 · 서리 폭발 / 얼음 감옥 · 얼음 가시 / 눈보라 + 호위병", 560, 10, 1.9F, BossEvent.BossBarColor.BLUE);

        public final String title, patterns;
        public final int health, damage;
        public final float scale;
        public final BossEvent.BossBarColor color;

        Boss(String title, String patterns, int health, int damage, float scale, BossEvent.BossBarColor color) {
            this.title = title;
            this.patterns = patterns;
            this.health = health;
            this.damage = damage;
            this.scale = scale;
            this.color = color;
        }

        EntityType<? extends Mob> type() {
            return switch (this) {
                case TITAN -> com.snolred.wildplus.registry.ModEntities.TITAN_BOSS.get();
                case LICH -> com.snolred.wildplus.registry.ModEntities.LICH_BOSS.get();
                case FROST -> com.snolred.wildplus.registry.ModEntities.FROST_BOSS.get();
            };
        }

        ItemStack trophy() {
            return new ItemStack(switch (this) {
                case TITAN -> ModItems8.TITAN_CORE.get();
                case LICH -> ModItems8.LICH_ORB.get();
                case FROST -> ModItems8.FROST_CROWN.get();
            });
        }
    }

    public static final String TAG = "wildplus_raid_boss", MINION = "wildplus_raid_minion";
    public static final int MAX_TIER = 10, TIME_LIMIT = 20 * 60 * 15;

    private record Strike(Vec3 pos, long at, float radius, float damage, int kind) {}

    private static UUID bossId;
    private static Boss boss;
    private static ServerLevel level;
    private static BlockPos altar;
    /** 레이드 전용 방 (하늘 위 원형 투기장) · 끝나면 돌아갈 제단 위치 */
    public static final BlockPos ARENA = new BlockPos(1_000_000, 200, -1_000_000);
    public static final int ARENA_R = 18, ARENA_H = 11;
    private static BlockPos home;
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> homeDim;
    private static final java.util.Set<UUID> FIGHTERS = new java.util.LinkedHashSet<>();
    private static ServerBossEvent bar;
    private static long startedAt, lastNear;
    private static int phase, tier;
    private static final Map<UUID, Float> DAMAGE = new LinkedHashMap<>();
    private static final Map<UUID, String> NAMES = new HashMap<>();
    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<UUID> MINIONS = new ArrayList<>();
    /** 얼음 감옥 블록 {위치, 사라질 시간} */
    private static final List<long[]> ICE = new ArrayList<>();

    private static void tickIce(long now, boolean all) {
        if (ICE.isEmpty() || level == null) return;
        var it = ICE.iterator();
        while (it.hasNext()) {
            long[] e = it.next();
            if (!all && now < e[1]) continue;
            BlockPos q = BlockPos.of(e[0]);
            if (level.getBlockState(q).is(net.minecraft.world.level.block.Blocks.PACKED_ICE)) {
                level.setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
            }
            it.remove();
        }
    }

    private Raid() {}

    public static Boss bossOfWeek() {
        return Boss.values()[(int) Math.floorMod(ContentData.week(), 3L)];
    }

    public static boolean active() {
        return bossId != null;
    }

    public static CompoundTag state(MinecraftServer server) {
        return ContentData.get(server).section("raid");
    }

    public static int tier(MinecraftServer server) {
        return Math.max(1, state(server).getIntOr("tier", 1));
    }

    public static long hpFor(Boss b, int tier, int players) {
        return Math.round(b.health * (1.0 + 0.6 * Math.max(0, players - 1)) * (1.0 + 0.3 * (tier - 1)));
    }

    public static List<ServerPlayer> nearby(ServerLevel lv, BlockPos pos, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : lv.players()) {
            if (!p.isSpectator() && p.isAlive() && p.distanceToSqr(Vec3.atCenterOf(pos)) <= r * r) out.add(p);
        }
        return out;
    }

    private static void set(Mob mob, Holder<Attribute> attr, double v) {
        AttributeInstance a = mob.getAttribute(attr);
        if (a != null) a.setBaseValue(v);
    }

    // ───────── 시작 ─────────
    public static void start(ServerPlayer opener, BlockPos altarPos) {
        if (active()) {
            opener.sendSystemMessage(Component.literal("이미 레이드가 진행 중이에요!").withStyle(ChatFormatting.RED));
            return;
        }
        ServerLevel from = (ServerLevel) opener.level();
        List<ServerPlayer> party = nearby(from, altarPos, 24);
        if (party.isEmpty()) party.add(opener);
        Boss b = bossOfWeek();
        ServerLevel lv = from.getServer().overworld();
        int t = tier(lv.getServer());
        Direction facing = Direction.NORTH;
        // 레이드 전용 방을 짓고 모두 데려간다
        home = altarPos.immutable();
        homeDim = from.dimension();
        state(lv.getServer()).putLong("home", home.asLong());
        ContentData.get(lv.getServer()).dirty();
        forceArena(lv, true);
        buildArena(lv, b);
        FIGHTERS.clear();
        for (int i = 0; i < party.size(); i++) {
            ServerPlayer p = party.get(i);
            double a = Math.PI / 2 + (i - (party.size() - 1) / 2.0) * 0.35;
            p.teleportTo(lv, ARENA.getX() + 0.5 + Math.cos(a) * 12, ARENA.getY() + 1, ARENA.getZ() + 0.5 + Math.sin(a) * 12,
                    java.util.Set.of(), 180F, 0F, true);
            FIGHTERS.add(p.getUUID());
        }
        BlockPos spawn = ARENA.above().north(6);
        Mob mob = b.type().create(lv, EntitySpawnReason.EVENT);
        if (mob == null) return;
        mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, facing.getOpposite().toYRot(), 0F);
        mob.finalizeSpawn(lv, lv.getCurrentDifficultyAt(spawn), EntitySpawnReason.EVENT, null);
        long hp = hpFor(b, t, party.size());
        set(mob, Attributes.MAX_HEALTH, Math.min(1024 * 16, hp));
        set(mob, Attributes.ATTACK_DAMAGE, b.damage * (1.0 + 0.15 * (t - 1)));
        set(mob, Attributes.ARMOR, 12 + t);
        set(mob, Attributes.ARMOR_TOUGHNESS, 4);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, 1.0);
        set(mob, Attributes.FOLLOW_RANGE, 48);
        set(mob, Attributes.SCALE, b.scale);
        mob.setHealth(mob.getMaxHealth());
        if (b == Boss.FROST) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0F);
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        mob.getPersistentData().putBoolean(TAG, true);
        mob.setCustomName(Component.literal(b.title + " · " + t + "단계").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.setTarget(party.get(lv.getRandom().nextInt(party.size())));
        bossId = mob.getUUID();
        lv.addFreshEntity(mob);

        boss = b;
        level = lv;
        altar = ARENA;
        tier = t;
        phase = 1;
        startedAt = lastNear = lv.getGameTime();
        DAMAGE.clear();
        NAMES.clear();
        STRIKES.clear();
        MINIONS.clear();
        for (ServerPlayer p : party) {
            DAMAGE.put(p.getUUID(), 0F);
            NAMES.put(p.getUUID(), p.getGameProfile().name());
        }
        bar = new ServerBossEvent(UUID.randomUUID(), mob.getCustomName(), b.color, BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setDarkenScreen(true);
        lv.playSound(null, spawn, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.8F);
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, 2, 0.5, 0.5, 0.5, 0);
        for (ServerPlayer p : party) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("⚔ " + b.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }
        lv.getServer().getPlayerList().broadcastSystemMessage(Component.literal(String.format("⚔ 주말 레이드 시작! %s %d단계 (참가 %d명, 체력 %,d)",
                b.title, t, party.size(), hp)).withStyle(ChatFormatting.GOLD), false);
    }

    // ───────── 진행 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (bossId == null || level == null) return;
        long now = level.getGameTime();
        Entity e = level.getEntity(bossId);
        if (!(e instanceof Mob mob) || !mob.isAlive()) {
            if (e == null && now - lastNear > 200) fail("보스가 사라졌어요");
            return;
        }
        tickStrikes(now);
        tickIce(now, false);
        if (now % 5 != 0) return;
        float ratio = mob.getHealth() / mob.getMaxHealth();
        bar.setProgress(Mth.clamp(ratio, 0F, 1F));
        long left = Math.max(0, TIME_LIMIT - (now - startedAt)) / 20;
        bar.setName(Component.literal(String.format("%s · %d단계 · %d페이즈  %d:%02d", boss.title, tier, phase, left / 60, left % 60))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        List<ServerPlayer> near = nearby(level, mob.blockPosition(), 48);
        for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (!near.contains(p)) bar.removePlayer(p);
        for (ServerPlayer p : near) {
            if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
            DAMAGE.putIfAbsent(p.getUUID(), 0F);
            NAMES.put(p.getUUID(), p.getGameProfile().name());
        }
        if (!near.isEmpty()) lastNear = now;
        if (now - lastNear > 600) {
            fail("모두 레이드 장소를 떠났어요");
            return;
        }
        if (now - startedAt > TIME_LIMIT) {
            fail("제한 시간 15분이 지났어요");
            return;
        }
        // 대상이 없으면 가장 가까운 참가자를 노림, 너무 멀리 끌려가면 제단 근처로 돌아옴
        if (mob.getTarget() == null && !near.isEmpty()) mob.setTarget(near.get(level.getRandom().nextInt(near.size())));
        if (mob.distanceToSqr(Vec3.atCenterOf(altar)) > 40 * 40) mob.teleportTo(altar.getX() + 0.5, altar.getY() + 2, altar.getZ() + 0.5);
        int newPhase = ratio > 0.66F ? 1 : ratio > 0.33F ? 2 : 3;
        if (newPhase > phase) {
            phase = newPhase;
            for (ServerPlayer p : near) {
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(phase + "페이즈!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            }
            level.playSound(null, mob.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0F, 0.9F);
            if (phase == 3) {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, 1, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            }
        }
        patterns(mob, near, now);
    }

    private static void patterns(Mob mob, List<ServerPlayer> near, long now) {
        RandomSource r = level.getRandom();
        long t = now - startedAt;
        float dmg = boss.damage * (1.0F + 0.15F * (tier - 1));
        switch (boss) {
            case TITAN -> {
                if (t % 160 == 0) strike(mob.position(), 30, 5.5F, dmg * 0.8F, 0);
                // 지진 돌진: 대상 쪽으로 땅이 줄지어 갈라지고 거신이 돌진
                if (t % 220 == 110 && !near.isEmpty()) {
                    ServerPlayer tg = near.get(r.nextInt(near.size()));
                    Vec3 dir = tg.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
                    for (int i = 1; i <= 8; i++) strike(mob.position().add(dir.scale(i * 2.2)), 12 + i * 3, 1.9F, dmg * 0.7F, 0);
                    mob.setDeltaMovement(dir.x * 1.6, 0.25, dir.z * 1.6);
                    for (ServerPlayer p : near) p.sendOverlayMessage(Component.literal("⚠ 지진 돌진! 옆으로 피하세요").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    level.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.7F);
                }
                if (phase >= 2 && t % 100 == 50) for (ServerPlayer p : near) strike(p.position(), 40, 2.5F, dmg * 0.6F, 1);
                if (phase >= 3 && t % 400 == 200) minions(mob, EntityTypes.VINDICATOR, 2);
            }
            case LICH -> {
                if (t % 200 == 0) {
                    double a = r.nextDouble() * Math.PI * 2;
                    BlockPos to = altar.offset(Mth.floor(Math.cos(a) * 8), 1, Mth.floor(Math.sin(a) * 8));
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 40, 0.5, 1, 0.5, 0.1);
                    mob.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                    level.playSound(null, to, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
                }
                // 영혼 사슬: 한 명을 끌어당기고 체력을 빨아들임
                if (t % 260 == 130 && !near.isEmpty()) {
                    ServerPlayer tg = near.get(r.nextInt(near.size()));
                    Vec3 d = mob.position().subtract(tg.position());
                    for (int i = 0; i < 12; i++) {
                        Vec3 at = tg.position().add(d.scale(i / 12.0));
                        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.2, at.z, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                    Vec3 n = d.normalize();
                    tg.push(n.x * 1.5, 0.35, n.z * 1.5);
                    tg.connection.send(new ClientboundSetEntityMotionPacket(tg));
                    tg.hurtServer(level, level.damageSources().indirectMagic(mob, mob), dmg * 0.5F);
                    tg.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    mob.heal(mob.getMaxHealth() * 0.02F);
                    tg.sendOverlayMessage(Component.literal("⚠ 영혼 사슬에 묶였어요!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                    level.playSound(null, tg.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5F, 0.6F);
                }
                if (phase >= 2 && t % 120 == 60) {
                    for (ServerPlayer p : near) {
                        for (int i = 0; i < 2; i++) {
                            Vec3 at = p.position().add(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
                            strike(at, 30, 2.0F, dmg * 0.7F, 2);
                        }
                    }
                }
                if (phase >= 3 && t % 300 == 150) {
                    minions(mob, EntityTypes.WITHER_SKELETON, 2);
                    for (ServerPlayer p : near) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                }
            }
            case FROST -> {
                if (t % 140 == 0) strike(mob.position(), 25, 6.0F, dmg * 0.6F, 3);
                if (phase >= 2 && t % 110 == 55) for (ServerPlayer p : near) strike(p.position(), 35, 2.0F, dmg * 0.7F, 3);
                // 얼음 감옥: 한 명을 얼음 속에 4초 가둠 (깨고 나올 수도 있음)
                if (phase >= 2 && t % 240 == 120 && !near.isEmpty()) {
                    ServerPlayer tg = near.get(r.nextInt(near.size()));
                    BlockPos c = tg.blockPosition();
                    for (BlockPos q : new BlockPos[]{c.north(), c.south(), c.east(), c.west(), c.north().above(), c.south().above(),
                            c.east().above(), c.west().above(), c.above(2)}) {
                        if (level.getBlockState(q).isAir()) {
                            level.setBlock(q, net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState(), 3);
                            ICE.add(new long[]{q.asLong(), level.getGameTime() + 80});
                        }
                    }
                    tg.teleportTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
                    tg.setTicksFrozen(tg.getTicksRequiredToFreeze() + 80);
                    tg.hurtServer(level, level.damageSources().freeze(), dmg * 0.4F);
                    tg.sendOverlayMessage(Component.literal("⚠ 얼음 감옥! 4초 버티거나 깨고 나오세요").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    level.playSound(null, c, SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.5F, 0.5F);
                }
                if (phase >= 3) {
                    if (t % 40 == 0) {
                        for (ServerPlayer p : near) {
                            p.hurtServer(level, level.damageSources().freeze(), 1.5F);
                            p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 40, p.getTicksFrozen() + 30));
                            level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 2, p.getZ(), 20, 2, 1, 2, 0.05);
                        }
                    }
                    if (t % 300 == 150) minions(mob, EntityTypes.STRAY, 3);
                }
            }
        }
    }

    private static void minions(Mob boss, EntityType<? extends Mob> type, int n) {
        for (int i = 0; i < n; i++) {
            Mob m = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (m == null) continue;
            m.snapTo(boss.getX() + level.getRandom().nextInt(5) - 2, boss.getY(), boss.getZ() + level.getRandom().nextInt(5) - 2, 0F, 0F);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            m.getPersistentData().putBoolean(MINION, true);
            if (boss.getTarget() != null) m.setTarget(boss.getTarget());
            MINIONS.add(m.getUUID());
            level.addFreshEntity(m);
        }
        level.sendParticles(ParticleTypes.SOUL, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 2, 1, 2, 0.05);
    }

    /** 바닥에 원을 그리고 delay 틱 뒤에 터지는 범위 공격. kind 0 땅울림 · 1 낙석 · 2 번개 · 3 서리 */
    private static void strike(Vec3 at, int delay, float radius, float damage, int kind) {
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
        if (Math.abs(ground.getY() - at.y) > 4) ground = BlockPos.containing(at);
        STRIKES.add(new Strike(new Vec3(at.x, ground.getY() + 0.1, at.z), level.getGameTime() + delay, radius, damage, kind));
    }

    private static void tickStrikes(long now) {
        if (STRIKES.isEmpty()) return;
        var it = STRIKES.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            ParticleOptions ring = switch (s.kind()) {
                case 2 -> ParticleTypes.SOUL_FIRE_FLAME;
                case 3 -> ParticleTypes.SNOWFLAKE;
                default -> ParticleTypes.FLAME;
            };
            if (now < s.at()) {
                if (now % 4 == 0) {
                    int pts = (int) (s.radius() * 10);
                    for (int i = 0; i < pts; i++) {
                        double a = i * Math.PI * 2 / pts;
                        level.sendParticles(ring, s.pos().x + Math.cos(a) * s.radius(), s.pos().y, s.pos().z + Math.sin(a) * s.radius(), 1, 0, 0, 0, 0);
                    }
                    if (s.kind() == 1) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, s.pos().x, s.pos().y + 8, s.pos().z, 2, 0.3, 0.3, 0.3, 0.0);
                }
                continue;
            }
            it.remove();
            SoundEvent snd = switch (s.kind()) {
                case 0 -> SoundEvents.GENERIC_EXPLODE.value();
                case 1 -> SoundEvents.ANVIL_LAND;
                case 2 -> SoundEvents.LIGHTNING_BOLT_THUNDER;
                default -> SoundEvents.GLASS_BREAK;
            };
            level.playSound(null, BlockPos.containing(s.pos()), snd, SoundSource.HOSTILE, 1.0F, s.kind() == 3 ? 0.6F : 1.0F);
            if (s.kind() == 2) {
                LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
                if (bolt != null) {
                    bolt.snapTo(s.pos().x, s.pos().y, s.pos().z, 0F, 0F);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
            level.sendParticles(s.kind() == 3 ? ParticleTypes.ITEM_SNOWBALL : ParticleTypes.EXPLOSION, s.pos().x, s.pos().y + 0.5, s.pos().z,
                    s.kind() == 3 ? 40 : 6, s.radius() * 0.4, 0.3, s.radius() * 0.4, 0.05);
            Entity b = level.getEntity(bossId);
            for (ServerPlayer p : level.players()) {
                double dx = p.getX() - s.pos().x, dz = p.getZ() - s.pos().z;
                if (dx * dx + dz * dz > s.radius() * s.radius() || Math.abs(p.getY() - s.pos().y) > 3) continue;
                p.hurtServer(level, b instanceof LivingEntity le ? level.damageSources().mobAttack(le) : level.damageSources().magic(), s.damage());
                switch (s.kind()) {
                    case 0 -> {
                        double len = Math.max(0.3, Math.sqrt(dx * dx + dz * dz));
                        p.push(dx / len * 1.2, 0.7, dz / len * 1.2);
                        p.connection.send(new ClientboundSetEntityMotionPacket(p));
                    }
                    case 3 -> {
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                        p.setTicksFrozen(p.getTicksRequiredToFreeze() + 60);
                    }
                    default -> {}
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (bossId == null || !event.getEntity().getUUID().equals(bossId)) return;
        if (event.getSource().getEntity() instanceof ServerPlayer p) {
            DAMAGE.merge(p.getUUID(), event.getHealthDamage(), Float::sum);
            NAMES.put(p.getUUID(), p.getGameProfile().name());
        }
    }

    // ───────── 끝 ─────────
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (bossId == null || !event.getEntity().getUUID().equals(bossId) || level == null) return;
        MinecraftServer server = level.getServer();
        long time = level.getGameTime() - startedAt;
        UUID mvp = null;
        float best = -1;
        for (var en : DAMAGE.entrySet()) if (en.getValue() > best) {
            best = en.getValue();
            mvp = en.getKey();
        }
        CompoundTag st = state(server);
        long week = ContentData.week();
        server.getPlayerList().broadcastSystemMessage(Component.literal(String.format("★ 레이드 클리어! %s %d단계 · %d분 %02d초", boss.title, tier,
                time / 1200, time / 20 % 60)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        server.getPlayerList().broadcastSystemMessage(DamageBoard.of("보스에게 준 피해 순위", DAMAGE, NAMES), false);
        for (UUID id : DAMAGE.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;
            CompoundTag me = ContentData.get(server).player("raid_p", id);
            boolean first = me.getLongOr("week", Long.MIN_VALUE) != week;
            me.putLong("week", week);
            me.putInt("clears", me.getIntOr("clears", 0) + 1);
            List<ItemStack> loot = new ArrayList<>();
            long coins;
            if (first) {
                coins = (8 + 2L * tier) * 100;
                loot.add(new ItemStack(ModItems7.RUBY.get(), 2 + tier / 3));
                loot.add(new ItemStack(Items.DIAMOND, 2 + tier / 4));
                loot.add(boss.trophy());
                if (tier >= 5) loot.add(new ItemStack(Items.NETHERITE_SCRAP, 1 + tier / 5));
                p.giveExperiencePoints(300 + 50 * tier);
                WeeklyGoals.addPoints(p, 60);
            } else {
                coins = (2 + tier / 2L) * 100;
                if (level.getRandom().nextFloat() < 0.3F) loot.add(boss.trophy());
                p.giveExperiencePoints(100);
            }
            if (id.equals(mvp)) {
                coins += 500;
                loot.add(new ItemStack(ModItems7.RUBY.get()));
                p.sendSystemMessage(Component.literal("👑 MVP 보너스: 500동 + 루비").withStyle(ChatFormatting.GOLD));
            }
            Coins.give(p, coins);
            for (ItemStack s : loot) if (!p.getInventory().add(s) && !s.isEmpty()) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
            p.sendSystemMessage(Component.literal(first ? String.format("이번 주 첫 클리어 보상: %,d동 · 루비 · 다이아몬드 · %s", coins, boss.trophy().getHoverName().getString())
                    : String.format("반복 클리어 보상: %,d동 (이번 주 첫 보상은 이미 받았어요)", coins)).withStyle(ChatFormatting.GREEN));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("레이드 클리어!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }
        st.putInt("tier", Math.min(MAX_TIER, tier + 1));
        st.putInt("clears", st.getIntOr("clears", 0) + 1);
        String bestKey = "best_" + boss.name();
        if (st.getLongOr(bestKey, Long.MAX_VALUE) > time) st.putLong(bestKey, time);
        st.putString("mvp", mvp == null ? "-" : NAMES.getOrDefault(mvp, "?"));
        st.putString("last", boss.title + " " + tier + "단계");
        ContentData.get(server).dirty();
        WeeklyGoals.add(server, WeeklyGoals.Kind.RAID_CLEAR, 1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, event.getEntity().getX(), event.getEntity().getY() + 1, event.getEntity().getZ(), 80, 1, 1, 1, 0.5);
        level.playSound(null, event.getEntity().blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        cleanup(false);
    }

    // ───────── 레이드 전용 방 ─────────
    private static void forceArena(ServerLevel lv, boolean on) {
        for (int cx = (ARENA.getX() - ARENA_R - 2) >> 4; cx <= (ARENA.getX() + ARENA_R + 2) >> 4; cx++) {
            for (int cz = (ARENA.getZ() - ARENA_R - 2) >> 4; cz <= (ARENA.getZ() + ARENA_R + 2) >> 4; cz++) lv.setChunkForced(cx, cz, on);
        }
    }

    /** 보스마다 다른 원형 투기장: 거신 = 현무암 협곡 · 망령 = 심층암 묘실 · 서리 = 얼음 궁전 */
    private static void buildArena(ServerLevel lv, Boss b) {
        net.minecraft.world.level.block.Block floor, accent, wall, pillar, light;
        switch (b) {
            case TITAN -> {
                floor = net.minecraft.world.level.block.Blocks.SMOOTH_BASALT; accent = net.minecraft.world.level.block.Blocks.POLISHED_BASALT;
                wall = net.minecraft.world.level.block.Blocks.BASALT; pillar = net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS;
                light = net.minecraft.world.level.block.Blocks.SHROOMLIGHT;
            }
            case LICH -> {
                floor = net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES; accent = net.minecraft.world.level.block.Blocks.SCULK;
                wall = net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE; pillar = net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN;
                light = net.minecraft.world.level.block.Blocks.SEA_LANTERN;
            }
            default -> {
                floor = net.minecraft.world.level.block.Blocks.PACKED_ICE; accent = net.minecraft.world.level.block.Blocks.SNOW_BLOCK;
                wall = net.minecraft.world.level.block.Blocks.BLUE_ICE; pillar = net.minecraft.world.level.block.Blocks.PACKED_ICE;
                light = net.minecraft.world.level.block.Blocks.SEA_LANTERN;
            }
        }
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var barrier = net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState();
        int cx = ARENA.getX(), cy = ARENA.getY(), cz = ARENA.getZ(), R = ARENA_R;
        for (int dx = -R - 1; dx <= R + 1; dx++) {
            for (int dz = -R - 1; dz <= R + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R + 1.5) continue;
                boolean edge = d > R;
                int ring = (int) d;
                var fl = (dx == 0 && dz == 0) ? light : (ring % 6 == 0 && (dx + dz) % 3 == 0) ? light : (ring % 4 == 0 ? accent : floor);
                lv.setBlock(new BlockPos(cx + dx, cy, cz + dz), (edge ? wall : fl).defaultBlockState(), 2);
                lv.setBlock(new BlockPos(cx + dx, cy - 1, cz + dz), wall.defaultBlockState(), 2);
                for (int y = 1; y <= ARENA_H; y++) {
                    var st = edge ? (y <= 4 || (y <= ARENA_H - 2 && (dx + dz) % 5 == 0) ? wall.defaultBlockState() : y == ARENA_H ? barrier : air)
                            : (y == ARENA_H ? barrier : air);
                    lv.setBlock(new BlockPos(cx + dx, cy + y, cz + dz), st, 2);
                }
                lv.setBlock(new BlockPos(cx + dx, cy + ARENA_H + 1, cz + dz), barrier, 2);
            }
        }
        // 엄폐용 기둥 4개 (꼭대기에 조명)
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            int px = cx + (int) Math.round(Math.cos(a) * 10), pz = cz + (int) Math.round(Math.sin(a) * 10);
            for (int y = 1; y <= 5; y++) {
                lv.setBlock(new BlockPos(px, cy + y, pz), pillar.defaultBlockState(), 2);
                lv.setBlock(new BlockPos(px + 1, cy + y, pz), pillar.defaultBlockState(), 2);
            }
            lv.setBlock(new BlockPos(px, cy + 6, pz), light.defaultBlockState(), 2);
        }
    }

    /** 레이드 방에 있는 사람을 제단으로 돌려보낸다 */
    private static void sendHome(ServerPlayer p) {
        ServerLevel to = homeDim != null ? p.level().getServer().getLevel(homeDim) : null;
        if (to == null) to = p.level().getServer().overworld();
        BlockPos h = home != null ? home : BlockPos.of(state(p.level().getServer()).getLongOr("home", 0L));
        if (home == null) h = to.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, h).below(2);
        p.teleportTo(to, h.getX() + 0.5, h.getY() + 2, h.getZ() + 0.5, java.util.Set.of(), p.getYRot(), 0F, true);
        p.clearFire();
    }

    private static boolean inArena(net.minecraft.world.entity.Entity e) {
        return e.level() == level && Math.abs(e.getX() - ARENA.getX()) < ARENA_R + 4 && Math.abs(e.getZ() - ARENA.getZ()) < ARENA_R + 4
                && Math.abs(e.getY() - ARENA.getY()) < 20;
    }

    /** 레이드가 끝난 뒤(서버 재시작 등) 레이드 방에 남아 있으면 제단으로 */
    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (bossId == null && event.getEntity() instanceof ServerPlayer p && Math.abs(p.getX() - ARENA.getX()) < ARENA_R + 4
                && Math.abs(p.getZ() - ARENA.getZ()) < ARENA_R + 4) {
            sendHome(p);
        }
    }

    /** 레이드 방에서 쓰러지면 죽지 않고 제단으로 (아이템 유지) */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (bossId == null || !(event.getEntity() instanceof ServerPlayer p) || !FIGHTERS.contains(p.getUUID()) || !inArena(p)) return;
        event.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        p.removeAllEffects();
        FIGHTERS.remove(p.getUUID());
        sendHome(p);
        p.sendSystemMessage(Component.literal("💀 레이드에서 쓰러졌어요 (아이템은 그대로) · 제단으로 돌아왔어요").withStyle(ChatFormatting.RED));
        if (FIGHTERS.isEmpty()) fail("모두 쓰러졌어요");
    }

    private static void fail(String why) {
        if (level != null) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✖ 레이드 실패: " + why).withStyle(ChatFormatting.RED), false);
            if (!DAMAGE.isEmpty()) level.getServer().getPlayerList().broadcastSystemMessage(DamageBoard.of("보스에게 준 피해 순위", DAMAGE, NAMES), false);
        }
        cleanup(true);
    }

    private static void cleanup(boolean removeBoss) {
        if (level != null) {
            tickIce(0, true);
            for (ServerPlayer p : new ArrayList<>(level.players())) if (inArena(p)) sendHome(p);
            if (removeBoss && bossId != null) {
                Entity e = level.getEntity(bossId);
                if (e != null) e.discard();
            }
            for (UUID id : MINIONS) {
                Entity e = level.getEntity(id);
                if (e != null) e.discard();
            }
        }
        if (bar != null) bar.removeAllPlayers();
        bar = null;
        bossId = null;
        STRIKES.clear();
        MINIONS.clear();
        FIGHTERS.clear();
        if (level != null) forceArena(level, false);
    }

    /** 서버를 다시 켰을 때 남아 있는 레이드 보스·부하는 정리 */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        var data = event.getEntity().getPersistentData();
        if ((data.getBooleanOr(TAG, false) && !event.getEntity().getUUID().equals(bossId))
                || (data.getBooleanOr(MINION, false) && !MINIONS.contains(event.getEntity().getUUID()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        var d = event.getEntity().getPersistentData();
        if (d.getBooleanOr(TAG, false) || d.getBooleanOr(MINION, false)) event.getDrops().clear();
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        var d = event.getEntity().getPersistentData();
        if (d.getBooleanOr(TAG, false) || d.getBooleanOr(MINION, false)) event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        if (bar != null) bar.removeAllPlayers();
        bar = null;
        bossId = null;
        level = null;
        STRIKES.clear();
        MINIONS.clear();
    }

    public static String status() {
        if (!active()) return "";
        long left = Math.max(0, TIME_LIMIT - (level.getGameTime() - startedAt)) / 20;
        return String.format("진행 중: %s %d단계 · %d페이즈 · %d:%02d 남음", boss.title, tier, phase, left / 60, left % 60);
    }
}

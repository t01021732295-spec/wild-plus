package com.snolred.wildplus.omen;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.event.EliteMobs;
import com.snolred.wildplus.registry.ModItems19;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.8 역습격: 우민 전초 기지의 깃발에 흉조 병을 쓰면 우리가 쳐들어간다.
 * 흉조 세기에 따라 3~7 웨이브 (약탈자 · 변명자 · 소환사 · 마녀 · 마지막에 파괴수), 다 이기면 큰 보상 + 전초 기지 점령:
 * 우민 깃발이 내려가고, 그 자리에 공용 워프 "⚑ 전진 기지"가 생기며, 다시는 약탈자가 생기지 않는다.
 */
public final class CounterRaid {
    private CounterRaid() {}

    static final String TAG = "wildplus_counter";

    static final class Siege {
        long key;
        BlockPos center;
        BoundingBox box;
        int waves, wave;
        long nextWaveAt, lastSeen;
        UUID starter;
        final List<UUID> mobs = new ArrayList<>();
        final Map<UUID, Float> dmg = new HashMap<>();
        final Map<UUID, String> names = new HashMap<>();
        ServerBossEvent bar;
    }

    private static final Map<Long, Siege> SIEGES = new HashMap<>();

    private static CompoundTag captured(MinecraftServer s) {
        return ContentData.get(s).section("omen_outposts");
    }

    public static boolean isCaptured(MinecraftServer s, BlockPos pos) {
        CompoundTag c = captured(s);
        for (String k : c.keySet()) {
            CompoundTag t = c.getCompoundOrEmpty(k);
            if (pos.getX() >= t.getIntOr("x0", 0) - 16 && pos.getX() <= t.getIntOr("x1", 0) + 16
                    && pos.getZ() >= t.getIntOr("z0", 0) - 16 && pos.getZ() <= t.getIntOr("z1", 0) + 16) {
                return true;
            }
        }
        return false;
    }

    // ───────── 시작 ─────────
    @SubscribeEvent
    public static void onBanner(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getLevel() instanceof ServerLevel level) || event.getHand() != InteractionHand.MAIN_HAND) return;
        BlockState state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof AbstractBannerBlock)) return;
        ItemStack hand = p.getMainHandItem();
        boolean themed = hand.getItem() instanceof com.snolred.wildplus.item.ThemedOmenItem;
        if (!Omen.isOminousBottle(hand) && !themed) return;
        if (level.dimension() != Level.OVERWORLD) return;
        StructureStart start = level.structureManager().getStructureWithPieceAt(event.getPos(), h -> h.is(BuiltinStructures.PILLAGER_OUTPOST));
        if (start == null || !start.isValid()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        BoundingBox box = start.getBoundingBox();
        BlockPos c = box.getCenter();
        long key = BlockPos.asLong(c.getX(), 0, c.getZ());
        if (isCaptured(level.getServer(), c)) {
            p.sendOverlayMessage(Component.literal("이미 점령한 전진 기지예요").withStyle(ChatFormatting.AQUA));
            return;
        }
        if (SIEGES.containsKey(key)) {
            p.sendOverlayMessage(Component.literal("역습격이 이미 진행 중이에요").withStyle(ChatFormatting.RED));
            return;
        }
        int amp = themed ? 2 : Omen.amp(hand);
        if (!p.getAbilities().instabuild) hand.shrink(1);
        Siege s = new Siege();
        s.key = key;
        s.box = box;
        s.center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(c.getX(), 0, c.getZ()));
        s.waves = 3 + amp;
        s.wave = 0;
        s.nextWaveAt = level.getGameTime() + 100;
        s.lastSeen = level.getGameTime();
        s.starter = p.getUUID();
        s.bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("⚑ 역습격"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_6);
        SIEGES.put(key, s);
        level.playSound(null, event.getPos(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 6.0F, 1.2F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, event.getPos().getX() + 0.5, event.getPos().getY() + 1, event.getPos().getZ() + 0.5, 40, 0.5, 1, 0.5, 0.05);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(String.format("⚑ %s 님이 우민 전초 기지(%d, %d)에 역습격을 걸었어요! %d 웨이브 · 이기면 점령",
                p.getName().getString(), c.getX(), c.getZ(), s.waves)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
    }

    // ───────── 진행 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (SIEGES.isEmpty() || event.getServer().getTickCount() % 20 != 0) return;
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        for (Siege s : new ArrayList<>(SIEGES.values())) {
            List<ServerPlayer> near = new ArrayList<>();
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && p.distanceToSqr(s.center.getX(), s.center.getY(), s.center.getZ()) < 96 * 96) near.add(p);
            }
            if (!near.isEmpty()) s.lastSeen = now;
            if (now - s.lastSeen > 1200) {
                finish(level, s, false);
                continue;
            }
            s.mobs.removeIf(u -> {
                Entity e = level.getEntity(u);
                return e == null || !e.isAlive();
            });
            if (s.mobs.isEmpty()) {
                if (s.wave >= s.waves) {
                    finish(level, s, true);
                    continue;
                }
                if (s.nextWaveAt == 0) s.nextWaveAt = now + 100;
                if (now >= s.nextWaveAt) {
                    s.wave++;
                    s.nextWaveAt = 0;
                    spawnWave(level, s);
                }
            }
            for (ServerPlayer p : near) {
                s.names.put(p.getUUID(), p.getName().getString());
                if (!s.bar.getPlayers().contains(p)) s.bar.addPlayer(p);
            }
            for (ServerPlayer p : new ArrayList<>(s.bar.getPlayers())) if (!near.contains(p)) s.bar.removePlayer(p);
            s.bar.setName(Component.literal(String.format("⚑ 역습격 · 웨이브 %d/%d · 남은 우민 %d%s", s.wave, s.waves, s.mobs.size(),
                    s.mobs.isEmpty() && s.wave < s.waves ? " · 다음 웨이브 준비 중" : "")).withStyle(ChatFormatting.AQUA));
            s.bar.setProgress(Mth.clamp((s.wave - (s.mobs.isEmpty() ? 0F : 0.5F)) / (float) s.waves, 0F, 1F));
        }
    }

    private static void spawnWave(ServerLevel level, Siege s) {
        int k = s.wave;
        List<EntityType<? extends Mob>> list = new ArrayList<>();
        for (int i = 0; i < 2 + k; i++) list.add(EntityTypes.PILLAGER);
        for (int i = 0; i < 1 + k / 2; i++) list.add(EntityTypes.VINDICATOR);
        if (k >= 3) list.add(EntityTypes.EVOKER);
        if (k >= 6) list.add(EntityTypes.EVOKER);
        if (k % 2 == 0) list.add(EntityTypes.WITCH);
        if (k == s.waves) list.add(EntityTypes.RAVAGER);
        var rnd = level.getRandom();
        for (EntityType<? extends Mob> t : list) {
            Mob m = t.create(level, EntitySpawnReason.EVENT);
            if (m == null) continue;
            double ang = rnd.nextDouble() * Math.PI * 2, d = 10 + rnd.nextDouble() * 10;
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(Mth.floor(s.center.getX() + Math.cos(ang) * d), 0, Mth.floor(s.center.getZ() + Math.sin(ang) * d)));
            m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360F, 0F);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
            m.getPersistentData().putLong(TAG, s.key);
            if (m instanceof Raider r) r.setCanJoinRaid(false);
            m.setPersistenceRequired();
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 10, 0, false, false));
            if (rnd.nextFloat() < 0.25F + 0.04F * k && t != EntityTypes.RAVAGER) EliteMobs.makeElite(m, 1);
            level.addFreshEntity(m);
            s.mobs.add(m.getUUID());
            ServerPlayer target = level.getNearestPlayer(m, 64) instanceof ServerPlayer sp ? sp : null;
            if (target != null) m.setTarget(target);
        }
        level.playSound(null, s.center, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 6.0F, 1.0F);
        for (ServerPlayer p : s.bar.getPlayers()) {
            p.sendOverlayMessage(Component.literal("⚑ 웨이브 " + k + "/" + s.waves + " — 우민 " + s.mobs.size() + "명").withStyle(ChatFormatting.AQUA));
        }
    }

    private static void finish(ServerLevel level, Siege s, boolean won) {
        MinecraftServer server = level.getServer();
        SIEGES.remove(s.key);
        s.bar.removeAllPlayers();
        for (UUID u : s.mobs) {
            Entity e = level.getEntity(u);
            if (e != null) e.discard();
        }
        if (!won) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("⚑ 역습격 실패 — 아무도 전초 기지에 남지 않았어요").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        // 보상
        float total = 0;
        for (float v : s.dmg.values()) total += v;
        Map<UUID, Float> share = new HashMap<>(s.dmg);
        for (UUID u : s.names.keySet()) share.putIfAbsent(u, 0F);
        int n = Math.max(1, share.size());
        WildData wd = WildData.get(server);
        long base = 20_000L * s.waves;
        for (var e : share.entrySet()) {
            double sh = total > 0 ? e.getValue() / total : 1.0 / n;
            double factor = Math.max(0.6, Math.min(1.8, 0.6 + 0.4 * sh * n));
            long money = Math.round(base * factor / 100.0) * 100;
            wd.addBalance(e.getKey(), money);
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) continue;
            var rnd = level.getRandom();
            Omen.give(p, new ItemStack(Items.EMERALD_BLOCK, Math.max(1, s.waves / 2)));
            Omen.give(p, Omen.withAmp(new ItemStack(Items.OMINOUS_BOTTLE), Math.min(4, 1 + rnd.nextInt(3))));
            if (rnd.nextFloat() < 0.3F + 0.05F * s.waves) Omen.give(p, new ItemStack(ModItems19.LIFE_ESSENCE.get()));
            p.sendSystemMessage(Component.literal(String.format("🏆 역습격 승리! %s 통장 입금 (기여 %d%%) · 에메랄드 블록 · 흉조 병", Omen.won(money), Math.round(sh * 100)))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        wd.setDirty();
        capture(level, s);
    }

    /** 점령: 우민 깃발 내리기 · 전진 기지 워프 · 기록 */
    private static void capture(ServerLevel level, Siege s) {
        MinecraftServer server = level.getServer();
        BoundingBox b = s.box;
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof AbstractBannerBlock)) continue;
            level.setBlock(p.immutable(), Blocks.AIR.defaultBlockState(), 3); // 우민 깃발을 내림
        }
        String name = String.format("⚑ 전진 기지 (%d, %d)", s.center.getX(), s.center.getZ());
        BlockPos warp = s.center;
        if (level.getBlockState(warp).canBeReplaced()) {
            level.setBlock(warp, com.snolred.wildplus.registry.ModBlocks.WARP_POINT.get().defaultBlockState(), 3);
            ServerPlayer owner = server.getPlayerList().getPlayer(s.starter);
            if (owner != null && level.getBlockEntity(warp) instanceof com.snolred.wildplus.block.WarpPointBlockEntity be) {
                var w = com.snolred.wildplus.economy.Warps.register(owner, level, warp, name);
                be.setWarpId(w.id);
            }
        }
        CompoundTag t = new CompoundTag();
        t.putInt("x0", b.minX());
        t.putInt("x1", b.maxX());
        t.putInt("z0", b.minZ());
        t.putInt("z1", b.maxZ());
        t.putString("by", s.starter.toString());
        captured(server).put(String.valueOf(s.key), t);
        ContentData.get(server).dirty();
        level.sendParticles(ParticleTypes.FIREWORK, s.center.getX() + 0.5, s.center.getY() + 3, s.center.getZ() + 0.5, 80, 3, 3, 3, 0.1);
        server.getPlayerList().broadcastSystemMessage(Component.literal("⚑ 전초 기지를 점령했어요! " + name
                + " — 우민 깃발을 내리고 워프가 생겼어요 · 이제 약탈자가 생기지 않아요").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
    }

    // ───────── 기여도 · 점령지 스폰 막기 ─────────
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (SIEGES.isEmpty()) return;
        LivingEntity v = event.getEntity();
        long key = v.getPersistentData().getLongOr(TAG, 0L);
        if (key == 0L) return;
        Siege s = SIEGES.get(key);
        if (s == null) return;
        if (event.getSource().getEntity() instanceof ServerPlayer p) {
            s.dmg.merge(p.getUUID(), event.getHealthDamage(), Float::sum);
            s.names.put(p.getUUID(), p.getName().getString());
        } else if (event.getSource().getEntity() instanceof com.snolred.wildplus.entity.MercenaryEntity m && m.ownerId() != null) {
            s.dmg.merge(m.ownerId(), event.getHealthDamage(), Float::sum);
        }
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Raider) || event.getEntity().getPersistentData().getLongOr(TAG, 0L) != 0L) return;
        EntitySpawnReason r = event.getSpawnType();
        if (r != EntitySpawnReason.NATURAL && r != EntitySpawnReason.STRUCTURE && r != EntitySpawnReason.PATROL && r != EntitySpawnReason.CHUNK_GENERATION) return;
        if (!(event.getLevel().getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (isCaptured(level.getServer(), event.getEntity().blockPosition())) event.setSpawnCancelled(true);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        for (Siege s : SIEGES.values()) s.bar.removeAllPlayers();
        SIEGES.clear();
    }
}

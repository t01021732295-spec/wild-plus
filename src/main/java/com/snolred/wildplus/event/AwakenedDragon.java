package com.snolred.wildplus.event;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModItems22;
import com.snolred.wildplus.registry.ModItems23;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 4.0 각성 드래곤: 첫 처치 뒤 각성의 핵으로 소환. 단계 I~V 패턴 누적. 유적 보스도 예고 장판(warn)을 함께 쓴다. */
public final class AwakenedDragon {
    private AwakenedDragon() {}

    public static final String STAGE = "wildplus_awakened";
    private static final String CRYSTAL = "wildplus_awakened_crystal", REWARD_AT = "wildplus_dragon_reward", REGEN = "wildplus_regen";
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V"};
    private static final int[] HP = {0, 400, 600, 900, 1300, 1800};
    private static final double[] PARTY = {0.6, 0.6, 0.75, 0.9, 1.0};
    private static final int[] SHARDS = {0, 8, 12, 16, 24, 32};
    private static final long WEEK = 168000L;
    private static final double RANGE = 160;

    private record Warn(ServerLevel level, Vec3 at, double radius, int fireAt, float damage, boolean corrupt, Mob source) {}

    private static final List<Warn> WARNS = new ArrayList<>();
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Float>> DAMAGE = new HashMap<>();
    private static final Map<UUID, Integer> CORRUPT = new HashMap<>();
    private static final Map<UUID, Integer> PULL_AT = new HashMap<>();

    private static int stage(Entity e) {
        return e.getPersistentData().getIntOr(STAGE, 0);
    }

    /** 바닥에 예고 표시 뒤 폭발 */
    public static void warn(ServerLevel level, Vec3 ground, double radius, int delay, float damage, boolean corrupt, Mob source) {
        WARNS.add(new Warn(level, ground, radius, level.getServer().getTickCount() + delay, damage, corrupt, source));
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        int st = ModItems23.coreStage(event.getItemStack().getItem());
        if (st == 0 || !(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        event.setCanceled(true);
        EnderDragonFight fight = level.getDragonFight();
        if (level.dimension() != Level.END || fight == null || p.blockPosition().distSqr(new BlockPos(0, p.getBlockY(), 0)) > 80 * 80) {
            p.sendOverlayMessage(Component.literal("엔드 중앙 섬에서만 쓸 수 있어요").withStyle(ChatFormatting.RED));
            return;
        }
        if (!fight.hasPreviouslyKilledDragon()) {
            p.sendOverlayMessage(Component.literal("먼저 엔더 드래곤을 쓰러뜨려야 해요").withStyle(ChatFormatting.RED));
            return;
        }
        if (!level.getDragons().isEmpty()) {
            p.sendOverlayMessage(Component.literal("이미 드래곤이 깨어 있어요").withStyle(ChatFormatting.RED));
            return;
        }
        EnderDragon d = EntityTypes.ENDER_DRAGON.create(level, EntitySpawnReason.EVENT);
        if (d == null) return;
        int party = Math.min(4, (int) level.players().stream().filter(o -> o.distanceToSqr(p) < RANGE * RANGE && !o.isSpectator()).count());
        d.getPersistentData().putInt(STAGE, st);
        d.setDragonFight(fight);
        d.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        d.snapTo(0.5, 128.0, 0.5, level.getRandom().nextFloat() * 360F, 0F);
        d.setCustomName(Component.literal("각성 드래곤 " + ROMAN[st]));
        AttributeInstance hp = d.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(HP[st] * PARTY[party]);
        d.setHealth(d.getMaxHealth());
        level.addFreshEntity(d);
        placeCrystals(level, 99);
        event.getItemStack().consume(1, p);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("🐉 각성 드래곤 " + ROMAN[st] + "단계가 깨어났어요!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 0.7F);
    }

    /** 빈 기둥 꼭대기에 크리스탈을 최대 max 개 놓고 놓은 개수를 돌려 줌 */
    private static int placeCrystals(ServerLevel level, int max) {
        int made = 0;
        for (int i = 0; i < 10 && made < max; i++) {
            int x = Mth.floor(42.0 * Math.cos(2.0 * (-Math.PI + Math.PI / 10.0 * i)));
            int z = Mth.floor(42.0 * Math.sin(2.0 * (-Math.PI + Math.PI / 10.0 * i)));
            // 바닐라 기둥 꼭대기의 기반암을 찾는다
            for (int y = 125; y > 60; y--) {
                if (!level.getBlockState(new BlockPos(x, y, z)).is(Blocks.BEDROCK)) continue;
                Vec3 at = new Vec3(x + 0.5, y + 1, z + 0.5);
                if (level.getEntitiesOfClass(EndCrystal.class, new net.minecraft.world.phys.AABB(at, at).inflate(1.5)).isEmpty()) {
                    EndCrystal c = EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.EVENT);
                    if (c != null) {
                        c.snapTo(at.x, at.y, at.z, 0F, 0F);
                        c.setShowBottom(true);
                        c.getPersistentData().putBoolean(CRYSTAL, true);
                        level.addFreshEntity(c);
                        made++;
                    }
                }
                break;
            }
        }
        return made;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EnderDragon d) || stage(d) == 0) return;
        BARS.computeIfAbsent(d.getUUID(), id -> new ServerBossEvent(UUID.randomUUID(), Component.literal("각성 드래곤 " + ROMAN[stage(d)]).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10));
    }

    private static List<ServerPlayer> targets(ServerLevel level, Entity near) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && p.isAlive() && !p.isCreative() && p.distanceToSqr(near) < RANGE * RANGE) out.add(p);
        return out;
    }

    private static Vec3 ground(ServerLevel level, double x, double z) {
        return new Vec3(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)), z);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();
        tickWarns(now);
        for (Iterator<Map.Entry<UUID, Integer>> it = CORRUPT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> en = it.next();
            if (now < en.getValue()) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            AttributeInstance a = p == null ? null : p.getAttribute(Attributes.MAX_HEALTH);
            if (a != null) a.removeModifier(WildPlus.id("void_corrupt"));
            it.remove();
        }
        ServerLevel level = server.getLevel(Level.END);
        if (level == null || BARS.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, ServerBossEvent>> it = BARS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, ServerBossEvent> en = it.next();
            ServerBossEvent bar = en.getValue();
            if (!(level.getEntity(en.getKey()) instanceof EnderDragon d) || !d.isAlive()) {
                bar.removeAllPlayers();
                it.remove();
                continue;
            }
            bar.setProgress(Mth.clamp(d.getHealth() / d.getMaxHealth(), 0F, 1F));
            for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (p.level() != level || p.distanceToSqr(d) > RANGE * RANGE) bar.removePlayer(p);
            for (ServerPlayer p : level.players()) if (p.distanceToSqr(d) <= RANGE * RANGE && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            patterns(level, d, now);
        }
    }

    private static boolean every(EnderDragon d, int period, boolean rage) {
        return d.tickCount % (rage ? Math.max(20, period * 3 / 5) : period) == 0;
    }

    private static void patterns(ServerLevel level, EnderDragon d, int now) {
        int st = stage(d);
        List<ServerPlayer> t = targets(level, d);
        if (t.isEmpty() || d.getHealth() <= 0) return;
        var r = level.getRandom();
        boolean rage = st >= 5 && d.getHealth() <= d.getMaxHealth() * 0.3F;
        if (every(d, 240, rage)) { // I 공허 브레스
            ServerPlayer p = t.get(r.nextInt(t.size()));
            warn(level, ground(level, p.getX(), p.getZ()), 5.0, 30, 6 + st, true, d);
        }
        var data = d.getPersistentData();
        if (st >= 2 && d.tickCount % 600 == 0 && data.getIntOr(REGEN, 0) < 3 && placeCrystals(level, 1) > 0) { // II 크리스탈 재생
            data.putInt(REGEN, data.getIntOr(REGEN, 0) + 1);
            for (ServerPlayer p : t) p.sendOverlayMessage(Component.literal("크리스탈이 되살아났어요!").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (st >= 3 && every(d, 400, rage) && !PULL_AT.containsKey(d.getUUID())) { // III 인력 예고
            PULL_AT.put(d.getUUID(), now + 60);
            for (ServerPlayer p : t) p.sendOverlayMessage(Component.literal("공허가 끌어당깁니다! 3초 뒤").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            level.playSound(null, d.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.5F);
        }
        Integer pull = PULL_AT.get(d.getUUID());
        if (pull != null && now >= pull) {
            PULL_AT.remove(d.getUUID());
            for (ServerPlayer p : t) {
                Vec3 to = d.position().subtract(p.position());
                if (to.lengthSqr() < 1) continue;
                p.push(to.normalize().scale(Math.min(2.4, to.length() * 0.05)));
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
            for (int i = 0; i < 2; i++) {
                ServerPlayer p = t.get(r.nextInt(t.size()));
                Mob m = EntityTypes.ENDERMAN.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (m == null) continue;
                m.snapTo(p.getX() + r.nextInt(9) - 4, p.getY(), p.getZ() + r.nextInt(9) - 4, 0F, 0F);
                m.setTarget(p);
                level.addFreshEntity(m);
            }
        }
        if (st >= 4 && every(d, 300, rage)) { // IV 공허 장판
            for (int i = 0; i < 4; i++) {
                ServerPlayer p = t.get(r.nextInt(t.size()));
                warn(level, ground(level, p.getX() + r.nextInt(13) - 6, p.getZ() + r.nextInt(13) - 6), 4.0, 40, 8, false, d);
            }
        }
        if (rage && d.tickCount % 40 == 0) { // V 운석
            ServerPlayer p = t.get(r.nextInt(t.size()));
            Vec3 at = ground(level, p.getX() + r.nextInt(17) - 8, p.getZ() + r.nextInt(17) - 8);
            warn(level, at, 3.0, 30, 10, false, d);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 18, at.z, 20, 0.3, 4.0, 0.3, 0.1);
        }
    }

    private static void tickWarns(int now) {
        for (Iterator<Warn> it = WARNS.iterator(); it.hasNext(); ) {
            Warn w = it.next();
            if (now < w.fireAt) {
                if (now % 4 == 0) {
                    for (int i = 0; i < 18; i++) {
                        double a = Math.toRadians(i * 20);
                        w.level.sendParticles(ParticleTypes.REVERSE_PORTAL, w.at.x + Math.cos(a) * w.radius, w.at.y + 0.15, w.at.z + Math.sin(a) * w.radius, 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            it.remove();
            w.level.sendParticles(ParticleTypes.EXPLOSION, w.at.x, w.at.y + 0.5, w.at.z, 3, w.radius * 0.4, 0.2, w.radius * 0.4, 0);
            w.level.sendParticles(ParticleTypes.PORTAL, w.at.x, w.at.y + 0.5, w.at.z, 60, w.radius * 0.5, 0.4, w.radius * 0.5, 0.4);
            w.level.playSound(null, BlockPos.containing(w.at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9F, 1.4F);
            for (ServerPlayer p : w.level.players()) {
                if (p.isSpectator() || p.position().distanceToSqr(w.at) > w.radius * w.radius + 4) continue;
                p.hurtServer(w.level, w.source != null && w.source.isAlive() ? w.level.damageSources().indirectMagic(w.source, w.source) : w.level.damageSources().magic(), w.damage);
                if (w.corrupt) corrupt(p, now);
            }
        }
    }

    private static void corrupt(ServerPlayer p, int now) {
        AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        var id = WildPlus.id("void_corrupt");
        if (a.getModifier(id) == null) a.addTransientModifier(new AttributeModifier(id, -6.0, AttributeModifier.Operation.ADD_VALUE));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        CORRUPT.put(p.getUUID(), now + 400);
        p.sendOverlayMessage(Component.literal("공허 침식: 20초간 최대 체력 -3칸").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof EnderDragon d && stage(d) > 0 && event.getSource().getEntity() instanceof ServerPlayer p) {
            DAMAGE.computeIfAbsent(d.getUUID(), k -> new HashMap<>()).merge(p.getUUID(), event.getNewDamage(), Float::sum);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon d) || !(d.level() instanceof ServerLevel level)) return;
        int st = stage(d);
        if (st == 0) {
            EnderDragonFight f = level.getDragonFight();
            if (f != null && !f.hasPreviouslyKilledDragon()) {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("🌌 공허가 깨어났어요. 이제 각성의 핵으로 드래곤을 다시 부를 수 있어요").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            return;
        }
        long day = level.getGameTime();
        for (ServerPlayer p : targets(level, d)) {
            long last = p.getPersistentData().getLongOr(REWARD_AT, -WEEK);
            boolean full = day - last >= WEEK;
            give(p, new ItemStack(ModItems22.VOID_SHARD.get(), full ? SHARDS[st] : SHARDS[st] / 4));
            if (full) {
                p.getPersistentData().putLong(REWARD_AT, day);
                give(p, new ItemStack(ModItems22.VOID_STONE.get(), st));
                if (st >= 3) give(p, new ItemStack(ModItems22.VOID_HEART.get(), st >= 5 ? 2 : 1));
                p.sendSystemMessage(Component.literal("🎁 이번 주 첫 처치 보상을 받았어요").withStyle(ChatFormatting.GOLD));
            }
        }
        Map<UUID, Float> dmg = DAMAGE.remove(d.getUUID());
        if (dmg != null) {
            var list = new ArrayList<>(dmg.entrySet());
            list.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            int i = 1;
            for (var en : list) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(en.getKey());
                String name = p == null ? "?" : p.getName().getString();
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(i++ + "위 " + name + " · 피해 " + Math.round(en.getValue())).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        for (EndCrystal c : level.getEntitiesOfClass(EndCrystal.class, new net.minecraft.world.phys.AABB(-90, 0, -90, 90, 200, 90))) {
            if (c.getPersistentData().getBooleanOr(CRYSTAL, false)) c.discard();
        }
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) return;
        if (!p.getInventory().add(s)) p.spawnAtLocation((ServerLevel) p.level(), s);
    }
}

package com.snolred.wildplus.omen;

import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.event.RandomEvents;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.omen.Omen.Curse;
import com.snolred.wildplus.omen.Omen.Theme;
import com.snolred.wildplus.registry.ModItems19;
import com.snolred.wildplus.registry.ModItems20;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.8 흉조 습격: 바닐라 습격(마을 습격)에 흉조 계약 · 흉조 양조 테마 · 연쇄 · 기여도 · 큰 보상을 얹는다.
 * - 흉조가 걸린 채 마을 종을 누르면 계약서 (저주 카드 → 보상 배율), 서명하면 바로 습격 시작
 * - 테마 흉조(양조)를 마시고 일으킨 습격은 테마 적 · 테마 효과
 * - 이기면 보상은 "정산 대기": 5분 안에 같은 마을에서 다시 습격하면 연쇄 배율 (×1.5 → ×2 → ×3), 종을 치면 바로 정산, 지면 모두 증발
 */
public final class OmenRaids {
    private OmenRaids() {}

    static final long WINDOW_TICKS = 6000, TIME_LIMIT = 18000;
    /** 흉조 세기(I~V)별 1인 기본 보상 */
    static final long[] BASE = {60_000, 100_000, 150_000, 220_000, 300_000};
    static final double[] STREAK = {1.0, 1.0, 1.5, 2.0, 3.0};

    static final class State {
        int id;
        Raid raid;
        BlockPos center;
        Theme theme = Theme.NONE;
        int curses, level = 1, streak = 1, villagers;
        long start, timeEnd;
        final Map<UUID, Float> dmg = new HashMap<>();
        final Map<UUID, Integer> present = new HashMap<>();
        final Map<UUID, String> names = new HashMap<>();
        final Map<UUID, Long> carried = new HashMap<>();
        final List<UUID> allies = new ArrayList<>();
        ServerBossEvent info, board;
        boolean ended;

        double mult() {
            return (1.0 + Omen.curseBonus(curses) + theme.bonus) * STREAK[Math.min(streak, 4)];
        }
    }

    static final class Window {
        BlockPos center;
        long until;
        int streak;
        final Map<UUID, Long> pending = new HashMap<>();
        final Map<UUID, String> names = new HashMap<>();
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final List<Window> WINDOWS = new ArrayList<>();
    private static final Map<UUID, Integer> PICK = new HashMap<>();
    private static boolean loaded;

    // ───────── 저장 ─────────
    private static CompoundTag root(MinecraftServer s) {
        return com.snolred.wildplus.content.ContentData.get(s).section("omen_raid");
    }

    private static void load(MinecraftServer s) {
        if (loaded) return;
        loaded = true;
        CompoundTag ws = root(s).getCompoundOrEmpty("windows");
        for (String k : ws.keySet()) {
            CompoundTag t = ws.getCompoundOrEmpty(k);
            Window w = new Window();
            w.center = BlockPos.of(t.getLongOr("c", 0L));
            w.until = t.getLongOr("until", 0L);
            w.streak = t.getIntOr("streak", 1);
            CompoundTag p = t.getCompoundOrEmpty("pending");
            for (String u : p.keySet()) {
                UUID id = Omen.uuidOr(u);
                if (id != null) w.pending.put(id, p.getLongOr(u, 0L));
            }
            WINDOWS.add(w);
        }
    }

    private static void save(MinecraftServer s) {
        CompoundTag r = root(s);
        CompoundTag ws = new CompoundTag();
        for (int i = 0; i < WINDOWS.size(); i++) {
            Window w = WINDOWS.get(i);
            CompoundTag t = new CompoundTag();
            t.putLong("c", w.center.asLong());
            t.putLong("until", w.until);
            t.putInt("streak", w.streak);
            CompoundTag p = new CompoundTag();
            w.pending.forEach((k, v) -> p.putLong(k.toString(), v));
            t.put("pending", p);
            ws.put(String.valueOf(i), t);
        }
        r.put("windows", ws);
        CompoundTag rs = new CompoundTag();
        for (State st : STATES.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("theme", st.theme.name());
            t.putInt("curses", st.curses);
            t.putInt("streak", st.streak);
            t.putLong("start", st.start);
            t.putLong("timeEnd", st.timeEnd);
            t.putInt("villagers", st.villagers);
            CompoundTag c = new CompoundTag();
            st.carried.forEach((k, v) -> c.putLong(k.toString(), v));
            t.put("carried", c);
            rs.put(String.valueOf(st.id), t);
        }
        r.put("raids", rs);
        com.snolred.wildplus.content.ContentData.get(s).dirty();
    }

    // ───────── 틱 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        if (now % 20 != 0) return;
        load(server);
        Raids raids = level.getRaids();
        for (ServerPlayer p : level.players()) {
            Raid r = level.getRaidAt(p.blockPosition());
            if (r == null || r.isOver() || r.isStopped()) continue;
            OptionalInt id = raids.getId(r);
            if (id.isPresent() && !STATES.containsKey(id.getAsInt())) begin(level, r, id.getAsInt());
        }
        for (State st : new ArrayList<>(STATES.values())) tickState(level, st, now);
        boolean changed = false;
        for (Window w : new ArrayList<>(WINDOWS)) {
            if (now < w.until) continue;
            settle(server, w, "연쇄 대기 시간이 끝나서");
            changed = true;
        }
        if (changed) save(server);
        if (now % 100 == 0) {
            for (Window w : WINDOWS) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, w.center.getX() + 0.5, w.center.getY() + 2, w.center.getZ() + 0.5, 6, 1.5, 1, 1.5, 0.01);
                for (ServerPlayer p : level.players()) {
                    if (p.distanceToSqr(w.center.getX(), w.center.getY(), w.center.getZ()) > 96 * 96) continue;
                    long sec = (w.until - now) / 20;
                    p.sendOverlayMessage(Component.literal(String.format("⛓ 연쇄 %d회 대기 · 내 정산 대기 %s · %d:%02d 안에 흉조를 다시 마시면 ×%.1f · 종 = 정산",
                            w.streak, Omen.won(w.pending.getOrDefault(p.getUUID(), 0L)), sec / 60, sec % 60, STREAK[Math.min(w.streak + 1, 4)]))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
    }

    /** 처음 발견한 습격: 근처 플레이어가 마신 테마 · 서명한 저주 · 연쇄를 붙인다 */
    private static void begin(ServerLevel level, Raid r, int id) {
        MinecraftServer server = level.getServer();
        long now = level.getGameTime();
        State st = new State();
        st.id = id;
        st.raid = r;
        st.center = r.getCenter();
        st.level = Math.max(1, Math.min(5, r.getRaidOmenLevel()));
        CompoundTag saved = root(server).getCompoundOrEmpty("raids").getCompoundOrEmpty(String.valueOf(id));
        if (!saved.isEmpty()) {
            st.theme = Theme.of(saved.getStringOr("theme", ""));
            st.curses = saved.getIntOr("curses", 0);
            st.streak = saved.getIntOr("streak", 1);
            st.start = saved.getLongOr("start", now);
            st.timeEnd = saved.getLongOr("timeEnd", 0L);
            st.villagers = saved.getIntOr("villagers", 0);
            CompoundTag c = saved.getCompoundOrEmpty("carried");
            for (String u : c.keySet()) {
                UUID uid = Omen.uuidOr(u);
                if (uid != null) st.carried.put(uid, c.getLongOr(u, 0L));
            }
        } else {
            st.start = now;
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(st.center.getX(), st.center.getY(), st.center.getZ()) > 112 * 112) continue;
                CompoundTag d = p.getPersistentData();
                if (st.theme == Theme.NONE && !d.getStringOr(Omen.THEME_KEY, "").isEmpty()) {
                    st.theme = Theme.of(d.getStringOr(Omen.THEME_KEY, ""));
                    d.remove(Omen.THEME_KEY);
                }
                if (st.curses == 0 && d.getIntOr(Omen.CURSE_KEY, 0) != 0 && now - d.getLongOr(Omen.CURSE_AT, 0L) < 2400) {
                    st.curses = d.getIntOr(Omen.CURSE_KEY, 0);
                    d.remove(Omen.CURSE_KEY);
                }
            }
            // 연쇄: 같은 마을의 정산 대기가 열려 있으면 이어 붙인다
            for (Window w : new ArrayList<>(WINDOWS)) {
                if (w.center.distSqr(st.center) > 64 * 64) continue;
                st.streak = w.streak + 1;
                st.carried.putAll(w.pending);
                WINDOWS.remove(w);
                break;
            }
            if (Curse.TIMED.in(st.curses)) st.timeEnd = now + TIME_LIMIT;
            st.villagers = level.getEntitiesOfClass(AbstractVillager.class, new AABB(st.center).inflate(64)).size();
            if (Curse.NIGHT.in(st.curses)) command(server, "time set midnight");
        }
        STATES.put(id, st);
        st.info = new ServerBossEvent(UUID.randomUUID(), Component.literal("흉조 습격"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        st.board = new ServerBossEvent(UUID.randomUUID(), Component.literal("기여도"), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
        save(server);
        StringBuilder sb = new StringBuilder("⚔ 흉조 습격 시작! 흉조 ").append(roman(st.level));
        if (st.theme != Theme.NONE) sb.append(" · ").append(st.theme.title);
        if (st.curses != 0) sb.append(" · 저주 ").append(Omen.curseCount(st.curses)).append("장");
        if (st.streak > 1) sb.append(" · 연쇄 ").append(st.streak).append("회째");
        sb.append(String.format(" · 보상 ×%.2f", st.mult()));
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(st.center.getX(), st.center.getY(), st.center.getZ()) > 160 * 160) continue;
            p.sendSystemMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            if (st.theme != Theme.NONE) p.sendSystemMessage(Component.literal("  " + st.theme.title + ": " + st.theme.desc()).withStyle(ChatFormatting.GRAY));
            for (Curse c : Curse.values()) {
                if (c.in(st.curses)) p.sendSystemMessage(Component.literal("  저주 · " + c.title + ": " + c.desc).withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    private static void tickState(ServerLevel level, State st, long now) {
        MinecraftServer server = level.getServer();
        Raid r = st.raid;
        if (r.isVictory()) {
            win(level, st);
            return;
        }
        if (r.isLoss() || r.isStopped()) {
            if (r.isStopped() && r.getGroupsSpawned() == 0) {
                refund(level, st);
                lose(level, st, "우민이 들어올 자리를 못 찾아 습격이 취소됐어요 (흉조를 돌려줬어요)");
                return;
            }
            lose(level, st, r.isStopped() ? "습격이 멈췄어요" : "마을이 함락됐어요");
            return;
        }
        // 3.9: 우민이 나올 자리를 바닐라가 못 찾으면(빽빽한 마을 · 좁은 시뮬레이션 거리) 습격이 멈추거나 계속 비어 있음 → 대신 찾아 줌
        if (r.getTotalRaidersAlive() == 0) ensureSpawnPos(level, r, now);
        if (st.timeEnd > 0 && now > st.timeEnd) {
            r.stop();
            lose(level, st, "⏱ 시간 제한 15분을 넘겼어요");
            return;
        }
        // 우민 손질 (테마 · 무장)
        for (Raider raider : r.getAllRaiders()) prepare(level, st, raider);
        // 참가자 · 저주
        double cx = st.center.getX() + 0.5, cy = st.center.getY(), cz = st.center.getZ() + 0.5;
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(cx, cy, cz) > 112 * 112) continue;
            near.add(p);
            st.present.merge(p.getUUID(), 1, Integer::sum);
            st.names.put(p.getUUID(), p.getName().getString());
            if (Curse.FOG.in(st.curses)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, true));
        }
        if (Curse.NIGHT.in(st.curses) && now % 1200 == 0 && level.getOverworldClockTime() % 24000L < 13000L) command(server, "time set midnight");
        // 보스바
        int alive = r.getTotalRaidersAlive();
        int waves = r.getNumGroups(level.getDifficulty());
        int villagersNow = level.getEntitiesOfClass(AbstractVillager.class, new AABB(st.center).inflate(64)).size();
        StringBuilder sb = new StringBuilder("☠ 흉조 ").append(roman(st.level));
        if (st.theme != Theme.NONE) sb.append(" · ").append(st.theme.title);
        sb.append(String.format(" · 웨이브 %d/%d · 남은 우민 %d · 주민 %d/%d · ×%.2f", Math.min(r.getGroupsSpawned(), waves), waves, alive,
                villagersNow, st.villagers, st.mult()));
        if (st.streak > 1) sb.append(" · 연쇄 ").append(st.streak);
        if (st.timeEnd > 0) {
            long sec = Math.max(0, (st.timeEnd - now) / 20);
            sb.append(String.format(" · ⏱ %d:%02d", sec / 60, sec % 60));
            st.info.setProgress((float) Math.max(0, Math.min(1, (st.timeEnd - now) / (double) TIME_LIMIT)));
        } else {
            st.info.setProgress(1F);
        }
        st.info.setName(Component.literal(sb.toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        float total = 0;
        for (float v : st.dmg.values()) total += v;
        List<Map.Entry<UUID, Float>> top = new ArrayList<>(st.dmg.entrySet());
        top.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        StringBuilder bd = new StringBuilder("기여도  ");
        for (int i = 0; i < Math.min(4, top.size()); i++) {
            if (i > 0) bd.append("  ·  ");
            bd.append(i + 1).append(". ").append(st.names.getOrDefault(top.get(i).getKey(), "?"))
                    .append(String.format(" %d%%", Math.round(top.get(i).getValue() * 100 / Math.max(1F, total))));
        }
        if (top.isEmpty()) bd.append("우민을 처치해 보상 몫을 늘리세요");
        st.board.setName(Component.literal(bd.toString()).withStyle(ChatFormatting.YELLOW));
        st.board.setProgress(top.isEmpty() ? 0F : Math.min(1F, top.get(0).getValue() / Math.max(1F, total)));
        sync(st.info, near);
        sync(st.board, near);
    }

    private static void sync(ServerBossEvent bar, List<ServerPlayer> near) {
        for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) if (!near.contains(p)) bar.removePlayer(p);
        for (ServerPlayer p : near) if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
    }

    /** 우민 한 명 손질: 저주 "무장" · 테마 버프 · 테마 동료 소환 */
    private static void prepare(ServerLevel level, State st, Raider raider) {
        CompoundTag d = raider.getPersistentData();
        if (d.getBooleanOr(Omen.MOB_DONE, false)) return;
        d.putBoolean(Omen.MOB_DONE, true);
        d.putInt("wildplus_omen_raid", st.id);
        int inf = -1;
        if (Curse.ARMORED.in(st.curses) && st.theme != Theme.GOLDEN) {
            raider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            raider.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            raider.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            raider.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) raider.setDropChance(s, 0F);
        }
        if (st.theme == Theme.NONE) return;
        d.putString(Omen.MOB_THEME, st.theme.name());
        var rnd = level.getRandom();
        EntityType<? extends Mob> allyType = null;
        switch (st.theme) {
            case BURNING -> {
                raider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, inf, 0, false, false));
                allyType = EntityTypes.BLAZE;
            }
            case FROZEN -> {
                raider.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, inf, 0, false, false));
                allyType = EntityTypes.STRAY;
            }
            case DEEP -> {
                raider.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, inf, 0, false, false));
                raider.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, inf, 0, false, false));
                allyType = EntityTypes.DROWNED;
            }
            case WRAITH -> {
                raider.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, inf, 1, false, false));
                allyType = EntityTypes.VEX;
            }
            case GOLDEN -> {
                raider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                raider.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                raider.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
                raider.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
                for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) raider.setDropChance(s, 0F);
                var hp = raider.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    hp.setBaseValue(hp.getBaseValue() * 1.5);
                    raider.setHealth(raider.getMaxHealth());
                }
            }
            default -> {}
        }
        level.sendParticles(st.theme == Theme.FROZEN ? ParticleTypes.SNOWFLAKE : st.theme == Theme.DEEP ? ParticleTypes.BUBBLE
                        : st.theme == Theme.WRAITH ? ParticleTypes.SOUL : st.theme == Theme.GOLDEN ? ParticleTypes.WAX_ON : ParticleTypes.FLAME,
                raider.getX(), raider.getY() + 1, raider.getZ(), 15, 0.4, 0.6, 0.4, 0.02);
        if (allyType != null && rnd.nextFloat() < 0.3F) {
            Mob ally = allyType.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (ally != null) {
                ally.snapTo(raider.getX() + rnd.nextInt(5) - 2, raider.getY(), raider.getZ() + rnd.nextInt(5) - 2, rnd.nextFloat() * 360F, 0F);
                ally.finalizeSpawn(level, level.getCurrentDifficultyAt(raider.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
                if (st.theme == Theme.DEEP) ally.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
                ally.getPersistentData().putBoolean(Omen.MOB_DONE, true);
                ally.getPersistentData().putInt(Omen.ALLY, st.id);
                ally.getPersistentData().putString(Omen.MOB_THEME, st.theme.name());
                ally.setPersistenceRequired();
                level.addFreshEntity(ally);
                st.allies.add(ally.getUUID());
            }
        }
    }

    // ───────── 승패 ─────────
    private static void win(ServerLevel level, State st) {
        MinecraftServer server = level.getServer();
        end(level, st);
        // 참가자: 피해를 줬거나 30초 이상 마을에 있던 플레이어
        Map<UUID, Float> share = new LinkedHashMap<>();
        for (var e : st.present.entrySet()) if (e.getValue() >= 30) share.put(e.getKey(), 0F);
        for (var e : st.dmg.entrySet()) share.put(e.getKey(), e.getValue());
        if (share.isEmpty()) return;
        float total = 0;
        for (float v : share.values()) total += v;
        int n = share.size();
        int villagersNow = level.getEntitiesOfClass(AbstractVillager.class, new AABB(st.center).inflate(64)).size();
        boolean allSafe = st.villagers > 0 && villagersNow >= st.villagers;
        double mult = st.mult() * st.theme.money * (allSafe ? 1.1 : 1.0)
                * (RandomEvents.isActive(RandomEvents.Type.OMEN_STORM) ? 1.5 : 1.0);
        long base = BASE[st.level - 1];
        Window w = new Window();
        w.center = st.center;
        w.until = level.getGameTime() + WINDOW_TICKS;
        w.streak = st.streak;
        w.pending.putAll(st.carried);
        w.names.putAll(st.names);
        for (var e : share.entrySet()) {
            double s = total > 0 ? e.getValue() / total : 1.0 / n;
            double factor = Math.max(0.6, Math.min(1.8, 0.6 + 0.4 * s * n));
            long reward = Math.round(base * mult * factor / 100.0) * 100;
            w.pending.merge(e.getKey(), reward, Long::sum);
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) continue;
            // 물건 보상은 바로
            var rnd = level.getRandom();
            Omen.give(p, new ItemStack(Items.EMERALD, 4 * st.level));
            if (rnd.nextDouble() < Math.min(0.9, 0.05 * st.level * st.mult())) Omen.give(p, new ItemStack(ModItems19.LIFE_ESSENCE.get()));
            if (rnd.nextDouble() < 0.25 + 0.1 * st.level) Omen.give(p, Omen.withAmp(new ItemStack(Items.OMINOUS_BOTTLE), Math.min(4, st.level - 1 + rnd.nextInt(2))));
            if (st.level >= 4 && st.mult() >= 2) Omen.give(p, new ItemStack(ModItems20.OMEN_FLASK.get(), 2));
            p.sendSystemMessage(Component.literal(String.format("🏆 흉조 습격 승리! 내 몫 %s (기여 %d%% · 배율 ×%.2f%s) — 정산 대기 중 · 누적 %s",
                    Omen.won(reward), Math.round(s * 100), mult, allSafe ? " · 주민 전원 생존 +10%" : "",
                    Omen.won(w.pending.getOrDefault(e.getKey(), 0L)))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            p.sendSystemMessage(Component.literal(String.format("  5분 안에 이 마을에서 다시 흉조 습격을 일으키면 연쇄 %d회째 ×%.1f! 마을 종을 치면 바로 정산 · 지면 모두 사라져요",
                    st.streak + 1, STREAK[Math.min(st.streak + 1, 4)])).withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        WINDOWS.add(w);
        save(server);
    }

    private static java.lang.reflect.Field waveField;

    /** 다음 웨이브가 나올 자리: 마을 중심에서 16~40칸, 지면 위 두 칸이 비고 몹이 움직이는(엔티티 틱) 곳 */
    @SuppressWarnings("unchecked")
    private static void ensureSpawnPos(ServerLevel level, Raid r, long now) {
        try {
            if (waveField == null) {
                waveField = Raid.class.getDeclaredField("waveSpawnPos");
                waveField.setAccessible(true);
            }
            java.util.Optional<BlockPos> cur = (java.util.Optional<BlockPos>) waveField.get(r);
            if (cur != null && cur.isPresent() && level.isPositionEntityTicking(cur.get())) return;
            BlockPos c = r.getCenter();
            var rnd = level.getRandom();
            for (int i = 0; i < 48; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2, d = 16 + rnd.nextDouble() * 24;
                int x = c.getX() + (int) Math.round(Math.cos(ang) * d), z = c.getZ() + (int) Math.round(Math.sin(ang) * d);
                if (!level.hasChunksAt(x - 10, z - 10, x + 10, z + 10)) continue;
                BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (Math.abs(at.getY() - c.getY()) > 40 || !level.isPositionEntityTicking(at)) continue;
                if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) continue;
                if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
                        || !level.getFluidState(at).isEmpty()) {
                    continue;
                }
                waveField.set(r, java.util.Optional.of(at));
                if (now % 200 == 0) com.snolred.wildplus.WildPlus.LOGGER.info("Omen raid {}: wave spawn pos set to {} (groups {})", r.getCenter(), at, r.getGroupsSpawned());
                return;
            }
            if (now % 200 == 0) com.snolred.wildplus.WildPlus.LOGGER.warn("Omen raid {}: no spawn pos found (groups {})", r.getCenter(), r.getGroupsSpawned());
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (now % 1200 == 0) com.snolred.wildplus.WildPlus.LOGGER.warn("Omen raid spawn helper failed: {}", e.toString());
        }
    }

    /** 습격이 한 번도 못 나오고 멈췄으면 근처 사람에게 흉조를 돌려줌 */
    private static void refund(ServerLevel level, State st) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(st.center.getX(), st.center.getY(), st.center.getZ()) > 112 * 112) continue;
            p.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 120000, Math.max(0, st.level - 1)));
            if (st.theme != Theme.NONE) p.getPersistentData().putString(Omen.THEME_KEY, st.theme.name());
            break;
        }
    }

    private static void lose(ServerLevel level, State st, String why) {
        MinecraftServer server = level.getServer();
        end(level, st);
        long lost = 0;
        for (long v : st.carried.values()) lost += v;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(st.center.getX(), st.center.getY(), st.center.getZ()) > 160 * 160 && !st.carried.containsKey(p.getUUID())) continue;
            p.sendSystemMessage(Component.literal("💀 흉조 습격 실패 — " + why + (lost > 0 ? " · 연쇄 정산 대기 " + Omen.won(lost) + " 증발" : ""))
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        save(server);
    }

    private static void end(ServerLevel level, State st) {
        st.ended = true;
        STATES.remove(st.id);
        if (st.info != null) st.info.removeAllPlayers();
        if (st.board != null) st.board.removeAllPlayers();
        for (UUID u : st.allies) {
            Entity e = level.getEntity(u);
            if (e != null) {
                level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                e.discard();
            }
        }
    }

    /** 정산: 정산 대기 금액을 통장으로 */
    private static void settle(MinecraftServer server, Window w, String why) {
        WINDOWS.remove(w);
        WildData wd = WildData.get(server);
        for (var e : w.pending.entrySet()) {
            if (e.getValue() <= 0) continue;
            wd.addBalance(e.getKey(), e.getValue());
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                p.sendSystemMessage(Component.literal(String.format("💰 흉조 습격 정산 (%s, 연쇄 %d회): %s 통장 입금", why, w.streak, Omen.won(e.getValue())))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
        }
        wd.setDirty();
    }

    // ───────── 종: 계약서 · 정산 ─────────
    @SubscribeEvent
    public static void onBell(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof BellBlock)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (Omen.omenLevel(p) >= 0) event.setCanceled(true);
            return;
        }
        load(level.getServer());
        BlockPos pos = event.getPos();
        if (Omen.omenLevel(p) >= 0 && level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            if (!level.isVillage(pos)) {
                p.sendOverlayMessage(Component.literal("마을 안의 종에서만 흉조 계약을 할 수 있어요").withStyle(ChatFormatting.RED));
                return;
            }
            if (level.getRaidAt(pos) != null && !level.getRaidAt(pos).isOver()) {
                p.sendOverlayMessage(Component.literal("이미 습격이 진행 중이에요").withStyle(ChatFormatting.RED));
                return;
            }
            openContract(p, pos);
            return;
        }
        for (Window w : new ArrayList<>(WINDOWS)) {
            if (w.center.distSqr(pos) > 96 * 96) continue;
            settle(level.getServer(), w, "종을 쳐서");
            save(level.getServer());
            return;
        }
    }

    /** 테스트용 (/wildplus uitest contract): 저주 두 장을 붙인 계약서 */
    public static void debugContract(ServerPlayer p) {
        PICK.put(p.getUUID(), 0b000101);
        openContract(p, p.blockPosition());
    }

    static void openContract(ServerPlayer p, BlockPos bell) {
        int mask = PICK.getOrDefault(p.getUUID(), 0);
        // 계약서를 읽는 동안 습격 징조 카운트다운을 1분으로 늘려 줌
        MobEffectInstance ro = p.getEffect(MobEffects.RAID_OMEN);
        if (ro != null && ro.getDuration() < 1200) {
            p.removeEffect(MobEffects.RAID_OMEN);
            p.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 1200, ro.getAmplifier()));
        }
        int amp = Math.max(0, Omen.omenLevel(p));
        Theme theme = Theme.of(p.getPersistentData().getStringOr(Omen.THEME_KEY, ""));
        List<PickMenu.Entry> es = new ArrayList<>();
        for (Curse c : Curse.values()) {
            boolean on = c.in(mask);
            es.add(new PickMenu.Entry((on ? "☑ " : "☐ ") + c.title + String.format("  +%d%%", Math.round(c.bonus * 100)), c.desc, new ItemStack(c.icon),
                    PickMenu.K_ROW));
        }
        double mult = 1 + Omen.curseBonus(mask) + theme.bonus;
        es.add(PickMenu.Entry.of(PickMenu.K_INFO, String.format("보상 배율 ×%.2f", mult * theme.money),
                "흉조 " + roman(amp + 1) + (theme != Theme.NONE ? " · " + theme.title : "") + " · 1인 기본 " + Omen.won(BASE[Math.min(4, amp)])));
        es.add(PickMenu.Entry.of(PickMenu.K_PRIMARY, "✒ 서명하고 종 치기", "바로 습격이 시작돼요"));
        PickMenu.open(p, Component.literal("📜 흉조 계약서"), "붙일 저주를 고르세요 · 저주가 많을수록 보상이 커져요", "", es, PickMenu.themed(PickMenu.T_CONTRACT), i -> {
            if (i >= 0 && i < Curse.values().length) {
                PICK.put(p.getUUID(), mask ^ (1 << i));
                openContract(p, bell);
            } else if (i == Curse.values().length + 1) {
                sign(p, bell, mask);
            }
        }, null, q -> q.distanceToSqr(bell.getX() + 0.5, bell.getY() + 0.5, bell.getZ() + 0.5) < 64);
    }

    private static void sign(ServerPlayer p, BlockPos bell, int mask) {
        ServerLevel level = p.level();
        int amp = Omen.omenLevel(p);
        if (amp < 0) {
            p.closeContainer();
            p.sendOverlayMessage(Component.literal("흉조가 사라졌어요").withStyle(ChatFormatting.RED));
            return;
        }
        p.closeContainer();
        PICK.remove(p.getUUID());
        p.getPersistentData().putInt(Omen.CURSE_KEY, mask);
        p.getPersistentData().putLong(Omen.CURSE_AT, level.getGameTime());
        p.removeEffect(MobEffects.BAD_OMEN);
        p.removeEffect(MobEffects.RAID_OMEN);
        p.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, amp));
        Raid r = level.getRaids().createOrExtendRaid(p, bell);
        p.removeEffect(MobEffects.RAID_OMEN);
        if (r == null) {
            p.sendOverlayMessage(Component.literal("여기서는 습격을 일으킬 수 없어요").withStyle(ChatFormatting.RED));
            return;
        }
        if (r.getRaidOmenLevel() <= 0) r.setRaidOmenLevel(Math.min(r.getMaxRaidOmenLevel(), amp + 1));
        level.playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.7F);
        level.playSound(null, bell, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4.0F, 1.0F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, bell.getX() + 0.5, bell.getY() + 1, bell.getZ() + 0.5, 40, 0.6, 0.6, 0.6, 0.05);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("📜 " + p.getName().getString() + " 님이 흉조 계약서에 서명했어요! (저주 "
                + Omen.curseCount(mask) + "장)").withStyle(ChatFormatting.DARK_PURPLE), false);
        OptionalInt id = level.getRaids().getId(r);
        if (id.isPresent() && !STATES.containsKey(id.getAsInt())) begin(level, r, id.getAsInt());
    }

    // ───────── 전투 규칙 ─────────
    private static State stateOf(LivingEntity e) {
        if (e instanceof Raider r && r.getCurrentRaid() != null && e.level() instanceof ServerLevel sl) {
            OptionalInt id = sl.getRaids().getId(r.getCurrentRaid());
            if (id.isPresent()) return STATES.get(id.getAsInt());
        }
        int ally = e.getPersistentData().getIntOr(Omen.ALLY, -1);
        if (ally < 0) ally = e.getPersistentData().getIntOr("wildplus_omen_raid", -1);
        return ally >= 0 ? STATES.get(ally) : null;
    }

    /** 기여도 (용병이 준 피해는 주인 몫) */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        Entity src = event.getSource().getEntity();
        // 테마 공격 효과
        if (src instanceof LivingEntity att && !att.getPersistentData().getStringOr(Omen.MOB_THEME, "").isEmpty()
                && (victim instanceof Player || victim instanceof AbstractVillager || victim instanceof IronGolem)) {
            switch (Theme.of(att.getPersistentData().getStringOr(Omen.MOB_THEME, ""))) {
                case BURNING -> victim.igniteForSeconds(4F);
                case FROZEN -> victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                case DEEP -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                case WRAITH -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                default -> {}
            }
        }
        if (STATES.isEmpty()) return;
        State st = stateOf(victim);
        if (st == null) return;
        UUID who = null;
        String name = null;
        if (src instanceof ServerPlayer p) {
            who = p.getUUID();
            name = p.getName().getString();
        } else if (src instanceof com.snolred.wildplus.entity.MercenaryEntity m && m.ownerId() != null) {
            who = m.ownerId();
        }
        if (who == null) return;
        st.dmg.merge(who, event.getHealthDamage(), Float::sum);
        if (name != null) st.names.put(who, name);
    }

    /** 저주 "겁먹은 주민": 주민이 받는 피해 2배 */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (STATES.isEmpty() || !(event.getEntity() instanceof AbstractVillager v)) return;
        for (State st : STATES.values()) {
            if (Curse.PANIC.in(st.curses) && v.distanceToSqr(st.center.getX(), st.center.getY(), st.center.getZ()) < 96 * 96) {
                event.setAmount(event.getAmount() * 2F);
                return;
            }
        }
    }

    /** 저주 "피의 맹세": 회복 절반 */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (STATES.isEmpty() || !(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
        for (State st : STATES.values()) {
            if (Curse.BLOOD.in(st.curses) && p.distanceToSqr(st.center.getX(), st.center.getY(), st.center.getZ()) < 112 * 112) {
                event.setAmount(event.getAmount() * 0.5F);
                return;
            }
        }
    }

    /** 불타는 흉조의 쇠뇌 화살은 불화살 */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow a) || event.getLevel().isClientSide()) return;
        if (a.getOwner() instanceof LivingEntity o && Theme.of(o.getPersistentData().getStringOr(Omen.MOB_THEME, "")) == Theme.BURNING) a.igniteForSeconds(100);
    }

    /** 황금 흉조: 우민 처치마다 금화 */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e instanceof Raider) || Theme.of(e.getPersistentData().getStringOr(Omen.MOB_THEME, "")) != Theme.GOLDEN) return;
        int n = 2 + e.getRandom().nextInt(4);
        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.level(), e.getX(), e.getY() + 0.5, e.getZ(),
                new ItemStack(com.snolred.wildplus.registry.ModItems3.GOLD_COIN.get(), n)));
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        for (State st : STATES.values()) {
            if (st.info != null) st.info.removeAllPlayers();
            if (st.board != null) st.board.removeAllPlayers();
        }
        STATES.clear();
        WINDOWS.clear();
        PICK.clear();
        loaded = false;
    }

    // ───────── 도우미 ─────────
    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> n >= 5 ? "V" : "?";
        };
    }

    static void command(MinecraftServer server, String cmd) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
    }

    /** 근처에서 진행 중인 흉조 습격이 있는지 (제단 보호막 등) */
    public static boolean raidNear(BlockPos pos, int r) {
        for (State st : STATES.values()) if (st.center.distSqr(pos) < (double) r * r) return true;
        return false;
    }
}

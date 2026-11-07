package com.snolred.wildplus.content;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 투기장: 팀 인원(1~4명)을 정해 A팀 vs B팀으로 싸운다.
 * - 장비: ⚔ 지급(모두 같은 장비, 내 가방은 맡겨 두고 끝나면 돌려받음) / 🛡 본인 장비(아이템 안 잃음)
 * - 쓰러지면 죽지 않고 관전석으로. 한 팀이 모두 쓰러지면 경기 끝 (5분 넘으면 무승부)
 * - 승점(등급)과 투기장 메달(전용 재화) · 메달 상점
 * - 맵: 테마 5개 (기믹 없이 엄폐 기둥만)
 */
public final class Arena {
    private Arena() {}

    public enum Phase { NONE, LOBBY, COUNTDOWN, FIGHT, END }

    public record Theme(String name, Block floor, Block accent, Block wall, Block pillar, Block light, Block stand) {}

    public static final Theme[] THEMES = {
            new Theme("콜로세움", Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.GLOWSTONE, Blocks.SMOOTH_SANDSTONE),
            new Theme("얼음 신전", Blocks.POLISHED_DIORITE, Blocks.CALCITE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SEA_LANTERN, Blocks.SNOW_BLOCK),
            new Theme("지옥 투기장", Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRIMSON_HYPHAE, Blocks.SHROOMLIGHT, Blocks.BLACKSTONE),
            new Theme("숲속 결투장", Blocks.MOSS_BLOCK, Blocks.ROOTED_DIRT, Blocks.STRIPPED_SPRUCE_LOG, Blocks.OAK_LOG, Blocks.OCHRE_FROGLIGHT, Blocks.SPRUCE_PLANKS),
            new Theme("끝의 섬", Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.OBSIDIAN, Blocks.PEARLESCENT_FROGLIGHT, Blocks.END_STONE),
    };

    public static final BlockPos CENTER = new BlockPos(-1_000_000, 150, 1_000_000);
    public static final int R = 16, H = 12, TIME_LIMIT = 20 * 60 * 5;
    public static final String[] TIER = {"브론즈", "실버", "골드", "플래티넘", "다이아", "챔피언"};
    private static final int[] TIER_AT = {0, 100, 250, 500, 800, 1200};

    // 경기 상태
    private static Phase phase = Phase.NONE;
    private static UUID host;
    private static int teamSize = 1, theme = 0;
    private static boolean kit = true;
    private static final Set<UUID> TEAM_A = new LinkedHashSet<>(), TEAM_B = new LinkedHashSet<>(), DOWN = new LinkedHashSet<>();
    private static final Map<UUID, String> NAMES = new HashMap<>();
    private static final Map<UUID, Float> DMG = new LinkedHashMap<>();
    private static long timer, startedAt;
    private static ServerBossEvent bar;
    // 경기 만들기 전 각자 고른 설정 [인원, 장비(1=지급), 맵(-1 무작위)]
    private static final Map<UUID, int[]> DRAFT = new HashMap<>();

    // ───────── 조회 ─────────
    public static boolean inMatch(UUID id) {
        return (phase == Phase.COUNTDOWN || phase == Phase.FIGHT || phase == Phase.END) && (TEAM_A.contains(id) || TEAM_B.contains(id));
    }

    /** 지급 장비(공평) 모드 경기 중인가 — 명예 등급 효과를 모두 끈다 */
    public static boolean fairMatch(UUID id) {
        return kit && inMatch(id);
    }

    /** 투기장 안에서 서로 다른 팀으로 싸우는 중인가 (파티원끼리라도 피해가 들어감) */
    public static boolean opponents(UUID a, UUID b) {
        return phase == Phase.FIGHT && ((TEAM_A.contains(a) && TEAM_B.contains(b)) || (TEAM_B.contains(a) && TEAM_A.contains(b)));
    }

    private static CompoundTag stats(MinecraftServer s, UUID id) {
        return ContentData.get(s).player("arena", id);
    }

    public static int tier(int points) {
        int t = 0;
        for (int i = 0; i < TIER_AT.length; i++) if (points >= TIER_AT[i]) t = i;
        return t;
    }

    public static long medals(MinecraftServer s, UUID id) {
        return stats(s, id).getLongOr("medals", 0L);
    }

    public static void addMedals(MinecraftServer s, UUID id, long d) {
        CompoundTag st = stats(s, id);
        st.putLong("medals", Math.max(0, st.getLongOr("medals", 0L) + d));
        ContentData.get(s).dirty();
    }

    private static String name(MinecraftServer s, UUID id) {
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        return p != null ? p.getGameProfile().name() : NAMES.getOrDefault(id, "?");
    }

    private static String teamText(MinecraftServer s, Set<UUID> team) {
        if (team.isEmpty()) return "(비어 있음)";
        List<String> n = new ArrayList<>();
        for (UUID id : team) n.add(name(s, id));
        return String.join(", ", n);
    }

    private static void broadcast(MinecraftServer s, Component c) {
        s.getPlayerList().broadcastSystemMessage(c, false);
    }

    private static List<ServerPlayer> online(MinecraftServer s, Set<UUID> ids) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : ids) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    // ───────── 메뉴 ─────────
    public static void open(ServerPlayer p, BlockPos pos) {
        open(p);
    }

    public static void open(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        UUID me = p.getUUID();
        List<PickMenu.Entry> e = new ArrayList<>();
        List<Runnable> act = new ArrayList<>();   // 누르기 (설정은 ▶)
        List<Runnable> dec = new ArrayList<>();   // 설정 ◀
        Set<Integer> leaves = new java.util.HashSet<>(); // 다른 화면으로 가는 버튼 (다시 안 엶)
        int[] startIdx = {-1}; // 경기 시작: 성공하면 닫고, 못 하면 다시 엶
        CompoundTag st = stats(s, me);
        int pts = st.getIntOr("points", 0);
        Runnable none = () -> {};
        String header;
        if (phase == Phase.NONE) {
            int[] d = DRAFT.computeIfAbsent(me, k -> new int[]{1, 1, -1});
            header = "경기 만들기 · ◀ ▶ 로 설정을 고르고 모집 시작";
            e.add(PickMenu.Entry.of(PickMenu.K_OPTION, "👥 팀 인원", d[0] + " 대 " + d[0]));
            act.add(() -> d[0] = d[0] % 4 + 1);
            dec.add(() -> d[0] = (d[0] + 2) % 4 + 1);
            e.add(PickMenu.Entry.of(PickMenu.K_OPTION, "⚔ 장비", d[1] == 1 ? "지급 장비 (공평)" : "본인 장비"));
            act.add(() -> d[1] = 1 - d[1]);
            dec.add(() -> d[1] = 1 - d[1]);
            e.add(PickMenu.Entry.of(PickMenu.K_OPTION, "🗺 맵", d[2] < 0 ? "무작위" : THEMES[d[2]].name()));
            act.add(() -> d[2] = d[2] + 1 >= THEMES.length ? -1 : d[2] + 1);
            dec.add(() -> d[2] = d[2] <= -1 ? THEMES.length - 1 : d[2] - 1);
            e.add(PickMenu.Entry.of(PickMenu.K_PRIMARY, "⚔ 모집 시작", "나는 🔴 A팀"));
            act.add(() -> createLobby(p, d));
            dec.add(none);
        } else if (phase == Phase.LOBBY) {
            header = String.format("모집 중 · %d 대 %d · %s · %s", teamSize, teamSize, kit ? "지급 장비" : "본인 장비",
                    theme < 0 ? "무작위 맵" : THEMES[theme].name());
            boolean inA = TEAM_A.contains(me), inB = TEAM_B.contains(me);
            e.add(PickMenu.Entry.of(PickMenu.K_TEAM_A, String.format("A팀 %d/%d", TEAM_A.size(), teamSize),
                    (inA ? "*" : "") + teamText(s, TEAM_A)));
            act.add(() -> join(p, true));
            dec.add(none);
            e.add(PickMenu.Entry.of(PickMenu.K_TEAM_B, String.format("B팀 %d/%d", TEAM_B.size(), teamSize),
                    (inB ? "*" : "") + teamText(s, TEAM_B)));
            act.add(() -> join(p, false));
            dec.add(none);
            if (me.equals(host)) {
                startIdx[0] = e.size();
                e.add(PickMenu.Entry.of(PickMenu.K_PRIMARY, "▶ 경기 시작", "양 팀 인원이 같아야 해요"));
                act.add(() -> start(p));
                dec.add(none);
            }
            if (inA || inB) {
                e.add(PickMenu.Entry.of(PickMenu.K_SMALL, "🚪 나가기", ""));
                act.add(() -> leaveLobby(p));
                dec.add(none);
            }
            if (me.equals(host)) {
                e.add(PickMenu.Entry.of(PickMenu.K_DANGER, "✖ 모집 취소", ""));
                act.add(() -> {
                    reset(s);
                    broadcast(s, Component.literal("⚔ 투기장 모집이 취소됐어요").withStyle(ChatFormatting.GRAY));
                });
                dec.add(none);
            }
        } else {
            header = String.format("경기 중 · 🔴 %d명 vs 🔵 %d명", alive(TEAM_A), alive(TEAM_B));
            leaves.add(e.size());
            e.add(PickMenu.Entry.of(PickMenu.K_PRIMARY, "👀 관전하러 가기", "관전석으로 이동"));
            act.add(() -> {
                if (!inMatch(me)) {
                    saveReturn(p);
                    toStand(p);
                }
            });
            dec.add(none);
        }
        e.add(PickMenu.Entry.of(PickMenu.K_INFO, String.format("🏅 승점 %d · %s", pts, TIER[tier(pts)]),
                String.format("메달 %,d개 · %d승 %d패", st.getLongOr("medals", 0L), st.getIntOr("wins", 0), st.getIntOr("losses", 0))));
        act.add(none);
        dec.add(none);
        leaves.add(e.size());
        e.add(PickMenu.Entry.of(PickMenu.K_SMALL, "🛒 메달 상점", ""));
        act.add(() -> shop(p));
        dec.add(none);
        leaves.add(e.size());
        e.add(PickMenu.Entry.of(PickMenu.K_SMALL, "🏆 랭킹", ""));
        act.add(() -> ranking(p));
        dec.add(none);
        PickMenu.open(p, Component.literal("⚔ 투기장"), header, "", e, PickMenu.FLAG_ARENA, i -> {
            boolean down = i >= PickMenu.DEC;
            int k = down ? i - PickMenu.DEC : i;
            if (k < 0 || k >= act.size()) return;
            Phase before = phase;
            (down ? dec : act).get(k).run();
            if (leaves.contains(k)) return;
            if (k == startIdx[0] && phase != before) {
                p.closeContainer();
                return;
            }
            open(p);
        }, null, pl -> true);
    }

    private static int alive(Set<UUID> team) {
        int n = 0;
        for (UUID id : team) if (!DOWN.contains(id)) n++;
        return n;
    }

    private static void ranking(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag all = ContentData.get(s).section("arena");
        List<String[]> rows = new ArrayList<>();
        for (String k : all.keySet()) {
            CompoundTag c = all.getCompoundOrEmpty(k);
            rows.add(new String[]{c.getStringOr("name", "?"), String.valueOf(c.getIntOr("points", 0)),
                    c.getIntOr("wins", 0) + "승 " + c.getIntOr("losses", 0) + "패"});
        }
        rows.sort((a, b) -> Integer.parseInt(b[1]) - Integer.parseInt(a[1]));
        List<PickMenu.Entry> e = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            int pt = Integer.parseInt(r[1]);
            e.add(new PickMenu.Entry((i == 0 ? "👑 " : "") + (i + 1) + "위 " + r[0], pt + "점 · " + TIER[tier(pt)] + " · " + r[2]));
        }
        PickMenu.open(p, Component.literal("🏆 투기장 랭킹"), "누르면 돌아가기", "아직 경기 기록이 없어요", e, PickMenu.FLAG_ARENA, i -> open(p), null, pl -> true);
    }

    // ───────── 메달 상점 ─────────
    private record Good(String name, long price, java.util.function.Consumer<ServerPlayer> give) {}

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s) && !s.isEmpty()) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
    }

    private static List<Good> goods() {
        List<Good> g = new ArrayList<>();
        g.add(new Good("황금 사과 ×4", 10, p -> give(p, new ItemStack(Items.GOLDEN_APPLE, 4))));
        g.add(new Good("경험치 병 ×16", 15, p -> give(p, new ItemStack(Items.EXPERIENCE_BOTTLE, 16))));
        g.add(new Good("휴대용 수리 키트", 30, p -> give(p, new ItemStack(com.snolred.wildplus.registry.ModItems11.REPAIR_KIT.get()))));
        g.add(new Good("불사의 토템", 40, p -> give(p, new ItemStack(Items.TOTEM_OF_UNDYING))));
        g.add(new Good("마법이 부여된 황금 사과", 60, p -> give(p, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE))));
        g.add(new Good("네더라이트 주괴", 80, p -> give(p, new ItemStack(Items.NETHERITE_INGOT))));
        g.add(new Good("스텔라 코어 (스텔라 루비 승급)", 250, p -> give(p, new ItemStack(com.snolred.wildplus.registry.ModItems13.STELLAR_CORE.get()))));
        g.add(new Good("용의 깃털 (스카이 사파이어 승급)", 200, p -> give(p, new ItemStack(com.snolred.wildplus.registry.ModItems11.DRAGON_PLUME.get()))));
        g.add(new Good("생명의 결정 (로열 에메랄드 승급)", 250, p -> give(p, new ItemStack(com.snolred.wildplus.registry.ModItems12.LIFE_CRYSTAL.get()))));
        g.add(new Good("👑 칭호 [검투사]", 300, p -> com.snolred.wildplus.economy.Luxury.grantTitle(p, 3)));
        g.add(new Good("👑 칭호 [챔피언] (챔피언 등급 이상)", 1000, p -> com.snolred.wildplus.economy.Luxury.grantTitle(p, 4)));
        return g;
    }

    private static void shop(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        List<Good> list = goods();
        List<PickMenu.Entry> e = new ArrayList<>();
        for (Good g : list) e.add(new PickMenu.Entry(g.name(), String.format("메달 %,d", g.price())));
        PickMenu.open(p, Component.literal("🛒 메달 상점"), String.format("내 메달 %,d개", medals(s, p.getUUID())), "", e, PickMenu.FLAG_ARENA, i -> {
            Good g = list.get(i);
            if (g.name().contains("챔피언") && tier(stats(s, p.getUUID()).getIntOr("points", 0)) < 5) {
                p.sendOverlayMessage(Component.literal("챔피언 등급(승점 1,200) 이상만 살 수 있어요").withStyle(ChatFormatting.RED));
                return;
            }
            if (medals(s, p.getUUID()) < g.price()) {
                p.sendOverlayMessage(Component.literal("메달이 모자라요").withStyle(ChatFormatting.RED));
                return;
            }
            addMedals(s, p.getUUID(), -g.price());
            g.give().accept(p);
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
            p.sendSystemMessage(Component.literal("🛒 " + g.name() + " 구매 (메달 " + g.price() + ")").withStyle(ChatFormatting.GOLD));
            shop(p);
        }, null, pl -> true);
    }

    // ───────── 모집 ─────────
    private static void createLobby(ServerPlayer p, int[] d) {
        if (phase != Phase.NONE) return;
        MinecraftServer s = p.level().getServer();
        if (busyElsewhere(p)) return;
        teamSize = d[0];
        kit = d[1] == 1;
        theme = d[2];
        host = p.getUUID();
        TEAM_A.clear();
        TEAM_B.clear();
        DOWN.clear();
        TEAM_A.add(p.getUUID());
        NAMES.put(p.getUUID(), p.getGameProfile().name());
        phase = Phase.LOBBY;
        broadcast(s, Component.literal(String.format("⚔ %s 님이 투기장 %d 대 %d 경기(%s)를 모집해요! 투기장 접수처나 /arena 로 참가",
                p.getGameProfile().name(), teamSize, teamSize, kit ? "지급 장비" : "본인 장비")).withStyle(ChatFormatting.GOLD));
    }

    private static boolean busyElsewhere(ServerPlayer p) {
        if (Raid.active() || Tower.isIn(p.getUUID())) {
            p.sendSystemMessage(Component.literal("레이드 · 하늘의 탑 도중에는 투기장에 들어갈 수 없어요").withStyle(ChatFormatting.RED));
            return true;
        }
        return false;
    }

    private static void join(ServerPlayer p, boolean a) {
        if (phase != Phase.LOBBY || busyElsewhere(p)) return;
        Set<UUID> to = a ? TEAM_A : TEAM_B;
        if (to.contains(p.getUUID())) return;
        if (to.size() >= teamSize) {
            p.sendOverlayMessage(Component.literal("그 팀은 꽉 찼어요").withStyle(ChatFormatting.RED));
            return;
        }
        TEAM_A.remove(p.getUUID());
        TEAM_B.remove(p.getUUID());
        to.add(p.getUUID());
        NAMES.put(p.getUUID(), p.getGameProfile().name());
        broadcast(p.level().getServer(), Component.literal("⚔ " + p.getGameProfile().name() + " 님이 " + (a ? "🔴 A팀" : "🔵 B팀") + "에 들어갔어요 ("
                + TEAM_A.size() + " 대 " + TEAM_B.size() + ")").withStyle(ChatFormatting.YELLOW));
    }

    private static void leaveLobby(ServerPlayer p) {
        TEAM_A.remove(p.getUUID());
        TEAM_B.remove(p.getUUID());
        if (p.getUUID().equals(host)) {
            reset(p.level().getServer());
            broadcast(p.level().getServer(), Component.literal("⚔ 방장이 나가서 투기장 모집이 취소됐어요").withStyle(ChatFormatting.GRAY));
        }
    }

    // ───────── 시작 ─────────
    private static void start(ServerPlayer starter) {
        MinecraftServer s = starter.level().getServer();
        if (phase != Phase.LOBBY) return;
        if (TEAM_A.isEmpty() || TEAM_A.size() != TEAM_B.size()) {
            starter.sendOverlayMessage(Component.literal("양 팀 인원이 같아야 시작할 수 있어요").withStyle(ChatFormatting.RED));
            return;
        }
        for (UUID id : concat()) {
            if (s.getPlayerList().getPlayer(id) == null) {
                starter.sendOverlayMessage(Component.literal(name(s, id) + " 님이 접속해 있지 않아요").withStyle(ChatFormatting.RED));
                return;
            }
        }
        ServerLevel lv = s.overworld();
        int t = theme < 0 ? lv.getRandom().nextInt(THEMES.length) : theme;
        theme = t;
        force(lv, true);
        build(lv, THEMES[t]);
        DMG.clear();
        DOWN.clear();
        int i = 0;
        for (ServerPlayer p : online(s, TEAM_A)) prepare(p, true, i++);
        i = 0;
        for (ServerPlayer p : online(s, TEAM_B)) prepare(p, false, i++);
        phase = Phase.COUNTDOWN;
        timer = lv.getGameTime() + 100;
        bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("⚔ 투기장"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer p : online(s, concat())) bar.addPlayer(p);
        broadcast(s, Component.literal(String.format("⚔ 투기장 경기 시작! %s · 🔴 %s vs 🔵 %s", THEMES[t].name(), teamText(s, TEAM_A), teamText(s, TEAM_B)))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private static Set<UUID> concat() {
        Set<UUID> all = new LinkedHashSet<>(TEAM_A);
        all.addAll(TEAM_B);
        return all;
    }

    private static BlockPos spawnPos(boolean a, int i) {
        int x = a ? -12 : 12;
        int z = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * 3;
        return CENTER.offset(x, 1, z);
    }

    private static void prepare(ServerPlayer p, boolean a, int i) {
        saveReturn(p);
        if (kit) {
            saveInventory(p);
            giveKit(p);
        }
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.removeAllEffects();
        p.clearFire();
        BlockPos sp = spawnPos(a, i);
        p.teleportTo((ServerLevel) CENTER_LEVEL(p), sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, Set.of(), a ? -90F : 90F, 0F, true);
    }

    private static Level CENTER_LEVEL(ServerPlayer p) {
        return p.level().getServer().overworld();
    }

    private static void giveKit(ServerPlayer p) {
        Inventory inv = p.getInventory();
        inv.clearContent();
        p.setItemSlot(EquipmentSlot.HEAD, tag(new ItemStack(Items.DIAMOND_HELMET)));
        p.setItemSlot(EquipmentSlot.CHEST, tag(new ItemStack(Items.DIAMOND_CHESTPLATE)));
        p.setItemSlot(EquipmentSlot.LEGS, tag(new ItemStack(Items.DIAMOND_LEGGINGS)));
        p.setItemSlot(EquipmentSlot.FEET, tag(new ItemStack(Items.DIAMOND_BOOTS)));
        p.setItemSlot(EquipmentSlot.OFFHAND, tag(new ItemStack(Items.SHIELD)));
        inv.add(tag(new ItemStack(Items.DIAMOND_SWORD)));
        inv.add(tag(new ItemStack(Items.DIAMOND_AXE)));
        inv.add(tag(new ItemStack(Items.BOW)));
        inv.add(tag(new ItemStack(Items.GOLDEN_APPLE, 2)));
        inv.add(tag(new ItemStack(Items.COOKED_BEEF, 16)));
        inv.add(tag(new ItemStack(Items.ARROW, 32)));
    }

    private static ItemStack tag(ItemStack s) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s,
                t -> t.putBoolean("wildplus_arena_kit", true));
        return s;
    }

    // ───────── 가방 맡기기 · 돌아가기 ─────────
    private static void saveInventory(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag me = ContentData.get(s).player("arena_inv", p.getUUID());
        if (me.getBooleanOr("saved", false)) return;
        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ListTag l = new ListTag();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it.isEmpty()) continue;
            int slot = i;
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, it).result().ifPresent(t -> {
                CompoundTag c = new CompoundTag();
                c.putInt("slot", slot);
                c.put("item", t);
                l.add(c);
            });
        }
        me.put("items", l);
        me.putBoolean("saved", true);
        ContentData.get(s).dirty();
        inv.clearContent();
    }

    /** 맡겨 둔 가방을 돌려준다 (경기용 장비는 지움) */
    public static void restoreInventory(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag me = ContentData.get(s).player("arena_inv", p.getUUID());
        if (!me.getBooleanOr("saved", false)) return;
        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Inventory inv = p.getInventory();
        inv.clearContent();
        for (Tag t : me.getListOrEmpty("items")) {
            if (!(t instanceof CompoundTag c)) continue;
            int slot = c.getIntOr("slot", -1);
            Tag it = c.get("item");
            if (it == null) continue;
            ItemStack.OPTIONAL_CODEC.parse(ops, it).result().ifPresent(st -> {
                if (slot >= 0 && slot < inv.getContainerSize() && inv.getItem(slot).isEmpty()) inv.setItem(slot, st);
                else if (!inv.add(st)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), st);
            });
        }
        me.putBoolean("saved", false);
        me.remove("items");
        ContentData.get(s).dirty();
    }

    private static void saveReturn(ServerPlayer p) {
        CompoundTag me = ContentData.get(p.level().getServer()).player("arena_ret", p.getUUID());
        if (me.getBooleanOr("set", false)) return;
        me.putString("dim", p.level().dimension().identifier().toString());
        me.putDouble("x", p.getX());
        me.putDouble("y", p.getY());
        me.putDouble("z", p.getZ());
        me.putBoolean("set", true);
        ContentData.get(p.level().getServer()).dirty();
    }

    private static void goBack(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag me = ContentData.get(s).player("arena_ret", p.getUUID());
        if (!me.getBooleanOr("set", false)) return;
        ServerLevel lv = s.overworld();
        String dim = me.getStringOr("dim", "");
        for (ServerLevel l : s.getAllLevels()) if (l.dimension().identifier().toString().equals(dim)) lv = l;
        p.teleportTo(lv, me.getDoubleOr("x", 0), me.getDoubleOr("y", 100), me.getDoubleOr("z", 0), Set.of(), p.getYRot(), 0F, true);
        p.fallDistance = 0;
        me.putBoolean("set", false);
        ContentData.get(s).dirty();
    }

    private static void toStand(ServerPlayer p) {
        ServerLevel lv = p.level().getServer().overworld();
        double a = p.getRandom().nextDouble() * Math.PI * 2;
        double rr = R + 3.5;
        double x = CENTER.getX() + 0.5 + Math.cos(a) * rr, z = CENTER.getZ() + 0.5 + Math.sin(a) * rr;
        x = Mth.clamp(x, CENTER.getX() - R - 4.5, CENTER.getX() + R + 4.5);
        z = Mth.clamp(z, CENTER.getZ() - R - 4.5, CENTER.getZ() + R + 4.5);
        float yaw = (float) (Math.toDegrees(Math.atan2(CENTER.getZ() - z, CENTER.getX() - x)) - 90);
        p.teleportTo(lv, x, CENTER.getY() + 8, z, Set.of(), yaw, 20F, true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || inMatch(p.getUUID())) return;
        restoreInventory(p);
        goBack(p);
    }

    // ───────── 진행 ─────────
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (phase == Phase.NONE || phase == Phase.LOBBY) return;
        MinecraftServer s = event.getServer();
        ServerLevel lv = s.overworld();
        long now = lv.getGameTime();
        if (now % 5 != 0) return;
        if (phase == Phase.COUNTDOWN) {
            long left = (timer - now + 19) / 20;
            int idxA = 0, idxB = 0;
            for (UUID id : concat()) {
                ServerPlayer p = s.getPlayerList().getPlayer(id);
                if (p == null) continue;
                boolean a = TEAM_A.contains(id);
                BlockPos sp = spawnPos(a, a ? idxA++ : idxB++);
                if (p.distanceToSqr(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5) > 1.0) p.teleportTo(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
                if (now % 20 == 0 && left > 0) {
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.valueOf(left)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(a ? "🔴 A팀" : "🔵 B팀").withStyle(a ? ChatFormatting.RED : ChatFormatting.BLUE)));
                    lv.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1F, 1.2F);
                }
            }
            if (now >= timer) {
                phase = Phase.FIGHT;
                startedAt = now;
                for (ServerPlayer p : online(s, concat())) {
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("⚔ 싸움 시작!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("")));
                    lv.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
                }
            }
            return;
        }
        if (phase == Phase.FIGHT) {
            // 투기장을 벗어나거나 나간 사람은 탈락
            for (UUID id : concat()) {
                if (DOWN.contains(id)) continue;
                ServerPlayer p = s.getPlayerList().getPlayer(id);
                if (p == null || p.level() != lv || !inside(p)) down(s, id, p, p == null ? "접속 종료" : "투기장 이탈");
            }
            long left = Math.max(0, TIME_LIMIT - (now - startedAt));
            if (bar != null) {
                bar.setName(Component.literal(String.format("⚔ 🔴 %d명  vs  🔵 %d명 · %d:%02d", alive(TEAM_A), alive(TEAM_B), left / 1200, left / 20 % 60)));
                bar.setProgress(Mth.clamp(left / (float) TIME_LIMIT, 0F, 1F));
            }
            if (alive(TEAM_A) == 0 || alive(TEAM_B) == 0) finish(s, alive(TEAM_A) > 0 ? 1 : alive(TEAM_B) > 0 ? 2 : 0);
            else if (left <= 0) finish(s, 0);
            return;
        }
        if (phase == Phase.END && now >= timer) cleanup(s);
    }

    private static boolean inside(Player p) {
        return Math.abs(p.getX() - CENTER.getX() - 0.5) <= R + 0.5 && Math.abs(p.getZ() - CENTER.getZ() - 0.5) <= R + 0.5
                && p.getY() > CENTER.getY() - 3 && p.getY() < CENTER.getY() + H + 2;
    }

    private static void down(MinecraftServer s, UUID id, ServerPlayer p, String why) {
        DOWN.add(id);
        boolean a = TEAM_A.contains(id);
        broadcast(s, Component.literal("☠ " + name(s, id) + " (" + (a ? "🔴" : "🔵") + ") " + why + " · 🔴 " + alive(TEAM_A) + " vs 🔵 " + alive(TEAM_B))
                .withStyle(ChatFormatting.GRAY));
        if (p != null && p.isAlive()) {
            p.setHealth(p.getMaxHealth());
            p.clearFire();
            p.removeAllEffects();
            if (p.level() == s.overworld() && (inside(p) || why.equals("쓰러짐"))) toStand(p);
        }
    }

    /** winner: 1 = A, 2 = B, 0 = 무승부 */
    private static void finish(MinecraftServer s, int winner) {
        phase = Phase.END;
        timer = s.overworld().getGameTime() + 100;
        Set<UUID> win = winner == 1 ? TEAM_A : winner == 2 ? TEAM_B : Set.of();
        Set<UUID> lose = winner == 1 ? TEAM_B : winner == 2 ? TEAM_A : Set.of();
        String title = winner == 0 ? "무승부" : (winner == 1 ? "🔴 A팀 승리!" : "🔵 B팀 승리!");
        broadcast(s, Component.literal("⚔ 투기장 경기 끝 · " + title + (winner == 0 ? "" : " (" + teamText(s, win) + ")"))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        Map<UUID, String> names = new HashMap<>();
        for (UUID id : concat()) names.put(id, name(s, id));
        broadcast(s, DamageBoard.of("투기장 피해 순위", DMG, names));
        int winPts = 25 + 5 * (teamSize - 1), losePts = 15;
        long winMedal = 10 + 5L * (teamSize - 1);
        for (UUID id : concat()) {
            CompoundTag st = stats(s, id);
            st.putString("name", name(s, id));
            int before = st.getIntOr("points", 0);
            long medal;
            int after;
            if (win.contains(id)) {
                after = before + winPts;
                st.putInt("wins", st.getIntOr("wins", 0) + 1);
                medal = winMedal;
            } else if (lose.contains(id)) {
                after = Math.max(0, before - losePts);
                st.putInt("losses", st.getIntOr("losses", 0) + 1);
                medal = 3;
            } else {
                after = before;
                medal = 5;
            }
            st.putInt("points", after);
            st.putLong("medals", st.getLongOr("medals", 0L) + medal);
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p != null) {
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(win.contains(id) ? "🏆 승리!" : lose.contains(id) ? "패배" : "무승부")
                        .withStyle(win.contains(id) ? ChatFormatting.GOLD : ChatFormatting.GRAY, ChatFormatting.BOLD)));
                p.sendSystemMessage(Component.literal(String.format("🏅 승점 %d → %d (%s) · 메달 +%d", before, after, TIER[tier(after)], medal))
                        .withStyle(ChatFormatting.YELLOW));
                if (tier(after) > tier(before)) p.sendSystemMessage(Component.literal("⬆ 등급 상승! " + TIER[tier(after)]).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
        }
        ContentData.get(s).dirty();
        for (ServerPlayer p : online(s, win)) {
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1, 0.6, 0.3);
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    private static void cleanup(MinecraftServer s) {
        ServerLevel lv = s.overworld();
        for (ServerPlayer p : online(s, concat())) {
            p.setHealth(p.getMaxHealth());
            p.removeAllEffects();
            p.clearFire();
            restoreInventory(p);
            goBack(p);
        }
        // 관전하러 온 사람도 돌려보냄
        for (ServerPlayer p : new ArrayList<>(lv.players())) {
            if (Math.abs(p.getX() - CENTER.getX()) < R + 8 && Math.abs(p.getZ() - CENTER.getZ()) < R + 8) goBack(p);
        }
        AABB box = new AABB(CENTER).inflate(R + 6, 20, R + 6);
        for (ItemEntity it : lv.getEntitiesOfClass(ItemEntity.class, box)) it.discard();
        for (var e : lv.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, box)) e.discard();
        force(lv, false);
        reset(s);
    }

    private static void reset(MinecraftServer s) {
        phase = Phase.NONE;
        host = null;
        TEAM_A.clear();
        TEAM_B.clear();
        DOWN.clear();
        DMG.clear();
        if (bar != null) bar.removeAllPlayers();
        bar = null;
    }

    // ───────── 전투 규칙 ─────────
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !inMatch(p.getUUID())) return;
        event.setCanceled(true);
        if (phase == Phase.FIGHT && !DOWN.contains(p.getUUID())) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1F, 1F);
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            down(p.level().getServer(), p.getUUID(), p, "쓰러짐");
        } else {
            p.setHealth(p.getMaxHealth());
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (phase == Phase.NONE || phase == Phase.LOBBY) return;
        if (event.getEntity() instanceof ServerPlayer t && inMatch(t.getUUID())) {
            // 시작 전 · 끝난 뒤 · 이미 쓰러진 사람은 피해 없음
            if (phase != Phase.FIGHT || DOWN.contains(t.getUUID())) {
                event.setCanceled(true);
                return;
            }
            if (event.getSource().getEntity() instanceof ServerPlayer a && a != t) {
                boolean same = (TEAM_A.contains(a.getUUID()) && TEAM_A.contains(t.getUUID())) || (TEAM_B.contains(a.getUUID()) && TEAM_B.contains(t.getUUID()));
                if (same || DOWN.contains(a.getUUID()) || !inMatch(a.getUUID())) event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (phase != Phase.FIGHT || !(event.getEntity() instanceof ServerPlayer t) || !inMatch(t.getUUID())) return;
        if (event.getSource().getEntity() instanceof ServerPlayer a && opponents(a.getUUID(), t.getUUID())) {
            DMG.merge(a.getUUID(), event.getHealthDamage(), Float::sum);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        reset(event.getServer());
    }

    // ───────── 경기장 짓기 ─────────
    private static void force(ServerLevel lv, boolean on) {
        for (int cx = (CENTER.getX() - R - 6) >> 4; cx <= (CENTER.getX() + R + 6) >> 4; cx++) {
            for (int cz = (CENTER.getZ() - R - 6) >> 4; cz <= (CENTER.getZ() + R + 6) >> 4; cz++) lv.setChunkForced(cx, cz, on);
        }
    }

    private static void build(ServerLevel lv, Theme t) {
        BlockState air = Blocks.AIR.defaultBlockState(), barrier = Blocks.BARRIER.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        int cx = CENTER.getX(), cy = CENTER.getY(), cz = CENTER.getZ();
        int O = R + 5;
        for (int dx = -O; dx <= O; dx++) {
            for (int dz = -O; dz <= O; dz++) {
                int m = Math.max(Math.abs(dx), Math.abs(dz));
                for (int y = -1; y <= H + 2; y++) {
                    BlockPos q = new BlockPos(cx + dx, cy + y, cz + dz);
                    BlockState st;
                    if (m < R) {
                        // 경기장 안
                        if (y == -1) st = t.wall().defaultBlockState();
                        else if (y == 0) {
                            boolean line = dx == 0;
                            boolean lamp = Math.floorMod(dx, 8) == 4 && Math.floorMod(dz, 8) == 4;
                            st = (line ? t.accent() : lamp ? t.light()
                                    : Math.floorMod((dx >> 1) + (dz >> 1), 2) == 0 ? t.floor() : t.accent()).defaultBlockState();
                        } else if (y == H + 1) st = barrier;
                        else {
                            // 엄폐 기둥 4개 (2×2, 높이 4) + 가운데 낮은 둔덕
                            boolean pillar = (Math.abs(dx) == 6 || Math.abs(dx) == 7) && (Math.abs(dz) == 6 || Math.abs(dz) == 7) && y <= 4;
                            st = pillar ? (y == 4 ? t.light().defaultBlockState() : t.pillar().defaultBlockState()) : air;
                        }
                    } else if (m == R) {
                        // 벽: 1~5칸 벽, 그 위는 유리 · 보이지 않는 벽
                        if (y <= 0) st = t.wall().defaultBlockState();
                        else if (y <= 5) st = (y == 5 && (dx + dz) % 4 == 0) ? t.light().defaultBlockState() : t.wall().defaultBlockState();
                        else if (y <= 7) st = glass;
                        else st = barrier;
                    } else {
                        // 관전석: 벽 바깥 높은 단 (y 7) + 난간
                        if (y == 6) st = m <= R + 4 ? t.stand().defaultBlockState() : air;
                        else if (y == 7 && m == R + 4) st = t.wall().defaultBlockState();
                        else if (y == 8 && m == R + 4 && (dx + dz) % 6 == 0) st = t.light().defaultBlockState();
                        else if (y == H + 2 && m <= R + 4) st = barrier;
                        else if (y >= 7 && m == R + 5) st = barrier;
                        else st = air;
                    }
                    lv.setBlock(q, st, 2);
                }
            }
        }
        // 양쪽 출발 지점 표시
        for (int k = -4; k <= 4; k++) {
            lv.setBlock(CENTER.offset(-12, 0, k), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            lv.setBlock(CENTER.offset(12, 0, k), Blocks.LAPIS_BLOCK.defaultBlockState(), 2);
        }
    }

    /** 시험용: 경기장만 지어서 보여 준다 */
    public static void debugBuild(ServerPlayer p, int t) {
        ServerLevel lv = p.level().getServer().overworld();
        force(lv, true);
        build(lv, THEMES[Math.floorMod(t, THEMES.length)]);
        p.teleportTo(lv, CENTER.getX() + 0.5, CENTER.getY() + 8, CENTER.getZ() - R - 3.5, Set.of(), 0F, 25F, true);
    }

    // ───────── 명령어 ─────────
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arena")
                .executes(ctx -> {
                    open(ctx.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("leave").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (phase == Phase.LOBBY) leaveLobby(p);
                    else if (phase == Phase.FIGHT && inMatch(p.getUUID()) && !DOWN.contains(p.getUUID())) down(p.level().getServer(), p.getUUID(), p, "기권");
                    else if (!inMatch(p.getUUID())) goBack(p);
                    return 1;
                })));
    }
}

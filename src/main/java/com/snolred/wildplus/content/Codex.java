package com.snolred.wildplus.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.snolred.wildplus.economy.Prices;
import com.snolred.wildplus.economy.WildData;
import com.snolred.wildplus.menu.PickMenu;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 4.9 도감 · 업적 · 칭호.
 * 도감: 아이템은 가방에 들어오면, 몬스터는 잡으면 등록. 분류 절반/전부 채우면 보상.
 * 업적: 통계(stat)가 목표에 닿으면 통장으로 보상 + 칭호. 칭호는 이름 앞에 하나 골라 단다.
 * 저장: ContentData "codex" (플레이어별) — f(등록) · st(통계) · ach(업적) · ms(분류 보상) · title · book
 */
public final class Codex {
    private Codex() {}

    // ───────── 도감 ─────────
    public record Cat(String name, String icon, String hint, boolean mobs, List<String> ids) {}

    private static List<String> w(String s) {
        List<String> out = new ArrayList<>();
        for (String k : s.trim().split("\\s+")) out.add(k.contains(":") ? k : "wildplus:" + k);
        return out;
    }

    public static final List<Cat> CATS = List.of(
            new Cat("농장", "wildplus:tomato", "밭이나 목장에서 얻어요", false, w("chili tomato strawberry corn cabbage blueberry grape rice onion ginseng "
                    + "milk_bottle butter cream cheese wool_tuft yarn cloth golden_egg")),
            new Cat("물고기", "wildplus:rainbow_trout", "낚시로 낚아요", false, w("rainbow_trout golden_carp moonlight_fish storm_eel anglerfish ice_smelt catfish tuna sweetfish "
                    + "dragon_king_fish sunken_chest")),
            new Cat("요리", "wildplus:spicy_stew", "조리 기구나 제작대에서 만들어요", false, w("spicy_stew honey_toast spicy_skewer traveler_ration tomato_soup pizza strawberry_tart sashimi_platter fish_stew "
                    + "cooked_rainbow_trout grilled_eel grilled_corn salad blueberry_muffin cooked_catfish tuna_steak grilled_sweetfish "
                    + "kimchi_stew spicy_fish_stew seafood_stew curry_rice ginseng_chicken seaweed_soup mushroom_cream_soup onion_soup cheese_pizza apple_pie "
                    + "strawberry_cake grape_tart gratin baguette fish_bread fish_pie steak bulgogi pork_belly grilled_fish_set chicken_skewer lamb_chop "
                    + "dragon_king_grill fried_rice tteokbokki seafood_pancake omurice cream_pasta eel_rice_bowl rolled_omelette gimbap sushi_set hoe_rice_bowl "
                    + "bibimbap fruit_salad kimchi wine beer makgeolli ginseng_wine butter_cookie cheesecake custard_pudding carbonara cream_stew egg_toast")),
            new Cat("광물 · 보석", "wildplus:ruby", "캐거나 제련해서 얻어요", false, w("raw_mythril mythril_ingot jade ruby sapphire blood_shard bloodsteel_ingot stellar_core life_crystal life_essence "
                    + "void_shard raw_void_stone void_stone rift_dust")),
            new Cat("장비", "wildplus:ruby_sword", "제작하거나 강화해서 얻어요", false, w("bloodsteel_sword bloodsteel_chestplate mythril_sword mythril_chestplate jade_sword jade_chestplate ruby_sword ruby_chestplate "
                    + "star_ruby_sword star_ruby_chestplate sapphire_sword sapphire_chestplate sky_sapphire_sword sky_sapphire_chestplate emerald_sword emerald_chestplate "
                    + "royal_emerald_sword royal_emerald_chestplate void_sword void_chestplate mythril_bow ruby_bow star_ruby_bow sapphire_bow sky_sapphire_bow emerald_bow "
                    + "royal_emerald_bow void_bow jade_fishing_rod dragon_fishing_rod drill")),
            new Cat("전리품 · 각인", "wildplus:lord_heart", "보스나 유적에서 얻어요", false, w("lord_heart titan_core lich_orb frost_crown dragon_plume void_heart void_feather void_key lord_shard lord_sigil "
                    + "wraith_plume volcano_heart awakening_core_1 awakening_core_2 awakening_core_3 awakening_core_4 awakening_core_5 "
                    + "engrave_abyss engrave_devour engrave_chain engrave_warp engrave_heart engrave_harvest engrave_fury engrave_guard engrave_vigor engrave_rush")),
            new Cat("몬스터", "minecraft:bone", "직접 쓰러뜨리면 등록돼요", true, w("bandit bandit_boss wanted golden_slime treasure_rabbit illager_mercenary void_stalker rift_mite void_wraith "
                    + "cloud_sentinel thunder_hawk cloud_priest blood_lord dungeon_guardian titan_boss lich_boss frost_boss vine_boss pharaoh_boss flame_boss abyss_boss "
                    + "void_boss gold_boss storm_king volcano_boss void_lord minecraft:ender_dragon minecraft:wither minecraft:warden minecraft:elder_guardian")));

    private static final Set<String> BOSSES = Set.copyOf(w("blood_lord dungeon_guardian titan_boss lich_boss frost_boss vine_boss pharaoh_boss flame_boss abyss_boss void_boss gold_boss "
            + "storm_king volcano_boss void_lord bandit_boss minecraft:ender_dragon minecraft:wither minecraft:warden minecraft:elder_guardian"));
    private static final Map<String, String> VANILLA_MOB = Map.of("minecraft:ender_dragon", "엔더 드래곤", "minecraft:wither", "위더", "minecraft:warden", "워든", "minecraft:elder_guardian", "엘더 가디언");
    private static final Map<Item, String> ITEM_KEY = new HashMap<>();
    private static final Map<String, Integer> CAT_OF = new HashMap<>();
    private static JsonObject lang;
    private static int total = -1;

    private static void init() {
        if (total >= 0) return;
        int n = 0;
        for (int c = 0; c < CATS.size(); c++) {
            for (String id : CATS.get(c).ids()) {
                CAT_OF.put(id, c);
                n++;
                if (!CATS.get(c).mobs()) {
                    Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
                    if (it != Items.AIR) ITEM_KEY.put(it, id);
                }
            }
        }
        try (var in = Codex.class.getResourceAsStream("/assets/wildplus/lang/ko_kr.json")) {
            lang = in == null ? new JsonObject() : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            lang = new JsonObject();
        }
        total = n;
    }

    /** 서버 언어와 상관없이 한글 이름 */
    private static String nameOf(String id, boolean mob) {
        if (VANILLA_MOB.containsKey(id)) return VANILLA_MOB.get(id);
        String path = id.substring(id.indexOf(':') + 1);
        String k = (mob ? "entity.wildplus." : "item.wildplus.") + path;
        return lang.has(k) ? lang.get(k).getAsString() : path;
    }

    private static ItemStack stack(String id) {
        Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return new ItemStack(it == Items.AIR ? Items.PAPER : it);
    }

    private static CompoundTag data(MinecraftServer s, UUID id) {
        return ContentData.get(s).player("codex", id);
    }

    private static CompoundTag sub(CompoundTag t, String k) {
        if (t.get(k) instanceof CompoundTag c) return c;
        CompoundTag c = new CompoundTag();
        t.put(k, c);
        return c;
    }

    private static int found(CompoundTag d, int cat) {
        CompoundTag f = sub(d, "f");
        int n = 0;
        for (String id : CATS.get(cat).ids()) if (f.contains(id)) n++;
        return n;
    }

    private static int foundAll(CompoundTag d) {
        return sub(d, "f").keySet().size();
    }

    private static void reward(MinecraftServer s, UUID id, long amount, String why) {
        if (amount <= 0) return;
        WildData wd = WildData.get(s);
        wd.reason = why;
        wd.addBalance(id, amount);
    }

    private static void discover(ServerPlayer p, String id) {
        init();
        Integer cat = CAT_OF.get(id);
        if (cat == null) return;
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        CompoundTag f = sub(d, "f");
        if (f.contains(id)) return;
        f.putBoolean(id, true);
        ContentData.get(s).dirty();
        Cat c = CATS.get(cat);
        int n = found(d, cat), m = c.ids().size();
        long coin = Prices.x(300L);
        reward(s, p.getUUID(), coin, "도감 등록");
        p.sendSystemMessage(Component.literal(String.format("📖 도감 등록: %s  (%s %d/%d) · +%,d동", nameOf(id, c.mobs()), c.name(), n, m, coin)).withStyle(ChatFormatting.AQUA));
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        CompoundTag ms = sub(d, "ms");
        if (n * 2 >= m && !ms.contains("h" + cat)) {
            ms.putBoolean("h" + cat, true);
            long r = Prices.x(10_000L);
            reward(s, p.getUUID(), r, "도감 보상");
            p.sendSystemMessage(Component.literal(String.format("📖 %s 도감 절반 달성! +%,d동", c.name(), r)).withStyle(ChatFormatting.GOLD));
        }
        if (n >= m && !ms.contains("c" + cat)) {
            ms.putBoolean("c" + cat, true);
            long r = Prices.x(50_000L);
            reward(s, p.getUUID(), r, "도감 보상");
            String title = CAT_TITLE[cat];
            sub(d, "titles").putBoolean(title, true);
            s.getPlayerList().broadcastSystemMessage(Component.literal(String.format("📖 %s 님이 %s 도감을 완성했어요! (칭호: %s)", p.getName().getString(), c.name(), title))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            p.sendSystemMessage(Component.literal(String.format("   완성 보상 +%,d동", r)).withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        check(s, p.getUUID());
    }

    private static final String[] CAT_TITLE = {"농사의 달인", "바다의 친구", "미식가", "보석 수집가", "무기고", "전리품 사냥꾼", "몬스터 박사"};

    // ───────── 업적 ─────────
    /** stat: 통계 이름 (bal·honor·dex 는 계산값) · goal: 목표 · coin: 보상(물가 적용 전) · title: 칭호(없으면 "") */
    public record Ach(String id, String name, String desc, String stat, long goal, long coin, String title, String icon) {}

    public static final List<Ach> ACHS = List.of(
            new Ach("kill1", "첫 사냥", "몬스터 1마리 처치", "kill", 1, 500, "", "minecraft:iron_sword"),
            new Ach("kill2", "사냥꾼", "몬스터 100마리 처치", "kill", 100, 3_000, "사냥꾼", "minecraft:iron_sword"),
            new Ach("kill3", "학살자", "몬스터 1,000마리 처치", "kill", 1_000, 20_000, "학살자", "minecraft:diamond_sword"),
            new Ach("kill4", "전설의 사냥꾼", "몬스터 5,000마리 처치", "kill", 5_000, 100_000, "전설의 사냥꾼", "minecraft:netherite_sword"),
            new Ach("boss1", "보스 사냥", "보스 1마리 처치", "boss", 1, 5_000, "", "wildplus:lord_heart"),
            new Ach("boss2", "보스 헌터", "보스 10마리 처치", "boss", 10, 30_000, "보스 헌터", "wildplus:titan_core"),
            new Ach("boss3", "신 살해자", "보스 50마리 처치", "boss", 50, 200_000, "신 살해자", "wildplus:lord_sigil"),
            new Ach("death1", "칠전팔기", "10번 쓰러지기", "death", 10, 2_000, "오뚝이", "minecraft:totem_of_undying"),
            new Ach("fish1", "첫 입질", "물고기 1마리 낚기", "fish", 1, 500, "", "minecraft:fishing_rod"),
            new Ach("fish2", "강태공", "100번 낚기", "fish", 100, 5_000, "강태공", "wildplus:jade_fishing_rod"),
            new Ach("fish3", "낚시왕", "1,000번 낚기", "fish", 1_000, 50_000, "낚시왕", "wildplus:dragon_fishing_rod"),
            new Ach("crop1", "첫 수확", "다 자란 작물 1개 수확", "crop", 1, 500, "", "minecraft:wheat"),
            new Ach("crop2", "농부", "작물 500개 수확", "crop", 500, 5_000, "농부", "wildplus:tomato"),
            new Ach("crop3", "대농장주", "작물 5,000개 수확", "crop", 5_000, 50_000, "대농장주", "wildplus:ginseng"),
            new Ach("ore1", "첫 광석", "광석 1개 캐기", "ore", 1, 500, "", "minecraft:raw_iron"),
            new Ach("ore2", "광부", "광석 300개 캐기", "ore", 300, 5_000, "광부", "minecraft:diamond"),
            new Ach("ore3", "광산왕", "광석 3,000개 캐기", "ore", 3_000, 50_000, "광산왕", "wildplus:ruby"),
            new Ach("log1", "나무꾼", "원목 500개 캐기", "log", 500, 5_000, "나무꾼", "minecraft:iron_axe"),
            new Ach("breed1", "목동", "동물 50마리 번식", "breed", 50, 5_000, "목동", "minecraft:wheat"),
            new Ach("cook1", "첫 요리", "조리 기구로 요리 1개 완성", "cook", 1, 500, "", "wildplus:tomato_soup"),
            new Ach("cook2", "요리사", "요리 100개 완성", "cook", 100, 5_000, "요리사", "wildplus:curry_rice"),
            new Ach("cook3", "미슐랭 셰프", "요리 1,000개 완성", "cook", 1_000, 50_000, "미슐랭 셰프", "wildplus:sushi_set"),
            new Ach("ship1", "첫 출하", "출하로 돈 벌기", "ship", 1, 500, "", "minecraft:chest"),
            new Ach("ship2", "상인", "출하 누적 100만동", "ship", 1_000_000, 10_000, "상인", "wildplus:silver_coin"),
            new Ach("ship3", "거상", "출하 누적 1억동", "ship", 100_000_000, 200_000, "거상", "wildplus:gold_coin"),
            new Ach("bal1", "부자", "통장 잔액 1,000만동", "bal", 10_000_000, 20_000, "부자", "wildplus:gold_coin"),
            new Ach("bal2", "재벌", "통장 잔액 10억동", "bal", 1_000_000_000, 500_000, "재벌", "wildplus:diamond_coin"),
            new Ach("market1", "첫 손님", "거래소에서 물건 1건 판매", "market", 1, 1_000, "", "minecraft:emerald"),
            new Ach("market2", "장사꾼", "거래소에서 30건 판매", "market", 30, 20_000, "장사꾼", "minecraft:emerald"),
            new Ach("lotto1", "행운아", "복권 당첨 1번", "lotto", 1, 1_000, "행운아", "wildplus:lottery_ticket"),
            new Ach("tower1", "탑 입문", "하늘의 탑 10층 도달", "tower", 10, 5_000, "", "minecraft:feather"),
            new Ach("tower2", "탑 등반가", "하늘의 탑 30층 도달", "tower", 30, 20_000, "등반가", "minecraft:feather"),
            new Ach("tower3", "탑의 정복자", "하늘의 탑 60층 도달", "tower", 60, 80_000, "탑의 정복자", "minecraft:elytra"),
            new Ach("tower4", "하늘의 지배자", "하늘의 탑 100층 도달", "tower", 100, 300_000, "하늘의 지배자", "minecraft:nether_star"),
            new Ach("raid1", "레이드 첫 클리어", "레이드 1번 클리어", "raid", 1, 5_000, "", "minecraft:shield"),
            new Ach("raid2", "레이더", "레이드 10번 클리어", "raid", 10, 50_000, "레이더", "minecraft:trident"),
            new Ach("honor1", "명예의 시작", "명예 브론즈 달성", "honor", 1, 2_000, "", "minecraft:gold_nugget"),
            new Ach("honor2", "살아 있는 전설", "명예 레전드 달성", "honor", 5, 100_000, "살아 있는 전설", "minecraft:gold_block"),
            new Ach("dex1", "수집 시작", "도감 25개 등록", "dex", 25, 5_000, "", "minecraft:book"),
            new Ach("dex2", "수집가", "도감 100개 등록", "dex", 100, 50_000, "수집가", "minecraft:enchanted_book"),
            new Ach("dex3", "만물박사", "도감 전부 등록", "dex", -1, 500_000, "만물박사", "minecraft:nether_star"),
            new Ach("play1", "단골", "10시간 플레이", "play", 600, 5_000, "단골", "minecraft:clock"),
            new Ach("play2", "터줏대감", "100시간 플레이", "play", 6_000, 100_000, "터줏대감", "minecraft:clock"));

    private static String short_(long v) {
        return v >= 100_000_000L ? (v / 100_000_000L) + "억" + (v % 100_000_000L >= 10_000L ? " " + (v % 100_000_000L / 10_000L) + "만" : "") : v >= 10_000L ? (v / 10_000L) + "만" + (v % 10_000L > 0 ? " " + (v % 10_000L) : "") : String.valueOf(v);
    }

    private static long goal(Ach a) {
        init();
        return a.goal() < 0 ? total : a.goal();
    }

    private static long stat(MinecraftServer s, UUID id, CompoundTag d, String k) {
        return switch (k) {
            case "bal" -> WildData.get(s).balance(id);
            case "honor" -> WildData.get(s).honor(id);
            case "dex" -> foundAll(d);
            default -> sub(d, "st").getLongOr(k, 0L);
        };
    }

    /** 통계 더하기 (접속해 있지 않아도 됨) */
    public static void add(MinecraftServer s, UUID id, String stat, long delta) {
        if (s == null || id == null || delta <= 0) return;
        CompoundTag st = sub(data(s, id), "st");
        st.putLong(stat, st.getLongOr(stat, 0L) + delta);
        ContentData.get(s).dirty();
        check(s, id);
    }

    public static void add(ServerPlayer p, String stat, long delta) {
        add(p.level().getServer(), p.getUUID(), stat, delta);
    }

    /** 최고 기록 갱신 (탑 층수) */
    public static void max(ServerPlayer p, String stat, long value) {
        MinecraftServer s = p.level().getServer();
        CompoundTag st = sub(data(s, p.getUUID()), "st");
        if (st.getLongOr(stat, 0L) >= value) return;
        st.putLong(stat, value);
        ContentData.get(s).dirty();
        check(s, p.getUUID());
    }

    private static void check(MinecraftServer s, UUID id) {
        CompoundTag d = data(s, id);
        CompoundTag done = sub(d, "ach");
        for (Ach a : ACHS) {
            if (done.contains(a.id()) || stat(s, id, d, a.stat()) < goal(a)) continue;
            done.putBoolean(a.id(), true);
            if (!a.title().isEmpty()) sub(d, "titles").putBoolean(a.title(), true);
            ContentData.get(s).dirty();
            long coin = Prices.x(a.coin());
            reward(s, id, coin, "업적 보상");
            String who = WildData.get(s).names.getOrDefault(id, "누군가");
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p != null) who = p.getName().getString();
            s.getPlayerList().broadcastSystemMessage(Component.literal(String.format("🏆 %s 님이 업적 [%s] 달성!", who, a.name())).withStyle(ChatFormatting.YELLOW), false);
            if (p != null) {
                p.sendSystemMessage(Component.literal(String.format("   %s · 보상 +%,d동%s", a.desc(), coin, a.title().isEmpty() ? "" : " · 칭호 [" + a.title() + "] 획득")).withStyle(ChatFormatting.GREEN));
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7F, 1.0F);
            }
        }
    }

    // ───────── 이벤트 ─────────
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 7) return;
        init();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            String id = ITEM_KEY.get(st.getItem());
            if (id != null) discover(p, id);
        }
        if (p.tickCount % 1200 == 7) add(p, "play", 1); // 1분마다 (잔액 · 명예 업적도 이때 확인)
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer dead) {
            add(dead, "death", 1);
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        if (event.getEntity() instanceof Monster || BOSSES.contains(id)) add(p, "kill", 1);
        if (BOSSES.contains(id)) add(p, "boss", 1);
        discover(p, id);
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) add(p, "fish", 1);
    }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer p)) return;
        var st = event.getState();
        if (st.getBlock() instanceof CropBlock c && c.isMaxAge(st)) add(p, "crop", 1);
        else if (st.is(Tags.Blocks.ORES)) add(p, "ore", 1);
        else if (st.is(BlockTags.LOGS)) add(p, "log", 1);
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer p) add(p, "breed", 1);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        if (d.getBooleanOr("book", false)) return;
        d.putBoolean("book", true);
        ContentData.get(s).dirty();
        Item journal = BuiltInRegistries.ITEM.getValue(Identifier.parse("wildplus:adventure_journal"));
        if (journal != Items.AIR) p.getInventory().add(new ItemStack(journal));
        p.sendSystemMessage(Component.literal("📖 모험 수첩을 받았어요! 들고 우클릭하면 도감 · 업적 · 칭호를 볼 수 있어요 (/dex 로도 열려요)").withStyle(ChatFormatting.AQUA));
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString().equals("wildplus:adventure_journal")) return;
        open(p);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onName(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            String t = data(p.level().getServer(), p.getUUID()).getStringOr("title", "");
            if (!t.isEmpty()) event.setDisplayname(Component.literal("[" + t + "] ").withStyle(ChatFormatting.AQUA).append(event.getDisplayname()));
        }
    }

    // ───────── 화면 ─────────
    public static void open(ServerPlayer p) {
        init();
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        List<PickMenu.Entry> e = new ArrayList<>();
        for (int c = 0; c < CATS.size(); c++) {
            Cat cat = CATS.get(c);
            int n = found(d, c), m = cat.ids().size();
            e.add(new PickMenu.Entry(cat.name(), n >= m ? "완성!" : n + " / " + m, stack(cat.icon())));
        }
        int done = sub(d, "ach").keySet().size();
        String title = d.getStringOr("title", "");
        e.add(new PickMenu.Entry("업적", done + " / " + ACHS.size(), new ItemStack(Items.GOLD_INGOT)));
        e.add(new PickMenu.Entry("칭호", title.isEmpty() ? "없음" : title, new ItemStack(Items.NAME_TAG)));
        PickMenu.open(p, Component.literal("📖 모험 수첩"), String.format("도감 %d / %d · 업적 %d / %d", foundAll(d), total, done, ACHS.size()), "", e, PickMenu.FLAG_SHOP | PickMenu.FLAG_BOOK, i -> {
            if (i < CATS.size()) openCat(p, i);
            else if (i == CATS.size()) openAch(p);
            else openTitles(p);
        }, null, x -> true);
    }

    private static void openCat(ServerPlayer p, int c) {
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        CompoundTag f = sub(d, "f");
        Cat cat = CATS.get(c);
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("돌아가기", "첫 화면", new ItemStack(Items.BOOK)));
        for (String id : cat.ids()) {
            if (f.contains(id)) e.add(new PickMenu.Entry(nameOf(id, cat.mobs()), "등록 완료", cat.mobs() ? new ItemStack(BOSSES.contains(id) ? Items.NETHER_STAR : Items.BONE) : stack(id)));
            else e.add(new PickMenu.Entry("???", "미발견", new ItemStack(Items.PAPER)));
        }
        int n = found(d, c), m = cat.ids().size();
        PickMenu.open(p, Component.literal("📖 " + cat.name() + " 도감"), String.format("%d / %d · %s · 완성하면 %,d동 + 칭호 [%s]", n, m, cat.hint(), Prices.x(50_000L), CAT_TITLE[c]),
                "", e, PickMenu.FLAG_SHOP | PickMenu.FLAG_BOOK, i -> {
                    if (i == 0) open(p);
                }, null, x -> true);
    }

    private static int group(String stat) {
        return switch (stat) {
            case "kill", "boss", "death" -> 1;
            case "fish", "crop", "ore", "log", "breed", "cook" -> 2;
            case "ship", "bal", "market", "lotto" -> 3;
            case "tower", "raid", "honor" -> 4;
            default -> 5;
        };
    }

    /** 업적 전용 화면 (client/AchScreen): 이름 / 설명\n현재\n목표\n보상\n칭호\n달성\n분류 */
    private static void openAch(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        CompoundTag done = sub(d, "ach");
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("돌아가기", "첫 화면", new ItemStack(Items.BOOK)));
        for (Ach a : ACHS) {
            boolean ok = done.contains(a.id());
            long g = goal(a), v = Math.min(g, stat(s, p.getUUID(), d, a.stat()));
            e.add(new PickMenu.Entry(a.name(), a.desc() + "\n" + v + "\n" + g + "\n" + short_(Prices.x(a.coin())) + "\n" + a.title() + "\n" + (ok ? 1 : 0) + "\n" + group(a.stat()), stack(a.icon())));
        }
        PickMenu.open(p, Component.literal("🏆 업적"), "", "", e, PickMenu.FLAG_ACH, i -> {
            if (i == 0) open(p);
        }, null, x -> true);
    }

    private static void openTitles(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        CompoundTag d = data(s, p.getUUID());
        List<String> mine = new ArrayList<>(sub(d, "titles").keySet());
        java.util.Collections.sort(mine);
        String now = d.getStringOr("title", "");
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("돌아가기", "첫 화면", new ItemStack(Items.BOOK)));
        e.add(new PickMenu.Entry("칭호 떼기", "이름 앞 칭호를 없애요", new ItemStack(Items.BARRIER)));
        for (String t : mine) e.add(new PickMenu.Entry("[" + t + "]", t.equals(now) ? "지금 달고 있어요" : "누르면 이름 앞에 달아요", new ItemStack(Items.NAME_TAG)));
        PickMenu.open(p, Component.literal("🎖 칭호"), mine.isEmpty() ? "아직 얻은 칭호가 없어요 · 업적과 도감 완성으로 얻어요" : mine.size() + "개 보유 · 누르면 바꿔요", "", e, 0, i -> {
            if (i == 0) {
                open(p);
                return;
            }
            String t = i == 1 ? "" : i - 2 < mine.size() ? mine.get(i - 2) : null;
            if (t == null) return;
            d.putString("title", t);
            ContentData.get(s).dirty();
            p.refreshDisplayName();
            p.refreshTabListName();
            p.sendOverlayMessage(Component.literal(t.isEmpty() ? "칭호를 뗐어요" : "칭호 [" + t + "] 을(를) 달았어요").withStyle(ChatFormatting.AQUA));
            openTitles(p);
        }, null, x -> true);
    }

    public static void debugPage(ServerPlayer p, int k) {
        if (k == 0) openCat(p, 0);
        else if (k == 1) openAch(p);
        else openTitles(p);
    }

    /** 화면 시험용: 몇 가지를 등록해 둔다 */
    public static void debug(ServerPlayer p) {
        for (String id : List.of("wildplus:tomato", "wildplus:chili", "wildplus:corn", "wildplus:ruby", "wildplus:bandit", "wildplus:blood_lord")) discover(p, id);
        add(p, "kill", 120);
        add(p, "fish", 3);
        open(p);
    }
}

package com.snolred.wildplus.content;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 3.5 마을 발전 프로젝트: 서버 모두가 함께 짓는 건물 5종 (도서관 → 놀이공원 → 항구 → 시계탑 광장 → 온천 차례로 열림).
 * 건물마다 3단계 (터 닦기 → 골조 → 완공). 단계마다 재료 2가지 + 돈을 모으면 다음 단계로 올라가고, 공사 부지 상자로 정한 자리에 그 단계 건물이 지어진다.
 * 완공 효과: 도서관 경험치 +10% · 놀이공원 금 · 토 행운 · 항구 낚시 등급 +15% · 시계탑 아침 소식 + /plaza · 온천 재생 · 배고픔 회복.
 */
public final class Village {
    private Village() {}

    public static final String[] IDS = {"library", "amusement", "harbor", "clock_tower", "hot_spring"};
    public static final String[] NAMES = {"도서관", "놀이공원", "항구", "시계탑 광장", "온천"};
    public static final String[] EFFECT = {"모두 경험치 +10%", "금 · 토요일 모두 행운", "낚시 등급 확률 +15%", "매일 아침 소식 · 날씨 예보 · /plaza 귀환", "들어가면 재생 · 배고픔 회복"};
    /** [건물][단계 0~2] = {재료1, 개수, 재료2, 개수, 돈} ("#" 로 시작하면 태그) */
    private static final Object[][][] NEED = {
            {{"minecraft:oak_planks", 256, "minecraft:stone_bricks", 128, com.snolred.wildplus.economy.Prices.x(50_000)}, {"minecraft:book", 64, "minecraft:glass", 64, com.snolred.wildplus.economy.Prices.x(100_000)},
                    {"minecraft:bookshelf", 32, "minecraft:lapis_lazuli", 64, com.snolred.wildplus.economy.Prices.x(150_000)}},
            {{"#minecraft:wool", 128, "minecraft:iron_ingot", 64, com.snolred.wildplus.economy.Prices.x(80_000)}, {"minecraft:gold_ingot", 32, "minecraft:redstone", 128, com.snolred.wildplus.economy.Prices.x(150_000)},
                    {"minecraft:diamond", 8, "minecraft:emerald", 32, com.snolred.wildplus.economy.Prices.x(250_000)}},
            {{"minecraft:birch_planks", 256, "#minecraft:logs", 64, com.snolred.wildplus.economy.Prices.x(60_000)}, {"#minecraft:fishes", 128, "minecraft:iron_ingot", 32, com.snolred.wildplus.economy.Prices.x(120_000)},
                    {"minecraft:pufferfish", 16, "minecraft:gold_ingot", 16, com.snolred.wildplus.economy.Prices.x(180_000)}},
            {{"minecraft:stone", 256, "minecraft:bricks", 128, com.snolred.wildplus.economy.Prices.x(50_000)}, {"minecraft:copper_ingot", 64, "minecraft:gold_ingot", 16, com.snolred.wildplus.economy.Prices.x(100_000)},
                    {"minecraft:clock", 4, "minecraft:redstone_block", 8, com.snolred.wildplus.economy.Prices.x(150_000)}},
            {{"minecraft:stone", 192, "minecraft:bamboo", 128, com.snolred.wildplus.economy.Prices.x(60_000)}, {"minecraft:water_bucket", 16, "minecraft:magma_block", 16, com.snolred.wildplus.economy.Prices.x(120_000)},
                    {"wildplus:milk_bottle", 32, "wildplus:ginseng", 4, com.snolred.wildplus.economy.Prices.x(200_000)}}};

    private static CompoundTag root(MinecraftServer s) {
        return ContentData.get(s).section("village");
    }

    private static CompoundTag proj(MinecraftServer s, String id) {
        CompoundTag r = root(s);
        CompoundTag p = r.getCompoundOrEmpty(id);
        r.put(id, p);
        return p;
    }

    public static int index(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }

    /** 0 = 시작 전, 1~2 = 공사 중 (지은 단계), 3 = 완공 */
    public static int stage(MinecraftServer s, String id) {
        return proj(s, id).getIntOr("stage", 0);
    }

    public static boolean done(MinecraftServer s, String id) {
        return s != null && stage(s, id) >= 3;
    }

    public static boolean unlocked(MinecraftServer s, int i) {
        return i == 0 || done(s, IDS[i - 1]);
    }

    // ───────── 재료 ─────────
    private static boolean matches(String req, ItemStack st) {
        if (st.isEmpty()) return false;
        if (req.startsWith("#")) return st.is(TagKey.create(Registries.ITEM, Identifier.parse(req.substring(1))));
        return BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(req);
    }

    /** 화면에 보일 대표 아이템 (태그면 첫 아이템) */
    private static String iconOf(String req) {
        if (!req.startsWith("#")) return req;
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(req.substring(1)));
        for (Item it : BuiltInRegistries.ITEM) if (new ItemStack(it).is(tag)) return BuiltInRegistries.ITEM.getKey(it).toString();
        return "minecraft:barrier";
    }

    private static String nameOf(String req) {
        if (req.equals("#minecraft:wool")) return "양털 (아무 색)";
        if (req.equals("#minecraft:logs")) return "원목 (아무 나무)";
        if (req.equals("#minecraft:fishes")) return "물고기 (아무거나)";
        Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(req));
        return new ItemStack(it).getHoverName().getString();
    }

    /** 지금 단계(올리려는 단계)의 필요한 것 */
    private static Object[] need(int i, int stage) {
        return NEED[i][Math.min(2, stage)];
    }

    // ───────── 기부 ─────────
    private static void contribute(ServerPlayer p, String id, long money, int items) {
        CompoundTag c = proj(p.level().getServer(), id).getCompoundOrEmpty("by");
        CompoundTag me = c.getCompoundOrEmpty(p.getUUID().toString());
        me.putString("name", p.getName().getString());
        me.putLong("money", me.getLongOr("money", 0) + money);
        me.putInt("items", me.getIntOr("items", 0) + items);
        c.put(p.getUUID().toString(), me);
        proj(p.level().getServer(), id).put("by", c);
    }

    private static void donateItems(ServerPlayer p, int i) {
        MinecraftServer s = p.level().getServer();
        String id = IDS[i];
        CompoundTag pr = proj(s, id);
        int st = pr.getIntOr("stage", 0);
        if (st >= 3) return;
        Object[] n = need(i, st);
        int given = 0;
        for (int k = 0; k < 2; k++) {
            String req = (String) n[k * 2];
            int want = (Integer) n[k * 2 + 1] - pr.getIntOr("m" + k, 0);
            if (want <= 0) continue;
            var inv = p.getInventory();
            for (int slot = 0; slot < inv.getContainerSize() && want > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (!matches(req, stack)) continue;
                int take = Math.min(want, stack.getCount());
                ItemStack back = stack.getItem() == net.minecraft.world.item.Items.WATER_BUCKET ? new ItemStack(net.minecraft.world.item.Items.BUCKET, take) : ItemStack.EMPTY;
                stack.shrink(take);
                if (!back.isEmpty() && !inv.add(back)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), back);
                want -= take;
                given += take;
                pr.putInt("m" + k, pr.getIntOr("m" + k, 0) + take);
            }
        }
        if (given == 0) {
            p.sendOverlayMessage(Component.literal("📦 지금 단계에 필요한 재료가 가방에 없어요").withStyle(ChatFormatting.YELLOW));
            return;
        }
        contribute(p, id, 0, given);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.9F);
        p.sendSystemMessage(Component.literal(String.format("📦 %s에 재료 %d개 기부! 고마워요", NAMES[i], given)).withStyle(ChatFormatting.GREEN));
        check(p, i);
    }

    private static void donateMoney(ServerPlayer p, int i, long amount) {
        MinecraftServer s = p.level().getServer();
        CompoundTag pr = proj(s, IDS[i]);
        int st = pr.getIntOr("stage", 0);
        if (st >= 3) return;
        long want = ((Integer) need(i, st)[4]) - pr.getLongOr("money", 0);
        long pay = Math.min(amount, Math.max(0, want));
        if (pay <= 0) {
            p.sendOverlayMessage(Component.literal("💰 이 단계 돈은 다 모였어요 · 재료를 기부해 주세요").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!Honor.pay(p, pay)) {
            p.sendOverlayMessage(Component.literal(String.format("돈이 모자라요 (%,d동)", pay)).withStyle(ChatFormatting.RED));
            return;
        }
        pr.putLong("money", pr.getLongOr("money", 0) + pay);
        contribute(p, IDS[i], pay, 0);
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        p.sendSystemMessage(Component.literal(String.format("💰 %s에 %,d동 기부! 고마워요", NAMES[i], pay)).withStyle(ChatFormatting.GOLD));
        check(p, i);
    }

    /** 다 모이면 다음 단계 */
    private static void check(ServerPlayer p, int i) {
        MinecraftServer s = p.level().getServer();
        CompoundTag pr = proj(s, IDS[i]);
        int st = pr.getIntOr("stage", 0);
        Object[] n = need(i, st);
        if (pr.getIntOr("m0", 0) < (Integer) n[1] || pr.getIntOr("m1", 0) < (Integer) n[3] || pr.getLongOr("money", 0) < (Integer) n[4]) {
            ContentData.get(s).dirty();
            return;
        }
        if (st == 0 && !pr.contains("site")) {
            pr.putBoolean("ready", true); // 부지를 정하면 바로 1단계
            ContentData.get(s).dirty();
            s.getPlayerList().broadcastSystemMessage(Component.literal("🏗 " + NAMES[i] + " 1단계 준비 끝! 게시판에서 [공사 부지 상자]를 받아 지을 곳에 놓아 주세요")
                    .withStyle(ChatFormatting.GOLD), false);
            return;
        }
        advance(s, i);
    }

    private static void advance(MinecraftServer s, int i) {
        CompoundTag pr = proj(s, IDS[i]);
        int st = Math.min(3, pr.getIntOr("stage", 0) + 1);
        pr.putInt("stage", st);
        pr.putInt("m0", 0);
        pr.putInt("m1", 0);
        pr.putLong("money", 0);
        pr.remove("ready");
        ContentData.get(s).dirty();
        build(s, i, st);
        String msg = st >= 3 ? "🎉 " + NAMES[i] + " 완공! 이제 모두에게 효과: " + EFFECT[i] + (i + 1 < IDS.length ? " · 다음 프로젝트 「" + NAMES[i + 1] + "」 시작!" : "")
                : "🏗 " + NAMES[i] + " " + st + "단계 " + (st == 1 ? "터 닦기" : "골조") + " 공사 완료! 다음 단계도 함께 모아요";
        s.getPlayerList().broadcastSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.level().playSound(null, p.blockPosition(), st >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7F, 1.0F);
        }
    }

    /** 부지에 그 단계 구조물 놓기 */
    private static void build(MinecraftServer s, int i, int st) {
        CompoundTag pr = proj(s, IDS[i]);
        if (!pr.contains("site")) return;
        ServerLevel lv = null;
        for (ServerLevel l : s.getAllLevels()) if (l.dimension().identifier().toString().equals(pr.getStringOr("dim", ""))) lv = l;
        if (lv == null) return;
        BlockPos origin = BlockPos.of(pr.getLongOr("site", 0));
        StructureTemplate t = lv.getStructureTemplateManager().get(WildPlus.id("village/" + IDS[i] + "_" + st)).orElse(null);
        if (t == null) return;
        StructurePlaceSettings set = new StructurePlaceSettings().setRotation(Rotation.values()[Math.floorMod(pr.getIntOr("rot", 0), 4)]).setRotationPivot(BlockPos.ZERO);
        t.placeInWorld(lv, origin, origin, set, lv.getRandom(), Block.UPDATE_ALL);
        var box = t.getBoundingBox(set, origin);
        com.snolred.wildplus.home.HouseKitItem.refillParts(lv, box);
        for (int k = 0; k < 30; k++) {
            lv.sendParticles(ParticleTypes.CLOUD, box.minX() + lv.getRandom().nextDouble() * box.getXSpan(), origin.getY() + 1 + lv.getRandom().nextDouble() * 4,
                    box.minZ() + lv.getRandom().nextDouble() * box.getZSpan(), 1, 0, 0.05, 0, 0.02);
        }
    }

    /** 공사 부지 상자를 놓았을 때 (VillageSiteItem): 자리 기억 · 준비됐으면 1단계 바로 짓기 */
    public static boolean setSite(ServerPlayer p, String id, BlockPos origin, Rotation rot) {
        MinecraftServer s = p.level().getServer();
        int i = index(id);
        CompoundTag pr = proj(s, id);
        if (i < 0 || pr.contains("site")) {
            p.sendSystemMessage(Component.literal("이 건물은 이미 부지가 정해져 있어요").withStyle(ChatFormatting.RED));
            return false;
        }
        pr.putLong("site", origin.asLong());
        pr.putInt("rot", rot.ordinal());
        pr.putString("dim", p.level().dimension().identifier().toString());
        ContentData.get(s).dirty();
        if (pr.getBooleanOr("ready", false)) advance(s, i);
        else p.sendSystemMessage(Component.literal("🏗 " + NAMES[i] + " 부지를 정했어요! 1단계 재료가 다 모이면 여기에 지어져요").withStyle(ChatFormatting.GOLD));
        return true;
    }

    // ───────── 완공 효과 ─────────
    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        if (event.getAmount() > 0 && event.getEntity().level().getServer() != null && done(event.getEntity().level().getServer(), "library")) {
            event.setAmount((int) Math.ceil(event.getAmount() * 1.1));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        long t = s.overworld().getOverworldClockTime();
        if (s.getTickCount() % 600 == 0 && done(s, "amusement")) {
            int wd = (int) Math.floorMod(t / 24000L, 7);
            if (wd == Contest.FRI || wd == Contest.SAT) {
                for (ServerPlayer p : s.getPlayerList().getPlayers()) p.addEffect(new MobEffectInstance(MobEffects.LUCK, 700, 0, true, false));
            }
        }
        if (s.getTickCount() % 40 == 0 && done(s, "hot_spring")) {
            CompoundTag pr = proj(s, "hot_spring");
            BlockPos c = BlockPos.of(pr.getLongOr("site", 0)).offset(5, 0, 7);
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                if (p.isInWater() && p.blockPosition().distSqr(c) < 64 && p.level().dimension().identifier().toString().equals(pr.getStringOr("dim", ""))) {
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
                    p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 0, true, false));
                    if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.2, p.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
                }
            }
        }
        if (s.getTickCount() % 20 == 0 && done(s, "clock_tower")) {
            long day = t / 24000L, tod = Math.floorMod(t, 24000L);
            CompoundTag pr = proj(s, "clock_tower");
            if (tod < 600 && pr.getLongOr("news_day", -1) != day) {
                pr.putLong("news_day", day);
                ContentData.get(s).dirty();
                ServerLevel lv = s.overworld();
                for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.literal("🕰 시계탑 아침 소식 — " + (day + 1) + "일차 (" + Contest.weekdayName(day) + "요일)").withStyle(ChatFormatting.GOLD));
                    p.sendSystemMessage(Component.literal("  날씨: " + (lv.isThundering() ? "⛈ 뇌우 · 밖은 위험해요" : lv.isRaining() ? "🌧 비 · 물고기가 잘 잡혀요" : "☀ 맑음 · 농사하기 좋은 날")
                            + " · " + Contest.headline(s)).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("plaza").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            MinecraftServer s = p.level().getServer();
            CompoundTag pr = proj(s, "clock_tower");
            if (!done(s, "clock_tower")) {
                ctx.getSource().sendFailure(Component.literal("시계탑 광장이 완공되면 쓸 수 있어요"));
                return 0;
            }
            for (ServerLevel l : s.getAllLevels()) {
                if (l.dimension().identifier().toString().equals(pr.getStringOr("dim", ""))) {
                    BlockPos o = BlockPos.of(pr.getLongOr("site", 0));
                    p.teleportTo(l, o.getX() + 7.5, o.getY() + 1, o.getZ() + 12.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), p.getYRot(), p.getXRot(), true);
                    return 1;
                }
            }
            return 0;
        }));
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("village").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(net.minecraft.commands.Commands.literal("advance").executes(ctx -> {
                    MinecraftServer s = ctx.getSource().getServer();
                    for (int i = 0; i < IDS.length; i++) {
                        if (!done(s, IDS[i])) {
                            CompoundTag pr = proj(s, IDS[i]);
                            if (pr.getIntOr("stage", 0) == 0 && !pr.contains("site")) pr.putBoolean("ready", true);
                            else advance(s, i);
                            break;
                        }
                    }
                    return 1;
                })));
    }

    // ───────── 화면 ─────────
    public static void open(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        List<PickMenu.Entry> rows = new ArrayList<>();
        for (int i = 0; i < IDS.length; i++) {
            CompoundTag pr = proj(s, IDS[i]);
            int st = pr.getIntOr("stage", 0);
            Object[] n = need(i, st);
            StringBuilder mats = new StringBuilder();
            for (int k = 0; k < 2; k++) {
                String req = (String) n[k * 2];
                mats.append(nameOf(req)).append('|').append(iconOf(req)).append('|').append(pr.getIntOr("m" + k, 0)).append('|').append(n[k * 2 + 1]).append(';');
            }
            mats.append("돈|minecraft:gold_ingot|").append(pr.getLongOr("money", 0)).append('|').append(n[4]);
            double frac = st >= 3 ? 1 : (Math.min(1.0, pr.getIntOr("m0", 0) / (double) (Integer) n[1]) + Math.min(1.0, pr.getIntOr("m1", 0) / (double) (Integer) n[3])
                    + Math.min(1.0, pr.getLongOr("money", 0) / (double) (Integer) n[4])) / 3;
            rows.add(new PickMenu.Entry(NAMES[i], String.join("\n", String.valueOf(st), unlocked(s, i) ? "1" : "0", String.valueOf((int) Math.round(frac * 100)),
                    EFFECT[i], mats.toString(), pr.contains("site") ? "1" : "0", pr.getBooleanOr("ready", false) ? "1" : "0"),
                    new ItemStack(com.snolred.wildplus.registry.ModItems18.SITES.get(IDS[i]).get())));
        }
        // 기여 순위 (모든 건물 합계)
        java.util.Map<String, long[]> sum = new java.util.HashMap<>();
        java.util.Map<String, String> names = new java.util.HashMap<>();
        for (String id : IDS) {
            CompoundTag by = proj(s, id).getCompoundOrEmpty("by");
            for (String u : by.keySet()) {
                CompoundTag me = by.getCompoundOrEmpty(u);
                long[] v = sum.computeIfAbsent(u, k -> new long[2]);
                v[0] += me.getLongOr("money", 0);
                v[1] += me.getIntOr("items", 0);
                names.put(u, me.getStringOr("name", "?"));
            }
        }
        sum.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0] + b.getValue()[1] * 200, a.getValue()[0] + a.getValue()[1] * 200)).limit(4)
                .forEach(e -> rows.add(new PickMenu.Entry(names.get(e.getKey()), String.format("%,d동 · 재료 %d", e.getValue()[0], e.getValue()[1]))));
        PickMenu.open(p, Component.literal("🏘 마을 발전 게시판"), "", "", rows, PickMenu.FLAG_VILLAGE, id -> {
            int i = id / 10, a = id % 10;
            if (i < 0 || i >= IDS.length || !unlocked(s, i)) return;
            switch (a) {
                case 0 -> donateItems(p, i);
                case 1 -> donateMoney(p, i, 10_000);
                case 2 -> donateMoney(p, i, 100_000);
                case 3 -> {
                    if (proj(s, IDS[i]).contains("site")) {
                        p.sendOverlayMessage(Component.literal("부지는 이미 정해졌어요").withStyle(ChatFormatting.GRAY));
                    } else {
                        ItemStack kit = new ItemStack(com.snolred.wildplus.registry.ModItems18.SITES.get(IDS[i]).get());
                        if (!p.getInventory().add(kit)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), kit);
                        p.sendSystemMessage(Component.literal("🏗 " + NAMES[i] + " 공사 부지 상자를 받았어요 · 지을 곳(15×15)을 보고 웅크리고 우클릭").withStyle(ChatFormatting.GOLD));
                    }
                }
                default -> {}
            }
            open(p, pos);
        }, null, pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }
}

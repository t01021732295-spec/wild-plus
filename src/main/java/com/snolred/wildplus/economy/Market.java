package com.snolred.wildplus.economy;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.content.Mail;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 4.8 거래소: 플레이어끼리 물건을 올리고 산다. 대금은 판 사람 통장으로(수수료 5%), 게임 속 7일 안 팔리면 우편으로 반환 */
public final class Market {
    private static final int MAX_PER_PLAYER = 20, DAYS = 7, FEE_PCT = 5, HEAD = 4;
    private static final long MAX_PRICE = 1_000_000_000L;
    private static final long[] STEPS = {100L, 1_000L, 10_000L, 100_000L, 1_000_000L};
    private static final Map<UUID, Long> PRICE = new HashMap<>();
    private static final Map<UUID, Boolean> BY_PRICE = new HashMap<>();

    private record Listing(String id, UUID seller, String name, long price, long day, ItemStack stack) {}

    private Market() {}

    private static CompoundTag items(MinecraftServer s) {
        return ContentData.get(s).section("market_items");
    }

    private static List<Listing> all(MinecraftServer s) {
        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<Listing> out = new ArrayList<>();
        CompoundTag it = items(s);
        for (String k : it.keySet()) {
            if (!(it.get(k) instanceof CompoundTag t) || t.get("item") == null) continue;
            ItemStack st = ItemStack.OPTIONAL_CODEC.parse(ops, t.get("item")).result().orElse(ItemStack.EMPTY);
            if (st.isEmpty()) continue;
            try {
                out.add(new Listing(k, UUID.fromString(t.getStringOr("u", "")), t.getStringOr("name", "?"), t.getLongOr("price", 0L), t.getLongOr("day", 0L), st));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.blockPosition().closerThan(pos, 10);
    }

    private static void say(ServerPlayer p, String s, ChatFormatting c) {
        p.sendOverlayMessage(Component.literal(s).withStyle(c));
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        boolean byPrice = BY_PRICE.getOrDefault(p.getUUID(), false);
        List<Listing> list = all(s);
        list.sort(byPrice ? Comparator.comparingLong(Listing::price) : Comparator.comparingLong((Listing l) -> -Long.parseLong(l.id())));
        List<PickMenu.Entry> e = new ArrayList<>();
        e.add(new PickMenu.Entry("판매 등록", "손에 든 물건을 올려요 · 수수료 5% · 최대 " + MAX_PER_PLAYER + "건", new ItemStack(Items.WRITABLE_BOOK)), PickMenu.K_SMALL));
        e.add(new PickMenu.Entry("내 판매 목록", "누르면 판매를 취소하고 돌려받아요", new ItemStack(Items.CHEST)), PickMenu.K_SMALL));
        e.add(new PickMenu.Entry("정렬: " + (byPrice ? "싼 순" : "최신순"), "누르면 바뀌어요", new ItemStack(Items.HOPPER)), PickMenu.K_SMALL));
        e.add(new PickMenu.Entry("일괄 출하", "가방의 팔 수 있는 물건을 한 번에", new ItemStack(Items.GOLD_INGOT)), PickMenu.K_SMALL));
        for (Listing l : list) e.add(entry(l, p, s));
        PickMenu.open(p, Component.literal("🏪 거래소"), list.size() + "건 판매 중 · 물건을 누르면 사요", "", e, PickMenu.FLAG_SHOP | PickMenu.FLAG_MARKET, i -> {
            switch (i) {
                case 0 -> startSell(p, pos);
                case 1 -> mine(p, pos);
                case 2 -> {
                    BY_PRICE.put(p.getUUID(), !byPrice);
                    open(p, pos);
                }
                case 3 -> BulkShip.open(p, pos);
                default -> {
                    if (i - HEAD < list.size()) buy(p, pos, list.get(i - HEAD));
                }
            }
        }, null, x -> near(p, pos));
    }

    private static PickMenu.Entry entry(Listing l, ServerPlayer viewer, MinecraftServer s) {
        long left = Math.max(0, l.day() + DAYS - ContentData.gameDay);
        String who = l.seller().equals(viewer.getUUID()) ? "내 물건" : "판매: " + l.name();
        return new PickMenu.Entry(l.stack().getHoverName().getString() + (l.stack().getCount() > 1 ? " ×" + l.stack().getCount() : ""),
                String.format("%,d동 · %s · %d일 남음", l.price(), who, left), l.stack().copy());
    }

    private static void buy(ServerPlayer p, BlockPos pos, Listing l) {
        MinecraftServer s = p.level().getServer();
        CompoundTag it = items(s);
        if (!it.contains(l.id())) {
            say(p, "방금 팔렸거나 내려간 물건이에요", ChatFormatting.RED);
            open(p, pos);
            return;
        }
        if (l.seller().equals(p.getUUID())) {
            it.remove(l.id());
            ContentData.get(s).dirty();
            give(p, l.stack().copy());
            say(p, "판매를 취소하고 돌려받았어요", ChatFormatting.YELLOW);
            open(p, pos);
            return;
        }
        if (!Coins.pay(p, l.price())) {
            say(p, String.format("%,d동이 필요해요", l.price()), ChatFormatting.RED);
            return;
        }
        it.remove(l.id());
        give(p, l.stack().copy());
        long net = l.price() - l.price() * FEE_PCT / 100;
        WildData wd = WildData.get(s);
        wd.reason = "거래소 판매";
        wd.addBalance(l.seller(), net);
        String what = l.stack().getHoverName().getString() + (l.stack().getCount() > 1 ? " ×" + l.stack().getCount() : "");
        ServerPlayer seller = s.getPlayerList().getPlayer(l.seller());
        if (seller != null) {
            seller.sendSystemMessage(Component.literal(String.format("🏪 %s 님이 %s 을(를) 샀어요 · +%,d동 (수수료 %,d동)", p.getName().getString(), what, net, l.price() - net)).withStyle(ChatFormatting.GOLD));
        } else {
            CompoundTag n = ContentData.get(s).player("market_note", l.seller());
            n.putInt("n", n.getIntOr("n", 0) + 1);
            n.putLong("sum", n.getLongOr("sum", 0L) + net);
        }
        ContentData.get(s).dirty();
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        p.sendSystemMessage(Component.literal(String.format("🏪 %s 구매 · %,d동", what, l.price())).withStyle(ChatFormatting.GREEN));
        open(p, pos);
    }

    private static void mine(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.level().getServer();
        List<Listing> list = all(s).stream().filter(l -> l.seller().equals(p.getUUID())).toList();
        List<PickMenu.Entry> e = new ArrayList<>();
        for (Listing l : list) e.add(entry(l, p, s));
        PickMenu.open(p, Component.literal("내 판매 목록"), list.size() + "/" + MAX_PER_PLAYER + "건 · 누르면 취소", "올린 물건이 없어요", e, PickMenu.FLAG_SHOP | PickMenu.FLAG_MARKET, i -> {
            if (i >= 0 && i < list.size()) buy(p, pos, list.get(i));
        }, null, x -> near(p, pos));
    }

    private static void startSell(ServerPlayer p, BlockPos pos) {
        ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty() || Coins.unitValue(hand) > 0 || Coins.isPouch(hand)) {
            say(p, "팔 물건을 손에 들고 눌러 주세요", ChatFormatting.RED);
            return;
        }
        long count = all(p.level().getServer()).stream().filter(l -> l.seller().equals(p.getUUID())).count();
        if (count >= MAX_PER_PLAYER) {
            say(p, "한 사람당 " + MAX_PER_PLAYER + "건까지 올릴 수 있어요", ChatFormatting.RED);
            return;
        }
        PRICE.put(p.getUUID(), 0L);
        priceMenu(p, pos);
    }

    private static void priceMenu(ServerPlayer p, BlockPos pos) {
        long price = PRICE.getOrDefault(p.getUUID(), 0L);
        ItemStack hand = p.getMainHandItem();
        List<PickMenu.Entry> e = new ArrayList<>();
        String[] label = {"+100", "+1천", "+1만", "+10만", "+100만"};
        ItemStack[] icon = {new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD)};
        for (int i = 0; i < STEPS.length; i++) e.add(new PickMenu.Entry(label[i], "가격 올리기", icon[i]));
        e.add(new PickMenu.Entry("0으로", "가격 초기화", new ItemStack(Items.BARRIER)));
        e.add(new PickMenu.Entry(price > 0 ? "이 가격에 등록" : "가격을 정해 주세요", String.format("%,d동 · 팔리면 %,d동 입금", price, price - price * FEE_PCT / 100), new ItemStack(price > 0 ? Items.EMERALD_BLOCK : Items.STONE)));
        String what = hand.isEmpty() ? "(손이 비었어요)" : hand.getHoverName().getString() + " ×" + hand.getCount();
        PickMenu.open(p, Component.literal("판매 등록"), what + " · " + String.format("%,d동", price), "", e, PickMenu.FLAG_SHOP | PickMenu.FLAG_MARKET, i -> {
            if (i < STEPS.length) {
                PRICE.put(p.getUUID(), Math.min(MAX_PRICE, price + STEPS[i]));
                priceMenu(p, pos);
            } else if (i == STEPS.length) {
                PRICE.put(p.getUUID(), 0L);
                priceMenu(p, pos);
            } else if (price > 0) {
                register(p, pos, price);
            }
        }, null, x -> near(p, pos));
    }

    private static void register(ServerPlayer p, BlockPos pos, long price) {
        ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty() || Coins.unitValue(hand) > 0 || Coins.isPouch(hand)) {
            say(p, "팔 물건을 손에 들고 있어야 해요", ChatFormatting.RED);
            return;
        }
        MinecraftServer s = p.level().getServer();
        ContentData d = ContentData.get(s);
        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var tag = ItemStack.OPTIONAL_CODEC.encodeStart(ops, hand).result().orElse(null);
        if (tag == null) return;
        CompoundTag meta = d.section("market");
        int seq = meta.getIntOr("seq", 0) + 1;
        meta.putInt("seq", seq);
        CompoundTag t = new CompoundTag();
        t.putString("u", p.getUUID().toString());
        t.putString("name", p.getName().getString());
        t.putLong("price", price);
        t.putLong("day", ContentData.gameDay);
        t.put("item", tag);
        items(s).put(Integer.toString(seq), t);
        d.dirty();
        String what = hand.getHoverName().getString() + " ×" + hand.getCount();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PRICE.remove(p.getUUID());
        p.sendSystemMessage(Component.literal(String.format("🏪 거래소에 올렸어요: %s · %,d동", what, price)).withStyle(ChatFormatting.GOLD));
        open(p, pos);
    }

    /** 기간이 지난 물건은 우편으로 돌려준다 (우편함이 꽉 차 있으면 다음에) */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 600 != 41) return;
        for (Listing l : all(s)) {
            if (ContentData.gameDay < l.day() + DAYS) continue;
            if (!Mail.put(s, l.seller(), List.of(l.stack().copy())).isEmpty()) continue;
            items(s).remove(l.id());
            ContentData.get(s).dirty();
            ServerPlayer on = s.getPlayerList().getPlayer(l.seller());
            if (on != null) on.sendSystemMessage(Component.literal("🏪 거래소에서 안 팔린 " + l.stack().getHoverName().getString() + " 이(가) 우편으로 돌아왔어요").withStyle(ChatFormatting.YELLOW));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag n = ContentData.get(p.level().getServer()).player("market_note", p.getUUID());
        int c = n.getIntOr("n", 0);
        if (c <= 0) return;
        p.sendSystemMessage(Component.literal(String.format("🏪 자리를 비운 사이 거래소에서 %d건이 팔렸어요 · +%,d동 (통장 입금)", c, n.getLongOr("sum", 0L))).withStyle(ChatFormatting.GOLD));
        n.putInt("n", 0);
        n.putLong("sum", 0L);
        ContentData.get(p.level().getServer()).dirty();
    }
}

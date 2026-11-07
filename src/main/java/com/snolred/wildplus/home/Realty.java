package com.snolred.wildplus.home;

import com.snolred.wildplus.content.Land;
import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModItems16;
import com.snolred.wildplus.registry.ModItems17;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 3.4 부동산 중개소: 진짜 부동산처럼 매물 카드 목록 → 오른쪽 상세 정보 → [계약하기] → [서명] 으로 산다 (RealtyScreen).
 * 매물: 사무소 둘레 땅 9필지 (청크) · 신축 분양 집 6종 · 목장 건물 2종 · 내 부동산 관리.
 * 카드 글(sub) 형식: "가격\n꼬리표\n상세 1\n상세 2 …"
 */
public final class Realty {
    private Realty() {}

    private static final String[] DIR = {"북서", "북", "북동", "서", "중개소 자리", "동", "남서", "남", "남동"};

    private record Listing(PickMenu.Entry entry, Runnable action) {}

    public static void open(ServerPlayer p, BlockPos office) {
        MinecraftServer s = p.level().getServer();
        UUID me = p.getUUID();
        BlockPos base = office == null ? p.blockPosition() : office;
        List<Listing> list = new ArrayList<>();
        int mine = Land.owned(me);
        long day = s.overworld().getOverworldClockTime() / 24000L + 1;
        list.add(new Listing(new PickMenu.Entry("📂 내 부동산 관리", String.format("내 땅 %d필지\n관리\n재산세 하루 %,d동 (필지당 5,000동)\n출입 · 블록 · 상자 권한 설정\n땅 친구 추가: /land trust 이름\n여기서 열면 내 땅 목록과 설정이 나와요",
                mine, mine * Land.UPKEEP), new ItemStack(Items.WRITABLE_BOOK), PickMenu.K_SMALL), () -> Land.open(p)));
        // 땅 9필지 (중개소 둘레 3×3 청크)
        int cx = base.getX() >> 4, cz = base.getZ() >> 4;
        for (int dz = -1, k = 0; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++, k++) {
                BlockPos at = new BlockPos(((cx + dx) << 4) + 8, base.getY(), ((cz + dz) << 4) + 8);
                UUID o = Land.ownerAt(p.level(), at);
                String where = DIR[k] + " 땅 (" + (cx + dx) + ", " + (cz + dz) + ")";
                String detail = String.format("위치: 중개소 %s쪽 · X %d~%d · Z %d~%d\n크기: 16×16 (위아래 전부)\n재산세: 하루 5,000동\n폭발 · 남의 출입 막기 · 친구 권한",
                        DIR[k].equals("중개소 자리") ? "바로 이" : DIR[k], (cx + dx) << 4, ((cx + dx) << 4) + 15, (cz + dz) << 4, ((cz + dz) << 4) + 15);
                if (o == null) {
                    long price = Land.nextPrice(me);
                    list.add(new Listing(new PickMenu.Entry("🌿 " + where, String.format("%,d동\n땅 · 매매\n%s\n%d번째 땅이라 이 값 (땅이 늘수록 +60만)", price, detail, mine + 1),
                            new ItemStack(Items.GRASS_BLOCK), PickMenu.K_PRIMARY), () -> contract(p, Land.buyAt(p, at), where, price, day)));
                } else if (o.equals(me)) {
                    list.add(new Listing(new PickMenu.Entry("🏡 " + where, String.format("내 땅\n내 땅 · 내놓기\n%s\n내놓으면 %,d동을 돌려받아요", detail, Land.REFUND),
                            new ItemStack(Items.OAK_SIGN), PickMenu.K_DANGER), () -> Land.sellAt(p, at)));
                } else {
                    list.add(new Listing(new PickMenu.Entry("🔒 " + where, "계약 완료\n" + Land.ownerNameOf(s, o) + " 소유\n" + detail + "\n이미 주인이 있는 땅이에요",
                            new ItemStack(Items.PODZOL), PickMenu.K_INFO), () -> {}));
                }
            }
        }
        // 신축 분양: 집 6종
        ModItems16.HOUSES.forEach((id, it) -> {
            long price = com.snolred.wildplus.economy.Prices.x(HomeShop.HOUSE_PRICE.getOrDefault(id, 100_000L));
            String name = new ItemStack(it.get()).getHoverName().getString().replace(" 상자", "");
            String[] sub = HomeShop.HOUSE_SUB.getOrDefault(id, "").split(" · ", 3);
            list.add(new Listing(new PickMenu.Entry("🏠 " + name, String.format("%,d동\n집 · 신축 분양\n크기: %s\n구성: %s\n특징: %s\n열쇠(집 상자)를 드려요 · 원하는 땅에 웅크리고 우클릭하면 완공\n내 땅에 지으면 남이 못 부숴요",
                    price, sub.length > 0 ? sub[0] : "", sub.length > 1 ? sub[1] : "", sub.length > 2 ? sub[2] : ""), new ItemStack(it.get()), PickMenu.K_PRIMARY),
                    () -> contract(p, pay(p, price, it.get()), name, price, day)));
        });
        // 목장 건물 2종
        Object[][] farms = {{"🏚 큰 축사", 150_000L, ModItems17.BARN_KIT.get(), "13×15 · 소·양 우리 2칸 · 여물통 · 건초 다락 · 울타리 마당"},
                {"🐔 닭장", 40_000L, ModItems17.COOP_KIT.get(), "7×9 · 둥지 상자 2 · 물그릇 · 철망 마당"},
                {"🏇 경마장", 300_000L, com.snolred.wildplus.registry.ModItems18.RACETRACK_KIT.get(), "37×41 · 흙 타원 주로 · 관중석 · 출발 게이트 · 전광판 · 마구간 4 · 마권 발매기 2"}};
        for (Object[] f : farms) {
            long price = com.snolred.wildplus.economy.Prices.x((Long) f[1]);
            Item item = (Item) f[2];
            String name = ((String) f[0]).substring(3);
            list.add(new Listing(new PickMenu.Entry((String) f[0], String.format("%,d동\n목장 · 분양\n%s\n열쇠(건물 상자)를 드려요\n가축은 가축 상점에서", price, f[3]),
                    new ItemStack(item), PickMenu.K_PRIMARY), () -> contract(p, pay(p, price, item), name, price, day)));
        }
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (Listing l : list) entries.add(l.entry());
        String header = String.format("담당 중개사: 스놀레드 공인중개사 · %d일차 · 내 땅 %d필지 · 다음 땅 %,d동", day, mine, Land.nextPrice(me));
        PickMenu.open(p, Component.literal("🏢 야생 공인중개사 사무소"), header, "매물이 없어요", entries, PickMenu.FLAG_REALTY, i -> {
            if (i < 0 || i >= list.size()) return;
            list.get(i).action().run();
            if (i != 0) open(p, office);
        }, null, pl -> office == null || pl.distanceToSqr(office.getX() + 0.5, office.getY() + 0.5, office.getZ() + 0.5) < 64.0);
    }

    private static boolean pay(ServerPlayer p, long price, Item item) {
        if (!Honor.pay(p, price)) {
            p.sendSystemMessage(Component.literal(String.format("돈이 모자라요 (%,d동 · 통장에서 먼저 빠져요)", price)).withStyle(ChatFormatting.RED));
            return false;
        }
        ItemStack s = new ItemStack(item);
        if (!p.getInventory().add(s)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
        return true;
    }

    /** 계약서 (채팅) + 도장 소리 */
    private static void contract(ServerPlayer p, boolean ok, String what, long price, long day) {
        if (!ok) return;
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.7F, 1.1F);
        p.sendSystemMessage(Component.literal("━━━━━ 📝 부동산 매매 계약서 ━━━━━").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal(String.format("물건: %s · 금액: %,d동 (일시불)", what, price)).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(Component.literal(String.format("매도인: 야생 공인중개사 · 매수인: %s · 계약일: %d일차", p.getName().getString(), day)).withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal("✍ 서명 완료 · 🔴 인 — 축하해요! 좋은 집 되세요").withStyle(ChatFormatting.GREEN));
    }
}

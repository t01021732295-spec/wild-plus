package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.menu.HallMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 명예의 전당: 대리석 + 금테 액자.
 * 위 왼쪽: 내 등급 메달 · 지금 혜택 · 다음 등급 혜택 · 등급 올리기 / 위 오른쪽: 부자 순위
 * 아래: 특별 구매 8가지 (2열 × 4줄)
 */
public class HallScreen extends AbstractContainerScreen<HallMenu> {
    private static final int W = 320, H = 230;
    private static final int GOLD = 0xFFD9A22E, INK = 0xFF2A2218, SOFT = 0xFF6E6252;
    private static final int SPLIT = 196, BOTTOM = 140;
    private UiButton rankBtn, flightBtn;

    public HallScreen(HallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        rankBtn = addRenderableWidget(UiButton.of(x + 10, y + 116, SPLIT - 18, 18, "", UiButton.GOLD, () -> click(Honor.BTN_RANK)));
        UiButton.Style purple = new UiButton.Style(0xFFE07AF0, 0xFFA83CC0, 0xFF4A1060, 0xFFFFFFFF, true, UiButton.Shape.SOFT);
        flightBtn = buy(0, "", UiButton.BLUE, Honor.BTN_FLIGHT);
        buy(1, String.format("완전 회복 · %,d동", Honor.HEAL_COST), UiButton.GREEN, Honor.BTN_HEAL);
        buy(2, String.format("경험치 5레벨 · %,d동", Honor.XP_COST), UiButton.TEAL, Honor.BTN_XP);
        buy(3, String.format("상인 호출 · %,d동", Honor.MERCHANT_COST), UiButton.ORANGE, Honor.BTN_MERCHANT);
        buy(4, String.format("보물 지도 · %,d동", Honor.MAP_COST), UiButton.GOLD, Honor.BTN_MAP);
        buy(5, String.format("수호의 축복 · %,d동", Honor.GUARD_COST), UiButton.STEEL, Honor.BTN_GUARD);
        buy(6, String.format("보스 소환권 · %,d동", Honor.BOSS_COST), UiButton.RED, Honor.BTN_BOSS);
        buy(7, String.format("서버 축제 · %,d동", Honor.FESTIVAL_COST), purple, Honor.BTN_FESTIVAL);
        refresh();
    }

    /** 특별 구매 버튼: 2열 × 4줄 */
    private UiButton buy(int i, String label, UiButton.Style style, int id) {
        int col = i % 2, row = i / 2;
        return addRenderableWidget(UiButton.of(leftPos + 10 + col * 152, topPos + BOTTOM + 14 + row * 18, 148, 16, label, style, () -> click(id)));
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void refresh() {
        int r = menu.rank();
        if (r >= 5) {
            rankBtn.setMessage(Component.literal("최고 등급 달성!"));
            rankBtn.active = false;
        } else {
            rankBtn.setMessage(Component.literal(String.format("%s 등급 올리기 · %,d동", Honor.RANK_NAME[r + 1], Honor.RANK_COST[r])));
            rankBtn.active = menu.wealth() >= Honor.RANK_COST[r];
        }
        flightBtn.setMessage(Component.literal(String.format("비행 30분 · %,d동", Honor.flightCost(r))));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 금테 액자 + 대리석
        UiKit.device(g, x, y, W, H, 0xFFE8B84A, 0xFF9A6A14);
        UiKit.round(g, x + 4, y + 4, W - 8, H - 8, 2, 0xFF6A4A0A);
        g.fillGradient(x + 5, y + 5, x + W - 5, y + H - 5, 0xFFF8F5EE, 0xFFE8E2D6);
        UiKit.speckle(g, x + 5, y + 5, W - 10, H - 10, 41, 260, 0x14806A50, 0x10FFFFFF);
        for (int i = 0; i < 6; i++) {
            int sx = x + 20 + i * 52;
            for (int k = 0; k < 40; k++) g.fill(sx + k, y + 20 + k * 5, sx + k + 1, y + 22 + k * 5, 0x0E5A4A30);
        }
        // 머리 띠
        g.fillGradient(x + 5, y + 5, x + W - 5, y + 21, 0xFF2A2218, 0xFF14100A);
        UiKit.hline(g, x + 5, y + 21, W - 10, GOLD);
        // 내 등급 메달
        int r = menu.rank();
        int c = Honor.RANK_COLOR[r];
        UiKit.circle(g, x + 11, y + 27, 30, UiKit.darker(c, 0.45F));
        UiKit.circle(g, x + 13, y + 29, 26, c);
        UiKit.circle(g, x + 17, y + 33, 18, UiKit.lighter(c, 0.25F));
        g.fill(x + 20, y + 57, x + 24, y + 64, 0xFFB01822);
        g.fill(x + 28, y + 57, x + 32, y + 64, 0xFF1E4FB8);
        // 등급 단계 막대
        for (int i = 1; i <= 5; i++) {
            UiKit.round(g, x + 10 + (i - 1) * 36, y + 108, 34, 6, 2, i <= r ? Honor.RANK_COLOR[i] : 0xFFD8D2C6);
        }
        // 구분선
        UiKit.hline(g, x + 10, y + BOTTOM, W - 20, 0xFFCFC4AE);
        g.fill(x + SPLIT, y + 26, x + SPLIT + 1, y + BOTTOM - 4, 0xFFCFC4AE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "★ " + this.title.getString(), 10, 9, 0xFFF2C14E);
        UiKit.right(g, String.format("국고 %,d동", menu.treasury()), W - 10, 9, 0xFFBFAF8A);
        int r = menu.rank();
        UiKit.center(g, r == 0 ? "-" : String.valueOf(r), 26, 37, INK);
        UiKit.text(g, "내 명예 등급", 46, 26, SOFT);
        UiKit.bold(g, r == 0 ? "평민" : Honor.RANK_NAME[r] + " 시민", 46, 37, r == 0 ? INK : UiKit.darker(Honor.RANK_COLOR[r], 0.35F));
        UiKit.text(g, String.format("통장+현금 %,d동", menu.wealth()), 46, 49, SOFT);
        // 지금 혜택
        UiKit.text(g, r == 0 ? "혜택 없음" : String.format("하트+%d · 세금-%d%% · 이자+%.1f%%", 2 * r, Honor.taxCut(r), 0.2 * r), 46, 61, 0xFF1E7A2E);
        int ly = 69;
        for (int i = 1; i <= r && i <= 5; i++) {
            if (i <= 3) {
                UiKit.text(g, "✔ " + Honor.PERK[i], 10 + ((i - 1) % 2) * 92, ly + ((i - 1) / 2) * 10, 0xFF1E7A2E);
            }
        }
        if (r >= 4) UiKit.text(g, "✔ " + Honor.PERK[4], 10, ly + 20, 0xFF1E7A2E);
        if (r >= 5) UiKit.text(g, "✔ 신속 I · 피해-10%", 102, ly + 20, 0xFF1E7A2E);
        // 다음 등급
        if (r < 5) {
            String next = "다음 " + Honor.RANK_NAME[r + 1] + ": " + Honor.PERK[r + 1];
            if (UiKit.font().width(next) > SPLIT - 16) next = next.substring(0, Math.min(next.length(), 22)) + "…";
            UiKit.text(g, next, 10, 98, 0xFFB07A10);
        }
        // 특별 구매
        UiKit.bold(g, "특별 구매", 10, BOTTOM + 4, INK);
        String status = (menu.flying() ? "비행 " + menu.flightMinutes() + "분 남음" : "")
                + (menu.festivalMinutes() > 0 ? (menu.flying() ? " · " : "") + "축제 " + menu.festivalMinutes() + "분" : "");
        UiKit.right(g, status, W - 10, BOTTOM + 4, 0xFF2563D9);
        // 부자 순위
        UiKit.bold(g, "부자 순위", SPLIT + 8, 26, INK);
        for (int i = 0; i < Math.min(5, menu.ranking.size()); i++) {
            Honor.Entry e = menu.ranking.get(i);
            int ry = 40 + i * 19;
            UiKit.text(g, (i + 1) + ".", SPLIT + 8, ry, i < 3 ? 0xFFB07A10 : SOFT);
            int nx = SPLIT + 20;
            if (e.rank() > 0) {
                g.text(UiKit.font(), Honor.title(e.rank()), nx, ry, 0xFFFFFFFF, false);
                nx += UiKit.font().width(Honor.title(e.rank())) + 3;
            }
            String n = e.name();
            while (n.length() > 3 && nx + UiKit.font().width(n) > W - 10) n = n.substring(0, n.length() - 1);
            UiKit.text(g, n, nx, ry, INK);
            UiKit.right(g, String.format("%,d동", e.wealth()), W - 10, ry + 9, 0xFF1E7A2E);
        }
        if (menu.ranking.isEmpty()) UiKit.text(g, "아직 기록이 없어요", SPLIT + 8, 40, SOFT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

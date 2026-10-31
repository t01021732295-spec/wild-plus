package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.menu.HallMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 명예의 전당: 대리석 + 금테 액자. 왼쪽 위 내 등급 메달과 혜택, 왼쪽 아래 특별 구매 3개, 오른쪽 부자 순위 시상대.
 */
public class HallScreen extends AbstractContainerScreen<HallMenu> {
    private static final int W = 300, H = 214;
    private static final int GOLD = 0xFFD9A22E, INK = 0xFF2A2218, SOFT = 0xFF6E6252;
    private UiButton rankBtn, flightBtn;

    public HallScreen(HallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        rankBtn = addRenderableWidget(UiButton.of(x + 12, y + 104, 166, 18, "", UiButton.GOLD, () -> click(Honor.BTN_RANK)));
        flightBtn = addRenderableWidget(UiButton.of(x + 12, y + 140, 166, 16, "", UiButton.BLUE, () -> click(Honor.BTN_FLIGHT)));
        addRenderableWidget(UiButton.of(x + 12, y + 160, 166, 16, String.format("보스 소환권 · %,d동", Honor.BOSS_COST), UiButton.RED,
                () -> click(Honor.BTN_BOSS)));
        addRenderableWidget(UiButton.of(x + 12, y + 180, 166, 16, String.format("서버 축제 후원 · %,d동", Honor.FESTIVAL_COST),
                new UiButton.Style(0xFFE07AF0, 0xFFA83CC0, 0xFF4A1060, 0xFFFFFFFF, true, UiButton.Shape.SOFT), () -> click(Honor.BTN_FESTIVAL)));
        refresh();
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
        flightBtn.setMessage(Component.literal(String.format("비행 허가증 30분 · %,d동", Honor.flightCost(r))));
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
        // 대리석 결 (사선)
        for (int i = 0; i < 6; i++) {
            int sx = x + 20 + i * 48;
            for (int k = 0; k < 40; k++) g.fill(sx + k, y + 20 + k * 4, sx + k + 1, y + 22 + k * 4, 0x0E5A4A30);
        }
        // 머리 띠
        g.fillGradient(x + 5, y + 5, x + W - 5, y + 21, 0xFF2A2218, 0xFF14100A);
        UiKit.hline(g, x + 5, y + 21, W - 10, GOLD);
        // 내 등급 메달
        int r = menu.rank();
        int c = Honor.RANK_COLOR[r];
        UiKit.circle(g, x + 13, y + 29, 30, UiKit.darker(c, 0.45F));
        UiKit.circle(g, x + 15, y + 31, 26, c);
        UiKit.circle(g, x + 19, y + 35, 18, UiKit.lighter(c, 0.25F));
        g.fill(x + 22, y + 59, x + 26, y + 66, 0xFFB01822);
        g.fill(x + 30, y + 59, x + 34, y + 66, 0xFF1E4FB8);
        // 등급 단계 막대
        for (int i = 1; i <= 5; i++) {
            int bx = x + 50 + (i - 1) * 26;
            UiKit.round(g, bx, y + 84, 24, 6, 2, i <= r ? Honor.RANK_COLOR[i] : 0xFFD8D2C6);
        }
        // 구분선
        UiKit.hline(g, x + 10, y + 128, 172, 0xFFCFC4AE);
        g.fill(x + 186, y + 26, x + 187, y + H - 10, 0xFFCFC4AE);
        // 순위 시상대 (1·2·3위 받침)
        int px = x + 196;
        UiKit.round(g, px + 32, y + 64, 30, 24, 1, 0xFFE8B84A);
        UiKit.round(g, px, y + 72, 30, 16, 1, 0xFFC0C8D0);
        UiKit.round(g, px + 64, y + 78, 30, 10, 1, 0xFFCD7F32);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "★ " + this.title.getString(), 10, 9, 0xFFF2C14E);
        UiKit.right(g, String.format("국고 %,d동", menu.treasury()), W - 10, 9, 0xFFBFAF8A);
        int r = menu.rank();
        UiKit.center(g, r == 0 ? "-" : String.valueOf(r), 28, 39, INK);
        UiKit.text(g, "내 명예 등급", 50, 28, SOFT);
        UiKit.bold(g, r == 0 ? "평민" : Honor.RANK_NAME[r] + " 시민", 50, 39, r == 0 ? INK : UiKit.darker(Honor.RANK_COLOR[r], 0.35F));
        UiKit.text(g, String.format("가진 돈 (통장+현금) %,d동", menu.wealth()), 50, 52, SOFT);
        UiKit.text(g, r == 0 ? "혜택 없음" : String.format("하트 +%d · 세금 -%d%% · 이자 +%.1f%%", 2 * r, Honor.taxCut(r), 0.2 * r), 50, 66,
                0xFF1E7A2E);
        if (r >= 5) UiKit.text(g, "레전드: 비행 허가증 반값", 50, 75, 0xFFB03CC0);
        UiKit.text(g, "등급마다 하트+2 · 세금-10% · 이자+0.2%p", 12, 94, SOFT);
        UiKit.bold(g, "특별 구매", 12, 131, INK);
        String status = (menu.flying() ? "비행 " + menu.flightMinutes() + "분 남음" : "")
                + (menu.festivalMinutes() > 0 ? (menu.flying() ? " · " : "") + "축제 " + menu.festivalMinutes() + "분" : "");
        UiKit.right(g, status, 178, 131, 0xFF2563D9);
        // 순위
        UiKit.bold(g, "부자 순위", 196, 28, INK);
        UiKit.text(g, "통장+현금", 248, 28, SOFT);
        int[] order = {1, 0, 2};
        for (int k = 0; k < 3 && k < menu.ranking.size(); k++) {
            int idx = order[k];
            if (idx >= menu.ranking.size()) continue;
            int cx = 196 + k * 32 + 15;
            int ty = idx == 0 ? 54 : idx == 1 ? 62 : 68;
            UiKit.center(g, String.valueOf(idx + 1), cx, ty + (idx == 0 ? 14 : idx == 1 ? 12 : 12), 0xFFFFFFFF);
            String n = menu.ranking.get(idx).name();
            if (UiKit.font().width(n) > 30) n = n.substring(0, Math.min(n.length(), 4)) + "…";
            UiKit.center(g, n, cx, ty, INK);
        }
        for (int i = 0; i < Math.min(5, menu.ranking.size()); i++) {
            Honor.Entry e = menu.ranking.get(i);
            int ry = 96 + i * 20;
            UiKit.text(g, (i + 1) + ".", 196, ry, i < 3 ? 0xFFB07A10 : SOFT);
            g.text(UiKit.font(), e.rank() > 0 ? Honor.title(e.rank()) : Component.literal(""), 208, ry, 0xFFFFFFFF, false);
            UiKit.text(g, e.name(), 208 + (e.rank() > 0 ? UiKit.font().width(Honor.title(e.rank())) + 3 : 0), ry, INK);
            UiKit.right(g, String.format("%,d동", e.wealth()), W - 10, ry + 9, 0xFF1E7A2E);
        }
        if (menu.ranking.isEmpty()) UiKit.text(g, "아직 기록이 없어요", 196, 96, SOFT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

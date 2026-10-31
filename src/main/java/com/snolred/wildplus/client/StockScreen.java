package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.StockMarket;
import com.snolred.wildplus.menu.StockMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 증권 단말기: 어두운 증권 앱(HTS) 화면.
 * 한국식 색 - 오르면 빨강, 내리면 파랑. 매수 버튼은 빨강, 매도 버튼은 파랑.
 */
public class StockScreen extends AbstractContainerScreen<StockMenu> {
    private static final int W = 316, H = 250, ROW = 18, TOP = 34;
    private static final int BG = 0xFF0E131A, BAR = 0xFF161E29, ROW_ALT = 0xFF131A23, MUTED = 0xFF7D8A9C, FLAT = 0xFFB8C2CF;
    private static final UiButton.Style BUY = new UiButton.Style(0xFFFF5A5F, 0xFFD9363C, 0xFF6E1418, 0xFFFFFFFF, true, UiButton.Shape.SOFT);
    private static final UiButton.Style SELL = new UiButton.Style(0xFF4F95FF, 0xFF2B6FE0, 0xFF103C88, 0xFFFFFFFF, true, UiButton.Shape.SOFT);

    public StockScreen(StockMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        String[] labels = {"+1", "+10", "-1", "전부"};
        int[] xs = {206, 228, 254, 276}, ws = {20, 24, 20, 32};
        for (int i = 0; i < StockMenu.N; i++) {
            for (int a = 0; a < 4; a++) {
                int id = i * 10 + a;
                addRenderableWidget(UiButton.of(leftPos + xs[a], topPos + TOP + i * ROW + 1, ws[a], 15, labels[a], a < 2 ? BUY : SELL,
                        () -> click(id)));
            }
        }
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 모니터 테두리 + 앱 바탕
        UiKit.device(g, x, y, W, H, 0xFF2A2F37, 0xFF15181D);
        g.fill(x + 3, y + 3, x + W - 3, y + H - 3, BG);
        // 앱 상단바
        g.fill(x + 3, y + 3, x + W - 3, y + 17, BAR);
        UiKit.round(g, x + 6, y + 5, 10, 10, 2, 0xFFE5484D);
        g.fill(x + 8, y + 11, x + 9, y + 13, 0xFFFFFFFF);
        g.fill(x + 10, y + 9, x + 11, y + 13, 0xFFFFFFFF);
        g.fill(x + 12, y + 7, x + 13, y + 13, 0xFFFFFFFF);
        boolean live = menu.live();
        UiKit.led(g, x + 112, y + 8, live ? 0xFF2BD46A : 0xFFFFA020);
        // 표 머리줄
        g.fill(x + 3, y + 19, x + W - 3, y + 31, 0xFF111821);
        UiKit.hline(g, x + 3, y + 31, W - 6, 0xFF243041);
        // 종목 줄
        for (int i = 0; i < StockMenu.N; i++) {
            int ry = y + TOP + i * ROW - 1;
            if (i % 2 == 1) g.fill(x + 3, ry, x + W - 3, ry + ROW, ROW_ALT);
            long price = menu.price(i), prev = menu.prev(i);
            int c = price > prev ? UiKit.UP : price < prev ? UiKit.DOWN : 0xFF4A5566;
            g.fill(x + 3, ry + 2, x + 5, ry + ROW - 2, c);
            // 등락률 배지 배경
            UiKit.pill(g, x + 130, ry + 3, 40, 12, price > prev ? 0x33FF4D4F : price < prev ? 0x333D8BFF : 0x22FFFFFF);
            // 보유 주식 있으면 점
            if (menu.shares(i) > 0) UiKit.circle(g, x + 202, ry + 7, 3, 0xFFF2C14E);
        }
        // 아래 요약 카드 3개
        int fy = y + TOP + StockMenu.N * ROW + 4;
        for (int k = 0; k < 3; k++) {
            int cx = x + 6 + k * 102;
            UiKit.round(g, cx, fy, 98, H - (fy - y) - 7, 2, 0xFF1A2330);
            UiKit.hline(g, cx + 2, fy, 94, 0xFF2A3647);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "WILD 증권", 20, 6, 0xFFFFFFFF);
        boolean live = menu.live();
        UiKit.text(g, (live ? "실시간 시세 · " : "가상 시세 (오프라인) · ") + menu.updated, 120, 6, live ? 0xFF7FE0A3 : 0xFFFFC266);
        UiKit.text(g, "종목", 10, 21, MUTED);
        UiKit.right(g, "현재가", 124, 21, MUTED);
        UiKit.right(g, "등락률", 168, 21, MUTED);
        UiKit.right(g, "보유", 200, 21, MUTED);
        UiKit.text(g, "매수", 210, 21, 0xFFFF7A7E);
        UiKit.text(g, "매도", 262, 21, 0xFF6FA8FF);
        long value = 0, basis = 0;
        for (int i = 0; i < StockMenu.N; i++) {
            int y = TOP + i * ROW + 4;
            long price = menu.price(i), prev = menu.prev(i), shares = menu.shares(i);
            value += price * shares / 100;
            basis += menu.cost(i);
            int color = price > prev ? UiKit.UP : price < prev ? UiKit.DOWN : FLAT;
            UiKit.text(g, StockMarket.STOCKS.get(i).name(), 10, y, 0xFFE8EDF3);
            UiKit.right(g, StockMarket.fmt(price), 124, y, color);
            double pct = prev > 0 ? (price - prev) * 100.0 / prev : 0;
            UiKit.right(g, String.format("%s%.2f%%", pct > 0 ? "▲" : pct < 0 ? "▼" : "", Math.abs(pct)), 168, y, color);
            UiKit.right(g, shares > 0 ? String.format("%,d", shares) : "-", 200, y, shares > 0 ? 0xFFF2C14E : 0xFF56616F);
        }
        long profit = value - basis;
        int fy = TOP + StockMenu.N * ROW + 7;
        UiKit.text(g, "예수금 (현금)", 12, fy, MUTED);
        UiKit.bold(g, String.format("%,d동", menu.cash()), 12, fy + 11, 0xFFFFFFFF);
        UiKit.text(g, "평가 금액", 114, fy, MUTED);
        UiKit.bold(g, String.format("%,d동", value), 114, fy + 11, 0xFFFFFFFF);
        UiKit.text(g, "평가 손익", 216, fy, MUTED);
        double rate = basis > 0 ? profit * 100.0 / basis : 0;
        UiKit.bold(g, String.format("%s%,d", profit >= 0 ? "+" : "", profit), 216, fy + 11,
                profit > 0 ? UiKit.UP : profit < 0 ? UiKit.DOWN : FLAT);
        UiKit.right(g, String.format("%+.1f%%", rate), 306, fy, profit > 0 ? UiKit.UP : profit < 0 ? UiKit.DOWN : FLAT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

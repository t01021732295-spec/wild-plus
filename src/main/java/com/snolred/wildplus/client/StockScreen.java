package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.StockMarket;
import com.snolred.wildplus.menu.StockMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 증권 단말기 화면: 종목 표 + 매수/매도 버튼. 한국식 색: 오르면 빨강, 내리면 파랑. */
public class StockScreen extends AbstractContainerScreen<StockMenu> {
    private static final int W = 316, H = 236, ROW = 18, TOP = 34;
    private static final int UP = 0xFFFF5A5A, DOWN = 0xFF5AA0FF, FLAT = 0xFFCCCCCC;

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
                addRenderableWidget(Button.builder(Component.literal(labels[a]), b -> click(id))
                        .bounds(leftPos + xs[a], topPos + TOP + i * ROW, ws[a], 16).build());
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
        UiKit.panel(g, leftPos, topPos, W, H);
        UiKit.inset(g, leftPos + 4, topPos + 17, W - 8, H - 21, 0xFF0E1620);
        for (int i = 0; i < StockMenu.N; i++) {
            if (i % 2 == 0) {
                g.fill(leftPos + 4, topPos + TOP + i * ROW - 1, leftPos + W - 4, topPos + TOP + i * ROW + 17, 0xFF16222E);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, UiKit.TEXT, false);
        boolean live = menu.live();
        g.text(this.font, Component.literal((live ? "● 실시간 시세 " : "● 가상 시세 (인터넷 연결 안 됨) ") + menu.updated),
                110, 6, live ? 0xFF1E7A2E : 0xFFA05A00, false);
        g.text(this.font, Component.literal("종목"), 8, 22, 0xFF8899AA, false);
        g.text(this.font, Component.literal("현재가(동)"), 78, 22, 0xFF8899AA, false);
        g.text(this.font, Component.literal("등락"), 132, 22, 0xFF8899AA, false);
        g.text(this.font, Component.literal("보유"), 172, 22, 0xFF8899AA, false);
        long value = 0, basis = 0;
        for (int i = 0; i < StockMenu.N; i++) {
            int y = TOP + i * ROW + 4;
            long price = menu.price(i), prev = menu.prev(i), shares = menu.shares(i);
            value += price * shares / 100;
            basis += menu.cost(i);
            int color = price > prev ? UP : price < prev ? DOWN : FLAT;
            g.text(this.font, Component.literal(StockMarket.STOCKS.get(i).name()), 8, y, 0xFFFFFFFF, false);
            g.text(this.font, Component.literal(StockMarket.fmt(price)), 78, y, color, false);
            double pct = prev > 0 ? (price - prev) * 100.0 / prev : 0;
            g.text(this.font, Component.literal(String.format("%s%.2f%%", pct > 0 ? "▲" : pct < 0 ? "▼" : "", Math.abs(pct))), 132, y, color, false);
            g.text(this.font, Component.literal(shares > 0 ? String.format("%,d주", shares) : "-"), 172, y, 0xFFDDDDDD, false);
        }
        long profit = value - basis;
        int fy = TOP + StockMenu.N * ROW + 4;
        g.text(this.font, Component.literal(String.format("내 돈 %,d동", menu.cash())), 8, fy, 0xFFFFD84A, false);
        g.text(this.font, Component.literal(String.format("평가액 %,d동", value)), 110, fy, 0xFFFFFFFF, false);
        g.text(this.font, Component.literal(String.format("손익 %s%,d동", profit >= 0 ? "+" : "", profit)), 210, fy,
                profit > 0 ? UP : profit < 0 ? DOWN : FLAT, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

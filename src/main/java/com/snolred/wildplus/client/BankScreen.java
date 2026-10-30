package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.BankMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 은행 ATM 화면: 잔액 표시창 + 입금 · 출금 · 송금 버튼 */
public class BankScreen extends AbstractContainerScreen<BankMenu> {
    private static final int W = 220, H = 216;
    private Button sendButton;

    public BankScreen(BankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        btn("입금: 전부", BankMenu.DEP_ALL, x + 8, y + 60, 68);
        btn("+100", BankMenu.DEP_100, x + 78, y + 60, 34);
        btn("+10", BankMenu.DEP_10, x + 114, y + 60, 30);
        btn("출금 10", BankMenu.WD_10, x + 8, y + 92, 48);
        btn("100", BankMenu.WD_100, x + 58, y + 92, 34);
        btn("1000", BankMenu.WD_1000, x + 94, y + 92, 38);
        btn("전부", BankMenu.WD_ALL, x + 134, y + 92, 34);
        btn("◀", BankMenu.PREV, x + 8, y + 140, 16);
        btn("▶", BankMenu.NEXT, x + 112, y + 140, 16);
        btn("+10", BankMenu.AMT_10, x + 8, y + 162, 30);
        btn("+100", BankMenu.AMT_100, x + 40, y + 162, 34);
        btn("+1000", BankMenu.AMT_1000, x + 76, y + 162, 38);
        btn("0", BankMenu.AMT_RESET, x + 116, y + 162, 18);
        sendButton = btn("보내기", BankMenu.SEND, x + 150, y + 162, 62);
    }

    private Button btn(String label, int id, int x, int y, int w) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }).bounds(x, y, w, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        sendButton.active = !menu.names.isEmpty() && menu.amount() > 0 && menu.amount() <= menu.balance();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, leftPos, topPos, W, H);
        UiKit.inset(g, leftPos + 7, topPos + 17, W - 14, 30, 0xFF10301A);
        UiKit.inset(g, leftPos + 26, topPos + 140, 84, 16, 0xFF8B8B8B);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, UiKit.TEXT, false);
        g.text(this.font, Component.literal(String.format("통장 잔액  %,d동", menu.balance())), 12, 21, 0xFF7CFC6A, false);
        g.text(this.font, Component.literal(String.format("가진 현금  %,d동", menu.cash())), 12, 34, 0xFFFFD84A, false);
        g.text(this.font, Component.literal("입금 (가방의 동전·주머니에서)"), 8, 50, UiKit.TEXT, false);
        g.text(this.font, Component.literal("출금 (주머니가 있으면 주머니로)"), 8, 82, UiKit.TEXT, false);
        g.text(this.font, Component.literal("이자: 매일 아침 잔액의 1% (하루 최대 500동)"), 8, 114, 0xFF2A6A2A, false);
        g.text(this.font, Component.literal("송금 (통장 → 상대 통장)"), 8, 128, UiKit.TEXT, false);
        String who = menu.names.isEmpty() ? "접속한 사람 없음" : menu.names.get(Math.min(menu.target(), menu.names.size() - 1));
        g.text(this.font, Component.literal(who), 30, 144, 0xFFFFFFFF, true);
        g.text(this.font, Component.literal(String.format("%,d동", menu.amount())), 134, 144, UiKit.TEXT, false);
        long inc = menu.incomeToday();
        g.text(this.font, Component.literal(String.format("오늘 소득 %,d동 · 낸 세금 %,d동", inc, menu.taxToday())), 8, 184, 0xFF7A2A2A, false);
        g.text(this.font, Component.literal(String.format("지금 세율 %d%% · 국고 %,d동", com.snolred.wildplus.economy.Tax.currentRate(inc),
                menu.treasury())), 8, 196, 0xFF505050, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

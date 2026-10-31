package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Tax;
import com.snolred.wildplus.menu.BankMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 은행 ATM. 은색 기기 본체 + 남색 머리판 + 파란 터치스크린 + 아래 카드 투입구·지폐 배출구.
 * 화면 안: 잔액 카드(큰 숫자) · 입금 · 출금 · 송금 · 오늘 세금.
 */
public class BankScreen extends AbstractContainerScreen<BankMenu> {
    private static final int W = 240, H = 266;
    private static final int SX = 12, SY = 26, SW = W - 24, SH = 196; // 화면 영역
    private UiButton sendButton;

    public BankScreen(BankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + SX + 4, y = topPos;
        btn("전부 입금", BankMenu.DEP_ALL, x, y + 94, 66, UiButton.GREEN);
        btn("+10", BankMenu.DEP_10, x + 70, y + 94, 40, UiButton.WHITE);
        btn("+100", BankMenu.DEP_100, x + 114, y + 94, 40, UiButton.WHITE);
        btn("10", BankMenu.WD_10, x, y + 132, 40, UiButton.WHITE);
        btn("100", BankMenu.WD_100, x + 44, y + 132, 40, UiButton.WHITE);
        btn("1,000", BankMenu.WD_1000, x + 88, y + 132, 44, UiButton.WHITE);
        btn("전부 출금", BankMenu.WD_ALL, x + 136, y + 132, 64, UiButton.ORANGE);
        btn("◀", BankMenu.PREV, x, y + 170, 16, UiButton.WHITE);
        btn("▶", BankMenu.NEXT, x + 112, y + 170, 16, UiButton.WHITE);
        btn("+10", BankMenu.AMT_10, x, y + 190, 32, UiButton.WHITE);
        btn("+100", BankMenu.AMT_100, x + 35, y + 190, 34, UiButton.WHITE);
        btn("+1000", BankMenu.AMT_1000, x + 72, y + 190, 38, UiButton.WHITE);
        btn("0", BankMenu.AMT_RESET, x + 113, y + 190, 16, UiButton.WHITE);
        sendButton = btn("보내기", BankMenu.SEND, x + 140, y + 180, 60, UiButton.BLUE);
    }

    private UiButton btn(String label, int id, int x, int y, int w, UiButton.Style style) {
        int h = id == BankMenu.SEND ? 26 : 16;
        return addRenderableWidget(UiButton.of(x, y, w, h, label, style, () -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        sendButton.active = !menu.names.isEmpty() && menu.amount() > 0 && menu.amount() <= menu.balance();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 본체 (은색 금속)
        UiKit.device(g, x, y, W, H, 0xFFE6E9ED, 0xFFAEB5BE);
        // 남색 머리판
        UiKit.round(g, x + 4, y + 4, W - 8, 16, 2, 0xFF0D2552);
        g.fillGradient(x + 5, y + 5, x + W - 5, y + 19, 0xFF1A3F86, 0xFF0D2552);
        UiKit.led(g, x + W - 14, y + 10, 0xFF39E07A);
        // 터치스크린
        UiKit.screen(g, x + SX, y + SY, SW, SH, 0xFFF3F6FB, 0xFFDDE5F0);
        // 잔액 카드 (파란 그라데이션)
        UiKit.round(g, x + SX + 4, y + SY + 4, SW - 8, 46, 2, 0xFF123C8C);
        UiKit.roundGradient(g, x + SX + 5, y + SY + 5, SW - 10, 44, 2, 0xFF3B7BEA, 0xFF1E4FB8);
        g.fill(x + SX + 6, y + SY + 6, x + SX + SW - 6, y + SY + 7, 0x44FFFFFF);
        // 구역 구분선
        UiKit.hline(g, x + SX + 4, y + 116, SW - 8, 0xFFCCD5E2);
        UiKit.hline(g, x + SX + 4, y + 154, SW - 8, 0xFFCCD5E2);
        UiKit.hline(g, x + SX + 4, y + 210, SW - 8, 0xFFCCD5E2);
        // 송금 대상 표시칸
        UiKit.card(g, x + SX + 4 + 18, y + 170, 92, 16, 0xFFFFFFFF, 0xFFB5BDC8);
        // 아래 하드웨어: 카드 투입구 · 명세표 · 지폐 배출구
        int hy = y + SY + SH + 8;
        UiKit.round(g, x + 14, hy, 66, 26, 2, 0xFF7E8692);
        g.fillGradient(x + 15, hy + 1, x + 79, hy + 25, 0xFFC9CED6, 0xFF9AA2AD);
        UiKit.groove(g, x + 22, hy + 11, 50, 4);
        UiKit.led(g, x + 18, hy + 4, 0xFF39E07A);
        UiKit.round(g, x + 88, hy, 64, 26, 2, 0xFF7E8692);
        g.fillGradient(x + 89, hy + 1, x + 151, hy + 25, 0xFFC9CED6, 0xFF9AA2AD);
        UiKit.groove(g, x + 96, hy + 11, 48, 3);
        UiKit.round(g, x + 160, hy, 66, 26, 2, 0xFF2A2F36);
        g.fillGradient(x + 161, hy + 1, x + 225, hy + 25, 0xFF4A515B, 0xFF2A2F36);
        UiKit.groove(g, x + 166, hy + 12, 54, 5);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "WILD BANK", 10, 8, 0xFFFFFFFF);
        UiKit.text(g, this.title, 70, 8, 0xFF9DB8E8);
        int sx = SX + 4;
        // 잔액 카드
        UiKit.text(g, "통장 잔액", sx + 6, SY + 9, 0xFFD6E4FF);
        UiKit.right(g, "보통예금 · 이자 1%/일", SX + SW - 10, SY + 9, 0xFFA9C4F5);
        String bal = String.format("%,d", menu.balance());
        UiKit.big(g, bal, sx + 6, SY + 22, 2, 0xFFFFFFFF);
        UiKit.text(g, "동", sx + 8 + UiKit.bigWidth(bal, 2), SY + 30, 0xFFD6E4FF);
        UiKit.right(g, String.format("현금 %,d동", menu.cash()), SX + SW - 10, SY + 38, 0xFFFFE08A);
        // 입금
        UiKit.bold(g, "입금", sx, 82, UiKit.INK);
        UiKit.text(g, "가방·동전 주머니의 동전을 통장으로", sx + 26, 82, UiKit.INK_SOFT);
        // 출금
        UiKit.bold(g, "출금", sx, 120, UiKit.INK);
        UiKit.text(g, "주머니가 있으면 주머니로", sx + 26, 120, UiKit.INK_SOFT);
        // 송금
        UiKit.bold(g, "송금", sx, 158, UiKit.INK);
        UiKit.text(g, "통장 → 상대 통장", sx + 26, 158, UiKit.INK_SOFT);
        String who = menu.names.isEmpty() ? "접속한 사람 없음" : menu.names.get(Math.min(menu.target(), menu.names.size() - 1));
        UiKit.text(g, who, sx + 22, 174, menu.names.isEmpty() ? UiKit.INK_SOFT : UiKit.INK);
        UiKit.right(g, String.format("%,d동", menu.amount()), SX + SW - 6, 170, 0xFF1E4FB8);
        // 세금
        long inc = menu.incomeToday();
        UiKit.text(g, String.format("오늘 소득 %,d동 · 세금 %,d동", inc, menu.taxToday()), sx, 213, 0xFF8A3A3A);
        UiKit.right(g, String.format("세율 %d%%", Tax.currentRate(inc)), SX + SW - 6, 213, 0xFF8A3A3A);
        // 하드웨어 라벨
        int hy = SY + SH + 8;
        UiKit.text(g, "카드", 24, hy + 2, 0xFF3A414B);
        UiKit.text(g, "명세표", 96, hy + 2, 0xFF3A414B);
        UiKit.text(g, "현금", 166, hy + 2, 0xFFC9CED6);
        UiKit.right(g, String.format("국고 %,d", menu.treasury()), 222, hy + 2, 0xFF8A929D);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

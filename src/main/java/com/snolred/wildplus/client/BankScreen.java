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
    private static final int W = 240, H = 228;
    private static final int SX = 12, SY = 24, SW = W - 24, SH = 170; // 화면 영역
    private UiButton sendButton;
    private final java.util.List<UiButton> mainBtns = new java.util.ArrayList<>(), loanBtns = new java.util.ArrayList<>();
    private UiButton tab0, tab1, tab2, takeBtn, repayBtn, termBtn;
    private int tab, scroll;
    private static final int HIST_ROWS = 6;

    public BankScreen(BankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + SX + 4, y = topPos;
        btn("전부 입금", BankMenu.DEP_ALL, x, y + 78, 66, UiButton.GREEN);
        btn("+10", BankMenu.DEP_10, x + 70, y + 78, 40, UiButton.WHITE);
        btn("+100", BankMenu.DEP_100, x + 114, y + 78, 40, UiButton.WHITE);
        btn("10", BankMenu.WD_10, x, y + 111, 24, UiButton.WHITE);
        btn("100", BankMenu.WD_100, x + 28, y + 111, 28, UiButton.WHITE);
        btn("1천", BankMenu.WD_1000, x + 60, y + 111, 28, UiButton.WHITE);
        btn("1만", BankMenu.WD_10K, x + 92, y + 111, 28, UiButton.WHITE);
        btn("10만", BankMenu.WD_100K, x + 124, y + 111, 32, UiButton.WHITE);
        btn("전부", BankMenu.WD_ALL, x + 160, y + 111, 40, UiButton.ORANGE);
        btn("◀", BankMenu.PREV, x, y + 144, 16, UiButton.WHITE);
        btn("▶", BankMenu.NEXT, x + 112, y + 144, 16, UiButton.WHITE);
        btn("+10", BankMenu.AMT_10, x, y + 164, 32, UiButton.WHITE);
        btn("+100", BankMenu.AMT_100, x + 35, y + 164, 34, UiButton.WHITE);
        btn("+1000", BankMenu.AMT_1000, x + 72, y + 164, 38, UiButton.WHITE);
        btn("0", BankMenu.AMT_RESET, x + 113, y + 164, 16, UiButton.WHITE);
        sendButton = btn("보내기", BankMenu.SEND, x + 140, y + 154, 60, UiButton.BLUE);
        mainBtns.clear();
        for (var w : children()) if (w instanceof UiButton b) mainBtns.add(b);
        loanBtns.clear();
        loanBtns.add(btn("+10만", BankMenu.LOAN_100K, x, y + 118, 44, UiButton.WHITE));
        loanBtns.add(btn("+100만", BankMenu.LOAN_1M, x + 48, y + 118, 48, UiButton.WHITE));
        loanBtns.add(btn("+1000만", BankMenu.LOAN_10M, x + 100, y + 118, 52, UiButton.WHITE));
        loanBtns.add(btn("0", BankMenu.LOAN_RESET, x + 156, y + 118, 24, UiButton.WHITE));
        termBtn = btn("7일", BankMenu.LOAN_TERM, x + 184, y + 118, 24, UiButton.STEEL);
        loanBtns.add(termBtn);
        takeBtn = btn("대출 받기", BankMenu.LOAN_TAKE, x, y + 158, 100, UiButton.BLUE);
        repayBtn = btn("전액 상환 (수수료 1%)", BankMenu.LOAN_REPAY, x + 106, y + 158, 100, UiButton.ORANGE);
        loanBtns.add(takeBtn);
        loanBtns.add(repayBtn);
        tab0 = addRenderableWidget(UiButton.of(leftPos + 112, topPos + 5, 36, 14, "이체", UiButton.STEEL, () -> setTab(0)));
        tab1 = addRenderableWidget(UiButton.of(leftPos + 150, topPos + 5, 36, 14, "내역", UiButton.STEEL, () -> setTab(1)));
        tab2 = addRenderableWidget(UiButton.of(leftPos + 188, topPos + 5, 36, 14, "대출", UiButton.STEEL, () -> setTab(2)));
        setTab(tab);
    }

    private void setTab(int t) {
        tab = t;
        for (UiButton b : mainBtns) b.visible = t == 0;
        for (UiButton b : loanBtns) b.visible = t == 2;
        tab0.setSelected(t == 0);
        tab1.setSelected(t == 1);
        tab2.setSelected(t == 2);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (tab == 1) {
            scroll = Math.max(0, Math.min(Math.max(0, menu.history.size() - HIST_ROWS), scroll - (int) Math.signum(dy)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
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
        termBtn.setMessage(net.minecraft.network.chat.Component.literal(menu.term() + "일"));
        takeBtn.active = menu.debt() <= 0 && menu.ask() > 0 && menu.streak() < 7;
        repayBtn.active = menu.debt() > 0 && menu.balance() >= menu.debt() + menu.debt() / 100;
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
        UiKit.round(g, x + SX + 4, y + SY + 3, SW - 8, 38, 2, 0xFF123C8C);
        UiKit.roundGradient(g, x + SX + 5, y + SY + 4, SW - 10, 36, 2, 0xFF3B7BEA, 0xFF1E4FB8);
        g.fill(x + SX + 6, y + SY + 5, x + SX + SW - 6, y + SY + 6, 0x44FFFFFF);
        // 구역 구분선
        if (tab == 0) UiKit.hline(g, x + SX + 4, y + 98, SW - 8, 0xFFCCD5E2);
        if (tab == 0) UiKit.hline(g, x + SX + 4, y + 131, SW - 8, 0xFFCCD5E2);
        if (tab == 0) UiKit.hline(g, x + SX + 4, y + 184, SW - 8, 0xFFCCD5E2);
        // 송금 대상 표시칸
        if (tab == 0) UiKit.card(g, x + SX + 4 + 18, y + 144, 92, 16, 0xFFFFFFFF, 0xFFB5BDC8);
        // 아래 하드웨어: 카드 투입구 · 명세표 · 지폐 배출구
        int hy = y + SY + SH + 6;
        UiKit.round(g, x + 14, hy, 66, 22, 2, 0xFF7E8692);
        g.fillGradient(x + 15, hy + 1, x + 79, hy + 21, 0xFFC9CED6, 0xFF9AA2AD);
        UiKit.groove(g, x + 22, hy + 13, 50, 4);
        UiKit.led(g, x + 18, hy + 4, 0xFF39E07A);
        UiKit.round(g, x + 88, hy, 64, 22, 2, 0xFF7E8692);
        g.fillGradient(x + 89, hy + 1, x + 151, hy + 21, 0xFFC9CED6, 0xFF9AA2AD);
        UiKit.groove(g, x + 96, hy + 13, 48, 3);
        UiKit.round(g, x + 160, hy, 66, 22, 2, 0xFF2A2F36);
        g.fillGradient(x + 161, hy + 1, x + 225, hy + 21, 0xFF4A515B, 0xFF2A2F36);
        UiKit.groove(g, x + 166, hy + 13, 54, 5);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "WILD BANK", 10, 8, 0xFFFFFFFF);
        UiKit.text(g, this.title, 70, 8, 0xFF9DB8E8);
        int sx = SX + 4;
        // 잔액 카드
        UiKit.text(g, "통장 잔액", sx + 6, SY + 7, 0xFFD6E4FF);
        UiKit.right(g, "보통예금 · 이자 1%/일", SX + SW - 10, SY + 7, 0xFFA9C4F5);
        String bal = String.format("%,d", menu.balance());
        UiKit.big(g, bal, sx + 6, SY + 20, 2, 0xFFFFFFFF);
        UiKit.text(g, "동", sx + 8 + UiKit.bigWidth(bal, 2), SY + 28, 0xFFD6E4FF);
        UiKit.right(g, String.format("현금 %,d동", menu.cash()), SX + SW - 10, SY + 30, 0xFFFFE08A);
        int hyy = SY + SH + 6;
        UiKit.text(g, "카드", 24, hyy + 2, 0xFF3A414B);
        UiKit.text(g, "명세표", 96, hyy + 2, 0xFF3A414B);
        UiKit.text(g, "현금", 166, hyy + 2, 0xFFC9CED6);
        UiKit.right(g, String.format("국고 %,d", menu.treasury()), 222, hyy + 2, 0xFF8A929D);
        if (tab == 1) { history(g, sx); return; }
        if (tab == 2) { loans(g, sx); return; }
        // 입금
        UiKit.bold(g, "입금", sx, 68, UiKit.INK);
        UiKit.text(g, "가방·동전 주머니의 동전을 통장으로", sx + 26, 68, UiKit.INK_SOFT);
        // 출금
        UiKit.bold(g, "출금", sx, 101, UiKit.INK);
        UiKit.text(g, "주머니가 있으면 주머니로", sx + 26, 101, UiKit.INK_SOFT);
        // 송금
        UiKit.bold(g, "송금", sx, 134, UiKit.INK);
        UiKit.text(g, "통장 → 상대 통장", sx + 26, 134, UiKit.INK_SOFT);
        String who = menu.names.isEmpty() ? "접속한 사람 없음" : menu.names.get(Math.min(menu.target(), menu.names.size() - 1));
        UiKit.text(g, who, sx + 22, 148, menu.names.isEmpty() ? UiKit.INK_SOFT : UiKit.INK);
        UiKit.right(g, String.format("%,d동", menu.amount()), SX + SW - 6, 144, 0xFF1E4FB8);
        // 세금
        long inc = menu.incomeToday();
        UiKit.text(g, String.format("오늘 소득 %,d동 · 세금 %,d동", inc, menu.taxToday()), sx, 186, 0xFF8A3A3A);
        UiKit.right(g, String.format("세율 %d%%", Tax.currentRate(inc)), SX + SW - 6, 186, 0xFF8A3A3A);
    }

    private void history(GuiGraphicsExtractor g, int sx) {
        UiKit.bold(g, "입출금 내역 (최근 50건)", sx, 68, UiKit.INK);
        if (menu.history.isEmpty()) {
            UiKit.text(g, "아직 내역이 없어요", sx, 90, UiKit.INK_SOFT);
            return;
        }
        scroll = Math.min(scroll, Math.max(0, menu.history.size() - HIST_ROWS));
        for (int i = 0; i < HIST_ROWS && scroll + i < menu.history.size(); i++) {
            String[] f = menu.history.get(scroll + i).split("\\|", 4);
            if (f.length < 4) continue;
            int ry = 82 + i * 20;
            long d = Long.parseLong(f[2]);
            UiKit.text(g, f[0] + "일차", sx, ry, UiKit.INK_SOFT);
            UiKit.text(g, UiKit.font().plainSubstrByWidth(f[1], 100), sx + 44, ry, UiKit.INK);
            UiKit.right(g, String.format("%+,d동", d), SX + SW - 6, ry, d >= 0 ? 0xFF1E8A4C : 0xFFC92A31);
            UiKit.right(g, String.format("잔액 %,d", Long.parseLong(f[3])), SX + SW - 6, ry + 9, UiKit.INK_SOFT);
            UiKit.hline(g, sx, ry + 18, SW - 8, 0xFFDCE0E6);
        }
    }

    private void loans(GuiGraphicsExtractor g, int sx) {
        UiKit.bold(g, "대출", sx, 68, UiKit.INK);
        UiKit.right(g, String.format("신용점수 %d", menu.credit()), SX + SW - 6, 68, menu.credit() >= 700 ? 0xFF1E8A4C : menu.credit() >= 500 ? UiKit.INK : 0xFFC92A31);
        boolean owe = menu.debt() > 0;
        UiKit.text(g, String.format("한도 %,d동 · 금리 하루 %.2f%%", menu.loanLimit(), menu.rate() / 100.0), sx, 80, UiKit.INK_SOFT);
        if (owe) {
            UiKit.bold(g, String.format("남은 빚 %,d동", menu.debt()), sx, 94, 0xFFC92A31);
            UiKit.text(g, String.format("매일 %,d동 자동 상환 · %d일 남음", menu.pay(), menu.days()), sx, 106, UiKit.INK);
            if (menu.streak() > 0) UiKit.text(g, String.format("연체 %d일째", menu.streak()), sx, 146, 0xFFC92A31);
        } else {
            UiKit.bold(g, String.format("신청 금액 %,d동", menu.ask()), sx, 94, 0xFF1E4FB8);
            UiKit.text(g, menu.pay() > 0 ? String.format("예상 매일 상환 %,d동 (%d일)", menu.pay(), menu.term()) : "금액을 고르면 상환액이 나와요", sx, 106, UiKit.INK);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 3.5 마권 발매기: 왼쪽 전광판 경주표 (번호 · 기수색 · 이름 · 주인 · 속도/지구력 막대 · 최근 · 배당),
 * 오른쪽 종이 마권 (고른 말 · 단승/연승/복연승 · 칩 1천/5천/1만/5만 · 예상 당첨금 · [마권 사기]).
 * 서버: 첫 줄 = 경주 (상태\n남은 초\n날씨\n내 마권\n지난 결과\n경마장 있음), 다음 6줄 = 말 (주인\n속도\n지구력\n최근\n배당\n색\n내 말)
 */
public class BettingScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 400, H = 236;
    private static final int[][] SILK = {{0xFFDC3232, 0xFFFAFAFA}, {0xFF325AC8, 0xFFFADC3C}, {0xFF289650, 0xFFFAFAFA}, {0xFFF0AA28, 0xFF3C281E},
            {0xFF9646B4, 0xFFFAFAFA}, {0xFF1E1E24, 0xFFE63C3C}};
    private static final String[] TYPE = {"단승", "연승", "복연승"};
    private static final String[] TYPE_SUB = {"1등을 맞혀요", "2등 안에 들면", "3등 안에 들면"};
    private static final String[] CHIP = {"1천", "5천", "1만", "5만"};
    private static final long[] CHIPS = {1_000, 5_000, 10_000, 50_000};
    private int sel, type, chip = 2;
    private UiButton buy;
    private final List<UiButton> rows = new ArrayList<>();

    public BettingScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    private String[] info() {
        return menu.entries.isEmpty() ? new String[0] : menu.entries.get(0).sub().split("\n", -1);
    }

    private String[] horse(int i) {
        return menu.entries.get(i + 1).sub().split("\n", -1);
    }

    private int horses() {
        return Math.max(0, menu.entries.size() - 1);
    }

    private double odds(int i, int t) {
        double w;
        try {
            w = Double.parseDouble(horse(i)[4]);
        } catch (Exception e) {
            w = 1;
        }
        return t == 0 ? w : t == 1 ? Math.max(1.1, 1 + (w - 1) * 0.4) : Math.max(1.05, 1 + (w - 1) * 0.25);
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        for (int i = 0; i < 6; i++) {
            final int k = i;
            rows.add(addRenderableWidget(new Clear(leftPos + 11, topPos + 50 + i * 28, 222, 25, () -> sel = k)));
        }
        int tx = leftPos + 244;
        for (int t = 0; t < 3; t++) {
            final int k = t;
            addRenderableWidget(new Clear(tx + 8 + t * 48, topPos + 96, 46, 13, () -> type = k));
        }
        for (int c = 0; c < 4; c++) {
            final int k = c;
            addRenderableWidget(new Clear(tx + 8 + c * 36, topPos + 126, 30, 16, () -> chip = k));
        }
        buy = addRenderableWidget(UiButton.of(tx, topPos + 192, W - 252, 18, "🎫 마권 사기", UiButton.ORANGE, () -> {
            if (this.minecraft != null && this.minecraft.gameMode != null)
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1000 + sel * 100 + type * 10 + chip);
        }));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        String[] in = info();
        buy.active = in.length > 0 && in[0].equals("0") && sel < horses();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        UiKit.device(g, x, y, W, H, 0xFF1F7A4C, 0xFF145232);
        g.fillGradient(x + 3, y + 3, x + W - 3, y + 30, 0xFF2E9A60, 0xFF1F7A4C);
        g.fill(x + 3, y + 30, x + W - 3, y + 32, 0xFFE8BE46);
        UiKit.round(g, x + W - 96, y + 7, 86, 18, 3, 0xFFE8BE46);
        UiKit.round(g, x + W - 95, y + 8, 84, 16, 3, 0xFF0F1412);
        // 전광판
        UiKit.round(g, x + 8, y + 36, 228, H - 54, 4, 0xFF3C4642);
        UiKit.round(g, x + 9, y + 37, 226, H - 56, 4, 0xFF101418);
        for (int i = 0; i < horses() && i < 6; i++) {
            int ry = y + 50 + i * 28;
            boolean on = i == sel;
            g.fill(x + 11, ry, x + 233, ry + 25, on ? 0xFF243C30 : 0xFF181E22);
            if (on) UiKit.outline(g, x + 11, ry, 222, 25, 0xFFFFDC50);
            int[] c = SILK[i % 6];
            g.fill(x + 14, ry + 3, x + 28, ry + 22, c[1]);
            g.fill(x + 15, ry + 4, x + 27, ry + 21, c[0]);
            String[] h = horse(i);
            bar(g, x + 100, ry + 10, 26, num(h, 1) / 100f, 0xFF5AC878);
            bar(g, x + 132, ry + 10, 26, num(h, 2) / 100f, 0xFF5AA0E6);
        }
        // 종이 마권
        int tx = x + 244;
        UiKit.round(g, tx, y + 36, W - 252, 150, 4, 0xFFD2C8AF);
        UiKit.round(g, tx + 1, y + 37, W - 254, 148, 4, 0xFFFCF8EB);
        for (int k = 0; k < 146; k += 6) g.fill(tx - 1, y + 39 + k, tx + 2, y + 42 + k, 0xFF1F7A4C);
        g.fill(tx + 8, y + 56, tx + 48, y + 90, 0xFFF0E8D2);
        if (sel < horses()) {
            g.pose().pushMatrix();
            g.pose().translate(tx + 12, y + 57);
            g.pose().scale(2F, 2F);
            g.item(menu.entries.get(sel + 1).icon(), 0, 0);
            g.pose().popMatrix();
        }
        for (int t = 0; t < 3; t++) {
            int bx = tx + 8 + t * 48;
            UiKit.round(g, bx, y + 96, 46, 13, 3, 0xFF247850);
            UiKit.round(g, bx + 1, y + 97, 44, 11, 3, t == type ? 0xFF247850 : 0xFFEBE8DE);
        }
        int[] chipC = {0xFFC8C8CD, 0xFF4682DC, 0xFFDC3C3C, 0xFF1E1E24};
        for (int c = 0; c < 4; c++) {
            int cx = tx + 8 + c * 36;
            UiKit.round(g, cx - 1, y + 125, 32, 18, 8, c == chip ? 0xFFFFDC50 : 0xFFFFFFFF);
            UiKit.round(g, cx, y + 126, 30, 16, 7, chipC[c]);
        }
        g.fill(tx + 8, y + 148, x + W - 14, y + 149, 0xFFD2C8AF);
    }

    private static float num(String[] a, int i) {
        try {
            return Float.parseFloat(a[i]);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float f, int c) {
        UiKit.round(g, x, y, w, 5, 2, 0xFF323C38);
        UiKit.round(g, x, y, Math.max(4, (int) (w * Math.min(1, f))), 5, 2, c);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var font = UiKit.font();
        String[] in = info();
        String title = menu.entries.isEmpty() ? "경주" : menu.entries.get(0).title();
        UiKit.bold(g, "🏇 야생 경마장 · " + title, 10, 7, 0xFFFFF4DA);
        UiKit.text(g, "오후 3시 · 흙 주로 · " + (in.length > 2 ? in[2] : "") + (in.length > 5 && in[5].equals("0") ? " · 경마장 상자를 지으면 말들이 달려요" : ""), 10, 19, 0xFFBEE6C8);
        String timer;
        if (in.length > 0 && in[0].equals("1")) timer = "경주 중!";
        else if (in.length > 0 && in[0].equals("2")) timer = "오늘 경주 끝";
        else {
            long left = in.length > 1 ? (long) num(in, 1) : 0;
            timer = String.format("출발까지 %d:%02d", left / 60, left % 60);
        }
        UiKit.center(g, timer, W - 53, 12, 0xFFFFDC50);
        String[] heads = {"번", "말 / 주인", "속도", "지구력", "최근", "배당"};
        int[] hx = {14, 32, 100, 132, 166, 206};
        for (int k = 0; k < heads.length; k++) g.text(font, heads[k], hx[k], 41, 0xFF8CAA96, false);
        for (int i = 0; i < horses() && i < 6; i++) {
            int ry = 50 + i * 28;
            String[] h = horse(i);
            int[] c = SILK[i % 6];
            UiKit.center(g, String.valueOf(i + 1), 21, ry + 10, c[1]);
            g.text(font, menu.entries.get(i + 1).title(), 32, ry + 4, 0xFFF0F5F0, false);
            g.text(font, (h.length > 0 ? h[0] : "") + (h.length > 6 && h[6].equals("1") ? " (내 말)" : ""), 32, ry + 15, h.length > 6 && h[6].equals("1") ? 0xFFFFDC50 : 0xFF96B4A0, false);
            g.text(font, h.length > 3 ? h[3] : "", 166, ry + 9, 0xFFE6E6DC, false);
            UiKit.right(g, (h.length > 4 ? h[4] : "") + "배", 230, ry + 9, 0xFFFFDC50);
        }
        // 마권
        int tx = 244;
        UiKit.bold(g, "마 권", tx + 10, 42, 0xFFB0283A);
        UiKit.right(g, title, W - 14, 43, 0xFF6E6E78);
        if (sel < horses()) {
            UiKit.bold(g, (sel + 1) + "번 " + menu.entries.get(sel + 1).title(), tx + 54, 60, 0xFF23305A);
            g.text(font, String.format("단승 배당 %.1f배", odds(sel, 0)), tx + 54, 74, 0xFF6E6E78, false);
        }
        for (int t = 0; t < 3; t++) UiKit.center(g, TYPE[t], tx + 31 + t * 48, 99, t == type ? 0xFFFFFFFF : 0xFF3C4650);
        g.text(font, TYPE_SUB[type] + " · 건 돈", tx + 10, 113, 0xFF6E6E78, false);
        for (int c = 0; c < 4; c++) UiKit.center(g, CHIP[c], tx + 23 + c * 36, 130, 0xFFFFFFFF);
        double o = sel < horses() ? odds(sel, type) : 0;
        g.text(font, String.format("%,d동 × %.1f배 → 당첨 시", CHIPS[chip], o), tx + 10, 154, 0xFF3C4650, false);
        String win = String.format("%,d동", Math.round(CHIPS[chip] * o));
        UiKit.big(g, win, W - 14 - UiKit.bigWidth(win, 2), 166, 2, 0xFFB0283A);
        g.text(font, in.length > 3 ? in[3] : "", tx, 214, 0xFFBEE6C8, false);
        if (in.length > 4 && !in[4].isEmpty()) {
            String last = "지난 경주 " + in[4];
            while (font.width(last) > 222 && last.length() > 4) last = last.substring(0, last.length() - 2) + "…";
            g.text(font, last, 12, H - 12, 0xFF8CAA96, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    private static final class Clear extends UiButton {
        Clear(int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.empty(), UiButton.WHITE, action);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            if (isHoveredOrFocused()) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x22FFFFFF);
        }
    }
}

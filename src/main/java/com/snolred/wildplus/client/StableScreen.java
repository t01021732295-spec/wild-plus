package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 3.5 마구간: 왼쪽 내 말 사진 (마구간 안) · 이름 · 성적 · 레벨 / 가운데 능력치 (속도 · 지구력 · 순발력 · 근성 · 컨디션 하트 · 배고픔) /
 * 오른쪽 할 일 카드 (먹이 · 달리기 · 장애물 · 휴식) + [출전 신청]. 등록 전이면 "말을 타고 눌러 등록" 큰 안내.
 * 서버: 첫 줄 = 말 (등록\n속도\n지구력\n순발력\n근성\n컨디션\n배고픔\n레벨\n경험치\n출전\n1착\n2착\n상금\n훈련\n출전신청\n경주안내\n3착)
 */
public class StableScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 400, H = 236;
    private static final String[][] ACTS = {{"🥕 먹이 주기", "당근 · 건초 · 사과", "황금 당근은 컨디션 +1"},
            {"🏃 주로 달리기", "속도 +2 · 지구력 +1", "1,000동 · 배고픔 -15"}, {"🚧 장애물 연습", "순발력 +2 · 근성 +1", "2,000동 · 배고픔 -15"},
            {"😴 푹 쉬기", "컨디션 +2", "하루 한 번"}};
    private static final int[] ACT_C = {0xFF5AA050, 0xFF4682DC, 0xFFDC8C28, 0xFF966EB4};

    public StableScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    private String[] f() {
        return menu.entries.isEmpty() ? new String[0] : menu.entries.get(0).sub().split("\n", -1);
    }

    private String s(int i) {
        String[] a = f();
        return i < a.length ? a[i] : "0";
    }

    private int n(int i) {
        try {
            return (int) Double.parseDouble(s(i));
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean reg() {
        return s(0).equals("1");
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        int cx = leftPos + 278;
        for (int i = 0; i < 4; i++) {
            final int k = i;
            addRenderableWidget(new Card(cx, topPos + 36 + i * 37, W - 286, 34, k, () -> click(k)));
        }
        addRenderableWidget(UiButton.of(cx, topPos + 188, W - 286, 22, "🏁 다음 경주 출전 신청", UiButton.RED, () -> click(5)));
        addRenderableWidget(UiButton.of(leftPos + 8, topPos + 214, 142, 16, "🐴 타고 있는 말 등록", UiButton.GOLD, () -> click(4)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        UiKit.device(g, x, y, W, H, 0xFF784E2C, 0xFF5A3A20);
        for (int k = y + 34; k < y + H - 4; k += 9) g.fill(x + 4, k, x + W - 4, k + 1, 0xFF694224);
        g.fillGradient(x + 3, y + 3, x + W - 3, y + 30, 0xFFAA3C32, 0xFF782822);
        // 사진 (마구간 안)
        UiKit.round(g, x + 8, y + 36, 142, 122, 4, 0xFF503219);
        g.fillGradient(x + 10, y + 38, x + 148, y + 124, 0xFFFAEBC8, 0xFFE1CDA5);
        g.fillGradient(x + 10, y + 124, x + 148, y + 156, 0xFFDEBE5A, 0xFFBEA046);
        if (reg() && !menu.entries.isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(x + 35, y + 44);
            g.pose().scale(5.5F, 5.5F);
            g.item(menu.entries.get(0).icon(), 0, 0);
            g.pose().popMatrix();
        }
        UiKit.round(g, x + 8, y + 161, 142, 18, 3, 0xFFFAF0D7);
        // 경험치 막대
        UiKit.round(g, x + 12, y + 204, 136, 5, 2, 0xFF503219);
        UiKit.round(g, x + 12, y + 204, Math.max(5, 136 * n(8) / 100), 5, 2, 0xFFFFC83C);
        // 능력치 판
        int px = x + 158;
        UiKit.round(g, px, y + 36, 112, H - 44, 4, 0xFF503219);
        UiKit.round(g, px + 1, y + 37, 110, H - 46, 4, 0xFFFCF6E6);
        int[] cols = {0xFF5ABE6E, 0xFF5096DC, 0xFFF0A032, 0xFFC85A5A};
        for (int i = 0; i < 4; i++) bar(g, px + 8, y + 66 + i * 21, 96, n(1 + i) / 100f, cols[i]);
        bar(g, px + 8, y + 182, 96, n(6) / 100f, 0xFFE6963C);
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float f, int c) {
        UiKit.round(g, x, y, w, 6, 3, 0xFFE1E1E6);
        UiKit.round(g, x, y, Math.max(6, (int) (w * Math.min(1, f))), 6, 3, c);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var font = UiKit.font();
        UiKit.bold(g, "🐴 마구간 · 내 경주마", 10, 7, 0xFFFFF4DA);
        UiKit.text(g, "말을 타고 [등록] · 하루 훈련 3번 · 컨디션이 좋을수록 빨라요 · " + s(15), 10, 19, 0xFFFFDCD2);
        String name = menu.entries.isEmpty() ? "" : menu.entries.get(0).title();
        if (!reg()) {
            UiKit.center(g, "아직 경주마가 없어요", 79, 84, 0xFF6B4A2A);
            UiKit.center(g, "말을 타고 아래 [등록]", 79, 98, 0xFF8A6A4A);
            name = "등록 전";
        }
        UiKit.center(g, name, 79, 166, 0xFF3C2819);
        g.text(font, String.format("출전 %d · 1착 %d · 2착 %d · 3착 %d", n(9), n(10), n(11), n(16)), 12, 184, 0xFFFAEBCD, false);
        g.text(font, String.format("레벨 %d · 상금 %,d동", n(7), n(12)), 12, 194, 0xFFFAEBCD, false);
        int px = 158;
        UiKit.bold(g, "능력치", px + 8, 42, 0xFF3C2819);
        String[] st = {"속도", "지구력", "순발력", "근성"};
        for (int i = 0; i < 4; i++) {
            g.text(font, st[i], px + 8, 56 + i * 21, 0xFF503C28, false);
            UiKit.right(g, String.valueOf(n(1 + i)), px + 104, 56 + i * 21, 0xFF3C2819);
        }
        g.text(font, "컨디션", px + 8, 144, 0xFF503C28, false);
        int cond = n(5);
        g.text(font, "♥".repeat(cond) + "♡".repeat(Math.max(0, 5 - cond)) + "  " + (cond >= 4 ? "좋음" : cond >= 2 ? "보통" : "지침"), px + 8, 155, 0xFFDC3C50, false);
        g.text(font, "배고픔 " + n(6), px + 8, 172, 0xFF503C28, false);
        g.text(font, "훈련 " + n(13) + "/3" + (s(14).equals("1") ? " · 출전 신청함 ✔" : ""), px + 8, 194, s(14).equals("1") ? 0xFF2E8A3E : 0xFF786450, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    /** 할 일 카드 (색 테 · 제목 · 효과 · 비용) */
    private static final class Card extends UiButton {
        final int k;

        Card(int x, int y, int w, int h, int k, Runnable action) {
            super(x, y, w, h, Component.empty(), UiButton.WHITE, action);
            this.k = k;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            UiKit.round(g, x, y, w, h, 4, ACT_C[k]);
            UiKit.round(g, x + 2, y + 2, w - 4, h - 4, 3, isHoveredOrFocused() ? 0xFFFFFFF4 : 0xFFFCF6E6);
            var font = UiKit.font();
            UiKit.bold(g, ACTS[k][0], x + 6, y + 4, 0xFF3C2819);
            g.text(font, ACTS[k][1], x + 6, y + 15, 0xFF5A5046, false);
            g.text(font, ACTS[k][2], x + 6, y + 24, ACT_C[k], false);
        }
    }
}

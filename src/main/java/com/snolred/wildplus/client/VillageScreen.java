package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 3.5 마을 발전 게시판: 위에 건물 카드 5장 (모형 · 이름 · 단계 점 · 완공 · 잠김) / 아래 파란 설계도 (다음 단계 재료 막대 · 돈 · 전체 진행) /
 * 오른쪽 기여 순위 + [손에 든 재료 기부] [돈 기부 1만 · 10만] [공사 부지 상자 받기].
 * 서버: 처음 5줄 = 건물 (단계\n열림\n진행%\n효과\n재료;재료;돈 (이름|아이템|모음|필요)\n부지\n준비), 그다음 = 기여 순위 (이름 / 내용)
 * 버튼 번호 = 건물 × 10 + (0 재료 · 1 만동 · 2 십만동 · 3 부지 상자)
 */
public class VillageScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 420, H = 262, N = 5;
    private int sel;
    private UiButton giveItems, give1, give10, site;
    private final List<UiButton> cards = new ArrayList<>();

    public VillageScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        // 처음엔 진행 중인 (열렸고 완공 안 된) 건물
        for (int i = 0; i < Math.min(N, menu.entries.size()); i++) {
            String[] f = menu.entries.get(i).sub().split("\n", -1);
            if (f.length > 1 && f[1].equals("1") && !f[0].equals("3")) {
                sel = i;
                break;
            }
        }
    }

    private String[] p(int i) {
        return i < menu.entries.size() ? menu.entries.get(i).sub().split("\n", -1) : new String[0];
    }

    private static int num(String[] a, int i) {
        try {
            return Integer.parseInt(a[i]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void click(int a) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, sel * 10 + a);
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();
        for (int i = 0; i < N; i++) {
            final int k = i;
            cards.add(addRenderableWidget(new Clear(leftPos + 8 + i * 81, topPos + 36, 77, 76, () -> sel = k)));
        }
        int rx = leftPos + 276, w = W - 284;
        giveItems = addRenderableWidget(UiButton.of(rx, topPos + 186, w, 18, "📦 손에 든 재료 기부", UiButton.GREEN, () -> click(0)));
        give1 = addRenderableWidget(UiButton.of(rx, topPos + 208, w / 2 - 2, 18, "💰 1만 동", UiButton.GOLD, () -> click(1)));
        give10 = addRenderableWidget(UiButton.of(rx + w / 2 + 2, topPos + 208, w / 2 - 2, 18, "💰 10만 동", UiButton.GOLD, () -> click(2)));
        site = addRenderableWidget(UiButton.of(rx, topPos + 230, w, 18, "🏗 공사 부지 상자 받기", UiButton.BLUE, () -> click(3)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        String[] f = p(sel);
        boolean open = f.length > 1 && f[1].equals("1"), done = f.length > 0 && f[0].equals("3");
        giveItems.active = give1.active = give10.active = open && !done;
        site.active = open && !done && f.length > 5 && f[5].equals("0");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        UiKit.device(g, x, y, W, H, 0xFFB07E50, 0xFF8A5E36);
        g.fillGradient(x + 3, y + 3, x + W - 3, y + 30, 0xFF3C6EBE, 0xFF2850A0);
        // 건물 카드
        for (int i = 0; i < N && i < menu.entries.size(); i++) {
            String[] f = p(i);
            int cx = x + 8 + i * 81, st = num(f, 0);
            boolean open = f.length > 1 && f[1].equals("1");
            UiKit.round(g, cx, y + 36, 77, 76, 4, i == sel ? 0xFF3C6EBE : 0xFF8C643C);
            UiKit.round(g, cx + (i == sel ? 2 : 1), y + 36 + (i == sel ? 2 : 1), 77 - (i == sel ? 4 : 2), 76 - (i == sel ? 4 : 2), 3, 0xFFFCF6E6);
            g.fillGradient(cx + 4, y + 40, cx + 73, y + 82, open ? 0xFF9CCFF7 : 0xFFB4B4B4, open ? 0xFFD6ECFC : 0xFFD2D2D2);
            g.pose().pushMatrix();
            g.pose().translate(cx + 22.5F, y + 44);
            g.pose().scale(2F, 2F);
            g.item(menu.entries.get(i).icon(), 0, 0);
            g.pose().popMatrix();
            if (!open) g.fill(cx + 4, y + 40, cx + 73, y + 82, 0x88505050);
            for (int k = 0; k < 3; k++) {
                int dx = cx + 26 + k * 9;
                UiKit.round(g, dx, y + 100, 7, 7, 3, k < st ? 0xFF3CA05A : 0xFFCDC3AF);
            }
            if (st >= 3) UiKit.round(g, cx + 46, y + 41, 26, 11, 2, 0xFF3CA05A);
        }
        // 설계도
        int bx = x + 8, by = y + 118, bw = 260, bh = H - 126;
        UiKit.round(g, bx, by, bw, bh, 4, 0xFFE6ECFA);
        UiKit.round(g, bx + 2, by + 2, bw - 4, bh - 4, 3, 0xFF2C54A4);
        for (int k = bx + 4; k < bx + bw - 4; k += 10) g.fill(k, by + 3, k + 1, by + bh - 3, 0xFF3660B2);
        for (int k = by + 4; k < by + bh - 4; k += 10) g.fill(bx + 3, k, bx + bw - 3, k + 1, 0xFF3660B2);
        String[] f = p(sel);
        String[] mats = f.length > 4 ? f[4].split(";") : new String[0];
        for (int m = 0; m < mats.length && m < 3; m++) {
            String[] q = mats[m].split("\\|");
            if (q.length < 4) continue;
            int my = by + 34 + m * 24;
            UiKit.round(g, bx + 8, my - 1, 18, 18, 2, 0xFFFFFFFF);
            try {
                g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(q[1]))), bx + 9, my);
            } catch (Exception ignored) {
            }
            long have = Long.parseLong(q[2]), need = Math.max(1, Long.parseLong(q[3]));
            bar(g, bx + 32, my + 11, 220, (float) have / need, have >= need ? 0xFF96F0AA : 0xFFFFC83C);
        }
        bar(g, bx + 60, by + 112, 160, num(f, 2) / 100f, 0xFFFF785A);
        // 기여 순위
        int rx = x + 276;
        UiKit.round(g, rx, by, W - 284, 64, 4, 0xFF8C643C);
        UiKit.round(g, rx + 1, by + 1, W - 286, 62, 4, 0xFFFCF6E6);
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float f, int c) {
        UiKit.round(g, x, y, w, 6, 3, 0xFF1E3C82);
        UiKit.round(g, x, y, Math.max(6, (int) (w * Math.min(1, f))), 6, 3, c);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var font = UiKit.font();
        UiKit.bold(g, "🏘 마을 발전 게시판 · 함께 짓는 우리 마을", 10, 7, 0xFFFFFFFF);
        UiKit.text(g, "재료와 돈을 기부하면 건물이 한 단계씩 올라가요 · 완공되면 서버 모두에게 효과", 10, 19, 0xFFD2E1FF);
        for (int i = 0; i < N && i < menu.entries.size(); i++) {
            String[] f = p(i);
            int cx = 8 + i * 81;
            UiKit.center(g, menu.entries.get(i).title(), cx + 38, 87, 0xFF3C2819);
            if (num(f, 0) >= 3) UiKit.center(g, "완공", cx + 59, 43, 0xFFFFFFFF);
            if (f.length > 1 && f[1].equals("0")) UiKit.center(g, "🔒 다음 차례", cx + 38, 58, 0xFFFFFFFF);
        }
        String[] f = p(sel);
        int st = num(f, 0);
        String name = sel < menu.entries.size() ? menu.entries.get(sel).title() : "";
        String head = st >= 3 ? "📐 " + name + " · 완공!" : f.length > 6 && f[6].equals("1") ? "📐 " + name + " · 1단계 준비 끝 → 공사 부지 상자를 놓아 주세요"
                : "📐 " + name + " · " + (st == 0 ? "1단계 터 닦기" : st == 1 ? "2단계 골조" : "3단계 완공") + " 모으는 중";
        UiKit.bold(g, head, 16, 124, 0xFFFFFFFF);
        g.text(font, "완공 효과: " + (f.length > 3 ? f[3] : ""), 16, 136, 0xFFC8DCFF, false);
        String[] mats = f.length > 4 ? f[4].split(";") : new String[0];
        for (int m = 0; m < mats.length && m < 3; m++) {
            String[] q = mats[m].split("\\|");
            if (q.length < 4) continue;
            int my = 118 + 34 + m * 24;
            long have = Long.parseLong(q[2]), need = Long.parseLong(q[3]);
            g.text(font, q[0], 40, my, 0xFFFFFFFF, false);
            String t = String.format("%,d / %,d%s%s", have, need, q[0].equals("돈") ? "동" : "", have >= need ? " ✔" : "");
            UiKit.right(g, t, 252, my, have >= need ? 0xFF96F0AA : 0xFFFFFFFF);
        }
        UiKit.bold(g, "전체 진행", 16, 228, 0xFFFFFFFF);
        UiKit.right(g, num(f, 2) + "%", 260, 228, 0xFFFFC83C);
        // 기여 순위
        int rx = 276;
        UiKit.bold(g, "🏅 기여 순위", rx + 8, 123, 0xFF3C2819);
        String[] medal = {"🥇", "🥈", "🥉", "4"};
        for (int k = 0; k + N < menu.entries.size() && k < 4; k++) {
            PickMenu.Entry e = menu.entries.get(N + k);
            g.text(font, medal[k] + " " + e.title(), rx + 8, 136 + k * 11, 0xFF3C2819, false);
            UiKit.right(g, e.sub(), W - 12, 136 + k * 11, 0xFF6E5A46);
        }
        if (menu.entries.size() <= N) g.text(font, "아직 기부한 사람이 없어요", rx + 8, 140, 0xFF8A7A66, false);
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
            if (isHoveredOrFocused()) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x14304080);
        }
    }
}

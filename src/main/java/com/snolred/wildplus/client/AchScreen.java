package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 4.9.2 업적 전용 화면: 남색 바탕 + 금색 테두리의 "명예의 전당".
 * 위: 전체 달성 막대 · 분류 탭(전체/전투/생활/경제/도전/수집) · 가운데: 업적 카드 2열 × 4줄 (메달 · 이름 · 설명 · 진행 막대 · 보상).
 * 서버 데이터: 0번 = 돌아가기, 그 뒤 업적마다 (이름 / 설명\n현재\n목표\n보상\n칭호\n달성(1/0)\n분류 번호)
 */
public class AchScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 372, H = 232, COLS = 2, ROWS = 4, PER = COLS * ROWS, CW = 174, CH = 35;
    private static final int GOLD = 0xFFE8B84A, GOLD_D = 0xFF8A6420, NAVY = 0xFF141C30, NAVY_L = 0xFF222E4C, INK = 0xFFE8ECF6, SOFT = 0xFF9AA6C0, GREEN = 0xFF4CD08A;
    private static final String[] TABS = {"전체", "전투", "생활", "경제", "도전", "수집"};

    private record A(String name, String desc, long cur, long goal, String coin, String title, boolean done, int group, ItemStack icon) {}

    private final List<A> all = new ArrayList<>();
    private final List<UiButton> tabBtns = new ArrayList<>();
    private UiButton prev, next;
    private int tab, page;

    public AchScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        for (int i = 1; i < menu.entries.size(); i++) {
            PickMenu.Entry e = menu.entries.get(i);
            String[] f = e.sub().split("\n", -1);
            if (f.length < 7) continue;
            all.add(new A(e.title(), f[0], num(f[1]), num(f[2]), f[3], f[4], f[5].equals("1"), (int) num(f[6]), e.icon()));
        }
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<A> shown() {
        List<A> out = new ArrayList<>();
        for (A a : all) if (tab == 0 || a.group() == tab) out.add(a);
        return out;
    }

    private int pages() {
        return Math.max(1, (shown().size() + PER - 1) / PER);
    }

    private int doneCount(int group) {
        int n = 0;
        for (A a : all) if ((group == 0 || a.group() == group) && a.done()) n++;
        return n;
    }

    @Override
    protected void init() {
        super.init();
        tabBtns.clear();
        for (int t = 0; t < TABS.length; t++) {
            final int k = t;
            tabBtns.add(addRenderableWidget(UiButton.of(leftPos + 10 + t * 46, topPos + 34, 44, 15, TABS[t], UiButton.DARK, () -> {
                tab = k;
                page = 0;
                refresh();
            })));
        }
        int by = topPos + H - 22;
        addRenderableWidget(UiButton.of(leftPos + 10, by, 74, 16, "◀ 모험 수첩", UiButton.GOLD, () -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }));
        prev = addRenderableWidget(UiButton.of(leftPos + W - 100, by, 20, 16, "◀", UiButton.WHITE, () -> { page--; refresh(); }));
        next = addRenderableWidget(UiButton.of(leftPos + W - 30, by, 20, 16, "▶", UiButton.WHITE, () -> { page++; refresh(); }));
        refresh();
    }

    private void refresh() {
        page = Math.max(0, Math.min(page, pages() - 1));
        prev.active = page > 0;
        next.active = page < pages() - 1;
        for (int t = 0; t < tabBtns.size(); t++) tabBtns.get(t).setSelected(t == tab);
    }

    private static String fit(String s, int maxW) {
        var font = UiKit.font();
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        var font = UiKit.font();
        int x = leftPos, y = topPos;
        // 틀
        UiKit.round(g, x + 2, y + 3, W, H, 5, 0x77000000);
        UiKit.round(g, x, y, W, H, 5, GOLD_D);
        UiKit.round(g, x + 1, y + 1, W - 2, H - 2, 4, GOLD);
        UiKit.roundGradient(g, x + 3, y + 3, W - 6, H - 6, 3, 0xFF1A2440, NAVY);
        UiKit.speckle(g, x + 3, y + 3, W - 6, H - 6, 11, 70, 0x18FFFFFF, 0x10E8B84A);
        // 머리말
        UiKit.big(g, "🏆 업적", x + 12, y + 9, 2, GOLD);
        int total = all.size(), done = doneCount(0);
        String cnt = done + " / " + total;
        UiKit.right(g, cnt, x + W - 12, y + 9, INK);
        int bx = x + 150, bw = W - 162 - font.width(cnt) - 6;
        UiKit.round(g, bx, y + 10, bw, 7, 3, 0xFF0C1222);
        if (done > 0) UiKit.roundGradient(g, bx, y + 10, Math.max(6, bw * done / Math.max(1, total)), 7, 3, 0xFFFFDE85, GOLD);
        UiKit.text(g, "보상은 달성하는 순간 통장으로 들어와요", x + 150, y + 21, SOFT);
        // 탭 옆: 이 분류 달성 수
        if (tab != 0) {
            int tn = 0;
            for (A a : all) if (a.group() == tab) tn++;
            UiKit.right(g, TABS[tab] + " " + doneCount(tab) + " / " + tn, x + W - 12, y + 38, SOFT);
        }
        g.fill(x + 8, y + 52, x + W - 8, y + 53, 0x55E8B84A);
        // 카드
        List<A> list = shown();
        for (int r = 0; r < PER; r++) {
            int k = page * PER + r;
            if (k >= list.size()) break;
            A a = list.get(k);
            int cx = x + 9 + (r % COLS) * (CW + 6), cy = y + 55 + (r / COLS) * (CH + 2);
            UiKit.round(g, cx, cy, CW, CH, 3, a.done() ? GOLD : 0xFF3A4868);
            UiKit.roundGradient(g, cx + 1, cy + 1, CW - 2, CH - 2, 2, a.done() ? 0xFF3A3420 : NAVY_L, a.done() ? 0xFF2A2616 : 0xFF1A2238);
            // 메달
            UiKit.circle(g, cx + 5, cy + 5, 24, a.done() ? GOLD_D : 0xFF10182A);
            UiKit.circle(g, cx + 6, cy + 6, 22, a.done() ? 0xFFFFDE85 : 0xFF2C3854);
            g.item(a.icon(), cx + 9, cy + 9);
            if (!a.done()) UiKit.circle(g, cx + 6, cy + 6, 22, 0x99141C30);
            int tx = cx + 34, tw = CW - 40;
            // 보상 (오른쪽 위)
            String rew = a.coin() + "동";
            int rw = font.width(rew);
            UiKit.right(g, rew, cx + CW - 5, cy + 4, a.done() ? 0xFF8A7A50 : 0xFFFFDE85);
            UiKit.text(g, fit(a.name(), tw - rw - 4), tx, cy + 4, a.done() ? 0xFFFFE9A8 : INK);
            // 설명 + 칭호
            String tl = a.title().isEmpty() ? "" : "[" + a.title() + "]";
            int tlw = tl.isEmpty() ? 0 : font.width(tl) + 4;
            if (!tl.isEmpty()) UiKit.right(g, tl, cx + CW - 5, cy + 14, a.done() ? 0xFF7FE0D0 : 0xFF5E86A0);
            UiKit.text(g, fit(a.desc(), tw - tlw), tx, cy + 14, SOFT);
            // 진행 막대
            int px = tx, py = cy + 25, pw = tw;
            if (a.done()) {
                UiKit.round(g, px, py - 2, pw, 9, 3, 0xFF2E7A52);
                UiKit.round(g, px, py - 2, pw, 8, 3, GREEN);
                UiKit.center(g, "✔ 달성", px + pw / 2, py - 2, 0xFF0C3A22);
            } else {
                String pr = String.format("%,d / %,d", Math.min(a.cur(), a.goal()), a.goal());
                int prw = font.width(pr) + 4;
                UiKit.round(g, px, py + 1, pw - prw, 4, 2, 0xFF0C1222);
                long f = a.goal() <= 0 ? 0 : (pw - prw) * Math.min(a.cur(), a.goal()) / a.goal();
                if (f > 0) UiKit.round(g, px, py + 1, (int) Math.max(3, f), 4, 2, 0xFF4C8DF6);
                UiKit.right(g, pr, cx + CW - 5, py - 1, 0xFFB8C4DC);
            }
        }
        if (list.isEmpty()) UiKit.center(g, "이 분류에는 업적이 없어요", x + W / 2, y + 120, SOFT);
        UiKit.center(g, (page + 1) + " / " + pages(), x + W - 55, y + H - 18, INK);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {}
}

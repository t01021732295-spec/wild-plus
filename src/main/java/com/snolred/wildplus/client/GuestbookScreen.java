package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 3.4 방명록: 펼친 가죽 책.
 * 왼쪽 쪽: 집 이름 · 주인 · 평균 별점 · 집 점수 막대 · 예쁜 집 리본 / 손님은 별 다섯 개를 눌러 별점 + 한 줄 후기 → [✍ 서명하기],
 * 주인은 집 이름 바꾸기. 오른쪽 쪽: 손님들 후기 (손글씨 · 별 · 날짜 · 주인 답글은 빨간 잉크) 3개씩, 모서리를 눌러 넘김.
 * 주인은 후기를 누르면 아래 칸에 답글을 쓴다. 쓰기는 명령으로 보냄: /house review · reply · name
 * 서버 데이터: 첫 줄 = 집 (이름 / 주인\n평균\n개수\n점수\n점수 내역\n예쁜집\n내 집\n오늘), 다음 줄들 = 후기 (이름 / 별\n날\n글\n답글)
 */
public class GuestbookScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 384, H = 232, PER = 3;
    private static final int INK = 0xFF23305A, SOFT_INK = 0xFF6A6F86, RED_INK = 0xFFB0283A, PAGE = 0xFFFBF3E0, PAGE_EDGE = 0xFFE9DCC0, STAR = 0xFFE8A92A;
    private final String name, owner, breakdown;
    private final double avg;
    private final int count, score;
    private final boolean best, mine;
    private int page, stars = 5, selected = -1;
    private EditBox text, reply;
    private UiButton sign, replyBtn, prev, next;
    private final List<UiButton> starBtns = new ArrayList<>(), entryBtns = new ArrayList<>();

    public GuestbookScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        PickMenu.Entry info = menu.entries.isEmpty() ? new PickMenu.Entry("집", "") : menu.entries.get(0);
        String[] f = info.sub().split("\n", -1);
        name = info.title();
        owner = f.length > 0 ? f[0] : "";
        avg = f.length > 1 ? parse(f[1]) : 0;
        count = f.length > 2 ? (int) parse(f[2]) : 0;
        score = f.length > 3 ? (int) parse(f[3]) : 0;
        breakdown = f.length > 4 ? f[4] : "";
        best = f.length > 5 && f[5].equals("1");
        mine = f.length > 6 && f[6].equals("1");
    }

    private static double parse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int reviews() {
        return Math.max(0, menu.entries.size() - 1);
    }

    private int pages() {
        return Math.max(1, (reviews() + PER - 1) / PER);
    }

    private String[] review(int i) {
        PickMenu.Entry e = menu.entries.get(i + 1);
        String[] f = e.sub().split("\n", -1);
        return new String[]{e.title(), f.length > 0 ? f[0] : "0", f.length > 1 ? f[1] : "", f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : ""};
    }

    @Override
    protected void init() {
        super.init();
        starBtns.clear();
        entryBtns.clear();
        int lx = leftPos + 18, rx = leftPos + W / 2 + 10;
        if (!mine) {
            for (int s = 1; s <= 5; s++) {
                final int k = s;
                starBtns.add(addRenderableWidget(new Clear(lx + 34 + (s - 1) * 16, topPos + 150, 15, 14, () -> stars = k)));
            }
            text = addRenderableWidget(new EditBox(this.font, lx, topPos + 170, W / 2 - 34, 16, Component.literal("후기")));
            text.setMaxLength(60);
            text.setHint(Component.literal("한 줄 후기 (60자까지 · 비워도 돼요)"));
            sign = addRenderableWidget(UiButton.of(lx, topPos + 191, W / 2 - 34, 18, "✍ 서명하기", UiButton.BLUE, () -> {
                String t = text.getValue().trim();
                send("house review " + stars + (t.isEmpty() ? "" : " " + t));
            }));
        } else {
            text = addRenderableWidget(new EditBox(this.font, lx, topPos + 170, W / 2 - 34, 16, Component.literal("집 이름")));
            text.setMaxLength(20);
            text.setHint(Component.literal("새 집 이름 (20자까지)"));
            sign = addRenderableWidget(UiButton.of(lx, topPos + 191, W / 2 - 34, 18, "🏷 집 이름 바꾸기", UiButton.TEAL, () -> {
                String t = text.getValue().trim();
                if (!t.isEmpty()) send("house name " + t);
            }));
            reply = addRenderableWidget(new EditBox(this.font, rx, topPos + 170, W / 2 - 34, 16, Component.literal("답글")));
            reply.setMaxLength(60);
            reply.setHint(Component.literal("후기를 누르고 답글 쓰기"));
            replyBtn = addRenderableWidget(UiButton.of(rx, topPos + 191, W / 2 - 34, 18, "💬 답글 달기", UiButton.ORANGE, () -> {
                String t = reply.getValue().trim();
                if (selected >= 0 && !t.isEmpty()) send("house reply " + selected + " " + t);
            }));
        }
        for (int r = 0; r < PER; r++) {
            final int row = r;
            entryBtns.add(addRenderableWidget(new Clear(rx - 4, topPos + 30 + r * (mine ? 44 : 54), W / 2 - 26, mine ? 42 : 52, () -> {
                int idx = page * PER + row;
                if (mine && idx < reviews()) selected = idx;
            })));
        }
        prev = addRenderableWidget(UiButton.of(leftPos + W / 2 + 6, topPos + H - 26, 18, 15, "◀", UiButton.WHITE, () -> { page--; refresh(); }));
        next = addRenderableWidget(UiButton.of(leftPos + W - 28, topPos + H - 26, 18, 15, "▶", UiButton.WHITE, () -> { page++; refresh(); }));
        refresh();
    }

    private void send(String cmd) {
        if (this.minecraft != null && this.minecraft.player != null) this.minecraft.player.connection.sendCommand(cmd);
    }

    private void refresh() {
        page = Math.max(0, Math.min(page, pages() - 1));
        prev.active = page > 0;
        next.active = page < pages() - 1;
        for (int r = 0; r < entryBtns.size(); r++) entryBtns.get(r).visible = page * PER + r < reviews();
        if (replyBtn != null) replyBtn.active = selected >= 0;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (replyBtn != null) replyBtn.active = selected >= 0;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        for (EditBox b : new EditBox[]{text, reply}) {
            if (b != null && b.isFocused() && !event.isEscape()) {
                b.keyPressed(event);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos, mid = x + W / 2;
        // 가죽 표지
        UiKit.round(g, x + 2, y + 3, W, H, 6, 0x66000000);
        UiKit.roundGradient(g, x, y, W, H, 6, 0xFF7A3B22, 0xFF4E2412);
        UiKit.speckle(g, x, y, W, H, 7, 260, 0x22000000, 0x14FFFFFF);
        UiKit.outline(g, x + 4, y + 4, W - 8, H - 8, 0x66E8C060);
        // 종이 (여러 장 겹친 가장자리)
        for (int k = 3; k >= 1; k--) g.fill(x + 10 + k, y + 10 + k, x + W - 10 + k, y + H - 10 + k, PAGE_EDGE);
        g.fill(x + 10, y + 10, x + W - 10, y + H - 10, PAGE);
        // 줄 (오른쪽 쪽) · 가운데 접힌 그늘
        for (int ly = y + 40; ly < y + H - 30; ly += 11) g.fill(mid + 8, ly, x + W - 18, ly + 1, 0x18304080);
        g.fillGradient(mid - 10, y + 10, mid, y + H - 10, 0x00000000, 0x30000000);
        g.fillGradient(mid, y + 10, mid + 10, y + H - 10, 0x30000000, 0x00000000);
        g.fill(mid, y + 10, mid + 1, y + H - 10, 0x40000000);
        // 빨간 책갈피 끈
        g.fill(mid + 30, y + 6, mid + 34, y + 36, 0xFFB0283A);
        for (int k = 0; k < 3; k++) g.fill(mid + 30 + k, y + 36 + k, mid + 34 - k, y + 37 + k, 0xFFB0283A);
        // 왼쪽: 집 점수 막대
        int bx = x + 18, by = y + 108, bw = W / 2 - 34;
        UiKit.round(g, bx, by, bw, 8, 3, 0xFFE2D6BC);
        UiKit.round(g, bx, by, Math.max(6, bw * score / 100), 8, 3, score >= 80 ? 0xFF2E9E5A : score >= 50 ? 0xFFE0A23A : 0xFFC0504A);
        // 예쁜 집 리본
        if (best) {
            int rx = x + W / 2 - 40, ry = y + 14;
            g.fill(rx, ry, rx + 22, ry + 16, 0xFFD0303A);
            g.fill(rx + 4, ry + 16, rx + 9, ry + 26, 0xFFB0283A);
            g.fill(rx + 13, ry + 16, rx + 18, ry + 26, 0xFFB0283A);
            UiKit.circle(g, rx + 5, ry + 2, 12, 0xFFF2C14E);
        }
        // 별 고르기 칸 (손님)
        if (!mine) g.fill(x + 16, y + 146, x + W / 2 - 14, y + 167, 0x14000000);
        // 고른 후기 (주인)
        if (mine && selected >= page * PER && selected < page * PER + PER) {
            int r = selected - page * PER;
            UiKit.outline(g, mid + 6, y + 30 + r * 44 - 2, W / 2 - 22, 44, 0xAAB0283A);
        }
    }

    private void drawStars(GuiGraphicsExtractor g, int x, int y, double v, int scale) {
        for (int i = 0; i < 5; i++) {
            boolean full = v >= i + 0.75, half = !full && v >= i + 0.25;
            String s = full ? "★" : half ? "★" : "☆";
            int c = full ? STAR : half ? 0xFFF0CF80 : 0xFFC9B99A;
            if (scale > 1) UiKit.big(g, s, x + i * 10 * scale, y, scale, c);
            else g.text(UiKit.font(), s, x + i * 9, y, c, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var font = UiKit.font();
        int lx = 18, rx = W / 2 + 10, lw = W / 2 - 34;
        // ── 왼쪽 쪽 ──
        g.text(font, "📖 방명록", lx, 16, SOFT_INK, false);
        String nm = name;
        while (font.width(nm) * 2 > lw && nm.length() > 1) nm = nm.substring(0, nm.length() - 1);
        UiKit.big(g, nm, lx, 28, font.width(name) * 2 <= lw ? 2 : 1, INK);
        g.text(font, "주인 " + owner, lx, 50, SOFT_INK, false);
        drawStars(g, lx, 64, avg, 2);
        g.text(font, String.format("%.1f · 후기 %d개", avg, count), lx, 86, INK, false);
        g.text(font, "🏅 집 점수 " + score + " / 100", lx, 97, INK, false);
        List<String> bl = RealtyScreenWrap.wrap(breakdown, lw);
        for (int i = 0; i < Math.min(2, bl.size()); i++) g.text(font, bl.get(i), lx, 120 + i * 10, SOFT_INK, false);
        if (best) g.text(font, "🎀 이번 주 예쁜 집!", lx, 140, RED_INK, false);
        if (!mine) {
            g.text(font, "별점", lx, 153, INK, false);
            for (int s = 1; s <= 5; s++) g.text(font, s <= stars ? "★" : "☆", lx + 38 + (s - 1) * 16, 153, s <= stars ? STAR : 0xFFB8A888, false);
            String[] say = {"", "별로예요", "아쉬워요", "괜찮아요", "멋져요", "최고예요!"};
            g.text(font, say[stars], lx + 120, 153, SOFT_INK, false);
        } else {
            g.text(font, "✏ 집 이름 바꾸기", lx, 158, SOFT_INK, false);
            g.text(font, "💬 답글: 오른쪽 후기를 눌러요", rx, 158, SOFT_INK, false);
        }
        // ── 오른쪽 쪽: 후기 ──
        g.text(font, "손님 후기", rx, 16, SOFT_INK, false);
        if (reviews() == 0) {
            UiKit.center(g, "아직 아무도 다녀가지 않았어요", rx + (W / 2 - 30) / 2, 80, SOFT_INK);
            UiKit.center(g, mine ? "친구들을 집들이에 초대해 보세요!" : "첫 손님이 되어 주세요 ✍", rx + (W / 2 - 30) / 2, 92, SOFT_INK);
        }
        int rowH = mine ? 44 : 54;
        for (int r = 0; r < PER; r++) {
            int idx = page * PER + r;
            if (idx >= reviews()) break;
            String[] f = review(idx);
            int y = 30 + r * rowH;
            g.text(font, f[0], rx, y, INK, false);
            int st = (int) parse(f[1]);
            for (int s = 0; s < 5; s++) g.text(font, s < st ? "★" : "☆", rx + font.width(f[0]) + 6 + s * 8, y, s < st ? STAR : 0xFFC9B99A, false);
            UiKit.right(g, f[2] + "일차", W - 18, y, SOFT_INK);
            List<String> tl = RealtyScreenWrap.wrap(f[3].isEmpty() ? "(별점만 남겼어요)" : "“" + f[3] + "”", W / 2 - 34);
            for (int i = 0; i < Math.min(2, tl.size()); i++) g.text(font, tl.get(i), rx + 4, y + 11 + i * 10, f[3].isEmpty() ? SOFT_INK : INK, false);
            if (!f[4].isEmpty()) {
                String rp = "↳ 주인: " + f[4];
                while (font.width(rp) > W / 2 - 36 && rp.length() > 4) rp = rp.substring(0, rp.length() - 2) + "…";
                g.text(font, rp, rx + 8, y + 32, RED_INK, false);
            }
        }
        UiKit.center(g, (page + 1) + " / " + pages(), W / 2 + (W / 2) / 2, H - 22, SOFT_INK);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    /** 보이지 않는 누름 칸 (별 · 후기 줄) */
    private static final class Clear extends UiButton {
        Clear(int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.empty(), UiButton.WHITE, action);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            if (isHoveredOrFocused()) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x14304080);
        }
    }

    /** 단어 단위 줄바꿈 */
    private static final class RealtyScreenWrap {
        static List<String> wrap(String s, int maxW) {
            var font = UiKit.font();
            List<String> out = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            for (String word : s.split(" ")) {
                while (font.width(word) > maxW && word.length() > 1) { // 띄어쓰기 없이 긴 글은 글자 단위로
                    int n = word.length();
                    while (n > 1 && font.width(word.substring(0, n)) > maxW) n--;
                    if (!cur.isEmpty()) {
                        out.add(cur.toString());
                        cur = new StringBuilder();
                    }
                    out.add(word.substring(0, n));
                    word = word.substring(n);
                }
                String t = cur.isEmpty() ? word : cur + " " + word;
                if (font.width(t) > maxW && !cur.isEmpty()) {
                    out.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    cur = new StringBuilder(t);
                }
            }
            if (!cur.isEmpty()) out.add(cur.toString());
            return out;
        }
    }
}

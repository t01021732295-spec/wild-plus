package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 3.4 부동산 중개소 화면: 진짜 부동산처럼.
 * 왼쪽: 매물 카드 (사진 · 이름 · 가격 · 꼬리표) 5장씩 / 오른쪽: 고른 매물의 큰 사진 · 상세 정보 · 가격 ·
 * [📝 계약하기] → [✍ 서명하고 계약] (두 번 눌러야 결제, 실수 방지).
 * 서버가 보내는 줄 글(sub): "가격\n꼬리표\n상세 1\n상세 2 …"
 */
public class RealtyScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 372, H = 236, CARDS = 5, CARD_H = 34, LIST_W = 168;
    private static final int BG_TOP = 0xFF2B3A55, BG_BOTTOM = 0xFF1A2438, PAPER = 0xFFFBF8F1, INK = 0xFF26303F, SOFT = 0xFF6B7484;
    private final List<CardButton> cards = new ArrayList<>();
    private UiButton prev, next, deal;
    private int page, selected = 0;
    private boolean confirm;

    public RealtyScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        // 첫 매물: 살 수 있는 첫 땅 → 없으면 첫 줄
        for (int i = 0; i < menu.entries.size(); i++) {
            if (menu.entries.get(i).kind() == PickMenu.K_PRIMARY) {
                selected = i;
                break;
            }
        }
        page = selected / CARDS;
    }

    private String[] lines(PickMenu.Entry e) {
        return e.sub().split("\n");
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();
        for (int r = 0; r < CARDS; r++) {
            final int row = r;
            cards.add(addRenderableWidget(new CardButton(leftPos + 8, topPos + 34 + r * (CARD_H + 2), () -> select(page * CARDS + row))));
        }
        prev = addRenderableWidget(UiButton.of(leftPos + 8, topPos + H - 22, 22, 16, "◀", UiButton.WHITE, () -> { page--; refresh(); }));
        next = addRenderableWidget(UiButton.of(leftPos + 34, topPos + H - 22, 22, 16, "▶", UiButton.WHITE, () -> { page++; refresh(); }));
        deal = addRenderableWidget(new UiButton(leftPos + LIST_W + 20, topPos + H - 30, W - LIST_W - 28, 22, Component.empty(), UiButton.GREEN, this::press));
        refresh();
    }

    private int pages() {
        return Math.max(1, (menu.entries.size() + CARDS - 1) / CARDS);
    }

    private void select(int idx) {
        if (idx < 0 || idx >= menu.entries.size()) return;
        selected = idx;
        confirm = false;
        refresh();
    }

    private void press() {
        if (selected < 0 || selected >= menu.entries.size()) return;
        int kind = menu.entries.get(selected).kind();
        if (kind == PickMenu.K_SMALL || confirm) {
            confirm = false;
            if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selected);
        } else {
            confirm = true;
        }
        refresh();
    }

    private void refresh() {
        page = Math.max(0, Math.min(page, pages() - 1));
        for (int r = 0; r < cards.size(); r++) {
            int idx = page * CARDS + r;
            CardButton c = cards.get(r);
            c.visible = idx < menu.entries.size();
            c.entry = c.visible ? menu.entries.get(idx) : null;
            c.on = idx == selected;
        }
        prev.active = page > 0;
        next.active = page < pages() - 1;
        PickMenu.Entry e = selected < menu.entries.size() ? menu.entries.get(selected) : null;
        int kind = e == null ? PickMenu.K_INFO : e.kind();
        deal.visible = kind != PickMenu.K_INFO;
        if (kind == PickMenu.K_SMALL) {
            deal.style(UiButton.BLUE).setMessage(Component.literal("📂 열기"));
        } else if (kind == PickMenu.K_DANGER) {
            deal.style(confirm ? UiButton.RED : UiButton.ORANGE).setMessage(Component.literal(confirm ? "✍ 정말 내놓을까요? (한 번 더)" : "💸 매물로 내놓기"));
        } else {
            deal.style(confirm ? UiButton.ORANGE : UiButton.GREEN).setMessage(Component.literal(confirm ? "✍ 계약서에 서명 (한 번 더 누르면 결제)" : "📝 계약하기"));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 사무소 벽 (남색) + 나무 테
        UiKit.device(g, x, y, W, H, BG_TOP, BG_BOTTOM);
        g.fillGradient(x + 3, y + 3, x + W - 3, y + 28, 0xFFC8343E, 0xFF8E1C26);   // 빨간 간판 띠
        g.fill(x + 3, y + 28, x + W - 3, y + 30, 0xFFE8C060);
        // 왼쪽 목록 판 (코르크)
        g.fillGradient(x + 5, y + 31, x + LIST_W + 12, y + H - 26, 0xFFC99A64, 0xFFAE7F4C);
        UiKit.speckle(g, x + 5, y + 31, LIST_W + 7, H - 57, 31, 180, 0x22603A14, 0x18FFFFFF);
        // 오른쪽 상세 (흰 서류)
        int dx = x + LIST_W + 16, dw = W - LIST_W - 22;
        UiKit.round(g, dx + 1, y + 35, dw, H - 70, 3, 0x55000000);
        UiKit.round(g, dx, y + 34, dw, H - 70, 3, PAPER);
        PickMenu.Entry e = selected < menu.entries.size() ? menu.entries.get(selected) : null;
        if (e == null) return;
        // 매물 사진: 하늘 · 잔디 위에 큰 물건 그림
        int px = dx + 6, py = y + 40, pw = 64, ph = 56;
        g.fillGradient(px, py, px + pw, py + ph * 2 / 3, 0xFF8EC8F6, 0xFFD6ECFC);
        g.fillGradient(px, py + ph * 2 / 3, px + pw, py + ph, 0xFF7CC05A, 0xFF5A9A3C);
        UiKit.outline(g, px - 1, py - 1, pw + 2, ph + 2, 0xFFB0B8C4);
        g.pose().pushMatrix();
        g.pose().translate(px + pw / 2F - 24, py + 6);
        g.pose().scale(3F, 3F);
        g.item(e.icon(), 0, 0);
        g.pose().popMatrix();
        // 꼬리표 도장
        String[] ls = lines(e);
        String tag = ls.length > 1 ? ls[1] : "";
        int tagColor = e.kind() == PickMenu.K_INFO ? 0xFF8A8F98 : e.kind() == PickMenu.K_DANGER ? 0xFFE0661A : e.kind() == PickMenu.K_SMALL ? 0xFF3B82F6 : 0xFF22A35A;
        int tw = UiKit.font().width(tag) + 8;
        UiKit.round(g, px + pw + 6, py + 2, tw, 12, 2, tagColor);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var font = UiKit.font();
        UiKit.bold(g, "🏢 " + this.title.getString().replace("🏢", "").trim(), 9, 7, 0xFFFFF4DA);
        UiKit.text(g, menu.header, 9, 18, 0xFFFFD6D6);
        UiKit.right(g, "REAL ESTATE", W - 9, 7, 0x88FFE9B0);
        UiKit.text(g, (page + 1) + " / " + pages() + "  · 카드를 눌러 매물 보기", 60, H - 18, 0xFFE6EDF3);
        PickMenu.Entry e = selected < menu.entries.size() ? menu.entries.get(selected) : null;
        if (e == null) return;
        String[] ls = lines(e);
        int dx = LIST_W + 16, px = dx + 6, py = 40, pw = 64;
        String tag = ls.length > 1 ? ls[1] : "";
        g.text(font, tag, px + pw + 10, py + 4, 0xFFFFFFFF, false);
        // 이름 (두 줄까지)
        String name = e.title();
        int maxW = W - (px + pw + 10) - 10;
        List<String> nm = wrap(name, maxW);
        for (int i = 0; i < Math.min(2, nm.size()); i++) UiKit.bold(g, nm.get(i), px + pw + 8, py + 18 + i * 10, INK);
        // 가격 (크게)
        String price = ls.length > 0 ? ls[0] : "";
        int priceColor = e.kind() == PickMenu.K_INFO ? SOFT : 0xFFC0392B;
        UiKit.big(g, price, px + pw + 8, py + 40, 1, priceColor);
        // 상세 줄
        int ly = py + 62;
        UiKit.hline(g, dx + 6, ly - 3, W - LIST_W - 34, 0xFFE2DED4);
        for (int i = 2; i < ls.length && ly < H - 40; i++) {
            for (String w : wrap("• " + ls[i], W - dx - 20)) {
                if (ly >= H - 40) break;
                g.text(font, w, dx + 8, ly, i == 2 ? INK : SOFT, false);
                ly += 10;
            }
        }
    }

    private static List<String> wrap(String s, int maxW) {
        var font = UiKit.font();
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    /** 매물 카드: 왼쪽 사진 · 이름 · 가격 · 꼬리표 (고르면 파란 테) */
    private final class CardButton extends UiButton {
        PickMenu.Entry entry;
        boolean on;

        CardButton(int x, int y, Runnable action) {
            super(x, y, LIST_W, CARD_H, Component.empty(), UiButton.WHITE, action);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            if (entry == null) return;
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hover = isHoveredOrFocused();
            boolean sold = entry.kind() == PickMenu.K_INFO;
            // 압정 + 카드
            g.fill(x + 2, y + 2, x + w + 2, y + h + 1, 0x44000000);
            g.fill(x, y, x + w, y + h, sold ? 0xFFE9E6DF : hover ? 0xFFFFFDF4 : 0xFFFFFFFF);
            UiKit.outline(g, x, y, w, h, on ? 0xFF2563D9 : 0xFFD0C8B8);
            if (on) UiKit.outline(g, x + 1, y + 1, w - 2, h - 2, 0xFF7FB0FF);
            g.fill(x + w / 2 - 2, y - 2, x + w / 2 + 2, y + 2, 0xFFD0303A);
            // 사진
            g.fillGradient(x + 3, y + 3, x + 31, y + 22, 0xFF9CCFF7, 0xFFD6ECFC);
            g.fillGradient(x + 3, y + 22, x + 31, y + h - 3, 0xFF7CC05A, 0xFF5A9A3C);
            g.item(entry.icon(), x + 9, y + 9);
            var font = UiKit.font();
            String[] ls = entry.sub().split("\n");
            String t = entry.title();
            while (font.width(t) > w - 40 && t.length() > 2) t = t.substring(0, t.length() - 2) + "…";
            g.text(font, t, x + 35, y + 5, sold ? 0xFF8A8F98 : INK, false);
            String price = ls.length > 0 ? ls[0] : "";
            g.text(font, price, x + 35, y + 19, sold ? 0xFF8A8F98 : 0xFFC0392B, false);
            String tag = ls.length > 1 ? ls[1] : "";
            int tw = font.width(tag);
            if (tw < w - 40 - font.width(price) - 8) {
                int c = sold ? 0xFF9AA0A8 : entry.kind() == PickMenu.K_DANGER ? 0xFFE0661A : entry.kind() == PickMenu.K_SMALL ? 0xFF3B82F6 : 0xFF22A35A;
                g.fill(x + w - tw - 8, y + 18, x + w - 3, y + 28, c);
                g.text(font, tag, x + w - tw - 5, y + 19, 0xFFFFFFFF, false);
            }
            if (sold) { // 계약 완료 도장
                UiKit.outline(g, x + w - 46, y + 3, 42, 12, 0xCCC0392B);
                g.text(font, "계약완료", x + w - 44, y + 5, 0xFFC0392B, false);
            }
        }
    }
}

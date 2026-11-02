package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 목록에서 하나를 고르는 화면. 테마 셋:
 *  - 기본: 키오스크 (워프 목적지, 텔레포트 대상 …) — 한 쪽에 7줄
 *  - 투기장: 돌기둥 콜로세움 + 붉은 깃발 + 횃불 — 한 쪽에 7줄
 *  - 진열장(명품관): 유리 케이스 안에 물건, 아래 이름 · 가격 — 한 쪽에 5×2칸
 */
public class PickScreen extends AbstractContainerScreen<PickMenu> {
    private static final int ROWS = 7;
    private static final int COLS = 5, GRID_ROWS = 2, CASE_W = 58, CASE_H = 92;
    /** 투기장 줄: 낡은 양피지 + 청동 테 */
    private static final UiButton.Style ARENA_ROW = new UiButton.Style(0xFFF7E6C0, 0xFFE6CC96, 0xFF6E4A1E, 0xFF3A2410, false,
            UiButton.Shape.PAPER);
    private static final UiButton.Style ARENA_KEY = new UiButton.Style(0xFFC9A86A, 0xFF8E6A32, 0xFF3E2A10, 0xFFFFF4DA, true,
            UiButton.Shape.SOFT);
    private final boolean arena, shop;
    private final int W;
    /** 목록 줄: 흰 카드, 마우스 올리면 옅은 하늘색 */
    private static final UiButton.Style ROW_STYLE = new UiButton.Style(0xFFFFFFFF, 0xFFF1F4F8, 0xFFC9D1DC, 0xFF1E2430, false,
            UiButton.Shape.SOFT);
    private final int H;
    private net.minecraft.client.gui.components.EditBox nameBox;
    private int page;
    private final List<UiButton> rows = new ArrayList<>();
    private UiButton prev, next;

    public PickScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, (menu.flags & PickMenu.FLAG_SHOP) != 0 ? 330 : (menu.flags & PickMenu.FLAG_ARENA) != 0 ? 262 : 240,
                (menu.flags & PickMenu.FLAG_SHOP) != 0 ? 36 + GRID_ROWS * (CASE_H + 6) + 26
                        : (menu.flags & PickMenu.FLAG_CAN_TOGGLE) != 0 ? 222 : (menu.flags & PickMenu.FLAG_ARENA) != 0 ? 206 : 200);
        this.arena = (menu.flags & PickMenu.FLAG_ARENA) != 0;
        this.shop = (menu.flags & PickMenu.FLAG_SHOP) != 0;
        this.W = this.imageWidth;
        this.H = this.imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        if (shop) {
            int gx = leftPos + (W - (COLS * CASE_W + (COLS - 1) * 6)) / 2;
            for (int r = 0; r < COLS * GRID_ROWS; r++) {
                final int row = r;
                rows.add(addRenderableWidget(new CaseButton(gx + (r % COLS) * (CASE_W + 6), topPos + 36 + (r / COLS) * (CASE_H + 6), () -> pick(row))));
            }
        } else {
            int inset = arena ? 24 : 10;
            for (int r = 0; r < ROWS; r++) {
                final int row = r;
                rows.add(addRenderableWidget(new UiButton(leftPos + inset, topPos + (arena ? 38 : 32) + r * 20, W - inset * 2, 18, Component.empty(),
                        arena ? ARENA_ROW : ROW_STYLE, () -> pick(row)).left()));
            }
        }
        UiButton.Style key = shop ? UiButton.GOLD : arena ? ARENA_KEY : UiButton.STEEL;
        int px = leftPos + (arena ? 24 : 10);
        prev = addRenderableWidget(UiButton.of(px, topPos + H - 24, 20, 17, "◀", key, () -> { page--; refresh(); }));
        next = addRenderableWidget(UiButton.of(px + 24, topPos + H - 24, 20, 17, "▶", key, () -> { page++; refresh(); }));
        if ((menu.flags & PickMenu.FLAG_CAN_TOGGLE) != 0) {
            boolean pub = (menu.flags & PickMenu.FLAG_PUBLIC) != 0;
            addRenderableWidget(new UiButton(leftPos + W - 110, topPos + H - 24, 100, 17,
                    Component.translatable(pub ? "gui.wildplus.warp.make_private" : "gui.wildplus.warp.make_public"),
                    pub ? UiButton.DARK : UiButton.TEAL, () -> click(PickMenu.BTN_TOGGLE)));
        }
        if ((menu.flags & PickMenu.FLAG_CAN_RENAME) != 0) {
            // 이름 바꾸기 (주인 · 공개 워프): 입력칸 + 버튼 → 서버 명령어 /warpname 으로 보낸다
            nameBox = addRenderableWidget(new net.minecraft.client.gui.components.EditBox(this.font, leftPos + 10, topPos + H - 47, W - 104, 16,
                    Component.translatable("gui.wildplus.warp.rename")));
            nameBox.setMaxLength(32);
            nameBox.setHint(Component.translatable("gui.wildplus.warp.rename_hint"));
            addRenderableWidget(new UiButton(leftPos + W - 90, topPos + H - 48, 80, 17, Component.translatable("gui.wildplus.warp.rename"),
                    UiButton.BLUE, () -> {
                        String name = nameBox.getValue().trim();
                        if (!name.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.connection.sendCommand("warpname " + name);
                        }
                    }));
        }
        refresh();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        // 이름 입력 중에는 E 키 등으로 창이 닫히지 않게
        if (nameBox != null && nameBox.isFocused() && !event.isEscape()) {
            nameBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    private int per() {
        return shop ? COLS * GRID_ROWS : ROWS;
    }

    private int pages() {
        return Math.max(1, (menu.entries.size() + per() - 1) / per());
    }

    private void refresh() {
        page = Math.max(0, Math.min(page, pages() - 1));
        for (int r = 0; r < rows.size(); r++) {
            int idx = page * per() + r;
            UiButton b = rows.get(r);
            if (idx < menu.entries.size()) {
                PickMenu.Entry e = menu.entries.get(idx);
                b.visible = true;
                if (b instanceof CaseButton c) {
                    c.entry = e;
                    b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(e.title())
                            .append(e.sub().isEmpty() ? Component.empty() : Component.literal("\n" + e.sub()).withStyle(ChatFormatting.GOLD))));
                } else {
                    b.setMessage(Component.literal(e.title()).append(Component.literal("   " + e.sub())
                            .withStyle(arena ? ChatFormatting.DARK_RED : ChatFormatting.GRAY)));
                }
            } else {
                b.visible = false;
            }
        }
        prev.active = page > 0;
        next.active = page < pages() - 1;
    }

    private void pick(int row) {
        int idx = page * per() + row;
        if (idx < menu.entries.size()) click(idx);
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        if (shop) {
            showroom(g, x, y);
            return;
        }
        if (arena) {
            colosseum(g, x, y);
            return;
        }
        // 키오스크 본체 (짙은 남색) + 흰 터치스크린
        UiKit.device(g, x, y, W, H, 0xFF2A3446, 0xFF161C27);
        UiKit.screen(g, x + 5, y + 5, W - 10, H - 10, 0xFFF6F8FB, 0xFFE6EBF2);
        // 앱 머리줄 (청록)
        g.fillGradient(x + 5, y + 5, x + W - 5, y + 27, 0xFF14A394, 0xFF0E7A70);
        g.fill(x + 5, y + 27, x + W - 5, y + 28, 0x33000000);
        UiKit.led(g, x + W - 14, y + 9, 0xFF7CFFDA);
        // 아래 조작줄
        UiKit.hline(g, x + 8, y + H - 28, W - 16, 0xFFD5DCE5);
    }

    /** 투기장: 돌벽돌 테 · 양쪽 돌기둥 · 붉은 깃발 머리줄 · 횃불 · 모래 바닥 */
    private void colosseum(GuiGraphicsExtractor g, int x, int y) {
        UiKit.device(g, x, y, W, H, 0xFF8C8173, 0xFF4E463D);
        // 돌벽돌
        for (int by = y + 3, k = 0; by < y + H - 3; by += 7, k++) {
            for (int bx = x + 3 - (k % 2) * 7; bx < x + W - 3; bx += 14) {
                int l = Math.max(bx, x + 3), r = Math.min(bx + 13, x + W - 3);
                if (r > l) g.fill(l, by, r, Math.min(by + 6, y + H - 3), ((bx / 14 + k) % 3 == 0) ? 0xFF7E7366 : 0xFF8A7F70);
            }
        }
        // 모래 경기장 바닥
        g.fillGradient(x + 20, y + 34, x + W - 20, y + H - 30, 0xFFEBD3A0, 0xFFD2B279);
        UiKit.speckle(g, x + 20, y + 34, W - 40, H - 64, 17, 220, 0x22704A1A, 0x18FFFFFF);
        UiKit.outline(g, x + 19, y + 33, W - 38, H - 62, 0xFF5A3E1E);
        // 돌기둥 (주두 · 기둥 홈 · 받침)
        for (int cx : new int[]{x + 4, x + W - 18}) {
            g.fillGradient(cx + 1, y + 36, cx + 13, y + H - 30, 0xFFE9E2D2, 0xFFBFB5A2);
            for (int k = 0; k < 3; k++) g.fill(cx + 3 + k * 4, y + 38, cx + 4 + k * 4, y + H - 32, 0x30000000);
            g.fill(cx - 1, y + 32, cx + 15, y + 37, 0xFFD8CFBC);
            g.fill(cx - 1, y + H - 31, cx + 15, y + H - 27, 0xFFB1A792);
            g.fill(cx - 1, y + 36, cx + 15, y + 37, 0x40000000);
        }
        // 붉은 깃발 머리줄 + 금 테
        g.fillGradient(x + 3, y + 3, x + W - 3, y + 30, 0xFFB3202A, 0xFF6E0E16);
        g.fill(x + 3, y + 30, x + W - 3, y + 32, 0xFFD9A22E);
        g.fill(x + 3, y + 3, x + W - 3, y + 4, 0xFFE8C060);
        // 늘어진 깃발 끝 (삼각 술)
        for (int bx = x + 8; bx < x + W - 8; bx += 12) {
            for (int k = 0; k < 4; k++) g.fill(bx + k, y + 32 + k, bx + 8 - k, y + 33 + k, 0xFF8E1520);
        }
        // 엇갈린 칼 (오른쪽 위)
        int sx = x + W - 40, sy = y + 7;
        for (int k = 0; k < 18; k++) {
            g.fill(sx + k, sy + k, sx + k + 2, sy + k + 2, 0xFFE6ECF2);
            g.fill(sx + 17 - k, sy + k, sx + 19 - k, sy + k + 2, 0xFFCBD3DC);
        }
        g.fill(sx + 2, sy + 13, sx + 7, sy + 15, 0xFFD9A22E);
        g.fill(sx + 12, sy + 13, sx + 17, sy + 15, 0xFFD9A22E);
        // 횃불 (좌우 아래)
        long t = net.minecraft.util.Util.getMillis() / 120;
        for (int tx : new int[]{x + 7, x + W - 15}) {
            int ty = y + H - 24;
            g.fill(tx + 2, ty + 6, tx + 6, ty + 18, 0xFF5A3A1E);
            g.fill(tx + 1, ty + 5, tx + 7, ty + 7, 0xFF8A8F96);
            int f = (int) ((t + tx) % 3);
            g.fill(tx + 1, ty - 1 + f % 2, tx + 7, ty + 5, 0xFFFF8A1E);
            g.fill(tx + 2, ty - 3 + f, tx + 6, ty + 3, 0xFFFFC340);
            g.fill(tx + 3, ty - 5 + f, tx + 5, ty + 1, 0xFFFFF0A0);
        }
    }

    /** 명품관: 짙은 원목 테 · 어두운 벽지 · 위에서 비추는 조명 · 금 간판 */
    private void showroom(GuiGraphicsExtractor g, int x, int y) {
        UiKit.device(g, x, y, W, H, 0xFF6A3E22, 0xFF2E1A0E);
        g.fillGradient(x + 4, y + 4, x + W - 4, y + H - 4, 0xFF2B1D2E, 0xFF140C16);
        // 벽지 다마스크 무늬
        for (int py = y + 34; py < y + H - 28; py += 12)
            for (int px = x + 8 + ((py / 12) % 2) * 6; px < x + W - 8; px += 12) {
                g.fill(px + 2, py, px + 3, py + 3, 0x22D9A22E);
                g.fill(px + 1, py + 1, px + 4, py + 2, 0x22D9A22E);
            }
        // 간판: 검은 판 + 금 테 두 줄
        g.fill(x + 4, y + 4, x + W - 4, y + 30, 0xFF0C0A0E);
        g.fill(x + 4, y + 29, x + W - 4, y + 30, 0xFFD9A22E);
        g.fill(x + 4, y + 31, x + W - 4, y + 32, 0x88D9A22E);
        // 진열장 위 조명 빛줄기
        int gx = x + (W - (COLS * CASE_W + (COLS - 1) * 6)) / 2;
        for (int r = 0; r < GRID_ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                int cx = gx + c * (CASE_W + 6) + CASE_W / 2, cy = y + 36 + r * (CASE_H + 6);
                for (int k = 0; k < 8; k++) g.fill(cx - 6 - k * 2, cy - 2 + k * 6, cx + 6 + k * 2, cy + 4 + k * 6, 0x08FFF2C8);
                g.fill(cx - 5, cy - 3, cx + 5, cy - 1, 0xFFFFF2C8);
            }
        // 아래 바닥 띠
        g.fillGradient(x + 4, y + H - 28, x + W - 4, y + H - 4, 0xFF3A2414, 0xFF1E120A);
        g.fill(x + 4, y + H - 28, x + W - 4, y + H - 27, 0xFFD9A22E);
    }

    /** 유리 진열장 한 칸: 벨벳 받침 위 물건, 앞 유리 반사, 아래 이름표 · 가격 */
    private final class CaseButton extends UiButton {
        PickMenu.Entry entry;

        CaseButton(int x, int y, Runnable action) {
            super(x, y, CASE_W, CASE_H, Component.empty(), UiButton.GOLD, action);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            if (entry == null) return;
            int x = getX(), y = getY(), w = getWidth();
            boolean hover = isHoveredOrFocused();
            int gh = 52; // 유리 높이
            // 그림자 + 금 받침대
            g.fill(x + 1, y + gh + 9, x + w + 1, y + gh + 12, 0x66000000);
            g.fillGradient(x, y + gh, x + w, y + gh + 10, 0xFFE8C060, 0xFF8E6416);
            g.fill(x, y + gh, x + w, y + gh + 1, 0xFFFFF0B0);
            // 유리 안쪽 (벨벳 뒷벽)
            g.fillGradient(x + 2, y + 2, x + w - 2, y + gh, 0xFF3A1430, 0xFF1C0A18);
            g.fillGradient(x + 6, y + gh - 10, x + w - 6, y + gh, 0xFF7A1840, 0xFF4A0C26); // 벨벳 쿠션
            // 물건 (2배, 마우스 올리면 살짝 떠오름)
            int bob = hover ? -2 : 0;
            g.pose().pushMatrix();
            g.pose().translate(x + w / 2F - 16, y + 12 + bob);
            g.pose().scale(2F, 2F);
            g.item(entry.icon(), 0, 0);
            g.pose().popMatrix();
            // 유리: 옅은 푸른 막 + 사선 반사 + 테
            g.fill(x + 2, y + 2, x + w - 2, y + gh, 0x1AB8E4FF);
            for (int k = 0; k < 14; k++) g.fill(x + 6 + k, y + 4 + k * 2, x + 9 + k, y + 6 + k * 2, 0x30FFFFFF);
            g.fill(x + w - 10, y + 4, x + w - 8, y + gh - 4, 0x26FFFFFF);
            int rim = hover ? 0xFFFFE27A : 0xFFB89040;
            UiKit.outline(g, x + 1, y + 1, w - 2, gh, rim);
            g.fill(x, y, x + w, y + 2, rim);
            if (hover) UiKit.outline(g, x - 1, y - 1, w + 2, CASE_H + 2, 0x88FFE27A);
            // 이름표: 이모지 · 상태 괄호 떼고 두 줄까지
            String name = entry.title();
            String state = "";
            int open = name.indexOf(" (");
            if (open > 0) {
                state = name.substring(open + 2, name.endsWith(")") ? name.length() - 1 : name.length());
                state = state.contains("착용") ? "착용 중" : state.contains("보유") ? "보유" : state;
                name = name.substring(0, open);
            }
            int sp = name.indexOf(' ');
            if (sp > 0 && sp <= 2 && !Character.isLetterOrDigit(name.charAt(0))) name = name.substring(sp + 1).trim();
            var font = UiKit.font();
            String l1 = name, l2 = "";
            if (font.width(name) > w - 2) {
                int cut = name.lastIndexOf(' ', name.length() / 2 + 3);
                if (cut <= 0) cut = Math.min(name.length(), 6);
                l1 = name.substring(0, cut).trim();
                l2 = name.substring(cut).trim();
                while (font.width(l2) > w - 2 && l2.length() > 1) l2 = l2.substring(0, l2.length() - 2) + "…";
            }
            int ty = y + gh + 13;
            g.text(font, l1, x + (w - font.width(l1)) / 2, ty, 0xFFF4E9D8, false);
            if (!l2.isEmpty()) g.text(font, l2, x + (w - font.width(l2)) / 2, ty + 9, 0xFFF4E9D8, false);
            // 가격 (보유 · 착용이면 초록 글씨)
            String price = !state.isEmpty() ? state : entry.sub().isEmpty() ? "무료" : entry.sub();
            int pc = !state.isEmpty() ? 0xFF7CE08A : 0xFFFFD25A;
            g.text(font, price, x + (w - font.width(price)) / 2, ty + (l2.isEmpty() ? 11 : 19), pc, true);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (shop) {
            UiKit.bold(g, "✦ " + this.title.getString().replace("💎", "").trim() + " ✦", 10, 8, 0xFFF2C14E);
            UiKit.text(g, menu.header, 10, 19, 0xFFB8A98A);
            UiKit.right(g, "LUXURY BOUTIQUE", W - 10, 8, 0x88D9A22E);
            UiKit.text(g, (page + 1) + " / " + pages(), 60, H - 19, 0xFFE8C060);
            UiKit.right(g, "진열장을 누르면 바로 결제", W - 10, H - 19, 0xFFB8A98A);
            return;
        }
        if (arena) {
            UiKit.bold(g, this.title.getString(), 10, 8, 0xFFFFE9B0);
            UiKit.text(g, menu.header, 10, 19, 0xFFF4C9C9);
            if (menu.entries.isEmpty()) UiKit.center(g, menu.empty, W / 2, 90, 0xFF5A3E1E);
            UiKit.text(g, (page + 1) + " / " + pages(), 74, H - 19, 0xFFFFE9B0);
            return;
        }
        UiKit.bold(g, this.title.getString(), 10, 8, 0xFFFFFFFF);
        UiKit.text(g, menu.header, 10, 18, 0xFFCFF7EF);
        if (menu.entries.isEmpty()) {
            UiKit.center(g, menu.empty, W / 2, 80, UiKit.INK_SOFT);
        }
        UiKit.text(g, (page + 1) + " / " + pages(), 60, H - 19, UiKit.INK_SOFT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

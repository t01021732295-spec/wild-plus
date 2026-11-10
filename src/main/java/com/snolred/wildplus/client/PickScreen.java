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
    private static final int COLS = 5, GRID_ROWS = 2, CASE_W = 58, CASE_H = 76;
    /** 투기장 줄: 낡은 양피지 + 청동 테 */
    private static final UiButton.Style ARENA_ROW = new UiButton.Style(0xFFF7E6C0, 0xFFE6CC96, 0xFF6E4A1E, 0xFF3A2410, false,
            UiButton.Shape.PAPER);
    private static final UiButton.Style ARENA_KEY = new UiButton.Style(0xFFC9A86A, 0xFF8E6A32, 0xFF3E2A10, 0xFFFFF4DA, true,
            UiButton.Shape.SOFT);
    private final boolean arena, shop, job, market;
    /** 알바 게시판: 코르크판에 꽂힌 메모지 */
    private static final UiButton.Style JOB_NOTE = new UiButton.Style(0xFFFFF6D8, 0xFFF4E4B0, 0xFFB8925A, 0xFF3A2A14, false, UiButton.Shape.PAPER);
    /** 투기장 판 화면 (설정 ◀▶ · 팀 판 · 큰 버튼): 칸 모양이 하나라도 있으면 */
    private final boolean panel;
    private record Rect(int x, int y, int w, int h, int top, int bottom, int border) {}
    private record Txt(int x, int y, String s, int c, int align, boolean bold) {}
    private final List<Rect> rects = new ArrayList<>();
    private final List<Txt> txts = new ArrayList<>();
    private static final UiButton.Style ARENA_GO = new UiButton.Style(0xFFE8484F, 0xFF9E1820, 0xFF3E070B, 0xFFFFF4DA, true, UiButton.Shape.SOFT);
    private static final UiButton.Style ARENA_STOP = new UiButton.Style(0xFF6A625A, 0xFF433C35, 0xFF1E1A16, 0xFFF0E6D6, true, UiButton.Shape.SOFT);
    private static final UiButton.Style ARENA_RED = new UiButton.Style(0xFFF0595E, 0xFFC92A31, 0xFF701217, 0xFFFFFFFF, true, UiButton.Shape.SOFT);
    private static final UiButton.Style ARENA_BLUE = new UiButton.Style(0xFF4C8DF6, 0xFF2563D9, 0xFF123C8C, 0xFFFFFFFF, true, UiButton.Shape.SOFT);
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
        super(menu, inv, title, (menu.flags & PickMenu.FLAG_SHOP) != 0 ? 330 : (menu.flags & PickMenu.FLAG_ARENA) != 0 ? 284
                : (menu.flags & PickMenu.FLAG_JOB) != 0 ? 300 : 240,
                (menu.flags & PickMenu.FLAG_SHOP) != 0 ? 32 + GRID_ROWS * (CASE_H + 4) + 24
                        : (menu.flags & PickMenu.FLAG_JOB) != 0 ? 170
                        : (menu.flags & PickMenu.FLAG_CAN_TOGGLE) != 0 ? 222 : (menu.flags & PickMenu.FLAG_ARENA) != 0 ? 206 : 200);
        this.arena = (menu.flags & PickMenu.FLAG_ARENA) != 0;
        this.shop = (menu.flags & PickMenu.FLAG_SHOP) != 0;
        this.market = (menu.flags & PickMenu.FLAG_MARKET) != 0;
        this.job = (menu.flags & PickMenu.FLAG_JOB) != 0;
        this.panel = arena && menu.entries.stream().anyMatch(e -> e.kind() != PickMenu.K_ROW);
        this.W = this.imageWidth;
        this.H = this.imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        if (panel) {
            layoutPanel();
            return;
        }
        if (shop) {
            int gx = leftPos + (W - (COLS * CASE_W + (COLS - 1) * 6)) / 2;
            for (int r = 0; r < COLS * GRID_ROWS; r++) {
                final int row = r;
                rows.add(addRenderableWidget(new CaseButton(gx + (r % COLS) * (CASE_W + 6), topPos + 32 + (r / COLS) * (CASE_H + 4), () -> pick(row))));
            }
        } else if (job) {
            // 메모지 3장: 큼직하게, 살짝 엇갈려 꽂힘
            for (int r = 0; r < 3; r++) {
                final int row = r;
                UiButton b = addRenderableWidget(new UiButton(leftPos + 18 + (r % 2) * 6, topPos + 40 + r * 36, W - 42, 30, Component.empty(),
                        JOB_NOTE, () -> pick(row)).left());
                b.visible = false;
                rows.add(b);
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
        return shop ? COLS * GRID_ROWS : job ? 3 : ROWS;
    }

    private int pages() {
        return Math.max(1, (menu.entries.size() + per() - 1) / per());
    }

    /**
     * 투기장 판: 위에서부터 팀 판 → 설정 줄(◀ 값 ▶) → 내 기록 → 큰 버튼, 맨 아래 줄에 작은 버튼.
     * 좌표는 창 왼쪽 위 기준으로 기억했다가 배경 · 글씨를 그릴 때 쓴다.
     */
    private void layoutPanel() {
        rects.clear();
        txts.clear();
        int x0 = 24, cw = W - 48, y = 38;
        List<PickMenu.Entry> es = menu.entries;
        // 팀 판 두 개 (나란히)
        int pw = (cw - 6) / 2;
        boolean teams = false;
        for (int i = 0; i < es.size(); i++) {
            PickMenu.Entry e = es.get(i);
            if (e.kind() != PickMenu.K_TEAM_A && e.kind() != PickMenu.K_TEAM_B) continue;
            teams = true;
            boolean a = e.kind() == PickMenu.K_TEAM_A;
            int px = x0 + (a ? 0 : pw + 6);
            int band = a ? 0xFFC9262F : 0xFF2563D9;
            rects.add(new Rect(px, y, pw, 66, 0xFFF7E6C0, 0xFFE6CC96, 0xFF6E4A1E));
            rects.add(new Rect(px + 1, y + 1, pw - 2, 12, band, UiKit.darker(band, 0.25F), band));
            txts.add(new Txt(px + pw / 2, y + 3, (a ? "🔴 " : "🔵 ") + e.title(), 0xFFFFFFFF, 1, true));
            String sub = e.sub();
            boolean mine = sub.startsWith("*");
            if (mine) sub = sub.substring(1);
            String[] names = sub.split(", ");
            for (int k = 0; k < Math.min(4, names.length); k++)
                txts.add(new Txt(px + 6, y + 16 + k * 8, names[k].equals("(비어 있음)") ? "· 빈자리" : "⚔ " + names[k],
                        names[k].equals("(비어 있음)") ? 0xFF9A8060 : 0xFF3A2410, 0, false));
            final int idx = i;
            UiButton join = addRenderableWidget(UiButton.of(leftPos + px + (pw - 64) / 2, topPos + y + 49, 64, 14,
                    mine ? "참가 중" : "참가하기", a ? ARENA_RED : ARENA_BLUE, () -> click(idx)));
            join.active = !mine;
        }
        if (teams) y += 70;
        // 설정 줄
        for (int i = 0; i < es.size(); i++) {
            PickMenu.Entry e = es.get(i);
            if (e.kind() != PickMenu.K_OPTION) continue;
            final int idx = i;
            rects.add(new Rect(x0, y, cw, 19, 0xFFF7E6C0, 0xFFE6CC96, 0xFF6E4A1E));
            txts.add(new Txt(x0 + 8, y + 6, e.title(), 0xFF3A2410, 0, true));
            int vx = x0 + cw - 132;
            rects.add(new Rect(vx + 18, y + 2, 92, 15, 0xFF3A2410, 0xFF2A180A, 0xFF8E6A32));
            txts.add(new Txt(vx + 64, y + 6, e.sub(), 0xFFFFE9B0, 1, false));
            addRenderableWidget(UiButton.of(leftPos + vx, topPos + y + 2, 16, 15, "◀", ARENA_KEY, () -> click(PickMenu.DEC + idx)));
            addRenderableWidget(UiButton.of(leftPos + vx + 112, topPos + y + 2, 16, 15, "▶", ARENA_KEY, () -> click(idx)));
            y += 21;
        }
        // 내 기록 (못 누름)
        for (PickMenu.Entry e : es) {
            if (e.kind() != PickMenu.K_INFO) continue;
            y += 2;
            rects.add(new Rect(x0, y, cw, 16, 0xFF5A3E1E, 0xFF3E2A10, 0xFF2A1A08));
            txts.add(new Txt(x0 + 8, y + 4, e.title(), 0xFFFFD25A, 0, false));
            txts.add(new Txt(x0 + cw - 8, y + 4, e.sub(), 0xFFF0DEC0, 2, false));
            y += 20;
        }
        // 큰 버튼 (한 줄에 나란히)
        List<Integer> big = new ArrayList<>();
        for (int i = 0; i < es.size(); i++)
            if (es.get(i).kind() == PickMenu.K_PRIMARY || es.get(i).kind() == PickMenu.K_DANGER) big.add(i);
        if (!big.isEmpty()) {
            int bw = (cw - (big.size() - 1) * 6) / big.size();
            for (int k = 0; k < big.size(); k++) {
                int idx = big.get(k);
                PickMenu.Entry e = es.get(idx);
                UiButton b = addRenderableWidget(UiButton.of(leftPos + x0 + k * (bw + 6), topPos + y + 2, bw, 22, e.title(),
                        e.kind() == PickMenu.K_DANGER ? ARENA_STOP : ARENA_GO, () -> click(idx)));
                if (!e.sub().isEmpty()) b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(e.sub())));
                if (!e.sub().isEmpty() && big.size() == 1) txts.add(new Txt(x0 + cw / 2, y + 27, e.sub(), 0xFF6E4A1E, 1, false));
            }
        }
        // 아래 줄 작은 버튼 (횃불 사이)
        List<Integer> small = new ArrayList<>();
        for (int i = 0; i < es.size(); i++) if (es.get(i).kind() == PickMenu.K_SMALL) small.add(i);
        if (!small.isEmpty()) {
            int sw = Math.min(90, (cw - (small.size() - 1) * 6) / small.size());
            int sx = x0 + (cw - (small.size() * sw + (small.size() - 1) * 6)) / 2;
            for (int k = 0; k < small.size(); k++) {
                int idx = small.get(k);
                addRenderableWidget(UiButton.of(leftPos + sx + k * (sw + 6), topPos + H - 24, sw, 17, es.get(idx).title(), ARENA_KEY, () -> click(idx)));
            }
        }
    }

    private void refresh() {
        if (panel) return;
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
                } else if (job) {
                    b.setMessage(Component.empty());
                    b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(e.sub())));
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
        prev.visible = next.visible = !job;
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
        if (market) {
            stall(g, x, y);
            return;
        }
        if (shop) {
            showroom(g, x, y);
            return;
        }
        if (job) {
            corkboard(g, x, y);
            return;
        }
        if (arena) {
            colosseum(g, x, y);
            for (Rect r : rects) {
                UiKit.round(g, x + r.x(), y + r.y() + 1, r.w(), r.h(), 2, 0x40000000);
                UiKit.round(g, x + r.x(), y + r.y(), r.w(), r.h(), 2, r.border());
                g.fillGradient(x + r.x() + 1, y + r.y() + 1, x + r.x() + r.w() - 1, y + r.y() + r.h() - 1, r.top(), r.bottom());
            }
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

    /** 알바 게시판: 원목 테 + 코르크판 + 위 나무 간판 */
    private void corkboard(GuiGraphicsExtractor g, int x, int y) {
        UiKit.device(g, x, y, W, H, 0xFF8A5A30, 0xFF5A3618);
        for (int k = 0; k < 6; k++) g.fill(x + 3, y + 6 + k * 27, x + W - 3, y + 7 + k * 27, 0x18000000); // 나뭇결
        g.fillGradient(x + 8, y + 32, x + W - 8, y + H - 8, 0xFFC8955A, 0xFFB07A42);
        UiKit.speckle(g, x + 8, y + 32, W - 16, H - 40, 23, 520, 0x40603A18, 0x30E8C090);
        UiKit.outline(g, x + 7, y + 31, W - 14, H - 38, 0xFF4A2A10);
        // 간판
        g.fillGradient(x + 40, y + 5, x + W - 40, y + 27, 0xFF6A4020, 0xFF4A2A12);
        UiKit.outline(g, x + 40, y + 5, W - 80, 22, 0xFF2A180A);
        g.fill(x + 44, y + 8, x + 46, y + 10, 0xFFB0B0B0);
        g.fill(x + W - 46, y + 8, x + W - 44, y + 10, 0xFFB0B0B0);
    }

    /** 명품관: 짙은 원목 테 · 어두운 벽지 · 위에서 비추는 조명 · 금 간판 */
    /** 4.8 거래소: 빨강 · 흰 줄무늬 차양 아래 종이 벽과 나무 바닥 */
    private void stall(GuiGraphicsExtractor g, int x, int y) {
        UiKit.device(g, x, y, W, H, 0xFF8A5A2E, 0xFF4A2E14);
        g.fillGradient(x + 4, y + 4, x + W - 4, y + H - 4, 0xFFF6E9CC, 0xFFE6D2A8);
        for (int py = y + 40; py < y + H - 28; py += 14) g.fill(x + 6, py, x + W - 6, py + 1, 0x14805A2A); // 판자 결
        // 차양
        for (int sx = x + 4, k = 0; sx < x + W - 4; sx += 14, k++) {
            int ex = Math.min(sx + 14, x + W - 4), c = k % 2 == 0 ? 0xFFC43434 : 0xFFF4EEE2;
            g.fillGradient(sx, y + 4, ex, y + 28, c, UiKit.darker(c, 0.12F));
            g.fill(sx + 2, y + 28, ex - 2, y + 31, c);
            g.fill(sx + 4, y + 31, ex - 4, y + 33, c);
        }
        g.fill(x + 4, y + 33, x + W - 4, y + 35, 0x22000000);
        // 아래 나무 바닥 띠
        g.fillGradient(x + 4, y + H - 26, x + W - 4, y + H - 4, 0xFF9A6C38, 0xFF6B4726);
        g.fill(x + 4, y + H - 26, x + W - 4, y + H - 25, 0xFFD9B070);
    }

    private void showroom(GuiGraphicsExtractor g, int x, int y) {
        UiKit.device(g, x, y, W, H, 0xFF6A3E22, 0xFF2E1A0E);
        g.fillGradient(x + 4, y + 4, x + W - 4, y + H - 4, 0xFF2B1D2E, 0xFF140C16);
        // 벽지 다마스크 무늬
        for (int py = y + 32; py < y + H - 26; py += 12)
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
                int cx = gx + c * (CASE_W + 6) + CASE_W / 2, cy = y + 32 + r * (CASE_H + 4);
                for (int k = 0; k < 8; k++) g.fill(cx - 6 - k * 2, cy - 2 + k * 6, cx + 6 + k * 2, cy + 4 + k * 6, 0x08FFF2C8);
                g.fill(cx - 5, cy - 3, cx + 5, cy - 1, 0xFFFFF2C8);
            }
        // 아래 바닥 띠
        g.fillGradient(x + 4, y + H - 26, x + W - 4, y + H - 4, 0xFF3A2414, 0xFF1E120A);
        g.fill(x + 4, y + H - 26, x + W - 4, y + H - 25, 0xFFD9A22E);
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
            int gh = 46; // 유리 높이
            if (market) {
                boolean sign = entry.kind() == PickMenu.K_SMALL;
                g.fill(x + 1, y + gh + 9, x + w + 1, y + gh + 12, 0x33000000);
                if (sign) { // 메뉴 버튼: 청록 나무 간판
                    g.fillGradient(x, y + 2, x + w, y + gh + 10, hover ? 0xFF3A97A8 : 0xFF2F7F8F, 0xFF1E5563);
                    UiKit.outline(g, x + 2, y + 4, w - 4, gh + 4, 0xFFF6E9CC);
                    g.fill(x + w / 2 - 12, y - 1, x + w / 2 - 10, y + 3, 0xFF6B4726);
                    g.fill(x + w / 2 + 10, y - 1, x + w / 2 + 12, y + 3, 0xFF6B4726);
                } else { // 나무 상자
                    g.fillGradient(x, y + 2, x + w, y + gh, 0xFFC89A5E, 0xFF9A6C38);
                    g.fillGradient(x + 3, y + 5, x + w - 3, y + gh - 2, 0xFF6B4726, 0xFF4A2F16);
                    for (int k = 1; k < 4; k++) g.fill(x + 3, y + 5 + k * 10, x + w - 3, y + 6 + k * 10, 0x22000000);
                    g.fillGradient(x - 1, y + gh, x + w + 1, y + gh + 10, 0xFFD9B070, 0xFFA87A40);
                    g.fill(x - 1, y + gh, x + w + 1, y + gh + 1, 0xFFF2D9A0);
                }
                int bob2 = hover ? -2 : 0;
                g.pose().pushMatrix();
                g.pose().translate(x + w / 2F - 16, y + 9 + bob2);
                g.pose().scale(2F, 2F);
                g.item(entry.icon(), 0, 0);
                g.pose().popMatrix();
                if (hover) UiKit.outline(g, x - 1, y + 1, w + 2, gh + 10, 0xFFFFF0C8);
            } else {
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
                g.pose().translate(x + w / 2F - 16, y + 8 + bob);
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
            }
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
            String l1 = name;
            while (font.width(l1) > w - 2 && l1.length() > 1) l1 = l1.substring(0, l1.length() - 2) + "…";
            int ty = y + gh + 13;
            g.text(font, l1, x + (w - font.width(l1)) / 2, ty, market ? 0xFF3A2A14 : 0xFFF4E9D8, false);
            // 가격 (보유 · 착용이면 초록 글씨)
            String price = !state.isEmpty() ? state : entry.sub().isEmpty() ? "무료" : entry.sub();
            int pc = market ? 0xFFA8321E : !state.isEmpty() ? 0xFF7CE08A : 0xFFFFD25A;
            price = shortPrice(price, w - 2);
            g.text(font, price, x + (w - font.width(price)) / 2, ty + 9, pc, !market);
        }
    }

    /**
     * 3.9.4: 진열장 칸 아래 가격 줄이 옆 칸을 덮지 않게.
     * 설명이 붙은 긴 줄("설명 · 흉조 병 3개 · 남은 1")은 값 · 남은 개수만 남기고 (전체는 마우스를 올리면 보임), 그래도 길면 …로 자른다.
     */
    static String shortPrice(String price, int maxW) {
        var font = UiKit.font();
        if (font.width(price) <= maxW) return price;
        if (price.contains(" · ")) {
            StringBuilder b = new StringBuilder();
            for (String seg : price.split(" · ")) {
                if (seg.matches(".*\\d.*(동|개).*") || seg.startsWith("남은") || seg.equals("품절")) {
                    seg = seg.replace("흉조 병 ", "병 ");
                    if (!b.isEmpty()) b.append(" · ");
                    b.append(seg);
                }
            }
            if (!b.isEmpty()) price = b.toString();
            if (font.width(price) > maxW && price.contains(" · ")) price = price.substring(0, price.indexOf(" · "));
        }
        while (font.width(price) > maxW && price.length() > 2) price = price.substring(0, price.length() - 2) + "…";
        return price;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (job) {
            String t = this.title.getString();
            UiKit.bold(g, t, (W - UiKit.font().width(t)) / 2, 9, 0xFFFFE9B0);
            UiKit.center(g, menu.header, W / 2, 19, 0xFFE8CFA0);
            // 메모지 글씨 + 압정
            for (int r = 0; r < rows.size(); r++) {
                UiButton b = rows.get(r);
                if (!b.visible) continue;
                int idx = page * per() + r;
                if (idx >= menu.entries.size()) continue;
                PickMenu.Entry e = menu.entries.get(idx);
                int bx = b.getX() - leftPos, by = b.getY() - topPos;
                g.fill(bx + b.getWidth() / 2 - 2, by - 3, bx + b.getWidth() / 2 + 2, by + 1, 0xFFD0303A);
                g.fill(bx + b.getWidth() / 2 - 1, by - 2, bx + b.getWidth() / 2, by - 1, 0xFFFF9090);
                UiKit.bold(g, e.title(), bx + 8, by + 6, 0xFF3A2A14);
                boolean done = e.sub().startsWith("✔"), on = e.sub().startsWith("▶");
                UiKit.text(g, e.sub(), bx + 8, by + 18, done ? 0xFF2E8A3E : on ? 0xFFC0501A : 0xFF8A6A3A);
                if (done) { // 완료 도장
                    int sx = bx + b.getWidth() - 40, sy = by + 6;
                    UiKit.outline(g, sx, sy, 32, 18, 0xCC2E8A3E);
                    UiKit.center(g, "완료", sx + 16, sy + 5, 0xFF2E8A3E);
                }
            }
            return;
        }
        if (market) {
            String t = this.title.getString();
            int tw = Math.max(UiKit.font().width(t) + 4, UiKit.font().width(menu.header));
            UiKit.round(g, 8, 5, tw + 12, 25, 2, 0xF04A2E14);
            UiKit.bold(g, t, 14, 8, 0xFFFFF0C8);
            UiKit.text(g, menu.header, 14, 19, 0xFFE8CFA0);
            UiKit.text(g, (page + 1) + " / " + pages(), 60, H - 19, 0xFFFFF0C8);
            UiKit.right(g, t.contains("거래소") ? "수수료 5% · 대금은 통장으로" : t.contains("출하") ? "누르면 제외 / 다시 포함" : "", W - 10, H - 19, 0xFFFFF0C8);
            return;
        }
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
            if (panel) {
                for (Txt t : txts) {
                    if (t.align() == 1) {
                        if (t.bold()) UiKit.bold(g, t.s(), t.x() - UiKit.font().width(t.s()) / 2, t.y(), t.c());
                        else UiKit.center(g, t.s(), t.x(), t.y(), t.c());
                    } else if (t.align() == 2) UiKit.right(g, t.s(), t.x(), t.y(), t.c());
                    else if (t.bold()) UiKit.bold(g, t.s(), t.x(), t.y(), t.c());
                    else UiKit.text(g, t.s(), t.x(), t.y(), t.c());
                }
                return;
            }
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

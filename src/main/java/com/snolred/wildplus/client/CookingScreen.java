package com.snolred.wildplus.client;

import com.snolred.wildplus.cooking.Cooking;
import com.snolred.wildplus.cooking.CookingData;
import com.snolred.wildplus.menu.CookingMenu;
import com.snolred.wildplus.registry.ModItems14;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 조리 화면 (도안대로): 기구마다 그림이 다르다 — 가마솥(벽돌 벽 + 끓는 솥) · 화덕(아치 안 불길) · 그릴(숯불 석쇠) ·
 * 화구(타일 + 파란 불 + 프라이팬) · 도마(나무 + 큰 도마) · 숙성통(나무 선반 + 통). 양옆 재료 칸, 아래 땔감 · 진행 · 완성.
 * [레시피] 버튼: 이 기구로 만들 수 있는 요리 목록.
 */
public class CookingScreen extends AbstractContainerScreen<CookingMenu> {
    private static final int W = CookingMenu.W, H = CookingMenu.H;
    private boolean book;
    private int page;
    private UiButton bookBtn, prev, next;

    public CookingScreen(CookingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        this.inventoryLabelY = CookingMenu.INV_Y - 11;
        this.inventoryLabelX = 18;
    }

    private CookingData.Station st() {
        int i = menu.station();
        CookingData.Station[] all = CookingData.Station.values();
        return i >= 0 && i < all.length ? all[i] : CookingData.Station.PREP;
    }

    private List<CookingData.Dish> dishes() {
        List<CookingData.Dish> out = new ArrayList<>();
        for (CookingData.Dish d : CookingData.DISHES) if (d.station() == st()) out.add(d);
        return out;
    }

    @Override
    protected void init() {
        super.init();
        bookBtn = addRenderableWidget(UiButton.of(leftPos + W - 52, topPos + 5, 44, 13, "레시피", UiButton.GOLD, () -> {
            book = !book;
            page = 0;
        }));
        prev = addRenderableWidget(UiButton.of(leftPos + 8, topPos + H - 22, 20, 15, "◀", UiButton.STEEL, () -> page--));
        next = addRenderableWidget(UiButton.of(leftPos + 32, topPos + H - 22, 20, 15, "▶", UiButton.STEEL, () -> page++));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int pages = Math.max(1, (dishes().size() + 7) / 8);
        page = Math.max(0, Math.min(page, pages - 1));
        prev.visible = next.visible = book;
        prev.active = page > 0;
        next.active = page < pages - 1;
        bookBtn.setMessage(Component.literal(book ? "닫기" : "레시피"));
    }

    // ───────── 배경 ─────────
    private static void bricks(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int c1, int c2, int mortar) {
        g.fill(x0, y0, x1, y1, mortar);
        int row = 0;
        for (int y = y0; y < y1; y += 7, row++) {
            for (int x = x0 - (row % 2) * 7; x < x1; x += 14) {
                int l = Math.max(x0, x + 1), r = Math.min(x1, x + 13);
                if (r > l) g.fill(l, y + 1, r, Math.min(y1, y + 6), ((x / 14 + row) % 3 == 0) ? c2 : c1);
            }
        }
    }

    private static void planks(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int c) {
        for (int y = y0; y < y1; y += 8) {
            g.fill(x0, y, x1, Math.min(y1, y + 8), (y / 8) % 2 == 0 ? c : UiKit.darker(c, 0.08F));
            g.fill(x0, y, x1, y + 1, UiKit.darker(c, 0.3F));
        }
    }

    private void flames(GuiGraphicsExtractor g, int x0, int x1, int yb, int hmax, boolean lit) {
        long t = Util.getMillis() / 110;
        for (int x = x0, k = 0; x < x1; x += 6, k++) {
            int h = lit ? hmax - (int) ((t + k * 3) % 4) * 2 : 3;
            g.fill(x, yb - h, x + 5, yb, 0xFFFF7A1E);
            g.fill(x + 1, yb - h + 3, x + 4, yb, 0xFFFFC340);
            g.fill(x + 2, yb - h + 6, x + 3, yb, 0xFFFFF0A0);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        boolean cooking = menu.progress() > 0;
        long t = Util.getMillis() / 120;
        UiKit.device(g, x, y, W, H, 0xFF6A4630, 0xFF3A2414);
        int cx = x + W / 2, top = y + 20, bot = y + 86;
        boolean lit = menu.burn() > 0;
        switch (st()) {
            case POT -> {
                bricks(g, x + 4, y + 4, x + W - 4, y + 90, 0xFFA84A34, 0xFF96402E, 0xFF5A463C);
                g.fill(cx - 30, bot - 8, cx + 30, bot, 0xFF1E1410);
                UiKit.circle(g, cx - 25, top + 12, 50, 0xFF1E1E24);
                UiKit.circle(g, cx - 21, top + 16, 20, 0xFF2E2E36);
                UiKit.round(g, cx - 31, top + 12, 62, 9, 4, 0xFF46464E);
                UiKit.round(g, cx - 27, top + 13, 54, 6, 3, cooking ? 0xFFE0702E : 0xFF8A5A3A);
                g.fill(cx - 36, top + 14, cx - 31, top + 18, 0xFF46464E);
                g.fill(cx + 31, top + 14, cx + 36, top + 18, 0xFF46464E);
                if (cooking) for (int k = 0; k < 4; k++) UiKit.circle(g, cx - 20 + k * 11, top + 13 + (int) ((t + k * 5) % 4) / 2, 4, 0xFFF6B070);
                for (int k = 0; k < 3; k++) {
                    int sy = top + 6 - (int) ((t + k * 5) % 12);
                    if (cooking) UiKit.circle(g, cx - 14 + k * 11, sy, 7, 0x88F0F0F4);
                }
                flames(g, cx - 26, cx + 26, bot, 8, lit);
            }
            case OVEN -> {
                bricks(g, x + 4, y + 4, x + W - 4, y + 90, 0xFFC47E54, 0xFFB06E48, 0xFF786054);
                UiKit.circle(g, cx - 38, top, 76, 0xFF8A5A40);
                g.fill(cx - 38, top + 38, cx + 38, bot, 0xFF8A5A40);
                UiKit.circle(g, cx - 34, top + 4, 68, 0xFF28180E);
                g.fill(cx - 34, top + 38, cx + 34, bot, 0xFF28180E);
                g.fill(cx - 26, top + 34, cx + 26, top + 37, 0xFF55555A);
                if (cooking) UiKit.round(g, cx - 14, top + 26, 28, 8, 4, 0xFFE8B060);
                flames(g, cx - 30, cx + 30, bot, 16, lit);
                UiKit.round(g, cx + 44, top + 8, 8, 52, 4, 0xFFF0F0EC);
                int lv = lit ? 44 : 8;
                UiKit.round(g, cx + 46, top + 58 - lv, 4, lv, 2, 0xFFE63C28);
            }
            case GRILL -> {
                g.fillGradient(x + 4, y + 4, x + W - 4, y + 90, 0xFF2A2A30, 0xFF16161A);
                UiKit.round(g, cx - 52, top + 14, 104, 50, 5, 0xFFB42824);
                UiKit.round(g, cx - 52, top + 14, 104, 6, 3, 0xFFD23A30);
                g.fill(cx - 46, top + 22, cx + 46, top + 58, lit ? 0xFFFF7A28 : 0xFF3A2020);
                if (lit) for (int k = 0; k < 9; k++) UiKit.circle(g, cx - 42 + k * 10, top + 44 + (k % 2) * 4, 8, (t + k) % 3 == 0 ? 0xFFFFC040 : 0xFFE0501C);
                for (int k = 0; k < 9; k++) g.fill(cx - 46, top + 24 + k * 4, cx + 46, top + 25 + k * 4, 0xFF28282C);
                if (cooking) {
                    UiKit.round(g, cx - 30, top + 26, 24, 10, 3, 0xFF8A3A26);
                    UiKit.round(g, cx + 6, top + 28, 24, 10, 3, 0xFF8A3A26);
                    for (int k = 0; k < 5; k++) UiKit.circle(g, cx - 36 + k * 16, top + 8 - (int) ((t + k * 4) % 10), 6, 0x66D0D0D0);
                }
            }
            case STOVE -> {
                for (int ty = y + 4; ty < y + 90; ty += 10)
                    for (int tx = x + 4; tx < x + W - 4; tx += 10)
                        g.fill(tx, ty, Math.min(tx + 9, x + W - 4), Math.min(ty + 9, y + 90), ((tx + ty) / 10) % 2 == 0 ? 0xFFEEF0EC : 0xFFE2E6E4);
                UiKit.round(g, cx - 48, top + 8, 96, 56, 4, 0xFF1E1E22);
                UiKit.circle(g, cx - 26, top + 16, 40, lit ? 0xFF3C6CF0 : 0xFF33333A);
                UiKit.circle(g, cx - 22, top + 20, 32, 0xFF1E1E22);
                UiKit.circle(g, cx - 23, top + 19, 34, 0xFF3A3A40);
                UiKit.circle(g, cx - 20, top + 22, 28, 0xFF55555C);
                g.fill(cx + 11, top + 34, cx + 40, top + 39, 0xFF2A1E18);
                if (cooking) {
                    UiKit.circle(g, cx - 14, top + 28, 16, 0xFFFFF8F0);
                    UiKit.circle(g, cx - 10, top + 32, 8, 0xFFFFC030);
                }
                UiKit.circle(g, cx + 26, top + 14, 10, 0xFF46464C);
                UiKit.circle(g, cx + 36, top + 14, 10, 0xFF46464C);
            }
            case PREP -> {
                planks(g, x + 4, y + 4, x + W - 4, y + 90, 0xFFB98A52);
                UiKit.round(g, cx - 50, top + 6, 100, 58, 6, 0xFF8A5A30);
                UiKit.round(g, cx - 48, top + 8, 96, 54, 6, 0xFFDCB070);
                for (int k = 0; k < 5; k++) g.fill(cx - 44, top + 14 + k * 10, cx + 44, top + 15 + k * 10, 0x22000000);
                g.fill(cx + 4, top + 20, cx + 36, top + 24, 0xFFD2D7DE);
                g.fill(cx + 36, top + 20, cx + 48, top + 24, 0xFF2A1E18);
                if (cooking) for (int k = 0; k < 4; k++) UiKit.round(g, cx - 38 + k * 9, top + 34, 7, 12, 2, k % 2 == 0 ? 0xFF4CAF50 : 0xFFF2F2F2);
            }
            case AGING -> {
                planks(g, x + 4, y + 4, x + W - 4, y + 90, 0xFF5A3A20);
                for (int k = 0; k < 2; k++) {
                    int by = top + 2 + k * 32;
                    g.fill(cx - 54, by + 28, cx + 54, by + 31, 0xFF3A2410);
                    UiKit.round(g, cx - 48, by, 96, 28, 10, 0xFF9A5E32);
                    for (int r = 0; r < 3; r++) g.fill(cx - 34 + r * 32, by, cx - 30 + r * 32, by + 28, 0xFF46464C);
                    g.fill(cx - 3, by + 11, cx + 3, by + 17, 0xFFC8AA3C);
                }
                if (cooking) g.fill(cx - 1, top + 19, cx + 1, top + 22 + (int) (t % 6), 0xFFB03050);
            }
        }
        // 제목판
        UiKit.round(g, cx - 44, y + 4, 88, 14, 3, 0xCC2A1A10);
        // 아래 판: 땔감 · 진행 · 완성
        UiKit.round(g, x + 4, y + 88, W - 8, 26, 4, 0xEEF0E4CC);
        int barX = x + 36, barW = 122;
        UiKit.round(g, barX, y + 102, barW, 7, 3, 0xFFC8B494);
        int fill = (int) ((long) barW * menu.progress() / menu.total());
        if (fill > 0) UiKit.round(g, barX, y + 102, Math.max(6, fill), 7, 3, 0xFFE0702E);
        if (st().fuel) {
            int fh = (int) (10L * menu.burn() / menu.burnMax());
            g.fill(x + 30, y + 112 - fh, x + 33, y + 112, 0xFFFF8A1E);
        }
        // 칸 테두리 (재료 = 크림, 땔감 = 주황, 완성 = 금)
        for (Slot s : menu.slots) {
            if (s.index >= 8) break;
            int c = s.index == 7 ? 0xFFFFD25A : s.index == 6 ? 0xFFE08A3A : 0xFFF4E4C0;
            UiKit.outline(g, x + s.x - 2, y + s.y - 2, 20, 20, c);
            UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.LIGHT);
        }
        // 가방 쟁반
        UiKit.inventoryTray(g, x + 4, y + CookingMenu.INV_Y - 14, W - 8, H - CookingMenu.INV_Y + 10);
        for (Slot s : menu.slots) if (s.index >= 8) UiKit.slot(g, x + s.x, y + s.y);
    }

    private void recipeBook(GuiGraphicsExtractor g, int x, int y) {
        UiKit.round(g, x + 4, y + 20, W - 8, H - 46, 4, 0xF2241A12);
        List<CookingData.Dish> ds = dishes();
        for (int i = 0; i < 8; i++) {
            int idx = page * 8 + i;
            if (idx >= ds.size()) break;
            CookingData.Dish d = ds.get(idx);
            int ry = y + 26 + i * 22;
            UiKit.round(g, x + 8, ry - 2, W - 16, 20, 3, i % 2 == 0 ? 0x33FFFFFF : 0x18FFFFFF);
            g.item(new ItemStack(ModItems14.dish(d.id())), x + 10, ry);
            List<ItemStack> ing = Cooking.icons(d);
            for (int k = 0; k < ing.size(); k++) g.item(ing.get(k), x + 92 + k * 16, ry);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String t = this.title.getString();
        UiKit.bold(g, t, (W - UiKit.font().width(t)) / 2, 8, 0xFFFFE9B0);
        UiKit.text(g, this.playerInventoryTitle.getString(), this.inventoryLabelX, this.inventoryLabelY, 0xFF3A414B);
        int di = menu.dish();
        String state;
        if (di >= 0 && di < CookingData.DISHES.length) {
            CookingData.Dish d = CookingData.DISHES[di];
            int left = (menu.total() - menu.progress()) / 20;
            state = d.ko() + (menu.progress() > 0 ? " · " + left + "초" : st().fuel && menu.burn() <= 0 ? " · 땔감 필요" : "");
        } else {
            state = "재료를 넣으세요 (레시피 버튼)";
        }
        UiKit.text(g, state, 36, 92, 0xFF6A3A14);
        if (book) {
            recipeBook(g, 0, 0);
            List<CookingData.Dish> ds = dishes();
            for (int i = 0; i < 8; i++) {
                int idx = page * 8 + i;
                if (idx >= ds.size()) break;
                CookingData.Dish d = ds.get(idx);
                int ry = 26 + i * 22;
                UiKit.text(g, d.ko() + (d.count() > 1 ? " ×" + d.count() : ""), 28, ry + 4, 0xFFFFE9B0);
            }
            UiKit.right(g, (page + 1) + "쪽 · " + st().ko + " " + st().seconds + "초", W - 10, H - 18, 0xFFE8CFA0);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

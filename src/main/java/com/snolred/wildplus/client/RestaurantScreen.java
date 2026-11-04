package com.snolred.wildplus.client;

import com.snolred.wildplus.block.RestaurantCounterBlockEntity;
import com.snolred.wildplus.cooking.Cooking;
import com.snolred.wildplus.cooking.CookingData;
import com.snolred.wildplus.entity.CustomerEntity;
import com.snolred.wildplus.menu.RestaurantMenu;
import com.snolred.wildplus.registry.ModItems14;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 식당 카운터 화면 (도안대로): 위 빨강·흰 차양 + 가게 이름판 · 별 · 오늘 매출.
 * 탭 [주방] 손님 주문 티켓 4장 (요리 · 값 · 인내심 · 내주기) · [메뉴] 사 먹기 · [평점] 별점 · 후기 · [관리] 이름 · 영업 · 알바 · 직원.
 * 아래: 진열대 10칸 (주인 · 직원만 채움) + 가방.
 */
public class RestaurantScreen extends AbstractContainerScreen<RestaurantMenu> {
    private static final int W = RestaurantMenu.W, H = RestaurantMenu.H, TOP = 48, CH = 82;
    private static final String[] TABS = {"주방", "메뉴", "평점", "관리"};
    private static final int[] KIND_COLOR = {0xFF2E8B57, 0xFFC27A16, 0xFF2A7FB8, 0xFFA03CA0, 0xFF666666};
    private int tab = -1;
    private int pickStars = 5;
    private final List<List<net.minecraft.client.gui.components.AbstractWidget>> tabWidgets = new ArrayList<>();
    private final UiButton[] serve = new UiButton[4];
    private final UiButton[] buy = new UiButton[10];
    private final UiButton[] tabBtn = new UiButton[4];
    private final UiButton[] starBtn = new UiButton[5];
    private UiButton openBtn, hireBtn, albaBtn, applyBtn, quitBtn;
    private EditBox nameBox, reviewBox;

    public RestaurantScreen(RestaurantMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        this.inventoryLabelX = RestaurantMenu.INV_X;
        this.inventoryLabelY = RestaurantMenu.INV_Y - 10;
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private <T extends net.minecraft.client.gui.components.AbstractWidget> T on(int t, T w) {
        addRenderableWidget(w);
        tabWidgets.get(t).add(w);
        return w;
    }

    @Override
    protected void init() {
        super.init();
        tabWidgets.clear();
        for (int t = 0; t < 4; t++) tabWidgets.add(new ArrayList<>());
        int x = leftPos, y = topPos;
        for (int t = 0; t < 4; t++) {
            final int tt = t;
            tabBtn[t] = addRenderableWidget(UiButton.of(x + 8 + t * 61, y + 33, 58, 13, TABS[t], UiButton.WHITE, () -> tab = tt));
        }
        // 주방: 티켓 내주기
        for (int k = 0; k < 4; k++) {
            final int kk = k;
            serve[k] = on(0, UiButton.of(x + 8 + k * 61, y + TOP + 66, 56, 13, "내주기", UiButton.GREEN, () -> click(kk)));
        }
        // 메뉴: 사 먹기
        for (int i = 0; i < 10; i++) {
            final int ii = i;
            int col = i / 5, row = i % 5;
            buy[i] = on(1, UiButton.of(x + 98 + col * 122, y + TOP + 2 + row * 16, 26, 13, "주문", UiButton.ORANGE, () -> click(RestaurantMenu.BTN_BUY + ii)));
        }
        // 평점: 별 고르기 + 후기 + 남기기
        for (int s = 0; s < 5; s++) {
            final int ss = s + 1;
            starBtn[s] = on(2, UiButton.of(x + 10 + s * 18, y + TOP + 32, 16, 14, "★", UiButton.GOLD, () -> pickStars = ss));
        }
        reviewBox = on(2, new EditBox(this.font, x + 10, y + TOP + 50, 96, 14, Component.literal("후기")));
        reviewBox.setMaxLength(40);
        reviewBox.setHint(Component.literal("한 줄 후기 (선택)"));
        on(2, UiButton.of(x + 10, y + TOP + 67, 96, 13, "평점 남기기", UiButton.BLUE, () -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                String t = reviewBox.getValue().trim();
                this.minecraft.player.connection.sendCommand("restaurant review " + pickStars + (t.isEmpty() ? "" : " " + t));
            }
        }));
        // 관리 (주인)
        nameBox = on(3, new EditBox(this.font, x + 8, y + TOP + 2, 82, 14, Component.literal("식당 이름")));
        nameBox.setMaxLength(20);
        nameBox.setHint(Component.literal("새 식당 이름"));
        on(3, UiButton.of(x + 92, y + TOP + 2, 34, 14, "변경", UiButton.BLUE, () -> {
            String n = nameBox.getValue().trim();
            if (!n.isEmpty() && this.minecraft != null && this.minecraft.player != null) this.minecraft.player.connection.sendCommand("restaurant name " + n);
        }));
        openBtn = on(3, UiButton.of(x + 8, y + TOP + 19, 58, 13, "영업", UiButton.GREEN, () -> click(RestaurantMenu.BTN_OPEN)));
        hireBtn = on(3, UiButton.of(x + 68, y + TOP + 19, 58, 13, "알바 모집", UiButton.TEAL, () -> click(RestaurantMenu.BTN_HIRING)));
        on(3, UiButton.of(x + 66, y + TOP + 35, 13, 12, "-", UiButton.STEEL, () -> click(RestaurantMenu.BTN_FEE_DOWN)));
        on(3, UiButton.of(x + 113, y + TOP + 35, 13, 12, "+", UiButton.STEEL, () -> click(RestaurantMenu.BTN_FEE_UP)));
        on(3, UiButton.of(x + 66, y + TOP + 50, 13, 12, "-", UiButton.STEEL, () -> click(RestaurantMenu.BTN_PCT_DOWN)));
        on(3, UiButton.of(x + 113, y + TOP + 50, 13, 12, "+", UiButton.STEEL, () -> click(RestaurantMenu.BTN_PCT_UP)));
        for (int i = 0; i < 3; i++) {
            final int ii = i;
            on(3, UiButton.of(x + 216, y + TOP + 12 + i * 12, 30, 11, "해고", UiButton.RED, () -> click(RestaurantMenu.BTN_FIRE + ii)));
        }
        for (int i = 0; i < 2; i++) {
            final int ii = i;
            on(3, UiButton.of(x + 216, y + TOP + 60 + i * 11, 30, 11, "수락", UiButton.GREEN, () -> click(RestaurantMenu.BTN_ACCEPT + ii)));
        }
        // 관리 (손님 · 알바 · 직원)
        albaBtn = on(3, UiButton.of(x + 8, y + TOP + 40, 74, 15, "알바 시작", UiButton.GREEN, () -> click(RestaurantMenu.BTN_ALBA)));
        applyBtn = on(3, UiButton.of(x + 86, y + TOP + 40, 74, 15, "직원 지원", UiButton.BLUE, () -> click(RestaurantMenu.BTN_APPLY)));
        quitBtn = on(3, UiButton.of(x + 164, y + TOP + 40, 74, 15, "그만두기", UiButton.RED, () -> click(RestaurantMenu.BTN_QUIT)));
        if (tab < 0) tab = menu.canServe() ? 0 : 1;
        layout();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (tab < 0) tab = menu.canServe() ? 0 : 1;
        layout();
    }

    private boolean owner() {
        return menu.role() == RestaurantCounterBlockEntity.ROLE_OWNER;
    }

    private void layout() {
        for (int t = 0; t < 4; t++) {
            for (var w : tabWidgets.get(t)) w.visible = t == tab;
            tabBtn[t].active = t != tab;
        }
        boolean own = owner();
        if (tab == 3) {
            // 주인용 / 손님용 위젯 나누기
            for (var w : tabWidgets.get(3)) w.visible = own;
            albaBtn.visible = !own && (menu.village() || menu.get(41) == 1) && menu.role() == RestaurantCounterBlockEntity.ROLE_GUEST;
            applyBtn.visible = !own && !menu.village() && menu.role() == RestaurantCounterBlockEntity.ROLE_GUEST;
            quitBtn.visible = !own && menu.role() != RestaurantCounterBlockEntity.ROLE_GUEST;
            int staff = menu.extra.staff().size(), apps = menu.extra.applicants().size();
            int k = 0;
            for (var w : tabWidgets.get(3)) {
                if (w instanceof UiButton b && b.getMessage().getString().equals("해고")) b.visible = own && k++ < staff;
            }
            k = 0;
            for (var w : tabWidgets.get(3)) {
                if (w instanceof UiButton b && b.getMessage().getString().equals("수락")) b.visible = own && k++ < Math.min(2, apps) && staff < 3;
            }
            openBtn.setMessage(Component.literal(menu.get(40) == 1 ? "영업 중 ON" : "영업 OFF"));
            hireBtn.setMessage(Component.literal(menu.get(41) == 1 ? "알바 모집 ON" : "알바 모집 OFF"));
        }
        if (tab == 0) {
            for (int k = 0; k < 4; k++) {
                boolean has = menu.get(k * 8) > 0;
                serve[k].visible = has && menu.canServe();
                serve[k].active = menu.get(k * 8 + 7) == 1;
                serve[k].setMessage(Component.literal(menu.get(k * 8 + 7) == 1 ? "내주기" : "요리 부족"));
            }
        }
        if (tab == 1) {
            for (int i = 0; i < 10; i++) {
                ItemStack s = menu.getSlot(i).getItem();
                buy[i].visible = !s.isEmpty() && Cooking.dishOf(s) != null;
            }
        }
        if (tab == 2) {
            for (int s = 0; s < 5; s++) starBtn[s].setMessage(Component.literal(s < pickStars ? "★" : "☆"));
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        for (EditBox b : new EditBox[]{nameBox, reviewBox}) {
            if (b != null && b.visible && b.isFocused() && !event.isEscape()) {
                b.keyPressed(event);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    // ───────── 그리기 ─────────
    private static String won(long v) {
        return String.format("%,d동", v);
    }

    private static String stars(int n) {
        return "★".repeat(Math.max(0, n)) + "☆".repeat(Math.max(0, 5 - n));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        UiKit.device(g, x, y, W, H, 0xFF6A4630, 0xFF3A2414);
        // 차양 (빨강 · 흰 줄무늬) + 이름판
        for (int k = 0; x + 4 + k * 12 < x + W - 4; k++) {
            int l = x + 4 + k * 12;
            g.fill(l, y + 4, Math.min(x + W - 4, l + 12), y + 16, k % 2 == 0 ? 0xFFDC3232 : 0xFFFAFAFA);
            g.fill(l + 2, y + 16, Math.min(x + W - 4, l + 10), y + 19, k % 2 == 0 ? 0xFFB82828 : 0xFFE0E0E0);
        }
        UiKit.round(g, x + 40, y + 6, W - 80, 24, 5, 0xFF281C12);
        UiKit.outline(g, x + 41, y + 7, W - 82, 22, 0xFFFFD27A);
        // 탭 줄 바탕
        UiKit.round(g, x + 4, y + 31, W - 8, 17, 3, 0xFF4A3220);
        // 내용 판 (탭마다 색)
        int bg = switch (tab) {
            case 0 -> 0xFFEEF0EC;
            case 1 -> 0xFFF6EBD6;
            case 2 -> 0xFFFFF4E0;
            default -> 0xFFE6E0D4;
        };
        UiKit.round(g, x + 4, y + TOP, W - 8, CH, 4, bg);
        if (tab == 0) for (int ty = y + TOP; ty < y + TOP + CH; ty += 12) g.fill(x + 5, ty, x + W - 5, ty + 1, 0x14000000);
        // 진열대
        UiKit.round(g, x + 4, y + RestaurantMenu.SHELF_Y - 4, W - 8, 24, 4, 0xFFC89A62);
        g.fill(x + 6, y + RestaurantMenu.SHELF_Y + 18, x + W - 6, y + RestaurantMenu.SHELF_Y + 19, 0xFF7A5534);
        for (int i = 0; i < 10; i++) UiKit.slot(g, x + RestaurantMenu.SHELF_X + i * 18, y + RestaurantMenu.SHELF_Y, UiKit.SlotStyle.LIGHT);
        // 가방
        UiKit.inventoryTray(g, x + 4, y + RestaurantMenu.INV_Y - 13, W - 8, H - RestaurantMenu.INV_Y + 9);
        for (var s : menu.slots) if (s.index >= 10) UiKit.slot(g, x + s.x, y + s.y);
        if (tab == 0) tickets(g, x, y);
        if (tab == 1) menuTab(g, x, y);
        if (tab == 2) ratingTab(g, x, y);
    }

    private void tickets(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 8, y + TOP + 3, x + W - 8, y + TOP + 5, 0xFF96969C);
        for (int k = 0; k < 4; k++) {
            int cx = x + 8 + k * 61, cy = y + TOP + 7;
            int kind = menu.get(k * 8) - 1;
            g.fill(cx + 26, cy - 4, cx + 30, cy, 0xFF78787E);
            if (kind < 0) {
                UiKit.dashed(g, cx, cy, 56, 72, 3, 0xFFB4B4B4);
                continue;
            }
            boolean noble = kind == CustomerEntity.Kind.NOBLE.ordinal();
            int paper = noble ? 0xFFFFECF0 : 0xFFFFFADC;
            UiKit.round(g, cx, cy, 56, 72, 2, paper);
            UiKit.outline(g, cx, cy, 56, 72, 0xFFBEAA82);
            for (int j = 0; j < 3; j++) {
                int di = menu.get(k * 8 + 1 + j) - 1;
                if (di >= 0 && di < CookingData.DISHES.length) g.item(new ItemStack(ModItems14.dish(CookingData.DISHES[di].id())), cx + 3 + j * 18, cy + 14);
            }
            int pat = menu.get(k * 8 + 4);
            int pc = pat > 50 ? 0xFF5ABE64 : pat > 25 ? 0xFFE6B432 : 0xFFE04638;
            UiKit.round(g, cx + 3, cy + 52, 50, 4, 2, 0xFFDCDCDC);
            if (pat > 0) UiKit.round(g, cx + 3, cy + 52, Math.max(4, 50 * pat / 100), 4, 2, pc);
        }
    }

    private void menuTab(GuiGraphicsExtractor g, int x, int y) {
        for (int i = 0; i < 10; i++) {
            ItemStack s = menu.getSlot(i).getItem();
            int col = i / 5, row = i % 5;
            int ex = x + 8 + col * 122, ey = y + TOP + 2 + row * 16;
            if (row % 2 == 0) g.fill(ex - 2, ey - 1, ex + 118, ey + 15, 0x10000000);
            if (s.isEmpty()) continue;
            g.item(s, ex, ey - 1);
        }
    }

    private void ratingTab(GuiGraphicsExtractor g, int x, int y) {
        UiKit.round(g, x + 112, y + TOP + 3, W - 120, CH - 6, 3, 0xFFFFFBF2);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // 이름 · 별 · 오늘
        String name = this.title.getString();
        UiKit.center(g, name, W / 2, 10, 0xFFFFDC8C);
        int st = menu.get(32);
        UiKit.center(g, Component.literal(stars(st)).withColor(0xFFFFC832), W / 2, 20, 0xFFFFC832);
        UiKit.text(g, this.playerInventoryTitle.getString(), this.inventoryLabelX, this.inventoryLabelY, 0xFF3A414B);
        UiKit.text(g, "진열대", 8, RestaurantMenu.SHELF_Y + 5, 0xFF4A2A10);
        UiKit.right(g, "오늘 매출 " + won(menu.get(37) * 10L) + " · 팁 " + won(menu.get(38) * 10L) + " · 손님 " + menu.get(39) + "명", W - 10, this.inventoryLabelY, 0xFF1E6A32);
        switch (tab) {
            case 0 -> ticketLabels(g);
            case 1 -> menuLabels(g);
            case 2 -> ratingLabels(g);
            default -> manageLabels(g);
        }
    }

    private void ticketLabels(GuiGraphicsExtractor g) {
        boolean any = false;
        for (int k = 0; k < 4; k++) {
            int cx = 8 + k * 61, cy = TOP + 7;
            int kind = menu.get(k * 8) - 1;
            if (kind < 0) {
                UiKit.center(g, "빈 자리", cx + 28, cy + 30, 0xFFA0A0A0);
                continue;
            }
            any = true;
            CustomerEntity.Kind kd = CustomerEntity.Kind.values()[Math.min(kind, CustomerEntity.Kind.values().length - 1)];
            UiKit.text(g, kd.ko, cx + 3, cy + 3, KIND_COLOR[Math.min(kind, KIND_COLOR.length - 1)]);
            UiKit.text(g, won(menu.get(k * 8 + 5)), cx + 3, cy + 34, 0xFF1E7832);
            int tipB = menu.get(k * 8 + 6);
            UiKit.text(g, tipB > 0 ? "+팁 많음" : "+팁", cx + 3, cy + 43, 0xFF8C6A3A);
        }
        if (!menu.canServe()) {
            UiKit.center(g, menu.village() ? "서빙하려면 [관리]에서 알바 시작" : "주인 · 직원 · 알바만 서빙할 수 있어요", W / 2, TOP + CH - 9, 0xFF8A5A2A);
        } else if (!any) {
            UiKit.center(g, menu.get(40) == 1 ? "손님을 기다리는 중... (진열대에 요리 · 근처에 입구 필요)" : "영업 OFF", W / 2, TOP + 38, 0xFF7A7A7A);
        }
    }

    private void menuLabels(GuiGraphicsExtractor g) {
        int st = Math.max(1, menu.get(32));
        boolean any = false;
        for (int i = 0; i < 10; i++) {
            ItemStack s = menu.getSlot(i).getItem();
            if (s.isEmpty() || Cooking.dishOf(s) == null) continue;
            any = true;
            int col = i / 5, row = i % 5;
            int ex = 8 + col * 122, ey = TOP + 2 + row * 16;
            String n = s.getHoverName().getString();
            while (UiKit.font().width(n) > 50 && n.length() > 2) n = n.substring(0, n.length() - 1);
            UiKit.text(g, n, ex + 18, ey + 3, 0xFF3A2410);
            UiKit.right(g, won(RestaurantCounterBlockEntity.price(s, st)), ex + 89, ey + 3, 0xFF1E7832);
        }
        if (!any) UiKit.center(g, "진열대에 요리가 없어요", W / 2, TOP + 36, 0xFF8A6A4A);
        UiKit.right(g, "내 통장 " + won(menu.get(46) * 10L), W - 8, TOP + CH - 9 + 0, 0xFF6A4A2A);
    }

    private void ratingLabels(GuiGraphicsExtractor g) {
        int avg10 = menu.get(35), n = menu.get(36);
        UiKit.big(g, n == 0 ? "-" : String.format("%.1f", avg10 / 10.0), 12, TOP + 6, 2, 0xFFE0A020);
        UiKit.text(g, stars(Math.round(avg10 / 10f)), 48, TOP + 6, 0xFFFFB400);
        UiKit.text(g, "평점 " + n + "개", 48, TOP + 17, 0xFF8A6A4A);
        List<String> rv = menu.extra.reviews();
        if (rv.isEmpty()) UiKit.center(g, "아직 후기가 없어요", 112 + (W - 120) / 2, TOP + 36, 0xFFA08A6A);
        for (int i = 0; i < Math.min(6, rv.size()); i++) {
            String[] p = rv.get(i).split("\t", 3);
            int s = p.length > 0 ? parse(p[0]) : 0;
            String line = (p.length > 1 ? p[1] : "?") + (p.length > 2 && !p[2].isBlank() ? ": " + p[2] : "");
            while (UiKit.font().width(line) > W - 160 && line.length() > 3) line = line.substring(0, line.length() - 1);
            UiKit.text(g, "★".repeat(s), 116, TOP + 6 + i * 12, 0xFFFFB400);
            UiKit.text(g, line, 150, TOP + 6 + i * 12, 0xFF4A3420);
        }
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void manageLabels(GuiGraphicsExtractor g) {
        int st = menu.get(32), xp = menu.get(33), next = menu.get(34);
        if (owner()) {
            UiKit.text(g, "알바비", 10, TOP + 37, 0xFF4A3420);
            UiKit.center(g, menu.get(42) + "동", 96, TOP + 37, 0xFF1E7832);
            UiKit.text(g, "직원 몫", 10, TOP + 52, 0xFF4A3420);
            UiKit.center(g, menu.get(43) + "%", 96, TOP + 52, 0xFF1E7832);
            UiKit.text(g, next > 0 ? "평판 " + xp + " / " + next + " (다음 별)" : "평판 최고 ★5", 10, TOP + 68, 0xFF8A5A2A);
            UiKit.text(g, "직원 (최대 3)", 134, TOP + 2, 0xFF2A4A7A);
            List<String> staff = menu.extra.staff();
            for (int i = 0; i < 3; i++) UiKit.text(g, i < staff.size() ? staff.get(i) : "-", 136, TOP + 14 + i * 12, 0xFF3A2410);
            UiKit.text(g, "지원자", 134, TOP + 50, 0xFF2A7A4A);
            List<String> apps = menu.extra.applicants();
            if (apps.isEmpty()) UiKit.text(g, "없음", 136, TOP + 62, 0xFF9A8A7A);
            for (int i = 0; i < Math.min(2, apps.size()); i++) UiKit.text(g, apps.get(i), 136, TOP + 62 + i * 11, 0xFF3A2410);
            g.fill(130, TOP + 2, 131, TOP + CH - 4, 0x30000000);
        } else {
            String role = switch (menu.role()) {
                case RestaurantCounterBlockEntity.ROLE_STAFF -> "직원 (매출의 " + menu.get(43) + "% + 팁)";
                case RestaurantCounterBlockEntity.ROLE_ALBA -> "오늘 알바 중" + (menu.village() ? " (매출 20% + 팁)" : " (서빙당 " + menu.get(42) + "동 + 팁)");
                default -> "손님";
            };
            UiKit.text(g, "주인: " + menu.extra.owner() + "   ·   나: " + role, 10, TOP + 6, 0xFF3A2410);
            String info = menu.village() ? "마을 식당: [알바 시작] 하면 오늘 하루 서빙할 수 있어요"
                    : menu.get(41) == 1 ? "알바 모집 중 · 알바비 서빙당 " + menu.get(42) + "동 + 팁" : "지금은 알바를 모집하지 않아요";
            UiKit.text(g, info, 10, TOP + 20, 0xFF6A4A2A);
            UiKit.text(g, "직원은 주인이 수락해야 돼요 · 진열대도 채울 수 있어요 (매출의 " + menu.get(43) + "%)", 10, TOP + 62, 0xFF8A7A6A);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

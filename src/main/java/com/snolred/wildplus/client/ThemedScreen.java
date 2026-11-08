package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 3.9 테마 화면: 예전엔 기본 목록이던 화면들을 시설마다 다르게 꾸민다 (PickMenu.theme).
 * 약국 · 기상 관측소 · 내 땅 관리 · 순간이동 지점 · 주말 대회 게시판 · 순간이동기 · 흉조 계약서 · 우민 감옥 · 흉조 제단.
 * 고르면 PickMenu 와 똑같이 번호를 서버로 보낸다.
 */
public class ThemedScreen extends AbstractContainerScreen<PickMenu> {
    private final int theme;
    private final int w, h;
    private final List<Card> cards = new ArrayList<>();
    private UiButton prev, next, action;
    private int page, selected;
    private net.minecraft.client.gui.components.EditBox nameBox;

    public ThemedScreen(PickMenu menu, Inventory inv, Component title) {
        this(menu, inv, title, PickMenu.theme(menu.flags));
    }

    private ThemedScreen(PickMenu menu, Inventory inv, Component title, int theme) {
        super(menu, inv, title, width(theme), height(theme, menu));
        this.theme = theme;
        this.w = width(theme);
        this.h = height(theme, menu);
        for (int i = 0; i < menu.entries.size(); i++) {
            if (menu.entries.get(i).kind() != PickMenu.K_INFO) {
                selected = i;
                break;
            }
        }
    }

    private static int width(int t) {
        return switch (t) {
            case PickMenu.T_LAND -> 344;
            case PickMenu.T_CONTRACT, PickMenu.T_ALTAR -> 324;
            default -> 336;
        };
    }

    private static int height(int t, PickMenu m) {
        return switch (t) {
            case PickMenu.T_PHARMACY -> 216;
            case PickMenu.T_WEATHER -> 212;
            case PickMenu.T_LAND -> 230;
            case PickMenu.T_WARP -> 214 + ((m.flags & PickMenu.FLAG_CAN_RENAME) != 0 ? 24 : 0);
            case PickMenu.T_CONTEST -> 206;
            case PickMenu.T_TELEPORT -> 176;
            case PickMenu.T_CONTRACT -> 214;
            case PickMenu.T_PRISON -> 200;
            case PickMenu.T_ALTAR -> 176;
            default -> 200;
        };
    }

    // ───────── 배치 ─────────
    /** 이 테마에서 한 쪽에 보이는 칸 수 */
    private int per() {
        return switch (theme) {
            case PickMenu.T_PHARMACY -> 9;
            case PickMenu.T_WEATHER -> 6;
            case PickMenu.T_LAND -> 6;
            case PickMenu.T_WARP -> 5;
            case PickMenu.T_TELEPORT -> 4;
            case PickMenu.T_CONTRACT -> 6;
            case PickMenu.T_PRISON -> 4;
            case PickMenu.T_ALTAR -> 5;
            case PickMenu.T_CONTEST -> 0; // 게시판은 배경에 직접 씀
            default -> 8;
        };
    }

    /** 칸으로 그릴 줄들 (정보 줄 · 큰 버튼은 따로) */
    private List<Integer> items() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < menu.entries.size(); i++) {
            int k = menu.entries.get(i).kind();
            boolean skip = switch (theme) {
                case PickMenu.T_LAND -> i == 0;
                case PickMenu.T_CONTRACT -> k == PickMenu.K_INFO || k == PickMenu.K_PRIMARY;
                case PickMenu.T_ALTAR -> i == 0;
                case PickMenu.T_PRISON -> prisonActions() && k == PickMenu.K_SMALL;
                default -> false;
            };
            if (!skip) out.add(i);
        }
        return out;
    }

    /** 감옥 포로 하나를 고른 뒤의 화면 (몸값 · 전향 · 심문) */
    private boolean prisonActions() {
        for (PickMenu.Entry e : menu.entries) if (e.kind() == PickMenu.K_PRIMARY) return true;
        return false;
    }

    private int[] slot(int r) {
        int x = leftPos, y = topPos;
        return switch (theme) {
            case PickMenu.T_PHARMACY -> new int[]{x + 8 + (r % 3) * 68, y + 32 + (r / 3) * 54, 64, 50};
            case PickMenu.T_WEATHER -> new int[]{x + 126 + (r % 2) * 102, y + 34 + (r / 2) * 52, 98, 48};
            case PickMenu.T_LAND -> new int[]{x + 188, y + 92 + r * 18, w - 196, 16};
            case PickMenu.T_WARP -> new int[]{x + 8, y + 32 + r * 31, w - 16, 28};
            case PickMenu.T_TELEPORT -> new int[]{x + 10 + r * 80, y + 34, 74, 116};
            case PickMenu.T_CONTRACT -> new int[]{x + 10 + (r % 3) * 102, y + 40 + (r / 3) * 64, 96, 58};
            case PickMenu.T_PRISON -> prisonActions() ? new int[]{x + 14 + r * 104, y + 70, 98, 74} : new int[]{x + 10 + r * 80, y + 36, 74, 128};
            case PickMenu.T_ALTAR -> new int[]{x + 8 + r * 62, y + 72, 58, 72};
            default -> new int[]{x + 8, y + 30 + r * 20, w - 16, 18};
        };
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();
        for (int r = 0; r < per(); r++) {
            int[] s = slot(r);
            final int row = r;
            cards.add(addRenderableWidget(new Card(s[0], s[1], s[2], s[3], () -> clickCard(row))));
        }
        int by = topPos + h - 22;
        int px = theme == PickMenu.T_LAND ? leftPos + 188 : leftPos + 8;
        if (theme != PickMenu.T_CONTEST) {
            prev = addRenderableWidget(UiButton.of(px, by, 20, 16, "◀", UiButton.WHITE, () -> { page--; refresh(); }));
            next = addRenderableWidget(UiButton.of(px + 24, by, 20, 16, "▶", UiButton.WHITE, () -> { page++; refresh(); }));
        }
        if (theme == PickMenu.T_PHARMACY) {
            action = addRenderableWidget(new UiButton(leftPos + 218, topPos + 168, w - 226, 22, Component.literal("결제하고 마시기"), UiButton.GREEN, () -> send(selected)));
        } else if (theme == PickMenu.T_CONTRACT) {
            int sign = -1;
            for (int i = 0; i < menu.entries.size(); i++) if (menu.entries.get(i).kind() == PickMenu.K_PRIMARY) sign = i;
            final int s = sign;
            if (s >= 0) action = addRenderableWidget(new UiButton(leftPos + w - 120, topPos + h - 30, 110, 22, Component.literal("✒ 서명하고 종 치기"), UiButton.RED, () -> send(s)));
        } else if (theme == PickMenu.T_PRISON && prisonActions()) {
            for (int i = 0; i < menu.entries.size(); i++) {
                if (menu.entries.get(i).kind() == PickMenu.K_SMALL) {
                    final int s = i;
                    addRenderableWidget(UiButton.of(leftPos + w - 92, topPos + h - 24, 84, 17, "◀ 감옥으로", UiButton.WHITE, () -> send(s)));
                }
            }
        } else if (theme == PickMenu.T_WARP) {
            if ((menu.flags & PickMenu.FLAG_CAN_TOGGLE) != 0) {
                boolean pub = (menu.flags & PickMenu.FLAG_PUBLIC) != 0;
                addRenderableWidget(new UiButton(leftPos + w - 110, by, 100, 16, Component.translatable(pub ? "gui.wildplus.warp.make_private" : "gui.wildplus.warp.make_public"),
                        pub ? UiButton.DARK : UiButton.TEAL, () -> send(PickMenu.BTN_TOGGLE)));
            }
            if ((menu.flags & PickMenu.FLAG_CAN_RENAME) != 0) {
                nameBox = addRenderableWidget(new net.minecraft.client.gui.components.EditBox(this.font, leftPos + 10, by - 24, w - 104, 16,
                        Component.translatable("gui.wildplus.warp.rename")));
                nameBox.setMaxLength(32);
                nameBox.setHint(Component.translatable("gui.wildplus.warp.rename_hint"));
                addRenderableWidget(new UiButton(leftPos + w - 90, by - 25, 80, 17, Component.translatable("gui.wildplus.warp.rename"), UiButton.BLUE, () -> {
                    String name = nameBox.getValue().trim();
                    if (!name.isEmpty() && this.minecraft != null && this.minecraft.player != null) this.minecraft.player.connection.sendCommand("warpname " + name);
                }));
            }
        }
        refresh();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (nameBox != null && nameBox.isFocused() && !event.isEscape()) {
            nameBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    private int pages() {
        if (per() <= 0) return 1;
        return Math.max(1, (items().size() + per() - 1) / per());
    }

    private void refresh() {
        List<Integer> it = items();
        page = Math.max(0, Math.min(page, pages() - 1));
        for (int r = 0; r < cards.size(); r++) {
            int k = page * per() + r;
            Card c = cards.get(r);
            c.index = k < it.size() ? it.get(k) : -1;
            c.visible = c.index >= 0 || (theme == PickMenu.T_PRISON && !prisonActions());
            c.active = c.index >= 0 && menu.entries.get(c.index).kind() != PickMenu.K_INFO;
        }
        if (prev != null) {
            prev.visible = next.visible = pages() > 1;
            prev.active = page > 0;
            next.active = page < pages() - 1;
        }
        if (action != null && theme == PickMenu.T_PHARMACY) action.visible = selected >= 0 && selected < menu.entries.size();
    }

    private void clickCard(int row) {
        List<Integer> it = items();
        int k = page * per() + row;
        if (k >= it.size()) return;
        int idx = it.get(k);
        if (theme == PickMenu.T_PHARMACY) {
            selected = idx;
            refresh();
            return;
        }
        send(idx);
    }

    private void send(int idx) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, idx);
    }

    // ───────── 그리기 ─────────
    private static String[] lines(PickMenu.Entry e) {
        return e.sub().split("\n");
    }

    private static String strip(String s) {
        return s.replaceAll("^[☐☑✔🔒⛓☠📜🛡💰]+\\s*", "").trim();
    }

    private static List<String> wrap(String s, int maxW) {
        var font = UiKit.font();
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n")) {
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                String t = cur.isEmpty() ? word : cur + " " + word;
                if (font.width(t) > maxW && !cur.isEmpty()) {
                    out.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    cur = new StringBuilder(t);
                }
            }
            out.add(cur.toString());
        }
        return out;
    }

    private static String fit(String s, int maxW) {
        var font = UiKit.font();
        while (font.width(s) > maxW && s.length() > 2) s = s.substring(0, s.length() - 2) + "…";
        return s;
    }

    private void icon(GuiGraphicsExtractor g, ItemStack s, int x, int y, float scale) {
        if (s.isEmpty()) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.item(s, 0, 0);
        g.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        String title = strip(this.title.getString());
        switch (theme) {
            case PickMenu.T_PHARMACY -> {
                UiKit.round(g, x, y, w, h, 4, 0xFF2E8C6C);
                UiKit.round(g, x + 2, y + 2, w - 4, h - 4, 3, 0xFFECF6F2);
                UiKit.round(g, x + 2, y + 2, w - 4, 24, 3, 0xFF36A07C);
                g.fill(x + 9, y + 10, x + 21, y + 14, 0xFFFFFFFF);
                g.fill(x + 13, y + 6, x + 17, y + 18, 0xFFFFFFFF);
                // 오른쪽 상세
                UiKit.round(g, x + 214, y + 32, w - 222, 162, 3, 0xFFFFFFFF);
                UiKit.outline(g, x + 214, y + 32, w - 222, 162, 0xFFC8DCD4);
                if (selected >= 0 && selected < menu.entries.size()) {
                    PickMenu.Entry e = menu.entries.get(selected);
                    int cx = x + 214 + (w - 222) / 2;
                    icon(g, e.icon(), cx - 16, y + 38, 2F);
                    UiKit.center(g, strip(e.title()), cx, y + 74, 0xFF284038);
                    String[] ls = lines(e);
                    int ly = y + 88;
                    for (int i = 1; i < ls.length; i++) {
                        for (String l : wrap(ls[i], w - 236)) {
                            if (ly > y + 150) break;
                            UiKit.text(g, l, x + 220, ly, 0xFF4A5E56);
                            ly += 10;
                        }
                    }
                    UiKit.text(g, "가격 " + ls[0], x + 220, y + 154, 0xFF1E8A5E);
                }
                UiKit.text(g, menu.header, x + 60, y + h - 18, 0xFF3C6E5E);
            }
            case PickMenu.T_WEATHER -> {
                Minecraft mc = Minecraft.getInstance();
                boolean rain = mc.level != null && mc.level.isRaining(), storm = mc.level != null && mc.level.isThundering();
                long time = mc.level == null ? 6000 : mc.level.getOverworldClockTime();
                int tod = (int) (time % 24000L);
                boolean night = tod >= 13000 && tod < 23000;
                int top = night ? 0xFF1C2A55 : storm ? 0xFF4A5468 : rain ? 0xFF6B7F99 : 0xFF5AA0E6;
                int bot = night ? 0xFF3A4C80 : storm ? 0xFF6E7890 : rain ? 0xFF9AAAC0 : 0xFFA8D4F6;
                UiKit.round(g, x, y, w, h, 4, 0xFF28507E);
                UiKit.roundGradient(g, x + 2, y + 2, w - 4, h - 4, 3, top, bot);
                UiKit.round(g, x + 8, y + 32, 112, 162, 4, 0x99FFFFFF);
                int cx = x + 64, cy = y + 74;
                if (storm || rain) {
                    UiKit.circle(g, cx - 22, cy - 10, 26, 0xFF9AA4B4);
                    UiKit.circle(g, cx - 6, cy - 18, 32, 0xFFB0BACA);
                    for (int k = 0; k < 4; k++) g.fill(cx - 16 + k * 9, cy + 16, cx - 14 + k * 9, cy + 24, 0xFF4A80E0);
                    if (storm) {
                        g.fill(cx, cy + 8, cx + 4, cy + 16, 0xFFFFE040);
                        g.fill(cx - 4, cy + 16, cx + 2, cy + 22, 0xFFFFE040);
                    }
                } else if (night) {
                    UiKit.circle(g, cx - 16, cy - 16, 32, 0xFFF2F0D0);
                    UiKit.circle(g, cx - 8, cy - 22, 30, UiKit.alpha(top, 0xFF));
                } else {
                    UiKit.circle(g, cx - 18, cy - 18, 36, 0xFFFFD24E);
                    for (int k = 0; k < 8; k++) {
                        double a = Math.PI / 4 * k;
                        int px = cx + (int) (Math.cos(a) * 26), py = cy + (int) (Math.sin(a) * 26);
                        g.fill(px - 2, py - 2, px + 2, py + 2, 0xFFFFC030);
                    }
                }
                String now = storm ? "뇌우" : rain ? "비" : night ? "맑은 밤" : "맑음";
                int hour = (tod / 1000 + 6) % 24, minute = (tod % 1000) * 60 / 1000;
                UiKit.center(g, "지금: " + now, cx, y + 118, 0xFF203A60);
                UiKit.center(g, String.format("%s %d:%02d · %d일째", hour < 12 ? "오전" : "오후", hour % 12 == 0 ? 12 : hour % 12, minute, time / 24000L + 1), cx, y + 132, 0xFF405A80);
                UiKit.center(g, "모두에게 적용돼요", cx, y + 176, 0xFF506A90);
            }
            case PickMenu.T_LAND -> {
                UiKit.round(g, x, y, w, h, 4, 0xFF7A5C38);
                UiKit.round(g, x + 2, y + 2, w - 4, h - 4, 3, 0xFFF6F0E2);
                PickMenu.Entry info = menu.entries.isEmpty() ? null : menu.entries.get(0);
                String[] parts = info == null ? new String[0] : info.sub().split("\\|");
                String map = parts.length > 0 && parts[0].startsWith("MAP:") ? parts[0].substring(4) : "";
                int mx = x + 10, my = y + 30, cs = 18;
                for (int i = 0; i < 81; i++) {
                    int cx = i % 9, cy = i / 9;
                    char c = i < map.length() ? map.charAt(i) : '0';
                    int col = c == '1' ? 0xFF78BE6E : c == '2' ? 0xFF6E96DC : c == '3' ? 0xFFDC786E : 0xFFD6CEBA;
                    g.fill(mx + cx * cs, my + cy * cs, mx + cx * cs + cs - 1, my + cy * cs + cs - 1, col);
                    if (c == '1') UiKit.center(g, "▲", mx + cx * cs + cs / 2, my + cy * cs + 5, 0xFF2A5A28);
                }
                UiKit.outline(g, mx + 4 * cs - 1, my + 4 * cs - 1, cs + 1, cs + 1, 0xFFFFFFFF);
                UiKit.outline(g, mx + 4 * cs - 2, my + 4 * cs - 2, cs + 3, cs + 3, 0xFF2A2A2A);
                int[][] legend = {{0xFF78BE6E}, {0xFF6E96DC}, {0xFFDC786E}, {0xFFD6CEBA}};
                String[] ln = {"내 땅", "친구 땅", "남의 땅", "빈 땅"};
                for (int k = 0; k < 4; k++) {
                    g.fill(x + 10 + k * 42, y + h - 14, x + 18 + k * 42, y + h - 6, legend[k][0]);
                    UiKit.text(g, ln[k], x + 20 + k * 42, y + h - 15, 0xFF6A5030);
                }
                UiKit.round(g, x + 184, y + 30, w - 192, 58, 3, 0xFFFFFCF4);
                for (int k = 1; k < parts.length && k <= 5; k++) {
                    String[] kv = parts[k].split("=", 2);
                    UiKit.text(g, kv[0], x + 190, y + 34 + (k - 1) * 10, 0xFF8C6E46);
                    if (kv.length > 1) UiKit.right(g, fit(kv[1], w - 250), x + w - 12, y + 34 + (k - 1) * 10, 0xFF3C2C18);
                }
            }
            case PickMenu.T_WARP -> {
                UiKit.round(g, x, y, w, h, 4, 0xFF5A78C8);
                UiKit.roundGradient(g, x + 2, y + 2, w - 4, h - 4, 3, 0xFF1A1E2C, 0xFF262C40);
                if (menu.entries.isEmpty()) {
                    for (String l : wrap(menu.empty, w - 40)) UiKit.center(g, l, x + w / 2, y + 80, 0xFF9CB0E0);
                }
            }
            case PickMenu.T_CONTEST -> {
                UiKit.round(g, x, y, w, h, 4, 0xFF6E4A28);
                g.fillGradient(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFB4844E, 0xFFA0703E);
                UiKit.speckle(g, x + 3, y + 3, w - 6, h - 6, 17, 260, 0x30603A14, 0x18FFFFFF);
                UiKit.round(g, x + 70, y + 6, w - 140, 18, 2, 0xFFFAF4E4);
                UiKit.round(g, x + 12, y + 30, 168, h - 40, 1, 0xFFFFFAEC);
                UiKit.circle(g, x + 92, y + 26, 7, 0xFFD83A3A);
                List<Integer> it = items();
                int ly = y + 38;
                for (int k = 0; k < it.size() && ly < y + h - 20; k++) {
                    PickMenu.Entry e = menu.entries.get(it.get(k));
                    int c = k == 0 ? 0xFFB8860B : k == 1 ? 0xFF7A7A82 : k == 2 ? 0xFF9A5A2A : 0xFF4A3420;
                    for (String l : wrap(e.title(), 154)) {
                        UiKit.text(g, l, x + 20, ly, c);
                        ly += 10;
                    }
                    if (!e.sub().isEmpty()) {
                        UiKit.text(g, fit(e.sub(), 150), x + 24, ly, 0xFF8C6E46);
                        ly += 10;
                    }
                    ly += 4;
                }
                if (it.isEmpty()) for (String l : wrap(menu.empty, 150)) { UiKit.text(g, l, x + 20, ly, 0xFF8C6E46); ly += 10; }
                UiKit.round(g, x + 190, y + 30, w - 202, h - 40, 1, 0xFFFFEBA0);
                UiKit.circle(g, x + 190 + (w - 202) / 2 - 3, y + 26, 7, 0xFF3A78DC);
                int ny = y + 40;
                for (String l : wrap(menu.header.replace(" · ", "\n"), w - 220)) {
                    UiKit.text(g, l, x + 198, ny, 0xFF5A3A14);
                    ny += 11;
                }
            }
            case PickMenu.T_TELEPORT -> {
                UiKit.round(g, x, y, w, h, 4, 0xFFA064E6);
                UiKit.roundGradient(g, x + 2, y + 2, w - 4, h - 4, 3, 0xFF22183A, 0xFF34244E);
                if (menu.entries.isEmpty()) for (String l : wrap(menu.empty, w - 40)) UiKit.center(g, l, x + w / 2, y + 80, 0xFFC8B4F0);
            }
            case PickMenu.T_CONTRACT -> {
                UiKit.round(g, x, y, w, h, 6, 0xFF785A32);
                g.fillGradient(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFEAD6AA, 0xFFDCC490);
                UiKit.speckle(g, x + 3, y + 3, w - 6, h - 6, 5, 200, 0x22785A32, 0x22FFFFFF);
                for (PickMenu.Entry e : menu.entries) {
                    if (e.kind() == PickMenu.K_INFO) {
                        UiKit.round(g, x + 10, y + h - 32, w - 140, 24, 3, 0xFF3C2A1E);
                        UiKit.text(g, fit(e.title(), w - 160), x + 16, y + h - 29, 0xFFFFD25A);
                        UiKit.text(g, fit(e.sub(), w - 160), x + 16, y + h - 18, 0xFFE6D2B0);
                    }
                }
            }
            case PickMenu.T_PRISON -> {
                UiKit.round(g, x, y, w, h, 4, 0xFF2A2A30);
                UiKit.roundGradient(g, x + 2, y + 2, w - 4, h - 4, 3, 0xFF4A4A54, 0xFF3A3A42);
                for (int k = 0; k < 12; k++) g.fill(x + 10 + k * 27, y + 4, x + 13 + k * 27, y + 28, 0xFF2E3038);
                if (prisonActions() && !menu.entries.isEmpty()) {
                    UiKit.center(g, "포로를 어떻게 할까요?", x + w / 2, y + 50, 0xFFE6E0D0);
                }
            }
            case PickMenu.T_ALTAR -> {
                UiKit.round(g, x, y, w, h, 4, 0xFF78708C);
                UiKit.roundGradient(g, x + 2, y + 2, w - 4, h - 4, 3, 0xFF2E2C3A, 0xFF3A3848);
                PickMenu.Entry info = menu.entries.isEmpty() ? null : menu.entries.get(0);
                int cur = 0, max = 100;
                if (info != null) {
                    var m = java.util.regex.Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(info.title());
                    if (m.find()) {
                        cur = Integer.parseInt(m.group(1));
                        max = Math.max(1, Integer.parseInt(m.group(2)));
                    }
                }
                UiKit.round(g, x + 10, y + 34, w - 20, 14, 4, 0xFF1C1C24);
                int fill = (int) ((w - 24) * Math.min(1F, cur / (float) max));
                if (fill > 0) UiKit.roundGradient(g, x + 12, y + 36, fill, 10, 3, 0xFF7CF0DC, 0xFF3CB4A0);
                if (info != null) {
                    UiKit.center(g, info.title(), x + w / 2, y + 37, 0xFFFFFFFF);
                    UiKit.center(g, fit(info.sub(), w - 20), x + w / 2, y + 54, 0xFFB4F0E6);
                }
            }
            default -> {}
        }
        if (menu.entries.isEmpty() && theme != PickMenu.T_WARP && theme != PickMenu.T_TELEPORT && theme != PickMenu.T_CONTEST && theme != PickMenu.T_PRISON) {
            for (String l : wrap(menu.empty, w - 40)) UiKit.center(g, l, x + w / 2, y + 90, 0xFF808080);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String title = strip(this.title.getString());
        int light = 0xFFFFFFFF;
        switch (theme) {
            case PickMenu.T_PHARMACY -> {
                UiKit.bold(g, title, 26, 6, light);
                UiKit.right(g, "PHARMACY", w - 10, 6, 0xAAE6FFF4);
                UiKit.text(g, "고르면 오른쪽에 설명 · 결제하면 바로 마셔요", 26, 16, 0xFFD8F4EA);
            }
            case PickMenu.T_WEATHER -> {
                UiKit.bold(g, "☁ " + title, 10, 8, light);
                UiKit.text(g, menu.header, 10, 19, 0xFFE6F2FF);
            }
            case PickMenu.T_LAND -> {
                UiKit.bold(g, "🏠 " + title, 10, 8, 0xFF4A3218);
                UiKit.text(g, fit(menu.header, w - 100), 90, 8, 0xFF8C6E46);
            }
            case PickMenu.T_WARP -> {
                UiKit.bold(g, "✦ " + title, 10, 8, 0xFFC8DCFF);
                UiKit.text(g, fit(menu.header, w - 20), 10, 19, 0xFF8CA0D2);
            }
            case PickMenu.T_CONTEST -> UiKit.center(g, title, w / 2, 11, 0xFF5A3214);
            case PickMenu.T_TELEPORT -> {
                UiKit.bold(g, "🌀 " + title, 10, 8, 0xFFE6D2FF);
                UiKit.text(g, menu.header, 10, 19, 0xFFB4A0DC);
            }
            case PickMenu.T_CONTRACT -> {
                UiKit.big(g, title, w / 2 - UiKit.bigWidth(title, 2) / 2, 7, 2, 0xFF6E1E1E);
                UiKit.center(g, menu.header, w / 2, 27, 0xFF6A5030);
            }
            case PickMenu.T_PRISON -> {
                UiKit.center(g, title, w / 2, 8, 0xFFF0E8D8);
                UiKit.center(g, fit(menu.header, w - 20), w / 2, 19, 0xFFC0BCB4);
            }
            case PickMenu.T_ALTAR -> {
                UiKit.center(g, title, w / 2, 8, 0xFFB4FFF0);
                UiKit.center(g, menu.header, w / 2, 20, 0xFF9AC8C0);
            }
            default -> UiKit.bold(g, title, 8, 8, 0xFF404040);
        }
        if (pages() > 1 && theme != PickMenu.T_CONTEST) UiKit.text(g, (page + 1) + " / " + pages(), theme == PickMenu.T_LAND ? 238 : 58, h - 18, theme == PickMenu.T_LAND || theme == PickMenu.T_PHARMACY ? 0xFF6A5030 : 0xFFE0E0E0);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    // ───────── 칸 ─────────
    private final class Card extends UiButton {
        int index = -1;

        Card(int x, int y, int cw, int ch, Runnable action) {
            super(x, y, cw, ch, Component.empty(), UiButton.WHITE, action);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), cw = getWidth(), ch = getHeight();
            boolean hover = isHoveredOrFocused() && index >= 0;
            PickMenu.Entry e = index >= 0 ? menu.entries.get(index) : null;
            var font = UiKit.font();
            switch (theme) {
                case PickMenu.T_PHARMACY -> {
                    boolean sel = index == selected;
                    UiKit.round(g, x, y, cw, ch, 3, sel ? 0xFF36A07C : hover ? 0xFF9CD8C2 : 0xFFC8DCD4);
                    UiKit.round(g, x + 1, y + 1, cw - 2, ch - 2, 3, 0xFFFFFFFF);
                    icon(g, e.icon(), x + cw / 2 - 8, y + 3, 1F);
                    UiKit.center(g, fit(strip(e.title()), cw - 4), x + cw / 2, y + 22, 0xFF284038);
                    String price = lines(e)[0];
                    int pw = font.width(price) + 8;
                    UiKit.round(g, x + cw / 2 - pw / 2, y + 34, pw, 11, 3, 0xFFE2F6EE);
                    UiKit.center(g, price, x + cw / 2, y + 36, 0xFF1E8A5E);
                }
                case PickMenu.T_WEATHER -> {
                    UiKit.round(g, x, y, cw, ch, 4, hover ? 0xFF2E6EB8 : 0xFF4A7CB0);
                    UiKit.round(g, x + 1, y + 1, cw - 2, ch - 2, 4, hover ? 0xF0FFFFFF : 0xE0FFFFFF);
                    String t = strip(e.title());
                    int cx = x + 18, cy = y + 24;
                    if (t.contains("뇌우")) {
                        UiKit.circle(g, cx - 10, cy - 10, 18, 0xFF9AA4B4);
                        g.fill(cx - 1, cy + 4, cx + 3, cy + 12, 0xFFFFD830);
                    } else if (t.contains("비")) {
                        UiKit.circle(g, cx - 10, cy - 10, 18, 0xFFA8B4C6);
                        for (int k = 0; k < 3; k++) g.fill(cx - 7 + k * 6, cy + 6, cx - 5 + k * 6, cy + 12, 0xFF4A80E0);
                    } else if (t.contains("밤")) {
                        UiKit.circle(g, cx - 9, cy - 9, 18, 0xFFE6E4C4);
                        UiKit.circle(g, cx - 4, cy - 12, 16, 0xFFF4F8FF);
                    } else if (t.contains("아침")) {
                        UiKit.circle(g, cx - 10, cy - 6, 20, 0xFFFFA050);
                        g.fill(cx - 13, cy + 3, cx + 13, cy + 14, 0xF0FFFFFF);
                        g.fill(cx - 13, cy + 3, cx + 13, cy + 4, 0xFF8C6A3C);
                    } else {
                        UiKit.circle(g, cx - 9, cy - 9, 18, 0xFFFFC83C);
                    }
                    UiKit.text(g, fit(t, cw - 40), x + 36, y + 12, 0xFF20385A);
                    UiKit.text(g, lines(e)[0], x + 36, y + 28, 0xFF1E64B4);
                }
                case PickMenu.T_LAND -> {
                    String t = e.title();
                    int c = t.contains("팔기") ? 0xFFF0C8BE : t.contains("사기") || t.contains("기둥") || t.contains("테두리") ? 0xFFC0E2B4 : 0xFFE6DCC4;
                    UiKit.round(g, x, y, cw, ch, 3, hover ? UiKit.darker(c, 0.1F) : c);
                    UiKit.text(g, fit(t, cw - 70), x + 6, y + 4, 0xFF46301A);
                    String sub = e.sub();
                    if (!sub.isEmpty()) UiKit.right(g, fit(sub, 64), x + cw - 6, y + 4, 0xFF8C6E46);
                }
                case PickMenu.T_WARP -> {
                    boolean first = index == 0;
                    UiKit.round(g, x, y, cw, ch, 3, hover ? 0xFF3C5094 : first ? 0xFF324478 : 0xFF2A3048);
                    String[] ls = lines(e);
                    UiKit.text(g, fit(e.title(), cw - 140), x + 8, y + 4, first ? 0xFFFFDC78 : 0xFFE6ECFF);
                    UiKit.text(g, fit(ls[0], cw - 140), x + 8, y + 16, 0xFF96A6D2);
                    if (ls.length > 1) UiKit.right(g, ls[1], x + cw - 56, y + 10, 0xFFB4C8F0);
                    UiKit.round(g, x + cw - 48, y + 6, 40, 16, 3, hover ? 0xFF6EE6A8 : 0xFF5AC88E);
                    UiKit.center(g, "출발", x + cw - 28, y + 10, 0xFF143224);
                }
                case PickMenu.T_TELEPORT -> {
                    UiKit.round(g, x, y, cw, ch, 4, hover ? 0xFFB482FF : 0xFF7850B4);
                    UiKit.round(g, x + 1, y + 1, cw - 2, ch - 2, 4, 0xFF36284E);
                    int hue = Math.abs(e.title().hashCode());
                    int skin = UiKit.mix(0xFFC8A078, 0xFF6E4A30, (hue % 100) / 100F);
                    g.fill(x + cw / 2 - 16, y + 10, x + cw / 2 + 16, y + 42, skin);
                    g.fill(x + cw / 2 - 16, y + 10, x + cw / 2 + 16, y + 18, 0xFF3C2814);
                    g.fill(x + cw / 2 - 10, y + 26, x + cw / 2 - 5, y + 30, 0xFF282840);
                    g.fill(x + cw / 2 + 5, y + 26, x + cw / 2 + 10, y + 30, 0xFF282840);
                    UiKit.center(g, fit(e.title(), cw - 6), x + cw / 2, y + 50, 0xFFF0E6FF);
                    int ly = y + 62;
                    for (String l : wrap(e.sub().replace(" · ", "\n"), cw - 8)) {
                        if (ly > y + ch - 26) break;
                        UiKit.center(g, l, x + cw / 2, ly, 0xFFB4A0DC);
                        ly += 9;
                    }
                    UiKit.round(g, x + 10, y + ch - 20, cw - 20, 14, 3, hover ? 0xFFC896FF : 0xFF9660E6);
                    UiKit.center(g, "가기", x + cw / 2, y + ch - 17, 0xFFFFFFFF);
                }
                case PickMenu.T_CONTRACT -> {
                    boolean on = e.title().startsWith("☑");
                    UiKit.round(g, x, y, cw, ch, 4, on ? 0xFFB42832 : hover ? 0xFFB49664 : 0xFF96784E);
                    UiKit.round(g, x + 2, y + 2, cw - 4, ch - 4, 3, on ? 0xFF4A1C24 : 0xFFF6EBD2);
                    String t = strip(e.title());
                    String bonus = t.contains("+") ? t.substring(t.indexOf('+')).trim() : "";
                    String name = t.contains("+") ? t.substring(0, t.indexOf('+')).trim() : t;
                    UiKit.center(g, name, x + cw / 2, y + 7, on ? 0xFFFFE6C8 : 0xFF3C281E);
                    int ly = y + 19;
                    for (String l : wrap(e.sub(), cw - 10)) {
                        if (ly > y + ch - 20) break;
                        UiKit.center(g, l, x + cw / 2, ly, on ? 0xFFE6C8B4 : 0xFF6E5A46);
                        ly += 9;
                    }
                    UiKit.center(g, bonus, x + cw / 2, y + ch - 13, on ? 0xFFFFD25A : 0xFFA07832);
                    if (on) UiKit.text(g, "✔", x + cw - 11, y + 4, 0xFFFFD25A);
                }
                case PickMenu.T_PRISON -> {
                    if (prisonActions()) {
                        if (e == null) return;
                        boolean info = e.kind() == PickMenu.K_INFO;
                        int base = info ? 0xFF6E6E74 : index == 0 ? 0xFFC8A032 : index == 1 ? 0xFF326EBE : 0xFF7850A0;
                        UiKit.round(g, x, y, cw, ch, 4, hover && !info ? UiKit.lighter(base, 0.2F) : base);
                        icon(g, e.icon(), x + cw / 2 - 8, y + 6, 1F);
                        UiKit.center(g, fit(strip(e.title()), cw - 6), x + cw / 2, y + 26, 0xFFFFFFFF);
                        int ly = y + 40;
                        for (String l : wrap(e.sub(), cw - 8)) {
                            if (ly > y + ch - 8) break;
                            UiKit.center(g, l, x + cw / 2, ly, 0xFFEFEFEF);
                            ly += 9;
                        }
                        return;
                    }
                    UiKit.round(g, x, y, cw, ch, 3, 0xFF26262C);
                    UiKit.round(g, x + 2, y + 2, cw - 4, ch - 4, 2, e == null ? 0xFF34343A : hover ? 0xFF5A5246 : 0xFF48423A);
                    if (e != null) {
                        icon(g, e.icon(), x + cw / 2 - 16, y + 22, 2F);
                        UiKit.center(g, fit(strip(e.title()), cw - 6), x + cw / 2, y + 66, 0xFFF0E6D2);
                        int ly = y + 80;
                        for (String l : wrap(e.sub(), cw - 8)) {
                            if (ly > y + ch - 10) break;
                            UiKit.center(g, l, x + cw / 2, ly, 0xFFC8BCA8);
                            ly += 9;
                        }
                    } else {
                        UiKit.center(g, "빈 방", x + cw / 2, y + ch / 2 - 4, 0xFF808088);
                    }
                    for (int k = 0; k < 5; k++) g.fill(x + 6 + k * 15, y + 2, x + 8 + k * 15, y + ch - 2, 0x99707680);
                    g.fill(x + 2, y + 2, x + cw - 2, y + 4, 0xFF707680);
                    g.fill(x + 2, y + ch - 4, x + cw - 2, y + ch - 2, 0xFF707680);
                }
                case PickMenu.T_ALTAR -> {
                    boolean on = e.title().startsWith("✔");
                    UiKit.round(g, x, y, cw, ch, 4, on ? 0xFF3CBEA8 : 0xFF50505C);
                    icon(g, e.icon(), x + cw / 2 - 8, y + 6, 1F);
                    String t = strip(e.title());
                    String[] tw = t.split(" · ", 2);
                    UiKit.center(g, tw[0], x + cw / 2, y + 26, on ? 0xFF143C36 : 0xFFD0D0D8);
                    int ly = y + 38;
                    if (tw.length > 1) {
                        for (String l : wrap(tw[1], cw - 6)) {
                            if (ly > y + ch - 12) break;
                            UiKit.center(g, l, x + cw / 2, ly, on ? 0xFF143C36 : 0xFFB0B0B8);
                            ly += 9;
                        }
                    }
                    UiKit.center(g, on ? "적용 중" : lines(e)[0].split(" · ")[0], x + cw / 2, y + ch - 11, on ? 0xFF0E3A32 : 0xFF9090A0);
                }
                default -> {
                    UiKit.round(g, x, y, cw, ch, 2, hover ? 0xFFE0E6EE : 0xFFF4F6F8);
                    UiKit.text(g, fit(e.title(), cw - 10), x + 6, y + 5, 0xFF1E2430);
                }
            }
        }
    }
}

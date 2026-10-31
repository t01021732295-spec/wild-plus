package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 목록에서 하나를 고르는 화면 (워프 목적지, 텔레포트 대상). 한 쪽에 7개씩 넘겨 본다. */
public class PickScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 240, ROWS = 7;
    /** 목록 줄: 흰 카드, 마우스 올리면 옅은 하늘색 */
    private static final UiButton.Style ROW_STYLE = new UiButton.Style(0xFFFFFFFF, 0xFFF1F4F8, 0xFFC9D1DC, 0xFF1E2430, false,
            UiButton.Shape.SOFT);
    private final int H;
    private net.minecraft.client.gui.components.EditBox nameBox;
    private int page;
    private final List<UiButton> rows = new ArrayList<>();
    private UiButton prev, next;

    public PickScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, (menu.flags & PickMenu.FLAG_CAN_TOGGLE) != 0 ? 236 : 204);
        this.H = this.imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        for (int r = 0; r < ROWS; r++) {
            final int row = r;
            rows.add(addRenderableWidget(new UiButton(leftPos + 10, topPos + 32 + r * 22, W - 20, 20, Component.empty(), ROW_STYLE,
                    () -> pick(row)).left()));
        }
        prev = addRenderableWidget(UiButton.of(leftPos + 10, topPos + H - 24, 20, 17, "◀", UiButton.STEEL, () -> { page--; refresh(); }));
        next = addRenderableWidget(UiButton.of(leftPos + 34, topPos + H - 24, 20, 17, "▶", UiButton.STEEL, () -> { page++; refresh(); }));
        if ((menu.flags & PickMenu.FLAG_CAN_TOGGLE) != 0) {
            boolean pub = (menu.flags & PickMenu.FLAG_PUBLIC) != 0;
            addRenderableWidget(new UiButton(leftPos + W - 110, topPos + H - 24, 100, 17,
                    Component.translatable(pub ? "gui.wildplus.warp.make_private" : "gui.wildplus.warp.make_public"),
                    pub ? UiButton.DARK : UiButton.TEAL, () -> click(PickMenu.BTN_TOGGLE)));
            // 이름 바꾸기 (주인만): 입력칸 + 버튼 → 서버 명령어 /warpname 으로 보낸다
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

    private int pages() {
        return Math.max(1, (menu.entries.size() + ROWS - 1) / ROWS);
    }

    private void refresh() {
        page = Math.max(0, Math.min(page, pages() - 1));
        for (int r = 0; r < ROWS; r++) {
            int idx = page * ROWS + r;
            UiButton b = rows.get(r);
            if (idx < menu.entries.size()) {
                PickMenu.Entry e = menu.entries.get(idx);
                b.visible = true;
                b.setMessage(Component.literal(e.title()).append(Component.literal("   " + e.sub()).withStyle(ChatFormatting.GRAY)));
            } else {
                b.visible = false;
            }
        }
        prev.active = page > 0;
        next.active = page < pages() - 1;
    }

    private void pick(int row) {
        int idx = page * ROWS + row;
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

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
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

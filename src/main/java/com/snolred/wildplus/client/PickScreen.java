package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 목록에서 하나를 고르는 화면 (워프 목적지, 텔레포트 대상). 한 쪽에 7개씩 넘겨 본다. */
public class PickScreen extends AbstractContainerScreen<PickMenu> {
    private static final int W = 240, H = 204, ROWS = 7;
    private int page;
    private final List<Button> rows = new ArrayList<>();
    private Button prev, next;

    public PickScreen(PickMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        for (int r = 0; r < ROWS; r++) {
            final int row = r;
            rows.add(addRenderableWidget(Button.builder(Component.empty(), b -> pick(row))
                    .bounds(leftPos + 8, topPos + 32 + r * 22, W - 16, 20).build()));
        }
        prev = addRenderableWidget(Button.builder(Component.literal("◀"), b -> { page--; refresh(); })
                .bounds(leftPos + 8, topPos + H - 24, 20, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> { page++; refresh(); })
                .bounds(leftPos + 32, topPos + H - 24, 20, 18).build());
        if ((menu.flags & PickMenu.FLAG_CAN_TOGGLE) != 0) {
            boolean pub = (menu.flags & PickMenu.FLAG_PUBLIC) != 0;
            addRenderableWidget(Button.builder(Component.translatable(pub ? "gui.wildplus.warp.make_private" : "gui.wildplus.warp.make_public"),
                    b -> click(PickMenu.BTN_TOGGLE)).bounds(leftPos + W - 108, topPos + H - 24, 100, 18).build());
        }
        refresh();
    }

    private int pages() {
        return Math.max(1, (menu.entries.size() + ROWS - 1) / ROWS);
    }

    private void refresh() {
        page = Math.max(0, Math.min(page, pages() - 1));
        for (int r = 0; r < ROWS; r++) {
            int idx = page * ROWS + r;
            Button b = rows.get(r);
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
        UiKit.panel(g, leftPos, topPos, W, H);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, UiKit.TEXT, false);
        g.text(this.font, Component.literal(menu.header), 8, 18, 0xFF5A3A8A, false);
        if (menu.entries.isEmpty()) {
            g.text(this.font, Component.literal(menu.empty), 8, 40, 0xFF606060, false);
        }
        g.text(this.font, Component.literal((page + 1) + " / " + pages()), 58, H - 19, 0xFF606060, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

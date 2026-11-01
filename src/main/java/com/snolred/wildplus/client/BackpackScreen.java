package com.snolred.wildplus.client;

import com.snolred.wildplus.item.BackpackItem;
import com.snolred.wildplus.menu.BackpackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 배낭 화면 (10열, 다이아는 14열). 등급별 색: 동 · 은 · 금 · 다이아 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final int[][] COLORS = {{0xFF6B3A1E, 0xFFB87333}, {0xFF5A5F66, 0xFFB8BEC6}, {0xFF7A5A10, 0xFFE0B840}, {0xFF1B6E78, 0xFF6FE8F0}};

    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.cols * 18 + 16, menu.rows * 18 + 114);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = (menu.cols * 18 + 16 - 162) / 2;
        this.inventoryLabelY = 18 + menu.rows * 18 + 3;
        if (menu.tier < BackpackItem.MAX_TIER) {
            addRenderableWidget(new UiButton(leftPos + imageWidth - 100, topPos + 4, 94, 12, Component.translatable("gui.wildplus.backpack.upgrade",
                    BackpackItem.tierName(menu.tier + 1), BackpackItem.UPGRADE_COST[menu.tier]), UiButton.GOLD, () -> {
                        if (this.minecraft != null && this.minecraft.gameMode != null) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, BackpackMenu.BTN_UPGRADE);
                        }
                    }));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        int split = 18 + menu.rows * 18 + 2;
        // 가죽 몸체 + 바느질
        UiKit.device(g, x, y, w, h, 0xFF8A5530, 0xFF5E3519);
        UiKit.speckle(g, x + 2, y + 2, w - 4, h - 4, 17, 420, 0x22000000, 0x14FFD8B0);
        UiKit.dashed(g, x + 4, y + 4, w - 8, split - 2, 3, 0xAAE8C890);
        // 덮개 버클 띠
        g.fill(x + w / 2 - 6, y, x + w / 2 + 6, y + 6, 0xFF3E220E);
        // 등급 금속 배지 (동·은·금)
        int[] c = COLORS[menu.tier];
        UiKit.round(g, x + 7, y + 5, 10, 10, 2, c[0]);
        UiKit.round(g, x + 8, y + 6, 8, 8, 2, c[1]);
        g.fill(x + 9, y + 7, x + 12, y + 8, 0x99FFFFFF);
        // 배낭 칸
        for (var s : menu.slots) {
            if (!s.isActive()) continue;
            if (s.y < split) UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.BROWN);
        }
        // 아래: 내 가방 쟁반
        UiKit.inventoryTray(g, x + 6, y + split + 1, w - 12, h - split - 7);
        for (var s : menu.slots) {
            if (s.isActive() && s.y >= split) UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.CLASSIC);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 21, 6, 0xFFFFE8C8, true);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY + 1, 0xFF3A414B, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

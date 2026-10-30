package com.snolred.wildplus.client;

import com.snolred.wildplus.item.BackpackItem;
import com.snolred.wildplus.menu.BackpackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 배낭 화면 (10열). 등급별 색: 동 · 은 · 금 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final int[][] COLORS = {{0xFF6B3A1E, 0xFFB87333}, {0xFF5A5F66, 0xFFB8BEC6}, {0xFF7A5A10, 0xFFE0B840}};

    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 196, menu.rows * 18 + 114);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 17;
        this.inventoryLabelY = 18 + menu.rows * 18 + 3;
        if (menu.tier < 2) {
            addRenderableWidget(Button.builder(Component.translatable("gui.wildplus.backpack.upgrade",
                            BackpackItem.tierName(menu.tier + 1), BackpackItem.UPGRADE_COST[menu.tier]),
                    b -> {
                        if (this.minecraft != null && this.minecraft.gameMode != null) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, BackpackMenu.BTN_UPGRADE);
                        }
                    }).bounds(leftPos + imageWidth - 98, topPos + 3, 92, 12).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, leftPos, topPos, imageWidth, imageHeight);
        // 등급 색 띠 (제목 옆 작은 표시)
        int[] c = COLORS[menu.tier];
        g.fill(leftPos + 4, topPos + 4, leftPos + 6, topPos + 14, c[1]);
        UiKit.slots(g, leftPos, topPos, menu);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 9, 6, UiKit.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

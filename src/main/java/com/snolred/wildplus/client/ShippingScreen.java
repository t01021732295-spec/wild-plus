package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.ShippingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 출하 상자: 나무 상자 판 + 위쪽 송장 띠(오늘 밤 정산 예정 금액) + 주인 전용 공개/비공개 스위치 */
public class ShippingScreen extends AbstractContainerScreen<ShippingMenu> {
    private static final int W = 176, H = 170;

    public ShippingScreen(ShippingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.owner) {
            UiButton b = addRenderableWidget(UiButton.of(leftPos + W - 66, topPos + 4, 60, 13,
                    menu.isPublic ? "🔓 공개" : "🔒 비공개", menu.isPublic ? UiButton.TEAL : UiButton.DARK,
                    () -> {
                        if (this.minecraft != null && this.minecraft.gameMode != null) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ShippingMenu.BTN_TOGGLE);
                        }
                    }));
            b.setMessage(Component.literal(menu.isPublic ? "🔓 공개" : "🔒 비공개"));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 나무 상자
        UiKit.device(g, x, y, W, H, 0xFFB98A54, 0xFF7A5230);
        for (int i = 0; i < 8; i++) UiKit.hline(g, x + 3, y + 20 + i * 8, W - 6, 0x18000000);
        UiKit.outline(g, x + 3, y + 3, W - 6, H - 6, 0x40000000);
        // 송장 띠
        UiKit.round(g, x + 6, y + 3, W - 76, 15, 2, 0xFFF6EFD8);
        // 상자 칸 + 가방 칸
        UiKit.inventoryTray(g, x + 4, y + 82, W - 8, 84);
        for (var s : menu.slots) {
            if (!s.isActive()) continue;
            UiKit.slot(g, x + s.x, y + s.y, s.index < 27 ? UiKit.SlotStyle.BROWN : UiKit.SlotStyle.CLASSIC);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.text(g, "📦 출하 · 정산 예정 " + menu.pendingText(), 10, 7, 0xFF5A3A1A);
        UiKit.text(g, menu.owner ? (menu.isPublic ? "친구도 넣을 수 있어요 · 돈은 내 통장" : "나만 열 수 있어요")
                : "친구의 출하 상자 (공개)", 8, 73, 0xFFFFF0D0);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

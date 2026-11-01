package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.FilterHopperMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 필터 호퍼: 미스릴빛 금속 판. 위 호퍼 5칸, 가운데 노란 테두리 필터 9칸, 흐름 안내 */
public class FilterHopperScreen extends AbstractContainerScreen<FilterHopperMenu> {
    private static final int W = 176, H = 172;

    public FilterHopperScreen(FilterHopperMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        UiKit.device(g, x, y, W, H, 0xFF3D7FB0, 0xFF1C4466);
        UiKit.screws(g, x, y, W, 84);
        // 필터 띠
        UiKit.round(g, x + 5, y + FilterHopperMenu.FILTER_Y - 12, W - 10, 32, 2, 0xFF0F2A40);
        g.fillGradient(x + 6, y + FilterHopperMenu.FILTER_Y - 11, x + W - 6, y + FilterHopperMenu.FILTER_Y + 19, 0xFF173A58, 0xFF10283E);
        UiKit.inventoryTray(g, x + 4, y + 86, W - 8, 82);
        int hopperSlots = 5, filterEnd = 14;
        for (var s : menu.slots) {
            if (!s.isActive()) continue;
            if (s.index < hopperSlots) {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.DARK);
            } else if (s.index < filterEnd) {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.DARK);
                UiKit.outline(g, x + s.x - 1, y + s.y - 1, 18, 18, 0xFFF2C14E);
            } else {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.CLASSIC);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, this.title.getString(), 8, 6, 0xFFEAF6FF);
        UiKit.text(g, "필터: 여기 넣은 아이템만 ◀ 옆 ▶ 으로", 8, FilterHopperMenu.FILTER_Y - 9, 0xFFF2C14E);
        UiKit.text(g, "나머지는 호퍼가 바라보는 쪽(일자)으로 ▼", 8, 74, 0xFFB4E4F8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

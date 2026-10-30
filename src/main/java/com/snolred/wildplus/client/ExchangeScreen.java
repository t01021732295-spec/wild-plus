package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.ExchangeMenu;
import com.snolred.wildplus.registry.ModItems3;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 환전소 화면: 가진 동전 수 + 올리기/내리기 버튼 */
public class ExchangeScreen extends AbstractContainerScreen<ExchangeMenu> {
    private static final int W = 220, H = 176;

    public ExchangeScreen(ExchangeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        String[] labels = {"동화 10 → 은화 1", "은화 10 → 금화 1", "금화 1 → 은화 10", "은화 1 → 동화 10"};
        int[] ids = {ExchangeMenu.C2S, ExchangeMenu.S2G, ExchangeMenu.G2S, ExchangeMenu.S2C};
        for (int r = 0; r < 4; r++) {
            int y = topPos + 54 + r * 20 + (r >= 2 ? 12 : 0);
            int id = ids[r];
            btn(labels[r], id, leftPos + 8, y, 130);
            btn("전부", id + 1, leftPos + 142, y, 70);
        }
        btn("가장 적은 개수로 정리", ExchangeMenu.TIDY, leftPos + 8, topPos + H - 24, 120);
        btn("전부 동화로", ExchangeMenu.ALL_COPPER, leftPos + 132, topPos + H - 24, 80);
    }

    private void btn(String label, int id, int x, int y, int w) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }).bounds(x, y, w, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, leftPos, topPos, W, H);
        UiKit.inset(g, leftPos + 7, topPos + 17, W - 14, 26, 0xFF8B8B8B);
        int x = leftPos + 12, y = topPos + 22;
        g.item(new ItemStack(ModItems3.COPPER_COIN.get()), x, y);
        g.item(new ItemStack(ModItems3.SILVER_COIN.get()), x + 68, y);
        g.item(new ItemStack(ModItems3.GOLD_COIN.get()), x + 136, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, UiKit.TEXT, false);
        g.text(this.font, Component.literal("× " + menu.copper()), 30, 26, 0xFFFFFFFF, true);
        g.text(this.font, Component.literal("× " + menu.silver()), 98, 26, 0xFFFFFFFF, true);
        g.text(this.font, Component.literal("× " + menu.gold()), 166, 26, 0xFFFFFFFF, true);
        g.text(this.font, Component.literal("올리기 ▲"), 8, 45, UiKit.TEXT, false);
        g.text(this.font, Component.literal("내리기 ▼ (잔돈 만들기)"), 8, 96, UiKit.TEXT, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

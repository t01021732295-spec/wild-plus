package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Bounties;
import com.snolred.wildplus.menu.BountyMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 의뢰 게시판 화면: 오늘의 납품 의뢰 3개 */
public class BountyScreen extends AbstractContainerScreen<BountyMenu> {
    private static final int W = 220, H = 124;
    private final List<Button> buttons = new ArrayList<>();

    public BountyScreen(BountyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        for (int i = 0; i < Bounties.COUNT; i++) {
            int id = i;
            buttons.add(addRenderableWidget(Button.builder(Component.literal("납품"), b -> {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                }
            }).bounds(leftPos + W - 50, topPos + 24 + i * 30, 42, 16).build()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            boolean done = menu.done(i);
            b.setMessage(Component.literal(done ? "완료" : "납품"));
            b.active = !done && menu.have(i) >= menu.wanted(i).getCount();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, leftPos, topPos, W, H);
        UiKit.slots(g, leftPos, topPos, menu);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, Component.translatable("container.wildplus.bounty_board.day", menu.day() + 1), 8, 6, UiKit.TEXT, false);
        for (int i = 0; i < Bounties.COUNT; i++) {
            ItemStack want = menu.wanted(i);
            int y = 22 + i * 30;
            g.text(this.font, Component.literal(want.getHoverName().getString() + " × " + want.getCount()), 34, y, UiKit.TEXT, false);
            g.text(this.font, Component.literal(String.format("보상 %,d동", menu.reward(i))), 34, y + 11, 0xFF8A6A00, false);
            int have = menu.have(i);
            g.text(this.font, Component.literal("가진 수 " + Math.min(have, want.getCount()) + "/" + want.getCount()), 110, y + 11,
                    have >= want.getCount() ? 0xFF2A7A2A : 0xFF8A2A2A, false);
        }
        g.text(this.font, Component.literal("의뢰는 매일 아침 바뀌어요"), 8, H - 12, 0xFF707070, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

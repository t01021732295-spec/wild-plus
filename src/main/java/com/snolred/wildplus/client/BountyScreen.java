package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Bounties;
import com.snolred.wildplus.menu.BountyMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 의뢰 게시판: 나무 액자 + 코르크판 + 압정으로 꽂은 의뢰서 3장. 끝낸 의뢰엔 빨간 "완료" 도장. */
public class BountyScreen extends AbstractContainerScreen<BountyMenu> {
    private static final int W = 232, H = 136;
    private static final int[] PIN = {0xFFE0252B, 0xFF2A7AE0, 0xFF1E9A50};
    private final List<UiButton> buttons = new ArrayList<>();

    public BountyScreen(BountyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        for (int i = 0; i < Bounties.COUNT; i++) {
            int id = i;
            buttons.add(addRenderableWidget(UiButton.of(leftPos + W - 58, topPos + 26 + i * 30, 44, 16, "납품", UiButton.GREEN, () -> {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                }
            })));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < buttons.size(); i++) {
            UiButton b = buttons.get(i);
            boolean done = menu.done(i);
            b.visible = !done;
            b.active = !done && menu.have(i) >= menu.wanted(i).getCount();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 나무 액자
        UiKit.device(g, x, y, W, H, 0xFF8A5A2E, 0xFF5A3618);
        UiKit.speckle(g, x + 1, y + 1, W - 2, H - 2, 5, 160, 0x22000000, 0x1AFFE0B0);
        // 코르크
        UiKit.round(g, x + 5, y + 5, W - 10, H - 10, 1, 0xFF6B4520);
        g.fillGradient(x + 6, y + 6, x + W - 6, y + H - 6, 0xFFC99A62, 0xFFB5844E);
        UiKit.speckle(g, x + 6, y + 6, W - 12, H - 12, 21, 700, 0x40704010, 0x30FFF0C8);
        // 의뢰서 3장
        for (int i = 0; i < Bounties.COUNT; i++) {
            int ny = y + 20 + i * 30;
            int tilt = i == 1 ? 1 : 0;
            g.fill(x + 10 + tilt, ny + 2, x + W - 10 + tilt, ny + 28, 0x40000000);
            g.fill(x + 8 + tilt, ny, x + W - 12 + tilt, ny + 26, menu.done(i) ? 0xFFE9E6DC : 0xFFFBF5E3);
            g.fill(x + 8 + tilt, ny, x + W - 12 + tilt, ny + 1, 0xFFFFFFFF);
            // 줄 친 종이
            for (int l = 8; l < 26; l += 7) g.fill(x + 34 + tilt, ny + l + 3, x + W - 64 + tilt, ny + l + 4, 0x18406080);
            // 압정
            UiKit.circle(g, x + 7 + tilt, ny - 3, 6, UiKit.darker(PIN[i], 0.35F));
            UiKit.circle(g, x + 7 + tilt, ny - 4, 6, PIN[i]);
            g.fill(x + 9 + tilt, ny - 3, x + 10 + tilt, ny - 2, 0xCCFFFFFF);
        }
        for (var s : menu.slots) {
            if (s.isActive()) UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.LIGHT);
        }
        // 제목 종이띠
        g.fill(x + 70, y + 4, x + W - 70, y + 16, 0xFFFBF5E3);
        g.fill(x + 70, y + 16, x + W - 70, y + 17, 0x40000000);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.center(g, Component.translatable("container.wildplus.bounty_board.day", menu.day() + 1), W / 2, 6, 0xFF5A3618);
        for (int i = 0; i < Bounties.COUNT; i++) {
            ItemStack want = menu.wanted(i);
            int y = 23 + i * 30;
            boolean done = menu.done(i);
            UiKit.text(g, want.getHoverName().getString() + " × " + want.getCount(), 34, y, done ? 0xFF8A8A8A : 0xFF2A2018);
            UiKit.text(g, String.format("보상 %,d동", menu.reward(i)), 34, y + 11, done ? 0xFF9A9A9A : 0xFF9A6A00);
            int have = menu.have(i);
            if (!done) {
                UiKit.text(g, "가진 수 " + Math.min(have, want.getCount()) + "/" + want.getCount(), 104, y + 11,
                        have >= want.getCount() ? 0xFF1E7A2E : 0xFFA02A2A);
            } else {
                // 빨간 도장
                g.pose().pushMatrix();
                g.pose().translate(W - 40, y + 6);
                g.pose().rotate(-0.2F);
                UiKit.outline(g, -18, -6, 38, 15, 0xCCD0202A);
                UiKit.outline(g, -17, -5, 36, 13, 0x88D0202A);
                UiKit.bold(g, "완료", -10, -3, 0xDDD0202A);
                g.pose().popMatrix();
            }
        }
        UiKit.text(g, "의뢰는 매일 아침 바뀌어요", 10, H - 14 + 2, 0xFF5A3618);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

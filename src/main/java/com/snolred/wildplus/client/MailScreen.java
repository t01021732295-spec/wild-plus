package com.snolred.wildplus.client;

import com.snolred.wildplus.menu.MailMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 우편함: 빨간 우체통 판. 위는 받은 우편(크림색 편지지), 가운데는 보낼 물건 + 받는 사람 고르기 */
public class MailScreen extends AbstractContainerScreen<MailMenu> {
    private static final int W = 176, H = 206;
    private UiButton sendBtn;

    public MailScreen(MailMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        addRenderableWidget(UiButton.of(x + 8, y + 98, 14, 14, "◀", UiButton.WHITE, () -> click(MailMenu.BTN_PREV)));
        addRenderableWidget(UiButton.of(x + 98, y + 98, 14, 14, "▶", UiButton.WHITE, () -> click(MailMenu.BTN_NEXT)));
        sendBtn = addRenderableWidget(UiButton.of(x + 116, y + 97, 52, 16, "📮 보내기", UiButton.GOLD, () -> click(MailMenu.BTN_SEND)));
        sendBtn.active = !menu.names.isEmpty();
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
        UiKit.device(g, x, y, W, H, 0xFFE0473E, 0xFFA8261F);
        // 받은 우편: 편지지
        UiKit.round(g, x + 5, y + 15, W - 10, 50, 2, 0xFF7A1A14);
        g.fillGradient(x + 6, y + 16, x + W - 6, y + 64, 0xFFFFF8E6, 0xFFF2E6C8);
        for (int i = 0; i < 4; i++) UiKit.hline(g, x + 8, y + 22 + i * 11, W - 16, 0x20A07040);
        // 보낼 물건 칸 띠
        UiKit.round(g, x + 5, y + 67, W - 10, 48, 2, 0xFF7A1A14);
        g.fillGradient(x + 6, y + 68, x + W - 6, y + 114, 0xFFC83A32, 0xFF9E241E);
        // 받는 사람 이름표
        UiKit.round(g, x + 24, y + 98, 72, 14, 2, 0xFFFFF8E6);
        UiKit.inventoryTray(g, x + 4, y + MailMenu.INV_Y - 4, W - 8, 84);
        for (var s : menu.slots) {
            if (!s.isActive()) continue;
            UiKit.slot(g, x + s.x, y + s.y, s.index < MailMenu.INBOX ? UiKit.SlotStyle.LIGHT
                    : s.index < MailMenu.INBOX + MailMenu.SEND ? UiKit.SlotStyle.DARK : UiKit.SlotStyle.CLASSIC);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "📬 " + this.title.getString(), 8, 4, 0xFFFFFFFF);
        UiKit.right(g, menu.waiting > 0 ? "더 있음 " + menu.waiting + "묶음 (닫고 다시 열기)" : "받은 우편 (꺼내기만)", W - 8, 4, 0xFFFFE0D0);
        UiKit.text(g, "보낼 물건", 8, 69, 0xFFFFE0D0);
        String to = menu.names.isEmpty() ? "보낼 친구 없음" : menu.names.get(Math.floorMod(menu.target(), menu.names.size()));
        while (UiKit.font().width(to) > 66 && to.length() > 2) to = to.substring(0, to.length() - 1);
        UiKit.center(g, to, 60, 101, 0xFF5A1A14);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

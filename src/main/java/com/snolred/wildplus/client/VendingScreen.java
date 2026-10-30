package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.menu.VendingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 자판기 화면: 왼쪽 진열장(상품 + 가격 버튼), 오른쪽 동전 투입구·잔액·꺼내는 곳 */
public class VendingScreen extends AbstractContainerScreen<VendingMenu> {
    private static final int BODY = 0xFFB8202A, BODY_DARK = 0xFF7A121A, GLASS = 0xFF1E3340, GLASS_LIGHT = 0xFF2E4A5A;
    private static final int PANEL = 0xFF2A2A2E, SLOT_DARK = 0xFF373737, SLOT_LIGHT = 0xFFFFFFFF, SLOT_FILL = 0xFF8B8B8B;
    private static final int INV_BG = 0xFFC6C6C6;

    private final List<Button> buyButtons = new ArrayList<>();
    private final List<Button> priceButtons = new ArrayList<>();
    private Button manageButton;
    private Button collectButton;

    public VendingScreen(VendingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 6;
        this.inventoryLabelY = 130;
        buyButtons.clear();
        priceButtons.clear();
        for (int i = 0; i < 9; i++) {
            int id = VendingMenu.BTN_BUY + i;
            int x = leftPos + 6 + (i % 3) * 34;
            int y = topPos + 39 + (i / 3) * 32;
            buyButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> press(id)).bounds(x, y, 30, 11).build()));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.wildplus.vending.insert"), b -> press(VendingMenu.BTN_INSERT_ALL))
                .bounds(leftPos + 116, topPos + 62, 54, 12).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.wildplus.vending.refund"), b -> press(VendingMenu.BTN_REFUND))
                .bounds(leftPos + 116, topPos + 76, 54, 12).build());
        manageButton = addRenderableWidget(Button.builder(Component.translatable("gui.wildplus.vending.manage"), b -> press(VendingMenu.BTN_MANAGE))
                .bounds(leftPos + 116, topPos + 4, 54, 12).build());
        String[] labels = {"+1", "+10", "+100", "-1", "-10", "-100"};
        for (int i = 0; i < labels.length; i++) {
            int id = VendingMenu.BTN_PRICE + i;
            priceButtons.add(addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> press(id))
                    .bounds(leftPos + 4 + i * 18, topPos + 115, 18, 11).build()));
        }
        collectButton = addRenderableWidget(Button.builder(Component.translatable("gui.wildplus.vending.collect"), b -> press(VendingMenu.BTN_COLLECT))
                .bounds(leftPos + 116, topPos + 114, 56, 12).build());
        updateButtons();
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        boolean owner = menu.isOwner();
        boolean managing = owner && menu.isManaging();
        for (int i = 0; i < buyButtons.size(); i++) {
            Button b = buyButtons.get(i);
            boolean empty = menu.productEmpty(i);
            b.visible = !empty || managing;
            if (managing) {
                b.setMessage(Component.literal((menu.selected() == i ? "▶" : "") + menu.price(i)));
                b.active = true;
            } else {
                b.setMessage(Component.literal(menu.price(i) + "동"));
                b.active = !empty && menu.credit() >= menu.price(i);
            }
        }
        manageButton.visible = owner;
        manageButton.setMessage(Component.translatable(managing ? "gui.wildplus.vending.done" : "gui.wildplus.vending.manage"));
        for (Button b : priceButtons) b.visible = managing;
        collectButton.visible = owner;
        collectButton.setMessage(Component.translatable("gui.wildplus.vending.collect", String.format("%,d", menu.earnings())));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 자판기 몸체
        g.fill(x, y, x + 176, y + 128, BODY_DARK);
        g.fill(x + 2, y + 2, x + 174, y + 126, BODY);
        // 유리 진열장
        g.fill(x + 4, y + 16, x + 110, y + 114, GLASS);
        g.fill(x + 5, y + 17, x + 109, y + 113, GLASS_LIGHT);
        for (int row = 0; row < 3; row++) {
            g.fill(x + 5, y + 51 + row * 32, x + 109, y + 52 + row * 32, 0xFF9AA4AE); // 선반
        }
        // 오른쪽 조작판
        g.fill(x + 114, y + 18, x + 172, y + 116, PANEL);
        // 인벤토리 영역
        g.fill(x, y + 128, x + 176, y + 222, INV_BG);
        g.fill(x, y + 128, x + 176, y + 129, 0xFF555555);
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                drawSlot(g, x + slot.x, y + slot.y, slot.index == VendingMenu.SLOT_COIN || slot.index == VendingMenu.SLOT_TRAY);
            }
        }
    }

    private static void drawSlot(GuiGraphicsExtractor g, int sx, int sy, boolean highlight) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_DARK);
        g.fill(sx, sy, sx + 17, sy + 17, SLOT_LIGHT);
        g.fill(sx, sy, sx + 16, sy + 16, highlight ? 0xFF6E6E2A : SLOT_FILL);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, true);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
        g.text(this.font, Component.translatable("gui.wildplus.vending.coin_slot"), 118, 32, 0xFFFFD84A, false);
        long credit = menu.credit();
        g.text(this.font, Component.translatable("gui.wildplus.vending.credit"), 118, 20, 0xFFAAAAAA, false);
        g.text(this.font, Component.literal(String.format("%,d동", credit)), 146, 20, 0xFF7CFC6A, false);
        g.text(this.font, Component.translatable("gui.wildplus.vending.tray"), 118, 90, 0xFFFFD84A, false);
        if (menu.isOwner()) {
            if (!menu.isManaging()) {
                g.text(this.font, Component.translatable("gui.wildplus.vending.hint_owner"), 6, 118, 0xFFFFE0A0, false);
            }
        } else if (menu.isInfinite()) {
            g.text(this.font, Component.translatable("gui.wildplus.vending.fixed"), 6, 118, 0xFFFFE0A0, false);
        } else {
            g.text(this.font, Component.translatable("gui.wildplus.vending.fixed_owned"), 6, 118, 0xFFFFE0A0, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }

    /** 잔액 표시를 동전 단위로 풀어서 툴팁에 보여줄 때 사용 */
    static Component creditTooltip(long credit) {
        return Coins.describe(credit);
    }
}

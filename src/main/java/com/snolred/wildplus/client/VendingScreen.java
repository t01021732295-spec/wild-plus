package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.menu.VendingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 자판기 화면: 왼쪽 진열장(상품 + 가격 버튼), 오른쪽 동전 투입구·잔액·꺼내는 곳 */
public class VendingScreen extends AbstractContainerScreen<VendingMenu> {
    private static final int BODY = 0xFFB8202A, BODY_DARK = 0xFF7A121A, GLASS = 0xFF1E3340, GLASS_LIGHT = 0xFF2E4A5A;
    private static final int PANEL = 0xFF2A2A2E, SLOT_DARK = 0xFF373737, SLOT_LIGHT = 0xFFFFFFFF, SLOT_FILL = 0xFF8B8B8B;
    private static final int INV_BG = 0xFFC6C6C6;
    /** 진열장 가격표: 검은 바탕에 초록 LED 숫자 */
    private static final UiButton.Style PRICE_TAG = new UiButton.Style(0xFF1C1F24, 0xFF0E1013, 0xFF000000, 0xFF6BFF8A, false, UiButton.Shape.FLAT);

    private final List<UiButton> buyButtons = new ArrayList<>();
    private final List<UiButton> priceButtons = new ArrayList<>();
    private UiButton manageButton;
    private UiButton collectButton;

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
            buyButtons.add(addRenderableWidget(new UiButton(x, y, 30, 11, Component.empty(), PRICE_TAG, () -> press(id))));
        }
        addRenderableWidget(new UiButton(leftPos + 116, topPos + 62, 54, 12, Component.translatable("gui.wildplus.vending.insert"),
                UiButton.GREEN, () -> press(VendingMenu.BTN_INSERT_ALL)));
        addRenderableWidget(new UiButton(leftPos + 116, topPos + 76, 54, 12, Component.translatable("gui.wildplus.vending.refund"),
                UiButton.STEEL, () -> press(VendingMenu.BTN_REFUND)));
        manageButton = addRenderableWidget(new UiButton(leftPos + 116, topPos + 3, 54, 12, Component.translatable("gui.wildplus.vending.manage"),
                UiButton.DARK, () -> press(VendingMenu.BTN_MANAGE)));
        String[] labels = {"+1", "+10", "+100", "-1", "-10", "-100"};
        for (int i = 0; i < labels.length; i++) {
            int id = VendingMenu.BTN_PRICE + i;
            priceButtons.add(addRenderableWidget(UiButton.of(leftPos + 4 + i * 18, topPos + 115, 18, 11, labels[i],
                    i < 3 ? UiButton.GREEN : UiButton.RED, () -> press(id))));
        }
        collectButton = addRenderableWidget(new UiButton(leftPos + 116, topPos + 114, 56, 12, Component.translatable("gui.wildplus.vending.collect"),
                UiButton.GOLD, () -> press(VendingMenu.BTN_COLLECT)));
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
            UiButton b = buyButtons.get(i);
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
        for (UiButton b : priceButtons) b.visible = managing;
        collectButton.visible = owner;
        collectButton.setMessage(Component.translatable("gui.wildplus.vending.collect", String.format("%,d", menu.earnings())));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 빨간 자판기 몸체 (위는 광택)
        UiKit.device(g, x, y, 176, 130, 0xFFD5313B, 0xFF8E1620);
        g.fillGradient(x + 2, y + 2, x + 174, y + 14, 0x40FFFFFF, 0x00FFFFFF);
        // 유리 진열장: 안쪽 어둡게 + 대각선 반사 + 선반 3단
        UiKit.round(g, x + 3, y + 15, 108, 100, 2, 0xFF2A0A0E);
        g.fillGradient(x + 4, y + 16, x + 110, y + 114, 0xFF223A48, 0xFF14232C);
        for (int row = 0; row < 3; row++) {
            int sy = y + 51 + row * 32;
            g.fill(x + 5, sy, x + 109, sy + 1, 0xFFB8C4CE);
            g.fill(x + 5, sy + 1, x + 109, sy + 2, 0xFF5A6670);
            // 나선 코일 느낌의 점
            for (int c = 0; c < 3; c++) for (int k = 0; k < 5; k++) g.fill(x + 10 + c * 34 + k * 5, sy - 2, x + 11 + c * 34 + k * 5, sy, 0x88C8D0D8);
        }
        for (int i = 0; i < 40; i++) g.fill(x + 60 + i, y + 16 + i * 2, x + 62 + i, y + 18 + i * 2, 0x0CFFFFFF);
        // 조작판 (검은 금속)
        UiKit.round(g, x + 113, y + 15, 60, 100, 2, 0xFF0A0B0D);
        g.fillGradient(x + 114, y + 16, x + 172, y + 114, 0xFF33363C, 0xFF1C1E22);
        // 금액 LCD
        UiKit.screen(g, x + 117, y + 18, 52, 10, 0xFF0E2A12, 0xFF0A1E0D);
        // 동전 투입구 · 꺼내는 곳 테두리
        UiKit.round(g, x + 139, y + 39, 22, 22, 2, 0xFFB88A2A);
        UiKit.groove(g, x + 162, y + 44, 3, 12);
        UiKit.round(g, x + 117, y + 88, 52, 28, 2, 0xFF050506);
        g.fill(x + 118, y + 89, x + 168, y + 91, 0xFF2A2D33);
        // 내 가방: 밝은 금속 쟁반
        UiKit.inventoryTray(g, x, y + 126, 176, 96);
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                boolean special = slot.index == VendingMenu.SLOT_COIN || slot.index == VendingMenu.SLOT_TRAY;
                if (slot.index < 9) { // 진열 상품: 유리 안쪽에 옅은 테두리만
                    UiKit.outline(g, x + slot.x - 1, y + slot.y - 1, 18, 18, 0x30FFFFFF);
                    continue;
                }
                if (special) UiKit.slot(g, x + slot.x, y + slot.y, UiKit.SlotStyle.DARK);
                else UiKit.slot(g, x + slot.x, y + slot.y, UiKit.SlotStyle.CLASSIC);
            }
        }
    }

    private static void drawSlot(GuiGraphicsExtractor g, int sx, int sy, boolean highlight) {
        UiKit.inset(g, sx - 1, sy - 1, 18, 18, highlight ? 0xFF9A8A4A : SLOT_FILL);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, true);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF3A414B, false);
        g.text(this.font, Component.translatable("gui.wildplus.vending.coin_slot"), 117, 32, 0xFFFFD84A, false);
        long credit = menu.credit();
        String cr = String.format("%,d동", credit);
        g.text(this.font, Component.literal(cr), 167 - this.font.width(cr), 19, 0xFF6BFF8A, false);
        g.text(this.font, Component.translatable("gui.wildplus.vending.tray"), 119, 90, 0xFFFFD84A, false);
        if (menu.isOwner()) {
            if (!menu.isManaging()) {
                g.text(this.font, Component.translatable("gui.wildplus.vending.hint_owner"), 6, 117, 0xFFFFE0A0, false);
            }
        } else if (menu.isInfinite()) {
            g.text(this.font, Component.translatable("gui.wildplus.vending.fixed"), 6, 117, 0xFFFFE0A0, false);
        } else {
            g.text(this.font, Component.translatable("gui.wildplus.vending.fixed_owned"), 6, 117, 0xFFFFE0A0, false);
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

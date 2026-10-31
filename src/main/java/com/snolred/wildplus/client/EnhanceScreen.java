package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Enhance;
import com.snolred.wildplus.menu.EnhanceMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 강화대: 대장간 느낌. 검은 철판 + 리벳, 왼쪽은 불빛이 비치는 모루 받침(아이템 칸),
 * 오른쪽은 양피지 강화 목록 + 금색 비용 버튼, 아래는 밝은 금속 쟁반 위의 내 가방.
 */
public class EnhanceScreen extends AbstractContainerScreen<EnhanceMenu> {
    private static final int W = 240, H = 206, ROWS = 5;
    private final List<UiButton> rows = new ArrayList<>();
    private UiButton repair;

    public EnhanceScreen(EnhanceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 38;
        this.inventoryLabelY = 110;
        rows.clear();
        for (int i = 0; i < ROWS; i++) {
            int id = i;
            rows.add(addRenderableWidget(UiButton.of(leftPos + W - 56, topPos + 18 + i * 18, 48, 16, "", UiButton.GOLD, () -> click(id))));
        }
        repair = addRenderableWidget(UiButton.of(leftPos + 6, topPos + 66, 52, 16, "", UiButton.STEEL, () -> click(EnhanceMenu.BTN_REPAIR)));
        refresh();
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void refresh() {
        List<Enhance.Option> opts = Enhance.options(menu.item());
        for (int i = 0; i < ROWS; i++) {
            UiButton b = rows.get(i);
            b.visible = i < opts.size();
            if (b.visible) b.setMessage(Component.literal(String.format("%,d동", opts.get(i).cost())));
        }
        long rc = Enhance.repairCost(menu.item());
        repair.visible = rc > 0;
        repair.setMessage(Component.literal(String.format("수리 %,d", rc)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // 검은 철판
        UiKit.device(g, x, y, W, H, 0xFF3A3D44, 0xFF1E2024);
        UiKit.speckle(g, x + 2, y + 2, W - 2, 100, 13, 220, 0x18FFFFFF, 0x22000000);
        UiKit.screws(g, x, y, W, 112);
        // 모루 받침 + 불빛
        g.fillGradient(x + 8, y + 18, x + 56, y + 62, 0x00FF7A1A, 0x66FF5A0A);
        UiKit.round(g, x + 10, y + 58, 44, 6, 1, 0xFF15171A);
        g.fill(x + 16, y + 56, x + 48, y + 58, 0xFF2A2D33);
        // 화로 숯불 점
        for (int i = 0; i < 9; i++) UiKit.led(g, x + 12 + i * 4 + (i % 2), y + 60 - (i % 3), (i % 3 == 0) ? 0xFFFFD84A : 0xFFFF6A1A);
        // 양피지 목록
        UiKit.round(g, x + 62, y + 15, W - 70, ROWS * 18 + 4, 2, 0xFF5A3A1A);
        g.fillGradient(x + 63, y + 16, x + W - 9, y + 15 + ROWS * 18 + 3, 0xFFF3E4C0, 0xFFE2CC98);
        UiKit.speckle(g, x + 63, y + 16, W - 72, ROWS * 18 + 2, 9, 120, 0x22704010, 0x22FFFFFF);
        for (int i = 1; i < ROWS; i++) UiKit.hline(g, x + 66, y + 16 + i * 18, W - 78, 0x305A3A1A);
        // 가방 쟁반
        UiKit.inventoryTray(g, x + 30, y + 106, 178, 96);
        // 칸
        for (var s : menu.slots) {
            if (!s.isActive()) continue;
            boolean anvil = s.index == 0;
            UiKit.slot(g, x + s.x, y + s.y, anvil ? UiKit.SlotStyle.DARK : UiKit.SlotStyle.CLASSIC);
            if (anvil) UiKit.outline(g, x + s.x - 2, y + s.y - 2, 20, 20, 0xAAFF8A2A);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.bold(g, "⚒ " + this.title.getString(), 8, 5, 0xFFFFB86A);
        UiKit.text(g, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF3A414B);
        ItemStack item = menu.item();
        List<Enhance.Option> opts = Enhance.options(item);
        int ink = 0xFF3A2410, soft = 0xFF7A5A30;
        if (item.isEmpty()) {
            UiKit.text(g, "장비를 모루에 올려놓으세요", 68, 22, ink);
            UiKit.text(g, "인챈트를 IX(9)까지", 68, 36, soft);
            UiKit.text(g, "동전으로 강화할 수 있어요", 68, 48, soft);
        } else if (opts.isEmpty()) {
            UiKit.text(g, "강화할 수 있는 인챈트가 없어요", 68, 22, ink);
            UiKit.text(g, "먼저 마법 부여대로", 68, 36, soft);
            UiKit.text(g, "바닐라 최대 레벨까지", 68, 48, soft);
        }
        for (int i = 0; i < Math.min(ROWS, opts.size()); i++) {
            Enhance.Option o = opts.get(i);
            Component from = Enchantment.getFullname(o.enchant(), o.level());
            UiKit.text(g, from.getString(), 68, 22 + i * 18, ink);
            UiKit.bold(g, "→ " + roman(o.level() + 1), 68 + UiKit.font().width(from.getString()) + 4, 22 + i * 18, 0xFFB0400A);
        }
        UiKit.center(g, "모루", 32, 20, 0xFFFFB86A);
        UiKit.text(g, "단계마다", 8, 86, 0xFFB8BEC8);
        UiKit.text(g, "비용 ×3", 8, 96, 0xFFFFB86A);
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

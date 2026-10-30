package com.snolred.wildplus.client;

import com.snolred.wildplus.economy.Enhance;
import com.snolred.wildplus.menu.EnhanceMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** 강화대 화면: 왼쪽 아이템 칸 + 수리, 오른쪽 강화 가능한 인챈트 목록 */
public class EnhanceScreen extends AbstractContainerScreen<EnhanceMenu> {
    private static final int W = 240, H = 202, ROWS = 5;
    private final List<Button> rows = new ArrayList<>();
    private Button repair;

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
            rows.add(addRenderableWidget(Button.builder(Component.empty(), b -> click(id))
                    .bounds(leftPos + W - 54, topPos + 18 + i * 18, 46, 16).build()));
        }
        repair = addRenderableWidget(Button.builder(Component.empty(), b -> click(EnhanceMenu.BTN_REPAIR))
                .bounds(leftPos + 6, topPos + 64, 50, 16).build());
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
            Button b = rows.get(i);
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
        UiKit.panel(g, leftPos, topPos, W, H);
        UiKit.inset(g, leftPos + 62, topPos + 16, W - 70, ROWS * 18 + 4, 0xFF2A1F3A);
        UiKit.slots(g, leftPos, topPos, menu);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, UiKit.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, UiKit.TEXT, false);
        ItemStack item = menu.item();
        List<Enhance.Option> opts = Enhance.options(item);
        if (item.isEmpty()) {
            g.text(this.font, Component.literal("장비를 올려놓으세요"), 66, 22, 0xFFBBBBBB, false);
            g.text(this.font, Component.literal("(바닐라 최대 레벨인"), 66, 34, 0xFF999999, false);
            g.text(this.font, Component.literal(" 인챈트만 강화 가능)"), 66, 46, 0xFF999999, false);
        } else if (opts.isEmpty()) {
            g.text(this.font, Component.literal("강화할 수 있는 인챈트가"), 66, 22, 0xFFBBBBBB, false);
            g.text(this.font, Component.literal("없어요 (먼저 바닐라"), 66, 34, 0xFF999999, false);
            g.text(this.font, Component.literal(" 최대 레벨까지 올리기)"), 66, 46, 0xFF999999, false);
        }
        for (int i = 0; i < Math.min(ROWS, opts.size()); i++) {
            Enhance.Option o = opts.get(i);
            Component from = Enchantment.getFullname(o.enchant(), o.level());
            g.text(this.font, Component.empty().append(from).append(Component.literal(" → " + roman(o.level() + 1))),
                    66, 22 + i * 18, 0xFFE0C8FF, false);
        }
        g.text(this.font, Component.literal("단계마다"), 8, 86, 0xFF606060, false);
        g.text(this.font, Component.literal("비용 ×3"), 8, 96, 0xFF606060, false);
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

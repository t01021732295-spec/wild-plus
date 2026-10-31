package com.snolred.wildplus.client;

import com.snolred.wildplus.block.MachineBlock;
import com.snolred.wildplus.block.OreMinerBlockEntity;
import com.snolred.wildplus.menu.MachineMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 기계 화면: 칸 배치는 메뉴가 정하고, 여기서는 판·안내 문구·(채굴기) 광물 버튼을 그린다 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final List<Button> oreButtons = new ArrayList<>();

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, menu.playerY + 82);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = menu.playerY - 11;
        oreButtons.clear();
        if (menu.kind == MachineBlock.Kind.MINER) {
            List<OreMinerBlockEntity.Ore> ores = OreMinerBlockEntity.ORES;
            for (int i = 0; i < ores.size(); i++) {
                int id = i;
                int col = i % 4, row = i / 4;
                oreButtons.add(addRenderableWidget(Button.builder(Component.literal(ores.get(i).name()), b -> click(id))
                        .bounds(leftPos + 7 + col * 41, topPos + 18 + row * 17, 40, 16).build()));
            }
            addRenderableWidget(Button.builder(Component.literal("멈춤"), b -> click(MachineMenu.BTN_STOP))
                    .bounds(leftPos + 7 + 3 * 41, topPos + 18 + 2 * 17, 40, 16).build());
        }
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int sel = menu.status() - 1;
        for (int i = 0; i < oreButtons.size(); i++) oreButtons.get(i).active = i != sel;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        UiKit.panel(g, leftPos, topPos, imageWidth, imageHeight);
        UiKit.slots(g, leftPos, topPos, menu);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, UiKit.TEXT, false);
        g.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, UiKit.TEXT, false);
        int ok = menu.status();
        switch (menu.kind) {
            case HARVESTER -> {
                g.text(this.font, Component.literal("씨앗 칸"), 8, 40 - 0, 0xFF505050, false);
                g.text(this.font, Component.literal("수확물 (호퍼로 빼기)"), 60, 40, 0xFF505050, false);
                g.text(this.font, Component.literal(ok == 1 ? "● 스프링클러 연결됨" : "● 위에 스프링클러를 놓으세요"), 8, 97,
                        ok == 1 ? 0xFF1E7A2E : 0xFFA02020, false);
            }
            case FISHER -> {
                g.text(this.font, Component.literal("낚싯대"), 8, 26, 0xFF505050, false);
                g.text(this.font, Component.literal(ok == 1 ? "● 물 위" : "● 물 위에 놓으세요"), 8, 62,
                        ok == 1 ? 0xFF1E7A2E : 0xFFA02020, false);
                if (ok == 1 && !menu.getSlot(0).getItem().isEmpty()) {
                    g.text(this.font, Component.literal("다음 " + menu.secondsLeft() + "초"), 8, 74, 0xFF505050, false);
                }
            }
            case MINER -> {
                int sel = ok - 1;
                String text = sel < 0 ? "● 캘 광물을 고르세요 (분당 1~2개)"
                        : "● " + OreMinerBlockEntity.ORES.get(sel).name() + " 채굴 중 · 다음 " + menu.secondsLeft() + "초";
                g.text(this.font, Component.literal(text), 8, 74, sel < 0 ? 0xFFA05A00 : 0xFF1E7A2E, false);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

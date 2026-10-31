package com.snolred.wildplus.client;

import com.snolred.wildplus.block.MachineBlock;
import com.snolred.wildplus.block.OreMinerBlockEntity;
import com.snolred.wildplus.menu.MachineMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 기계 화면: 공장 제어반. 회색 강철판 + 리벳 + 노랑·검정 경고 띠 + 상태 LED.
 * 투입 칸은 초록 테, 배출 칸은 주황 테로 구분.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final List<UiButton> oreButtons = new ArrayList<>();
    private static final int IN = 0xFF39C27A, OUT = 0xFFFF9A2E;

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
                oreButtons.add(addRenderableWidget(UiButton.of(leftPos + 7 + col * 41, topPos + 18 + row * 17, 40, 15, ores.get(i).name(),
                        UiButton.STEEL, () -> click(id))));
            }
            addRenderableWidget(UiButton.of(leftPos + 7 + 3 * 41, topPos + 18 + 2 * 17, 40, 15, "■ 멈춤", UiButton.RED,
                    () -> click(MachineMenu.BTN_STOP)));
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
        for (int i = 0; i < oreButtons.size(); i++) {
            UiButton b = oreButtons.get(i);
            b.setSelected(i == sel);
            b.style(i == sel ? UiButton.GREEN : UiButton.STEEL);
        }
    }

    private boolean running() {
        int ok = menu.status();
        return menu.kind == MachineBlock.Kind.MINER ? ok > 0 : ok == 1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        int top = menu.playerY - 14;
        // 강철판 (위: 기계부)
        UiKit.device(g, x, y, w, h, 0xFF9AA3AE, 0xFF6E7782);
        UiKit.speckle(g, x + 2, y + 2, w - 4, top - 2, 31, 260, 0x14FFFFFF, 0x14000000);
        UiKit.hazard(g, x + 3, y + 3, w - 6, 3);
        // 이름판
        UiKit.round(g, x + 6, y + 6, 104, 11, 1, 0xFF1A1D22);
        UiKit.screws(g, x, y + 2, w, top);
        // 상태 LED
        UiKit.led(g, x + w - 14, y + 9, running() ? 0xFF2BE07A : 0xFFFF4040);
        // 아래: 가방 쟁반
        UiKit.inventoryTray(g, x + 3, y + top, w - 6, h - top - 3);
        // 칸: 투입(초록 테) · 배출(주황 테)
        int machineSlots = menu.slots.size() - 36;
        for (Slot s : menu.slots) {
            if (!s.isActive()) continue;
            boolean machine = s.index < machineSlots;
            if (machine) {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.DARK);
                boolean input = s instanceof MachineMenu.InputSlot;
                g.fill(x + s.x - 1, y + s.y + 16, x + s.x + 17, y + s.y + 17, input ? IN : OUT);
            } else {
                UiKit.slot(g, x + s.x, y + s.y, UiKit.SlotStyle.CLASSIC);
            }
        }
        // 상태 표시 LCD
        int ly = switch (menu.kind) {
            case HARVESTER -> 96;
            case FISHER -> 60;
            case MINER -> 72;
        };
        int lw = menu.kind == MachineBlock.Kind.FISHER ? 48 : w - 16;
        UiKit.screen(g, x + 8, y + ly, lw, menu.kind == MachineBlock.Kind.FISHER ? 22 : 11, 0xFF0E1A10, 0xFF081008);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        UiKit.text(g, this.title, 10, 8, 0xFFF2C14E);
        UiKit.text(g, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF3A414B);
        int ok = menu.status();
        int on = 0xFF6BFF8A, warn = 0xFFFF6A5A;
        switch (menu.kind) {
            case HARVESTER -> {
                UiKit.text(g, "▼ 씨앗 넣는 칸", 8, 40, 0xFF0E4A28);
                UiKit.right(g, "수확물 · 호퍼로 빼기 ▼", 168, 40, 0xFF6A3A00);
                UiKit.text(g, ok == 1 ? "● 스프링클러 연결됨 · 작동 중" : "● 위에 스프링클러를 놓으세요", 11, 98, ok == 1 ? on : warn);
            }
            case FISHER -> {
                UiKit.text(g, "낚싯대", 14, 26, 0xFF1A1D22);
                UiKit.text(g, ok == 1 ? "● 물 위" : "● 물 위에", 11, 62, ok == 1 ? on : warn);
                if (ok != 1) UiKit.text(g, "  놓으세요", 11, 72, warn);
                else if (!menu.getSlot(0).getItem().isEmpty()) UiKit.text(g, "다음 " + menu.secondsLeft() + "초", 11, 72, on);
                else UiKit.text(g, "낚싯대 없음", 11, 72, 0xFFFFC34D);
            }
            case MINER -> {
                int sel = ok - 1;
                String text = sel < 0 ? "● 캘 광물을 고르세요 (분당 1~2개)"
                        : "● " + OreMinerBlockEntity.ORES.get(sel).name() + " 채굴 중 · 다음 " + menu.secondsLeft() + "초";
                UiKit.text(g, text, 11, 74, sel < 0 ? 0xFFFFC34D : on);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.extractTooltip(g, mouseX, mouseY);
    }
}

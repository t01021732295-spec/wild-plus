package com.snolred.wildplus.menu;

import com.snolred.wildplus.block.AutoFisherBlockEntity;
import com.snolred.wildplus.block.AutoHarvesterBlockEntity;
import com.snolred.wildplus.block.MachineBlock;
import com.snolred.wildplus.block.MachineBlockEntity;
import com.snolred.wildplus.block.OreMinerBlockEntity;
import com.snolred.wildplus.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 기계 화면 (자동 수확기 · 광석 채굴기 · 자동 낚시 장치).
 * 데이터 0 = 상태(수확기: 스프링클러 있음 / 낚시: 물 위 / 채굴기: 고른 광물 +1), 1 = 다음까지 남은 초.
 * 채굴기 버튼: 0~10 = 광물 고르기, 99 = 멈춤.
 */
public class MachineMenu extends AbstractContainerMenu {
    public static final int BTN_STOP = 99;

    public final MachineBlock.Kind kind;
    public final int inputSlots;
    public final int playerY;
    private final Container container;
    private final ContainerData data;
    private final @Nullable MachineBlockEntity machine;

    public static int size(MachineBlock.Kind kind) {
        return switch (kind) {
            case HARVESTER -> AutoHarvesterBlockEntity.SEEDS + AutoHarvesterBlockEntity.OUTPUT;
            case MINER -> OreMinerBlockEntity.OUTPUT;
            case FISHER -> 1 + AutoFisherBlockEntity.OUTPUT;
        };
    }

    public static int playerY(MachineBlock.Kind kind) {
        return switch (kind) {
            case HARVESTER -> 116;
            case MINER -> 136;
            case FISHER -> 96;
        };
    }

    /** 클라이언트 */
    public MachineMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, MachineBlock.Kind.values()[buf.readVarInt()], null);
    }

    private MachineMenu(int id, Inventory inv, MachineBlock.Kind kind, @Nullable MachineBlockEntity be) {
        super(ModMenus.MACHINE.get(), id);
        this.kind = kind;
        this.machine = be;
        this.container = be != null ? be : new SimpleContainer(size(kind));
        this.inputSlots = switch (kind) {
            case HARVESTER -> AutoHarvesterBlockEntity.SEEDS;
            case MINER -> 0;
            case FISHER -> 1;
        };
        this.playerY = playerY(kind);
        this.data = be == null ? new SimpleContainerData(2) : new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 1) {
                    return be instanceof OreMinerBlockEntity m ? m.secondsLeft() : be instanceof AutoFisherBlockEntity f ? f.secondsLeft() : 0;
                }
                if (be instanceof AutoHarvesterBlockEntity h) return h.hasSprinkler() ? 1 : 0;
                if (be instanceof AutoFisherBlockEntity f) return f.onWater() ? 1 : 0;
                if (be instanceof OreMinerBlockEntity m) return m.selected() + 1;
                return 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        };
        switch (kind) {
            case HARVESTER -> {
                for (int i = 0; i < 9; i++) addSlot(new InputSlot(container, i, 8 + i * 18, 20));
                for (int r = 0; r < 2; r++) for (int c = 0; c < 9; c++) addSlot(new OutputSlot(container, 9 + c + r * 9, 8 + c * 18, 58 + r * 18));
            }
            case MINER -> {
                for (int r = 0; r < 2; r++) for (int c = 0; c < 9; c++) addSlot(new OutputSlot(container, c + r * 9, 8 + c * 18, 86 + r * 18));
            }
            case FISHER -> {
                addSlot(new InputSlot(container, 0, 20, 38) {
                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
                for (int r = 0; r < 3; r++) for (int c = 0; c < 6; c++) addSlot(new OutputSlot(container, 1 + c + r * 6, 62 + c * 18, 20 + r * 18));
            }
        }
        addStandardInventorySlots(inv, 8, playerY);
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, MachineBlock.Kind kind, MachineBlockEntity be) {
        Component title = Component.translatable(be.getBlockState().getBlock().getDescriptionId());
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new MachineMenu(id, inv, kind, be), title),
                buf -> buf.writeVarInt(kind.ordinal()));
    }

    public int status() { return data.get(0); }
    public int secondsLeft() { return data.get(1); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (machine instanceof OreMinerBlockEntity m) {
            m.select(id == BTN_STOP ? -1 : id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int machineSlots = size(kind);
        if (index < machineSlots) {
            if (!moveItemStackTo(s, machineSlots, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (inputSlots == 0 || !validInput(s) || !moveItemStackTo(s, 0, inputSlots, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    private boolean validInput(ItemStack s) {
        return switch (kind) {
            case HARVESTER -> s.getItem() instanceof net.minecraft.world.item.BlockItem bi
                    && bi.getBlock() instanceof net.minecraft.world.level.block.CropBlock;
            case FISHER -> s.getItem() instanceof FishingRodItem;
            case MINER -> false;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    private class InputSlot extends Slot {
        InputSlot(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return validInput(stack);
        }
    }

    private static class OutputSlot extends Slot {
        OutputSlot(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}

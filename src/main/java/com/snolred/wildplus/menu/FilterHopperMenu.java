package com.snolred.wildplus.menu;

import com.snolred.wildplus.block.FilterHopperBlockEntity;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 필터 호퍼 창: 위 5칸(호퍼), 가운데 9칸(필터 견본, 한 칸에 1개), 아래 내 가방 */
public class FilterHopperMenu extends AbstractContainerMenu {
    public static final int HOPPER_Y = 20, FILTER_Y = 54;
    private final Container hopper, filters;

    public FilterHopperMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(FilterHopperBlockEntity.SIZE), new SimpleContainer(FilterHopperBlockEntity.FILTERS));
    }

    private FilterHopperMenu(int id, Inventory inv, Container hopper, Container filters) {
        super(ModMenus.FILTER_HOPPER.get(), id);
        this.hopper = hopper;
        this.filters = filters;
        for (int i = 0; i < FilterHopperBlockEntity.SIZE; i++) addSlot(new Slot(hopper, i, 44 + i * 18, HOPPER_Y));
        for (int i = 0; i < FilterHopperBlockEntity.FILTERS; i++) {
            addSlot(new Slot(filters, i, 8 + i * 18, FILTER_Y) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addStandardInventorySlots(inv, 8, 90);
    }

    public static void open(ServerPlayer p, FilterHopperBlockEntity be) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new FilterHopperMenu(id, inv, be, be.filters),
                Component.translatable("block.wildplus.filter_hopper")), buf -> {});
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int own = FilterHopperBlockEntity.SIZE + FilterHopperBlockEntity.FILTERS;
        if (index < own) {
            if (!moveItemStackTo(s, own, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, 0, FilterHopperBlockEntity.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return hopper.stillValid(p);
    }
}

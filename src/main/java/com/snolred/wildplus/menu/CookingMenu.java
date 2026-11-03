package com.snolred.wildplus.menu;

import com.snolred.wildplus.block.CookingStationBlockEntity;
import com.snolred.wildplus.registry.ModMenus;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;

/** 조리 기구 화면: 재료 6칸(왼쪽 3 · 오른쪽 3) · 연료 · 완성 + 가방 */
public class CookingMenu extends AbstractContainerMenu {
    public static final int W = 196, H = 222;
    public static final int[][] IN = {{12, 22}, {12, 44}, {12, 66}, {166, 22}, {166, 44}, {166, 66}};
    public static final int FUEL_X = 12, FUEL_Y = 94, OUT_X = 166, OUT_Y = 94, INV_Y = 140;
    private final Container box;
    private final ContainerData data;

    public CookingMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(CookingStationBlockEntity.SIZE), new SimpleContainerData(6));
    }

    private CookingMenu(int id, Inventory inv, Container box, ContainerData data) {
        super(ModMenus.COOKING.get(), id);
        this.box = box;
        this.data = data;
        for (int i = 0; i < 6; i++) addSlot(new Slot(box, i, IN[i][0], IN[i][1]));
        addSlot(new Slot(box, CookingStationBlockEntity.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return CookingStationBlockEntity.fuelTicks(s) > 0;
            }
        });
        addSlot(new Slot(box, CookingStationBlockEntity.OUT, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return false;
            }
        });
        addStandardInventorySlots(inv, 18, INV_Y);
        addDataSlots(data);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        if (!(p.level().getBlockEntity(pos) instanceof CookingStationBlockEntity be)) return;
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CookingMenu(id, inv, be, be.data),
                Component.translatable(be.getBlockState().getBlock().getDescriptionId())), buf -> {});
    }

    public int progress() { return data.get(0); }
    public int total() { return Math.max(1, data.get(1)); }
    public int burn() { return data.get(2); }
    public int burnMax() { return Math.max(1, data.get(3)); }
    public int dish() { return data.get(4); }
    public int station() { return data.get(5); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < 8) {
            if (!moveItemStackTo(s, 8, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (CookingStationBlockEntity.fuelTicks(s) > 0 && !s.is(net.minecraft.world.item.Items.STICK) && moveItemStackTo(s, 6, 7, false)) {
            // 연료 칸으로
        } else if (!moveItemStackTo(s, 0, 6, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return box.stillValid(p);
    }
}

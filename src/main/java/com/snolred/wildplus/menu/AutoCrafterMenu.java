package com.snolred.wildplus.menu;

import com.snolred.wildplus.block.AutoCrafterBlockEntity;
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

/**
 * 자동 제작대 창.
 * 슬롯 순서: 0~8 재료, 9 결과, 10~18 설계도, 19 미리보기, 20~ 내 가방
 */
public class AutoCrafterMenu extends AbstractContainerMenu {
    public static final int PAT_X = 12, PAT_Y = 20, PREVIEW_X = 98, PREVIEW_Y = 42, OUT_X = 146, OUT_Y = 42;
    public static final int BUF_X = 8, BUF_Y = 100, INV_Y = 134;
    public static final int OWN = 20;
    private final Container box, pattern;

    public AutoCrafterMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(AutoCrafterBlockEntity.SIZE), new SimpleContainer(9), new SimpleContainer(1));
    }

    private AutoCrafterMenu(int id, Inventory inv, Container box, Container pattern, Container preview) {
        super(ModMenus.AUTO_CRAFTER.get(), id);
        this.box = box;
        this.pattern = pattern;
        for (int i = 0; i < AutoCrafterBlockEntity.BUFFER; i++) {
            addSlot(new Slot(box, i, BUF_X + i * 18, BUF_Y) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return inPattern(s);
                }
            });
        }
        addSlot(new Slot(box, AutoCrafterBlockEntity.OUT, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return false;
            }
        });
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(pattern, i, PAT_X + (i % 3) * 18, PAT_Y + (i / 3) * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addSlot(new Slot(preview, 0, PREVIEW_X, PREVIEW_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return false;
            }

            @Override
            public boolean mayPickup(Player p) {
                return false;
            }
        });
        addStandardInventorySlots(inv, 8, INV_Y);
    }

    public static void open(ServerPlayer p, AutoCrafterBlockEntity be) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AutoCrafterMenu(id, inv, be, be.pattern, be.preview),
                Component.translatable("block.wildplus.auto_crafter")), buf -> {});
    }

    private boolean inPattern(ItemStack s) {
        for (int i = 0; i < 9; i++) {
            ItemStack p = pattern.getItem(i);
            if (!p.isEmpty() && ItemStack.isSameItem(p, s)) return true;
        }
        return false;
    }

    public boolean hasRecipe() {
        return !this.slots.get(19).getItem().isEmpty();
    }

    public boolean patternEmpty() {
        return pattern.isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || index == 19) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < OWN) {
            if (!moveItemStackTo(s, OWN, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (inPattern(s)) {
            if (!moveItemStackTo(s, 0, AutoCrafterBlockEntity.BUFFER, false)) return ItemStack.EMPTY;
        } else {
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

package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.item.BackpackItem;
import com.snolred.wildplus.registry.ModItems4;
import com.snolred.wildplus.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

/**
 * 배낭 화면: 10열 × 3/5/7줄. 아래는 플레이어 가방(배낭이 들어 있는 칸은 잠김).
 * [업그레이드] 버튼으로 동전을 내고 다음 등급으로 올릴 수 있다(내용물 유지).
 */
public class BackpackMenu extends AbstractContainerMenu {
    public static final int BTN_UPGRADE = 0;

    public final int tier;
    public final int rows;
    public final int bagSlot;
    private final Container bag;
    private final Inventory inventory;
    private final @Nullable ItemStack stack;
    private final int bagSize;

    /** 클라이언트 */
    public BackpackMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readVarInt(), buf.readVarInt(), null);
    }

    /** 서버 (stack = 열린 배낭) */
    public BackpackMenu(int id, Inventory inv, int tier, int bagSlot, @Nullable ItemStack stack) {
        super(ModMenus.BACKPACK.get(), id);
        this.tier = tier;
        this.rows = BackpackItem.ROWS[tier];
        this.bagSlot = bagSlot;
        this.inventory = inv;
        this.stack = stack;
        this.bagSize = rows * BackpackItem.COLUMNS;
        SimpleContainer container = new SimpleContainer(bagSize) {
            @Override
            public void setChanged() {
                super.setChanged();
                save(this);
            }
        };
        if (stack != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(bagSize, ItemStack.EMPTY);
            stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < bagSize; i++) container.setItem(i, items.get(i));
        }
        this.bag = container;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < BackpackItem.COLUMNS; c++) {
                addSlot(new Slot(bag, c + r * BackpackItem.COLUMNS, 8 + c * 18, 18 + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack s) {
                        return !(s.getItem() instanceof BackpackItem);
                    }
                });
            }
        }
        int invX = 17, invY = 18 + rows * 18 + 14;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(playerSlot(inv, c + r * 9 + 9, invX + c * 18, invY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(playerSlot(inv, c, invX + c * 18, invY + 58));
        }
    }

    private Slot playerSlot(Inventory inv, int index, int x, int y) {
        if (index != bagSlot) return new Slot(inv, index, x, y);
        return new Slot(inv, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean mayPlace(ItemStack s) {
                return false;
            }
        };
    }

    private void save(Container c) {
        if (stack == null) return;
        NonNullList<ItemStack> list = NonNullList.withSize(bagSize, ItemStack.EMPTY);
        for (int i = 0; i < bagSize; i++) list.set(i, c.getItem(i).copy());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BTN_UPGRADE || tier >= 2 || !(player instanceof ServerPlayer sp) || stack == null) return false;
        long cost = BackpackItem.UPGRADE_COST[tier];
        if (!Coins.pay(sp, cost)) {
            sp.sendSystemMessage(Component.translatable("message.wildplus.backpack.no_money", cost).withStyle(ChatFormatting.RED));
            return true;
        }
        save(bag);
        Item next = tier == 0 ? ModItems4.SILVER_BACKPACK.get() : ModItems4.GOLD_BACKPACK.get();
        ItemStack upgraded = stack.transmuteCopy(next, 1);
        sp.getInventory().setItem(bagSlot, upgraded);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
        sp.sendSystemMessage(Component.translatable("message.wildplus.backpack.upgraded", BackpackItem.tierName(tier + 1), cost)
                .withStyle(ChatFormatting.GOLD));
        sp.closeContainer();
        BackpackItem.open(sp, bagSlot);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < bagSize) {
            if (!moveItemStackTo(s, bagSize, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (s.getItem() instanceof BackpackItem || !moveItemStackTo(s, 0, bagSize, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stack == null || inventory.getItem(bagSlot) == stack;
    }
}

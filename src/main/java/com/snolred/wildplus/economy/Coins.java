package com.snolred.wildplus.economy;

import com.snolred.wildplus.registry.ModItems3;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.NonNullList;

/**
 * 화폐 시스템.
 * 동화 1 = 1동, 은화 1 = 10동, 금화 1 = 100동, 다이아화 1 = 1,000동.
 * 동전 주머니에는 동전을 "금액"으로 넣어 두고, 필요할 때 가장 적은 개수의 동전으로 꺼낼 수 있다(= 환전).
 */
public final class Coins {
    public static final int COPPER = 1;
    public static final int SILVER = 10;
    public static final int GOLD = 100;
    public static final int DIAMOND = 1000;
    public static final int EMERALD = 10000;
    public static final String POUCH_KEY = "wildplus_coins";

    private Coins() {}

    /** 동전 한 개의 가치 (동전이 아니면 0) */
    public static int unitValue(ItemStack stack) {
        if (stack.is(ModItems3.COPPER_COIN.get())) return COPPER;
        if (stack.is(ModItems3.SILVER_COIN.get())) return SILVER;
        if (stack.is(ModItems3.GOLD_COIN.get())) return GOLD;
        if (stack.is(ModItems3.DIAMOND_COIN.get())) return DIAMOND;
        if (stack.is(com.snolred.wildplus.registry.ModItems13.EMERALD_COIN.get())) return EMERALD;
        return 0;
    }

    public static boolean isPouch(ItemStack stack) {
        return stack.is(ModItems3.COIN_POUCH.get());
    }

    public static long pouchValue(ItemStack pouch) {
        return pouch.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(POUCH_KEY, 0L);
    }

    public static void setPouchValue(ItemStack pouch, long value) {
        CustomData.update(DataComponents.CUSTOM_DATA, pouch, tag -> tag.putLong(POUCH_KEY, Math.max(0L, value)));
    }

    /** 배낭 안의 동전 + 주머니 금액 */
    public static long backpackValue(ItemStack bag) {
        if (!(bag.getItem() instanceof com.snolred.wildplus.item.BackpackItem)) return 0;
        long total = 0;
        NonNullList<ItemStack> items = NonNullList.withSize(((com.snolred.wildplus.item.BackpackItem) bag.getItem()).size(), ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        for (ItemStack s : items) {
            total += (long) unitValue(s) * s.getCount();
            if (isPouch(s)) total += pouchValue(s);
        }
        return total;
    }

    /** 배낭에서 동전을 모두 빼고 주머니를 비운다 (돌려준 금액만큼) */
    private static long emptyBackpack(ItemStack bag) {
        if (!(bag.getItem() instanceof com.snolred.wildplus.item.BackpackItem b)) return 0;
        long taken = 0;
        NonNullList<ItemStack> items = NonNullList.withSize(b.size(), ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (unitValue(s) > 0) {
                taken += (long) unitValue(s) * s.getCount();
                items.set(i, ItemStack.EMPTY);
            } else if (isPouch(s)) {
                taken += pouchValue(s);
                setPouchValue(s, 0);
            }
        }
        if (taken > 0) bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return taken;
    }

    /** 인벤토리의 동전 + 주머니 금액 + 배낭 속 돈 합계 */
    public static long totalValue(Player player) {
        Inventory inv = player.getInventory();
        long total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            total += (long) unitValue(s) * s.getCount();
            if (isPouch(s)) total += pouchValue(s);
            total += backpackValue(s);
        }
        return total;
    }

    /** 배낭을 빼고 몸에 지닌 돈만 */
    public static long carriedValue(Player player) {
        Inventory inv = player.getInventory();
        long total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            total += (long) unitValue(s) * s.getCount();
            if (isPouch(s)) total += pouchValue(s);
        }
        return total;
    }

    /**
     * 값을 치른다. 부족하면 아무것도 바꾸지 않고 false.
     * 동전을 모두 거둔 뒤 거스름돈을 첫 번째 주머니(없으면 동전)로 돌려준다.
     */
    public static boolean pay(Player player, long price) {
        if (price <= 0) return true;
        long carried = carriedValue(player);
        long total = totalValue(player);
        if (total < price) return false;
        Inventory inv = player.getInventory();
        ItemStack firstPouch = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (unitValue(s) > 0) {
                inv.setItem(i, ItemStack.EMPTY);
            } else if (isPouch(s)) {
                setPouchValue(s, 0);
                if (firstPouch.isEmpty()) firstPouch = s;
            }
        }
        // 몸에 지닌 돈이 모자랄 때만 배낭 속 돈까지 꺼내 쓴다
        long pool = carried;
        if (carried < price) {
            for (int i = 0; i < inv.getContainerSize() && pool < price; i++) pool += emptyBackpack(inv.getItem(i));
        }
        long change = pool - price;
        if (!firstPouch.isEmpty()) {
            setPouchValue(firstPouch, change);
        } else {
            give(player, change);
        }
        inv.setChanged();
        return true;
    }

    /** 돈을 돌려준다: 동전 주머니가 있으면 주머니에, 없으면 동전으로 */
    public static void refund(Player player, long amount) {
        if (amount <= 0) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isPouch(s)) {
                setPouchValue(s, pouchValue(s) + amount);
                return;
            }
        }
        give(player, amount);
    }

    /** 금액을 가장 적은 개수의 동전으로 */
    public static List<ItemStack> toCoins(long amount) {
        List<ItemStack> out = new ArrayList<>();
        long emerald = amount / EMERALD;
        long diamond = (amount % EMERALD) / DIAMOND;
        long gold = (amount % DIAMOND) / GOLD;
        long silver = (amount % GOLD) / SILVER;
        long copper = amount % SILVER;
        addStacks(out, new ItemStack(com.snolred.wildplus.registry.ModItems13.EMERALD_COIN.get()), emerald);
        addStacks(out, new ItemStack(ModItems3.DIAMOND_COIN.get()), diamond);
        addStacks(out, new ItemStack(ModItems3.GOLD_COIN.get()), gold);
        addStacks(out, new ItemStack(ModItems3.SILVER_COIN.get()), silver);
        addStacks(out, new ItemStack(ModItems3.COPPER_COIN.get()), copper);
        return out;
    }

    private static void addStacks(List<ItemStack> out, ItemStack proto, long count) {
        while (count > 0) {
            int n = (int) Math.min(count, proto.getMaxStackSize());
            out.add(proto.copyWithCount(n));
            count -= n;
        }
    }

    /** 플레이어에게 동전을 준다 (가방이 꽉 차면 바닥에) */
    public static void give(Player player, long amount) {
        for (ItemStack s : toCoins(amount)) {
            if (!player.getInventory().add(s) && !s.isEmpty()) {
                ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), s);
                drop.setNoPickUpDelay();
                player.level().addFreshEntity(drop);
            }
        }
    }

    /** "1,234동 (금화 12 · 은화 3 · 동화 4)" */
    public static Component describe(long amount) {
        return Component.translatable("wildplus.coins.amount", String.format("%,d", amount),
                amount / DIAMOND, (amount % DIAMOND) / GOLD, (amount % GOLD) / SILVER, amount % SILVER);
    }
}

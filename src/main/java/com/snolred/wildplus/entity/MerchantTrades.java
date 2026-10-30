package com.snolred.wildplus.entity;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems2;
import com.snolred.wildplus.registry.ModItems3;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;

/**
 * 상인 거래 목록. 가격 단위는 "동" (동화 1 = 1동, 은화 = 10동, 금화 = 100동).
 * 판매(상인이 파는 것)와 매입(상인이 사 주는 것)이 섞여 있다.
 */
public final class MerchantTrades {
    private MerchantTrades() {}

    public static List<MerchantOffer> build(MerchantEntity.Kind kind) {
        List<MerchantOffer> o = new ArrayList<>();
        if (kind == MerchantEntity.Kind.GENERAL) {
            // ── 매입: 농산물·물고기를 동전으로 ──
            buy(o, Items.WHEAT, 16, 3);
            buy(o, Items.CARROT, 16, 3);
            buy(o, Items.POTATO, 16, 3);
            buy(o, ModItems2.TOMATO.get(), 8, 4);
            buy(o, ModItems2.STRAWBERRY.get(), 8, 4);
            buy(o, ModItems.CHILI.get(), 8, 4);
            buy(o, ModItems2.RAINBOW_TROUT.get(), 4, 6);
            buy(o, ModItems2.ICE_SMELT.get(), 4, 6);
            buy(o, ModItems2.MOONLIGHT_FISH.get(), 2, 10);
            buy(o, ModItems2.STORM_EEL.get(), 2, 10);
            buy(o, ModItems2.ANGLERFISH.get(), 1, 10);
            buy(o, ModItems2.GOLDEN_CARP.get(), 1, 100);
            buy(o, Items.EMERALD, 1, 10);
            // ── 판매 ──
            sell(o, Items.BREAD, 3, 2);
            sell(o, ModItems2.TOMATO_SEEDS.get(), 4, 3);
            sell(o, ModItems2.STRAWBERRY.get(), 3, 3);
            sell(o, ModItems.CHILI_SEEDS.get(), 4, 3);
            sell(o, ModItems2.FERTILIZER.get(), 4, 10);
            sell(o, ModItems.TRAVELER_RATION.get(), 2, 10);
            sell(o, Items.EMERALD, 1, 12);
            sell(o, Items.NAME_TAG, 1, 40);
            sell(o, Items.SADDLE, 1, 50);
            sell(o, ModItems.RECALL_SCROLL.get(), 1, 35);
            sell(o, ModItems2.SPRINKLER.get(), 1, 60);
            sell(o, ModItems2.JADE_FISHING_ROD.get(), 1, 100);
            sell(o, ModItems3.COIN_POUCH.get(), 1, 15);
            sell(o, ModItems3.VENDING_MACHINE.get(), 1, 80);
            sell(o, ModItems3.GACHA_MACHINE.get(), 1, 120);
            sell(o, ModItems3.MAGNET_CHARM.get(), 1, 150);
            sell(o, ModItems3.PORTABLE_CRAFTING_TABLE.get(), 1, 30);
            sell(o, com.snolred.wildplus.registry.ModItems4.TELEPORTER.get(), 1, 40);
            sell(o, com.snolred.wildplus.registry.ModItems4.LOTTERY_TICKET.get(), 1, 30, 16);
            sell(o, com.snolred.wildplus.registry.ModItems4.COPPER_BACKPACK.get(), 1, 60, 4);
            sell(o, com.snolred.wildplus.registry.ModItems4.SHIPPING_BIN.get(), 1, 20, 4);
            sell(o, com.snolred.wildplus.registry.ModItems4.EXCHANGE.get(), 1, 50, 4);
            sellStack(o, com.snolred.wildplus.economy.Cookbook.create(), 5);
            sell(o, ModItems.LUCKY_CHARM.get(), 1, 500, 1);
        } else {
            // ── 매입: 광물 ──
            buy(o, Items.COAL, 16, 3);
            buy(o, Items.IRON_INGOT, 4, 5);
            buy(o, Items.GOLD_INGOT, 2, 6);
            buy(o, Items.REDSTONE, 16, 3);
            buy(o, Items.LAPIS_LAZULI, 8, 4);
            buy(o, ModItems2.RAW_MYTHRIL.get(), 2, 10);
            buy(o, ModItems2.JADE.get(), 2, 10);
            buy(o, ModItems.BLOOD_SHARD.get(), 4, 10);
            buy(o, Items.DIAMOND, 1, 30);
            buy(o, Items.EMERALD, 1, 10);
            // ── 판매 ──
            sell(o, Items.IRON_INGOT, 4, 10);
            sell(o, ModItems2.MYTHRIL_INGOT.get(), 1, 40);
            sell(o, ModItems2.JADE.get(), 1, 20);
            sell(o, ModItems.BLOOD_SHARD.get(), 2, 30);
            sell(o, Items.DIAMOND, 1, 100);
            sell(o, ModItems2.MYTHRIL_PICKAXE.get(), 1, 150);
            sell(o, ModItems2.MYTHRIL_SWORD.get(), 1, 120);
            sell(o, ModItems.BLOODSTEEL_SWORD.get(), 1, 150);
            sell(o, ModItems.BLOODSTEEL_CHESTPLATE.get(), 1, 200);
            sell(o, Items.ANVIL, 1, 40);
            sell(o, Items.EXPERIENCE_BOTTLE, 4, 30);
        }
        return o;
    }

    /** 상인이 물건을 사 준다: item n개 → payout동 (단일 동전 종류로 지급) */
    private static void buy(List<MerchantOffer> out, ItemLike item, int count, int payout) {
        ItemStack coins = Coins.toCoins(payout).get(0);
        out.add(new MerchantOffer(new ItemCost(item, count), coins, 16, 1, 0.0F));
    }

    private static void sell(List<MerchantOffer> out, ItemLike item, int count, int price) {
        sell(out, item, count, price, 12);
    }

    /** 상인이 물건을 판다: price동 → item n개 (동전 최대 두 종류로 지불) */
    private static void sell(List<MerchantOffer> out, ItemLike item, int count, int price, int maxUses) {
        List<ItemStack> coins = Coins.toCoins(price);
        ItemCost a = cost(coins.get(0));
        Optional<ItemCost> b = coins.size() > 1 ? Optional.of(cost(coins.get(1))) : Optional.empty();
        out.add(new MerchantOffer(a, b, new ItemStack(item, count), maxUses, 2, 0.0F));
    }

    private static void sellStack(List<MerchantOffer> out, ItemStack result, int price) {
        List<ItemStack> coins = Coins.toCoins(price);
        out.add(new MerchantOffer(cost(coins.get(0)), Optional.empty(), result, 4, 1, 0.0F));
    }

    private static ItemCost cost(ItemStack stack) {
        Item item = stack.getItem();
        return new ItemCost(item, stack.getCount());
    }
}

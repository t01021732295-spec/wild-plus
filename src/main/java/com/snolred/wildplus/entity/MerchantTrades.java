package com.snolred.wildplus.entity;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems2;
import com.snolred.wildplus.registry.ModItems3;
import com.snolred.wildplus.registry.ModItems4;
import com.snolred.wildplus.registry.ModItems5;
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
        switch (kind) {
            case GENERAL -> general(o);
            case BLACKSMITH -> blacksmith(o);
            case FARMER -> farmer(o);
            case FISHERMAN -> fisherman(o);
            case BANKER -> banker(o);
        }
        return o;
    }

    /** 잡화 상인: 여행·생활 용품, 몹 전리품 매입 */
    private static void general(List<MerchantOffer> o) {
        buy(o, Items.BONE, 16, 3);
        buy(o, Items.STRING, 16, 3);
        buy(o, Items.GUNPOWDER, 8, 4);
        buy(o, Items.ROTTEN_FLESH, 32, 2);
        buy(o, Items.SPIDER_EYE, 8, 3);
        buy(o, Items.LEATHER, 8, 5);
        buy(o, Items.ENDER_PEARL, 2, 10);
        buy(o, Items.EMERALD, 1, 10);
        sell(o, Items.BREAD, 3, 2);
        sell(o, Items.TORCH, 16, 2);
        sell(o, ModItems.TRAVELER_RATION.get(), 2, 10);
        sell(o, ModItems.RECALL_SCROLL.get(), 1, 35);
        sell(o, com.snolred.wildplus.registry.ModItems6.DEATH_SCROLL.get(), 1, 60, 8);
        sell(o, Items.NAME_TAG, 1, 40);
        sell(o, Items.SADDLE, 1, 50);
        sell(o, ModItems3.COIN_POUCH.get(), 1, 15);
        sell(o, ModItems3.PORTABLE_CRAFTING_TABLE.get(), 1, 30);
        sell(o, ModItems3.PORTABLE_ENDER_CHEST.get(), 1, 120, 4);
        sell(o, ModItems3.MAGNET_CHARM.get(), 1, 150);
        sell(o, ModItems4.COPPER_BACKPACK.get(), 1, 60, 4);
        sell(o, ModItems4.TELEPORTER.get(), 1, 40);
        sell(o, ModItems4.WARP_POINT.get(), 1, 150, 4);
        sell(o, ModItems4.LOTTERY_TICKET.get(), 1, 30, 16);
        sellStack(o, com.snolred.wildplus.economy.Cookbook.create(), 5);
        sellStack(o, com.snolred.wildplus.economy.Guidebook.create(), 5);
        sell(o, ModItems.LUCKY_CHARM.get(), 1, 500, 1);
        // 이벤트 투척병: 무작위 + 그날 고른 지정 이벤트 2종
        sellStack(o, com.snolred.wildplus.item.EventBottleItem.of(null), 800, 4);
        var shop = com.snolred.wildplus.event.RandomEvents.BOTTLE_SHOP;
        int a = RND.nextInt(shop.length), b = (a + 1 + RND.nextInt(shop.length - 1)) % shop.length;
        sellStack(o, com.snolred.wildplus.item.EventBottleItem.of(shop[a]), 1500, 2);
        sellStack(o, com.snolred.wildplus.item.EventBottleItem.of(shop[b]), 1500, 2);
    }

    private static final java.util.Random RND = new java.util.Random();

    /** 대장장이: 광물 매입, 금속·장비·채굴 도구 */
    private static void blacksmith(List<MerchantOffer> o) {
        buy(o, Items.COAL, 16, 3);
        buy(o, Items.IRON_INGOT, 4, 5);
        buy(o, Items.GOLD_INGOT, 2, 6);
        buy(o, Items.REDSTONE, 16, 3);
        buy(o, Items.LAPIS_LAZULI, 8, 4);
        buy(o, ModItems2.RAW_MYTHRIL.get(), 2, 10);
        buy(o, ModItems2.JADE.get(), 2, 10);
        buy(o, ModItems.BLOOD_SHARD.get(), 4, 10);
        buy(o, Items.DIAMOND, 1, 30);
        sell(o, Items.IRON_INGOT, 4, 10);
        sell(o, ModItems2.MYTHRIL_INGOT.get(), 1, 40);
        sell(o, ModItems2.JADE.get(), 1, 20);
        sell(o, ModItems.BLOOD_SHARD.get(), 2, 30);
        sell(o, Items.DIAMOND, 1, 100);
        buy(o, com.snolred.wildplus.registry.ModItems7.RUBY.get(), 1, 40);
        sell(o, com.snolred.wildplus.registry.ModItems7.RUBY.get(), 1, 160, 6);
        sell(o, ModItems2.MYTHRIL_PICKAXE.get(), 1, 150);
        sell(o, ModItems2.MYTHRIL_SWORD.get(), 1, 120);
        sell(o, ModItems4.JADE_PICKAXE.get(), 1, 120);
        sell(o, ModItems.BLOODSTEEL_SWORD.get(), 1, 150);
        sell(o, ModItems.BLOODSTEEL_CHESTPLATE.get(), 1, 200);
        sell(o, ModItems4.MYTHRIL_HELMET.get(), 1, 160);
        sell(o, ModItems5.DRILL.get(), 1, 600, 2);
        sell(o, ModItems5.ORE_MINER.get(), 1, 1500, 1);
        sell(o, Items.ANVIL, 1, 40);
        sell(o, ModItems4.ENHANCE_TABLE.get(), 1, 250, 2);
        sell(o, Items.EXPERIENCE_BOTTLE, 4, 30);
    }

    /** 농부: 씨앗·비료·농사 기계, 농산물 매입 */
    private static void farmer(List<MerchantOffer> o) {
        buy(o, Items.WHEAT, 16, 3);
        buy(o, Items.CARROT, 16, 3);
        buy(o, Items.POTATO, 16, 3);
        buy(o, Items.PUMPKIN, 4, 3);
        buy(o, Items.MELON_SLICE, 16, 3);
        buy(o, ModItems2.TOMATO.get(), 8, 4);
        buy(o, ModItems2.STRAWBERRY.get(), 8, 4);
        buy(o, ModItems.CHILI.get(), 8, 4);
        buy(o, ModItems5.CORN.get(), 8, 4);
        buy(o, ModItems5.CABBAGE.get(), 4, 4);
        buy(o, ModItems5.BLUEBERRY.get(), 8, 4);
        sell(o, Items.WHEAT_SEEDS, 8, 1);
        sell(o, ModItems2.TOMATO_SEEDS.get(), 4, 3);
        sell(o, ModItems.CHILI_SEEDS.get(), 4, 3);
        sell(o, ModItems2.STRAWBERRY.get(), 3, 3);
        sell(o, ModItems5.CORN_SEEDS.get(), 4, 3);
        sell(o, ModItems5.CABBAGE_SEEDS.get(), 4, 3);
        sell(o, ModItems5.BLUEBERRY.get(), 3, 3);
        sell(o, ModItems2.FERTILIZER.get(), 4, 10);
        sell(o, Items.BONE_MEAL, 8, 4);
        sell(o, ModItems2.SPRINKLER.get(), 1, 60);
        sell(o, ModItems5.AUTO_HARVESTER.get(), 1, 400, 2);
        sell(o, ModItems5.SALAD.get(), 1, 8);
        sell(o, ModItems5.BLUEBERRY_MUFFIN.get(), 2, 10);
        sell(o, ModItems2.PIZZA.get(), 2, 8);
        sell(o, ModItems.SPICY_STEW.get(), 1, 12);
    }

    /** 어부: 낚시 도구·자동 낚시, 물고기 매입 */
    private static void fisherman(List<MerchantOffer> o) {
        buy(o, Items.COD, 8, 3);
        buy(o, Items.SALMON, 8, 4);
        buy(o, ModItems2.RAINBOW_TROUT.get(), 4, 6);
        buy(o, ModItems2.ICE_SMELT.get(), 4, 6);
        buy(o, ModItems5.CATFISH.get(), 4, 6);
        buy(o, ModItems5.SWEETFISH.get(), 4, 6);
        buy(o, ModItems5.TUNA.get(), 2, 10);
        buy(o, ModItems2.MOONLIGHT_FISH.get(), 2, 10);
        buy(o, ModItems2.STORM_EEL.get(), 2, 10);
        buy(o, ModItems2.ANGLERFISH.get(), 1, 10);
        buy(o, ModItems2.GOLDEN_CARP.get(), 1, 100);
        buy(o, ModItems5.DRAGON_KING_FISH.get(), 1, 500);
        sell(o, Items.FISHING_ROD, 1, 8);
        sell(o, ModItems2.JADE_FISHING_ROD.get(), 1, 100);
        sell(o, ModItems5.AUTO_FISHER.get(), 1, 300, 2);
        sell(o, Items.WATER_BUCKET, 1, 5);
        sell(o, ModItems5.TUNA_STEAK.get(), 2, 8);
        sell(o, ModItems5.GRILLED_SWEETFISH.get(), 2, 8);
        sell(o, ModItems2.SASHIMI_PLATTER.get(), 1, 15);
        sell(o, ModItems2.FISH_STEW.get(), 1, 15);
        sell(o, ModItems2.SUNKEN_CHEST.get(), 1, 80, 2);
        sell(o, Items.NAUTILUS_SHELL, 1, 40, 2);
    }

    /** 경제 상인: 경제·편의 시설 블록 */
    private static void banker(List<MerchantOffer> o) {
        buy(o, Items.GOLD_INGOT, 1, 3);
        buy(o, Items.EMERALD, 1, 10);
        buy(o, com.snolred.wildplus.registry.ModItems6.LORD_HEART.get(), 1, 2000);
        sell(o, ModItems3.VENDING_MACHINE.get(), 1, 80);
        sell(o, ModItems3.GACHA_MACHINE.get(), 1, 120);
        sell(o, ModItems4.SHIPPING_BIN.get(), 1, 20, 4);
        sell(o, ModItems4.EXCHANGE.get(), 1, 50, 4);
        sell(o, ModItems4.ATM.get(), 1, 200, 2);
        sell(o, ModItems4.STOCK_TERMINAL.get(), 1, 200, 2);
        sell(o, ModItems4.BOUNTY_BOARD.get(), 1, 30, 4);
        sell(o, ModItems4.CHUNK_LOADER.get(), 1, 100, 8);
        sell(o, ModItems4.PHARMACY.get(), 1, 150, 2);
        sell(o, ModItems4.WEATHER_STATION.get(), 1, 300, 2);
        sell(o, ModItems5.CASINO_TABLE.get(), 1, 500, 2);
        sell(o, com.snolred.wildplus.registry.ModItems6.HALL_OF_FAME.get(), 1, 1000, 1);
        sell(o, ModItems4.SILVER_BACKPACK.get(), 1, 400, 2);
        sell(o, ModItems4.GOLD_BACKPACK.get(), 1, 1200, 1);
        sell(o, ModItems4.LOTTERY_TICKET.get(), 1, 30, 16);
        sellStack(o, com.snolred.wildplus.item.EventBottleItem.of(null), 800, 4);
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

    private static void sellStack(List<MerchantOffer> out, ItemStack result, int price, int maxUses) {
        List<ItemStack> coins = Coins.toCoins(price);
        Optional<ItemCost> b = coins.size() > 1 ? Optional.of(cost(coins.get(1))) : Optional.empty();
        out.add(new MerchantOffer(cost(coins.get(0)), b, result, maxUses, 2, 0.0F));
    }

    private static ItemCost cost(ItemStack stack) {
        Item item = stack.getItem();
        return new ItemCost(item, stack.getCount());
    }
}

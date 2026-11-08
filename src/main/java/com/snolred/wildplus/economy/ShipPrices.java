package com.snolred.wildplus.economy;

import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems2;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** 출하 상자 판매 가격표 (동 × 100). 표에 없는 물건은 팔리지 않고 상자에 남는다. */
public final class ShipPrices {
    private static Map<Item, Integer> table;

    private ShipPrices() {}

    private static void put(Map<Item, Integer> m, ItemLike item, int centi) {
        m.put(item.asItem(), centi);
    }

    private static Map<Item, Integer> table() {
        if (table != null) return table;
        Map<Item, Integer> m = new HashMap<>();
        // 농사
        put(m, Items.WHEAT, 40); put(m, Items.CARROT, 40); put(m, Items.POTATO, 40); put(m, Items.BEETROOT, 50);
        put(m, Items.PUMPKIN, 240); put(m, Items.MELON_SLICE, 20); put(m, Items.SUGAR_CANE, 20); put(m, Items.COCOA_BEANS, 60);
        put(m, Items.SWEET_BERRIES, 20); put(m, Items.APPLE, 80); put(m, Items.EGG, 10); put(m, Items.HONEY_BOTTLE, 150);
        put(m, ModItems.CHILI.get(), 100); put(m, ModItems2.TOMATO.get(), 100); put(m, ModItems2.STRAWBERRY.get(), 100);
        // 요리
        put(m, Items.BREAD, 80); put(m, Items.BAKED_POTATO, 30); put(m, Items.PUMPKIN_PIE, 200); put(m, Items.CAKE, 600);
        put(m, Items.COOKED_BEEF, 60); put(m, Items.COOKED_PORKCHOP, 60); put(m, Items.COOKED_CHICKEN, 40); put(m, Items.COOKED_MUTTON, 50);
        put(m, ModItems2.PIZZA.get(), 150); put(m, ModItems2.STRAWBERRY_TART.get(), 200); put(m, ModItems2.TOMATO_SOUP.get(), 200);
        put(m, ModItems.SPICY_STEW.get(), 300); put(m, ModItems.HONEY_TOAST.get(), 200); put(m, ModItems.SPICY_SKEWER.get(), 150);
        put(m, ModItems2.FISH_STEW.get(), 400); put(m, ModItems2.SASHIMI_PLATTER.get(), 350);
        // 물고기
        put(m, Items.COD, 300); put(m, Items.SALMON, 360); put(m, Items.TROPICAL_FISH, 600); put(m, Items.PUFFERFISH, 450);
        put(m, ModItems2.RAINBOW_TROUT.get(), 450); put(m, ModItems2.ICE_SMELT.get(), 450); put(m, ModItems2.MOONLIGHT_FISH.get(), 1500);
        put(m, ModItems2.STORM_EEL.get(), 1500); put(m, ModItems2.ANGLERFISH.get(), 3000); put(m, ModItems2.GOLDEN_CARP.get(), 30000);
        // 광물
        put(m, Items.COAL, 20); put(m, Items.IRON_INGOT, 125); put(m, Items.GOLD_INGOT, 300); put(m, Items.COPPER_INGOT, 30);
        put(m, Items.REDSTONE, 20); put(m, Items.LAPIS_LAZULI, 50); put(m, Items.DIAMOND, 3000); put(m, Items.EMERALD, 1000);
        put(m, Items.AMETHYST_SHARD, 40); put(m, Items.QUARTZ, 40);
        put(m, ModItems2.RAW_MYTHRIL.get(), 500); put(m, ModItems2.MYTHRIL_INGOT.get(), 700); put(m, ModItems2.JADE.get(), 500);
        put(m, ModItems.BLOOD_SHARD.get(), 250);
        // 몹 전리품
        put(m, Items.ROTTEN_FLESH, 2); put(m, Items.BONE, 10); put(m, Items.STRING, 10); put(m, Items.GUNPOWDER, 25);
        put(m, Items.SPIDER_EYE, 15); put(m, Items.ENDER_PEARL, 150); put(m, Items.BLAZE_ROD, 200); put(m, Items.LEATHER, 30);
        put(m, Items.FEATHER, 5); put(m, Items.SLIME_BALL, 30); put(m, Items.PHANTOM_MEMBRANE, 100);
        // 8차
        put(m, com.snolred.wildplus.registry.ModItems6.LORD_HEART.get(), 200000);
        put(m, com.snolred.wildplus.registry.ModItems5.CORN.get(), 80); put(m, com.snolred.wildplus.registry.ModItems5.CABBAGE.get(), 120);
        put(m, com.snolred.wildplus.registry.ModItems5.BLUEBERRY.get(), 80); put(m, com.snolred.wildplus.registry.ModItems5.GRILLED_CORN.get(), 100);
        put(m, com.snolred.wildplus.registry.ModItems5.SALAD.get(), 350); put(m, com.snolred.wildplus.registry.ModItems5.BLUEBERRY_MUFFIN.get(), 200);
        put(m, com.snolred.wildplus.registry.ModItems5.CATFISH.get(), 600); put(m, com.snolred.wildplus.registry.ModItems5.TUNA.get(), 900);
        put(m, com.snolred.wildplus.registry.ModItems5.SWEETFISH.get(), 600); put(m, com.snolred.wildplus.registry.ModItems5.DRAGON_KING_FISH.get(), 60000);
        put(m, com.snolred.wildplus.registry.ModItems5.TUNA_STEAK.get(), 450);
        // 16차: 루비 · 광물 블록 · 원석 · 낚시 잡동사니
        put(m, com.snolred.wildplus.registry.ModItems7.RUBY.get(), 4000);
        put(m, com.snolred.wildplus.registry.ModItems7.RUBY_BLOCK.get(), 36000);
        put(m, com.snolred.wildplus.registry.ModItems2.MYTHRIL_BLOCK.get(), 6300);
        put(m, com.snolred.wildplus.registry.ModItems2.JADE_BLOCK.get(), 4500);
        put(m, ModItems.BLOODSTEEL_INGOT.get(), 1500);
        put(m, Items.RAW_IRON, 100); put(m, Items.RAW_GOLD, 250); put(m, Items.RAW_COPPER, 25);
        put(m, Items.RAW_IRON_BLOCK, 900); put(m, Items.RAW_GOLD_BLOCK, 2250); put(m, Items.RAW_COPPER_BLOCK, 225);
        put(m, Items.COAL_BLOCK, 180); put(m, Items.IRON_BLOCK, 1125); put(m, Items.GOLD_BLOCK, 2700);
        put(m, Items.REDSTONE_BLOCK, 180); put(m, Items.LAPIS_BLOCK, 450); put(m, Items.DIAMOND_BLOCK, 27000); put(m, Items.EMERALD_BLOCK, 9000);
        put(m, Items.QUARTZ_BLOCK, 160); put(m, Items.AMETHYST_BLOCK, 160);
        put(m, Items.NETHERITE_SCRAP, 4000); put(m, Items.NETHERITE_INGOT, 18000); put(m, Items.NETHERITE_BLOCK, 162000);
        put(m, Items.GOLD_NUGGET, 33); put(m, Items.IRON_NUGGET, 14);
        // 낚시: 익힌 물고기 · 잡동사니 · 보물
        put(m, Items.COOKED_COD, 130); put(m, Items.COOKED_SALMON, 150);
        put(m, ModItems2.COOKED_RAINBOW_TROUT.get(), 200); put(m, ModItems2.GRILLED_EEL.get(), 650);
        put(m, com.snolred.wildplus.registry.ModItems5.GRILLED_SWEETFISH.get(), 260);
        put(m, Items.NAUTILUS_SHELL, 800); put(m, Items.HEART_OF_THE_SEA, 15000); put(m, Items.NAME_TAG, 1500); put(m, Items.SADDLE, 1200);
        put(m, Items.ENCHANTED_BOOK, 500); put(m, Items.BOW, 100); put(m, Items.FISHING_ROD, 50); put(m, Items.LILY_PAD, 10);
        put(m, Items.INK_SAC, 20); put(m, Items.BOWL, 2); put(m, Items.LEATHER_BOOTS, 20); put(m, Items.STICK, 1);
        put(m, Items.TRIPWIRE_HOOK, 10); put(m, Items.BAMBOO, 5); put(m, Items.GLOW_INK_SAC, 60); put(m, Items.PRISMARINE_SHARD, 30);
        put(m, Items.PRISMARINE_CRYSTALS, 40);
        // 19차: 물병 (물약도 같은 아이템이라 같은 값) · 빈 병
        // 2.18 새 작물
        put(m, com.snolred.wildplus.registry.ModItems13.GRAPE.get(), 200); put(m, com.snolred.wildplus.registry.ModItems13.RICE.get(), 60);
        put(m, com.snolred.wildplus.registry.ModItems13.ONION.get(), 70); put(m, com.snolred.wildplus.registry.ModItems13.GINSENG.get(), 6000);
        put(m, com.snolred.wildplus.registry.ModItems11.SAPPHIRE.get(), 3500);
        put(m, com.snolred.wildplus.registry.ModItems11.SAPPHIRE_BLOCK.get(), 31500);
        put(m, com.snolred.wildplus.registry.ModItems11.SAPPHIRE_ORE.get(), 3500);
        put(m, com.snolred.wildplus.registry.ModItems11.DEEPSLATE_SAPPHIRE_ORE.get(), 3500);
        put(m, Items.POTION, 20); put(m, Items.GLASS_BOTTLE, 5); put(m, Items.KELP, 2); put(m, Items.DRIED_KELP, 5); put(m, Items.SEA_PICKLE, 20);
        // 3.4 목장
        put(m, com.snolred.wildplus.registry.ModItems17.MILK_BOTTLE.get(), 150); put(m, com.snolred.wildplus.registry.ModItems17.BUTTER.get(), 350);
        put(m, com.snolred.wildplus.registry.ModItems17.CREAM.get(), 350); put(m, com.snolred.wildplus.registry.ModItems17.WOOL_TUFT.get(), 120);
        put(m, com.snolred.wildplus.registry.ModItems17.YARN.get(), 90); put(m, com.snolred.wildplus.registry.ModItems17.CLOTH.get(), 500);
        put(m, com.snolred.wildplus.registry.ModItems17.GOLDEN_EGG.get(), 8000);
        com.snolred.wildplus.cooking.Cooking.prices(m); // 요리: 재료값 × 2.5
        // 3.9 행운의 부적: 1단계 1,000동 · 단계마다 9배 (합친 값 그대로) · 최대 2,000만동
        for (int t = 1; t <= 10; t++) {
            long won = Math.min(20_000_000L, 1_000L * (long) Math.pow(9, t - 1));
            put(m, com.snolred.wildplus.registry.ModItems21.charm(t), (int) (won * 100));
        }
        table = m;
        return m;
    }

    /** 1개 가격 (동 × 100), 팔 수 없으면 0 */
    public static int centi(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int base = table().getOrDefault(stack.getItem(), 0);
        return base == 0 ? 0 : (int) ((long) base * Grade.priceX100(stack) / 100); // 등급 배율
    }

    /** "0.2동" 형식 */
    public static String fmt(int centi) {
        return centi % 100 == 0 ? (centi / 100) + "동" : String.format("%.2f", centi / 100.0).replaceAll("0$", "") + "동";
    }
}

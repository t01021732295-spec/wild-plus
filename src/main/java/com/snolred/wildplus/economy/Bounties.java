package com.snolred.wildplus.economy;

import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 의뢰 게시판: 하루마다 플레이어별 납품 의뢰 3개 (보상은 출하 가격의 2배, 최소 5동) */
public final class Bounties {
    public static final int COUNT = 3;

    public record Bounty(ItemStack stack, long reward) {}

    private record Kind(Item item, int min, int max) {}

    private Bounties() {}

    private static List<Kind> pool() {
        return List.of(
                new Kind(Items.WHEAT, 16, 48), new Kind(Items.CARROT, 16, 48), new Kind(Items.POTATO, 16, 48),
                new Kind(Items.PUMPKIN, 4, 12), new Kind(Items.SUGAR_CANE, 16, 48), new Kind(Items.EGG, 8, 16),
                new Kind(ModItems2.TOMATO.get(), 8, 24), new Kind(ModItems2.STRAWBERRY.get(), 8, 24), new Kind(ModItems.CHILI.get(), 8, 24),
                new Kind(Items.BREAD, 8, 24), new Kind(Items.COOKED_BEEF, 8, 24), new Kind(ModItems2.PIZZA.get(), 4, 12),
                new Kind(Items.COD, 4, 12), new Kind(Items.SALMON, 4, 12), new Kind(ModItems2.RAINBOW_TROUT.get(), 2, 8),
                new Kind(Items.COAL, 16, 48), new Kind(Items.IRON_INGOT, 4, 16), new Kind(Items.REDSTONE, 16, 32),
                new Kind(Items.GOLD_INGOT, 2, 8), new Kind(ModItems2.JADE.get(), 1, 4), new Kind(ModItems2.MYTHRIL_INGOT.get(), 1, 4),
                new Kind(Items.BONE, 8, 24), new Kind(Items.STRING, 8, 24), new Kind(Items.GUNPOWDER, 4, 16),
                new Kind(Items.ROTTEN_FLESH, 16, 48), new Kind(Items.LEATHER, 4, 12), new Kind(Items.ENDER_PEARL, 2, 6),
                new Kind(ModItems.BLOOD_SHARD.get(), 2, 6));
    }

    /** 그날 그 플레이어의 의뢰 (날짜와 플레이어로 정해지므로 저장할 필요가 없다) */
    public static List<Bounty> forDay(UUID player, long day) {
        Random rnd = new Random(day * 1_000_003L ^ player.getMostSignificantBits() ^ player.getLeastSignificantBits());
        List<Kind> kinds = new ArrayList<>(pool());
        List<Bounty> out = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            Kind k = kinds.remove(rnd.nextInt(kinds.size()));
            int n = k.min() + rnd.nextInt(k.max() - k.min() + 1);
            ItemStack s = new ItemStack(k.item(), n);
            long reward = Math.max(5, (long) ShipPrices.centi(s) * n * 2 / 100);
            out.add(new Bounty(s, reward));
        }
        return out;
    }
}

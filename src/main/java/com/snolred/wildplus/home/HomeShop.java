package com.snolred.wildplus.home;

import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModItems16;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 가구점: 집 상자 6종 + 가구 (진열장 화면). 고르면 바로 결제 (통장 먼저). */
public final class HomeShop {
    private HomeShop() {}

    private record Good(String name, String sub, long price, Item item) {}

    private static final Map<String, Long> HOUSE_PRICE = Map.of("cabin", 50_000L, "log_house", 120_000L, "brick_house", 250_000L,
            "hanok", 400_000L, "modern", 600_000L, "mansion", 1_200_000L);
    private static final Map<String, String> HOUSE_SUB = Map.of("cabin", "9×11 · 방 2 · 거실·부엌 + 다락 침실",
            "log_house", "11×11 · 방 3 · 거실(벽난로) · 부엌 · 침실 · 베란다", "brick_house", "11×12 · 방 5 · 2층 · 침실·서재·욕실 · 발코니",
            "hanok", "13×12 · 방 3 · 대청마루 · 안방 · 사랑방", "modern", "13×13 · 방 4 · 침실 · 드레스룸 · 옥상 정원 · 수영장",
            "mansion", "19×18 · 방 9 · 3층 · 식당 · 옷방 · 욕실 · 서재 · 음악실");

    private static long furniturePrice(String n) {
        if (n.startsWith("sofa_")) return 12_000;
        return switch (n) {
            case "armchair", "coffee_table", "nightstand", "monstera", "floor_lamp" -> 5_000;
            case "rug", "sink", "grandfather_clock", "chandelier" -> 8_000;
            case "tv_stand", "desk", "vanity", "bathtub", "fridge", "wardrobe", "bookshelf" -> 15_000;
            case "fireplace", "aquarium" -> 20_000;
            case "piano" -> 30_000;
            default -> 10_000;
        };
    }

    private static List<Good> goods() {
        List<Good> g = new ArrayList<>();
        ModItems16.HOUSES.forEach((id, it) -> g.add(new Good("🏠 " + new ItemStack(it.get()).getHoverName().getString(),
                HOUSE_SUB.getOrDefault(id, ""), HOUSE_PRICE.getOrDefault(id, 100_000L), it.get())));
        g.add(new Good("캐노피 침대", "잠자기 · 리스폰", 15_000, ModItems16.CANOPY_BED.get()));
        g.add(new Good("이층 침대", "잠자기 · 리스폰", 10_000, ModItems16.BUNK_BED.get()));
        ModItems16.FURNITURE.forEach((n, it) -> g.add(new Good(new ItemStack(it.get()).getHoverName().getString(), "", furniturePrice(n), it.get())));
        return g;
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        List<Good> list = goods();
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (Good g : list) entries.add(new PickMenu.Entry(g.name(), String.format("%,d동", g.price()), new ItemStack(g.item())));
        PickMenu.open(p, Component.literal("🛋 가구점"), "집 상자 · 가구 · 고르면 바로 결제", "", entries, PickMenu.FLAG_SHOP, i -> {
            Good g = list.get(i);
            if (!Honor.pay(p, g.price())) {
                p.sendOverlayMessage(Component.literal(String.format("돈이 모자라요 (%,d동)", g.price())).withStyle(ChatFormatting.RED));
                return;
            }
            ItemStack s = new ItemStack(g.item());
            if (!p.getInventory().add(s)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.6F, 1.2F);
            p.sendSystemMessage(Component.literal(String.format("🛋 %s 구매 (%,d동)", g.name(), g.price())).withStyle(ChatFormatting.GOLD));
            open(p, pos);
        }, null, pl -> pos == null || pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }
}

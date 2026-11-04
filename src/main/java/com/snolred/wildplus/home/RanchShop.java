package com.snolred.wildplus.home;

import com.snolred.wildplus.economy.Honor;
import com.snolred.wildplus.menu.PickMenu;
import com.snolred.wildplus.registry.ModItems17;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 3.4 가축 상점: 가축(알) · 사료 · 브러시 · 목장 기계 · 축사 상자 (진열장 화면, 고르면 바로 결제) */
public final class RanchShop {
    private RanchShop() {}

    private record Good(String name, String sub, long price, Item item, int count) {}

    private static List<Good> goods() {
        List<Good> g = new ArrayList<>();
        g.add(new Good("🏚 큰 축사 상자", "13×15 · 소·양 우리 · 건초 다락 · 여물통 · 마당", 150_000, ModItems17.BARN_KIT.get(), 1));
        g.add(new Good("🐔 닭장 상자", "7×9 · 닭 우리 · 둥지 상자 · 철망 마당", 40_000, ModItems17.COOP_KIT.get(), 1));
        g.add(new Good("🪮 동물 브러시", "하루 한 번 빗질 → 애정도 · 생산물", 1_500, ModItems17.ANIMAL_BRUSH.get(), 1));
        g.add(new Good("🌾 건초 더미 ×8", "여물통 사료 72회분", 1_200, Items.HAY_BLOCK, 8));
        g.add(new Good("🐄 소 알", "우유", 5_000, Items.COW_SPAWN_EGG, 1));
        g.add(new Good("🐑 양 알", "양털 뭉치", 4_000, Items.SHEEP_SPAWN_EGG, 1));
        g.add(new Good("🐓 닭 알", "달걀 · 황금 알", 1_500, Items.CHICKEN_SPAWN_EGG, 1));
        g.add(new Good("🐐 염소 알", "우유", 4_000, Items.GOAT_SPAWN_EGG, 1));
        g.add(new Good("🐖 돼지 알", "버섯 찾아 오기", 3_000, Items.PIG_SPAWN_EGG, 1));
        g.add(new Good("건초 여물통", "근처 동물 애정도 +25/일", 3_000, ModItems17.HAY_TROUGH.get(), 1));
        g.add(new Good("둥지 상자", "닭이 알을 낳는 곳", 2_000, ModItems17.NEST_BOX.get(), 1));
        g.add(new Good("치즈 숙성통", "우유 → 치즈 (1분)", 6_000, ModItems17.CHEESE_BARREL.get(), 1));
        g.add(new Good("버터 교반기", "우유 → 버터 · 생크림 (20초)", 5_000, ModItems17.BUTTER_CHURN.get(), 1));
        g.add(new Good("물레", "양털 → 실 (10초) · 실 4 → 천", 5_000, ModItems17.SPINNING_WHEEL.get(), 1));
        return g;
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        List<Good> list = goods();
        List<PickMenu.Entry> entries = new ArrayList<>();
        for (Good g : list) entries.add(new PickMenu.Entry(g.name(), String.format("%,d동", g.price()), new ItemStack(g.item(), g.count())));
        PickMenu.open(p, Component.literal("🐄 가축 상점"), "가축 · 사료 · 목장 기계 · 축사 상자 · 고르면 바로 결제", "", entries, PickMenu.FLAG_SHOP, i -> {
            Good g = list.get(i);
            if (!Honor.pay(p, g.price())) {
                p.sendOverlayMessage(Component.literal(String.format("돈이 모자라요 (%,d동)", g.price())).withStyle(ChatFormatting.RED));
                return;
            }
            ItemStack s = new ItemStack(g.item(), g.count());
            if (!p.getInventory().add(s)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), s);
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.6F, 1.2F);
            p.sendSystemMessage(Component.literal(String.format("🐄 %s 구매 (%,d동)", g.name(), g.price())).withStyle(ChatFormatting.GOLD));
            open(p, pos);
        }, null, pl -> pos == null || pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0);
    }
}

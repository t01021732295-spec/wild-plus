package com.snolred.wildplus.economy;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/**
 * 등급 (물고기 · 작물 공통). 아이템에 "wp_grade" 1~3 으로 붙고 출하 가격에 배율이 곱해진다.
 * <ul>
 *   <li>물고기: 고급 ×1.5 · 특급 ×3 · 전설 ×10</li>
 *   <li>작물: 은 ×1.25 · 금 ×1.5 · 무지개 ×2</li>
 * </ul>
 * 같은 등급끼리만 겹쳐진다. 조합(요리 재료)에는 등급과 상관없이 쓸 수 있다.
 */
public final class Grade {
    private Grade() {}

    public static final String KEY = "wp_grade";
    public static final String[] FISH = {"", "고급", "특급", "전설"};
    public static final String[] CROP = {"", "은", "금", "무지개"};
    private static final int[] FISH_X100 = {100, 150, 300, 1000};
    private static final int[] CROP_X100 = {100, 125, 150, 200};
    private static final ChatFormatting[] COLOR = {ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE};
    private static final Rarity[] RARITY = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};

    public static int of(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return 0;
        return Math.max(0, Math.min(3, d.copyTag().getIntOr(KEY, 0)));
    }

    private static java.util.Set<net.minecraft.world.item.Item> fishSet;

    /** 물고기인가 (바닐라 물고기 태그 + 모드 어종) */
    public static boolean isFish(ItemStack s) {
        if (s.is(ItemTags.FISHES)) return true;
        if (fishSet == null) {
            fishSet = java.util.Set.of(com.snolred.wildplus.registry.ModItems2.RAINBOW_TROUT.get(), com.snolred.wildplus.registry.ModItems2.ICE_SMELT.get(),
                    com.snolred.wildplus.registry.ModItems2.MOONLIGHT_FISH.get(), com.snolred.wildplus.registry.ModItems2.STORM_EEL.get(),
                    com.snolred.wildplus.registry.ModItems2.ANGLERFISH.get(), com.snolred.wildplus.registry.ModItems2.GOLDEN_CARP.get(),
                    com.snolred.wildplus.registry.ModItems5.CATFISH.get(), com.snolred.wildplus.registry.ModItems5.TUNA.get(),
                    com.snolred.wildplus.registry.ModItems5.SWEETFISH.get(), com.snolred.wildplus.registry.ModItems5.DRAGON_KING_FISH.get());
        }
        return fishSet.contains(s.getItem());
    }

    public static String name(ItemStack s, int g) {
        return (isFish(s) ? FISH : CROP)[g];
    }

    /** 출하 가격 배율 (×100) */
    public static int priceX100(ItemStack s) {
        int g = of(s);
        if (g == 0) return 100;
        return (isFish(s) ? FISH_X100 : CROP_X100)[g];
    }

    /** 등급을 붙인다 (0이면 아무것도 안 함) */
    public static ItemStack apply(ItemStack s, int g) {
        if (g <= 0 || s.isEmpty()) return s;
        g = Math.min(3, g);
        CompoundTag tag = s.has(DataComponents.CUSTOM_DATA) ? s.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        tag.putInt(KEY, g);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        s.set(DataComponents.RARITY, RARITY[g]);
        String stars = "★".repeat(g);
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(stars + " " + name(s, g) + " 등급").setStyle(Style.EMPTY.withColor(COLOR[g]).withItalic(false)),
                Component.literal(String.format("출하 가격 ×%.2f", priceX100(s) / 100.0).replace(".00", "")).setStyle(
                        Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))));
        return s;
    }

    public static MutableComponent tag(ItemStack s, int g) {
        return Component.literal("[" + "★".repeat(g) + " " + name(s, g) + "]").withStyle(COLOR[g]);
    }
}

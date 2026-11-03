package com.snolred.wildplus.cooking;

import com.snolred.wildplus.economy.Grade;
import com.snolred.wildplus.registry.ModItems14;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** 요리 레시피 맞추기 · 등급 · 가격 · 등급 효과 */
public final class Cooking {
    private Cooking() {}

    public static boolean matches(String ing, ItemStack s) {
        if (s.isEmpty()) return false;
        if (ing.equals("@fish")) return Grade.isFish(s) && !s.is(ModItems14.dish("sushi_set"));
        Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(ing));
        return it != Items.AIR && s.is(it);
    }

    /** 재료 칸들로 이 기구가 만들 수 있는 레시피 (재료가 많은 레시피부터, 남는 종류가 없어야 함) */
    public static CookingData.Dish find(CookingData.Station st, List<ItemStack> inputs) {
        CookingData.Dish best = null;
        for (CookingData.Dish d : CookingData.DISHES) {
            if (d.station() != st) continue;
            if (best != null && best.ingredients().length >= d.ingredients().length) continue;
            if (plan(d, inputs) != null) best = d;
        }
        return best;
    }

    /** 레시피 한 번에 각 칸에서 몇 개씩 쓰는지 (안 되면 null) */
    public static int[] plan(CookingData.Dish d, List<ItemStack> inputs) {
        int[] take = new int[inputs.size()];
        for (String ing : d.ingredients()) {
            int found = -1;
            for (int i = 0; i < inputs.size(); i++) {
                ItemStack s = inputs.get(i);
                if (matches(ing, s) && s.getCount() - take[i] > 0) {
                    found = i;
                    break;
                }
            }
            if (found < 0) return null;
            take[found]++;
        }
        // 레시피에 안 쓰이는 재료가 들어 있으면 안 됨
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack s = inputs.get(i);
            if (s.isEmpty()) continue;
            boolean used = false;
            for (String ing : d.ingredients()) if (matches(ing, s)) used = true;
            if (!used) return null;
        }
        return take;
    }

    /** 재료 등급 평균으로 요리 등급 (0~3) */
    public static int grade(List<ItemStack> used, RandomSource r) {
        if (used.isEmpty()) return 0;
        float sum = 0;
        int n = 0;
        for (ItemStack s : used) {
            if (s.is(Items.BOWL) || s.is(Items.GLASS_BOTTLE) || s.is(Items.STICK)) continue;
            sum += Grade.of(s);
            n++;
        }
        if (n == 0) return 0;
        float avg = sum / n + (r.nextFloat() - 0.5F) * 0.6F;
        return Math.max(0, Math.min(3, Math.round(avg)));
    }

    /** 출하 가격: 재료값 합 × 2.5 (최소 3동) — 재료값이 아직 없으면 20 */
    public static void prices(java.util.Map<Item, Integer> m) {
        // 재료 요리(김치 · 치즈)가 먼저 정해지도록 두 번
        for (int pass = 0; pass < 2; pass++) {
            for (CookingData.Dish d : CookingData.DISHES) {
                long sum = 0;
                for (String ing : d.ingredients()) {
                    if (ing.equals("@fish")) {
                        sum += 300;
                        continue;
                    }
                    Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(ing));
                    sum += it == Items.MILK_BUCKET ? 400 : m.getOrDefault(it, it == Items.BOWL || it == Items.GLASS_BOTTLE ? 5 : 20);
                }
                m.put(ModItems14.dish(d.id()), (int) Math.max(300, sum * 5 / 2 / Math.max(1, d.count())));
            }
        }
    }

    public static CookingData.Dish dishOf(ItemStack s) {
        for (CookingData.Dish d : CookingData.DISHES) if (s.is(ModItems14.dish(d.id()))) return d;
        return null;
    }

    /** 등급 요리를 먹으면 효과 시간 +50%/등급, 무지개는 세기 +1 */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
        ItemStack s = event.getItem();
        int g = Grade.of(s);
        if (g <= 0) return;
        CookingData.Dish d = dishOf(s);
        if (d == null) return;
        for (CookingData.Eff e : d.effects()) {
            p.addEffect(new MobEffectInstance(ModItems14.effect(e.id()), (int) (e.seconds() * 20 * (1 + 0.5F * g)), e.amp() + (g >= 3 ? 1 : 0)));
        }
    }

    public static List<ItemStack> icons(CookingData.Dish d) {
        List<ItemStack> out = new ArrayList<>();
        for (String ing : d.ingredients()) {
            if (ing.equals("@fish")) out.add(new ItemStack(Items.COD));
            else out.add(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(ing))));
        }
        return out;
    }

    static boolean isTagFish(ItemStack s) {
        return s.is(ItemTags.FISHES);
    }
}

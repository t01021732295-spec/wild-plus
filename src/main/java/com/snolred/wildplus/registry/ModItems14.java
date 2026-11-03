package com.snolred.wildplus.registry;

import static com.snolred.wildplus.registry.ModItems.ITEMS;

import com.snolred.wildplus.cooking.CookingData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.neoforge.registries.DeferredItem;

/** 요리 2차: 요리 41종(CookingData) + 조리 기구 6종 블록 아이템 */
public final class ModItems14 {
    public static final Map<String, DeferredItem<Item>> DISH = new LinkedHashMap<>();

    public static Holder<MobEffect> effect(String id) {
        return switch (id) {
            case "strength" -> MobEffects.STRENGTH;
            case "resistance" -> MobEffects.RESISTANCE;
            case "water_breathing" -> MobEffects.WATER_BREATHING;
            case "dolphins_grace" -> MobEffects.DOLPHINS_GRACE;
            case "haste" -> MobEffects.HASTE;
            case "regeneration" -> MobEffects.REGENERATION;
            case "health_boost" -> MobEffects.HEALTH_BOOST;
            case "absorption" -> MobEffects.ABSORPTION;
            case "fire_resistance" -> MobEffects.FIRE_RESISTANCE;
            case "jump_boost" -> MobEffects.JUMP_BOOST;
            case "luck" -> MobEffects.LUCK;
            case "night_vision" -> MobEffects.NIGHT_VISION;
            default -> MobEffects.SPEED;
        };
    }

    static {
        for (CookingData.Dish d : CookingData.DISHES) {
            DISH.put(d.id(), ITEMS.registerSimpleItem(d.id(), p -> {
                List<MobEffectInstance> fx = new ArrayList<>();
                for (CookingData.Eff e : d.effects()) fx.add(new MobEffectInstance(effect(e.id()), e.seconds() * 20, e.amp()));
                boolean drink = d.container().equals("bottle");
                Consumable.Builder c = drink ? Consumables.defaultDrink() : Consumables.defaultFood();
                if (!fx.isEmpty()) c = c.onConsume(new ApplyStatusEffectsConsumeEffect(fx));
                p = p.food(new FoodProperties.Builder().nutrition(drink ? 2 : d.nutrition()).saturationModifier(d.saturation())
                        .alwaysEdible().build(), c.build());
                if (d.container().equals("bowl")) p = p.stacksTo(16).usingConvertsTo(Items.BOWL);
                if (drink) p = p.stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE);
                return p;
            }));
        }
    }

    public static final DeferredItem<BlockItem> COOKING_POT = ITEMS.registerSimpleBlockItem("cooking_pot", ModBlocks4.COOKING_POT);
    public static final DeferredItem<BlockItem> BRICK_OVEN = ITEMS.registerSimpleBlockItem("brick_oven", ModBlocks4.BRICK_OVEN);
    public static final DeferredItem<BlockItem> BBQ_GRILL = ITEMS.registerSimpleBlockItem("bbq_grill", ModBlocks4.BBQ_GRILL);
    public static final DeferredItem<BlockItem> PREP_TABLE = ITEMS.registerSimpleBlockItem("prep_table", ModBlocks4.PREP_TABLE);
    public static final DeferredItem<BlockItem> KITCHEN_STOVE = ITEMS.registerSimpleBlockItem("kitchen_stove", ModBlocks4.KITCHEN_STOVE);
    public static final DeferredItem<BlockItem> AGING_RACK = ITEMS.registerSimpleBlockItem("aging_rack", ModBlocks4.AGING_RACK);

    public static Item dish(String id) {
        DeferredItem<Item> d = DISH.get(id);
        return d == null ? Items.AIR : d.get();
    }

    public static void init() {}

    private ModItems14() {}
}

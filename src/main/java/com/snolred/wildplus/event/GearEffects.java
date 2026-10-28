package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 핏빛 강철 장비의 특수 능력
 * - 검: 입힌 피해의 20%만큼 체력 회복 (흡혈)
 * - 갑옷 4부위 세트: 밤에 받는 몬스터 피해 15% 감소
 */
public final class GearEffects {
    private GearEffects() {}

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (event.getSource().getDirectEntity() != player) {
            return; // 화살 등 간접 공격 제외
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.is(ModItems.BLOODSTEEL_SWORD.get()) && player.getHealth() < player.getMaxHealth()) {
            float heal = Math.min(event.getHealthDamage() * 0.2F, 3.0F);
            if (heal > 0.0F) {
                player.heal(heal);
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Player player) || !(event.getSource().getEntity() instanceof net.minecraft.world.entity.monster.Monster)) {
            return;
        }
        if (hasFullSet(player) && player.level().isDarkOutside()) {
            event.setAmount(event.getAmount() * 0.85F);
        }
    }

    private static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BLOODSTEEL_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BLOODSTEEL_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BLOODSTEEL_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BLOODSTEEL_BOOTS.get());
    }
}

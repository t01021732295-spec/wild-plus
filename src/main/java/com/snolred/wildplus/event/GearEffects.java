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
 * 미스릴 갑옷 세트: 성급함 I / 비취 갑옷 세트: 행운 I
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

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 40 != 0) return;
        if (fullSet(player, com.snolred.wildplus.registry.ModItems4.MYTHRIL_HELMET.get(), com.snolred.wildplus.registry.ModItems4.MYTHRIL_CHESTPLATE.get(),
                com.snolred.wildplus.registry.ModItems4.MYTHRIL_LEGGINGS.get(), com.snolred.wildplus.registry.ModItems4.MYTHRIL_BOOTS.get())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HASTE, 80, 0, true, false, true));
        }
        if (fullSet(player, com.snolred.wildplus.registry.ModItems4.JADE_HELMET.get(), com.snolred.wildplus.registry.ModItems4.JADE_CHESTPLATE.get(),
                com.snolred.wildplus.registry.ModItems4.JADE_LEGGINGS.get(), com.snolred.wildplus.registry.ModItems4.JADE_BOOTS.get())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LUCK, 80, 0, true, false, true));
        }
    }

    private static boolean fullSet(Player p, net.minecraft.world.item.Item h, net.minecraft.world.item.Item c,
                                   net.minecraft.world.item.Item l, net.minecraft.world.item.Item b) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(h) && p.getItemBySlot(EquipmentSlot.CHEST).is(c)
                && p.getItemBySlot(EquipmentSlot.LEGS).is(l) && p.getItemBySlot(EquipmentSlot.FEET).is(b);
    }

    private static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BLOODSTEEL_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BLOODSTEEL_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BLOODSTEEL_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BLOODSTEEL_BOOTS.get());
    }
}

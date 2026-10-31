package com.snolred.wildplus.event;

import com.snolred.wildplus.registry.ModEffects;
import com.snolred.wildplus.registry.ModItems;
import com.snolred.wildplus.registry.ModItems2;
import com.snolred.wildplus.registry.ModItems3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 새 효과의 동작(취약·광전사·탐욕). 양조 레시피는 데이터(recipe/brewing_*.json) */
public final class EffectEvents {
    private EffectEvents() {}

    /** 취약·광전사: 받는 피해 증가 */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        float mul = 1.0F;
        MobEffectInstance frail = e.getEffect(ModEffects.FRAILTY);
        if (frail != null) mul += 0.25F * (frail.getAmplifier() + 1);
        if (e.hasEffect(ModEffects.BERSERK)) mul += 0.25F;
        if (mul != 1.0F) event.setAmount(event.getAmount() * mul);
    }

    /** 탐욕: 몬스터를 잡으면 은화 1~3개 (단계마다 더) */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Monster m) || !(event.getSource().getEntity() instanceof Player p)) return;
        MobEffectInstance greed = p.getEffect(ModEffects.GREED);
        if (greed == null) return;
        int n = (1 + p.getRandom().nextInt(3)) * (greed.getAmplifier() + 1);
        ItemEntity it = new ItemEntity(m.level(), m.getX(), m.getY() + 0.5, m.getZ(), new ItemStack(ModItems3.SILVER_COIN.get(), n));
        it.setDefaultPickUpDelay();
        event.getDrops().add(it);
    }
}

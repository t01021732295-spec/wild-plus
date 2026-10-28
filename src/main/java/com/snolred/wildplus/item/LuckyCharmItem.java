package com.snolred.wildplus.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 행운의 부적: 인벤토리에 있으면 행운 효과 (상자·낚시 보상이 좋아짐) */
public class LuckyCharmItem extends Item {
    public LuckyCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player && level.getGameTime() % 80 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 6, 0, true, false, true));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}

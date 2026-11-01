package com.snolred.wildplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 휴대용 수리 키트 (10회): 우클릭하면 손에 든 것 · 입은 갑옷 중 닳은 장비를
 * 하나당 최대 내구도의 25%씩 고친다. 고친 장비 하나마다 1회 소모.
 */
public class RepairKitItem extends Item {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public RepairKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        EquipmentSlot kitSlot = LivingEntity.getSlotForHand(hand);
        int fixed = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (slot == kitSlot || kit.isEmpty()) continue;
            ItemStack s = sp.getItemBySlot(slot);
            if (s.isEmpty() || !s.isDamageableItem() || !s.isDamaged() || s.getItem() instanceof RepairKitItem) continue;
            int amount = Math.max(1, s.getMaxDamage() / 4);
            s.setDamageValue(Math.max(0, s.getDamageValue() - amount));
            fixed++;
            kit.hurtAndBreak(1, sp, kitSlot);
        }
        if (fixed == 0) {
            sp.sendOverlayMessage(Component.literal("고칠 장비가 없어요 (손에 든 것 · 입은 갑옷만 고쳐요)").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5F, 1.3F);
        sp.sendOverlayMessage(Component.literal("🔧 장비 " + fixed + "개를 고쳤어요 (각 25%)").withStyle(ChatFormatting.GREEN));
        sp.getCooldowns().addCooldown(kit, 20);
        return InteractionResult.SUCCESS;
    }
}

package com.snolred.wildplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * 5.0.11 정화의 주문서: 귀속 저주 · 소실 저주 등 모든 저주 인챈트를 지운다.
 * 보조 손(왼손)에 든 장비 → 없으면 입고 있는 장비 중 저주 걸린 것 하나 (귀속 저주로 못 벗는 갑옷용).
 */
public class PurifyScrollItem extends Item {
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY};

    public PurifyScrollItem(Properties p) {
        super(p);
    }

    private static boolean cursed(ItemStack s) {
        if (s.isEmpty()) return false;
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(s).keySet()) if (e.is(EnchantmentTags.CURSE)) return true;
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        ItemStack target = p.getOffhandItem();
        if (!cursed(target)) {
            target = ItemStack.EMPTY;
            for (EquipmentSlot slot : WORN) {
                ItemStack s = p.getItemBySlot(slot);
                if (cursed(s)) {
                    target = s;
                    break;
                }
            }
        }
        if (target.isEmpty()) {
            p.sendOverlayMessage(Component.literal("저주 걸린 장비를 왼손(보조 손)에 들거나 입고 쓰세요").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        int[] n = {0};
        EnchantmentHelper.updateEnchantments(target, m -> m.removeIf(h -> {
            boolean c = h.is(EnchantmentTags.CURSE);
            if (c) n[0]++;
            return c;
        }));
        if (!p.getAbilities().instabuild) p.getMainHandItem().shrink(1);
        ServerLevel lv = (ServerLevel) level;
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
        p.sendSystemMessage(Component.literal("✨ ").append(target.getHoverName()).append(Component.literal("의 저주 " + n[0] + "개를 정화했어요"))
                .withStyle(ChatFormatting.AQUA));
        return InteractionResult.CONSUME;
    }
}

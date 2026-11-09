package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.Coins;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** 4.5 운명의 제단: 든 장비에 모든 인챈트 중 하나를 1~15단계로 붙인다 (3~5만동). 1~9단계 각 8% · 10~15단계 각 약 4.67% */
public final class FateAltar {
    private FateAltar() {}

    private static int level(net.minecraft.util.RandomSource r) {
        return r.nextInt(100) < 72 ? 1 + r.nextInt(9) : 10 + r.nextInt(6);
    }

    public static void use(ServerPlayer p, BlockPos pos) {
        ItemStack stack = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty() || !com.snolred.wildplus.event.Engraves.gear(stack)) {
            p.sendOverlayMessage(Component.literal("장비를 손에 들고 쓰세요 (3~5만동)").withStyle(ChatFormatting.RED));
            return;
        }
        ServerLevel lv = (ServerLevel) p.level();
        var r = p.getRandom();
        long price = 30_000L + r.nextInt(20_001);
        if (!Coins.pay(p, price)) {
            p.sendOverlayMessage(Component.literal(String.format("%,d동이 필요해요", price)).withStyle(ChatFormatting.RED));
            return;
        }
        List<Holder.Reference<Enchantment>> all = lv.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList();
        Holder<Enchantment> pick = all.get(r.nextInt(all.size()));
        int want = level(r);
        int had = EnchantmentHelper.getItemEnchantmentLevel(pick, stack);
        EnchantmentHelper.updateEnchantments(stack, m -> m.upgrade(pick, want));
        Component name = Enchantment.getFullname(pick, want);
        boolean up = want > had;
        p.sendSystemMessage(Component.literal(String.format("🔮 %,d동 · ", price)).append(name)
                .append(Component.literal(up ? (had > 0 ? " 로 올랐어요!" : " 이 붙었어요!") : " 이 나왔지만 이미 더 높아요")).withStyle(want >= 10 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
        lv.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 60 + want * 8, 0.6, 0.6, 0.6, 0.6);
        lv.playSound(null, pos, want >= 10 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.9F, 1.0F);
    }
}

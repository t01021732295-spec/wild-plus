package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.menu.PickMenu;
import java.util.ArrayList;
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

/** 4.5 운명의 제단: 든 장비에 모든 인챈트 중 하나를 1~15단계로 붙인다 (2만 4천~4만동). 1~9단계 각 8% · 10~15단계 각 약 4.67% */
public final class FateAltar {
    private FateAltar() {}

    private static int level(net.minecraft.util.RandomSource r) {
        return r.nextInt(100) < 72 ? 1 + r.nextInt(9) : 10 + r.nextInt(6);
    }

    /** 5.0.11: 우클릭하면 제단 화면 (든 장비 · 붙은 인챈트 · 확률 · 굴리기 버튼) */
    public static void use(ServerPlayer p, BlockPos pos) {
        open(p, pos, "");
    }

    public static void open(ServerPlayer p, BlockPos pos, String last) {
        ItemStack stack = p.getItemInHand(InteractionHand.MAIN_HAND);
        boolean ok = !stack.isEmpty() && com.snolred.wildplus.event.Engraves.gear(stack);
        List<PickMenu.Entry> e = new ArrayList<>();
        StringBuilder ench = new StringBuilder();
        if (ok) {
            var list = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            for (var en : list.entrySet()) {
                if (!ench.isEmpty()) ench.append('\n');
                ench.append(Enchantment.getFullname(en.getKey(), en.getIntValue()).getString());
            }
            if (ench.isEmpty()) ench.append("(인챈트 없음)");
        }
        e.add(new PickMenu.Entry(ok ? stack.getHoverName().getString() : "장비를 손에 들고 오세요", ok ? ench.toString() : "검 · 도구 · 갑옷 · 활 등",
                ok ? stack.copyWithCount(1) : ItemStack.EMPTY, PickMenu.K_INFO));
        e.add(new PickMenu.Entry(last, String.format("1~9단계 · 각 8%%\n10~15단계 · 각 약 4.7%%\n모든 인챈트 중 하나 (저주 포함)\n이미 더 높으면 그대로\n§e한 번에 %,d~%,d동", MIN, MIN + SPREAD), ItemStack.EMPTY, PickMenu.K_INFO));
        e.add(new PickMenu.Entry("🔮 운명 굴리기", String.format("%,d~%,d동", MIN, MIN + SPREAD), ItemStack.EMPTY, ok ? PickMenu.K_PRIMARY : PickMenu.K_INFO));
        PickMenu.open(p, Component.literal("🔮 운명의 제단"), "든 장비에 무작위 인챈트 하나를 1~15단계로", "", e, PickMenu.themed(PickMenu.T_FATE), i -> {
            if (i == 2) open(p, pos, roll(p, pos));
        }, null, pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64);
    }

    private static final long MIN = 24_000L;
    private static final int SPREAD = 16_000;

    /** 굴린 결과 한 줄 (§ 색 포함) */
    private static String roll(ServerPlayer p, BlockPos pos) {
        ItemStack stack = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty() || !com.snolred.wildplus.event.Engraves.gear(stack)) return "§c장비를 손에 들고 쓰세요";
        ServerLevel lv = (ServerLevel) p.level();
        var r = p.getRandom();
        long price = MIN + r.nextInt(SPREAD + 1);
        if (!Coins.pay(p, price)) return String.format("§c%,d동이 필요해요", price);
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
        return (want >= 10 ? "§d" : "§b") + name.getString() + (up ? (had > 0 ? " §f로 올랐어요!" : " §f이 붙었어요!") : " §7(이미 더 높음)") + String.format(" §7· %,d동", price);
    }
}

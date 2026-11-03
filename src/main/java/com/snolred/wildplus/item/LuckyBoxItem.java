package com.snolred.wildplus.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 행운 상자 (명품관): 들고 우클릭하면 열린다. 무엇이 나왔는지 채팅으로 알려 주고, 대박이면 서버 전체에 알린다.
 * 25% 네더라이트 ×4 · 20% 루비 블록 ×2 · 20% 사파이어 블록 ×2 · 15% 불사의 토템 ×3 · 10% 네더의 별 · 7% 겉날개 · 3% 겉날개 + 네더의 별 ×2
 */
public class LuckyBoxItem extends Item {
    public LuckyBoxItem(Properties properties) {
        super(properties);
    }

    public static List<ItemStack> roll(RandomSource rnd) {
        float r = rnd.nextFloat();
        List<ItemStack> got = new ArrayList<>();
        if (r < 0.25F) got.add(new ItemStack(Items.NETHERITE_INGOT, 4));
        else if (r < 0.45F) got.add(new ItemStack(com.snolred.wildplus.registry.ModItems7.RUBY_BLOCK.get(), 2));
        else if (r < 0.65F) got.add(new ItemStack(com.snolred.wildplus.registry.ModItems11.SAPPHIRE_BLOCK.get(), 2));
        else if (r < 0.80F) got.add(new ItemStack(Items.TOTEM_OF_UNDYING, 3));
        else if (r < 0.90F) got.add(new ItemStack(Items.NETHER_STAR));
        else if (r < 0.97F) got.add(new ItemStack(Items.ELYTRA));
        else {
            got.add(new ItemStack(Items.ELYTRA));
            got.add(new ItemStack(Items.NETHER_STAR, 2));
        }
        return got;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            List<ItemStack> got = roll(player.getRandom());
            boolean jackpot = got.size() > 1 || got.get(0).is(Items.NETHER_STAR) || got.get(0).is(Items.ELYTRA);
            MutableComponent msg = Component.literal("🎁 행운 상자를 열었더니: ").withStyle(ChatFormatting.GOLD);
            for (int i = 0; i < got.size(); i++) {
                ItemStack s = got.get(i);
                if (i > 0) msg.append(Component.literal(" + ").withStyle(ChatFormatting.GRAY));
                msg.append(s.getHoverName().copy().withStyle(s.getRarity().color()));
                if (s.getCount() > 1) msg.append(Component.literal(" ×" + s.getCount()).withStyle(ChatFormatting.WHITE));
            }
            if (jackpot) msg.append(Component.literal("  ✦ 대박! ✦").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            player.sendSystemMessage(msg);
            if (jackpot) {
                server.getServer().getPlayerList().broadcastSystemMessage(Component.literal("🎁 " + player.getName().getString() + " 님이 행운 상자에서 ")
                        .withStyle(ChatFormatting.GOLD).append(got.get(0).getHoverName().copy().withStyle(got.get(0).getRarity().color()))
                        .append(Component.literal(" 대박!").withStyle(ChatFormatting.GOLD)), false);
            }
            for (ItemStack s : got) {
                ItemStack give = s.copy();
                if (!player.getInventory().add(give)) {
                    ItemEntity drop = new ItemEntity(server, player.getX(), player.getY() + 0.5, player.getZ(), give);
                    drop.setNoPickUpDelay();
                    server.addFreshEntity(drop);
                }
            }
            server.sendParticles(jackpot ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.2, player.getZ(), jackpot ? 60 : 20, 0.5, 0.6, 0.5, jackpot ? 0.4 : 0.05);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1.0F, 1.1F);
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    jackpot ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}

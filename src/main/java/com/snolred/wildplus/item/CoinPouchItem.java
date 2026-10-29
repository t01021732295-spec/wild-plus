package com.snolred.wildplus.item;

import com.snolred.wildplus.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 동전 주머니
 * - 우클릭: 가방의 동전을 모두 넣는다
 * - 웅크리고 우클릭: 전부 꺼낸다 (가장 적은 개수로 = 환전)
 * 자판기·결제는 주머니 금액도 자동으로 사용한다.
 */
public class CoinPouchItem extends Item {
    public CoinPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            long value = Coins.pouchValue(pouch);
            if (player.isSecondaryUseActive()) {
                if (value > 0) {
                    Coins.setPouchValue(pouch, 0);
                    Coins.give(player, value);
                }
                ((net.minecraft.server.level.ServerPlayer) player).sendOverlayMessage(Component.translatable("message.wildplus.pouch.withdraw", Coins.describe(value))
                        .withStyle(ChatFormatting.GOLD));
            } else {
                Inventory inv = player.getInventory();
                long added = 0;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    int unit = Coins.unitValue(s);
                    if (unit > 0) {
                        added += (long) unit * s.getCount();
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                }
                Coins.setPouchValue(pouch, value + added);
                ((net.minecraft.server.level.ServerPlayer) player).sendOverlayMessage(Component.translatable("message.wildplus.pouch.deposit",
                        Coins.describe(added), Coins.describe(value + added)).withStyle(ChatFormatting.GOLD));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }
}

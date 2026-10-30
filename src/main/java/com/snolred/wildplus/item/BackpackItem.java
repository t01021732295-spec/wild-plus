package com.snolred.wildplus.item;

import com.snolred.wildplus.menu.BackpackMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 배낭: 동(30칸) · 은(50칸) · 금(70칸). 들고 우클릭하면 열린다. 내용물은 아이템 자체에 저장. */
public class BackpackItem extends Item {
    public static final int COLUMNS = 10;
    public static final int[] ROWS = {3, 5, 7};
    public static final long[] UPGRADE_COST = {200, 800};

    public final int tier;

    public BackpackItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int size() {
        return ROWS[tier] * COLUMNS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            int slot = hand == InteractionHand.MAIN_HAND ? sp.getInventory().getSelectedSlot() : 40;
            open(sp, slot);
        }
        return InteractionResult.SUCCESS;
    }

    public static void open(ServerPlayer player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof BackpackItem bag)) return;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BackpackMenu(id, inv, bag.tier, slot, stack), stack.getHoverName()),
                buf -> {
                    buf.writeVarInt(bag.tier);
                    buf.writeVarInt(slot);
                });
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.7F, 1.1F);
    }

    public static Component tierName(int tier) {
        return Component.translatable("gui.wildplus.backpack.tier" + tier);
    }
}

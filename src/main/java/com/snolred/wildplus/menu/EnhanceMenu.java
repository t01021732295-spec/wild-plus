package com.snolred.wildplus.menu;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.economy.Enhance;
import com.snolred.wildplus.registry.ModMenus;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * 강화대: 장비(또는 마법이 부여된 책)를 올려놓으면 바닐라 최대를 넘는 레벨을 동전으로 올리고, 동전으로 수리도 한다.
 * 버튼 0~19 = 그 줄 인챈트 +1, 50 = 수리. 창을 닫으면 아이템은 돌려준다.
 */
public class EnhanceMenu extends AbstractContainerMenu {
    public static final int BTN_REPAIR = 50;
    public static final int SLOT_X = 20, SLOT_Y = 40;

    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnhanceMenu.this.slotsChanged(this);
        }
    };
    private final @Nullable BlockPos pos;

    public EnhanceMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, (BlockPos) null);
    }

    private EnhanceMenu(int id, Inventory inv, @Nullable BlockPos pos) {
        super(ModMenus.ENHANCE.get(), id);
        this.pos = pos;
        addSlot(new Slot(input, 0, SLOT_X, SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inv, 38, 120);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new EnhanceMenu(id, inv, pos),
                Component.translatable("container.wildplus.enhance")), buf -> {});
    }

    public ItemStack item() {
        return slots.get(0).getItem();
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp)) return false;
        ItemStack stack = input.getItem(0);
        if (stack.isEmpty()) return false;
        if (id == BTN_REPAIR) {
            long cost = Enhance.repairCost(stack);
            if (cost <= 0) return true;
            if (!Coins.pay(sp, cost)) {
                sp.sendOverlayMessage(Component.translatable("message.wildplus.enhance.no_money", cost).withStyle(ChatFormatting.RED));
                return true;
            }
            stack.setDamageValue(0);
            input.setChanged();
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
            return true;
        }
        List<Enhance.Option> opts = Enhance.options(stack);
        if (id < 0 || id >= opts.size()) return false;
        Enhance.Option o = opts.get(id);
        if (!Coins.pay(sp, o.cost())) {
            sp.sendOverlayMessage(Component.translatable("message.wildplus.enhance.no_money", o.cost()).withStyle(ChatFormatting.RED));
            return true;
        }
        Enhance.apply(stack, o);
        input.setChanged();
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        sp.sendSystemMessage(Component.translatable("message.wildplus.enhance.done", Enchantment.getFullname(o.enchant(), o.level() + 1), o.cost())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index == 0) {
            if (!moveItemStackTo(s, 1, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(s, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) clearContainer(player, input);
    }

    @Override
    public boolean stillValid(Player p) {
        return pos == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }
}

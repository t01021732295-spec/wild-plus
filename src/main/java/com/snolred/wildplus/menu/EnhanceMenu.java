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
    public static final int BTN_REPAIR = 50, BTN_STAR = 60, BTN_TRUE_DMG = 61, BTN_ENGRAVE = 62, BTN_LIFESTEAL = 63;
    /** 흡혈 강화: 핏빛 강철 주괴 50개마다 +1%, 최대 30% */
    public static final int LIFESTEAL_COST = 50, LIFESTEAL_MAX = 30;
    public static final long STAR_COST = 1000, TRUE_DMG_COST = 500, ENGRAVE_COST = 2000;

    /** 레이드 전리품 → 각인 종류 */
    public static String engraveKind(ItemStack mat) {
        if (mat.is(com.snolred.wildplus.registry.ModItems8.TITAN_CORE.get())) return "TITAN";
        if (mat.is(com.snolred.wildplus.registry.ModItems8.LICH_ORB.get())) return "LICH";
        if (mat.is(com.snolred.wildplus.registry.ModItems8.FROST_CROWN.get())) return "FROST";
        if (mat.is(com.snolred.wildplus.registry.ModItems6.LORD_HEART.get())) return "BLOOD";
        return "";
    }

    public static String engraveName(String kind) {
        return switch (kind) {
            case "TITAN" -> "대지 각인";
            case "LICH" -> "망령 각인";
            case "FROST" -> "서리 각인";
            case "BLOOD" -> "핏빛 각인";
            default -> "";
        };
    }
    public static final int MAT_X = 10, MAT_Y = 92;
    public static final int SLOT_X = 20, SLOT_Y = 40;

    private final SimpleContainer input = new SimpleContainer(2) {
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
        addSlot(new Slot(input, 1, MAT_X, MAT_Y));
        addStandardInventorySlots(inv, 38, 120);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new EnhanceMenu(id, inv, pos),
                Component.translatable("container.wildplus.enhance")), buf -> {});
    }

    public ItemStack item() {
        return slots.get(0).getItem();
    }

    public ItemStack material() {
        return slots.get(1).getItem();
    }

    /** 이 장비에 지금 할 수 있는 특별 강화 버튼 (스텔라 루비 승급 · 고정 피해 강화) */
    public static java.util.List<Integer> specials(ItemStack stack) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        if (stack.isEmpty()) return out;
        if (com.snolred.wildplus.registry.ModItems9.upgrades().containsKey(stack.getItem())) out.add(BTN_STAR);
        if (stack.is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_SWORD.get())
                && com.snolred.wildplus.event.RubyGear.trueLevel(stack) < 4) out.add(BTN_TRUE_DMG);
        if (com.snolred.wildplus.event.RubyGear.isStar(stack) && com.snolred.wildplus.event.RubyGear.engraving(stack).isEmpty()) out.add(BTN_ENGRAVE);
        if (com.snolred.wildplus.event.RubyGear.lifestealable(stack)
                && com.snolred.wildplus.event.RubyGear.lifesteal(stack) < LIFESTEAL_MAX) out.add(BTN_LIFESTEAL);
        return out;
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        if (!(who instanceof ServerPlayer sp)) return false;
        ItemStack stack = input.getItem(0);
        if (stack.isEmpty()) return false;
        if (id == BTN_STAR) {
            net.minecraft.world.item.Item to = com.snolred.wildplus.registry.ModItems9.upgrades().get(stack.getItem());
            if (to == null) return true;
            net.minecraft.world.item.Item need = com.snolred.wildplus.registry.ModItems9.upgradeMaterial(stack.getItem());
            if (!input.getItem(1).is(need)) {
                sp.sendOverlayMessage(Component.literal("아래 재료 칸에 " + need.getDefaultInstance().getHoverName().getString() + "을(를) 넣으세요").withStyle(ChatFormatting.RED));
                return true;
            }
            if (!Coins.pay(sp, STAR_COST)) {
                sp.sendOverlayMessage(Component.translatable("message.wildplus.enhance.no_money", STAR_COST).withStyle(ChatFormatting.RED));
                return true;
            }
            input.getItem(1).shrink(1);
            ItemStack star = stack.transmuteCopy(to, 1);
            star.setDamageValue(0);
            input.setItem(0, star);
            input.setChanged();
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.2F);
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            sp.sendSystemMessage(Component.literal(need == net.minecraft.world.item.Items.NETHER_STAR ? "✦ 스텔라 루비로 승급했어요! "
                    : need == com.snolred.wildplus.registry.ModItems12.LIFE_CRYSTAL.get() ? "✦ 로열 에메랄드로 승급했어요! " : "✦ 스카이 사파이어로 승급했어요! ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                    .append(star.getHoverName().copy().withStyle(ChatFormatting.GOLD)));
            return true;
        }
        if (id == BTN_ENGRAVE) {
            String kind = engraveKind(input.getItem(1));
            if (!com.snolred.wildplus.event.RubyGear.isStar(stack) || !com.snolred.wildplus.event.RubyGear.engraving(stack).isEmpty()) return true;
            if (kind.isEmpty()) {
                sp.sendOverlayMessage(Component.literal("재료 칸에 레이드 전리품이나 핏빛 군주의 심장을 넣으세요")
                        .withStyle(ChatFormatting.RED));
                return true;
            }
            if (!Coins.pay(sp, ENGRAVE_COST)) {
                sp.sendOverlayMessage(Component.translatable("message.wildplus.enhance.no_money", ENGRAVE_COST).withStyle(ChatFormatting.RED));
                return true;
            }
            input.getItem(1).shrink(1);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putString(com.snolred.wildplus.event.RubyGear.ENGRAVE, kind));
            input.setChanged();
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
            sp.sendSystemMessage(Component.literal("✦ " + engraveName(kind) + "을(를) 새겼어요!").withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        if (id == BTN_LIFESTEAL) {
            if (!com.snolred.wildplus.event.RubyGear.lifestealable(stack)) return true;
            int lv = com.snolred.wildplus.event.RubyGear.lifesteal(stack);
            if (lv >= LIFESTEAL_MAX) return true;
            ItemStack mat = input.getItem(1);
            if (!mat.is(com.snolred.wildplus.registry.ModItems.BLOODSTEEL_INGOT.get()) || mat.getCount() < LIFESTEAL_COST) {
                sp.sendOverlayMessage(Component.literal("재료 칸에 핏빛 강철 주괴 " + LIFESTEAL_COST + "개를 넣으세요").withStyle(ChatFormatting.RED));
                return true;
            }
            mat.shrink(LIFESTEAL_COST);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putInt(com.snolred.wildplus.event.RubyGear.LIFESTEAL, lv + 1));
            input.setChanged();
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.BLOCKS, 0.4F, 1.6F);
            sp.sendSystemMessage(Component.literal(String.format("♥ 흡혈 %d%% → %d%%", lv, lv + 1)).withStyle(ChatFormatting.DARK_RED));
            return true;
        }
        if (id == BTN_TRUE_DMG) {
            if (!stack.is(com.snolred.wildplus.registry.ModItems9.STAR_RUBY_SWORD.get())) return true;
            int lv = com.snolred.wildplus.event.RubyGear.trueLevel(stack);
            if (lv >= 4) return true;
            if (!Coins.pay(sp, TRUE_DMG_COST)) {
                sp.sendOverlayMessage(Component.translatable("message.wildplus.enhance.no_money", TRUE_DMG_COST).withStyle(ChatFormatting.RED));
                return true;
            }
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putInt(com.snolred.wildplus.event.RubyGear.TRUE_DMG, lv + 1));
            input.setChanged();
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.4F);
            sp.sendSystemMessage(Component.literal(String.format("스텔라 루비 검 고정 피해 %.1f → %.1f", 4 + 0.5 * lv, 4 + 0.5 * (lv + 1)))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
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
        if (index <= 1) {
            if (!moveItemStackTo(s, 2, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (s.is(net.minecraft.world.item.Items.NETHER_STAR) || s.is(com.snolred.wildplus.registry.ModItems11.DRAGON_PLUME.get()) || s.is(com.snolred.wildplus.registry.ModItems12.LIFE_CRYSTAL.get())
                || s.is(com.snolred.wildplus.registry.ModItems.BLOODSTEEL_INGOT.get()) || !engraveKind(s).isEmpty()) {
            if (!moveItemStackTo(s, 1, 2, false)) return ItemStack.EMPTY;
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

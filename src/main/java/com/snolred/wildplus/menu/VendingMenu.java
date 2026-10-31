package com.snolred.wildplus.menu;

import com.snolred.wildplus.block.VendingMachineBlock;
import com.snolred.wildplus.block.VendingMachineBlockEntity;
import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModBlocks;
import com.snolred.wildplus.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 자판기 화면.
 * 손님: 동전을 투입구에 넣고(또는 [동전 넣기]) 상품 버튼을 누르면 꺼내는 곳으로 상품이 나온다. 닫으면 남은 돈 반환.
 * 주인: 진열 칸에 직접 상품을 넣고 빼며, [관리]에서 칸을 고른 뒤 +/- 로 가격을 정하고 수익을 회수한다.
 */
public class VendingMenu extends AbstractContainerMenu {
    // ── 버튼 번호 ──
    public static final int BTN_BUY = 0;          // 0..8
    public static final int BTN_REFUND = 10;
    public static final int BTN_INSERT_ALL = 11;
    public static final int BTN_MANAGE = 12;
    public static final int BTN_SELECT = 20;      // 20..28
    public static final int BTN_PRICE = 30;       // 30..35 : +1 +10 +100 -1 -10 -100
    public static final int BTN_COLLECT = 40;
    public static final int[] PRICE_STEPS = {1, 10, 100, -1, -10, -100};

    // ── 동기화 데이터 번호 ──
    public static final int D_PRICE = 0;          // 0..8
    public static final int D_CREDIT_LO = 9, D_CREDIT_HI = 10;
    public static final int D_FLAGS = 11;         // 1 주인, 2 무한 재고, 4 관리 모드
    public static final int D_SELECTED = 12;      // 선택 칸 + 1 (0 = 없음)
    public static final int D_EARN_LO = 13, D_EARN_HI = 14;
    public static final int DATA_COUNT = 15;

    public static final int SLOT_COIN = 9;
    public static final int SLOT_TRAY = 10;
    public static final int PLAYER_START = 11;

    private final @Nullable VendingMachineBlockEntity machine;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final SimpleContainer coinSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            VendingMenu.this.slotsChanged(this);
        }
    };
    private final SimpleContainer tray = new SimpleContainer(1);
    private final Player player;

    // 서버 쪽 상태
    private long credit;
    private boolean manageMode;
    private int selected = -1;
    private final boolean owner;

    /** 클라이언트 */
    public VendingMenu(int id, Inventory inv) {
        this(id, inv, null, false, new SimpleContainer(VendingMachineBlockEntity.SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    /** 서버 */
    public VendingMenu(int id, Inventory inv, VendingMachineBlockEntity be, boolean owner) {
        this(id, inv, be, owner, be.items(), null);
    }

    private VendingMenu(int id, Inventory inv, @Nullable VendingMachineBlockEntity be, boolean owner,
                        Container products, @Nullable ContainerData clientData) {
        super(ModMenus.VENDING.get(), id);
        this.machine = be;
        this.owner = owner;
        this.player = inv.player;
        this.access = be == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = clientData != null ? clientData : new ServerData();

        for (int i = 0; i < VendingMachineBlockEntity.SLOTS; i++) {
            addSlot(new ProductSlot(products, i, 12 + (i % 3) * 34, 20 + (i / 3) * 32));
        }
        addSlot(new Slot(coinSlot, 0, 142, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Coins.unitValue(stack) > 0;
            }
        });
        addSlot(new Slot(tray, 0, 142, 98) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inv, 8, 140);
        addDataSlots(data);
    }

    // ───────── 클라이언트가 읽는 값 ─────────
    public int price(int slot) { return data.get(D_PRICE + slot); }
    public long credit() { return data.get(D_CREDIT_LO) + data.get(D_CREDIT_HI) * 10000L; }
    public long earnings() { return data.get(D_EARN_LO) + data.get(D_EARN_HI) * 10000L; }
    public boolean isOwner() { return (data.get(D_FLAGS) & 1) != 0; }
    public boolean isInfinite() { return (data.get(D_FLAGS) & 2) != 0; }
    public boolean isManaging() { return (data.get(D_FLAGS) & 4) != 0; }
    public int selected() { return data.get(D_SELECTED) - 1; }
    public boolean productEmpty(int slot) { return slots.get(slot).getItem().isEmpty(); }

    // ───────── 동전 투입 ─────────
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == coinSlot && machine != null) {
            ItemStack coins = coinSlot.getItem(0);
            int unit = Coins.unitValue(coins);
            if (unit > 0) {
                credit += (long) unit * coins.getCount();
                coinSlot.setItem(0, ItemStack.EMPTY, false);
                playSound(SoundEvents.CHAIN_PLACE, 1.6F);
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (machine == null || !(p.level() instanceof ServerLevel)) {
            return false;
        }
        if (id >= BTN_BUY && id < BTN_BUY + 9) {
            if (manageMode && owner) {
                selected = id - BTN_BUY;
                return true;
            }
            return buy(id - BTN_BUY);
        }
        switch (id) {
            case BTN_REFUND -> {
                refund();
                return true;
            }
            case BTN_INSERT_ALL -> {
                insertAllCoins();
                return true;
            }
            case BTN_MANAGE -> {
                if (!owner) return false;
                manageMode = !manageMode;
                if (selected < 0) selected = 0;
                return true;
            }
            case BTN_COLLECT -> {
                if (!owner) return false;
                long e = machine.takeEarnings();
                if (e > 0 && p instanceof net.minecraft.server.level.ServerPlayer sp) {
                    long got = com.snolred.wildplus.economy.Tax.collect(sp.level().getServer(), sp.getUUID(), e);
                    Coins.refund(p, got);
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.wildplus.tax.income",
                            e, e - got, got).withStyle(net.minecraft.ChatFormatting.GOLD));
                    playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F);
                }
                return true;
            }
            default -> {
            }
        }
        if (owner && id >= BTN_PRICE && id < BTN_PRICE + PRICE_STEPS.length && selected >= 0) {
            machine.setPrice(selected, machine.price(selected) + PRICE_STEPS[id - BTN_PRICE]);
            return true;
        }
        return false;
    }

    private boolean buy(int slot) {
        if (machine.items().getItem(slot).isEmpty()) {
            playSound(SoundEvents.VILLAGER_NO, 1.4F);
            return false;
        }
        int price = machine.price(slot);
        if (credit < price) {
            playSound(SoundEvents.VILLAGER_NO, 1.4F);
            return false;
        }
        ItemStack product = machine.sellOne(slot);
        if (product.isEmpty()) {
            return false;
        }
        credit -= price;
        ItemStack inTray = tray.getItem(0);
        if (inTray.isEmpty()) {
            tray.setItem(0, product);
        } else if (ItemStack.isSameItemSameComponents(inTray, product) && inTray.getCount() < inTray.getMaxStackSize()) {
            int move = Math.min(product.getCount(), inTray.getMaxStackSize() - inTray.getCount());
            inTray.grow(move);
            product.shrink(move);
            tray.setChanged();
            if (!product.isEmpty()) dispenseInFront(product);
        } else {
            dispenseInFront(product);
        }
        playSound(SoundEvents.CRAFTER_CRAFT, 1.0F);
        return true;
    }

    /** 꺼내는 곳이 차 있으면 자판기 앞으로 툭 떨어뜨린다 */
    private void dispenseInFront(ItemStack stack) {
        BlockPos pos = machine.getBlockPos();
        Direction facing = machine.getBlockState().getValue(VendingMachineBlock.FACING);
        Containers.dropItemStack(machine.getLevel(), pos.getX() + 0.5 + facing.getStepX() * 0.7, pos.getY() + 0.3,
                pos.getZ() + 0.5 + facing.getStepZ() * 0.7, stack);
    }

    private void insertAllCoins() {
        Inventory inv = player.getInventory();
        long added = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            int unit = Coins.unitValue(s);
            if (unit > 0) {
                added += (long) unit * s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            } else if (Coins.isPouch(s)) {
                added += Coins.pouchValue(s);
                Coins.setPouchValue(s, 0);
            }
        }
        if (added > 0) {
            credit += added;
            playSound(SoundEvents.CHAIN_PLACE, 1.6F);
        }
    }

    private void refund() {
        if (credit > 0) {
            Coins.refund(player, credit);
            credit = 0;
            playSound(SoundEvents.CHAIN_BREAK, 1.4F);
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        if (machine != null && machine.getLevel() != null) {
            machine.getLevel().playSound(null, machine.getBlockPos(), sound, SoundSource.BLOCKS, 0.7F, pitch);
        }
    }

    private void playSound(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float pitch) {
        playSound(sound.value(), pitch);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (machine != null && !p.level().isClientSide()) {
            refund();
            clearContainer(p, coinSlot);
            clearContainer(p, tray);
        }
    }

    @Override
    public boolean stillValid(Player p) {
        return machine == null || stillValid(access, p, ModBlocks.VENDING_MACHINE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (Coins.unitValue(stack) > 0) {
            if (!moveItemStackTo(stack, SLOT_COIN, SLOT_COIN + 1, false)) return ItemStack.EMPTY;
        } else if (isOwner() && !isInfinite()) {
            if (!moveItemStackTo(stack, 0, VendingMachineBlockEntity.SLOTS, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /** 진열 칸: 주인만 넣고 뺄 수 있다 */
    private final class ProductSlot extends Slot {
        ProductSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isOwner() && !isInfinite() && Coins.unitValue(stack) == 0;
        }

        @Override
        public boolean mayPickup(Player p) {
            return isOwner() && !isInfinite();
        }
    }

    /** 서버에서 화면으로 보내는 값 */
    private final class ServerData implements ContainerData {
        @Override
        public int get(int i) {
            if (i < 9) return machine.price(i);
            return switch (i) {
                case D_CREDIT_LO -> (int) (credit % 10000);
                case D_CREDIT_HI -> (int) Math.min(credit / 10000, 30000);
                case D_FLAGS -> (owner ? 1 : 0) | (machine.infinite() ? 2 : 0) | (manageMode ? 4 : 0);
                case D_SELECTED -> selected + 1;
                case D_EARN_LO -> (int) (machine.earnings() % 10000);
                case D_EARN_HI -> (int) Math.min(machine.earnings() / 10000, 30000);
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }
}

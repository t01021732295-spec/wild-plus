package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 필터 호퍼: 5칸 + 필터 견본 9칸. 8틱마다 아이템 하나를 옮긴다 (보통 호퍼와 같은 속도). */
public class FilterHopperBlockEntity extends BlockEntity implements Hopper, net.minecraft.world.WorldlyContainer {
    public static final int SIZE = 5, FILTERS = 9;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    public final SimpleContainer filters = new SimpleContainer(FILTERS) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            FilterHopperBlockEntity.this.setChanged();
        }
    };
    private int cooldown;

    public FilterHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_HOPPER.get(), pos, state);
    }

    public boolean matches(ItemStack s) {
        for (int i = 0; i < FILTERS; i++) {
            ItemStack f = filters.getItem(i);
            if (!f.isEmpty() && ItemStack.isSameItem(f, s)) return true;
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FilterHopperBlockEntity be) {
        if (--be.cooldown > 0) return;
        be.cooldown = 8;
        boolean moved = be.push(level, pos, state);
        if (!be.isFull()) moved |= HopperBlockEntity.suckInItems(level, be);
        if (moved) be.setChanged();
    }

    private boolean isFull() {
        for (ItemStack s : items) if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) return false;
        return true;
    }

    /** 칸의 아이템 하나를: 필터에 걸리면 좌우로, 아니면 바라보는 쪽으로 */
    private boolean push(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FilterHopperBlock.FACING);
        List<Direction> sides = facing == Direction.DOWN
                ? List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
                : List.of(facing.getClockWise(), facing.getCounterClockWise());
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) continue;
            if (matches(s)) {
                for (Direction d : sides) if (moveOne(level, pos, i, d)) return true;
            } else if (moveOne(level, pos, i, facing)) {
                return true;
            }
        }
        return false;
    }

    private boolean moveOne(Level level, BlockPos pos, int slot, Direction dir) {
        Container target = HopperBlockEntity.getContainerAt(level, pos.relative(dir));
        if (target == null || target == this) return false;
        ItemStack one = items.get(slot).copyWithCount(1);
        ItemStack left = HopperBlockEntity.addItem(this, target, one, dir.getOpposite());
        if (left.isEmpty()) {
            items.get(slot).shrink(1);
            target.setChanged();
            return true;
        }
        return false;
    }

    // ───────── 면별 출입: 필터 아이템은 옆으로만 나가야 하므로 아래(다른 호퍼 · 광차)에서 빼갈 수 없다 ─────────
    private static final int[] ALL = {0, 1, 2, 3, 4};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @org.jspecify.annotations.Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return !matches(stack);
    }

    // ───────── Hopper / Container ─────────
    @Override
    public double getLevelX() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double getLevelY() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double getLevelZ() {
        return worldPosition.getZ() + 0.5;
    }

    @Override
    public boolean isGridAligned() {
        return true;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items.stream().map(ItemStack::copy).toList());
        out.store("filters", ItemStack.OPTIONAL_CODEC.listOf(), filters.getItems().stream().map(ItemStack::copy).toList());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        List<ItemStack> list = in.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SIZE; i++) items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        List<ItemStack> f = in.read("filters", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < FILTERS; i++) filters.getItems().set(i, i < f.size() ? f.get(i) : ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            Containers.dropContents(server, pos, items);
            Containers.dropContents(server, pos, filters);
        }
    }
}

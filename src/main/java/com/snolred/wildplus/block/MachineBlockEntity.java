package com.snolred.wildplus.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * 자동화 기계 공통: 입력 칸 + 결과 칸. 호퍼는 입력 칸에만 넣고(조건에 맞는 것만), 결과 칸에서만 뺀다.
 */
public abstract class MachineBlockEntity extends BlockEntity implements WorldlyContainer {
    protected final NonNullList<ItemStack> items;
    public final int inputSlots;
    private final int[] allSlots;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputSlots, int outputSlots) {
        super(type, pos, state);
        this.inputSlots = inputSlots;
        this.items = NonNullList.withSize(inputSlots + outputSlots, ItemStack.EMPTY);
        this.allSlots = java.util.stream.IntStream.range(0, inputSlots + outputSlots).toArray();
    }

    /** 이 물건을 입력 칸에 넣을 수 있는지 */
    public abstract boolean isValidInput(ItemStack stack);

    /** 결과 칸들에 물건을 넣는다. 다 못 넣으면 남은 것을 돌려준다 */
    public ItemStack addOutput(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = inputSlots; i < items.size() && !rest.isEmpty(); i++) {
            ItemStack here = items.get(i);
            if (here.isEmpty()) {
                int n = Math.min(rest.getCount(), rest.getMaxStackSize());
                items.set(i, rest.split(n));
            } else if (ItemStack.isSameItemSameComponents(here, rest) && here.getCount() < here.getMaxStackSize()) {
                int n = Math.min(rest.getCount(), here.getMaxStackSize() - here.getCount());
                here.grow(n);
                rest.shrink(n);
            }
        }
        setChanged();
        return rest;
    }

    /** 결과 칸에 이 물건들이 다 들어갈 자리가 있는지 (대략: 빈 칸 수로 판단) */
    public boolean hasRoomFor(List<ItemStack> stacks) {
        int free = 0;
        for (int i = inputSlots; i < items.size(); i++) if (items.get(i).isEmpty()) free++;
        return free >= Math.min(stacks.size(), 2) || stacks.isEmpty();
    }

    public boolean outputFull() {
        for (int i = inputSlots; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) return false;
        }
        return true;
    }

    // ───────── Container ─────────
    @Override
    public int[] getSlotsForFace(Direction side) {
        return allSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < inputSlots && isValidInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= inputSlots;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < inputSlots && isValidInput(stack);
    }

    @Override
    public int getContainerSize() {
        return items.size();
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
        ItemStack r = Containers.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return Containers.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", ItemStack.OPTIONAL_CODEC.listOf(), items.stream().map(ItemStack::copy).toList());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        List<ItemStack> list = in.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < items.size(); i++) items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) Containers.dropContents(server, pos, items);
    }
}

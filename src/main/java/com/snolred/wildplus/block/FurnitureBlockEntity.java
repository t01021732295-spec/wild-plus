package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 수납 가구 (옷장 54칸 · 냉장고 27칸) 보관함 */
public class FurnitureBlockEntity extends BlockEntity implements Container {
    private NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);

    public FurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNITURE.get(), pos, state);
    }

    public int size() {
        return getBlockState().getBlock() instanceof FurnitureBlock f ? f.kind.storage : 27;
    }

    @Override
    public int getContainerSize() {
        return size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < size(); i++) if (!items.get(i).isEmpty()) return false;
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
        return Container.stillValidBlockEntity(this, player, 8.0F);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) Containers.dropContents(server, pos, items);
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
}

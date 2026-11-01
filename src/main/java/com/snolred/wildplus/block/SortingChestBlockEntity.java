package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 정렬 상자: 넣은 아이템을 반경 8칸 안의 상자·통 중 "같은 아이템이 이미 들어 있는" 곳으로 알아서 옮긴다.
 * 맞는 상자가 없는 아이템은 그대로 남는다. 1초마다 한 칸씩 처리.
 */
public class SortingChestBlockEntity extends BoxBlockEntity {
    public static final int SIZE = 27, RADIUS = 8;

    public SortingChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTING_CHEST.get(), pos, state, SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SortingChestBlockEntity be) {
        if (level.getGameTime() % 20 != 3 || be.isEmpty()) return;
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = be.items.get(i);
            if (s.isEmpty()) continue;
            Container target = be.findTarget(level, pos, s);
            if (target == null) continue;
            ItemStack left = HopperBlockEntity.addItem(be, target, s.copy(), Direction.UP);
            if (left.getCount() != s.getCount()) {
                be.items.set(i, left);
                be.setChanged();
                target.setChanged();
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0);
                }
            }
        }
    }

    private Container findTarget(Level level, BlockPos pos, ItemStack stack) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -3, -RADIUS), pos.offset(RADIUS, 3, RADIUS))) {
            if (p.equals(pos)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)
                    && !(be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity)) continue;
            Container c = HopperBlockEntity.getContainerAt(level, p.immutable());
            if (c == null) continue;
            boolean has = false, room = false;
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (s.isEmpty()) room = true;
                else if (ItemStack.isSameItemSameComponents(s, stack)) {
                    has = true;
                    if (s.getCount() < s.getMaxStackSize()) room = true;
                }
            }
            if (has && room) return c;
        }
        return null;
    }
}

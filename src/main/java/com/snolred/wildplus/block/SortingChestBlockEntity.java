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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * 정렬 상자: 넣은 아이템을 반경 8칸 안의 상자·통으로 알아서 옮긴다. 1초마다 처리.
 * 받을 상자를 고르는 순서:
 *  1. 지정 상자 — 상자 겉에 붙인 아이템 액자에 그 아이템이 끼워져 있으면 (비어 있어도 받음)
 *  2. 같은 아이템이 이미 들어 있는 상자
 *  3. 나머지 상자 — 액자에 정렬 상자 아이템을 끼운 상자 (어디에도 안 맞는 아이템 전부)
 */
public class SortingChestBlockEntity extends BoxBlockEntity {
    public static final int SIZE = 27, RADIUS = 8;

    public SortingChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTING_CHEST.get(), pos, state, SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SortingChestBlockEntity be) {
        if (level.getGameTime() % 20 != 3 || be.isEmpty()) return;
        Map<BlockPos, List<ItemStack>> frames = be.frames(level, pos);
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = be.items.get(i);
            if (s.isEmpty()) continue;
            Container target = be.findTarget(level, pos, s, frames);
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

    /** 반경 안 아이템 액자 → 붙어 있는 블록 위치별 끼운 아이템 */
    private Map<BlockPos, List<ItemStack>> frames(Level level, BlockPos pos) {
        Map<BlockPos, List<ItemStack>> out = new HashMap<>();
        AABB box = new AABB(pos).inflate(RADIUS + 1, 4, RADIUS + 1);
        for (ItemFrame f : level.getEntitiesOfClass(ItemFrame.class, box)) {
            ItemStack shown = f.getItem();
            if (shown.isEmpty()) continue;
            BlockPos on = f.getPos().relative(f.getDirection().getOpposite());
            out.computeIfAbsent(on, k -> new ArrayList<>()).add(shown);
        }
        return out;
    }

    private static boolean isStorage(BlockEntity be) {
        return be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity
                || be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity;
    }

    /** 이 상자(겹상자면 두 칸 모두)에 붙은 액자 아이템 */
    private static List<ItemStack> framesOf(Level level, BlockPos p, Map<BlockPos, List<ItemStack>> frames) {
        List<ItemStack> list = new ArrayList<>(frames.getOrDefault(p, List.of()));
        BlockState st = level.getBlockState(p);
        if (st.getBlock() instanceof ChestBlock && st.hasProperty(ChestBlock.TYPE) && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            list.addAll(frames.getOrDefault(p.relative(ChestBlock.getConnectedDirection(st)), List.of()));
        }
        return list;
    }

    private static boolean room(Container c, ItemStack stack) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) return true;
        }
        return false;
    }

    private @Nullable Container findTarget(Level level, BlockPos pos, ItemStack stack, Map<BlockPos, List<ItemStack>> frames) {
        Container holds = null, rest = null;
        Item restMark = getBlockState().getBlock().asItem();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -3, -RADIUS), pos.offset(RADIUS, 3, RADIUS))) {
            if (p.equals(pos)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!isStorage(be)) continue;
            BlockPos at = p.immutable();
            Container c = HopperBlockEntity.getContainerAt(level, at);
            if (c == null || !room(c, stack)) continue;
            List<ItemStack> marks = framesOf(level, at, frames);
            boolean marked = false;
            for (ItemStack m : marks) {
                if (ItemStack.isSameItem(m, stack)) return c; // 1. 지정 상자
                if (m.is(restMark)) marked = true;
            }
            if (marked) {
                if (rest == null) rest = c;
                continue;
            }
            if (holds == null && marks.isEmpty()) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    if (ItemStack.isSameItemSameComponents(c.getItem(i), stack)) {
                        holds = c;
                        break;
                    }
                }
            }
        }
        return holds != null ? holds : rest;
    }
}

package com.snolred.wildplus.block;

import com.snolred.wildplus.content.Soil;
import com.snolred.wildplus.item.FertilizerItem;
import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 비료 살포기 (9칸): 넣어 둔 비료를 주변 9×9 경작지(높이 ±2)에 3초마다 한 칸씩 뿌린다.
 * 비료가 없거나 다 떨어진 칸, 더 낮은 단계 비료가 든 칸부터 채운다. 가장 좋은 비료부터 쓴다. 호퍼로 채울 수 있다.
 */
public class SpreaderBlockEntity extends BoxBlockEntity {
    public static final int SIZE = 9, RADIUS = 4;
    private int cursor;

    public SpreaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPREADER.get(), pos, state, SIZE);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof FertilizerItem;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpreaderBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || level.getGameTime() % 60 != 29 || be.isEmpty()) return;
        // 가장 좋은 비료 칸
        int best = -1, bestTier = 0;
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = be.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof FertilizerItem f && f.tier > bestTier) {
                best = i;
                bestTier = f.tier;
            }
        }
        if (best < 0) return;
        // 경작지를 차례로 훑으며 한 칸에 뿌림 (한 번에 최대 81칸 × 5층 확인)
        int side = RADIUS * 2 + 1, total = side * side;
        for (int n = 0; n < total; n++) {
            int idx = (be.cursor + n) % total;
            int dx = idx % side - RADIUS, dz = idx / side - RADIUS;
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos p = pos.offset(dx, dy, dz);
                if (!sl.getBlockState(p).is(Blocks.FARMLAND)) continue;
                if (Soil.fertilize(sl, p, bestTier)) {
                    be.items.get(best).shrink(1);
                    be.cursor = idx + 1;
                    be.setChanged();
                    sl.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.5F, 1.2F);
                    return;
                }
            }
        }
        be.cursor = 0;
    }
}

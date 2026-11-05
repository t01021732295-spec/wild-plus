package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 3.5 출발 게이트 (가로 4칸 · 높이 2): 놓이면 경주 길의 기준점으로 기억 (가운데 = 왼쪽 10칸, 게이트 방향이 긴 지름) */
public class StartingGateBlock extends BigBlock {
    public StartingGateBlock(Properties p) {
        super(p, null, -2, 1, 0, 0, 2, null);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel sl && !old.is(this)) com.snolred.wildplus.content.Derby.registerGate(sl, pos, state.getValue(FACING));
    }
}

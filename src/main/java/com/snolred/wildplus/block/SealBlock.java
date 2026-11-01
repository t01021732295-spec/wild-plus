package com.snolred.wildplus.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** 던전 봉인 벽돌: 서바이벌에서 부술 수 없다. gate=true 인 칸(봉인문)은 고대 수호자가 쓰러지면 사라진다. */
public class SealBlock extends Block {
    public static final BooleanProperty GATE = BooleanProperty.create("gate");

    public SealBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GATE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GATE);
    }
}

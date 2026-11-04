package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 가구 침대 (캐노피 침대 · 이층 침대): 바닐라 침대와 똑같이 잠자기 · 리스폰 지점 (1×2칸).
 * 발치 칸이 가구 모델 전체를 그리고 머리 칸은 안 보임. 침대 엔티티 그림(BedRenderer)은 쓰지 않음.
 */
public class FurnitureBedBlock extends BedBlock {
    private final boolean bunk;

    public FurnitureBedBlock(Properties p, boolean bunk) {
        super(DyeColor.RED, p);
        this.bunk = bunk;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private static final VoxelShape LOW = Block_box(0, 0, 0, 16, 9, 16), TALL = Block_box(0, 0, 0, 16, 16, 16);

    private static VoxelShape Block_box(double a, double b, double c, double d, double e, double f) {
        return net.minecraft.world.level.block.Block.box(a, b, c, d, e, f);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return bunk ? Shapes.block() : LOW;
    }

    public boolean isFoot(BlockState s) {
        return s.getValue(PART) == BedPart.FOOT;
    }
}

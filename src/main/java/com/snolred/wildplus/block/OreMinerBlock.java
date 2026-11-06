package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 3.4 광석 채굴기: 철골 데릭 탑 3D 모델. 캘 광물을 고르고 보관함이 차지 않았으면 작동 중(lit) → 드릴 회전 · 컨베이어 · 경광등 */
public class OreMinerBlock extends MachineBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(2, 3, 2, 14, 16, 14), Block.box(4, 16, 4, 12, 30, 12));

    public OreMinerBlock(Properties properties) {
        super(properties, Kind.MINER);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** 강화 단계가 적힌 채굴기를 놓으면 그대로 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@org.jspecify.annotations.Nullable LivingEntity placer,
            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof OreMinerBlockEntity be) be.restoreTier(stack);
    }

    /** 작동 중: 드릴 구멍에서 돌가루 · 배기관 연기 */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.SMOKE, x + 0.38, y + 1.0, z - 0.4, 0, 0.05, 0);
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.CRIT, x + r.nextGaussian() * 0.12, y + 0.2, z + r.nextGaussian() * 0.12, 0, 0.08, 0);
    }
}

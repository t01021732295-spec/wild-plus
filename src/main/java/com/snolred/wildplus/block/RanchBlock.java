package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 3.4 목장 기계 (여러 칸 3D 블록 + RanchBlockEntity).
 * 건초 여물통 (2칸) · 둥지 상자 · 치즈 숙성통 · 버터 교반기 · 물레. 재료를 들고 우클릭하면 넣고, 빈손이면 결과물을 꺼낸다.
 */
public class RanchBlock extends BigBlock implements EntityBlock {
    public enum Kind {
        HAY_TROUGH(-1, 0, 1, "건초 여물통"),
        NEST_BOX(0, 0, 1, "둥지 상자"),
        CHEESE_BARREL(0, 0, 1, "치즈 숙성통"),
        BUTTER_CHURN(0, 0, 2, "버터 교반기"),
        SPINNING_WHEEL(0, 0, 2, "물레");

        public final int lat0, lat1, height;
        public final String ko;

        Kind(int lat0, int lat1, int height, String ko) {
            this.lat0 = lat0;
            this.lat1 = lat1;
            this.height = height;
            this.ko = ko;
        }
    }

    public final Kind kind;

    public RanchBlock(Properties p, Kind kind) {
        super(p, null, kind.lat0, kind.lat1, 0, 0, kind.height, null);
        this.kind = kind;
    }

    private static final VoxelShape LOW = Block.box(0, 0, 0, 16, 13, 16);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind == Kind.HAY_TROUGH || kind == Kind.NEST_BOX ? LOW : Shapes.block();
    }

    @Override
    public void open(Player player, BlockPos masterPos) {
        if (player instanceof ServerPlayer sp && sp.level().getBlockEntity(masterPos) instanceof RanchBlockEntity be) be.use(sp);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RanchBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.RANCH.get()) return null;
        return (l, p, s, be) -> RanchBlockEntity.serverTick(l, p, s, (RanchBlockEntity) be);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (kind == Kind.CHEESE_BARREL && r.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.5 + r.nextGaussian() * 0.2, pos.getY() + 1.15, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}

package com.snolred.wildplus.block;

import com.snolred.wildplus.cooking.CookingData.Station;
import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 조리 기구: 여러 칸 큰 블록(BigBlock) + 보관함 · 조리(CookingStationBlockEntity). 우클릭하면 조리 화면 */
public class CookingStationBlock extends BigBlock implements EntityBlock {
    public final Station station;

    public CookingStationBlock(Properties properties, Station station, int lat0, int lat1, int dep0, int dep1, int height) {
        super(properties, com.snolred.wildplus.menu.CookingMenu::open, lat0, lat1, dep0, dep1, height, null);
        this.station = station;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CookingStationBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.COOKING.get()) return null;
        return (l, p, s, be) -> CookingStationBlockEntity.serverTick(l, p, s, (CookingStationBlockEntity) be);
    }

    /** 굴뚝 · 김 · 불꽃 (조리 중 아니어도 은은하게) */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        switch (station) {
            case POT -> {
                if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.CLOUD, x + 0.5 + r.nextGaussian() * 0.2, y + 1.75, z, 0, 0.04, 0);
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, x + 1.2, y + 2.1, z, 0, 0.05, 0);
            }
            case OVEN -> {
                if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, y + 2.4, z + 0.5, 0, 0.04, 0);
                if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, x + 0.5 + r.nextGaussian() * 0.2, y + 0.75, z - 0.3, 0, 0.01, 0);
            }
            case GRILL -> {
                if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.SMOKE, x + 0.5 + r.nextGaussian() * 0.4, y + 1.3, z, 0, 0.04, 0);
                if (r.nextInt(5) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x + 0.5 + r.nextGaussian() * 0.4, y + 1.2, z, 0, 0.01, 0);
            }
            case STOVE -> {
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.CLOUD, x - 0.15, y + 1.2, z, 0, 0.03, 0);
            }
            default -> {}
        }
    }
}

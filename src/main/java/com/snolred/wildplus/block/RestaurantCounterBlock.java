package com.snolred.wildplus.block;

import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 식당 카운터 (가로 3칸 · 높이 2칸): 우클릭하면 식당 화면 (주방 · 손님 · 관리) */
public class RestaurantCounterBlock extends BigBlock implements EntityBlock {
    public RestaurantCounterBlock(Properties properties) {
        super(properties, com.snolred.wildplus.menu.RestaurantMenu::open, -1, 1, 0, 0, 2, null);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RestaurantCounterBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.RESTAURANT.get()) return null;
        return (l, p, s, be) -> RestaurantCounterBlockEntity.serverTick(l, p, s, (RestaurantCounterBlockEntity) be);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof RestaurantCounterBlockEntity be) {
            be.owner = p.getUUID();
            be.ownerName = p.getName().getString();
            var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
            if (custom != null) be.name = custom.getString();
            be.setChanged();
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(6) == 0) level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5 + r.nextGaussian() * 0.6, pos.getY() + 1.3,
                pos.getZ() + 0.5, 0, 0.02, 0);
    }
}

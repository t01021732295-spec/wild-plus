package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.Warps;
import com.snolred.wildplus.economy.WildData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

/**
 * 워프 포인트. 설치하면 이름이 붙은 워프가 되고(모루에서 이름을 바꿔 설치하면 그 이름),
 * 우클릭하면 다른 워프 포인트로 몇 번이든 순간이동할 수 있다.
 * 설치한 사람만 부수거나 공개/비공개를 바꿀 수 있다.
 */
public class WarpPointBlock extends BaseEntityBlock {
    public WarpPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpPointBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof WarpPointBlockEntity be) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            WildData.Warp w = Warps.register(player, server, pos, custom == null ? null : custom.getString());
            be.setWarpId(w.id);
            player.sendSystemMessage(Component.translatable("message.wildplus.warp.created", w.name).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof WarpPointBlockEntity be && !be.canBreak(player)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof WarpPointBlockEntity be) {
            Warps.openMenu(sp, Warps.ensure(server, pos, be));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.1,
                    pos.getZ() + random.nextDouble(), (random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.4);
        }
    }
}

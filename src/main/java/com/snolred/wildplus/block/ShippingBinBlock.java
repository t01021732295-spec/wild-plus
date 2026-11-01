package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.ShipPrices;
import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** 출하 상자 블록: 상자처럼 열리고(27칸), 매일 아침 내용물이 팔린다 */
public class ShippingBinBlock extends BaseEntityBlock {
    public ShippingBinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShippingBinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SHIPPING_BIN.get(), ShippingBinBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof ShippingBinBlockEntity be) {
            be.setOwner(p);
        }
    }

    /** 비공개 상자는 주인만 부술 수 있음 */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ShippingBinBlockEntity be && !be.isOwner(player) && !be.isPublic()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShippingBinBlockEntity be) {
            boolean owner = be.isOwner(player);
            if (owner && player.isSecondaryUseActive()) {
                be.togglePublic();
                player.sendOverlayMessage(Component.translatable(be.isPublic() ? "message.wildplus.shipping.public" : "message.wildplus.shipping.private")
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                return InteractionResult.SUCCESS;
            }
            if (!owner && !be.isPublic()) {
                player.sendOverlayMessage(Component.translatable("message.wildplus.shipping.locked").withStyle(net.minecraft.ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.snolred.wildplus.menu.ShippingMenu.open(sp, be);
        }
        return InteractionResult.SUCCESS;
    }
}

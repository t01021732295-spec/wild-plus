package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.Coins;
import com.snolred.wildplus.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 동전 자판기
 * 주인:
 *   - 웅크리고 물건을 들고 우클릭: 상품 보충
 *   - 웅크리고 동전을 들고 우클릭: 그 금액을 1개당 가격으로 설정 (동전은 그대로)
 *   - 빈손 우클릭: 수익 회수 + 상태 보기
 *   - 웅크리고 빈손 우클릭: 남은 재고 회수
 * 손님:
 *   - 우클릭: 1개 구매 / 웅크리고 우클릭: 최대 8개 구매 (동전·주머니에서 자동 결제)
 * 주인만 부술 수 있다.
 */
public class VendingMachineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public VendingMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VendingMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.VENDING_MACHINE.get(), VendingMachineBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof VendingMachineBlockEntity be) {
            be.setOwner(player);
        }
    }

    /** 주인이 아니면 부술 수 없음 */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof VendingMachineBlockEntity be && !be.canManage(player)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof VendingMachineBlockEntity be)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (be.canManage(player) && player.isSecondaryUseActive()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            int unit = Coins.unitValue(stack);
            if (unit > 0) {
                be.setPrice(unit * stack.getCount());
                msg(player, Component.translatable("message.wildplus.vending.price_set", Coins.describe(be.price())), ChatFormatting.GOLD);
                return InteractionResult.SUCCESS;
            }
            if (Coins.isPouch(stack)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            int added = be.addStock(stack);
            if (added == 0) {
                msg(player, Component.translatable("message.wildplus.vending.other_product", be.product().getHoverName()), ChatFormatting.RED);
            } else {
                msg(player, Component.translatable("message.wildplus.vending.stocked", be.product().getHoverName(), added,
                        be.infinite() ? "∞" : String.valueOf(be.stock())), ChatFormatting.GREEN);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return buy(be, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof VendingMachineBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (be.canManage(player) && be.hasOwner()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (player.isSecondaryUseActive()) {
                ItemStack back = be.takeAllStock();
                int n = back.getCount();
                giveItems(player, back);
                msg(player, Component.translatable("message.wildplus.vending.unstocked", n), ChatFormatting.YELLOW);
            } else {
                long e = be.takeEarnings();
                if (e > 0) {
                    Coins.give(player, e);
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
                }
                msg(player, Component.translatable("message.wildplus.vending.status_owner",
                        be.product().isEmpty() ? Component.translatable("message.wildplus.vending.nothing") : be.product().getHoverName(),
                        be.stock(), Coins.describe(be.price()), Coins.describe(e)), ChatFormatting.GOLD);
            }
            return InteractionResult.SUCCESS;
        }
        return buy(be, level, pos, player);
    }

    private static InteractionResult buy(VendingMachineBlockEntity be, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (be.product().isEmpty() || be.stock() <= 0) {
            msg(player, Component.translatable("message.wildplus.vending.sold_out"), ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }
        int want = player.isSecondaryUseActive() ? 8 : 1;
        want = Math.min(want, be.stock());
        long wallet = Coins.totalValue(player);
        int affordable = (int) Math.min(want, wallet / be.price());
        if (affordable <= 0) {
            msg(player, Component.translatable("message.wildplus.vending.no_money", be.product().getHoverName(),
                    Coins.describe(be.price()), Coins.describe(wallet)), ChatFormatting.RED);
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.4F);
            return InteractionResult.SUCCESS;
        }
        long cost = (long) affordable * be.price();
        if (!Coins.pay(player, cost)) {
            return InteractionResult.SUCCESS;
        }
        int sold = be.sell(affordable);
        giveItems(player, be.product().copyWithCount(sold));
        msg(player, Component.translatable("message.wildplus.vending.bought", be.product().getHoverName(), sold,
                Coins.describe(cost), Coins.describe(Coins.totalValue(player))), ChatFormatting.GREEN);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    private static void giveItems(Player player, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack part = stack.split(stack.getMaxStackSize());
            if (!player.getInventory().add(part) && !part.isEmpty()) {
                ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), part);
                drop.setNoPickUpDelay();
                player.level().addFreshEntity(drop);
            }
        }
    }

    private static void msg(Player player, Component text, ChatFormatting color) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendOverlayMessage(text.copy().withStyle(color));
        }
    }
}

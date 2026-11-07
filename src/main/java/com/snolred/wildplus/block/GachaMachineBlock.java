package com.snolred.wildplus.block;

import com.snolred.wildplus.WildConfig;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.economy.Coins;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 뽑기 기계: 우클릭하면 동전(기본 10동)을 내고 캡슐 하나를 뽑는다.
 * 희귀 이상이 나오면 대박 효과 + 주변에 알림.
 */
public class GachaMachineBlock extends HorizontalDirectionalBlock {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, WildPlus.id("gameplay/gacha"));
    private static final Map<UUID, Long> LAST_PULL = new HashMap<>();

    public GachaMachineBlock(Properties properties) {
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        long now = server.getGameTime();
        Long last = LAST_PULL.get(player.getUUID());
        if (last != null && now - last < 15 && now >= last) {
            return InteractionResult.SUCCESS;
        }
        int price = com.snolred.wildplus.economy.Prices.x(WildConfig.GACHA_PRICE.getAsInt());
        if (!Coins.pay(player, price)) {
            sp.sendOverlayMessage(Component.translatable("message.wildplus.gacha.no_money", Coins.describe(price),
                    Coins.describe(Coins.totalValue(player))).withStyle(ChatFormatting.RED));
            server.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.4F);
            return InteractionResult.SUCCESS;
        }
        LAST_PULL.put(player.getUUID(), now);
        LootTable table = server.getServer().reloadableRegistries().getLootTable(LOOT);
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        List<ItemStack> prizes = table.getRandomItems(params);
        Direction facing = state.getValue(FACING);
        boolean jackpot = false;
        for (ItemStack prize : prizes) {
            ItemEntity e = new ItemEntity(server, pos.getX() + 0.5 + facing.getStepX() * 0.6, pos.getY() + 0.4,
                    pos.getZ() + 0.5 + facing.getStepZ() * 0.6, prize);
            e.setDeltaMovement(facing.getStepX() * 0.15, 0.2, facing.getStepZ() * 0.15);
            e.setPickUpDelay(5);
            server.addFreshEntity(e);
            Rarity r = prize.getRarity();
            if (r == Rarity.RARE || r == Rarity.EPIC) {
                jackpot = true;
                Component msg = Component.translatable("message.wildplus.gacha.jackpot", player.getDisplayName(), prize.getHoverName())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                for (ServerPlayer near : server.players()) {
                    if (near.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) near.sendSystemMessage(msg);
                }
            }
        }
        server.playSound(null, pos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (jackpot) {
            server.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            server.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1.0F, 1.0F);
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    40, 0.4, 0.4, 0.4, 0.4);
        } else {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    6, 0.3, 0.2, 0.3, 0.0);
        }
        return InteractionResult.SUCCESS;
    }
}

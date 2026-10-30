package com.snolred.wildplus.event;

import com.snolred.wildplus.WildConfig;
import com.snolred.wildplus.block.GraveBlock;
import com.snolred.wildplus.block.GraveBlockEntity;
import com.snolred.wildplus.registry.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * 편의 기능
 * - 나무 한 번에 베기: 웅크린 채 도끼로 원목을 캐면 붙어 있는 원목 전체 (최대 128개)
 * - 광맥 한 번에 캐기: 웅크린 채 곡괭이로 광석을 캐면 붙어 있는 같은 광석 전체 (최대 48개)
 * - 우클릭 수확: 다 자란 작물을 우클릭하면 수확하고 자동으로 다시 심음
 * - 무덤: 죽으면 아이템이 무덤에 보관됨 + 사망 좌표 안내
 */
public final class QolEvents {
    private static boolean chaining;

    private QolEvents() {}

    // ───────── 나무 베기 · 광맥 캐기 ─────────
    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (chaining || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!player.isShiftKeyDown() || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        int limit;
        boolean tree;
        if (WildConfig.TREE_FELLING.getAsBoolean() && state.is(BlockTags.LOGS) && tool.is(ItemTags.AXES)) {
            limit = 128;
            tree = true;
        } else if (WildConfig.VEIN_MINING.getAsBoolean() && state.is(Tags.Blocks.ORES) && tool.is(ItemTags.PICKAXES)
                && tool.isCorrectToolForDrops(state)) {
            limit = 48;
            tree = false;
        } else {
            return;
        }
        List<BlockPos> targets = collect(level, event.getPos(), state, limit, tree);
        if (targets.isEmpty()) {
            return;
        }
        chaining = true;
        try {
            for (BlockPos p : targets) {
                if (player.getMainHandItem().isEmpty() || player.getMainHandItem().nextDamageWillBreak()) {
                    break; // 도구가 부서지기 직전이면 멈춤
                }
                player.gameMode.destroyBlock(p);
            }
        } finally {
            chaining = false;
        }
    }

    /** 처음 캔 블록과 이어진 같은 종류 블록 찾기 (나무는 원목 전체, 대각선 포함) */
    private static List<BlockPos> collect(ServerLevel level, BlockPos origin, BlockState originState, int limit, boolean tree) {
        Block block = originState.getBlock();
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && out.size() < limit) {
            BlockPos cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = cur.offset(dx, dy, dz);
                        if (!seen.add(n) || Math.abs(n.getX() - origin.getX()) > 12 || Math.abs(n.getZ() - origin.getZ()) > 12) {
                            continue;
                        }
                        BlockState s = level.getBlockState(n);
                        boolean match = tree ? s.is(BlockTags.LOGS) : s.is(block);
                        if (match) {
                            out.add(n);
                            queue.add(n);
                            if (out.size() >= limit) break;
                        }
                    }
                }
            }
        }
        // 위쪽부터 캐면 나무가 공중에 뜨지 않는다
        out.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        return out;
    }

    // ───────── 우클릭 수확 ─────────
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!WildConfig.RIGHT_CLICK_HARVEST.getAsBoolean() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return;
        }
        if (event.getEntity().isSecondaryUseActive() && !event.getItemStack().isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
        Item seed = state.getBlock().asItem();
        boolean seedTaken = false;
        for (ItemStack drop : drops) {
            if (!seedTaken && drop.is(seed)) {
                drop.shrink(1);
                seedTaken = true;
            }
            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }
        level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ───────── 무덤 ─────────
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !WildConfig.GRAVES.getAsBoolean() || event.isCanceled() || event.getDrops().isEmpty()) {
            return;
        }
        BlockPos pos = findGraveSpot(level, player.blockPosition());
        if (pos == null) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity e : event.getDrops()) {
            stacks.add(e.getItem().copy());
        }
        Direction facing = player.getDirection().getOpposite();
        level.setBlock(pos, ModBlocks.GRAVE.get().defaultBlockState().setValue(GraveBlock.FACING, facing), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
            grave.fill(player, stacks);
            event.getDrops().clear();
            player.sendSystemMessage(Component.translatable("message.wildplus.grave.created",
                    pos.getX(), pos.getY(), pos.getZ(), stacks.size()).withStyle(ChatFormatting.GOLD));
        }
    }

    private static BlockPos findGraveSpot(ServerLevel level, BlockPos death) {
        int minY = level.getMinY() + 1;
        int maxY = level.getMaxY() - 1;
        BlockPos start = new BlockPos(death.getX(), Math.max(minY, Math.min(maxY, death.getY())), death.getZ());
        for (int r = 0; r <= 4; r++) {
            for (int dy = 0; dy <= 8; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        BlockPos p = start.offset(dx, dy, dz);
                        if (p.getY() > maxY) continue;
                        BlockState s = level.getBlockState(p);
                        if (s.isAir() || s.canBeReplaced()) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlockPos p = player.blockPosition();
            player.sendSystemMessage(Component.translatable("message.wildplus.death_pos", p.getX(), p.getY(), p.getZ(),
                    player.level().dimension().identifier().toString()).withStyle(ChatFormatting.GRAY));
        }
    }
}

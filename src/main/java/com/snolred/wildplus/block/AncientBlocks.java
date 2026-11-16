package com.snolred.wildplus.block;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.event.Noise;
import com.snolred.wildplus.registry.ModBlocks23;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 5.3 고대 도시 구조물 블록.
 * · 공명 종(note 0~3): 누르면 그 음이 울림. 악보 석판이 정한 순서대로 4개를 울리면 근처 스컬크 봉인이 녹음 (틀리면 처음부터 + 소음 +10)
 * · 악보 석판: 누르면 이 서고의 순서를 보여 줌 (석판 위치로 정해짐)
 * · 성소 제단: 메아리 조각 4개 → 고대의 워든 (제단마다 게임 하루 1번)
 */
public final class AncientBlocks {
    private AncientBlocks() {}

    private static final float[] PITCH = {0.6F, 0.8F, 1.0F, 1.3F};
    private static final String[] NAME = {"낮은 음", "가운데 음", "높은 음", "가장 높은 음"};
    /** 석판 위치 → 지금까지 맞게 울린 수 */
    private static final Map<Long, Integer> PROGRESS = new HashMap<>();

    /** 석판 위치로 정해지는 순서 (0~3 의 순열) */
    public static int[] order(BlockPos tablet) {
        List<Integer> l = new ArrayList<>(List.of(0, 1, 2, 3));
        java.util.Collections.shuffle(l, new java.util.Random(tablet.asLong() * 31L + 5_303L));
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    private static BlockPos findTablet(Level level, BlockPos near) {
        for (BlockPos b : BlockPos.betweenClosed(near.offset(-12, -4, -12), near.offset(12, 4, 12)))
            if (level.getBlockState(b).is(ModBlocks23.SCORE_TABLET.get())) return b.immutable();
        return null;
    }

    public static class Bell extends Block {
        public static final IntegerProperty NOTE = IntegerProperty.create("note", 0, 3);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

        public Bell(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(NOTE, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(NOTE);
        }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player p, BlockHitResult hit) {
            if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            int note = state.getValue(NOTE);
            sl.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.4F, PITCH[note]);
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, PITCH[note]);
            sl.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, note / 4.0);
            BlockPos tablet = findTablet(sl, pos);
            if (tablet == null) return InteractionResult.SUCCESS;
            int[] ord = order(tablet);
            int done = PROGRESS.getOrDefault(tablet.asLong(), 0);
            if (ord[done] != note) {
                PROGRESS.remove(tablet.asLong());
                sl.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.0F, 1.0F);
                Noise.add(p, 10F);
                p.sendOverlayMessage(Component.literal("♪ 음이 틀렸어요… 처음부터 (소음 +10)").withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }
            done++;
            if (done < 4) {
                PROGRESS.put(tablet.asLong(), done);
                p.sendOverlayMessage(Component.literal("♪ " + done + " / 4").withStyle(ChatFormatting.AQUA));
                return InteractionResult.SUCCESS;
            }
            PROGRESS.remove(tablet.asLong());
            int n = 0;
            for (BlockPos b : BlockPos.betweenClosed(tablet.offset(-14, -4, -14), tablet.offset(14, 6, 14))) {
                if (!sl.getBlockState(b).is(ModBlocks23.ANCIENT_SEAL.get())) continue;
                sl.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
                n++;
            }
            sl.playSound(null, pos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 2.0F, 0.8F);
            for (ServerPlayer q : sl.players())
                if (q.blockPosition().closerThan(pos, 24))
                    q.sendSystemMessage(Component.literal(n > 0 ? "♪ 공명이 맞았어요! 봉인된 서고의 문이 녹아내렸어요" : "♪ 공명이 맞았지만 열릴 문이 없어요").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            return InteractionResult.SUCCESS;
        }
    }

    public static class Tablet extends Block {
        public Tablet(Properties p) {
            super(p);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player p, BlockHitResult hit) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            int[] o = order(pos);
            StringBuilder sb = new StringBuilder("📜 악보: ");
            for (int i = 0; i < 4; i++) sb.append(i > 0 ? " → " : "").append(NAME[o[i]]);
            p.sendSystemMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.AQUA));
            p.sendSystemMessage(Component.literal("   공명 종은 왼쪽부터 낮은 음 · 가운데 음 · 높은 음 · 가장 높은 음 (울려서 들어 보세요)").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
    }

    public static class Altar extends Block {
        public Altar(Properties p) {
            super(p);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
            if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            if (!sl.getEntitiesOfClass(Warden.class, new AABB(pos).inflate(40)).isEmpty()) {
                p.sendOverlayMessage(Component.literal("워든이 이미 깨어 있어요!").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            var d = ContentData.get(sl.getServer());
            var t = d.section("sanctum_altar");
            String key = Long.toString(pos.asLong());
            if (ContentData.gameDay < t.getLongOr(key, 0L)) {
                p.sendOverlayMessage(Component.literal("워든은 아직 잠들어 있어요 (내일 다시)").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (!stack.is(Items.ECHO_SHARD) || (stack.getCount() < 4 && !p.getAbilities().instabuild)) {
                p.sendOverlayMessage(Component.literal("메아리 조각 4개를 들고 제단을 누르면 고대의 워든이 깨어나요").withStyle(ChatFormatting.DARK_AQUA));
                return InteractionResult.FAIL;
            }
            if (!p.getAbilities().instabuild) stack.shrink(4);
            t.putLong(key, ContentData.gameDay + 1);
            d.dirty();
            Warden w = net.minecraft.world.entity.EntityTypes.WARDEN.create(sl, EntitySpawnReason.TRIGGERED);
            if (w != null) {
                w.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 180F, 0F);
                sl.addFreshEntity(w);
                w.increaseAngerAt(p, 80, false);
            }
            sl.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 120, 3, 2, 3, 0.05);
            sl.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 3F, 0.8F);
            for (ServerPlayer q : sl.players())
                if (q.blockPosition().closerThan(pos, 48))
                    q.sendSystemMessage(Component.literal("🔊 잠든 성소의 고대의 워든이 깨어났습니다!").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player p, BlockHitResult hit) {
            if (!level.isClientSide())
                p.sendOverlayMessage(Component.literal("메아리 조각 4개를 들고 제단을 누르면 고대의 워든이 깨어나요").withStyle(ChatFormatting.DARK_AQUA));
            return InteractionResult.SUCCESS;
        }
    }
}

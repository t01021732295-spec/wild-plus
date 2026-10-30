package com.snolred.wildplus.block;

import com.snolred.wildplus.economy.WildData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 청크로더: 놓인 청크(16×16)를 항상 불러와 둔다. 아무도 근처에 없어도 농장·화로·자판기 보충이 돌아간다.
 * 같은 청크에 여러 개 놓아도 되고, 마지막 하나를 부수면 풀린다.
 */
public class ChunkLoaderBlock extends BaseEntityBlock {
    public ChunkLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkLoaderBlockEntity(pos, state);
    }

    static String key(ServerLevel level, int cx, int cz) {
        return level.dimension().identifier() + "|" + cx + "|" + cz;
    }

    /** 이 청크의 청크로더 수를 바꾸고, 0↔1 이 될 때 강제 로드를 켜고 끈다 */
    static void change(ServerLevel level, BlockPos pos, int delta) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        WildData data = WildData.get(level.getServer());
        String k = key(level, cx, cz);
        int before = data.chunkLoaders.getOrDefault(k, 0);
        int after = Math.max(0, before + delta);
        if (after == 0) data.chunkLoaders.remove(k); else data.chunkLoaders.put(k, after);
        data.setDirty();
        com.snolred.wildplus.WildPlus.LOGGER.info("[WildPlus] 청크로더 {} → {}개", k, after);
        if (before == 0 && after > 0) level.setChunkForced(cx, cz, true);
        if (before > 0 && after == 0) level.setChunkForced(cx, cz, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && !level.isClientSide()) {
            p.sendSystemMessage(Component.translatable("message.wildplus.chunk_loader.on", pos.getX() >> 4, pos.getZ() >> 4)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 2, -random.nextDouble(), (random.nextDouble() - 0.5) * 2);
        }
    }
}

package com.snolred.wildplus.block;

import com.snolred.wildplus.event.KalpaCitadel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 5.1 겁화의 화로: 부싯돌·화염구로 불을 붙인다. 성채의 화로 4개가 다 켜지면 본성 문이 열림 (event.KalpaCitadel) */
public class KalpaBrazierBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

    public KalpaBrazierBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(LIT) || !(stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        sl.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        sl.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.2F, 0.7F);
        sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.3, 0.4, 0.3, 0.05);
        if (!p.getAbilities().instabuild) {
            if (stack.is(Items.FIRE_CHARGE)) stack.shrink(1);
            else stack.hurtAndBreak(1, p, hand);
        }
        KalpaCitadel.onLit(sl, pos, p);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) {
            if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0, 0.02, 0);
            return;
        }
        for (int i = 0; i < 3; i++)
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.8, pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.04, 0);
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0, 0);
    }
}

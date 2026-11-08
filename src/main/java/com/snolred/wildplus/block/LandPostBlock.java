package com.snolred.wildplus.block;

import com.snolred.wildplus.content.Land;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 3.9 땅 경계 기둥: 땅을 사면 땅 모서리에 저절로 선다 (주인마다 깃발 색 · 누르면 주인 이름) */
public class LandPostBlock extends Block {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 7);
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape BASE = Block.box(4, 0, 4, 12, 3, 12);

    public LandPostBlock(Properties p) {
        super(p);
        this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(COLOR);
    }

    /** 주인 색 (tools/data_v30.py POST_COLORS 와 같은 순서) */
    public static final int[] RGB = {0x46AA5A, 0x4678DC, 0xDC4646, 0xF0BE32, 0xA050D2, 0x28BEBE, 0xF08228, 0xEB6EAA};

    /** 기둥 위로 주인 색 빛기둥 (멀리서도 보이게 · 화면 쪽에서만 그림) */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource r) {
        int c = RGB[state.getValue(COLOR)];
        for (int k = 0; k < 4; k++) {
            level.addParticle(new net.minecraft.core.particles.DustParticleOptions(c, 2.5F), pos.getX() + 0.5 + (r.nextFloat() - 0.5F) * 0.3,
                    pos.getY() + 2.1 + r.nextFloat() * 8.0, pos.getZ() + 0.5 + (r.nextFloat() - 0.5F) * 0.3, 0, 0.03, 0);
        }
        if (r.nextFloat() < 0.4F) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 0, 0.06, 0);
        }
    }

    public static int colorOf(UUID owner) {
        return Math.floorMod(owner.hashCode(), 8);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return net.minecraft.world.phys.shapes.Shapes.or(SHAPE, BASE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            UUID o = Land.ownerAt(level, pos);
            String msg = o == null ? "🌲 주인 없는 땅의 기둥이에요"
                    : o.equals(sp.getUUID()) ? "🏠 내 땅 · /land 로 관리" : "🏠 " + Land.ownerNameOf(level.getServer(), o) + "의 땅";
            sp.sendOverlayMessage(Component.literal(msg).withStyle(o != null && o.equals(sp.getUUID()) ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }
}

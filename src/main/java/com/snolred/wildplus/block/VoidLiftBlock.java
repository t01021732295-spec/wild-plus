package com.snolred.wildplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 유적 부유 다리 아래 착지 섬: 밟으면 위로 띄워 줌 */
public class VoidLiftBlock extends Block {
    public VoidLiftBlock(Properties p) {
        super(p);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
        if (!level.isClientSide() && e instanceof Player p) p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 70, 5, true, false, true));
        super.stepOn(level, pos, state, e);
    }
}

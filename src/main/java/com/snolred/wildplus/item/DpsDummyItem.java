package com.snolred.wildplus.item;

import com.snolred.wildplus.entity.DpsDummy;
import com.snolred.wildplus.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** 5.0.10 DPS 허수아비 설치 */
public class DpsDummyItem extends Item {
    public DpsDummyItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty())
            return InteractionResult.FAIL;
        DpsDummy d = ModEntities.DPS_DUMMY.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (d == null) return InteractionResult.FAIL;
        Player p = ctx.getPlayer();
        float yaw = p != null ? Math.round((p.getYRot() + 180F) / 45F) * 45F : 0F;
        d.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0F);
        d.setYHeadRot(yaw);
        d.setYBodyRot(yaw);
        level.addFreshEntity(d);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1F, 0.9F);
        if (p == null || !p.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}

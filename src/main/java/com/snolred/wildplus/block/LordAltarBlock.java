package com.snolred.wildplus.block;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.entity.VoidLordEntity;
import com.snolred.wildplus.event.VoidBiome;
import com.snolred.wildplus.registry.ModItems27;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 공허 군주의 제단: 공허 수정 평원에서 군주의 인장을 쓰면 소환 (게임 속 7일에 1번) */
public class LordAltarBlock extends Block {
    public LordAltarBlock(Properties p) {
        super(p);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems27.LORD_SIGIL.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        if (!sl.getBiome(pos).is(VoidBiome.KEY)) {
            p.sendOverlayMessage(Component.literal("공허 수정 평원에서만 소환할 수 있어요").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        ContentData d = ContentData.get(sl.getServer());
        var s = d.section("void_lord");
        long ready = s.getLongOr("ready", 0L);
        if (ContentData.gameDay < ready) {
            p.sendOverlayMessage(Component.literal("군주는 아직 잠들어 있어요 (" + (ready - ContentData.gameDay) + "일 뒤)").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        s.putLong("ready", ContentData.gameDay + 7);
        d.dirty();
        if (!p.getAbilities().instabuild) stack.shrink(1);
        VoidLordEntity.summon(sl, pos);
        return InteractionResult.SUCCESS;
    }
}

package com.snolred.wildplus.block;

import com.snolred.wildplus.content.ContentData;
import com.snolred.wildplus.entity.KalpaLordEntity;
import com.snolred.wildplus.registry.ModItems34;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** 5.1 군주의 제단 (겁화의 성채 본성 가운데): 겁화석 3개를 바치면 겁화의 군주 소환. 제단마다 게임 속 2일에 1번 */
public class KalpaAltarBlock extends Block {
    public static final int COST = 3, DAYS = 2;

    public KalpaAltarBlock(Properties p) {
        super(p);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        if (!sl.getEntitiesOfClass(KalpaLordEntity.class, new AABB(pos).inflate(48)).isEmpty()) {
            p.sendOverlayMessage(Component.literal("군주가 이미 깨어 있어요!").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        ContentData d = ContentData.get(sl.getServer());
        var s = d.section("kalpa_lord");
        String key = Long.toString(pos.asLong());
        long ready = s.getLongOr(key, 0L);
        if (ContentData.gameDay < ready) {
            p.sendOverlayMessage(Component.literal("군주는 아직 잠들어 있어요 (" + (ready - ContentData.gameDay) + "일 뒤)").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!stack.is(ModItems34.KALPA_STONE.get()) || (stack.getCount() < COST && !p.getAbilities().instabuild)) {
            p.sendOverlayMessage(Component.literal("겁화석 " + COST + "개를 들고 제단을 누르면 겁화의 군주가 깨어나요").withStyle(ChatFormatting.GOLD));
            return InteractionResult.FAIL;
        }
        s.putLong(key, ContentData.gameDay + DAYS);
        d.dirty();
        if (!p.getAbilities().instabuild) stack.shrink(COST);
        KalpaLordEntity.summon(sl, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (!level.isClientSide())
            p.sendOverlayMessage(Component.literal("겁화석 " + COST + "개를 들고 제단을 누르면 겁화의 군주가 깨어나요").withStyle(ChatFormatting.GOLD));
        return InteractionResult.SUCCESS;
    }
}

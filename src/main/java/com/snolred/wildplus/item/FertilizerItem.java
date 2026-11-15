package com.snolred.wildplus.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 비료 (1 일반 · 2 고급 · 3 황금): 5.0.8 부터 쓰면 1분 동안 주변 9×9 작물 · 묘목이 빨리 자라는 구역이 생기고,
 * 구역의 경작지에 영양을 남긴다 → 수확할 때 품질이 붙는다 (Soil, 수확 3번까지).
 */
public class FertilizerItem extends Item {
    public final int tier;

    public FertilizerItem(Properties properties) {
        this(properties, 1);
    }

    public FertilizerItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    /** 5.0.8: 쓰면 1분 동안 주변 9×9 (위아래 1칸) 의 작물 · 묘목이 모두 빨리 자라고, 경작지 전부에 영양을 남긴다 (content.FertilizerZones) */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return InteractionResult.SUCCESS;
        BlockPos center = ctx.getClickedPos();
        if (!com.snolred.wildplus.content.FertilizerZones.start(sl, center, tier)) return InteractionResult.PASS;
        Player player = ctx.getPlayer();
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendOverlayMessage(net.minecraft.network.chat.Component.literal("🌱 1분 동안 주변 9×9 작물이 빨리 자라요").withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        level.playSound(null, center, net.minecraft.sounds.SoundEvents.BONE_MEAL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}

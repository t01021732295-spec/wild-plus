package com.snolred.wildplus.home;

import com.snolred.wildplus.WildPlus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * 3.5 마을 공사 부지 상자 (15×15): 집 상자처럼 자리를 미리 보고 웅크리고 우클릭하면 공사 부지(1단계)가 생기고,
 * 그 자리가 이 건물의 부지로 기억된다 (다음 단계 · 완공이 같은 자리에 지어짐).
 */
public class VillageSiteItem extends HouseKitItem {
    public final String project;

    public VillageSiteItem(Properties p, String project) {
        super(p, project, 15, 16, 15);
        this.project = project;
    }

    @Override
    protected net.minecraft.resources.Identifier templateId() {
        return WildPlus.id("village/" + project + "_1");
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !(ctx.getLevel() instanceof ServerLevel level)) return super.useOn(ctx);
        if (com.snolred.wildplus.content.Village.stage(level.getServer(), project) > 0) {
            p.sendSystemMessage(Component.literal("이 건물은 이미 공사가 시작됐어요").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        // 집 상자와 같은 계산으로 놓일 자리 (원점 · 회전) 를 구해 기억
        BlockPos target = ctx.getClickedPos();
        if (level.getBlockState(target).canBeReplaced()) target = target.below();
        Direction look = p.getDirection();
        StructureTemplate t = level.getStructureTemplateManager().get(templateId()).orElse(null);
        if (t == null) return InteractionResult.FAIL;
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotationFor(look)).setRotationPivot(BlockPos.ZERO);
        BlockPos rel = StructureTemplate.calculateRelativePosition(settings, new BlockPos(sx / 2, 0, t.getSize().getZ() - 1));
        BlockPos origin = target.subtract(rel);
        InteractionResult r = super.useOn(ctx); // 실제로 1단계 터를 짓기 (막힌 칸 · 남의 땅 검사 포함)
        if (r == InteractionResult.SUCCESS) com.snolred.wildplus.content.Village.setSite(p, project, origin, rotationFor(look));
        return r;
    }
}

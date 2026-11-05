package com.snolred.wildplus.home;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.content.Land;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * 집 상자: 들고 땅을 보면 집이 들어갈 자리가 빛나는 테두리로 보이고 (HousePreview),
 * 웅크리고 땅을 우클릭하면 그 자리에 집이 지어진다. 현관이 나를 바라보고, 누른 칸이 현관 앞 가운데.
 * 자리에 나무 · 건물이 있으면 못 지음 (풀 · 꽃 · 눈은 괜찮음). 남의 땅에는 못 지음. 바닥이 비면 흙으로 기초를 채움.
 */
public class HouseKitItem extends Item {
    public final String house;
    public final int sx, sy, sz;

    public HouseKitItem(Properties p, String house, int sx, int sy, int sz) {
        super(p);
        this.house = house;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
    }

    /** 지을 구조물 (마을 부지 상자는 다른 폴더) */
    protected net.minecraft.resources.Identifier templateId() {
        return WildPlus.id("house/" + house);
    }

    public static Rotation rotationFor(Direction look) {
        return switch (look) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** 미리 보기와 같은 계산: 누른 칸(현관 앞 가운데)과 보는 방향 → 차지하는 칸 (x0, z0, x1, z1) */
    public int[] footprint(BlockPos target, Direction look) {
        Direction right = look.getClockWise();
        int l0 = -(sx / 2), l1 = sx - 1 - sx / 2;
        int ax = target.getX() + right.getStepX() * l0, az = target.getZ() + right.getStepZ() * l0;
        int bx = target.getX() + right.getStepX() * l1 + look.getStepX() * (sz - 1), bz = target.getZ() + right.getStepZ() * l1 + look.getStepZ() * (sz - 1);
        return new int[]{Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)};
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null) return InteractionResult.PASS;
        if (!p.isShiftKeyDown()) {
            if (!ctx.getLevel().isClientSide()) {
                p.sendOverlayMessage(Component.literal("🏠 웅크리고(Shift) 땅을 우클릭하면 빛나는 테두리 자리에 집이 지어져요").withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos target = ctx.getClickedPos();
        if (level.getBlockState(target).canBeReplaced()) target = target.below();
        Direction look = p.getDirection();
        StructureTemplate t = level.getStructureTemplateManager().get(templateId()).orElse(null);
        if (t == null) {
            p.sendSystemMessage(Component.literal("집 설계도를 찾을 수 없어요: " + house).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotationFor(look)).setRotationPivot(BlockPos.ZERO);
        // 템플릿의 현관 앞 가운데 (x 가운데, z 맨 앞) 를 누른 칸에 맞춤
        BlockPos local = new BlockPos(sx / 2, 0, t.getSize().getZ() - 1);
        BlockPos rel = StructureTemplate.calculateRelativePosition(settings, local);
        BlockPos origin = target.subtract(rel);
        BoundingBox box = t.getBoundingBox(settings, origin);
        // 땅 주인 확인
        for (BlockPos c : new BlockPos[]{new BlockPos(box.minX(), target.getY(), box.minZ()), new BlockPos(box.maxX(), target.getY(), box.maxZ()),
                new BlockPos(box.minX(), target.getY(), box.maxZ()), new BlockPos(box.maxX(), target.getY(), box.minZ()), target}) {
            if (!Land.canBuild(p, c)) {
                p.sendSystemMessage(Component.literal("🏠 남의 땅에는 집을 지을 수 없어요").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
        }
        // 막힌 칸 세기 (땅 위 · 풀 · 꽃 · 눈은 괜찮음)
        int blocked = 0;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = target.getY() + 1; y <= box.maxY(); y++) {
                    BlockState s = level.getBlockState(new BlockPos(x, y, z));
                    if (!s.isAir() && !s.canBeReplaced()) blocked++;
                }
            }
        }
        if (blocked > 0) {
            p.sendSystemMessage(Component.literal("🏠 자리에 막힌 블록이 " + blocked + "칸 있어요 · 나무나 건물을 치우거나 다른 곳을 골라 주세요").withStyle(ChatFormatting.RED));
            level.playSound(null, target, SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
            return InteractionResult.FAIL;
        }
        // 기초: 바닥 아래 빈 곳을 흙으로 (최대 12칸)
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = target.getY() - 1, k = 0; k < 12; y--, k++) {
                    BlockPos q = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(q);
                    if (!s.isAir() && !s.canBeReplaced()) break;
                    level.setBlock(q, (k == 0 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        String name = getName(ctx.getItemInHand()).getString();
        t.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_ALL);
        if (!p.isCreative()) ctx.getItemInHand().shrink(1);
        level.playSound(null, target, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, target, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        for (int k = 0; k < 40; k++) {
            double x = box.minX() + level.getRandom().nextDouble() * (box.getXSpan());
            double z = box.minZ() + level.getRandom().nextDouble() * (box.getZSpan());
            level.sendParticles(ParticleTypes.CLOUD, x, target.getY() + 1.5 + level.getRandom().nextDouble() * 4, z, 1, 0, 0.05, 0, 0.02);
        }
        p.sendSystemMessage(Component.literal("🏠 " + name + " 완성! 새 집에 오신 걸 환영해요").withStyle(ChatFormatting.GOLD));
        return InteractionResult.SUCCESS;
    }
}

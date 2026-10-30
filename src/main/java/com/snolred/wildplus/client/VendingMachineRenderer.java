package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snolred.wildplus.block.VendingMachineBlock;
import com.snolred.wildplus.block.VendingMachineBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 실물 크기 자판기의 유리창 안에 상품 9칸(3단 선반 × 3열)을 진열해서 그린다.
 * 모델 좌표(정면 = 북쪽, z=0, 아래 칸 기준): 유리창은 x 4~15, y 6~29 픽셀,
 * 선반 윗면은 y 6 / 13.5 / 21, 상품은 z≈1.8 픽셀 깊이.
 */
public class VendingMachineRenderer implements BlockEntityRenderer<VendingMachineBlockEntity, VendingMachineRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public VendingMachineRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VendingMachineBlockEntity be, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.facing = be.getBlockState().getValue(VendingMachineBlock.FACING);
        for (int i = 0; i < VendingMachineBlockEntity.SLOTS; i++) {
            ItemStack stack = be.items().getItem(i);
            ItemStackRenderState item = new ItemStackRenderState();
            if (!stack.isEmpty()) {
                itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.FIXED, be.getLevel(), be,
                        (int) be.getBlockPos().asLong() + i);
            }
            state.items[i] = item;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        for (int i = 0; i < VendingMachineBlockEntity.SLOTS; i++) {
            ItemStackRenderState item = state.items[i];
            if (item == null || item.isEmpty()) {
                continue;
            }
            int col = i % 3, row = i / 3;
            float x = (13.2F - col * 3.7F) / 16.0F;
            float y = (23.3F - row * 7.5F) / 16.0F;
            poseStack.pushPose();
            poseStack.translate(x, y, 1.8F / 16.0F);
            poseStack.scale(0.27F, 0.27F, 0.27F);
            item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** 위 칸까지 그리므로 화면 밖 판정 범위를 2칸 높이로 */
    @Override
    public AABB getRenderBoundingBox(VendingMachineBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        final ItemStackRenderState[] items = new ItemStackRenderState[VendingMachineBlockEntity.SLOTS];
    }
}

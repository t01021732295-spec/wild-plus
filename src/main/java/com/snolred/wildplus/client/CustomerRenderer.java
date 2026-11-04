package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.entity.CustomerEntity;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 식당 손님: 사람 모델 + 종류별 옷 · 머리 위에 주문한 요리 아이콘 (말풍선 대신 둥둥 떠 있음) */
public class CustomerRenderer extends HumanoidMobRenderer<CustomerEntity, CustomerRenderer.State, HumanoidModel<CustomerRenderer.State>> {
    private static final Identifier[] TEX = {tex("villager"), tex("traveler"), tex("adventurer"), tex("noble"), tex("chef")};

    public static class State extends HumanoidRenderState {
        int kind;
        int count;
        boolean showOrder;
        final ItemStackRenderState[] order = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};
    }

    private static Identifier tex(String n) {
        return WildPlus.id("textures/entity/customer/" + n + ".png");
    }

    public CustomerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CustomerEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.kind = e.kind().ordinal();
        if (e.seated()) s.isPassenger = true;
        s.showOrder = e.showOrder();
        List<ItemStack> o = e.order();
        s.count = s.showOrder ? Math.min(3, o.size()) : 0;
        for (int i = 0; i < s.count; i++) {
            itemModelResolver.updateForTopItem(s.order[i], o.get(i), ItemDisplayContext.FIXED, e.level(), e, e.getId() * 3 + i);
        }
    }

    @Override
    public Identifier getTextureLocation(State s) {
        return TEX[Math.max(0, Math.min(TEX.length - 1, s.kind))];
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        super.submit(s, ps, c, cam);
        if (s.count <= 0) return;
        ps.pushPose();
        ps.translate(0.0F, s.boundingBoxHeight + 0.85F + (float) Math.sin(s.ageInTicks * 0.1F) * 0.04F, 0.0F);
        ps.mulPose(cam.orientation);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
        ps.scale(0.42F, 0.42F, 0.42F);
        for (int i = 0; i < s.count; i++) {
            ps.pushPose();
            ps.translate((i - (s.count - 1) / 2.0F) * 0.95F, 0.0F, 0.0F);
            s.order[i].submit(ps, c, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }
        ps.popPose();
    }
}

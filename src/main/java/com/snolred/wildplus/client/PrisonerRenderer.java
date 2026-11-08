package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snolred.wildplus.entity.PrisonerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

/** 3.9 감옥 포로: 바닐라 우민 모델 · 바닐라 텍스처 (종류별) · 팔짱 낀 자세 · 조금 작게 */
public class PrisonerRenderer extends MobRenderer<PrisonerEntity, PrisonerRenderer.State, IllagerModel<PrisonerRenderer.State>> {
    private static final Identifier[] TEX = {
            Identifier.withDefaultNamespace("textures/entity/illager/pillager.png"),
            Identifier.withDefaultNamespace("textures/entity/illager/vindicator.png"),
            Identifier.withDefaultNamespace("textures/entity/illager/evoker.png"),
            Identifier.withDefaultNamespace("textures/entity/illager/illusioner.png")};

    public static class State extends IllagerRenderState {
        int kind;
        boolean elite;
    }

    public PrisonerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerModel<>(ctx.bakeLayer(ModelLayers.VINDICATOR)), 0.4F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PrisonerEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.kind = e.kind();
        s.elite = e.elite();
        s.armPose = AbstractIllager.IllagerArmPose.CROSSED;
        s.walkAnimationSpeed = 0F;
    }

    @Override
    protected void scale(State s, PoseStack ps) {
        float k = s.elite ? 0.9F : 0.82F;
        ps.scale(k, k, k);
    }

    @Override
    public Identifier getTextureLocation(State s) {
        return TEX[Math.max(0, Math.min(TEX.length - 1, s.kind))];
    }
}

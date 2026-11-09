package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.entity.VoidLordEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** 4.4 공허 군주: 머리 하나 · 숨 쉬는 흉곽 · 박동하는 핵 · 흔들리는 팔과 꼬리. 도안 좌표(y 위, +z 앞, 16=1칸)로 상자를 그린다 */
public class VoidLordRenderer extends MobRenderer<VoidLordEntity, VoidLordRenderer.State, VoidLordRenderer.Body> {
    private static final Identifier TEX = WildPlus.id("textures/entity/cosmetic/hat_base.png");

    public static class State extends LivingEntityRenderState {
        public float hp = 1F;
    }

    static final class Body extends EntityModel<State> {
        Body(ModelPart root) {
            super(root);
        }
    }

    private static ModelPart bake(boolean cube) {
        MeshDefinition m = new MeshDefinition();
        if (cube) m.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 1, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(m, 64, 64).bakeRoot();
    }

    public VoidLordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Body(bake(false)), 1.2F);
        addLayer(new Parts(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VoidLordEntity e, State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.hp = e.getHealth() / e.getMaxHealth();
    }

    @Override
    public Identifier getTextureLocation(State s) {
        return TEX;
    }

    static final class Parts extends RenderLayer<State, Body> {
        private static final int BONE = 0xDED6E2, BONE2 = 0xA094B0, VOID = 0x08050F, DARK = 0x120C20, DK2 = 0x281A46, FLESH = 0x461E6E, PUR = 0x9646F0, CYAN = 0x60F0F0, RED = 0xEB3C6E, WHITE = 0xFFFFFF;
        private final Body unit = new Body(bake(true));
        private PoseStack pose;
        private SubmitNodeCollector out;
        private State st;
        private int light;

        Parts(RenderLayerParent<State, Body> parent) {
            super(parent);
        }

        private void box(float x, float y, float z, float w, float h, float d, int color, boolean glow) {
            pose.pushPose();
            pose.translate(x / 16F, y / 16F, z / 16F);
            pose.scale(w, h, d);
            renderColoredCutoutModel(unit, TEX, pose, out, glow ? 0xF000F0 : light, st, 0xFF000000 | color, 1);
            pose.popPose();
        }

        /** 도안 좌표 상자 → 모델 상자 (y 뒤집기, 앞이 -z) */
        private void d(float x, float y, float z, float w, float h, float dd, int color, boolean glow) {
            box(x, 24F - (y + h), -(z + dd), w, h, dd, color, glow);
        }

        @Override
        public void submit(PoseStack p, SubmitNodeCollector c, int packedLight, State s, float yRot, float xRot) {
            if (s.isInvisible) return;
            pose = p;
            out = c;
            st = s;
            light = packedLight;
            float t = s.ageInTicks;
            float speed = s.hp > 0.66F ? 0.09F : s.hp > 0.33F ? 0.15F : 0.24F;
            float breath = (Mth.sin(t * speed) + 1F) * 0.5F;
            float hover = Mth.sin(t * 0.05F) * 2F;
            p.pushPose();
            p.translate(0F, 1.5F, 0F);
            p.scale(0.78F, 0.78F, 0.78F);
            p.translate(0F, -1.5F - 1.1F + hover / 16F, 0F);
            head();
            halo(t);
            chest(breath, t);
            spine(t);
            for (int side = -1; side <= 1; side += 2) {
                arm(side, 13, 56, 0, 7, 3.6F, -5.5F, DARK, t);
                arm(side, 13, 46, -6, 6, 5.5F, 1.5F, DK2, t + 20F);
                cloak(side, t);
            }
            p.popPose();
        }

        private void head() {
            d(-8, 62, -8, 16, 14, 16, BONE, false);
            d(-6, 57, -6, 12, 5, 12, BONE2, false);
            for (int i = -5; i <= 3; i += 2) d(i, 58, 6, 1, 4, .4F, WHITE, false);
            d(-6, 67, 7.8F, 4, 3, .5F, VOID, false);
            d(2, 67, 7.8F, 4, 3, .5F, VOID, false);
            d(-5, 68, 8F, 2, 1, .5F, RED, true);
            d(3, 68, 8F, 2, 1, .5F, RED, true);
            d(-1, 71, 7.9F, 2, 4, .5F, RED, true);
            d(-1, 64.5F, 7.9F, 2, 1.5F, .5F, VOID, false);
            for (int s = -1; s <= 1; s += 2) {
                float[][] h = {{9, 76, -2}, {13, 80, -4}, {18, 83, -5}, {23, 84, -5}, {28, 81, -5}};
                for (int i = 0; i < h.length; i++) d(s * h[i][0] - 1.5F, h[i][1], h[i][2], 3, 4, 3, i < 3 ? BONE : PUR, false);
            }
        }

        private void halo(float t) {
            for (int i = 0; i < 16; i++) {
                float a = i * Mth.PI / 8F + t * 0.012F;
                d(36F * Mth.cos(a) - 1.5F, 66F + 36F * Mth.sin(a) - 1.5F, -14, 3, 3, 2, i % 2 == 0 ? PUR : CYAN, true);
            }
        }

        private void chest(float b, float t) {
            d(-8, 40, -5, 16, 22, 3, FLESH, false);
            d(-1.5F, 38, -3, 3, 24, 3, BONE, false);
            d(-9, 60, -4, 18, 3, 8, BONE2, false);
            float open = b * 2.2F, front = b * 2.8F;
            for (int i = 0; i < 6; i++) {
                float y = 42 + i * 3.1F, len = 9F - Math.abs(i - 2.5F) * 0.8F;
                for (int s = -1; s <= 1; s += 2) {
                    float x = s > 0 ? 2 + open : -(2 + open) - len;
                    d(x, y, 2.6F + front, len, 1.5F, 1.5F, BONE, false);
                    d(s > 0 ? x + len : x - 1.5F, y, -2, 1.5F, 1.5F, 5 + front, BONE2, false);
                }
            }
            float cs = 5 + b * 3.5F, cy = 50 + Mth.sin(b * Mth.PI) * 1.2F;
            d(-cs / 2 - 1, cy - cs / 2 - 1, -2, cs + 2, cs + 2, cs + 1, PUR, true);
            d(-cs / 2, cy - cs / 2, -1.5F, cs, cs, cs + 2.2F, CYAN, true);
            d(-cs / 4, cy - cs / 4, -1, cs / 2, cs / 2, cs + 2.8F, WHITE, true);
            for (int i = 0; i < 6; i++) {
                float a = i * Mth.PI / 3F + t * 0.07F, r = cs / 2 + 4 + b * 2;
                d(Mth.cos(a) * r - .7F, cy + Mth.sin(a) * r - .7F, 3 + b * 2, 1.4F, 1.4F, 1.4F, i % 2 == 0 ? RED : CYAN, true);
            }
        }

        private void spine(float t) {
            for (int i = 0; i < 9; i++) {
                float w = 8 - i * .7F, sw = Mth.sin(t * 0.08F + i * 0.6F) * i * 0.55F;
                d(-w / 2 + sw, 36 - i * 5, -3 + i * .6F, w, 5, 6 - i * .4F, i % 2 == 0 ? DARK : DK2, false);
                d(-1 + sw, 36 - i * 5, 3 + i * .2F, 2, 3, 3, BONE, false);
            }
        }

        private void arm(int side, float sx, float sy, float z, int n, float dx, float dy, int col, float t) {
            float sway = Mth.sin(t * 0.06F + side) * 1.0F;
            float x = sx, y = sy;
            for (int i = 0; i < n; i++) {
                float px = side * x - 2, py = y + sway * i * 0.5F;
                d(px, py, z, 4, 6, 4, i % 2 == 0 ? col : PUR, false);
                x += dx;
                y += dy;
            }
            float ey = y + sway * n * 0.5F - 6;
            for (int k = -3; k <= 3; k += 3) d(side * x - 1 + k, ey, z + 1, 2, 7, 2, BONE, false);
        }

        private void cloak(int side, float t) {
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < 6; i++) {
                    float x = side * (5 + j * 7) + Mth.sin(t * 0.07F + i * .9F + j) * 2.2F;
                    d(x - 1.5F, 50 - i * 6.5F, -9 - j, 3, 7, 1.5F, i == 5 ? CYAN : i % 3 == 0 ? PUR : FLESH, i == 5);
                }
            }
        }
    }
}

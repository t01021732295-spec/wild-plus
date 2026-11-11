package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModItems34;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * 5.0 겁화 갑옷의 입체 장식 (도안 tools/nether_designs_v50.py gear):
 * 투구 → 뒤로 휜 뿔 한 쌍 · 불꽃 볏 · 이마 장식 / 흉갑 → 견갑 가시 · 가슴 불 핵 · 끝이 타오르는 진홍 망토.
 * 상자 하나가 부품 하나 (흰 바탕 그림에 색을 곱함). 좌표는 모델 픽셀 (y 아래가 +, 앞이 -z).
 */
public class KalpaArmorLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier TEX = WildPlus.id("textures/entity/cosmetic/hat_base.png");
    private static final int BONE = 0xE2D6C4, ASH = 0x84797A, GOLD = 0xF0C460, GOLD_D = 0xA07430, RED = 0xD62E36, RED_D = 0x96141E, EMBER = 0xFF963C, WHITE = 0xFFF6DC, BLACK = 0x2E222A;

    /** 색 · 상자(x,y,z,w,h,d) · 기준점(px,py,pz) · 회전(x,z) · 빛남 */
    private record P(int color, float x, float y, float z, float w, float h, float d, float px, float py, float pz, float xr, float zr, boolean glow) {}

    static final class Part extends EntityModel<AvatarRenderState> {
        Part(ModelPart root) {
            super(root);
        }
    }

    private final Part[] head, body, cape;
    private final P[] headP, bodyP, capeP;

    private static P p(int c, double x, double y, double z, double w, double h, double d) {
        return new P(c, (float) x, (float) y, (float) z, (float) w, (float) h, (float) d, 0, 0, 0, 0, 0, false);
    }

    private static P glow(int c, double x, double y, double z, double w, double h, double d) {
        return new P(c, (float) x, (float) y, (float) z, (float) w, (float) h, (float) d, 0, 0, 0, 0, 0, true);
    }

    private static Part[] bake(P[] ps) {
        Part[] out = new Part[ps.length];
        for (int i = 0; i < ps.length; i++) {
            P q = ps[i];
            MeshDefinition m = new MeshDefinition();
            m.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(0, 0).addBox(q.x() - q.px(), q.y() - q.py(), q.z() - q.pz(), q.w(), q.h(), q.d()),
                    PartPose.offsetAndRotation(q.px(), q.py(), q.pz(), q.xr(), 0F, q.zr()));
            out[i] = new Part(LayerDefinition.create(m, 64, 64).bakeRoot());
        }
        return out;
    }

    public KalpaArmorLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
        java.util.List<P> h = new java.util.ArrayList<>();
        // 뿔: 옆에서 나와 뒤 · 위로 휨
        double[][] horn = {{4.3, -7.6, -1.2, 2.4, 0}, {5.6, -9.4, 0.0, 2.0, 1}, {6.3, -11.4, 1.3, 1.6, 2}, {6.3, -13.4, 2.7, 1.2, 2}, {5.8, -15.0, 3.8, 0.8, 2}};
        for (int s = -1; s <= 1; s += 2) {
            for (double[] q : horn) {
                double x = s > 0 ? q[0] : -q[0] - q[3];
                h.add(p(q[4] == 0 ? BLACK : q[4] == 1 ? GOLD : BONE, x, q[1], q[2], q[3], 2.6, q[3]));
            }
        }
        // 불꽃 볏: 앞(-z)에서 뒤로 갈수록 높아짐
        for (int i = 0; i < 7; i++) {
            double hh = 1.4 + i * 0.75;
            int c = i % 2 == 0 ? (i < 3 ? WHITE : EMBER) : (i < 4 ? GOLD : RED);
            h.add(glow(c, -0.6, -8.7 - hh, -3.6 + i * 1.2, 1.2, hh, 1.2));
        }
        h.add(p(GOLD, -1.0, -8.2, -5.2, 2.0, 2.6, 0.6));
        h.add(glow(WHITE, -0.5, -7.6, -5.5, 1.0, 1.2, 0.4));
        headP = h.toArray(new P[0]);
        head = bake(headP);

        java.util.List<P> b = new java.util.ArrayList<>();
        for (int s = -1; s <= 1; s += 2) { // 견갑 가시
            b.add(new P(BONE, s > 0 ? 6.4F : -7.4F, -4.4F, -0.6F, 1.0F, 2.8F, 1.2F, s * 6.9F, -1.6F, 0F, 0F, s * 0.45F, false));
            b.add(p(GOLD, s > 0 ? 4.2 : -8.4, -1.4, -2.9, 4.2, 0.8, 5.8));
        }
        b.add(p(GOLD, -1.6, 2.6, -3.3, 3.2, 3.2, 0.4)); // 가슴 핵
        b.add(glow(WHITE, -0.9, 3.3, -3.6, 1.8, 1.8, 0.4));
        b.add(p(GOLD, -5.2, -0.4, 2.4, 10.4, 1.4, 1.2));  // 망토 걸이
        bodyP = b.toArray(new P[0]);
        body = bake(bodyP);

        java.util.List<P> c = new java.util.ArrayList<>();
        c.add(new P(RED_D, -5F, 0.6F, 2.9F, 10F, 15F, 0.6F, 0F, 0.6F, 2.9F, 0F, 0F, false));
        c.add(new P(GOLD_D, -5.2F, 0.6F, 3.0F, 0.6F, 15F, 0.5F, 0F, 0.6F, 2.9F, 0F, 0F, false));
        c.add(new P(GOLD_D, 4.6F, 0.6F, 3.0F, 0.6F, 15F, 0.5F, 0F, 0.6F, 2.9F, 0F, 0F, false));
        c.add(new P(RED, -5F, 15.6F, 2.9F, 10F, 2F, 0.6F, 0F, 0.6F, 2.9F, 0F, 0F, true));
        c.add(new P(EMBER, -5F, 17.6F, 2.9F, 10F, 1.2F, 0.6F, 0F, 0.6F, 2.9F, 0F, 0F, true));
        for (int i = 0; i < 5; i++) c.add(new P(WHITE, -4.6F + i * 2F, 18.8F, 2.9F, 1.2F, 1F, 0.6F, 0F, 0.6F, 2.9F, 0F, 0F, true));
        capeP = c.toArray(new P[0]);
        cape = bake(capeP);
    }

    private void draw(Part[] parts, P[] ps, PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state) {
        for (int i = 0; i < parts.length; i++) {
            renderColoredCutoutModel(parts[i], TEX, pose, collector, ps[i].glow() ? 0xF000F0 : light, state, 0xFF000000 | ps[i].color(), 1);
        }
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isSpectator) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(state.id);
        if (!(e instanceof LivingEntity le)) return;
        if (le.getItemBySlot(EquipmentSlot.HEAD).is(ModItems34.KALPA_HELMET.get())) {
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            draw(head, headP, pose, collector, light, state);
            pose.popPose();
        }
        if (le.getItemBySlot(EquipmentSlot.CHEST).is(ModItems34.KALPA_CHESTPLATE.get())) {
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            draw(body, bodyP, pose, collector, light, state);
            boolean flying = state.fallFlyingTimeInTicks > 0;
            float sway = 0.16F + Mth.sin(state.ageInTicks * 0.09F) * 0.05F + (flying ? 1.0F : 0F);
            for (Part part : cape) part.root().getChild("p").xRot = sway;
            draw(cape, capeP, pose, collector, light, state);
            pose.popPose();
        }
    }
}

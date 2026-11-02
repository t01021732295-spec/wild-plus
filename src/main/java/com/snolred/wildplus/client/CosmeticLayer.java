package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.net.CosmeticSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * 명품관 치장: 등 뒤 날개 · 머리 위 링 · 동물 탈을 플레이어 몸에 그린다.
 * 누가 무엇을 입었는지는 서버가 CosmeticSync 로 알려 준다.
 */
public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final String[] WINGS = {"", "angel", "demon", "dragon"};
    public static final String[] HALOS = {"", "gold", "star"};

    private final Part wings;
    private final Part halo;
    /** 동물 탈: 탈마다 부품 모델 목록과 그 색 */
    private final Part[][] hats;
    private final int[][] hatColors;
    private static final Identifier HAT_TEX = WildPlus.id("textures/entity/cosmetic/hat_base.png");

    /** 단순한 모델 껍데기 */
    static final class Part extends EntityModel<AvatarRenderState> {
        final ModelPart left, right;

        Part(ModelPart root, boolean pair) {
            super(root);
            this.left = pair ? root.getChild("left") : root.getChild("ring");
            this.right = pair ? root.getChild("right") : null;
        }

        /** 탈 부품 하나 */
        Part(ModelPart root) {
            super(root);
            this.left = root;
            this.right = null;
        }
    }

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
        // 날개: 얇은 판 두 장 (16×20), 등에 붙어서 바깥으로 펼쳐짐
        MeshDefinition wm = new MeshDefinition();
        PartDefinition wr = wm.getRoot();
        wr.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(0F, -4F, 0F, 16, 20, 1), PartPose.offset(1F, 1F, 2.5F));
        wr.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-16F, -4F, 0F, 16, 20, 1), PartPose.offset(-1F, 1F, 2.5F));
        this.wings = new Part(LayerDefinition.create(wm, 64, 32).bakeRoot(), true);
        // 링: 머리 위에 뜬 네모난 고리
        MeshDefinition hm = new MeshDefinition();
        hm.getRoot().addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5F, -12F, -5F, 10, 1, 1).addBox(-5F, -12F, 4F, 10, 1, 1)
                .addBox(-5F, -12F, -4F, 1, 1, 8).addBox(4F, -12F, -4F, 1, 1, 8), PartPose.ZERO);
        this.halo = new Part(LayerDefinition.create(hm, 32, 16).bakeRoot(), false);
        // 동물 탈: 도안 부품을 하나씩 상자로 (흰 바탕에 부품 색을 곱함)
        int n = HatModels.PARTS.length;
        this.hats = new Part[n][];
        this.hatColors = new int[n][];
        for (int h = 0; h < n; h++) {
            float[][] ps = HatModels.PARTS[h];
            hats[h] = new Part[ps.length];
            hatColors[h] = new int[ps.length];
            for (int i = 0; i < ps.length; i++) {
                float[] q = ps[i];
                MeshDefinition m = new MeshDefinition();
                m.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(q[1], q[2], q[3], q[4], q[5], q[6], new CubeDeformation(q[7])),
                        PartPose.offsetAndRotation(q[8], q[9], q[10], 0F, 0F, q[11]));
                hats[h][i] = new Part(LayerDefinition.create(m, 64, 64).bakeRoot());
                hatColors[h][i] = 0xFF000000 | (int) q[0];
            }
        }
    }

    private static Identifier tex(String kind, String name) {
        return WildPlus.id("textures/entity/cosmetic/" + kind + "_" + name + ".png");
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isSpectator) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(state.id);
        if (e == null) return;
        int[] c = CosmeticSync.CLIENT.get(e.getUUID());
        if (c == null) return;
        // 동물 탈: 머리를 따라 움직임
        if (c[2] > 0 && c[2] <= hats.length) {
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            Part[] ps = hats[c[2] - 1];
            for (int i = 0; i < ps.length; i++)
                renderColoredCutoutModel(ps[i], HAT_TEX, pose, collector, light, state, hatColors[c[2] - 1][i], 1);
            pose.popPose();
        }
        // 날개: 천천히 퍼덕이고, 날 때는 활짝
        if (c[0] > 0 && c[0] < WINGS.length) {
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            float t = state.ageInTicks;
            float open = state.fallFlyingTimeInTicks > 0 ? 1.15F : 0.45F + Mth.sin(t * 0.12F) * 0.08F;
            wings.left.yRot = open;
            wings.right.yRot = -open;
            wings.left.zRot = -0.12F;
            wings.right.zRot = 0.12F;
            renderColoredCutoutModel(wings, tex("wings", WINGS[c[0]]), pose, collector, light, state, 0xFFFFFFFF, 1);
            pose.popPose();
        }
        // 링: 머리 위에서 살짝 위아래로 뜸
        if (c[1] > 0 && c[1] < HALOS.length) {
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            pose.translate(0F, Mth.sin(state.ageInTicks * 0.1F) * 0.03F, 0F);
            renderColoredCutoutModel(halo, tex("halo", HALOS[c[1]]), pose, collector, 0xF000F0, state, 0xFFFFFFFF, 1);
            pose.popPose();
        }
    }
}

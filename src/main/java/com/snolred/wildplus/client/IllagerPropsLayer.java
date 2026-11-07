package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snolred.wildplus.WildPlus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * 3.8 우민 밀수꾼 · 용병 소품 (tools/omen_design.py 도안): 몸통에 붙는 상자들을 색칠해서 그린다.
 * 밀수꾼: 가죽 배낭 · 매달린 흉조 병 3 · 허리 등불 / 용병: 철 어깨 · 마을 문장 방패.
 */
public class IllagerPropsLayer extends RenderLayer<IllagerRenderState, IllagerModel<IllagerRenderState>> {
    private static final Identifier TEX = WildPlus.id("textures/entity/cosmetic/hat_base.png");

    /** {색, x, y, z, 너비, 높이, 깊이, 빛남(1)} — 몸통 기준 모델 좌표 (y 아래, +z 뒤) */
    static final float[][] SMUGGLER = {
            {0x78543A, -4.5F, 1F, 3.5F, 9F, 14F, 5.5F, 0},
            {0x50361F, -5F, -0.5F, 3F, 10F, 2.5F, 6.5F, 0},
            {0x604228, -3F, 7F, 8.8F, 6F, 5F, 0.8F, 0},
            {0xC8AA50, -1F, 8.5F, 9.4F, 2F, 1.5F, 0.6F, 0},
            {0x4AC8D5, -7.6F, 8.6F, 5.6F, 2.4F, 2.4F, 2.4F, 1},
            {0x7E453C, -7.0F, 7.9F, 6.2F, 1.2F, 0.7F, 1.2F, 0},
            {0x4AC8D5, 5F, 6.6F, 5.6F, 2.4F, 2.4F, 2.4F, 1},
            {0x7E453C, 5.6F, 5.9F, 6.2F, 1.2F, 0.7F, 1.2F, 0},
            {0x4AC8D5, -7.6F, 3.9F, 4.4F, 2.1F, 2.1F, 2.1F, 1},
            {0x323236, 5.2F, 8F, -1F, 0.5F, 4F, 0.5F, 0},
            {0x323236, 4.3F, 11.4F, -2F, 2.4F, 0.6F, 2.4F, 0},
            {0xFFDC78, 4.5F, 12F, -1.8F, 2F, 4F, 2F, 1},
            {0x323236, 4.3F, 16F, -2F, 2.4F, 0.6F, 2.4F, 0},
    };
    static final float[][] MERCENARY = {
            {0xB4B8C0, -8.6F, -0.2F, -2.6F, 5.2F, 3.2F, 5.2F, 0},
            {0xB4B8C0, 3.4F, -0.2F, -2.6F, 5.2F, 3.2F, 5.2F, 0},
            {0x284696, 8F, 3F, -5F, 1.2F, 13F, 9F, 0},
            {0x6E5032, 7.7F, 3F, -5F, 0.3F, 13F, 9F, 0},
            {0xF0C846, 9.1F, 5F, -1.3F, 0.3F, 9F, 1.6F, 0},
            {0xF0C846, 9.1F, 11F, -3.5F, 0.3F, 1.2F, 6F, 0},
    };

    static final class Part extends EntityModel<IllagerRenderState> {
        Part(ModelPart root) {
            super(root);
        }
    }

    private final List<Part> parts = new ArrayList<>();
    private final List<int[]> looks = new ArrayList<>();

    public IllagerPropsLayer(RenderLayerParent<IllagerRenderState, IllagerModel<IllagerRenderState>> parent, float[][] spec) {
        super(parent);
        for (float[] q : spec) {
            MeshDefinition m = new MeshDefinition();
            m.getRoot().addOrReplaceChild("p", CubeListBuilder.create().texOffs(0, 0).addBox(q[1], q[2], q[3], q[4], q[5], q[6]), PartPose.ZERO);
            parts.add(new Part(LayerDefinition.create(m, 64, 64).bakeRoot()));
            looks.add(new int[]{0xFF000000 | (int) q[0], (int) q[7]});
        }
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, IllagerRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        pose.pushPose();
        getParentModel().root().getChild("body").translateAndRotate(pose);
        for (int i = 0; i < parts.size(); i++) {
            int[] lk = looks.get(i);
            renderColoredCutoutModel(parts.get(i), TEX, pose, collector, lk[1] == 1 ? 0xF000F0 : light, state, lk[0], 1);
        }
        pose.popPose();
    }
}

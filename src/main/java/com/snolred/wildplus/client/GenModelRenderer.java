package com.snolred.wildplus.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snolred.wildplus.WildPlus;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
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
import net.minecraft.world.entity.Mob;

/** 4.7 도안에서 만든 모델(tools/gen_models_v47.py) 렌더러: 그룹별로 움직이고 glow 는 어두운 곳에서도 빛난다 */
public class GenModelRenderer<T extends Mob> extends MobRenderer<T, GenModelRenderer.State, GenModelRenderer.Body> {
    public static class State extends LivingEntityRenderState {
        public float hp = 1F;
        public boolean cooled;
    }

    public interface Anim {
        void apply(String group, State s, Pivot p);
    }

    /** 도안 좌표를 축으로 돌리기 */
    public static final class Pivot {
        final PoseStack pose;
        final float yshift;

        Pivot(PoseStack pose, float yshift) {
            this.pose = pose;
            this.yshift = yshift;
        }

        private void to(float x, float y, float z, float sign) {
            pose.translate(sign * x / 16F, sign * (24F - (y + yshift)) / 16F, sign * -z / 16F);
        }

        public void rotate(Axis axis, float deg, float x, float y, float z) {
            to(x, y, z, 1F);
            pose.rotate(axis.rotationDegrees(deg));
            to(x, y, z, -1F);
        }

        public void scale(float f, float x, float y, float z) {
            to(x, y, z, 1F);
            pose.scale(f, f, f);
            to(x, y, z, -1F);
        }

        public void move(float x, float y, float z) {
            pose.translate(x / 16F, -y / 16F, -z / 16F);
        }
    }

    static final class Body extends EntityModel<State> {
        Body(ModelPart root) {
            super(root);
        }
    }

    private record Part(String name, Body solid, Body glow) {}

    private final Identifier tex;
    private final float scale;
    private final float yshift;
    private final Map<String, Part> parts = new LinkedHashMap<>();
    private final Anim anim;

    private static Body bake(com.google.gson.JsonArray cubes, int tw, int th) {
        MeshDefinition m = new MeshDefinition();
        CubeListBuilder b = CubeListBuilder.create();
        for (var e : cubes) {
            var c = e.getAsJsonArray();
            b.texOffs(c.get(0).getAsInt(), c.get(1).getAsInt()).addBox(c.get(2).getAsFloat(), c.get(3).getAsFloat(), c.get(4).getAsFloat(), c.get(5).getAsFloat(), c.get(6).getAsFloat(), c.get(7).getAsFloat());
        }
        m.getRoot().addOrReplaceChild("p", b, PartPose.ZERO);
        return new Body(LayerDefinition.create(m, tw, th).bakeRoot());
    }

    private static Body empty() {
        return new Body(LayerDefinition.create(new MeshDefinition(), 64, 64).bakeRoot());
    }

    public GenModelRenderer(EntityRendererProvider.Context ctx, String name, float scale, float shadow, Anim anim) {
        super(ctx, empty(), shadow);
        this.tex = WildPlus.id("textures/entity/gen/" + name + ".png");
        this.scale = scale;
        this.anim = anim;
        float ys = 0;
        try (Reader r = ctx.getResourceManager().getResourceOrThrow(WildPlus.id("models_gen/" + name + ".json")).openAsReader()) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            ys = o.get("yshift").getAsFloat();
            int tw = o.get("tw").getAsInt(), th = o.get("th").getAsInt();
            for (var en : o.getAsJsonObject("groups").entrySet()) {
                JsonObject g = en.getValue().getAsJsonObject();
                parts.put(en.getKey(), new Part(en.getKey(), bake(g.getAsJsonArray("solid"), tw, th), bake(g.getAsJsonArray("glow"), tw, th)));
            }
        } catch (Exception e) {
            WildPlus.LOGGER.error("모델을 읽지 못했어요: " + name, e);
        }
        this.yshift = ys;
        addLayer(new Layer(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T e, State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.hp = e.getHealth() / e.getMaxHealth();
        s.cooled = e instanceof com.snolred.wildplus.entity.VolcanoEntity v && v.cooled();
    }

    @Override
    public Identifier getTextureLocation(State s) {
        return tex;
    }

    final class Layer extends RenderLayer<State, Body> {
        Layer(RenderLayerParent<State, Body> p) {
            super(p);
        }

        @Override
        public void submit(PoseStack p, SubmitNodeCollector c, int light, State s, float yRot, float xRot) {
            if (s.isInvisible) return;
            p.pushPose();
            p.translate(0F, 1.5F, 0F);
            p.scale(scale, scale, scale);
            p.translate(0F, -1.5F, 0F);
            for (Part part : parts.values()) {
                p.pushPose();
                anim.apply(part.name(), s, new Pivot(p, 0F));
                boolean heart = part.name().equals("heart");
                int solid = s.cooled ? 0xFF808084 : 0xFFFFFFFF;
                renderColoredCutoutModel(part.solid(), tex, p, c, light, s, solid, 1);
                int glow = s.cooled && !heart ? 0xFF505058 : 0xFFFFFFFF;
                renderColoredCutoutModel(part.glow(), tex, p, c, s.cooled && !heart ? light : 0xF000F0, s, glow, 1);
                p.popPose();
            }
            p.popPose();
        }
    }

    // ───────── 움직임 ─────────
    public static void volcano(String g, State s, Pivot p) {
        float t = s.ageInTicks, speed = s.hp > 0.5F ? 0.1F : 0.2F;
        switch (g) {
            case "heart" -> p.scale(1F + (Mth.sin(t * speed) + 1F) * 0.06F, 0F, 66F, 15F);
            case "armL" -> p.rotate(Axis.XP, Mth.sin(t * 0.06F) * 6F, -38F, 92F, 0F);
            case "armR" -> p.rotate(Axis.XP, -Mth.sin(t * 0.06F) * 6F, 42F, 94F, 0F);
            default -> {}
        }
    }

    public static void stormKing(String g, State s, Pivot p) {
        float t = s.ageInTicks;
        switch (g) {
            case "swords" -> p.rotate(Axis.YP, t * 3F, 0F, 0F, 0F);
            case "helm" -> p.move(0F, Mth.sin(t * 0.1F) * 1.4F, 0F);
            case "cape" -> p.rotate(Axis.XP, Mth.sin(t * 0.09F) * 4F, 0F, 74F, -15F);
            default -> p.move(0F, Mth.sin(t * 0.05F) * 1.2F, 0F);
        }
    }

    public static void hawk(String g, State s, Pivot p) {
        float a = Mth.sin(s.ageInTicks * 0.35F) * 26F;
        if (g.equals("wingR")) p.rotate(Axis.ZP, a, 5F, 26F, 0F);
        else if (g.equals("wingL")) p.rotate(Axis.ZP, -a, -5F, 26F, 0F);
    }

    public static void sentinel(String g, State s, Pivot p) {
        float t = s.ageInTicks;
        p.move(0F, Mth.sin(t * 0.09F) * (g.equals("cloud") ? 1.2F : 0.6F), 0F);
    }

    /** 턴테이블이 돌고, 스피커가 박자처럼 들썩인다 */
    public static void music(String g, State s, Pivot p) {
        float t = s.ageInTicks, beat = Mth.sin(t * 0.8F);
        float kick = beat * beat;
        switch (g) {
            case "disc" -> p.rotate(Axis.YP, t * 14F, 0F, 13F, 0F);
            case "spkL" -> p.scale(1F + kick * 0.05F, -18F, 0F, 0F);
            case "spkR" -> p.scale(1F + (1F - kick) * 0.05F, 18F, 0F, 0F);
            case "coneL" -> p.scale(1F + kick * 0.18F, -18F, 22F, 8F);
            case "coneR" -> p.scale(1F + (1F - kick) * 0.18F, 18F, 22F, 8F);
            default -> {}
        }
    }

    public static void priest(String g, State s, Pivot p) {
        float t = s.ageInTicks;
        if (g.equals("ring")) p.rotate(Axis.ZP, t * 4F, 0F, 60F, -9F);
        else p.move(0F, Mth.sin(t * 0.08F) * 0.8F, 0F);
    }
}

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
        public float hurt;
        /** 5.2.1 걷기 위상 · 걷기 세기(0~1) · 공격 휘두르기(0~1) */
        public float walkPos, walkSpeed, swing;
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
        int rn = 0;
        for (var e : cubes) {
            var c = e.getAsJsonArray();
            if (c.size() >= 13) { // 회전 상자: [u,v,x,y,z,w,h,d, 축, 라디안, px,py,pz] → 따로 굽는다
                float px = c.get(10).getAsFloat(), py = c.get(11).getAsFloat(), pz = c.get(12).getAsFloat(), a = c.get(9).getAsFloat();
                int axis = c.get(8).getAsInt();
                m.getRoot().addOrReplaceChild("r" + rn++, CubeListBuilder.create().texOffs(c.get(0).getAsInt(), c.get(1).getAsInt())
                        .addBox(c.get(2).getAsFloat() - px, c.get(3).getAsFloat() - py, c.get(4).getAsFloat() - pz, c.get(5).getAsFloat(), c.get(6).getAsFloat(), c.get(7).getAsFloat()),
                        PartPose.offsetAndRotation(px, py, pz, axis == 0 ? a : 0F, axis == 1 ? a : 0F, axis == 2 ? a : 0F));
                continue;
            }
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
        s.hurt = e.hurtTime > 0 ? e.hurtTime - partial : 0F;
        s.walkPos = e.walkAnimation.position(partial);
        s.walkSpeed = Math.min(1F, e.walkAnimation.speed(partial));
        s.swing = e.getSwingAnimation(partial);
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

    /** 5.1 겁화의 군주: 불의 고리 · 향로 · 날개 + (5.2.1) 걷기 · 대검 내려찍기 */
    public static void lord(String g, State s, Pivot p) {
        float t = s.ageInTicks, w = walk(s, 0.45F, 24F), atk = hit(s);
        p.move(0F, -Math.abs(Mth.sin(s.walkPos * 0.45F)) * s.walkSpeed * 1.5F, 0F);
        switch (g) {
            case "ring" -> p.rotate(Axis.ZP, t * 1.5F, 0F, 96F, -10F);
            case "wings" -> p.rotate(Axis.XP, Mth.sin(t * 0.07F) * 4F + atk * 10F, 0F, 68F, -13F);
            case "legL" -> p.rotate(Axis.XP, w, -8F, 37F, 0F);
            case "legR" -> p.rotate(Axis.XP, -w, 8F, 37F, 0F);
            case "armR" -> p.rotate(Axis.XP, w * 0.5F - atk * 75F, 25F, 62F, 0F);
            case "armL" -> p.rotate(Axis.XP, -w * 0.6F, -34F, 62F, 0F);
            case "censer" -> {
                p.rotate(Axis.XP, -w * 0.6F, -34F, 62F, 0F);
                p.rotate(Axis.XP, Mth.sin(t * 0.12F) * 14F + w * 0.8F, -35F, 24F, 11F);
            }
            default -> {}
        }
    }

    /** 5.0.10 허수아비: 맞으면 받침 위에서 앞뒤로 흔들림 */
    public static void dummy(String g, State s, Pivot p) {
        if (s.hurt <= 0F || g.equals("base")) return;
        float k = s.hurt / 10F;
        p.rotate(Axis.XP, Mth.sin(s.hurt * 1.6F) * 9F * k, 0F, 4F, 0F);
        if (g.equals("head")) p.rotate(Axis.ZP, Mth.sin(s.hurt * 1.1F) * 6F * k, 0F, 28F, 0F);
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

    // ───── 5.0 네더 ─────
    /** 걷기 흔들림 (각도) */
    private static float walk(State s, float freq, float deg) {
        return Mth.sin(s.walkPos * freq) * deg * s.walkSpeed;
    }

    /** 공격: 0 → 1 → 0 으로 한 번 휘두름 */
    private static float hit(State s) {
        return Mth.sin(s.swing * Mth.PI);
    }

    public static void hound(String g, State s, Pivot p) {
        float t = s.ageInTicks, w = walk(s, 0.9F, 38F);
        p.move(0F, Math.abs(Mth.sin(s.walkPos * 0.9F)) * s.walkSpeed * 0.8F, 0F);
        switch (g) {
            case "tail" -> p.rotate(Axis.YP, Mth.sin(t * 0.3F) * (14F + 20F * s.walkSpeed), 0F, 14F, -12F);
            case "head" -> {
                p.rotate(Axis.XP, Mth.sin(t * 0.12F) * 4F + hit(s) * 28F, 0F, 12F, 11F);
                p.move(0F, 0F, hit(s) * 2.5F);
            }
            case "legA" -> p.rotate(Axis.XP, w + Mth.sin(t * 0.1F) * 2F, 0F, 8F, 0F);
            case "legB" -> p.rotate(Axis.XP, -w - Mth.sin(t * 0.1F) * 2F, 0F, 8F, 0F);
            default -> {}
        }
    }

    public static void turtle(String g, State s, Pivot p) {
        float w = walk(s, 0.7F, 28F);
        p.rotate(Axis.ZP, Mth.sin(s.walkPos * 0.7F) * 3F * s.walkSpeed, 0F, 0F, 0F);
        switch (g) {
            case "head" -> p.move(0F, 0F, -Mth.sin(s.ageInTicks * 0.05F) * 1.5F + hit(s) * 4F);
            case "legFL", "legBR" -> p.rotate(Axis.XP, w, 0F, 5F, g.equals("legFL") ? 10F : -8F);
            case "legFR", "legBL" -> p.rotate(Axis.XP, -w, 0F, 5F, g.equals("legFR") ? 10F : -8F);
            default -> {}
        }
    }

    public static void shaman(String g, State s, Pivot p) {
        float t = s.ageInTicks;
        p.move(0F, Mth.sin(t * 0.08F) * 0.9F, 0F);
        p.rotate(Axis.XP, 12F * s.walkSpeed + hit(s) * 14F, 0F, 10F, 0F); // 나아갈 때 앞으로 기울고, 주문 때 앞으로 숙임
        if (g.equals("talisman")) p.rotate(Axis.YP, t * (2.5F + hit(s) * 12F), 0F, 0F, 0F);
    }

    public static void golem(String g, State s, Pivot p) {
        float idle = Mth.sin(s.ageInTicks * 0.07F) * 5F, w = walk(s, 0.55F, 30F), atk = hit(s) * 85F;
        p.move(0F, -Math.abs(Mth.sin(s.walkPos * 0.55F)) * s.walkSpeed * 1.2F, 0F);
        p.rotate(Axis.ZP, Mth.sin(s.walkPos * 0.55F) * 3F * s.walkSpeed, 0F, 16F, 0F);
        switch (g) {
            case "armL" -> p.rotate(Axis.XP, idle - w - atk, -20F, 40F, 0F);
            case "armR" -> p.rotate(Axis.XP, -idle + w - atk, 20F, 40F, 0F);
            case "legL" -> p.rotate(Axis.XP, w, -6F, 16F, 0F);
            case "legR" -> p.rotate(Axis.XP, -w, 6F, 16F, 0F);
            default -> {}
        }
    }

    // ───────── 5.3 고대 도시 ─────────
    /** 스컬크 추적자: 성큼성큼 걷기 · 감지 촉수가 떨림 · 공격 때 양팔 할퀴기 */
    public static void stalker(String g, State s, Pivot p) {
        float t = s.ageInTicks, w = walk(s, 0.7F, 34F), atk = hit(s);
        p.move(0F, Math.abs(Mth.sin(s.walkPos * 0.7F)) * s.walkSpeed * 1.2F, 0F);
        switch (g) {
            case "head" -> {
                p.rotate(Axis.YP, Mth.sin(t * 0.09F) * 18F, 0F, 34F, 6F); // 소리를 찾아 고개를 돌림
                p.rotate(Axis.ZP, Mth.sin(t * 0.9F) * 1.5F, 0F, 34F, 6F);
            }
            case "armL" -> p.rotate(Axis.XP, -w - atk * 70F, -9F, 33F, 3F);
            case "armR" -> p.rotate(Axis.XP, w - atk * 70F, 9F, 33F, 3F);
            case "legL" -> p.rotate(Axis.XP, w, -4F, 17F, 0F);
            case "legR" -> p.rotate(Axis.XP, -w, 4F, 17F, 0F);
            default -> {}
        }
    }

    /** 메아리 박쥐 떼: 박쥐 3마리가 소리 덩어리 주위를 돌며 날갯짓 */
    public static void bats(String g, State s, Pivot p) {
        float t = s.ageInTicks;
        p.move(0F, Mth.sin(t * 0.15F) * 1.2F, 0F);
        switch (g) {
            case "core" -> {
                p.rotate(Axis.YP, t * 6F, 0F, 16F, 0F);
                p.scale(1F + Mth.sin(t * 0.4F) * 0.12F, 0F, 16F, 0F);
            }
            case "bat1", "bat2", "bat3" -> {
                int k = g.charAt(3) - '1';
                p.rotate(Axis.YP, t * (5F + k), 0F, 16F, 0F);
                p.move(0F, Mth.sin(t * 0.5F + k * 2.1F) * 1.4F, 0F);
                p.scale(1F + Mth.sin(t * 1.8F + k) * 0.1F, 0F, 16F, 0F); // 날갯짓 대신 몸이 들썩
            }
            default -> {}
        }
    }

    /** 침묵의 사제: 떠다니듯 흔들림 · 종 지팡이를 흔들어 울림 · 초가 돎 */
    public static void silentPriest(String g, State s, Pivot p) {
        float t = s.ageInTicks, atk = hit(s);
        p.move(0F, Mth.sin(t * 0.07F) * 0.6F, 0F);
        p.rotate(Axis.XP, 8F * s.walkSpeed, 0F, 0F, 0F);
        switch (g) {
            case "staff" -> p.rotate(Axis.ZP, Mth.sin(t * 0.1F) * 4F - atk * 25F + Mth.sin(t * 1.2F) * atk * 8F, 10F, 18F, 5F);
            case "candles" -> {
                p.rotate(Axis.YP, t * 2F, 0F, 0F, 0F);
                p.move(0F, Mth.sin(t * 0.12F) * 1.5F, 0F);
            }
            default -> {}
        }
    }

    /** 스컬크 골렘: 무거운 걸음 · 양팔 내리치기 · 등 촉수가 꿈틀 */
    public static void sculkGolem(String g, State s, Pivot p) {
        float t = s.ageInTicks, idle = Mth.sin(t * 0.06F) * 4F, w = walk(s, 0.5F, 26F), atk = hit(s) * 90F;
        p.move(0F, -Math.abs(Mth.sin(s.walkPos * 0.5F)) * s.walkSpeed * 1.4F, 0F);
        p.rotate(Axis.ZP, Mth.sin(s.walkPos * 0.5F) * 3F * s.walkSpeed, 0F, 14F, 0F);
        switch (g) {
            case "armL" -> p.rotate(Axis.XP, idle - w - atk, -19F, 36F, 0F);
            case "armR" -> p.rotate(Axis.XP, -idle + w - atk, 19F, 36F, 0F);
            case "legL" -> p.rotate(Axis.XP, w, -6F, 14F, 0F);
            case "legR" -> p.rotate(Axis.XP, -w, 6F, 14F, 0F);
            case "tendrils" -> {
                p.rotate(Axis.XP, Mth.sin(t * 0.13F) * 8F + atk * 0.2F, 0F, 38F, -6F);
                p.rotate(Axis.ZP, Mth.sin(t * 0.09F) * 6F, 0F, 38F, -6F);
            }
            default -> {}
        }
    }

    public static void priest(String g, State s, Pivot p) {
        float t = s.ageInTicks;
        if (g.equals("ring")) p.rotate(Axis.ZP, t * 4F, 0F, 60F, -9F);
        else p.move(0F, Mth.sin(t * 0.08F) * 0.8F, 0F);
    }
}

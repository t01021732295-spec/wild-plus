package com.snolred.wildplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snolred.wildplus.WildPlus;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** 각성 단계는 드래곤 이름("각성 드래곤 III")으로 클라이언트까지 전달됨 */
public class VoidDragonRenderer extends EnderDragonRenderer {
    private static final String[] ROMAN = {"V", "IV", "III", "II", "I"};
    private static final Identifier VANILLA = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    private static int current;

    public static final class St extends EnderDragonRenderState {
        int stage;
    }

    public VoidDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public static Identifier texture() {
        return current <= 0 ? VANILLA : WildPlus.id("textures/entity/void_dragon_" + current + ".png");
    }

    @Override
    public EnderDragonRenderState createRenderState() {
        return new St();
    }

    @Override
    public void extractRenderState(EnderDragon dragon, EnderDragonRenderState state, float partial) {
        super.extractRenderState(dragon, state, partial);
        if (!(state instanceof St s)) return;
        s.stage = 0;
        if (dragon.getCustomName() == null) return;
        String n = dragon.getCustomName().getString();
        if (!n.startsWith("각성 드래곤 ")) return;
        for (int i = 0; i < 5; i++) if (n.endsWith(" " + ROMAN[i])) s.stage = 5 - i;
    }

    @Override
    public void submit(EnderDragonRenderState state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        current = state instanceof St s ? s.stage : 0;
        super.submit(state, pose, out, camera);
        current = 0;
    }
}

package com.snolred.wildplus.mixin;

import com.snolred.wildplus.client.VoidDragonRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.resources.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 4.0 각성 드래곤 단계별 텍스처. 맞지 않으면(require=0) 바닐라 그림 그대로 */
@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {
    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/EnderDragonRenderer;DRAGON_TEXTURE_LOCATION:Lnet/minecraft/resources/Identifier;", opcode = Opcodes.GETSTATIC), require = 0)
    private Identifier wildplus$texture() {
        return VoidDragonRenderer.texture();
    }
}

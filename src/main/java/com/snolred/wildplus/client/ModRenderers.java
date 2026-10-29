package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 새 몹은 바닐라 모델을 그대로 쓰고 텍스처만 바꾼다 */
@EventBusSubscriber(modid = WildPlus.MODID, value = Dist.CLIENT)
public final class ModRenderers {
    private ModRenderers() {}

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MERCHANT.get(), ctx -> new MerchantRenderer(ctx, tex("merchant")));
        event.registerEntityRenderer(ModEntities.BLACKSMITH_MERCHANT.get(), ctx -> new MerchantRenderer(ctx, tex("blacksmith_merchant")));
        event.registerEntityRenderer(ModEntities.BANDIT.get(), BanditRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDEN_SLIME.get(), GoldenSlimeRenderer::new);
    }

    private static Identifier tex(String name) {
        return WildPlus.id("textures/entity/" + name + ".png");
    }

    static final class MerchantRenderer extends WanderingTraderRenderer {
        private final Identifier texture;

        MerchantRenderer(EntityRendererProvider.Context ctx, Identifier texture) {
            super(ctx);
            this.texture = texture;
        }

        @Override
        public Identifier getTextureLocation(VillagerRenderState state) {
            return texture;
        }
    }

    static final class BanditRenderer extends VindicatorRenderer {
        private static final Identifier TEXTURE = tex("bandit");

        BanditRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public Identifier getTextureLocation(IllagerRenderState state) {
            return TEXTURE;
        }
    }

    static final class GoldenSlimeRenderer extends SlimeRenderer {
        private static final Identifier TEXTURE = tex("golden_slime");

        GoldenSlimeRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public Identifier getTextureLocation(SlimeRenderState state) {
            return TEXTURE;
        }
    }
}

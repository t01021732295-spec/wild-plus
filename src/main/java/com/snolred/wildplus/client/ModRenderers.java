package com.snolred.wildplus.client;

import com.snolred.wildplus.WildPlus;
import com.snolred.wildplus.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
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
        event.registerEntityRenderer(net.minecraft.world.entity.EntityTypes.ENDER_DRAGON, VoidDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.MERCHANT.get(), ctx -> new MerchantRenderer(ctx, tex("merchant")));
        event.registerEntityRenderer(ModEntities.BLACKSMITH_MERCHANT.get(), ctx -> new MerchantRenderer(ctx, tex("blacksmith_merchant")));
        event.registerEntityRenderer(ModEntities.FARMER_MERCHANT.get(), ctx -> new MerchantRenderer(ctx, tex("farmer_merchant")));
        event.registerEntityRenderer(ModEntities.FISHER_MERCHANT.get(), ctx -> new MerchantRenderer(ctx, tex("fisher_merchant")));
        event.registerEntityRenderer(ModEntities.BANKER_MERCHANT.get(), ctx -> new MerchantRenderer(ctx, tex("banker_merchant")));
        event.registerEntityRenderer(ModEntities.BANDIT.get(), BanditRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDEN_SLIME.get(), GoldenSlimeRenderer::new);
        event.registerEntityRenderer(ModEntities.CUSTOMER.get(), CustomerRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        // 3.8 흉조: 바닐라 변명자 모델 + 두건/철모(모자 층) + 소품
        event.registerEntityRenderer(ModEntities.SMUGGLER.get(), ctx -> new OmenIllager(ctx, tex("illager_smuggler"), IllagerPropsLayer.SMUGGLER));
        event.registerEntityRenderer(ModEntities.MERCENARY.get(), ctx -> new OmenIllager(ctx, tex("illager_mercenary"), IllagerPropsLayer.MERCENARY));
        event.registerEntityRenderer(ModEntities.PRISONER.get(), PrisonerRenderer::new);
        // 보스: 바닐라 모델 + 전용 텍스처
        event.registerEntityRenderer(ModEntities.TITAN_BOSS.get(), ctx -> new net.minecraft.client.renderer.entity.RavagerRenderer(ctx) {
            @Override
            public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.RavagerRenderState s) {
                return tex("boss/titan");
            }
        });
        event.registerEntityRenderer(ModEntities.LICH_BOSS.get(), ctx -> new net.minecraft.client.renderer.entity.EvokerRenderer<com.snolred.wildplus.entity.BossMobs.Lich>(ctx) {
            @Override
            public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.EvokerRenderState s) {
                return tex("boss/lich");
            }
        });
        event.registerEntityRenderer(ModEntities.FROST_BOSS.get(), ctx -> new net.minecraft.client.renderer.entity.StrayRenderer(ctx) {
            @Override
            public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.SkeletonRenderState s) {
                return tex("boss/frost");
            }
        });
        event.registerEntityRenderer(ModEntities.VINE_BOSS.get(), ctx -> new IllagerTex(ctx, tex("boss/vine")));
        event.registerEntityRenderer(ModEntities.VOID_BOSS.get(), ctx -> new IllagerTex(ctx, tex("boss/void")));
        event.registerEntityRenderer(ModEntities.GOLD_BOSS.get(), ctx -> new IllagerTex(ctx, tex("boss/gold")));
        event.registerEntityRenderer(ModEntities.PHARAOH_BOSS.get(), ctx -> new net.minecraft.client.renderer.entity.HuskRenderer(ctx) {
            @Override
            public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ZombieRenderState s) {
                return tex("boss/pharaoh");
            }
        });
        event.registerEntityRenderer(ModEntities.FLAME_BOSS.get(), ctx -> new net.minecraft.client.renderer.entity.WitherSkeletonRenderer(ctx) {
            @Override
            public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.SkeletonRenderState s) {
                return tex("boss/flame");
            }
        });
        event.registerEntityRenderer(ModEntities.ABYSS_BOSS.get(), ctx -> new net.minecraft.client.renderer.entity.DrownedRenderer(ctx) {
            @Override
            public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ZombieRenderState s) {
                return tex("boss/abyss");
            }
        });
        event.registerBlockEntityRenderer(com.snolred.wildplus.registry.ModBlockEntities.VENDING_MACHINE.get(), VendingMachineRenderer::new);
    }

    /** 모든 플레이어 그림에 치장(날개 · 링 · 옷) 층을 붙인다 */
    @SubscribeEvent
    static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> r = event.getPlayerRenderer(skin);
            if (r != null) r.addLayer(new CosmeticLayer(r));
        }
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

    static final class IllagerTex extends VindicatorRenderer {
        private final Identifier texture;

        IllagerTex(EntityRendererProvider.Context ctx, Identifier texture) {
            super(ctx);
            this.texture = texture;
        }

        @Override
        public Identifier getTextureLocation(IllagerRenderState state) {
            return texture;
        }
    }

    static final class OmenIllager extends VindicatorRenderer {
        private final Identifier texture;

        OmenIllager(EntityRendererProvider.Context ctx, Identifier texture, float[][] props) {
            super(ctx);
            this.texture = texture;
            this.getModel().getHat().visible = true;
            this.addLayer(new IllagerPropsLayer(this, props));
        }

        @Override
        public Identifier getTextureLocation(IllagerRenderState state) {
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
            // 바닐라 바깥 젤리 층은 초록 텍스처가 고정이라 황금색 층으로 교체
            this.layers.clear();
            this.addLayer(new GoldenOuterLayer(this, ctx.getModelSet()));
        }

        @Override
        public Identifier getTextureLocation(SlimeRenderState state) {
            return TEXTURE;
        }
    }

    static final class GoldenOuterLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
        private final SlimeModel model;

        GoldenOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> parent, EntityModelSet models) {
            super(parent);
            this.model = new SlimeModel(models.bakeLayer(ModelLayers.SLIME_OUTER));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SlimeRenderState state, float yRot, float xRot) {
            renderColoredCutoutModel(this.model, GoldenSlimeRenderer.TEXTURE, poseStack, collector, light, state, 0xFFFFFFFF, 1);
        }
    }
}

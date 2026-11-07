package com.exiledradio.buzzybeesbackport.golem.client;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.golem.GolemRepair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.util.ResourceLocation;

/**
 * The cracks: a layer over whatever renderer draws iron golems (vanilla's, or another mod's replacement if
 * it still draws a living model), added at init, once the renderers exist.
 */
public final class GolemClient {

    private static final ResourceLocation[] CRACKS = {
            null,
            tex("iron_golem_crackiness_low"),
            tex("iron_golem_crackiness_medium"),
            tex("iron_golem_crackiness_high"),
    };

    private GolemClient() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(BuzzyBeesBackport.MOD_ID, "textures/entity/iron_golem/" + name + ".png");
    }

    @SuppressWarnings("unchecked")
    public static void init() {
        Render<?> render = Minecraft.getMinecraft().getRenderManager().entityRenderMap.get(EntityIronGolem.class);
        if (!(render instanceof RenderLivingBase)) {
            BuzzyBeesBackport.LOGGER.warn("Iron golems are drawn by {}, which takes no layers; no cracks shown.", render);
            return;
        }
        final RenderLivingBase<EntityIronGolem> living = (RenderLivingBase<EntityIronGolem>) render;
        living.addLayer(new LayerRenderer<EntityIronGolem>() {
            @Override
            public void doRenderLayer(EntityIronGolem golem, float limbSwing, float limbSwingAmount, float partialTicks,
                    float ageInTicks, float netHeadYaw, float headPitch, float scale) {
                if (!ModConfig.golems.enabled || golem.isInvisible()) {
                    return;
                }
                ResourceLocation tex = CRACKS[GolemRepair.Cracks.of(golem).ordinal()];
                if (tex == null) {
                    return;
                }
                living.bindTexture(tex);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                living.getMainModel().render(golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            }

            @Override
            public boolean shouldCombineTextures() {
                return false;
            }
        });
        BuzzyBeesBackport.LOGGER.info("Iron golem cracks added to {}", render.getClass().getName());
    }
}

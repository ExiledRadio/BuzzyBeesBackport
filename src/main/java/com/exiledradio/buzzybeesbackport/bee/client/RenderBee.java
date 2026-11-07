package com.exiledradio.buzzybeesbackport.bee.client;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.bee.EntityBee;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/** The bee's renderer: shadow 0.4, normal, angry and nectar textures (1.15's). */
public class RenderBee extends RenderLiving<EntityBee> {

    private static final ResourceLocation BEE = tex("bee");
    private static final ResourceLocation ANGRY = tex("bee_angry");
    private static final ResourceLocation NECTAR = tex("bee_nectar");
    private static final ResourceLocation ANGRY_NECTAR = tex("bee_angry_nectar");

    public RenderBee(RenderManager manager) {
        super(manager, new ModelBee(), 0.4F);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(BuzzyBeesBackport.MOD_ID, "textures/entity/bee/" + name + ".png");
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBee bee) {
        if (bee.isAngry()) {
            return bee.hasNectar() ? ANGRY_NECTAR : ANGRY;
        }
        return bee.hasNectar() ? NECTAR : BEE;
    }
}

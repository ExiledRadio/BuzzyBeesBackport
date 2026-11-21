package com.exiledradio.buzzybeesbackport.bee.client;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.babies.client.BabyModels;
import com.exiledradio.buzzybeesbackport.babies.client.model.BabyBeeModel;
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
    private static final ResourceLocation BEE_BABY = tex("bee_baby");
    private static final ResourceLocation ANGRY_BABY = tex("bee_angry_baby");
    private static final ResourceLocation NECTAR_BABY = tex("bee_nectar_baby");
    private static final ResourceLocation ANGRY_NECTAR_BABY = tex("bee_angry_nectar_baby");

    private final ModelBee adultModel;
    private final BabyBeeModel babyModel = new BabyBeeModel();

    public RenderBee(RenderManager manager) {
        super(manager, new ModelBee(), 0.4F);
        adultModel = (ModelBee) mainModel;
    }

    /** @return true when this bee draws 26.1's baby model (while Game Drops Backport's babies are on) */
    static boolean newBaby(EntityBee bee) {
        return bee.isChild() && BabyModels.active();
    }

    @Override
    public void doRender(EntityBee bee, double x, double y, double z, float yaw, float partialTicks) {
        mainModel = newBaby(bee) ? babyModel : adultModel;
        super.doRender(bee, x, y, z, yaw, partialTicks);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(BuzzyBeesBackport.MOD_ID, "textures/entity/bee/" + name + ".png");
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBee bee) {
        if (newBaby(bee)) {
            if (bee.isAngry()) {
                return bee.hasNectar() ? ANGRY_NECTAR_BABY : ANGRY_BABY;
            }
            return bee.hasNectar() ? NECTAR_BABY : BEE_BABY;
        }
        if (bee.isAngry()) {
            return bee.hasNectar() ? ANGRY_NECTAR : ANGRY;
        }
        return bee.hasNectar() ? NECTAR : BEE;
    }
}

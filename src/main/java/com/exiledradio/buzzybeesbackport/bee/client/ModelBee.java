package com.exiledradio.buzzybeesbackport.bee.client;

import com.exiledradio.buzzybeesbackport.bee.EntityBee;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/**
 * 1.15's bee model (64x64 texture): body with stinger and antennae, two wings, three pairs of legs, all
 * hanging from one root bone at y 19. Animation from 1.16.5's {@code BeeModel}: wings beat while flying,
 * the body bobs unless angry, and it rolls over toward a close target.
 */
public class ModelBee extends ModelBase {

    private final ModelRenderer bone;
    private final ModelRenderer body;
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;
    private final ModelRenderer frontLegs;
    private final ModelRenderer middleLegs;
    private final ModelRenderer backLegs;
    private final ModelRenderer stinger;
    private final ModelRenderer leftAntenna;
    private final ModelRenderer rightAntenna;
    private float rollAmount;

    public ModelBee() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.bone = new ModelRenderer(this);
        this.bone.setRotationPoint(0.0F, 19.0F, 0.0F);
        this.body = new ModelRenderer(this, 0, 0);
        this.body.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.bone.addChild(this.body);
        this.body.addBox(-3.5F, -4.0F, -5.0F, 7, 7, 10, 0.0F);
        this.stinger = new ModelRenderer(this, 26, 7);
        this.stinger.addBox(0.0F, -1.0F, 5.0F, 0, 1, 2, 0.0F);
        this.body.addChild(this.stinger);
        this.leftAntenna = new ModelRenderer(this, 2, 0);
        this.leftAntenna.setRotationPoint(0.0F, -2.0F, -5.0F);
        this.leftAntenna.addBox(1.5F, -2.0F, -3.0F, 1, 2, 3, 0.0F);
        this.rightAntenna = new ModelRenderer(this, 2, 3);
        this.rightAntenna.setRotationPoint(0.0F, -2.0F, -5.0F);
        this.rightAntenna.addBox(-2.5F, -2.0F, -3.0F, 1, 2, 3, 0.0F);
        this.body.addChild(this.leftAntenna);
        this.body.addChild(this.rightAntenna);
        this.rightWing = new ModelRenderer(this, 0, 18);
        this.rightWing.setRotationPoint(-1.5F, -4.0F, -3.0F);
        this.rightWing.rotateAngleY = -0.2618F;
        this.bone.addChild(this.rightWing);
        this.rightWing.addBox(-9.0F, 0.0F, 0.0F, 9, 0, 6, 0.001F);
        this.leftWing = new ModelRenderer(this, 0, 18);
        this.leftWing.setRotationPoint(1.5F, -4.0F, -3.0F);
        this.leftWing.rotateAngleY = 0.2618F;
        this.leftWing.mirror = true;
        this.bone.addChild(this.leftWing);
        this.leftWing.addBox(0.0F, 0.0F, 0.0F, 9, 0, 6, 0.001F);
        this.frontLegs = new ModelRenderer(this, 26, 1);
        this.frontLegs.setRotationPoint(1.5F, 3.0F, -2.0F);
        this.frontLegs.addBox(-5.0F, 0.0F, 0.0F, 7, 2, 0, 0.0F);
        this.bone.addChild(this.frontLegs);
        this.middleLegs = new ModelRenderer(this, 26, 3);
        this.middleLegs.setRotationPoint(1.5F, 3.0F, 0.0F);
        this.middleLegs.addBox(-5.0F, 0.0F, 0.0F, 7, 2, 0, 0.0F);
        this.bone.addChild(this.middleLegs);
        this.backLegs = new ModelRenderer(this, 26, 5);
        this.backLegs.setRotationPoint(1.5F, 3.0F, 2.0F);
        this.backLegs.addBox(-5.0F, 0.0F, 0.0F, 7, 2, 0, 0.0F);
        this.bone.addChild(this.backLegs);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        if (entity instanceof EntityBee) {
            this.rollAmount = ((EntityBee) entity).getRollAmount(partialTicks);
            this.stinger.showModel = !((EntityBee) entity).hasStung();
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            float scale, Entity entity) {
        this.rightWing.rotateAngleX = 0.0F;
        this.leftAntenna.rotateAngleX = 0.0F;
        this.rightAntenna.rotateAngleX = 0.0F;
        this.bone.rotateAngleX = 0.0F;
        this.bone.rotationPointY = 19.0F;
        boolean resting = entity.onGround && entity.motionX * entity.motionX + entity.motionY * entity.motionY
                + entity.motionZ * entity.motionZ < 1.0E-7D;
        if (resting) {
            this.rightWing.rotateAngleY = -0.2618F;
            this.rightWing.rotateAngleZ = 0.0F;
            this.leftWing.rotateAngleX = 0.0F;
            this.leftWing.rotateAngleY = 0.2618F;
            this.leftWing.rotateAngleZ = 0.0F;
            this.frontLegs.rotateAngleX = 0.0F;
            this.middleLegs.rotateAngleX = 0.0F;
            this.backLegs.rotateAngleX = 0.0F;
        } else {
            float beat = ageInTicks * 2.1F;
            this.rightWing.rotateAngleY = 0.0F;
            this.rightWing.rotateAngleZ = MathHelper.cos(beat) * (float) Math.PI * 0.15F;
            this.leftWing.rotateAngleX = this.rightWing.rotateAngleX;
            this.leftWing.rotateAngleY = this.rightWing.rotateAngleY;
            this.leftWing.rotateAngleZ = -this.rightWing.rotateAngleZ;
            this.frontLegs.rotateAngleX = (float) Math.PI / 4F;
            this.middleLegs.rotateAngleX = (float) Math.PI / 4F;
            this.backLegs.rotateAngleX = (float) Math.PI / 4F;
            this.bone.rotateAngleX = 0.0F;
            this.bone.rotateAngleY = 0.0F;
            this.bone.rotateAngleZ = 0.0F;
        }
        boolean angry = entity instanceof EntityBee && ((EntityBee) entity).isAngry();
        if (!angry) {
            this.bone.rotateAngleX = 0.0F;
            this.bone.rotateAngleY = 0.0F;
            this.bone.rotateAngleZ = 0.0F;
            if (!resting) {
                float bob = MathHelper.cos(ageInTicks * 0.18F);
                this.bone.rotateAngleX = 0.1F + bob * (float) Math.PI * 0.025F;
                this.leftAntenna.rotateAngleX = bob * (float) Math.PI * 0.03F;
                this.rightAntenna.rotateAngleX = bob * (float) Math.PI * 0.03F;
                this.frontLegs.rotateAngleX = -bob * (float) Math.PI * 0.1F + (float) Math.PI / 8F;
                this.backLegs.rotateAngleX = -bob * (float) Math.PI * 0.05F + (float) Math.PI / 4F;
                this.bone.rotationPointY = 19.0F - MathHelper.cos(ageInTicks * 0.18F) * 0.9F;
            }
        }
        if (this.rollAmount > 0.0F) {
            this.bone.rotateAngleX = lerpAngle(this.bone.rotateAngleX, 3.0915928F, this.rollAmount);
        }
    }

    private static float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff < -(float) Math.PI) {
            diff += (float) Math.PI * 2F;
        }
        while (diff >= (float) Math.PI) {
            diff -= (float) Math.PI * 2F;
        }
        return from + t * diff;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            // 1.15's AgeableModel(false, 24, 0): the whole bee at half size, standing on the ground.
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            this.bone.render(scale);
            GlStateManager.popMatrix();
        } else {
            this.bone.render(scale);
        }
    }
}

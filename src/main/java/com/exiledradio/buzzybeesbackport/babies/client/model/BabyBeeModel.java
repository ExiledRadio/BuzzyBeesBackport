package com.exiledradio.buzzybeesbackport.babies.client.model;

import com.exiledradio.buzzybeesbackport.bee.EntityBee;

/** 26.3's {@code BabyBeeModel} with {@code BeeModel.setupAnim}. */
public final class BabyBeeModel extends BabyModel<EntityBee> {

    private final ModelPart bone;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart frontLeg;
    private final ModelPart midLeg;
    private final ModelPart backLeg;
    private final ModelPart stinger;

    public BabyBeeModel() {
        super(createBodyLayer());
        bone = root.getChild("bone");
        stinger = bone.getChild("body").getChild("stinger");
        rightWing = bone.getChild("right_wing");
        leftWing = bone.getChild("left_wing");
        frontLeg = bone.getChild("front_legs");
        midLeg = bone.getChild("middle_legs");
        backLeg = bone.getChild("back_legs");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone",
                CubeListBuilder.create()
                        .texOffs(6, 12).addBox(1.0F, -1.6667F, -2.1633F, 1.0F, 2.0F, 2.0F)
                        .texOffs(0, 12).addBox(-2.0F, -1.6667F, -2.1933F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 19.6667F, -1.8567F));
        PartDefinition body = bone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.5F, 4.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, 1.3333F, 2.3567F));
        body.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(13, 2).addBox(0.0F, -0.5F, 0.0F, 0.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.5F, 2.5F));
        bone.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(3, 9).addBox(-3.0F, 0.0F, 0.0F, 3.0F, 0.0F, 3.0F),
                PartPose.offsetAndRotation(-1.0F, -0.6667F, 0.8567F, 0.2182F, 0.3491F, 0.0F));
        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(-3, 9).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 3.0F).mirror(false),
                PartPose.offsetAndRotation(1.0F, -0.6667F, 0.8567F, 0.2182F, -0.3491F, 0.0F));
        bone.addOrReplaceChild("front_legs", CubeListBuilder.create().texOffs(13, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 0.0F),
                PartPose.offset(0.0F, 3.3333F, 1.8567F));
        bone.addOrReplaceChild("middle_legs", CubeListBuilder.create().texOffs(13, 1).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 0.0F),
                PartPose.offset(0.0F, 3.3333F, 2.8567F));
        bone.addOrReplaceChild("back_legs", CubeListBuilder.create().texOffs(13, 2).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 0.0F),
                PartPose.offset(0.0F, 3.3333F, 3.8567F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void setupAnim(EntityBee bee, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        stinger.visible = !bee.hasStung();
        boolean onGround = bee.onGround && bee.motionX * bee.motionX + bee.motionY * bee.motionY + bee.motionZ * bee.motionZ < 1.0E-7D;
        if (!onGround) {
            float beat = ageInTicks * 120.32113F * RAD;
            rightWing.yRot = 0.0F;
            rightWing.zRot = cos(beat) * (float) Math.PI * 0.15F;
            leftWing.xRot = rightWing.xRot;
            leftWing.yRot = rightWing.yRot;
            leftWing.zRot = -rightWing.zRot;
            frontLeg.xRot = (float) (Math.PI / 4);
            midLeg.xRot = (float) (Math.PI / 4);
            backLeg.xRot = (float) (Math.PI / 4);
        }
        if (!bee.isAngry() && !onGround) {
            float bob = cos(ageInTicks * 0.18F);
            bone.xRot = 0.1F + bob * (float) Math.PI * 0.025F;
            bone.y -= cos(ageInTicks * 0.18F) * 0.9F;
            frontLeg.xRot = -bob * (float) Math.PI * 0.1F + (float) (Math.PI / 8);
            backLeg.xRot = -bob * (float) Math.PI * 0.05F + (float) (Math.PI / 4);
        }
        float roll = bee.getRollAmount(partialTicks);
        if (roll > 0.0F) {
            float diff = 3.0915928F - bone.xRot;
            while (diff < -(float) Math.PI) {
                diff += (float) (Math.PI * 2);
            }
            while (diff >= (float) Math.PI) {
                diff -= (float) (Math.PI * 2);
            }
            bone.xRot += roll * diff;
        }
    }
}

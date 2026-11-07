package com.exiledradio.buzzybeesbackport.bee.client;

import com.exiledradio.buzzybeesbackport.bee.BeeContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.15's drip particles for honey and nectar, on 1.12.2's own drip sprites (particles.png 112-114), colored
 * with 1.16.5's {@code DripParticle} values.
 *
 * <ul>
 *   <li>{@link Kind#DRIPPING_HONEY}: hangs under a full hive for 100 ticks, then falls.</li>
 *   <li>{@link Kind#FALLING_HONEY}: falls and lands as {@link Kind#LANDING_HONEY}, with the drip sound.</li>
 *   <li>{@link Kind#FALLING_NECTAR}: what a pollinated bee sheds; short, slow, no landing.</li>
 * </ul>
 */
public class ParticleBeeDrip extends Particle {

    public enum Kind {
        DRIPPING_HONEY, FALLING_HONEY, LANDING_HONEY, FALLING_NECTAR
    }

    private final Kind kind;

    public ParticleBeeDrip(World world, double x, double y, double z, Kind kind) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.kind = kind;
        this.motionX = this.motionY = this.motionZ = 0.0D;
        setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        switch (kind) {
            case DRIPPING_HONEY:
                setColor(0.622F, 0.508F, 0.082F);
                this.particleGravity *= 0.02F * 0.01F;
                this.particleMaxAge = 100;
                setParticleTextureIndex(113);
                break;
            case FALLING_HONEY:
                setColor(0.582F, 0.448F, 0.082F);
                this.particleGravity = 0.01F;
                this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
                setParticleTextureIndex(112);
                break;
            case LANDING_HONEY:
                setColor(0.522F, 0.408F, 0.082F);
                this.particleMaxAge = (int) (128.0D / (Math.random() * 0.8D + 0.2D));
                setParticleTextureIndex(114);
                break;
            default:
                setColor(0.92F, 0.782F, 0.72F);
                this.particleGravity = 0.007F;
                this.particleMaxAge = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
                setParticleTextureIndex(112);
        }
    }

    private void setColor(float r, float g, float b) {
        this.particleRed = r;
        this.particleGreen = g;
        this.particleBlue = b;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleMaxAge-- <= 0) {
            setExpired();
            if (this.kind == Kind.DRIPPING_HONEY) {
                Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleBeeDrip(this.world, this.posX, this.posY, this.posZ, Kind.FALLING_HONEY));
            }
            return;
        }
        if (this.kind == Kind.LANDING_HONEY) {
            this.motionX *= 0.98D;
            this.motionZ *= 0.98D;
            return;
        }
        this.motionY -= this.particleGravity;
        move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;
        if (this.kind == Kind.DRIPPING_HONEY) {
            this.motionX *= 0.02D;
            this.motionY *= 0.02D;
            this.motionZ *= 0.02D;
        }
        if (this.onGround && this.kind != Kind.DRIPPING_HONEY) {
            setExpired();
            if (this.kind == Kind.FALLING_HONEY) {
                Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleBeeDrip(this.world, this.posX, this.posY, this.posZ, Kind.LANDING_HONEY));
                this.world.playSound(this.posX + 0.5D, this.posY, this.posZ + 0.5D, BeeContent.HIVE_DRIP, SoundCategory.BLOCKS,
                        0.3F + this.world.rand.nextFloat() * 2.0F / 3.0F, 1.0F, false);
            }
        } else if (this.world.getBlockState(new BlockPos(this.posX, this.posY, this.posZ)).getMaterial().isLiquid()) {
            setExpired();
        }
    }
}

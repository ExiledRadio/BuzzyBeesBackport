package com.exiledradio.buzzybeesbackport.bee.client;

import com.exiledradio.buzzybeesbackport.bee.BeeContent;
import com.exiledradio.buzzybeesbackport.bee.EntityBee;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client half of the bee stream: the renderer, the drip particles and the flight buzz.
 *
 * <p>Common code reaches it only through calls that name no client type ({@link #nectar}, {@link #hiveDrip}).
 */
public final class BeeClient {

    private BeeClient() {
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityBee.class, RenderBee::new);
        MinecraftForge.EVENT_BUS.register(new BeeClient());
    }

    /** A pollinated bee sheds nectar. */
    public static void nectar(World world, double x, double y, double z) {
        Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleBeeDrip(world, x, y, z, ParticleBeeDrip.Kind.FALLING_NECTAR));
    }

    /**
     * A full hive drips honey (1.15's {@code BeehiveBlock.animateTick}): 70% of display ticks, from under the
     * hive, when the block below is not a full solid top and holds no liquid.
     */
    public static void hiveDrip(World world, BlockPos pos) {
        if (world.rand.nextFloat() < 0.3F) {
            return;
        }
        BlockPos below = pos.down();
        IBlockState under = world.getBlockState(below);
        AxisAlignedBB box = under.getCollisionBoundingBox(world, below);
        boolean fullTop = box != null && box.maxY >= 1.0D && under.isSideSolid(world, below, net.minecraft.util.EnumFacing.UP);
        if (fullTop || under.getMaterial().isLiquid()) {
            return;
        }
        double x = pos.getX() + world.rand.nextDouble();
        double z = pos.getZ() + world.rand.nextDouble();
        Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleBeeDrip(world, x, pos.getY() - 0.05D, z, ParticleBeeDrip.Kind.DRIPPING_HONEY));
    }

    /** Every bee that appears on the client gets its looping buzz (1.15's {@code BeeSound}). */
    @SubscribeEvent
    public void join(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote && event.getEntity() instanceof EntityBee) {
            Minecraft.getMinecraft().getSoundHandler().playSound(new BuzzSound((EntityBee) event.getEntity()));
        }
    }

    /** The flight buzz, louder and higher with speed; aggressive while angry. */
    static final class BuzzSound extends MovingSound {
        private final EntityBee bee;
        private final boolean angry;

        BuzzSound(EntityBee bee) {
            this(bee, bee.isAngry());
        }

        private BuzzSound(EntityBee bee, boolean angry) {
            super(angry ? BeeContent.BEE_LOOP_AGGRESSIVE : BeeContent.BEE_LOOP, SoundCategory.NEUTRAL);
            this.bee = bee;
            this.angry = angry;
            this.xPosF = (float) bee.posX;
            this.yPosF = (float) bee.posY;
            this.zPosF = (float) bee.posZ;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.0F;
        }

        @Override
        public void update() {
            if (this.bee.isDead) {
                this.donePlaying = true;
                return;
            }
            if (this.bee.isAngry() != this.angry) {
                // Swap to the other loop, as 1.15 does when a bee turns angry or calms down.
                this.donePlaying = true;
                Minecraft.getMinecraft().getSoundHandler().playSound(new BuzzSound(this.bee, this.bee.isAngry()));
                return;
            }
            this.xPosF = (float) this.bee.posX;
            this.yPosF = (float) this.bee.posY;
            this.zPosF = (float) this.bee.posZ;
            float speed = MathHelper.sqrt(this.bee.motionX * this.bee.motionX + this.bee.motionZ * this.bee.motionZ);
            if (speed >= 0.01F) {
                this.pitch = this.bee.isChild() ? 1.1F + speed * 0.6F : 0.7F + speed * 0.6F;
                this.volume = Math.min(1.2F, speed * 2.0F + 0.2F) * (this.angry ? 0.8F : 0.6F);
            } else {
                this.pitch = this.bee.isChild() ? 1.3F : 0.9F;
                this.volume = 0.0F;
            }
        }
    }
}

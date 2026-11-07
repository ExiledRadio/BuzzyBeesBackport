package com.exiledradio.buzzybeesbackport.honey;

import com.exiledradio.buzzybeesbackport.common.Advancements;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The honey block, from 1.16.5's {@code HoneyBlock} (unchanged since 1.15).
 *
 * <ul>
 *   <li>Its collision box is 1/16 in from every side and 1/16 short of the top, so an entity on it or
 *       against it is inside the block's space and gets {@link #onEntityCollision} every tick.</li>
 *   <li>On top: horizontal motion times 0.4 (1.15's speed factor). Jumps at half height:
 *       {@link HoneyEvents}, which also handles both through a carpet or slab on top.</li>
 *   <li>Against a side while falling faster than 0.08 a tick: the fall is capped at 0.05 a tick, fall
 *       distance resets, with sounds and particles - and the <i>Sticky Situation</i> advancement.</li>
 *   <li>Landing: a fifth of the fall damage.</li>
 *   <li>Pistons move it and what touches it ({@link #isStickyBlock}).</li>
 * </ul>
 */
public class BlockHoney extends Block {

    /** 1.15's speed factor for an entity standing on honey. */
    public static final double SPEED_FACTOR = 0.4D;
    /** 1.15's jump factor. */
    public static final double JUMP_FACTOR = 0.5D;

    private static final AxisAlignedBB COLLISION = new AxisAlignedBB(1.0D / 16, 0.0D, 1.0D / 16, 15.0D / 16, 15.0D / 16, 15.0D / 16);

    public BlockHoney() {
        super(Material.CLAY, MapColor.ADOBE);
        setHardness(0.0F);
        setSoundType(HoneyContent.HONEY);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return COLLISION;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isStickyBlock(IBlockState state) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    /** Like glass and slime: no faces between two honey blocks. */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("deprecation")
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (world.getBlockState(pos.offset(side)).getBlock() == this) {
            return false;
        }
        return super.shouldSideBeRendered(state, world, pos, side);
    }

    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        entity.playSound(HoneyContent.HONEY_SLIDE, 1.0F, 1.0F);
        if (!world.isRemote) {
            particles(world, entity, 10);
        }
        entity.fall(fallDistance, 0.2F);
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (isSliding(pos, entity)) {
            if (entity instanceof EntityPlayerMP && world.getTotalWorldTime() % 20L == 0L) {
                Advancements.grant((EntityPlayerMP) entity, "adventure/honey_block_slide");
            }
            if (entity.motionY < -0.13D) {
                double scale = -0.05D / entity.motionY;
                entity.motionX *= scale;
                entity.motionZ *= scale;
            }
            entity.motionY = -0.05D;
            entity.fallDistance = 0.0F;
            if (isSlideEffected(entity)) {
                if (world.rand.nextInt(5) == 0) {
                    entity.playSound(HoneyContent.HONEY_SLIDE, 1.0F, 1.0F);
                }
                if (!world.isRemote && world.rand.nextInt(5) == 0) {
                    particles(world, entity, 5);
                }
            }
        } else if (isOnTop(pos, entity)) {
            entity.motionX *= SPEED_FACTOR;
            entity.motionZ *= SPEED_FACTOR;
        }
        super.onEntityCollision(world, pos, state, entity);
    }

    /** 1.15's test: in the air, below the top, falling, and touching a side rather than over the middle. */
    static boolean isSliding(BlockPos pos, Entity entity) {
        if (entity.onGround) {
            return false;
        }
        if (entity.posY > pos.getY() + 0.9375D - 1.0E-7D) {
            return false;
        }
        if (entity.motionY >= -0.08D) {
            return false;
        }
        double dx = Math.abs(pos.getX() + 0.5D - entity.posX);
        double dz = Math.abs(pos.getZ() + 0.5D - entity.posZ);
        double edge = 0.4375D + entity.width / 2.0F;
        return dx + 1.0E-7D > edge || dz + 1.0E-7D > edge;
    }

    /** The entity's own block is this one: 1.15 applies the speed factor of the block an entity is in. */
    private static boolean isOnTop(BlockPos pos, Entity entity) {
        return Math.floor(entity.posX) == pos.getX() && Math.floor(entity.posZ) == pos.getZ()
                && Math.floor(entity.posY) == pos.getY();
    }

    private static boolean isSlideEffected(Entity entity) {
        return entity instanceof EntityLivingBase || entity instanceof EntityMinecart
                || entity instanceof EntityTNTPrimed || entity instanceof EntityBoat;
    }

    /** The bits of honey 1.15 shows sliding (5) and landing (10), sent to everyone watching. */
    static void particles(World world, Entity entity, int count) {
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, entity.posX, entity.posY, entity.posZ,
                    count, 0.0D, 0.0D, 0.0D, 0.0D, Block.getStateId(HoneyContent.honeyBlock.getDefaultState()));
        }
    }
}

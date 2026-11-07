package com.exiledradio.buzzybeesbackport.honey;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Honey blocks moved by pistons, as 1.15.
 *
 * <ol>
 *   <li><b>Carrying</b>: a honey block pushed sideways takes the entities standing on it along (1.15's
 *       {@code PistonTileEntity} moves every entity on the ground whose middle is over the moving block).
 *       1.12.2's piston only pushes what the block runs into. Done at the start of each tick, before the
 *       piston's own update, by the same 0.5-block step.</li>
 *   <li><b>No launching</b>: 1.12.2's piston throws entities hit by any block that says
 *       {@code isStickyBlock}, which was meant for slime. Honey must not bounce them, so at the end of the
 *       tick the launch (motion set to exactly the push direction's 1.0) is taken back off the entities
 *       around a moving honey block.</li>
 * </ol>
 * Runs on the server world (from {@link HoneyEvents}) and on the client world (from the client class),
 * because each side moves its own copy of the player.
 */
public final class HoneyPistons {

    /** Each world's honey pistons seen at the start of this tick. */
    private static final Map<World, List<TileEntityPiston>> MOVING = Collections.synchronizedMap(new WeakHashMap<World, List<TileEntityPiston>>());

    /**
     * The piston's progress, 0 to 1. Read from the field: {@code getProgress(float)} is client-only, and a
     * dedicated server has no such method.
     */
    private static final java.lang.reflect.Field PROGRESS =
            net.minecraftforge.fml.relauncher.ReflectionHelper.findField(TileEntityPiston.class, "progress", "field_145873_m");

    private HoneyPistons() {
    }

    static float progress(TileEntityPiston piston) {
        try {
            return PROGRESS.getFloat(piston);
        } catch (IllegalAccessException e) {
            return 1.0F;
        }
    }

    /** Start of a world tick: carry what stands on moving honey, and remember the pistons for {@link #after}. */
    public static void before(World world) {
        if (HoneyContent.honeyBlock == null) {
            return;
        }
        List<TileEntityPiston> found = null;
        List<TileEntity> tiles = world.loadedTileEntityList;
        for (int i = 0; i < tiles.size(); i++) {
            TileEntity tile = tiles.get(i);
            if (!(tile instanceof TileEntityPiston)) {
                continue;
            }
            TileEntityPiston piston = (TileEntityPiston) tile;
            if (piston.getPistonState().getBlock() != HoneyContent.honeyBlock || progress(piston) >= 1.0F) {
                continue;
            }
            if (found == null) {
                found = new ArrayList<TileEntityPiston>();
            }
            found.add(piston);
            carry(world, piston);
        }
        if (found == null) {
            MOVING.remove(world);
        } else {
            MOVING.put(world, found);
        }
    }

    /** End of a world tick: undo the slime launch the piston gave entities around moving honey. */
    public static void after(World world) {
        List<TileEntityPiston> pistons = MOVING.remove(world);
        if (pistons == null) {
            return;
        }
        for (TileEntityPiston piston : pistons) {
            EnumFacing dir = direction(piston);
            AxisAlignedBB around = box(piston, progress(piston)).grow(1.0D);
            for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(null, around)) {
                switch (dir.getAxis()) {
                    case X:
                        if (entity.motionX == dir.getXOffset()) {
                            entity.motionX = 0.0D;
                        }
                        break;
                    case Y:
                        if (entity.motionY == dir.getYOffset()) {
                            entity.motionY = 0.0D;
                        }
                        break;
                    default:
                        if (entity.motionZ == dir.getZOffset()) {
                            entity.motionZ = 0.0D;
                        }
                }
            }
        }
    }

    private static void carry(World world, TileEntityPiston piston) {
        EnumFacing dir = direction(piston);
        if (!dir.getAxis().isHorizontal()) {
            return;
        }
        float progress = progress(piston);
        double step = Math.min(progress + 0.5F, 1.0F) - progress;
        if (step <= 0.0D) {
            return;
        }
        // Above the honey's 15/16 top, up to half a block over the block above it (1.15's box).
        net.minecraft.util.math.Vec3d at = offset(piston, progress);
        AxisAlignedBB top = new AxisAlignedBB(0.0D, 15.0D / 16, 0.0D, 1.0D, 1.5000000999999998D, 1.0D)
                .offset(at.x, at.y, at.z);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(null, top)) {
            if (entity.onGround && entity.posX >= top.minX && entity.posX <= top.maxX
                    && entity.posZ >= top.minZ && entity.posZ <= top.maxZ
                    && entity.getPushReaction() == net.minecraft.block.material.EnumPushReaction.NORMAL) {
                entity.move(MoverType.PISTON, step * dir.getXOffset(), 0.0D, step * dir.getZOffset());
            }
        }
    }

    /** The way the block itself moves: the piston's facing when extending, the other way when pulled back. */
    static EnumFacing direction(TileEntityPiston piston) {
        return piston.isExtending() ? piston.getFacing() : piston.getFacing().getOpposite();
    }

    /** 1.12.2's {@code moveByPositionAndProgress} offset for a progress. */
    private static net.minecraft.util.math.Vec3d offset(TileEntityPiston piston, float progress) {
        double along = piston.isExtending() ? progress - 1.0F : 1.0F - progress;
        EnumFacing facing = piston.getFacing();
        BlockPos pos = piston.getPos();
        return new net.minecraft.util.math.Vec3d(pos.getX() + along * facing.getXOffset(),
                pos.getY() + along * facing.getYOffset(), pos.getZ() + along * facing.getZOffset());
    }

    private static AxisAlignedBB box(TileEntityPiston piston, float progress) {
        net.minecraft.util.math.Vec3d at = offset(piston, progress);
        return new AxisAlignedBB(0, 0, 0, 1, 1, 1).offset(at.x, at.y, at.z);
    }
}

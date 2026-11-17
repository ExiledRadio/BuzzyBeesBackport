package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Hooks for three of 1.15's smaller changes, each called from its patch (docs/PATCHES.md):
 *
 * <ul>
 *   <li>P7 {@link #portalSearchTop}: an entity coming through a portal finds a portal in the top half of the Nether.</li>
 *   <li>P8 {@link #neighbors}: mobs keep away from lava when they find a path.</li>
 *   <li>P9 {@link #killsFarmland}: a block a piston pushes over farmland no longer turns it to dirt.</li>
 * </ul>
 * Each answers as 1.12.2 does with its switch (or the whole tweaks stream) off.
 */
public final class WorldTweaks {

    private WorldTweaks() {
    }

    // -------------------------------------------------------------------------------------- P7 portals

    /**
     * {@code Teleporter.placeInExistingPortal} searches each column from {@code world.getActualHeight() - 1} down; this
     * replaces that height. The Nether's actual height is 128, so 1.12.2 never finds a portal built above y 128 there
     * and builds a new one. 1.15 finds them (its portal search covers the whole world height), so the search starts at
     * the top of the world.
     */
    public static int portalSearchTop(World world) {
        int actual = world.getActualHeight();
        if (!Tweaks.on() || !ModConfig.tweaks.portalLinkHeight) {
            return actual;
        }
        return Math.max(actual, world.getHeight());
    }

    // ------------------------------------------------------------------------------------------ P8 lava

    /**
     * Run first in {@code WalkNodeProcessor.checkNeighborBlocks}; null lets 1.12.2's run. 1.15's
     * {@code checkNeighbourBlocks}: a walkable node next to cactus, fire or lava is a danger, and 1.15 looks at the
     * 26 blocks around (above and below too), not only the 8 at the same height, and counts lava with fire. A danger
     * costs a mob 8 to cross, so paths keep a block away from lava where they can. Forge's own path types of modded
     * blocks count as in 1.12.2.
     */
    @Nullable
    public static PathNodeType neighbors(IBlockAccess world, int x, int y, int z, PathNodeType type, @Nullable EntityLiving entity) {
        if (!Tweaks.on() || !ModConfig.tweaks.lavaPathing) {
            return null;
        }
        if (type != PathNodeType.WALKABLE) {
            return type;
        }
        BlockPos.PooledMutableBlockPos at = BlockPos.PooledMutableBlockPos.retain();
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        IBlockState state = world.getBlockState(at.setPos(x + dx, y + dy, z + dz));
                        Block block = state.getBlock();
                        PathNodeType own = block.getAiPathNodeType(state, world, at, entity);
                        if (block == Blocks.CACTUS || own == PathNodeType.DAMAGE_CACTUS) {
                            type = PathNodeType.DANGER_CACTUS;
                        } else if (block == Blocks.FIRE || state.getMaterial() == Material.LAVA || own == PathNodeType.DAMAGE_FIRE) {
                            type = PathNodeType.DANGER_FIRE;
                        } else if (own == PathNodeType.DAMAGE_OTHER) {
                            type = PathNodeType.DANGER_OTHER;
                        }
                    }
                }
            }
        } finally {
            at.release();
        }
        return type;
    }

    // -------------------------------------------------------------------------------------- P9 farmland

    /**
     * {@code BlockFarmland}'s two "is the block above solid" checks (on a neighbor change and when placed) ask this
     * instead. 1.15's farmland also survives a block being moved over it by a piston (the moving block, solid in
     * 1.12.2, turned it to dirt on its way), so blocks that can be placed on farmland can be pushed onto it too.
     */
    public static boolean killsFarmland(IBlockState above) {
        if (!above.getMaterial().isSolid()) {
            return false;
        }
        return !(Tweaks.on() && ModConfig.tweaks.farmlandPistons && above.getBlock() instanceof BlockPistonMoving);
    }
}

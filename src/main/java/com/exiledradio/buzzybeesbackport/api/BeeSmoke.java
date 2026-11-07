package com.exiledradio.buzzybeesbackport.api;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * What calms bees under a hive: 1.15's {@code CampfireBlock.isSmokeyPos}, and fire directly beneath.
 *
 * <p>The campfire is Village &amp; Pillage's (Rule 2: used when it is installed, read by name and its
 * {@code lit} property). Other mods add theirs - Nether Update's soul campfire - with {@link #addSource}.
 */
public final class BeeSmoke {

    private static final ResourceLocation CAMPFIRE = new ResourceLocation("villagerbackport", "campfire");
    private static final List<Predicate<IBlockState>> SOURCES = new ArrayList<Predicate<IBlockState>>();
    /** 1.15's smoke column: a solid block that fills the middle of the column stops the smoke. */
    private static final AxisAlignedBB COLUMN = new AxisAlignedBB(6.0D / 16, 0.0D, 6.0D / 16, 10.0D / 16, 1.0D, 10.0D / 16);

    private BeeSmoke() {
    }

    /** Registers another lit campfire-like block. */
    public static void addSource(Predicate<IBlockState> source) {
        SOURCES.add(source);
    }

    /** @return true for a lit campfire */
    public static boolean isSmokeSource(IBlockState state) {
        Block block = state.getBlock();
        if (CAMPFIRE.equals(block.getRegistryName())) {
            for (IProperty<?> p : state.getPropertyKeys()) {
                if ("lit".equals(p.getName())) {
                    return Boolean.TRUE.equals(state.getValue(p));
                }
            }
        }
        for (Predicate<IBlockState> source : SOURCES) {
            if (source.test(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if bees in a hive at this position stay calm: a lit campfire within 5 blocks below whose
     * smoke reaches it (it passes through one solid block only when that block sits right on the campfire),
     * or fire right under the hive
     */
    public static boolean isSmokey(World world, BlockPos hive) {
        if (world.getBlockState(hive.down()).getBlock() instanceof BlockFire) {
            return true;
        }
        for (int i = 1; i <= 5; i++) {
            BlockPos at = hive.down(i);
            IBlockState state = world.getBlockState(at);
            if (isSmokeSource(state)) {
                return true;
            }
            if (blocksSmoke(world, at, state)) {
                return isSmokeSource(world.getBlockState(at.down()));
            }
        }
        return false;
    }

    private static boolean blocksSmoke(World world, BlockPos at, IBlockState state) {
        AxisAlignedBB box = state.getCollisionBoundingBox(world, at);
        return box != null && box != Block.NULL_AABB && box.intersects(COLUMN);
    }
}

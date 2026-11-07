package com.exiledradio.buzzybeesbackport.honey;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockPistonStructureHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Called from the patched {@code BlockPistonStructureHelper} (patch P1, {@code asm/PistonStickTransformer}):
 * 1.15's rule that a honey block and a slime block do not stick to each other. Every other pair of blocks
 * sticks exactly as before.
 */
public final class PistonStick {

    private static final Field WORLD = ReflectionHelper.findField(BlockPistonStructureHelper.class, "world", "field_177261_a");
    private static final Field MOVE_DIRECTION = ReflectionHelper.findField(BlockPistonStructureHelper.class, "moveDirection", "field_177257_d");
    private static final Method ADD_BLOCK_LINE = ReflectionHelper.findMethod(BlockPistonStructureHelper.class, "addBlockLine",
            "func_177251_a", BlockPos.class, EnumFacing.class);

    private PistonStick() {
    }

    /** The kind of glue a sticky block has: slime, honey, or none of these two (sticks to everything). */
    private static int glue(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.SLIME_BLOCK) {
            return 1;
        }
        if (HoneyContent.honeyBlock != null && block == HoneyContent.honeyBlock) {
            return 2;
        }
        return 0;
    }

    /** @return false only for honey against slime (either way round), as 1.15's {@code canStick} */
    public static boolean canStick(IBlockState a, IBlockState b) {
        int ga = glue(a);
        int gb = glue(b);
        return ga == 0 || gb == 0 || ga == gb;
    }

    /** Replaces {@code addBlockLine(pos, side)} in {@code addBranchingBlocks}: a neighbor that cannot stick is left. */
    public static boolean branch(BlockPistonStructureHelper helper, BlockPos pos, EnumFacing side) {
        try {
            World world = (World) WORLD.get(helper);
            BlockPos from = pos.offset(side.getOpposite());
            if (!canStick(world.getBlockState(from), world.getBlockState(pos))) {
                return true;
            }
            return (Boolean) ADD_BLOCK_LINE.invoke(helper, pos, side);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        }
    }

    /**
     * Replaces {@code isAir} in the loop collecting what a sticky block pulls behind it: the line also ends where
     * the next block cannot stick to the one in front of it.
     */
    public static boolean airOrUnstuck(Block block, IBlockState state, IBlockAccess world, BlockPos pos, BlockPistonStructureHelper helper) {
        if (block.isAir(state, world, pos)) {
            return true;
        }
        try {
            EnumFacing move = (EnumFacing) MOVE_DIRECTION.get(helper);
            return !canStick(world.getBlockState(pos.offset(move)), state);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

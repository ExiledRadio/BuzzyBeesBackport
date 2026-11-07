package com.exiledradio.buzzybeesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch P1 (docs/PATCHES.md): honey and slime blocks do not pull each other in a piston contraption, as
 * 1.15's {@code PistonBlockStructureHelper.canStick}.
 *
 * <p>Two call sites in {@code BlockPistonStructureHelper}, each turned into a call to
 * {@code honey.PistonStick}:
 * <ol>
 *   <li>in {@code addBranchingBlocks}, the call {@code addBlockLine(neighbor, side)} that drags a block
 *       touching a sticky one becomes {@code PistonStick.branch(this, neighbor, side)}, which skips a
 *       neighbor that cannot stick to the sticky block;</li>
 *   <li>in {@code addBlockLine}, the {@code isAir} test of the loop that collects the blocks a sticky block
 *       pulls behind it (the second {@code isAir} call in the method) becomes
 *       {@code PistonStick.airOrUnstuck(..., this)}, which also ends the line where the next block cannot
 *       stick to the one in front of it.</li>
 * </ol>
 * Both keep vanilla's behavior for every other pair of blocks. If the method shapes differ (another mod
 * rewrote them), the patch is skipped and logged, never half-applied.
 */
public final class PistonStickTransformer implements IClassTransformer {

    private static final Logger LOG = LogManager.getLogger("Buzzy Bees Backport Core");
    private static final String TARGET = "net.minecraft.block.state.BlockPistonStructureHelper";
    private static final String HELPER = "net/minecraft/block/state/BlockPistonStructureHelper";
    private static final String HOOKS = "com/exiledradio/buzzybeesbackport/honey/PistonStick";
    private static final String ADD_LINE_DESC = "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Z";
    private static final String IS_AIR_DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Z";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        MethodNode branching = null;
        MethodNode line = null;
        for (MethodNode m : node.methods) {
            if (("addBranchingBlocks".equals(m.name) || "func_177250_b".equals(m.name)) && "(Lnet/minecraft/util/math/BlockPos;)Z".equals(m.desc)) {
                branching = m;
            } else if (("addBlockLine".equals(m.name) || "func_177251_a".equals(m.name)) && ADD_LINE_DESC.equals(m.desc)) {
                line = m;
            }
        }
        if (branching == null || line == null) {
            LOG.warn("P1 honey/slime pistons: methods not found in {}; patch skipped.", TARGET);
            return basicClass;
        }
        List<MethodInsnNode> addCalls = new ArrayList<MethodInsnNode>();
        for (AbstractInsnNode insn : branching.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (HELPER.equals(call.owner) && ADD_LINE_DESC.equals(call.desc)
                        && ("addBlockLine".equals(call.name) || "func_177251_a".equals(call.name))) {
                    addCalls.add(call);
                }
            }
        }
        List<MethodInsnNode> airCalls = new ArrayList<MethodInsnNode>();
        for (AbstractInsnNode insn : line.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("isAir".equals(call.name) && IS_AIR_DESC.equals(call.desc) && "net/minecraft/block/Block".equals(call.owner)) {
                    airCalls.add(call);
                }
            }
        }
        if (addCalls.size() != 1 || airCalls.size() != 3) {
            LOG.warn("P1 honey/slime pistons: unexpected shape ({} addBlockLine calls, {} isAir calls); patch skipped.",
                    addCalls.size(), airCalls.size());
            return basicClass;
        }
        // 1. addBranchingBlocks: this.addBlockLine(pos, side) -> PistonStick.branch(this, pos, side)
        MethodInsnNode add = addCalls.get(0);
        branching.instructions.set(add, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "branch",
                "(L" + HELPER + ";Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Z", false));
        // 2. addBlockLine's pull-behind loop: block.isAir(state, world, pos) -> PistonStick.airOrUnstuck(block, state, world, pos, this)
        MethodInsnNode air = airCalls.get(1);
        InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "airOrUnstuck",
                "(Lnet/minecraft/block/Block;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;L"
                        + HELPER + ";)Z", false));
        line.instructions.insertBefore(air, replacement);
        line.instructions.remove(air);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        applied = true;
        LOG.info("P1 honey/slime pistons: patched {}.", TARGET);
        return writer.toByteArray();
    }
}

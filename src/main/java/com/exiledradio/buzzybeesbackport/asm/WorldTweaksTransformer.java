package com.exiledradio.buzzybeesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patches P7, P8 and P9 (docs/PATCHES.md), whose hooks are {@code tweaks.WorldTweaks}:
 *
 * <ul>
 *   <li><b>P7</b> {@code Teleporter.placeInExistingPortal}: its one {@code world.getActualHeight()} (the top of each
 *       column searched) becomes {@code WorldTweaks.portalSearchTop(world)}.</li>
 *   <li><b>P8</b> {@code WalkNodeProcessor.checkNeighborBlocks} starts with
 *       {@code PathNodeType t = WorldTweaks.neighbors(world, x, y, z, type, this.entity); if (t != null) return t;}
 *       (one branch, with its frame).</li>
 *   <li><b>P9</b> {@code BlockFarmland.neighborChanged} and {@code onBlockAdded}: each
 *       {@code state.getMaterial().isSolid()} of the block above becomes {@code WorldTweaks.killsFarmland(state)}.</li>
 * </ul>
 * Each finds its instructions by owner, name and descriptor and changes nothing if they are not exactly there.
 */
public final class WorldTweaksTransformer implements IClassTransformer {

    private static final String HOOKS = "com/exiledradio/buzzybeesbackport/tweaks/WorldTweaks";

    public static volatile boolean appliedP7;
    public static volatile boolean appliedP8;
    public static volatile boolean appliedP9;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if ("net.minecraft.world.Teleporter".equals(transformedName)) {
            return portal(basicClass);
        }
        if ("net.minecraft.pathfinding.WalkNodeProcessor".equals(transformedName)) {
            return lava(basicClass);
        }
        if ("net.minecraft.block.BlockFarmland".equals(transformedName)) {
            return farmland(basicClass);
        }
        return basicClass;
    }

    private static byte[] portal(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "placeInExistingPortal", "func_180620_b", "(Lnet/minecraft/entity/Entity;F)Z");
        List<AbstractInsnNode> heights = m == null ? null : Asm.calls(m, "getActualHeight", "func_72940_L", "()I");
        if (heights == null || heights.size() != 1 || heights.get(0).getOpcode() != Opcodes.INVOKEVIRTUAL) {
            Asm.LOG.warn("P7 portal search height: Teleporter.placeInExistingPortal is not as expected; patch skipped.");
            return bytes;
        }
        m.instructions.set(heights.get(0), new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "portalSearchTop",
                "(Lnet/minecraft/world/World;)I", false));
        appliedP7 = true;
        Asm.LOG.info("P7 portal search height: patched Teleporter.placeInExistingPortal.");
        return Asm.write(node);
    }

    private static byte[] lava(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        String type = "Lnet/minecraft/pathfinding/PathNodeType;";
        MethodNode m = Asm.method(node, "checkNeighborBlocks", "func_193578_a", "(Lnet/minecraft/world/IBlockAccess;III" + type + ")" + type);
        FieldInsnNode entity = null;
        if (m != null) {
            for (AbstractInsnNode insn : m.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.GETFIELD && "Lnet/minecraft/entity/EntityLiving;".equals(((FieldInsnNode) insn).desc)
                        && ("currentEntity".equals(((FieldInsnNode) insn).name) || "entity".equals(((FieldInsnNode) insn).name)
                        || "field_186326_b".equals(((FieldInsnNode) insn).name))) {
                    entity = (FieldInsnNode) insn;
                }
            }
        }
        if (m == null || entity == null) {
            Asm.LOG.warn("P8 mobs avoid lava: WalkNodeProcessor.checkNeighborBlocks is not as expected; patch skipped.");
            return bytes;
        }
        LabelNode vanilla = new LabelNode();
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 1));
        head.add(new VarInsnNode(Opcodes.ILOAD, 2));
        head.add(new VarInsnNode(Opcodes.ILOAD, 3));
        head.add(new VarInsnNode(Opcodes.ILOAD, 4));
        head.add(new VarInsnNode(Opcodes.ALOAD, 5));
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new FieldInsnNode(Opcodes.GETFIELD, entity.owner, entity.name, entity.desc));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "neighbors",
                "(Lnet/minecraft/world/IBlockAccess;III" + type + "Lnet/minecraft/entity/EntityLiving;)" + type, false));
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new JumpInsnNode(Opcodes.IFNULL, vanilla));
        head.add(new InsnNode(Opcodes.ARETURN));
        head.add(vanilla);
        head.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[] {"net/minecraft/pathfinding/PathNodeType"}));
        head.add(new InsnNode(Opcodes.POP));
        m.instructions.insert(head);
        appliedP8 = true;
        Asm.LOG.info("P8 mobs avoid lava: patched WalkNodeProcessor.checkNeighborBlocks.");
        return Asm.write(node);
    }

    private static byte[] farmland(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        String world = "Lnet/minecraft/world/World;";
        String pos = "Lnet/minecraft/util/math/BlockPos;";
        String state = "Lnet/minecraft/block/state/IBlockState;";
        MethodNode changed = Asm.method(node, "neighborChanged", "func_189540_a", "(" + state + world + pos + "Lnet/minecraft/block/Block;" + pos + ")V");
        MethodNode added = Asm.method(node, "onBlockAdded", "func_176213_c", "(" + world + pos + state + ")V");
        List<AbstractInsnNode[]> pairs = new ArrayList<AbstractInsnNode[]>();
        for (MethodNode m : new MethodNode[] {changed, added}) {
            if (m == null) {
                continue;
            }
            for (AbstractInsnNode insn : m.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEINTERFACE && Asm.isCall(insn, "getMaterial", "func_185904_a", "()Lnet/minecraft/block/material/Material;")) {
                    AbstractInsnNode next = insn.getNext();
                    while (next != null && next.getOpcode() < 0) {
                        next = next.getNext();
                    }
                    if (next != null && next.getOpcode() == Opcodes.INVOKEVIRTUAL && Asm.isCall(next, "isSolid", "func_76220_a", "()Z")) {
                        pairs.add(new AbstractInsnNode[] {insn, next});
                    }
                }
            }
        }
        if (changed == null || added == null || pairs.size() != 2) {
            Asm.LOG.warn("P9 farmland and pistons: BlockFarmland is not as expected ({} solid checks); patch skipped.", pairs.size());
            return bytes;
        }
        for (AbstractInsnNode[] pair : pairs) {
            MethodNode m = changed.instructions.contains(pair[0]) ? changed : added;
            m.instructions.remove(pair[0]);
            m.instructions.set(pair[1], new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "killsFarmland", "(" + state + ")Z", false));
        }
        appliedP9 = true;
        Asm.LOG.info("P9 farmland and pistons: patched BlockFarmland.");
        return Asm.write(node);
    }
}

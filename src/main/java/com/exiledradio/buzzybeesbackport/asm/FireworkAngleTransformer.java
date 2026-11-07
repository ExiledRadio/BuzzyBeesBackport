package com.exiledradio.buzzybeesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch P4 (docs/PATCHES.md): a firework rocket "shot at an angle" (1.15's {@code shotAtAngle}, here: fired
 * from a dispenser) flies straight at its speed instead of speeding up sideways and rising.
 *
 * <p>{@code EntityFireworkRocket.onUpdate}, when the rocket boosts no one, runs
 * {@code motionX *= 1.15; motionZ *= 1.15; motionY += 0.04;} and then {@code move(SELF, motion)}. Those three
 * statements become {@code tweaks.DispensedFireworks.accelerate(this)}, which does exactly the same for
 * every other rocket and nothing for one shot at an angle (1.15's {@code if (!isShotAtAngle())}).
 */
public final class FireworkAngleTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.entity.item.EntityFireworkRocket";
    private static final String HOOKS = "com/exiledradio/buzzybeesbackport/tweaks/DispensedFireworks";

    /** Set once the class has been patched; the self-test and the dispenser behavior read it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode update = Asm.method(node, "onUpdate", "func_70071_h_", "()V");
        if (update == null) {
            Asm.LOG.warn("P4 dispensed fireworks: onUpdate not found; patch skipped.");
            return basicClass;
        }
        List<AbstractInsnNode> speedUps = new ArrayList<AbstractInsnNode>();
        List<AbstractInsnNode> rises = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode insn : update.instructions.toArray()) {
            if (insn instanceof LdcInsnNode && Double.valueOf(1.15D).equals(((LdcInsnNode) insn).cst)) {
                speedUps.add(insn);
            } else if (insn instanceof LdcInsnNode && Double.valueOf(0.04D).equals(((LdcInsnNode) insn).cst)) {
                rises.add(insn);
            }
        }
        List<AbstractInsnNode> moves = Asm.calls(update, "move", "func_70091_d", "(Lnet/minecraft/entity/MoverType;DDD)V");
        if (speedUps.size() != 2 || rises.size() != 1 || moves.size() != 1) {
            Asm.LOG.warn("P4 dispensed fireworks: unexpected shape ({} x1.15, {} +0.04, {} move calls); patch skipped.",
                    speedUps.size(), rises.size(), moves.size());
            return basicClass;
        }
        // Start: the "this" of "this.motionX *= 1.15" (aload 0, dup, getfield motionX, ldc 1.15).
        AbstractInsnNode getX = Asm.previous(speedUps.get(0));
        AbstractInsnNode dup = getX == null ? null : Asm.previous(getX);
        AbstractInsnNode start = dup == null ? null : Asm.previous(dup);
        // End: the "this" of "this.move(MoverType.SELF, ...)" (aload 0, getstatic SELF).
        AbstractInsnNode end = null;
        for (AbstractInsnNode insn = start; insn != null && insn != moves.get(0); insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.GETSTATIC
                    && "net/minecraft/entity/MoverType".equals(((FieldInsnNode) insn).owner)) {
                end = Asm.previous(insn);
                break;
            }
        }
        if (start == null || end == null || start.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) start).var != 0
                || dup.getOpcode() != Opcodes.DUP || getX.getOpcode() != Opcodes.GETFIELD
                || end.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) end).var != 0 || !between(start, rises.get(0), end)
                || !between(start, speedUps.get(1), end) || !straight(start, end)) {
            Asm.LOG.warn("P4 dispensed fireworks: the speed-up is not where expected; patch skipped.");
            return basicClass;
        }
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "accelerate", "(Lnet/minecraft/entity/item/EntityFireworkRocket;)V", false));
        update.instructions.insertBefore(start, hook);
        // Remove the three statements' instructions, keeping their labels and line numbers.
        AbstractInsnNode insn = start;
        while (insn != end) {
            AbstractInsnNode next = insn.getNext();
            if (insn.getOpcode() >= 0) {
                update.instructions.remove(insn);
            }
            insn = next;
        }
        applied = true;
        Asm.LOG.info("P4 dispensed fireworks: patched {}.", TARGET);
        return Asm.write(node);
    }

    private static boolean between(AbstractInsnNode from, AbstractInsnNode target, AbstractInsnNode to) {
        for (AbstractInsnNode insn = from; insn != null && insn != to; insn = insn.getNext()) {
            if (insn == target) {
                return true;
            }
        }
        return false;
    }

    /** No jumps, frames or jump targets inside the range, so removing it cannot break control flow. */
    private static boolean straight(AbstractInsnNode from, AbstractInsnNode to) {
        for (AbstractInsnNode insn = from; insn != null && insn != to; insn = insn.getNext()) {
            int type = insn.getType();
            if (type == AbstractInsnNode.JUMP_INSN || type == AbstractInsnNode.FRAME || type == AbstractInsnNode.TABLESWITCH_INSN
                    || type == AbstractInsnNode.LOOKUPSWITCH_INSN || insn.getOpcode() == Opcodes.ATHROW) {
                return false;
            }
        }
        return true;
    }
}

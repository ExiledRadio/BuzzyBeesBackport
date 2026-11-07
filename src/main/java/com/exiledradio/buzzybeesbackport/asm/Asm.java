package com.exiledradio.buzzybeesbackport.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers shared by the patches P2 onwards. Every name is matched as MCP (the dev environment) or SRG
 * (a real instance), never by position alone.
 */
final class Asm {

    static final Logger LOG = LogManager.getLogger("Buzzy Bees Backport Core");

    private Asm() {
    }

    static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    /** Writes a class back without recomputing frames (no patch here adds a jump). */
    static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    static MethodNode method(ClassNode node, String mcp, String srg, String desc) {
        for (MethodNode m : node.methods) {
            if ((mcp.equals(m.name) || srg.equals(m.name)) && desc.equals(m.desc)) {
                return m;
            }
        }
        return null;
    }

    static boolean isCall(AbstractInsnNode insn, String mcp, String srg, String desc) {
        if (!(insn instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) insn;
        return (mcp.equals(call.name) || srg.equals(call.name)) && desc.equals(call.desc);
    }

    static boolean isField(AbstractInsnNode insn, int opcode, String mcp, String srg, String desc) {
        if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != opcode) {
            return false;
        }
        FieldInsnNode field = (FieldInsnNode) insn;
        return (mcp.equals(field.name) || srg.equals(field.name)) && desc.equals(field.desc);
    }

    static List<AbstractInsnNode> calls(MethodNode m, String mcp, String srg, String desc) {
        List<AbstractInsnNode> out = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (isCall(insn, mcp, srg, desc)) {
                out.add(insn);
            }
        }
        return out;
    }

    /** The previous real instruction (skipping labels, line numbers and frames), or null. */
    static AbstractInsnNode previous(AbstractInsnNode insn) {
        AbstractInsnNode p = insn.getPrevious();
        while (p != null && p.getOpcode() < 0) {
            p = p.getPrevious();
        }
        return p;
    }

    /** The int a BIPUSH/SIPUSH/ICONST pushes, or null for anything else. */
    static Integer intConstant(AbstractInsnNode insn) {
        if (insn == null) {
            return null;
        }
        int op = insn.getOpcode();
        if (op >= org.objectweb.asm.Opcodes.ICONST_M1 && op <= org.objectweb.asm.Opcodes.ICONST_5) {
            return op - org.objectweb.asm.Opcodes.ICONST_0;
        }
        if (insn instanceof org.objectweb.asm.tree.IntInsnNode
                && (op == org.objectweb.asm.Opcodes.BIPUSH || op == org.objectweb.asm.Opcodes.SIPUSH)) {
            return ((org.objectweb.asm.tree.IntInsnNode) insn).operand;
        }
        return null;
    }
}

package com.exiledradio.buzzybeesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patch P3 (docs/PATCHES.md): the server lets a player open elytra while still rising, as 1.15's
 * {@code tryToStartFallFlying}, which dropped 1.12.2's "only while falling" test.
 *
 * <p>{@code NetHandlerPlayServer.processEntityAction} reads {@code player.motionY} once, in its
 * {@code START_FALL_FLYING} case ({@code motionY < 0.0}); that read becomes
 * {@code tweaks.ElytraStart.fallSpeed(player)}, which returns a falling speed while the switch is on (and no
 * Levitation, 1.15's other new condition) and the real {@code motionY} otherwise.
 */
public final class ElytraStartTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.network.NetHandlerPlayServer";
    private static final String HOOKS = "com/exiledradio/buzzybeesbackport/tweaks/ElytraStart";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode action = Asm.method(node, "processEntityAction", "func_147357_a",
                "(Lnet/minecraft/network/play/client/CPacketEntityAction;)V");
        if (action == null) {
            Asm.LOG.warn("P3 elytra while rising: processEntityAction not found; patch skipped.");
            return basicClass;
        }
        List<AbstractInsnNode> reads = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode insn : action.instructions.toArray()) {
            if (Asm.isField(insn, Opcodes.GETFIELD, "motionY", "field_70181_x", "D")) {
                reads.add(insn);
            }
        }
        AbstractInsnNode owner = reads.size() == 1 ? Asm.previous(reads.get(0)) : null;
        if (reads.size() != 1 || !(owner instanceof FieldInsnNode)
                || !"Lnet/minecraft/entity/player/EntityPlayerMP;".equals(((FieldInsnNode) owner).desc)) {
            Asm.LOG.warn("P3 elytra while rising: unexpected shape ({} motionY reads); patch skipped.", reads.size());
            return basicClass;
        }
        action.instructions.set(reads.get(0), new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "fallSpeed",
                "(Lnet/minecraft/entity/player/EntityPlayerMP;)D", false));
        applied = true;
        Asm.LOG.info("P3 elytra while rising: patched {}.", TARGET);
        return Asm.write(node);
    }
}

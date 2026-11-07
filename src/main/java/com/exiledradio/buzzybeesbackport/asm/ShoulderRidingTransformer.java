package com.exiledradio.buzzybeesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Patch P5 (docs/PATCHES.md): parrots stay on the player's shoulders while the player rides something, as
 * 1.15's {@code PlayerEntity.livingTick}, whose shoulder-drop test no longer has {@code isPassenger()}.
 *
 * <p>{@code EntityPlayer.onLivingUpdate} drops the shoulder parrots when
 * {@code !world.isRemote && (fallDistance > 0.5F || isInWater() || isRiding()) || capabilities.isFlying}.
 * That {@code isRiding()} call, the last one before the {@code spawnShoulderEntities()} call, becomes
 * {@code tweaks.ShoulderParrots.ridingDrops(this)}: false while the switch is on, {@code isRiding()}
 * otherwise. The method's other {@code isRiding()} calls are untouched.
 */
public final class ShoulderRidingTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.entity.player.EntityPlayer";
    private static final String HOOKS = "com/exiledradio/buzzybeesbackport/tweaks/ShoulderParrots";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode update = Asm.method(node, "onLivingUpdate", "func_70636_d", "()V");
        if (update == null) {
            Asm.LOG.warn("P5 parrots while riding: onLivingUpdate not found; patch skipped.");
            return basicClass;
        }
        List<AbstractInsnNode> drops = Asm.calls(update, "spawnShoulderEntities", "func_192030_dh", "()V");
        if (drops.size() != 1) {
            Asm.LOG.warn("P5 parrots while riding: unexpected shape ({} spawnShoulderEntities calls); patch skipped.", drops.size());
            return basicClass;
        }
        // The isRiding() of the drop test: within the few instructions of that one condition.
        AbstractInsnNode riding = null;
        int steps = 0;
        for (AbstractInsnNode insn = drops.get(0).getPrevious(); insn != null && steps < 16; insn = insn.getPrevious()) {
            if (insn.getOpcode() < 0) {
                continue;
            }
            steps++;
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL && Asm.isCall(insn, "isRiding", "func_184218_aH", "()Z")) {
                riding = insn;
                break;
            }
            if (insn instanceof MethodInsnNode && !Asm.isCall(insn, "isInWater", "func_70090_H", "()Z")) {
                break; // only isInWater() may stand between, as in vanilla's condition
            }
        }
        if (riding == null) {
            Asm.LOG.warn("P5 parrots while riding: the shoulder-drop test is not where expected; patch skipped.");
            return basicClass;
        }
        update.instructions.set(riding, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "ridingDrops",
                "(Lnet/minecraft/entity/player/EntityPlayer;)Z", false));
        applied = true;
        Asm.LOG.info("P5 parrots while riding: patched {}.", TARGET);
        return Asm.write(node);
    }
}

package com.exiledradio.buzzybeesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;

/**
 * Patch P6 (docs/PATCHES.md): 1.15.2's hidden effects. Two classes:
 *
 * <ul>
 *   <li>{@code PotionEffect} gets a field for the hidden effect and implements {@code tweaks.HiddenEffectHolder}
 *       (four small methods that read or set its fields). {@code onUpdate} starts with
 *       {@code int r = HiddenEffects.tick(this, entity); if (r >= 0) return r != 0;} (r is -1 unless there is a
 *       hidden effect), {@code writeCustomPotionEffectToNBT} returns through {@code HiddenEffects.write} and
 *       {@code readCustomPotionEffectFromNBT} through {@code HiddenEffects.read} (the {@code HiddenEffect} tag).</li>
 *   <li>{@code EntityLivingBase.addPotionEffect}'s one {@code existing.combine(added)} becomes
 *       {@code HiddenEffects.combine(existing, added, this)}.</li>
 * </ul>
 * One branch is added, at the head of {@code onUpdate}, with its frame. If any part of {@code PotionEffect} is not
 * as expected, nothing in it is patched; {@code HiddenEffects.combine} then sees an effect without the holder and
 * combines as 1.12.2 does (the two classes may load in either order). The self-test reads {@link #applied}.
 */
public final class HiddenEffectsTransformer implements IClassTransformer {

    private static final String EFFECT = "net/minecraft/potion/PotionEffect";
    private static final String EFFECT_DESC = "L" + EFFECT + ";";
    private static final String HOLDER = "com/exiledradio/buzzybeesbackport/tweaks/HiddenEffectHolder";
    private static final String HOOKS = "com/exiledradio/buzzybeesbackport/tweaks/HiddenEffects";
    private static final String NBT = "Lnet/minecraft/nbt/NBTTagCompound;";
    private static final String ENTITY = "Lnet/minecraft/entity/EntityLivingBase;";
    private static final String FIELD = "buzzybeesbackport$hiddenEffect";

    /** Set once {@code PotionEffect} has been patched; {@code addPotionEffect} is patched only then. */
    public static volatile boolean applied;
    /** Set once {@code EntityLivingBase.addPotionEffect} has been patched. */
    public static volatile boolean appliedAdd;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if ("net.minecraft.potion.PotionEffect".equals(transformedName)) {
            return effect(basicClass);
        }
        if ("net.minecraft.entity.EntityLivingBase".equals(transformedName)) {
            return living(basicClass);
        }
        return basicClass;
    }

    private static String field(ClassNode node, String mcp, String srg, String desc) {
        for (FieldNode f : node.fields) {
            if ((mcp.equals(f.name) || srg.equals(f.name)) && desc.equals(f.desc)) {
                return f.name;
            }
        }
        return null;
    }

    private static byte[] effect(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        if (node.interfaces.contains(HOLDER)) {
            return bytes;
        }
        String duration = field(node, "duration", "field_76460_b", "I");
        String amplifier = field(node, "amplifier", "field_76461_c", "I");
        String ambient = field(node, "isAmbient", "field_82723_d", "Z");
        String particles = field(node, "showParticles", "field_188421_h", "Z");
        MethodNode update = Asm.method(node, "onUpdate", "func_76455_a", "(" + ENTITY + ")Z");
        MethodNode write = Asm.method(node, "writeCustomPotionEffectToNBT", "func_82719_a", "(" + NBT + ")" + NBT);
        MethodNode read = Asm.method(node, "readCustomPotionEffectFromNBT", "func_82722_b", "(" + NBT + ")" + EFFECT_DESC);
        if (duration == null || amplifier == null || ambient == null || particles == null || update == null || write == null || read == null) {
            Asm.LOG.warn("P6 hidden effects: PotionEffect is not as expected; patch skipped (a stronger effect replaces a weaker one as in 1.12.2).");
            return bytes;
        }

        // The field and the holder's four methods.
        node.interfaces.add(HOLDER);
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, FIELD, EFFECT_DESC, null, null));
        MethodNode get = new MethodNode(Opcodes.ACC_PUBLIC, "buzzybeesbackport$hidden", "()" + EFFECT_DESC, null, null);
        get.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        get.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, EFFECT, FIELD, EFFECT_DESC));
        get.instructions.add(new InsnNode(Opcodes.ARETURN));
        MethodNode set = new MethodNode(Opcodes.ACC_PUBLIC, "buzzybeesbackport$setHidden", "(" + EFFECT_DESC + ")V", null, null);
        set.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        set.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        set.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, EFFECT, FIELD, EFFECT_DESC));
        set.instructions.add(new InsnNode(Opcodes.RETURN));
        MethodNode details = new MethodNode(Opcodes.ACC_PUBLIC, "buzzybeesbackport$set", "(IIZZ)V", null, null);
        String[] names = {duration, amplifier, ambient, particles};
        String[] descs = {"I", "I", "Z", "Z"};
        for (int i = 0; i < 4; i++) {
            details.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            details.instructions.add(new VarInsnNode(Opcodes.ILOAD, i + 1));
            details.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, EFFECT, names[i], descs[i]));
        }
        details.instructions.add(new InsnNode(Opcodes.RETURN));
        MethodNode setDuration = new MethodNode(Opcodes.ACC_PUBLIC, "buzzybeesbackport$setDuration", "(I)V", null, null);
        setDuration.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setDuration.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        setDuration.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, EFFECT, duration, "I"));
        setDuration.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(get);
        node.methods.add(set);
        node.methods.add(details);
        node.methods.add(setDuration);

        // onUpdate: int r = tick(this, entity); if (r >= 0) return r != 0 (r is 0 or 1);
        LabelNode vanilla = new LabelNode();
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new VarInsnNode(Opcodes.ALOAD, 1));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "tick", "(" + EFFECT_DESC + ENTITY + ")I", false));
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new JumpInsnNode(Opcodes.IFLT, vanilla));
        head.add(new InsnNode(Opcodes.IRETURN));
        head.add(vanilla);
        head.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER}));
        head.add(new InsnNode(Opcodes.POP));
        update.instructions.insert(head);

        // write: return HiddenEffects.write(this, nbt);
        for (AbstractInsnNode insn : write.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.ARETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new InsnNode(Opcodes.SWAP));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "write", "(" + EFFECT_DESC + NBT + ")" + NBT, false));
                write.instructions.insertBefore(insn, hook);
            }
        }
        // read: return HiddenEffects.read(effect, nbt);
        for (AbstractInsnNode insn : read.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.ARETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "read", "(" + EFFECT_DESC + NBT + ")" + EFFECT_DESC, false));
                read.instructions.insertBefore(insn, hook);
            }
        }
        applied = true;
        Asm.LOG.info("P6 hidden effects: patched PotionEffect.");
        return Asm.write(node);
    }

    private static byte[] living(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode add = Asm.method(node, "addPotionEffect", "func_70690_d", "(" + EFFECT_DESC + ")V");
        List<AbstractInsnNode> combines = add == null ? null : Asm.calls(add, "combine", "func_76452_a", "(" + EFFECT_DESC + ")V");
        if (combines == null || combines.size() != 1) {
            Asm.LOG.warn("P6 hidden effects: EntityLivingBase.addPotionEffect is not as expected; a stronger effect replaces a weaker one as in 1.12.2.");
            return bytes;
        }
        AbstractInsnNode call = combines.get(0);
        add.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 0));
        add.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "combine",
                "(" + EFFECT_DESC + EFFECT_DESC + ENTITY + ")V", false));
        appliedAdd = true;
        Asm.LOG.info("P6 hidden effects: patched EntityLivingBase.addPotionEffect.");
        return Asm.write(node);
    }
}

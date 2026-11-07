package com.exiledradio.buzzybeesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Patch P2 (docs/PATCHES.md): at a crafting table, shift-clicking a stack in the player's inventory moves it
 * into the crafting grid first, as 1.15's {@code WorkbenchContainer.transferStackInSlot}.
 *
 * <p>{@code ContainerWorkbench.transferStackInSlot} has four {@code mergeItemStack} calls; the second
 * (inventory to hotbar, slots 37-46) and the third (hotbar to inventory, slots 10-37) become
 * {@code tweaks.CraftingShift.merge(this, stack, start, end, reverse)}, which tries the grid (slots 1-10)
 * before doing what the call did. The other two calls (taking the result, emptying the grid) stay.
 */
public final class WorkbenchShiftTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.inventory.ContainerWorkbench";
    private static final String MERGE_DESC = "(Lnet/minecraft/item/ItemStack;IIZ)Z";
    private static final String HOOKS = "com/exiledradio/buzzybeesbackport/tweaks/CraftingShift";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode transfer = Asm.method(node, "transferStackInSlot", "func_82846_b",
                "(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;");
        if (transfer == null) {
            Asm.LOG.warn("P2 crafting table shift-click: transferStackInSlot not found; patch skipped.");
            return basicClass;
        }
        List<AbstractInsnNode> merges = Asm.calls(transfer, "mergeItemStack", "func_75135_a", MERGE_DESC);
        if (merges.size() != 4 || !range(merges.get(1), 37, 46) || !range(merges.get(2), 10, 37)) {
            Asm.LOG.warn("P2 crafting table shift-click: unexpected shape ({} mergeItemStack calls); patch skipped.", merges.size());
            return basicClass;
        }
        for (int i = 1; i <= 2; i++) {
            transfer.instructions.set(merges.get(i), new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "merge",
                    "(Lnet/minecraft/inventory/Container;Lnet/minecraft/item/ItemStack;IIZ)Z", false));
        }
        applied = true;
        Asm.LOG.info("P2 crafting table shift-click: patched {}.", TARGET);
        return Asm.write(node);
    }

    /** The call's arguments end with the constants start, end, false. */
    private static boolean range(AbstractInsnNode call, int start, int end) {
        AbstractInsnNode reverse = Asm.previous(call);
        AbstractInsnNode to = Asm.previous(reverse);
        AbstractInsnNode from = to == null ? null : Asm.previous(to);
        Integer r = Asm.intConstant(reverse);
        Integer t = Asm.intConstant(to);
        Integer f = Asm.intConstant(from);
        return r != null && r == 0 && t != null && t == end && f != null && f == start;
    }
}

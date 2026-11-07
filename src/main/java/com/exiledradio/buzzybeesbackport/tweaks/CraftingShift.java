package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Called from the patched {@code ContainerWorkbench.transferStackInSlot} (patch P2,
 * {@code asm/WorkbenchShiftTransformer}) in place of its two inventory-to-inventory {@code mergeItemStack}
 * calls: 1.15 first tries the crafting grid (slots 1-9) and only moves the stack to the other part of the
 * inventory when nothing went into the grid.
 */
public final class CraftingShift {

    private static final Method MERGE = ReflectionHelper.findMethod(Container.class, "mergeItemStack", "func_75135_a",
            ItemStack.class, int.class, int.class, boolean.class);

    private CraftingShift() {
    }

    public static boolean merge(Container container, ItemStack stack, int start, int end, boolean reverse) {
        if (Tweaks.on() && ModConfig.tweaks.craftingShiftClick && invoke(container, stack, 1, 10, false)) {
            return true;
        }
        return invoke(container, stack, start, end, reverse);
    }

    private static boolean invoke(Container container, ItemStack stack, int start, int end, boolean reverse) {
        try {
            return (Boolean) MERGE.invoke(container, stack, start, end, reverse);
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
}

package com.exiledradio.buzzybeesbackport.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This mod's own small coremod: the bytecode patches 1.15's behavior needs where 1.12.2 has no hook.
 * One transformer per patch, each documented in {@code docs/PATCHES.md}. No MixinBooter.
 *
 * <p>Named in the jar's manifest ({@code FMLCorePlugin}, with {@code FMLCorePluginContainsFMLMod}) and, in the
 * dev environment, by {@code -Dfml.coreMods.load}.
 */
@IFMLLoadingPlugin.Name("Buzzy Bees Backport Core")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.exiledradio.buzzybeesbackport.asm.")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class BuzzyBeesCore implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                PistonStickTransformer.class.getName(),
                WorkbenchShiftTransformer.class.getName(),
                ElytraStartTransformer.class.getName(),
                FireworkAngleTransformer.class.getName(),
                ShoulderRidingTransformer.class.getName(),
        };
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

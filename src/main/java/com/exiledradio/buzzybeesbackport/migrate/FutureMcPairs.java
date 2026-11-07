package com.exiledradio.buzzybeesbackport.migrate;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stream F: this mod's entries in Backport Core's tables for the other mod that adds 1.15's bees
 * ({@code futuremc}), so a world can move over and duplicates stay out of sight. Quiet compat (Rule 3):
 * no user-facing text names that mod.
 *
 * <ul>
 *   <li>Blocks and items of the same names stand in for theirs: bee_nest, beehive, honey_block,
 *       honeycomb_block, honey_bottle, honeycomb. Their hives' {@code full} property is dropped: ours keeps
 *       the exact honey level in the tile entity, carried over by {@link FutureMcBees}.</li>
 *   <li>Their metadata meanings are in {@code migration/futuremc_states.json} (tools/futuremc/states.py).</li>
 *   <li>With both installed, {@code compat.duplicates} picks which blocks and items stay visible.</li>
 * </ul>
 */
public final class FutureMcPairs {

    static final List<String> BLOCKS = Arrays.asList("bee_nest", "beehive", "honey_block", "honeycomb_block");
    static final List<String> ITEMS = Arrays.asList("honey_bottle", "honeycomb");

    private FutureMcPairs() {
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        FutureMcTable.contributor(BuzzyBeesBackport.MOD_ID, () -> ModConfig.compat.convertRemoved)
                .ignoreProperty("bee_nest", "full")
                .ignoreProperty("beehive", "full")
                .recordedStates("/assets/buzzybeesbackport/migration/futuremc_states.json");
        DuplicateBlocks.register(BuzzyBeesBackport.MOD_ID, FutureMcTable.FUTURE_MC, () -> ModConfig.compat.duplicates, () -> {
            List<String[]> pairs = new ArrayList<String[]>();
            for (String name : BLOCKS) {
                pairs.add(new String[] {name, name});
            }
            return pairs;
        });
        DuplicateBlocks.registerItems(() -> {
            List<Item> hide = new ArrayList<Item>();
            if (ModConfig.compat.duplicates == DuplicateBlocks.Keep.BOTH) {
                return hide;
            }
            String from = ModConfig.compat.duplicates == DuplicateBlocks.Keep.OURS ? FutureMcTable.FUTURE_MC : BuzzyBeesBackport.MOD_ID;
            String other = from.equals(FutureMcTable.FUTURE_MC) ? BuzzyBeesBackport.MOD_ID : FutureMcTable.FUTURE_MC;
            for (String name : ITEMS) {
                Item mine = ForgeRegistries.ITEMS.getValue(new ResourceLocation(from, name));
                Item kept = ForgeRegistries.ITEMS.getValue(new ResourceLocation(other, name));
                if (mine != null && kept != null && mine.getRegistryName() != null && from.equals(mine.getRegistryName().getNamespace())
                        && kept.getRegistryName() != null && other.equals(kept.getRegistryName().getNamespace())) {
                    hide.add(mine);
                }
            }
            return hide;
        });
    }
}

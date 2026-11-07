package com.exiledradio.buzzybeesbackport.api;

import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Where bee nests grow: which logs make a nest tree, and each biome's chance per tree.
 *
 * <p>1.15.2's table for 1.12.2's biomes: plains and sunflower plains 5%, flower forest 2%, forest, birch
 * forest and the tall birch forests 0.2%. Later updates' trees (mangrove, cherry) and biomes (meadow) are
 * added by their own backports through {@link #addTree} and {@link #setBiomeChance}.
 */
public final class BeeNests {

    private static final List<Predicate<IBlockState>> TREES = new ArrayList<Predicate<IBlockState>>();
    private static final Map<Biome, Float> CHANCES = new HashMap<Biome, Float>();
    private static boolean defaults;

    private BeeNests() {
    }

    /** A log that can carry a bee nest, as oak and birch do. */
    public static void addTree(Predicate<IBlockState> log) {
        TREES.add(log);
    }

    /** Sets a biome's chance per tree (0 to 1), replacing vanilla's or the dictionary's. */
    public static void setBiomeChance(Biome biome, float chance) {
        CHANCES.put(biome, chance);
    }

    /** @return true for an oak or birch log (any axis), or a log another mod added */
    public static boolean isNestTree(IBlockState state) {
        if (state.getBlock() == Blocks.LOG) {
            BlockPlanks.EnumType type = state.getValue(BlockOldLog.VARIANT);
            return type == BlockPlanks.EnumType.OAK || type == BlockPlanks.EnumType.BIRCH;
        }
        for (Predicate<IBlockState> tree : TREES) {
            if (tree.test(state)) {
                return true;
            }
        }
        return false;
    }

    /** @return the chance a tree generated in this biome gets a nest */
    public static float chance(Biome biome, boolean moddedBiomes) {
        fillDefaults();
        Float set = CHANCES.get(biome);
        if (set != null) {
            return set;
        }
        if (!moddedBiomes || biome.getRegistryName() == null || "minecraft".equals(biome.getRegistryName().getNamespace())) {
            return 0.0F;
        }
        // Other mods' biomes, by Forge's dictionary.
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.PLAINS) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SAVANNA)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.DRY)) {
            return 0.05F;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SPOOKY)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
            return 0.002F;
        }
        return 0.0F;
    }

    private static synchronized void fillDefaults() {
        if (defaults) {
            return;
        }
        defaults = true;
        putIfAbsent(Biomes.PLAINS, 0.05F);
        putIfAbsent(Biomes.MUTATED_PLAINS, 0.05F);
        putIfAbsent(Biomes.MUTATED_FOREST, 0.02F);
        for (Biome forest : new Biome[] {Biomes.FOREST, Biomes.FOREST_HILLS, Biomes.BIRCH_FOREST, Biomes.BIRCH_FOREST_HILLS,
                Biomes.MUTATED_BIRCH_FOREST, Biomes.MUTATED_BIRCH_FOREST_HILLS}) {
            putIfAbsent(forest, 0.002F);
        }
    }

    private static void putIfAbsent(Biome biome, float chance) {
        if (!CHANCES.containsKey(biome)) {
            CHANCES.put(biome, chance);
        }
    }
}

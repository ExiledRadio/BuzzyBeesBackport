package com.exiledradio.buzzybeesbackport.worldgen;

import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.StructurePlacements;
import com.exiledradio.backportcore.worldgen.StructurePlacements.BiomePoint;

/**
 * 1.15's structure placement, for Backport Core's placement table (the newest installed version's numbers
 * win), so a world with this mod matches 1.15's seed maps. Every number is 1.15.2's own, read from its
 * decompiled code: {@code ChunkGeneratorSettings} for the spacings and separations, each feature's
 * {@code getRandomSalt} (or its {@code setLargeFeatureWithSalt} call) for the salts.
 *
 * <ul>
 *   <li>The grids are 1.14's: villages 32/8 (salt 10387312), outposts 32/8 (165745296), the four temples 32/8
 *       (14357617 desert pyramid, 14357618 igloo, 14357619 jungle temple, 14357620 witch hut), monument 32/5
 *       (10387313) and mansion 80/20 (10387319), both triangular, shipwrecks and ocean ruins 16/8 (165745295,
 *       14357621).</li>
 *   <li>Every single-biome check reads the block biome at block (9, 0, 9) of the chunk
 *       ({@code ChunkGenerator.createStructures}), but 1.15's block biome is a new one: the 3D zoom of the
 *       server's {@code BiomeManager} (hashed seed, {@code FuzzyOffsetConstantColumnBiomeZoomer}), which Backport
 *       Core switches on for 1.15 and later. So villages and temples near biome edges move again.</li>
 *   <li>The biome lists are 1.14's: bamboo jungles have jungle temples, strongholds also go in the mushroom
 *       field shore (and the bamboo jungles, above sea level).</li>
 * </ul>
 */
public final class Placement115 {

    private Placement115() {
    }

    /** Pre-init. */
    public static void register() {
        StructurePlacements.version("1.15")
                .biomePoint(BiomePoint.BLOCK_9_9)
                .grid(Structures.VILLAGE, 32, 8, 10387312)
                .grid(Structures.PILLAGER_OUTPOST, 32, 8, 165745296)
                .grid(StructurePlacements.DESERT_PYRAMID, 32, 8, 14357617)
                .grid(StructurePlacements.IGLOO, 32, 8, 14357618)
                .grid(StructurePlacements.JUNGLE_PYRAMID, 32, 8, 14357619)
                .grid(StructurePlacements.SWAMP_HUT, 32, 8, 14357620)
                .grid(StructurePlacements.OCEAN_MONUMENT, 32, 5, 10387313, true)
                .grid(StructurePlacements.MANSION, 80, 20, 10387319, true)
                .grid(Structures.SHIPWRECK, 16, 8, 165745295)
                .grid(Structures.OCEAN_RUIN, 16, 8, 14357621)
                .structureBiome(StructurePlacements.JUNGLE_PYRAMID, "bamboo_jungle")
                .structureBiome(StructurePlacements.JUNGLE_PYRAMID, "bamboo_jungle_hills")
                .strongholdBiome("minecraft:mushroom_island_shore", true);
    }
}

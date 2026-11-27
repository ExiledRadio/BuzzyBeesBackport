package com.exiledradio.buzzybeesbackport.nestgen;

import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.api.BeeNests;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

/**
 * Bee nests on Dynamic Trees' oaks and birches. Dynamic Trees grows its trees from its own world generator (weight
 * 20), after the populate event the static trees are scanned in, so its trunks are scanned by a generator of their
 * own that runs after it, with the same biome chances. A trunk is Dynamic Trees' oak or birch branch standing on its
 * rooty soil, under its leaves. Nothing here runs without Dynamic Trees.
 */
public final class DynamicTreeNests implements IWorldGenerator {

    /** After Dynamic Trees' {@code WorldGeneratorTrees} (20). */
    static final int WEIGHT = 1000;
    private static final ResourceLocation OAK = new ResourceLocation("dynamictrees", "oakbranch");
    private static final ResourceLocation BIRCH = new ResourceLocation("dynamictrees", "birchbranch");

    private DynamicTreeNests() {
    }

    static void preInit() {
        if (!Loader.isModLoaded("dynamictrees")) {
            return;
        }
        BeeNests.addTree(state -> isTrunk(state.getBlock()));
        GameRegistry.registerWorldGenerator(new DynamicTreeNests(), WEIGHT);
    }

    /** The trunks' blocks, found once: the nest scan asks this of every block down every column it reads. */
    private static Block oakBlock;
    private static Block birchBlock;

    static boolean isTrunk(Block block) {
        if (oakBlock == null) {
            oakBlock = net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.getValue(OAK);
            birchBlock = net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.getValue(BIRCH);
        }
        return block != net.minecraft.init.Blocks.AIR && (block == oakBlock || block == birchBlock);
    }

    /** Dynamic Trees' rooty dirt (and its grass and podzol looks), sand or water: what its trees stand on. */
    static boolean isRootySoil(Block block) {
        ResourceLocation id = block.getRegistryName();
        return id != null && id.getNamespace().startsWith("dynamictrees") && id.getPath().startsWith("rooty");
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!ModConfig.nests.enabled || world.isRemote || !FlatWorlds.decoration(world)) {
            return;
        }
        Random rand = new Random(world.getSeed() ^ (chunkX * 341873128712L + chunkZ * 132897987541L) ^ 0x64796e54L);
        NestGenerator.scan(world, rand, chunkX * 16 + 8, chunkZ * 16 + 8, false, true);
    }
}

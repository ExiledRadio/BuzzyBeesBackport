package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.buzzybeesbackport.api.BeeNests;
import com.exiledradio.buzzybeesbackport.bee.BeeContent;
import com.exiledradio.buzzybeesbackport.nestgen.GeneratorNests;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

/**
 * Superflat worlds get nests only as vanilla's do: none without {@code decoration} (village farm borders and log
 * corners included), and on the grown trees of a preset with {@code decoration} at 1.15's rate.
 *
 * <p>In a world that is not superflat (the suite's, {@code -Pselftest=all}) it checks the populate scan's decision
 * with the world info saying superflat for a moment. In a superflat world ({@code tools/flat/selftest.sh}) it
 * generates the chunks around spawn and looks at every block of them.
 */
public final class FlatSelfTest implements SelfTest {

    /** Chunks each way from spawn's chunk: 13 x 13 generated, the inner 11 x 11 populated. */
    private static final int RADIUS = 6;

    @Override
    public String name() {
        return "flat";
    }

    @Override
    public void run(Check c) throws Exception {
        if (!c.ok(BeeContent.on(), "bee stream on (nests need its block)")) {
            return;
        }
        if (FlatWorlds.isFlat(c.world)) {
            realFlat(c);
        } else {
            rules(c);
        }
    }

    // ---- the decision, in the suite's world

    private void rules(Check c) throws Exception {
        WorldServer world = c.world;
        IChunkGenerator generator = world.getChunkProvider().chunkGenerator;
        c.ok(GeneratorNests.scans(world, generator), "a world that is not superflat gets the populate scan");
        WorldInfo info = world.getWorldInfo();
        Field type = ReflectionHelper.findField(WorldInfo.class, "terrainType", "field_76098_b");
        Field options = ReflectionHelper.findField(WorldInfo.class, "generatorOptions", "field_82576_c");
        Object oldType = type.get(info);
        Object oldOptions = options.get(info);
        try {
            type.set(info, WorldType.FLAT);
            preset(world, options, info, "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village");
            c.ok(!GeneratorNests.scans(world, generator), "default superflat preset: no populate scan, so no nests");
            preset(world, options, info, "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;4;decoration");
            c.ok(GeneratorNests.scans(world, generator), "superflat forest with decoration: the populate scan runs");
            preset(world, options, info, "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village,decoration");
            c.ok(GeneratorNests.scans(world, generator), "superflat plains with village and decoration: the scan runs");
            preset(world, options, info, "3;minecraft:air;1;decoration");
            c.ok(!GeneratorNests.scans(world, generator), "an all-air preset decorates nothing, so no scan");
        } finally {
            type.set(info, oldType);
            options.set(info, oldOptions);
            FlatWorlds.forget(world);
        }
        c.ok(GeneratorNests.scans(world, generator), "the world is itself again");
    }

    private static void preset(WorldServer world, Field options, WorldInfo info, String preset) throws Exception {
        options.set(info, preset);
        FlatWorlds.forget(world);
    }

    // ---- a real superflat world

    private void realFlat(Check c) {
        WorldServer world = c.world;
        String options = world.getWorldInfo().getGeneratorOptions();
        boolean decoration = options.contains("decoration");
        c.eq(decoration, FlatWorlds.decoration(world), "decoration as the preset says (" + options + ")");
        c.eq(decoration, GeneratorNests.scans(world, world.getChunkProvider().chunkGenerator), "the populate scan runs only with decoration");

        BlockPos spawn = world.getSpawnPoint();
        int cx = spawn.getX() >> 4;
        int cz = spawn.getZ() >> 4;
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                world.getChunkProvider().provideChunk(x, z);
            }
        }
        int populated = 0;
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                if (world.getChunk(x, z).isTerrainPopulated()) {
                    populated++;
                }
            }
        }
        c.ok(populated >= (2 * RADIUS - 1) * (2 * RADIUS - 1), "the chunks around spawn are populated: " + populated);

        int nests = 0;
        int offTree = 0;
        int logs = 0;
        int trees = 0;
        for (int x = (cx - RADIUS) * 16; x < (cx + RADIUS + 1) * 16; x++) {
            for (int z = (cz - RADIUS) * 16; z < (cz + RADIUS + 1) * 16; z++) {
                int top = world.getHeight(x, z);
                for (int y = top; y >= 1; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    Block block = world.getBlockState(p).getBlock();
                    if (block == BeeContent.beeNest) {
                        nests++;
                        if (!onGrownTree(world, p)) {
                            offTree++;
                            c.fail("a nest not on a grown oak or birch at " + p);
                        }
                    } else if (BeeNests.isNestTree(world.getBlockState(p))) {
                        logs++;
                        if (isTrunkBase(world, p)) {
                            trees++;
                        }
                    }
                }
            }
        }
        String counts = "nests " + nests + ", oak and birch logs " + logs + ", grown trees " + trees;
        com.exiledradio.buzzybeesbackport.BuzzyBeesBackport.LOGGER.info("SELFTEST flat: {} ({})", counts, options);
        if (!decoration) {
            c.eq(0, nests, "no nests anywhere without decoration (village logs included; " + counts + ")");
            c.eq(0, trees, "no grown trees without decoration");
        } else {
            c.ok(trees > 100, "decoration grows trees (" + counts + ")");
            c.ok(nests > 0, "decoration: nests on grown trees at the biome's rate (" + counts + ")");
            c.eq(0, offTree, "every nest is on a grown tree");
            float chance = BeeNests.chance(world.getBiome(spawn), true);
            c.ok(nests <= Math.max(10, trees * chance * 3), "nests no more often than the biome's rate " + chance + " (" + counts + ")");
        }
    }

    /** A nest against an oak or birch trunk of 4 or more logs on dirt or grass, with leaves. */
    private static boolean onGrownTree(WorldServer world, BlockPos nest) {
        for (EnumFacing f : EnumFacing.HORIZONTALS) {
            BlockPos log = nest.offset(f);
            if (!BeeNests.isNestTree(world.getBlockState(log))) {
                continue;
            }
            BlockPos base = log;
            while (BeeNests.isNestTree(world.getBlockState(base.down()))) {
                base = base.down();
            }
            if (isTrunkBase(world, base)) {
                return true;
            }
        }
        return false;
    }

    /** The lowest log of a grown tree: on dirt or grass, 4 or more logs, leaves around the top. */
    private static boolean isTrunkBase(WorldServer world, BlockPos base) {
        Block ground = world.getBlockState(base.down()).getBlock();
        if (!(ground instanceof BlockGrass || ground instanceof BlockDirt) || BeeNests.isNestTree(world.getBlockState(base.down()))) {
            return false;
        }
        int top = base.getY();
        while (top - base.getY() < 32 && BeeNests.isNestTree(world.getBlockState(new BlockPos(base.getX(), top + 1, base.getZ())))) {
            top++;
        }
        if (top - base.getY() + 1 < 4) {
            return false;
        }
        for (BlockPos p : BlockPos.getAllInBoxMutable(base.add(-2, top - base.getY() - 2, -2), base.add(2, top - base.getY() + 2, 2))) {
            if (world.getBlockState(p).getBlock() instanceof BlockLeaves) {
                return true;
            }
        }
        return false;
    }
}

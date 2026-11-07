package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.api.BeeNests;
import com.exiledradio.buzzybeesbackport.bee.BeeContent;
import com.exiledradio.buzzybeesbackport.bee.BlockBeehive;
import com.exiledradio.buzzybeesbackport.bee.TileEntityBeehive;
import com.exiledradio.buzzybeesbackport.nestgen.NestGenerator;

import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.Collections;
import java.util.Random;

/** Stream G: bee nests on generated trees and on trees grown from saplings near flowers. */
public final class NestSelfTest implements SelfTest {

    @Override
    public String name() {
        return "nests";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(BeeContent.on(), "bee stream on (nests need its block)")) {
            return;
        }
        chances(c);
        handBuiltTree(c);
        saplings(c);
        realWorld(c);
    }

    private void chances(Check c) {
        c.eq(0.05F, BeeNests.chance(Biomes.PLAINS, true), "plains: 5% of trees");
        c.eq(0.05F, BeeNests.chance(Biomes.MUTATED_PLAINS, true), "sunflower plains: 5%");
        c.eq(0.02F, BeeNests.chance(Biomes.MUTATED_FOREST, true), "flower forest: 2%");
        c.eq(0.002F, BeeNests.chance(Biomes.FOREST, true), "forest: 0.2%");
        c.eq(0.002F, BeeNests.chance(Biomes.MUTATED_BIRCH_FOREST, true), "tall birch forest: 0.2%");
        c.eq(0.0F, BeeNests.chance(Biomes.DESERT, true), "desert: none");
        c.eq(0.0F, BeeNests.chance(Biomes.TAIGA, true), "taiga: none");
        c.eq(0.0F, BeeNests.chance(Biomes.ROOFED_FOREST, true), "dark forest: none");
        c.ok(BeeNests.isNestTree(Blocks.LOG.getDefaultState()), "oak carries nests");
        c.ok(BeeNests.isNestTree(Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.BIRCH)), "birch carries nests");
        c.ok(!BeeNests.isNestTree(Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE)), "spruce does not");
    }

    /** An oak: 5 logs on grass, leaves from the 4th log up. */
    private BlockPos tree(Check c, BlockPos ground, BlockPlanks.EnumType wood) {
        c.world.setBlockState(ground, Blocks.GRASS.getDefaultState(), 2);
        IBlockState log = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, wood);
        IBlockState leaves = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, wood);
        for (int y = 1; y <= 5; y++) {
            c.world.setBlockState(ground.up(y), log, 2);
        }
        for (int y = 4; y <= 6; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = ground.add(dx, y, dz);
                    if (c.world.isAirBlock(p)) {
                        c.world.setBlockState(p, leaves, 2);
                    }
                }
            }
        }
        return ground.up();
    }

    private BlockPos findNest(Check c, BlockPos around, int r) {
        for (BlockPos p : BlockPos.getAllInBox(around.add(-r, -2, -r), around.add(r, 8, r))) {
            if (c.world.getBlockState(p).getBlock() == BeeContent.beeNest) {
                return p.toImmutable();
            }
        }
        return null;
    }

    private void checkNest(Check c, BlockPos nest, BlockPos trunk, String what) {
        c.eq(EnumFacing.SOUTH, c.world.getBlockState(nest).getValue(BlockBeehive.FACING), what + ": faces south");
        c.ok(nest.getX() != trunk.getX() || nest.getZ() != trunk.getZ(), what + ": beside the trunk");
        boolean onLog = false;
        for (EnumFacing f : new EnumFacing[] {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH}) {
            onLog |= BeeNests.isNestTree(c.world.getBlockState(nest.offset(f)));
        }
        c.ok(onLog, what + ": against the trunk's west, east or south side");
        c.ok(c.world.isAirBlock(nest.south()), what + ": air in front");
        TileEntityBeehive hive = (TileEntityBeehive) c.world.getTileEntity(nest);
        c.ok(hive != null && hive.getBeeCount() >= 2 && hive.getBeeCount() <= 3, what + ": 2-3 bees: " + (hive == null ? "none" : hive.getBeeCount()));
    }

    private void handBuiltTree(Check c) {
        BlockPos base = c.area();
        BlockPos trunk = tree(c, base.add(10, 0, 10), BlockPlanks.EnumType.OAK);
        int placed = NestGenerator.scan(c.world, new Random(2), base.getX(), base.getZ(), true);
        c.eq(1, placed, "one trunk in the area, one nest");
        BlockPos nest = findNest(c, trunk, 2);
        if (c.ok(nest != null, "the nest is on the tree")) {
            c.eq(trunk.getY() + 2, nest.getY(), "one below the lowest leaves");
            checkNest(c, nest, trunk, "hand-built oak");
        }
        // A spruce gets none.
        BlockPos base2 = c.area();
        tree(c, base2.add(10, 0, 10), BlockPlanks.EnumType.SPRUCE);
        c.eq(0, NestGenerator.scan(c.world, new Random(3), base2.getX(), base2.getZ(), true), "a spruce gets no nest");
    }

    private void saplings(Check c) {
        NestGenerator.saplingChanceOverride = 1.0F;
        try {
            for (int i = 0; i < 2; i++) {
                boolean flowers = i == 0;
                BlockPos base = c.area();
                BlockPos at = base.add(16, 1, 16);
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        c.world.setBlockState(base.add(16 + dx, 0, 16 + dz), Blocks.GRASS.getDefaultState(), 2);
                    }
                }
                if (flowers) {
                    c.world.setBlockState(at.add(2, 0, 1), Blocks.YELLOW_FLOWER.getDefaultState(), 2);
                }
                IBlockState sapling = Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.BIRCH);
                c.world.setBlockState(at, sapling, 2);
                ((BlockSapling) Blocks.SAPLING).generateTree(c.world, at, sapling, new Random(5));
                c.ok(BeeNests.isNestTree(c.world.getBlockState(at)), "the birch grew (" + (flowers ? "flowers" : "no flowers") + ")");
                NestGenerator.growPending(c.world);
                BlockPos nest = findNest(c, at, 2);
                if (flowers) {
                    if (c.ok(nest != null, "a birch grown near a flower gets a nest")) {
                        checkNest(c, nest, at, "sapling birch");
                    }
                } else {
                    c.ok(nest == null, "a birch grown with no flower near gets none");
                }
            }
        } finally {
            NestGenerator.saplingChanceOverride = null;
        }
    }

    /** Real terrain: a forest far from spawn, with nests on every tree for the test, generated by the world. */
    private void realWorld(Check c) {
        Random rand = new Random(7);
        BlockPos found = c.world.getBiomeProvider().findBiomePosition(-30000, 30000, 2000, Collections.singletonList(Biomes.FOREST), rand);
        if (!c.ok(found != null, "a forest to test in")) {
            return;
        }
        Biome forest = Biomes.FOREST;
        BeeNests.setBiomeChance(forest, 1.0F);
        int nests = 0;
        int bad = 0;
        try {
            int cx = found.getX() >> 4;
            int cz = found.getZ() >> 4;
            for (int x = cx - 3; x <= cx + 3; x++) {
                for (int z = cz - 3; z <= cz + 3; z++) {
                    c.world.getChunkProvider().provideChunk(x, z);
                }
            }
            for (int x = (cx - 2) * 16; x < (cx + 2) * 16; x++) {
                for (int z = (cz - 2) * 16; z < (cz + 2) * 16; z++) {
                    int top = c.world.getHeight(x, z);
                    for (int y = top; y > top - 20 && y > 1; y--) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (c.world.getBlockState(p).getBlock() == BeeContent.beeNest) {
                            nests++;
                            boolean onLog = false;
                            for (EnumFacing f : new EnumFacing[] {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH}) {
                                onLog |= BeeNests.isNestTree(c.world.getBlockState(p.offset(f)));
                            }
                            TileEntityBeehive hive = (TileEntityBeehive) c.world.getTileEntity(p);
                            if (!onLog || hive == null || hive.getBeeCount() < 2) {
                                bad++;
                            }
                        }
                    }
                }
            }
        } finally {
            BeeNests.setBiomeChance(forest, 0.002F);
        }
        c.ok(nests >= 5, "a generated forest with every tree eligible has nests: " + nests + " in 16 chunks at " + found);
        c.eq(0, bad, "every generated nest is on a trunk with bees");
    }
}

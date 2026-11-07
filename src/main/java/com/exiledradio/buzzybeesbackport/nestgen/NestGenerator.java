package com.exiledradio.buzzybeesbackport.nestgen;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.api.BeeFlowers;
import com.exiledradio.buzzybeesbackport.api.BeeNests;
import com.exiledradio.buzzybeesbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.SaplingGrowTreeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Stream G: bee nests on trees, as 1.15's {@code BeehiveTreeDecorator}.
 *
 * <ul>
 *   <li><b>Generation</b>: after a chunk is populated, every oak and birch trunk in its decoration area gets
 *       a nest with its biome's chance ({@link BeeNests#chance}). 1.12.2 has no per-tree hook, so the trunks
 *       are found by looking: a nest-tree log standing on dirt or grass, under leaves.</li>
 *   <li><b>Saplings</b>: an oak or birch sapling with a flower within 2 blocks (one up or down) grows a
 *       nest 5% of the time, in any biome or dimension. The sapling event fires before the tree exists, so it
 *       is remembered and the nest placed at the end of the same tick.</li>
 *   <li><b>Placement</b>: on the trunk one block below the lowest leaves (never below the trunk's base),
 *       on its west, east or south side, facing south, only where the nest and the block south of it are
 *       air; 2-3 bees inside, already partway through their rest.</li>
 * </ul>
 * Finds the nest block by name, so it does nothing with the bee stream switched off.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class NestGenerator {

    /** 1.15.2's chance for a sapling near flowers. */
    public static final float SAPLING_CHANCE = 0.05F;
    private static final EnumFacing[] SIDES = {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.SOUTH};

    /** Saplings that started growing this tick near flowers, per world, with the roll already made. */
    private static final Map<World, List<BlockPos>> PENDING = Collections.synchronizedMap(new WeakHashMap<World, List<BlockPos>>());

    /** Test hook: replaces the sapling chance when set. */
    public static Float saplingChanceOverride;

    private NestGenerator() {
    }

    @Nullable
    private static Block nestBlock() {
        Block block = ForgeRegistries.BLOCKS.getValue(Content.id("bee_nest"));
        return block != null && Content.id("bee_nest").equals(block.getRegistryName()) ? block : null;
    }

    // ---- generation ----

    @SubscribeEvent
    public static void populated(PopulateChunkEvent.Post event) {
        if (!ModConfig.nests.enabled || event.getWorld().isRemote || nestBlock() == null) {
            return;
        }
        World world = event.getWorld();
        int x0 = event.getChunkX() * 16 + 8;
        int z0 = event.getChunkZ() * 16 + 8;
        Random rand = new Random(world.getSeed() ^ (event.getChunkX() * 341873128712L + event.getChunkZ() * 132897987541L) ^ 0x6265656EL);
        scan(world, rand, x0, z0, false);
    }

    /**
     * Gives every nest-tree trunk in the 16x16 area from (x0, z0) its biome's chance of a nest.
     *
     * @param always true to give every trunk a nest (tests)
     * @return how many nests were placed
     */
    public static int scan(World world, Random rand, int x0, int z0, boolean always) {
        int placed = 0;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos base = trunkBase(world, x0 + dx, z0 + dz);
                if (base == null) {
                    continue;
                }
                float chance = always ? 1.0F : BeeNests.chance(world.getBiome(base), ModConfig.nests.moddedBiomes);
                if (chance > 0.0F && rand.nextFloat() < chance && decorate(world, rand, base)) {
                    placed++;
                }
            }
        }
        return placed;
    }

    /** @return the lowest log of a nest-tree trunk in this column, standing on dirt or grass, or null */
    @Nullable
    static BlockPos trunkBase(World world, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, world.getHeight(x, z), z);
        int bottom = Math.max(1, p.getY() - 40);
        // Down through air and leaves to the first log.
        while (p.getY() > bottom) {
            p.setY(p.getY() - 1);
            IBlockState s = world.getBlockState(p);
            if (BeeNests.isNestTree(s)) {
                break;
            }
            if (!(s.getBlock() instanceof BlockLeaves) && !world.isAirBlock(p) && !s.getBlock().isReplaceable(world, p)) {
                return null;
            }
        }
        if (!BeeNests.isNestTree(world.getBlockState(p))) {
            return null;
        }
        while (p.getY() > bottom && BeeNests.isNestTree(world.getBlockState(p.down()))) {
            p.setY(p.getY() - 1);
        }
        Block ground = world.getBlockState(p.down()).getBlock();
        return ground instanceof BlockGrass || ground instanceof BlockDirt ? p.toImmutable() : null;
    }

    /**
     * Puts a nest on the trunk standing at {@code base}, as 1.15's decorator.
     *
     * @return true if one was placed
     */
    public static boolean decorate(World world, Random rand, BlockPos base) {
        Block nest = nestBlock();
        if (nest == null) {
            return false;
        }
        int top = base.getY();
        while (BeeNests.isNestTree(world.getBlockState(new BlockPos(base.getX(), top + 1, base.getZ())))) {
            top++;
        }
        int lowestLeaf = -1;
        for (int y = base.getY() + 1; y <= top + 3 && lowestLeaf < 0; y++) {
            for (int dx = -2; dx <= 2 && lowestLeaf < 0; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (world.getBlockState(new BlockPos(base.getX() + dx, y, base.getZ() + dz)).getBlock() instanceof BlockLeaves) {
                        lowestLeaf = y;
                        break;
                    }
                }
            }
        }
        EnumFacing side = SIDES[rand.nextInt(SIDES.length)];
        int y = lowestLeaf >= 0 ? Math.max(lowestLeaf - 1, base.getY()) : Math.min(base.getY() + 1 + rand.nextInt(3), top);
        BlockPos log = new BlockPos(base.getX(), y, base.getZ());
        if (!BeeNests.isNestTree(world.getBlockState(log))) {
            return false;
        }
        BlockPos at = log.offset(side);
        if (!world.isAirBlock(at) || !world.isAirBlock(at.south())) {
            return false;
        }
        IBlockState state = nest.getDefaultState();
        for (IProperty<?> p : state.getPropertyKeys()) {
            if ("facing".equals(p.getName())) {
                state = withFacing(state, p);
            }
        }
        world.setBlockState(at, state, 2);
        fill(world, rand, at);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState withFacing(IBlockState state, IProperty p) {
        return state.withProperty(p, EnumFacing.SOUTH);
    }

    /** 2-3 bees, each already 0-598 ticks into its rest. */
    private static void fill(World world, Random rand, BlockPos at) {
        TileEntity te = world.getTileEntity(at);
        if (!(te instanceof com.exiledradio.buzzybeesbackport.bee.TileEntityBeehive)) {
            return;
        }
        int n = 2 + rand.nextInt(2);
        for (int i = 0; i < n; i++) {
            Entity bee = EntityList.createEntityByIDFromName(Content.id("bee"), world);
            if (bee == null) {
                return;
            }
            NBTTagCompound data = new NBTTagCompound();
            if (bee.writeToNBTOptional(data)) {
                ((com.exiledradio.buzzybeesbackport.bee.TileEntityBeehive) te).addBeeData(data, rand.nextInt(599), false);
            }
        }
    }

    // ---- saplings ----

    /** From the mod's pre-init: the sapling event is posted on Forge's terrain-generation bus, not the main one. */
    public static void preInit() {
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.register(new Object() {
            @SubscribeEvent
            public void grow(SaplingGrowTreeEvent event) {
                sapling(event);
            }
        });
    }

    static void sapling(SaplingGrowTreeEvent event) {
        World world = event.getWorld();
        if (!ModConfig.nests.enabled || world.isRemote || nestBlock() == null) {
            return;
        }
        IBlockState state = world.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BlockSapling)) {
            return;
        }
        BlockPlanks.EnumType type = state.getValue(BlockSapling.TYPE);
        if (type != BlockPlanks.EnumType.OAK && type != BlockPlanks.EnumType.BIRCH) {
            return;
        }
        float chance = saplingChanceOverride != null ? saplingChanceOverride : SAPLING_CHANCE;
        if (!hasFlowersNear(world, event.getPos()) || event.getRand().nextFloat() >= chance) {
            return;
        }
        List<BlockPos> list = PENDING.get(world);
        if (list == null) {
            list = new ArrayList<BlockPos>();
            PENDING.put(world, list);
        }
        list.add(event.getPos().toImmutable());
    }

    /** 1.15.2's {@code Tree.hasFlowers}: a flower within 2 blocks across and 1 up or down. */
    static boolean hasFlowersNear(World world, BlockPos pos) {
        for (BlockPos p : BlockPos.getAllInBoxMutable(pos.add(-2, -1, -2), pos.add(2, 1, 2))) {
            if (BeeFlowers.isFlower(world.getBlockState(p))) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            growPending(event.world);
        }
    }

    /** Puts nests on the trees that grew this tick from flowered saplings. */
    public static void growPending(World world) {
        List<BlockPos> list = PENDING.remove(world);
        if (list == null) {
            return;
        }
        for (BlockPos pos : list) {
            if (BeeNests.isNestTree(world.getBlockState(pos))) {
                decorate(world, world.rand, pos);
            }
        }
    }
}

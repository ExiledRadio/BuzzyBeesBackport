package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 1.15: a bell rings when it becomes powered. The bell is Village &amp; Pillage's (Rule 2), which has no
 * redstone handling; it is rung through its public {@code ringByVillager(World, BlockPos)}, found by name,
 * on the rising edge only. A block next to a bell changing is what can change its power, so the check
 * runs on Forge's neighbor-notify event.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class BellRedstone {

    private static final ResourceLocation BELL = new ResourceLocation("villagerbackport", "bell");
    private static final Map<World, Set<BlockPos>> POWERED = Collections.synchronizedMap(new WeakHashMap<World, Set<BlockPos>>());
    private static Method ring;
    private static boolean looked;
    private static int rings;

    private BellRedstone() {
    }

    @SubscribeEvent
    public static void notify(BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (!Tweaks.on() || !ModConfig.tweaks.bellRedstone || world.isRemote) {
            return;
        }
        for (EnumFacing side : event.getNotifiedSides()) {
            BlockPos pos = event.getPos().offset(side);
            if (BELL.equals(world.getBlockState(pos).getBlock().getRegistryName())) {
                check(world, pos);
            }
        }
        if (BELL.equals(event.getState().getBlock().getRegistryName())) {
            check(world, event.getPos());
        }
    }

    /** @return how many bells this has rung since start (the self-test reads it) */
    public static int rings() {
        return rings;
    }

    /** Rings the bell if it has just become powered; remembers power so it rings once per rising edge. */
    public static boolean check(World world, BlockPos pos) {
        boolean powered = world.isBlockPowered(pos);
        Set<BlockPos> set = POWERED.get(world);
        if (set == null) {
            set = new HashSet<BlockPos>();
            POWERED.put(world, set);
        }
        boolean was = set.contains(pos);
        if (powered == was) {
            return false;
        }
        if (powered) {
            set.add(pos.toImmutable());
            return ring(world, pos);
        }
        set.remove(pos);
        return false;
    }

    private static boolean ring(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        if (!looked) {
            looked = true;
            try {
                ring = block.getClass().getMethod("ringByVillager", World.class, BlockPos.class);
            } catch (NoSuchMethodException e) {
                BuzzyBeesBackport.LOGGER.warn("The bell has no ring method; bells will not ring on redstone.");
            }
        }
        if (ring == null || !ring.getDeclaringClass().isInstance(block)) {
            return false;
        }
        try {
            ring.invoke(block, world, pos);
            rings++;
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}

package com.exiledradio.buzzybeesbackport.bee;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The loaded hives and nests of each world, by chunk: 1.12.2's stand-in for 1.15's point-of-interest
 * search that homeless bees use to find a hive within 20 blocks.
 *
 * <p>Hive tile entities add themselves when they load and remove themselves when they are removed or
 * their chunk unloads, so a search only looks at the few chunks around the bee.
 */
final class HiveIndex {

    private static final Map<World, Map<Long, Set<BlockPos>>> WORLDS =
            Collections.synchronizedMap(new WeakHashMap<World, Map<Long, Set<BlockPos>>>());

    private HiveIndex() {
    }

    private static long key(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    static synchronized void add(World world, BlockPos pos) {
        Map<Long, Set<BlockPos>> chunks = WORLDS.get(world);
        if (chunks == null) {
            chunks = new HashMap<Long, Set<BlockPos>>();
            WORLDS.put(world, chunks);
        }
        Set<BlockPos> set = chunks.get(key(pos));
        if (set == null) {
            set = new HashSet<BlockPos>();
            chunks.put(key(pos), set);
        }
        set.add(pos.toImmutable());
    }

    static synchronized void remove(World world, BlockPos pos) {
        Map<Long, Set<BlockPos>> chunks = WORLDS.get(world);
        if (chunks != null) {
            Set<BlockPos> set = chunks.get(key(pos));
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) {
                    chunks.remove(key(pos));
                }
            }
        }
    }

    /** @return the indexed hives within {@code radius} blocks (any height) of a position */
    static synchronized List<BlockPos> near(World world, BlockPos center, int radius) {
        List<BlockPos> out = new ArrayList<BlockPos>();
        Map<Long, Set<BlockPos>> chunks = WORLDS.get(world);
        if (chunks == null) {
            return out;
        }
        long r2 = (long) radius * radius;
        for (int cx = (center.getX() - radius) >> 4; cx <= (center.getX() + radius) >> 4; cx++) {
            for (int cz = (center.getZ() - radius) >> 4; cz <= (center.getZ() + radius) >> 4; cz++) {
                Set<BlockPos> set = chunks.get(ChunkPos.asLong(cx, cz));
                if (set != null) {
                    for (BlockPos pos : set) {
                        if (pos.distanceSq(center) <= r2) {
                            out.add(pos);
                        }
                    }
                }
            }
        }
        return out;
    }
}

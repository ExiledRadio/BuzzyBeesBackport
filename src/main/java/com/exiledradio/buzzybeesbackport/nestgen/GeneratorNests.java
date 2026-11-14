package com.exiledradio.buzzybeesbackport.nestgen;

import com.exiledradio.backportcore.terrain.DecorationSource;
import com.exiledradio.backportcore.worldgen.FlatWorlds;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;

/**
 * Whether the populate scan gives a chunk's trees their nests, or the world has them another way or not at all.
 *
 * <ul>
 *   <li>A generator that places today's trees with their nests itself (Backport Core's {@link DecorationSource}:
 *       Caves &amp; Cliffs Backport's 1.18 terrain places 1.18's trees with their beehive decorators, bees included):
 *       the scan would add a second nest to trees that rolled theirs.</li>
 *   <li>A superflat world without {@code decoration} ({@link FlatWorlds#decoration}): superflat grows no trees, and
 *       so no nests, unless its preset has {@code decoration}. 1.12's flat generator still fires the populate events,
 *       and its villages build log corners and farm borders.</li>
 * </ul>
 */
public final class GeneratorNests {

    private GeneratorNests() {
    }

    /** @return true when the populate scan should look for trunks in a chunk this generator just populated */
    public static boolean scans(World world, @Nullable IChunkGenerator generator) {
        return FlatWorlds.decoration(world) && !DecorationSource.places(world, generator, "bee_nest");
    }
}

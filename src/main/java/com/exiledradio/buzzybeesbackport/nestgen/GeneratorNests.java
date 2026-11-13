package com.exiledradio.buzzybeesbackport.nestgen;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;

import java.lang.reflect.Method;

/**
 * Whether a world's generator already gives its trees their nests (Backport Core 1.2.0's {@code DecorationSource}:
 * Caves &amp; Cliffs Backport's 1.18 terrain places 1.18's trees with their beehive decorators, bees included). Then
 * the populate scan would add a second nest to trees that rolled theirs. This mod requires Core 1.1.0, which has no
 * such interface, so it is looked up once by name: missing, the answer is always no.
 */
final class GeneratorNests {

    private static final Method PLACES = find();

    private GeneratorNests() {
    }

    private static Method find() {
        try {
            return Class.forName("com.exiledradio.backportcore.terrain.DecorationSource")
                    .getMethod("places", World.class, IChunkGenerator.class, String.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    static boolean placedBy(World world, IChunkGenerator generator) {
        if (PLACES == null) {
            return false;
        }
        try {
            return (Boolean) PLACES.invoke(null, world, generator, "bee_nest");
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}

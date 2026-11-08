package com.exiledradio.buzzybeesbackport;

import com.exiledradio.buzzybeesbackport.network.NetworkHandler;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Buzzy Bees Backport: Minecraft 1.15's bees, hives, honey, iron golem repair and smaller changes for 1.12.2.
 *
 * <h2>Layout</h2>
 * One package per stream of content (see {@code docs/PLAN.md}): each registers its own blocks, items,
 * entities and sounds from its own {@code @Mod.EventBusSubscriber} class, behind its own config
 * category's {@code enabled} switch. {@code common} holds what several streams share. This class only
 * calls each stream's lifecycle hooks, one line per stream.
 *
 * <h2>Behavior source</h2>
 * Current Java Edition as minecraft.wiki describes it, with decompiled vanilla code for the numbers the
 * wiki leaves out. Where today's game differs from 1.15, today's wins.
 */
@Mod(
        modid = BuzzyBeesBackport.MOD_ID,
        name = BuzzyBeesBackport.MOD_NAME,
        version = BuzzyBeesBackport.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:backportcore@[1.2.0,)"
)
public final class BuzzyBeesBackport {

    public static final String MOD_ID = "buzzybeesbackport";
    public static final String MOD_NAME = "Buzzy Bees Backport";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.Instance(MOD_ID)
    public static BuzzyBeesBackport instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 1.15's structure placement numbers, for Backport Core's table (seed parity with 1.15).
        com.exiledradio.buzzybeesbackport.worldgen.Placement115.register();
        NetworkHandler.init();
        com.exiledradio.buzzybeesbackport.common.Compat.preInit();
        com.exiledradio.buzzybeesbackport.nestgen.NestGenerator.preInit();
        com.exiledradio.buzzybeesbackport.migrate.FutureMcPairs.preInit();

        // Client-only setup lives in a class a dedicated server never loads. The call names no client
        // type, so this class verifies on a server - a guarded method here would not.
        if (event.getSide().isClient()) {
            com.exiledradio.buzzybeesbackport.client.ClientSetup.init();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.exiledradio.buzzybeesbackport.bee.BeeContent.init();
        com.exiledradio.buzzybeesbackport.tweaks.Tweaks.init();
        if (event.getSide().isClient()) {
            com.exiledradio.buzzybeesbackport.golem.client.GolemClient.init();
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
    }

    /**
     * Starts the headless self-test when a test build asks for one ({@code -Pselftest=<stream>}).
     *
     * <p>Reached by name, so a release jar - which has no test classes - never links against them.
     */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        String selftest = System.getProperty(MOD_ID + ".selftest");
        if (selftest == null || selftest.isEmpty()) {
            return;
        }
        try {
            Class.forName("com.exiledradio.buzzybeesbackport.selftest.SelfTestRunner")
                    .getMethod("start", String.class).invoke(null, selftest);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("SELFTEST DONE pass=0 fail=1: '{}' requested but the test classes are not in this build.", selftest, e);
            // A test run must always end, or it holds its port and its log files.
            net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        }
    }
}

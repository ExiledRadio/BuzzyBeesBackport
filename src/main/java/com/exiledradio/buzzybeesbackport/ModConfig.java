package com.exiledradio.buzzybeesbackport;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Configuration: one category per stream, each with an {@code enabled} switch.
 *
 * <h2>Rules for streams</h2>
 * <ul>
 *   <li>A stream edits only inside its own category class.</li>
 *   <li>Category classes extend {@code Object} directly. Forge 1.12 crashes at startup ("Unknown type")
 *       on a category class that extends another one.</li>
 *   <li>{@code enabled} is read once, during registration, and needs a restart: switching a stream off
 *       means its blocks and items are never registered, so a world that has them loses them.</li>
 *   <li>A changed default never reaches an installed game (Forge keeps the value in the file), so a
 *       default that must change gets a new key.</li>
 *   <li>Every field has an explicit {@code @Config.LangKey}, with a name line and a {@code .tooltip} line
 *       in {@code en_us.lang} (the config screen shows the tooltip in place of the comment).</li>
 * </ul>
 */
@Config(modid = BuzzyBeesBackport.MOD_ID, name = BuzzyBeesBackport.MOD_ID)
public final class ModConfig {

    @Config.LangKey("config.buzzybeesbackport.honey")
    @Config.Comment("Honey bottles, honeycomb, honey blocks and honeycomb blocks.")
    public static final Honey honey = new Honey();

    @Config.LangKey("config.buzzybeesbackport.bees")
    @Config.Comment("Bees, bee nests and beehives.")
    public static final Bees bees = new Bees();

    @Config.LangKey("config.buzzybeesbackport.nests")
    @Config.Comment("Bee nests generating on trees and growing with saplings near flowers.")
    public static final Nests nests = new Nests();

    @Config.LangKey("config.buzzybeesbackport.golems")
    @Config.Comment("Iron golems cracking as they are hurt, and repairing them with iron ingots.")
    public static final Golems golems = new Golems();

    @Config.LangKey("config.buzzybeesbackport.tweaks")
    @Config.Comment("1.15's smaller changes, each with its own switch.")
    public static final Tweaks tweaks = new Tweaks();

    @Config.LangKey("config.buzzybeesbackport.compat")
    @Config.Comment("Worlds and blocks from other mods that add the same things.")
    public static final Compat compat = new Compat();

    // ------------------------------------------------------------------------------------------
    // Categories. Each stream owns exactly one.
    // ------------------------------------------------------------------------------------------

    public static final class Honey {
        @Config.RequiresMcRestart
        @Config.Comment("Honey bottles, honeycomb, honey blocks and honeycomb blocks. Off, nothing is registered.")
        @Config.LangKey("config.buzzybeesbackport.honey.enabled")
        public boolean enabled = true;
    }

    public static final class Bees {
        @Config.RequiresMcRestart
        @Config.Comment("Bees, bee nests and beehives. Off, nothing is registered.")
        @Config.LangKey("config.buzzybeesbackport.bees.enabled")
        public boolean enabled = true;
    }

    public static final class Nests {
        @Config.Comment("Bee nests generate on oak and birch trees in newly generated chunks, and sometimes grow with saplings near flowers.")
        @Config.LangKey("config.buzzybeesbackport.nests.enabled")
        public boolean enabled = true;

        @Config.Comment({"Other mods' biomes get nests too, by their Forge biome types: plains like plains (5% of trees),",
                "broadleaf forests like forests (0.2%)."})
        @Config.LangKey("config.buzzybeesbackport.nests.moddedBiomes")
        public boolean moddedBiomes = true;
    }

    public static final class Golems {
        @Config.Comment("Iron golems show cracks as they lose health and can be repaired with iron ingots.")
        @Config.LangKey("config.buzzybeesbackport.golems.enabled")
        public boolean enabled = true;
    }

    public static final class Tweaks {
        @Config.RequiresMcRestart
        @Config.Comment("1.15's smaller changes. Off, none of them apply, whatever their own switches say.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.enabled")
        public boolean enabled = true;

        @Config.Comment("End stone bricks take as long to mine as stone (hardness 3) and resist explosions like it (9).")
        @Config.LangKey("config.buzzybeesbackport.tweaks.endStoneBricks")
        public boolean endStoneBricks = true;

        @Config.Comment("A boat burns long enough to smelt 6 items.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.boatFuel")
        public boolean boatFuel = true;

        @Config.Comment("A wet sponge placed in the Nether dries at once.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.spongeDries")
        public boolean spongeDries = true;

        @Config.Comment("A rail with nothing to connect to is placed along the way you are facing.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.railFacing")
        public boolean railFacing = true;

        @Config.Comment("Large ferns drop wheat seeds like tall grass.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.fernSeeds")
        public boolean fernSeeds = true;

        @Config.Comment("Using a spawn egg on an adult zombie, husk, zombie villager, zombie pigman or drowned of its kind spawns a baby.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.spawnEggBabies")
        public boolean spawnEggBabies = true;

        @Config.Comment("Animals spawning in groups are sometimes babies: 5% for cows, sheep, pigs, chickens and foxes, 10% for wolves, horses and llamas.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.babyAnimals")
        public boolean babyAnimals = true;

        @Config.Comment("Parrots no longer imitate endermen, polar bears, wolves or zombie pigmen.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.parrotImitation")
        public boolean parrotImitation = true;

        @Config.Comment("Food and cake can be eaten in Creative mode.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.creativeEating")
        public boolean creativeEating = true;

        @Config.Comment("Using a bed sets your respawn point and says so, even when it is too early to sleep.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.bedSpawn")
        public boolean bedSpawn = true;

        @Config.Comment("Adds the game rules fallDamage, fireDamage, drowningDamage, doImmediateRespawn and doInsomnia.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.gameRules")
        public boolean gameRules = true;

        @Config.Comment("Dispensers fill glass bottles from water, place armor stands and light campfires with fire charges.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.dispensers")
        public boolean dispensers = true;

        @Config.Comment("A bell rings when it is powered by redstone.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.bellRedstone")
        public boolean bellRedstone = true;

        @Config.Comment("The title screen can show 1.15's three new splash texts.")
        @Config.LangKey("config.buzzybeesbackport.tweaks.splashTexts")
        public boolean splashTexts = true;
    }

    public static final class Compat {
        @Config.Comment("Convert blocks, items and bees left behind by another mod that added them, when that mod is removed from a world.")
        @Config.LangKey("config.buzzybeesbackport.compat.convertRemoved")
        public boolean convertRemoved = true;
    }

    // ------------------------------------------------------------------------------------------

    /** Saves edits made in the in-game config screen. */
    @Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (BuzzyBeesBackport.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(BuzzyBeesBackport.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }

    private ModConfig() {
    }
}

package com.exiledradio.buzzybeesbackport.bee;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Stream B: bees, bee nests and beehives. Registered only when {@code bees.enabled} is on.
 *
 * <p>Honey items come from stream H by name ({@code Owners.ours}); with honey switched off, hives still
 * fill, but shears and bottles take nothing.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class BeeContent {

    /** Entity id 1 of this mod (plan: bees 1-4). */
    public static final int BEE_ENTITY_ID = 1;
    /** The damage type: "<player> was stung to death". */
    public static final String STING_DAMAGE = BuzzyBeesBackport.MOD_ID + ".sting";

    public static final SoundEvent BEE_LOOP = sound("entity.bee.loop");
    public static final SoundEvent BEE_LOOP_AGGRESSIVE = sound("entity.bee.loop_aggressive");
    public static final SoundEvent BEE_POLLINATE = sound("entity.bee.pollinate");
    public static final SoundEvent BEE_STING = sound("entity.bee.sting");
    public static final SoundEvent BEE_HURT = sound("entity.bee.hurt");
    public static final SoundEvent BEE_DEATH = sound("entity.bee.death");
    public static final SoundEvent HIVE_ENTER = sound("block.beehive.enter");
    public static final SoundEvent HIVE_EXIT = sound("block.beehive.exit");
    public static final SoundEvent HIVE_WORK = sound("block.beehive.work");
    public static final SoundEvent HIVE_SHEAR = sound("block.beehive.shear");
    public static final SoundEvent HIVE_DRIP = sound("block.beehive.drip");

    public static BlockBeehive beeNest;
    public static BlockBeehive beehive;

    private BeeContent() {
    }

    private static SoundEvent sound(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    public static boolean on() {
        return beehive != null;
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> event) {
        if (!ModConfig.bees.enabled) {
            return;
        }
        beeNest = Content.block(event.getRegistry(), "bee_nest", new BlockBeehive(true), "axe");
        beehive = Content.block(event.getRegistry(), "beehive", new BlockBeehive(false), "axe");
        GameRegistry.registerTileEntity(TileEntityBeehive.class, Content.id("beehive"));
        // 1.15's FireBlock values: nests catch quickly, hives slowly; both burn the same.
        Blocks.FIRE.setFireInfo(beeNest, 30, 20);
        Blocks.FIRE.setFireInfo(beehive, 5, 20);
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> event) {
        if (!ModConfig.bees.enabled) {
            return;
        }
        Content.blockItem(event.getRegistry(), beeNest, new ItemBeehive(beeNest));
        Content.blockItem(event.getRegistry(), beehive, new ItemBeehive(beehive));
    }

    @SubscribeEvent
    public static void entities(RegistryEvent.Register<EntityEntry> event) {
        if (!ModConfig.bees.enabled) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityBee.class)
                .id(Content.id("bee"), BEE_ENTITY_ID)
                .name(BuzzyBeesBackport.MOD_ID + ".bee")
                .tracker(80, 3, true)
                .egg(0xEDC343, 0x43241B)
                .build());
        // Vanilla's spawn egg, listed where 1.19.2 lists the bee's (Backport Core's creative placement).
        com.exiledradio.backportcore.creative.CreativePlacement.placeSpawnEgg(Content.id("bee"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sounds(RegistryEvent.Register<SoundEvent> event) {
        if (!ModConfig.bees.enabled) {
            return;
        }
        event.getRegistry().registerAll(BEE_LOOP, BEE_LOOP_AGGRESSIVE, BEE_POLLINATE, BEE_STING, BEE_HURT, BEE_DEATH,
                HIVE_ENTER, HIVE_EXIT, HIVE_WORK, HIVE_SHEAR, HIVE_DRIP);
    }

    /** From the mod's init, after every registry: the dispenser behaviors. */
    public static void init() {
        if (on()) {
            BeeDispensing.register();
        }
    }
}

package com.exiledradio.buzzybeesbackport.honey;

import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;

/**
 * Stream H: the honey bottle, honeycomb, honey block and honeycomb block.
 *
 * <p>Registered only when {@code honey.enabled} is on. Other streams reach these by name
 * ({@code Owners.ours("honey_bottle")}), never through this class.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class HoneyContent {

    // Sound events are created eagerly: blocks take their SoundType in the block event, which 1.12 fires
    // before the sound event. They are only registered in the sound event.
    public static final SoundEvent HONEY_BREAK = sound("block.honey_block.break");
    public static final SoundEvent HONEY_FALL = sound("block.honey_block.fall");
    public static final SoundEvent HONEY_HIT = sound("block.honey_block.hit");
    public static final SoundEvent HONEY_PLACE = sound("block.honey_block.place");
    public static final SoundEvent HONEY_SLIDE = sound("block.honey_block.slide");
    public static final SoundEvent HONEY_STEP = sound("block.honey_block.step");
    public static final SoundEvent HONEY_DRINK = sound("item.honey_bottle.drink");
    public static final SoundEvent CORAL_BREAK = sound("block.coral_block.break");
    public static final SoundEvent CORAL_FALL = sound("block.coral_block.fall");
    public static final SoundEvent CORAL_HIT = sound("block.coral_block.hit");
    public static final SoundEvent CORAL_PLACE = sound("block.coral_block.place");
    public static final SoundEvent CORAL_STEP = sound("block.coral_block.step");

    /** 1.15's {@code SoundType.HONEY}. */
    public static final SoundType HONEY = new SoundType(1.0F, 1.0F, HONEY_BREAK, HONEY_STEP, HONEY_PLACE, HONEY_HIT, HONEY_FALL);
    /** 1.13's {@code SoundType.CORAL}, the honeycomb block's. */
    public static final SoundType CORAL = new SoundType(1.0F, 1.0F, CORAL_BREAK, CORAL_STEP, CORAL_PLACE, CORAL_HIT, CORAL_FALL);

    public static BlockHoney honeyBlock;
    public static Block honeycombBlock;
    public static ItemHoneyBottle honeyBottle;
    public static Item honeycomb;

    private HoneyContent() {
    }

    private static SoundEvent sound(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    /** @return true if this stream registered its content */
    public static boolean on() {
        return honeyBlock != null;
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> event) {
        if (!ModConfig.honey.enabled) {
            return;
        }
        honeyBlock = Content.block(event.getRegistry(), "honey_block", new BlockHoney(), null);
        honeycombBlock = Content.block(event.getRegistry(), "honeycomb_block", new BlockHoneycomb(), null);
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> event) {
        if (!ModConfig.honey.enabled) {
            return;
        }
        Content.registerBlockItem(event.getRegistry(), honeyBlock);
        Content.registerBlockItem(event.getRegistry(), honeycombBlock);
        honeyBottle = Content.item(event.getRegistry(), "honey_bottle", new ItemHoneyBottle());
        honeycomb = Content.item(event.getRegistry(), "honeycomb", new Item());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sounds(RegistryEvent.Register<SoundEvent> event) {
        if (!ModConfig.honey.enabled) {
            return;
        }
        event.getRegistry().registerAll(HONEY_BREAK, HONEY_FALL, HONEY_HIT, HONEY_PLACE, HONEY_SLIDE, HONEY_STEP,
                HONEY_DRINK, CORAL_BREAK, CORAL_FALL, CORAL_HIT, CORAL_PLACE, CORAL_STEP);
    }
}

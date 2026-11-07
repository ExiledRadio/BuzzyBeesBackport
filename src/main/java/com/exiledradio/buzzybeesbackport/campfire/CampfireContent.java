package com.exiledradio.buzzybeesbackport.campfire;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.common.Content;
import com.exiledradio.buzzybeesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

/**
 * 1.14's campfire, which Village &amp; Pillage Backport owns: the block, its item, its tile entity and its
 * crackle - registered here only when that mod is absent.
 *
 * <h2>Why a copy</h2>
 * The family's rule is that every mod fully works on its own, and 1.15's bees are calmed by a lit campfire
 * under their hive. So without the owner this mod registers an identical campfire under its own id,
 * {@code buzzybeesbackport:campfire}: the owner's classes copied verbatim by
 * {@code tools/campfire/copy_from_owner.py} (only the package and mod id renamed), the same three
 * properties and metadata, so the owner can later take ours over by renaming it. This class and
 * {@code client.CampfireClient} are the only hand-written ones: registration differs.
 *
 * <h2>With the owner installed</h2>
 * {@link #enabled()} is false, nothing here is registered, and every field stays null. Bees read the
 * owner's campfire by name ({@code api.BeeSmoke}). The copied event handlers stay subscribed but only ever
 * act on this package's {@link BlockCampfire}, which then never exists in the world.
 *
 * <h2>A world that loses the owner</h2>
 * Its campfires become ours ({@link #remapOwner}), lit or out, facing the same way. What was cooking on
 * them is lost: 1.12.2 has no missing-mapping event for tile entities, so the owner's
 * {@code villagerbackport:campfire} tile entity is skipped and ours starts empty.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class CampfireContent {

    /** The owner's (and our) registry path. */
    public static final String NAME = "campfire";

    @Nullable
    public static BlockCampfire campfire;

    @Nullable
    public static SoundEvent crackle;

    private CampfireContent() {
    }

    /**
     * @return true if this mod adds its campfire in this game: switched on and Village &amp; Pillage
     * absent. Fixed for the whole session.
     */
    public static boolean enabled() {
        return ModConfig.campfire.enabled && !Owners.present(Owners.VILLAGE_AND_PILLAGE);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            BuzzyBeesBackport.LOGGER.info(Owners.present(Owners.VILLAGE_AND_PILLAGE)
                    ? "The campfire comes from Village & Pillage Backport; not registering a copy."
                    : "The campfire is off (campfire.enabled).");
            return;
        }
        // The block sets its own harvest tool (axe), as the owner's does.
        campfire = Content.block(event.getRegistry(), NAME, new BlockCampfire(), null);
        GameRegistry.registerTileEntity(TileEntityCampfire.class, Content.id(NAME));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (campfire == null) {
            return;
        }
        Content.registerBlockItem(event.getRegistry(), campfire);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation id = Content.id("block.campfire.crackle");
        crackle = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(crackle);
    }

    /**
     * A world saved with Village &amp; Pillage, opened without it: its campfire becomes ours. The states
     * and metadata are the same, so this is a rename and nothing moves.
     */
    @SubscribeEvent
    public static void remapOwner(RegistryEvent.MissingMappings<Block> event) {
        if (campfire == null) {
            return;
        }
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            if (isOwners(mapping.key)) {
                mapping.remap(campfire);
            }
        }
    }

    /** As {@link #remapOwner}, for the item. */
    @SubscribeEvent
    public static void remapOwnerItems(RegistryEvent.MissingMappings<Item> event) {
        if (campfire == null) {
            return;
        }
        Item ours = Item.getItemFromBlock(campfire);
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (isOwners(mapping.key)) {
                mapping.remap(ours);
            }
        }
    }

    private static boolean isOwners(ResourceLocation key) {
        return Owners.VILLAGE_AND_PILLAGE.equals(key.getNamespace()) && NAME.equals(key.getPath());
    }
}

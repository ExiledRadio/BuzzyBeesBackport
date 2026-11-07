package com.exiledradio.buzzybeesbackport.campfire.client;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.campfire.CampfireContent;
import com.exiledradio.buzzybeesbackport.campfire.TileEntityCampfire;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The campfire copy's client setup: the renderer for the food on it and the smoke textures. Hand-written,
 * where the rest of the package is copied from Village &amp; Pillage: the owner's version also maps the
 * models, which here {@code client.ClientSetup} does for every item, and the blockstate file lists
 * {@code signal_fire} so the default state mapping finds every state.
 *
 * <p>Reached from {@code ClientSetup.init()}, so a dedicated server never loads it. Does nothing when the
 * copy is not registered (Village &amp; Pillage installed, or switched off).
 *
 * <h2>Smoke textures</h2>
 * 1.14's twelve {@code big_smoke} frames are stitched into the block atlas, which is the one texture
 * sheet 1.12.2's particle manager will draw a mod's particle from (layer 1). Each plume picks one frame
 * at random and keeps it ({@link CampfireParticles}).
 */
public final class CampfireClient {

    static final TextureAtlasSprite[] SMOKE = new TextureAtlasSprite[12];

    private CampfireClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new CampfireClient());
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        if (CampfireContent.campfire == null) {
            return;
        }
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCampfire.class, new TileEntityCampfireRenderer());
    }

    @SubscribeEvent
    public void stitch(TextureStitchEvent.Pre event) {
        if (CampfireContent.campfire == null) {
            return;
        }
        for (int i = 0; i < SMOKE.length; i++) {
            SMOKE[i] = event.getMap().registerSprite(
                    new ResourceLocation(BuzzyBeesBackport.MOD_ID, "particle/big_smoke_" + i));
        }
    }
}

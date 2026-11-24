package com.exiledradio.buzzybeesbackport.tweaks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Client half of the tweaks: closing the death screen once the server has respawned the player
 * (doImmediateRespawn). Also registers {@link ElytraClient} (elytra while rising) and {@link HardcoreDeathScreen}
 * (1.15's Hardcore buttons). 1.15's splashes and the Sneak and Sprint toggles are Backport Core's since Core 1.4.7.
 */
public final class TweaksClient {

    private TweaksClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new TweaksClient());
        MinecraftForge.EVENT_BUS.register(new ElytraClient());
        MinecraftForge.EVENT_BUS.register(new HardcoreDeathScreen());
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.END && mc.currentScreen instanceof GuiGameOver && mc.player != null
                && mc.player.getHealth() > 0.0F && !mc.player.isDead) {
            mc.displayGuiScreen(null);
        }
    }
}

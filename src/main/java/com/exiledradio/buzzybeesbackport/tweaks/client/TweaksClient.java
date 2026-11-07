package com.exiledradio.buzzybeesbackport.tweaks.client;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.Random;

/**
 * Client half of the tweaks: 1.15's three new splash texts, and closing the death screen once the server
 * has respawned the player (doImmediateRespawn). Also registers {@link ElytraClient} (elytra while rising)
 * and {@link ToggleKeys} (toggle sneak and sprint).
 */
public final class TweaksClient {

    /** 1.15's additions to splashes.txt, which vanilla never translates either. */
    private static final String[] SPLASHES = {
            "In case it isn't obvious, foxes aren't players.",
            "Buzzy Bees!",
            "Minecraft Java Edition presents: Disgusting Bugs",
    };
    /** About how many splash lines 1.12.2 has, so ours come up as often as 1.15's would. */
    private static final int VANILLA_SPLASHES = 330;
    private static final Random RANDOM = new Random();

    private TweaksClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new TweaksClient());
        MinecraftForge.EVENT_BUS.register(new ElytraClient());
        MinecraftForge.EVENT_BUS.register(new ToggleKeys());
    }

    @SubscribeEvent
    public void open(GuiOpenEvent event) {
        if (ModConfig.tweaks.enabled && ModConfig.tweaks.splashTexts && event.getGui() instanceof GuiMainMenu
                && RANDOM.nextInt(VANILLA_SPLASHES + SPLASHES.length) < SPLASHES.length) {
            try {
                ReflectionHelper.setPrivateValue(GuiMainMenu.class, (GuiMainMenu) event.getGui(),
                        SPLASHES[RANDOM.nextInt(SPLASHES.length)], "splashText", "field_73975_c");
            } catch (RuntimeException ignored) {
                // The title screen keeps vanilla's pick.
            }
        }
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

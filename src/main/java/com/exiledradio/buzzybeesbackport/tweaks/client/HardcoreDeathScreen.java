package com.exiledradio.buzzybeesbackport.tweaks.client;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.15's Hardcore death screen: "Spectate World" and "Title Screen" (1.12.2: "Spectate World" and "Delete World", or
 * "Leave Server" on a server). 1.15's Title Screen leaves the world and opens the title screen at once, with no
 * question; 1.12.2's second button opened the title screen without leaving the world, and never deleted anything.
 * The world is deleted by hand from the world list, as in 1.15.
 */
public final class HardcoreDeathScreen {

    /** The second button of 1.12.2's death screen. */
    static final int SECOND_BUTTON = 1;

    static boolean on(GuiGameOver screen) {
        Minecraft mc = Minecraft.getMinecraft();
        return ModConfig.tweaks.enabled && ModConfig.tweaks.hardcoreDeathScreen && mc.world != null
                && mc.world.getWorldInfo().isHardcoreModeEnabled();
    }

    @SubscribeEvent
    public void init(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiGameOver) || !on((GuiGameOver) event.getGui())) {
            return;
        }
        for (GuiButton button : event.getButtonList()) {
            if (button.id == SECOND_BUTTON) {
                button.displayString = I18n.format("deathScreen.titleScreen");
            }
        }
    }

    @SubscribeEvent
    public void press(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof GuiGameOver) || event.getButton().id != SECOND_BUTTON || !on((GuiGameOver) event.getGui())) {
            return;
        }
        event.setCanceled(true);
        // 1.15's exitToTitleScreen, as 1.12.2's own confirmed "Title Screen" does it.
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null) {
            mc.world.sendQuittingDisconnectingPacket();
        }
        mc.loadWorld((WorldClient) null);
        mc.displayGuiScreen(new GuiMainMenu());
    }
}

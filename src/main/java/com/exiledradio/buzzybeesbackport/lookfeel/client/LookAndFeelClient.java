package com.exiledradio.buzzybeesbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.lookfeel.LookAndFeel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.Display;

/**
 * 1.15's look and feel on the client: the Sneak and Sprint options kept equal to this mod's config, the hardcore
 * death screen's "Title screen" button (19w35a) and the window title (1.15.2).
 */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    /** For the client self-test. */
    public static volatile String lastTitle;
    public static volatile int hardcoreButtons;

    private String shownTitle;

    private LookAndFeelClient() {
    }

    public static void init() {
        // The config is the setting; the options show it on Accessibility Settings and set it from there.
        LookAndFeel.TOGGLE_SNEAK.set(ModConfig.tweaks.toggleSneak ? "toggle" : "hold");
        LookAndFeel.TOGGLE_SPRINT.set(ModConfig.tweaks.toggleSprint ? "toggle" : "hold");
        LookAndFeel.TOGGLE_SNEAK.onChange(v -> save(true, "toggle".equals(v)));
        LookAndFeel.TOGGLE_SPRINT.onChange(v -> save(false, "toggle".equals(v)));
        MinecraftForge.EVENT_BUS.register(new LookAndFeelClient());
    }

    private static void save(boolean sneak, boolean toggle) {
        if (sneak) {
            ModConfig.tweaks.toggleSneak = toggle;
        } else {
            ModConfig.tweaks.toggleSprint = toggle;
        }
        ConfigManager.sync(BuzzyBeesBackport.MOD_ID, Config.Type.INSTANCE);
    }

    /** The config screen changed the toggles: the options follow. */
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.PostConfigChangedEvent event) {
        if (BuzzyBeesBackport.MOD_ID.equals(event.getModID())) {
            LookAndFeel.TOGGLE_SNEAK.set(ModConfig.tweaks.toggleSneak ? "toggle" : "hold");
            LookAndFeel.TOGGLE_SPRINT.set(ModConfig.tweaks.toggleSprint ? "toggle" : "hold");
        }
    }

    /**
     * 19w35a: the hardcore death screen's "Delete world" / "Leave server" button becomes "Title screen", which
     * leaves the world (saving it) for the title screen, as 1.15's {@code DeathScreen.exitToTitleScreen}.
     */
    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiGameOver && isHardcore()) {
            for (GuiButton b : event.getButtonList()) {
                if (b.id == 1) {
                    b.displayString = I18n.format("deathScreen.titleScreen");
                    hardcoreButtons++;
                }
            }
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getGui() instanceof GuiGameOver && event.getButton().id == 1 && isHardcore()) {
            event.setCanceled(true);
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null) {
                mc.world.sendQuittingDisconnectingPacket();
            }
            mc.loadWorld(null);
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    private static boolean isHardcore() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.world != null && mc.world.getWorldInfo().isHardcoreModeEnabled();
    }

    /** 1.15.2 {@code Minecraft.createTitle}, set whenever it changes. */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Presentation.isAtLeast("1.15.2")) {
            return;
        }
        String title = title(Minecraft.getMinecraft());
        if (!title.equals(this.shownTitle)) {
            this.shownTitle = title;
            lastTitle = title;
            Display.setTitle(title);
        }
    }

    /**
     * "Minecraft", a star when the game is modded (always, under Forge), the version, and once connected, the
     * kind of session.
     */
    public static String title(Minecraft mc) {
        StringBuilder b = new StringBuilder("Minecraft* ").append(ForgeVersion.mcVersion);
        NetHandlerPlayClient connection = mc.getConnection();
        if (connection != null && connection.getNetworkManager().isChannelOpen()) {
            b.append(" - ");
            IntegratedServer server = mc.getIntegratedServer();
            ServerData data = mc.getCurrentServerData();
            if (server != null && !server.getPublic()) {
                b.append(I18n.format("title.singleplayer"));
            } else if (mc.isConnectedToRealms()) {
                b.append(I18n.format("title.multiplayer.realms"));
            } else if (server == null && (data == null || !data.isOnLAN())) {
                b.append(I18n.format("title.multiplayer.other"));
            } else {
                b.append(I18n.format("title.multiplayer.lan"));
            }
        }
        return b.toString();
    }
}

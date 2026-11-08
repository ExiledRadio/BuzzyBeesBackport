package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.backportcore.presentation.client.TitleSplashes;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.lookfeel.LookAndFeel;
import com.exiledradio.buzzybeesbackport.lookfeel.client.LookAndFeelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 1.15's look and feel on the client, for {@code gradlew runClient -Pselftest=client}: at the title screen it checks
 * what Backport Core shows (newest version, panorama, overlay, splash pool, loading revision), the Sneak and Sprint options, the window title and the lang
 * overrides, all in code; nothing reaches the window. Logs {@code LOOKFEEL ...} lines and
 * {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes the game.
 */
@SideOnly(Side.CLIENT)
public final class ClientLookFeelTest {

    private int ticks;
    private int pass;
    private int fail;
    private boolean done;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientLookFeelTest());
        BuzzyBeesBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
        } else {
            this.fail++;
            BuzzyBeesBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || this.done || !(mc.currentScreen instanceof GuiMainMenu) || ++this.ticks < 400) {
            return;
        }
        this.done = true;
        try {
            run(mc);
        } catch (Throwable t) {
            BuzzyBeesBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
        }
        BuzzyBeesBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    private void run(Minecraft mc) {
        GameVersion newest = Presentation.newest();
        LoadingRevision loading = Presentation.loadingRevision();
        ResourceLocation[] faces = PanoramaRenderer.faces();
        List<String> pool = TitleSplashes.lastPool;
        StringBuilder installed = new StringBuilder();
        for (Contribution c : Presentation.installed()) {
            installed.append(c.version()).append(c.owners()).append(' ');
        }
        BuzzyBeesBackport.LOGGER.info("LOOKFEEL installed: {}", installed.toString().trim());
        BuzzyBeesBackport.LOGGER.info("LOOKFEEL newest={} panorama={} overlay={} loading={} splashes={} panoramaFrames={}",
                newest, faces == null ? null : faces[0], Presentation.panoramaOverlay(), loading,
                pool == null ? -1 : pool.size(), PanoramaRenderer.frames);
        boolean alone = newest.equals(GameVersion.parse("1.15.2"));

        ok(PanoramaRenderer.modern(), "the title screen is drawn the 1.13+ way");
        ok(PanoramaRenderer.frames > 20, "the panorama was drawn: " + PanoramaRenderer.frames + " frames");
        if (alone) {
            ok(faces != null && faces[0].getNamespace().equals(BuzzyBeesBackport.MOD_ID), "1.15's bee panorama is shown: "
                    + (faces == null ? null : faces[0]));
            ok(Presentation.panoramaOverlay() != null && Presentation.panoramaOverlay().getNamespace().equals(BuzzyBeesBackport.MOD_ID),
                    "1.15's overlay is used: " + Presentation.panoramaOverlay());
            ok(loading == LoadingRevision.GRID_1_14, "the world-loading screen is 1.14's grid at 1.15: " + loading);
        }
        ok(pool != null, "the title screen drew from Core's splash pool");
        if (pool != null) {
            for (String s : new String[] {"Buzzy Bees!", "In case it isn't obvious, foxes aren't players.",
                    "Minecraft Java Edition presents: Disgusting Bugs"}) {
                ok(pool.contains(s), "1.15's splash \"" + s + "\" is in the pool");
            }
        }

        // Sneak and Sprint show this mod's config.
        ok(LookAndFeel.TOGGLE_SNEAK.get().equals(ModConfig.tweaks.toggleSneak ? "toggle" : "hold"), "Sneak shows tweaks.toggleSneak");
        ok(LookAndFeel.TOGGLE_SPRINT.get().equals(ModConfig.tweaks.toggleSprint ? "toggle" : "hold"), "Sprint shows tweaks.toggleSprint");
        boolean before = ModConfig.tweaks.toggleSneak;
        LookAndFeel.TOGGLE_SNEAK.cycle(false);
        ok(ModConfig.tweaks.toggleSneak != before, "cycling Sneak on Accessibility Settings sets the config");
        LookAndFeel.TOGGLE_SNEAK.cycle(false);
        ok(ModConfig.tweaks.toggleSneak == before, "and back");

        // 1.15.2's window title at the title screen: no session yet.
        String expected = "Minecraft* " + net.minecraftforge.common.ForgeVersion.mcVersion;
        ok(expected.equals(LookAndFeelClient.title(mc)), "the title screen's window title: " + LookAndFeelClient.title(mc));
        ok(expected.equals(org.lwjgl.opengl.Display.getTitle()), "the window shows it: " + org.lwjgl.opengl.Display.getTitle());

        String[][] lang = {
                {"options.key.hold", "Hold"},
                {"options.key.toggle", "Toggle"},
                {"title.singleplayer", "Singleplayer"},
                {"chat.link.open", "Open in Browser"},
        };
        for (String[] kv : lang) {
            ok(kv[1].equals(I18n.format(kv[0])), "I18n " + kv[0] + " = \"" + kv[1] + "\", got \"" + I18n.format(kv[0]) + "\"");
        }
    }
}

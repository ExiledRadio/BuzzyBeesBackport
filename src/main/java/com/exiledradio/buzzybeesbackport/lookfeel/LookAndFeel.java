package com.exiledradio.buzzybeesbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.option.CycleOption;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import net.minecraft.util.ResourceLocation;

/**
 * 1.15's look and feel: the changes 1.15 made beyond its content. Backport Core owns every piece only one version
 * can have at a time (panorama, splash list, world-loading screen, options screens) and shows the newest installed
 * version's; this class hands Core 1.15's pieces.
 *
 * <ul>
 *   <li>19w41a: the bee panorama (1.15.2's six faces, which the asset index carries; the jar's are 1x1
 *       placeholders) and the jar's {@code panorama_overlay.png} (1.13's gradient, unchanged);</li>
 *   <li>the three splashes 1.15.2's {@code splashes.txt} adds to 1.14.4's (none removed);</li>
 *   <li>19w41a: Sneak and Sprint, Hold or Toggle, on Accessibility Settings after Auto-Jump. They show and set this
 *       mod's {@code tweaks.toggleSneak} and {@code tweaks.toggleSprint}, which the toggle itself
 *       ({@code tweaks.client.ToggleKeys}) reads;</li>
 *   <li>19w35a: the hardcore death screen's second button is "Title screen" (client side,
 *       {@code lookfeel.client.LookAndFeelClient});</li>
 *   <li>1.15.2: the window title names the session ("Minecraft* 1.12.2 - Singleplayer").</li>
 * </ul>
 * The world-loading screen stays 1.14's grid (Core's table). Menu music did not change in 1.15
 * ({@code music.menu} is menu1 to menu4 in 1.14.4 and 1.15.2). "Open in Browser" is in the lang file.
 */
public final class LookAndFeel {

    private static final String ID = BuzzyBeesBackport.MOD_ID;
    private static final String[] NAMES = {"hold", "toggle"};
    private static final String[] VALUE_KEYS = {"options.key.hold", "options.key.toggle"};

    /** 1.15's "Sneak: Hold / Toggle" (Accessibility Settings). */
    public static final CycleOption TOGGLE_SNEAK = new CycleOption(ID + ":toggle_crouch", "key.sneak", OptionScreen.ACCESSIBILITY,
            NAMES, VALUE_KEYS, "hold");

    /** 1.15's "Sprint: Hold / Toggle" (Accessibility Settings). */
    public static final CycleOption TOGGLE_SPRINT = new CycleOption(ID + ":toggle_sprint", "key.sprint", OptionScreen.ACCESSIBILITY,
            NAMES, VALUE_KEYS, "hold");

    private static boolean registered;

    private LookAndFeel() {
    }

    /** Pre-init, both sides. */
    public static void preInit() {
        if (registered) {
            return;
        }
        registered = true;
        Contribution v115 = Presentation.version("1.15");
        v115.panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"))
                .addSplashes("In case it isn't obvious, foxes aren't players.", "Buzzy Bees!",
                        "Minecraft Java Edition presents: Disgusting Bugs")
                // Accessibility Settings in 1.15.2: ... Chat Text Opacity, Auto-Jump, Sneak, Sprint.
                .option(TOGGLE_SNEAK.after("AUTO_JUMP"))
                .option(TOGGLE_SPRINT.after(TOGGLE_SNEAK.id()));
        // 1.15.2's window title is client code (LookAndFeelClient); the version is declared on both sides.
        Presentation.version("1.15.2");
    }
}

package com.exiledradio.buzzybeesbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import net.minecraft.util.ResourceLocation;

/**
 * 1.15's look and feel: the changes 1.15 made beyond its content. Backport Core owns every piece only one version
 * can have at a time (panorama, splash list, world-loading screen, options screens) and shows the newest installed
 * version's; this class declares 1.15 and 1.15.2 installed and hands Core what stays this mod's: the bee panorama
 * (19w41a; 1.15.2's six faces, which the asset index carries; the jar's are 1x1 placeholders) and the jar's
 * {@code panorama_overlay.png} (1.13's gradient, unchanged).
 *
 * <p>Since Backport Core 1.4.7, Core itself has 1.15's other pieces, whichever backport is installed: Sneak and Sprint,
 * Hold or Toggle (still {@code buzzybeesbackport:} ids, so saved values carry over) and the toggle itself,
 * {@code widgets.png}, the check box and the multiplayer warning (1.15.2), the window title (1.15.2), the F3 text
 * without "chunk updates", the startup logo, the Accessibility icon, 1.15's splashes and vanilla's text as 1.15.2
 * words it. The hardcore death screen's "Title screen" button (19w35a) is Core's death-screen owner, by version.
 */
public final class LookAndFeel {

    private static final String ID = BuzzyBeesBackport.MOD_ID;

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
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"));
        // 1.15.2's pieces are Backport Core's; the version is declared on both sides.
        Presentation.version("1.15.2");
    }
}

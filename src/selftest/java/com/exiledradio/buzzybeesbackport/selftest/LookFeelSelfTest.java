package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.PresentationOption;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.backportcore.presentation.versions.Look115;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.15's look and feel registered with Backport Core, its assets, the Sneak and Sprint options and the lang. Since
 * Backport Core 1.4.7 all but the panorama and the version declarations are Core's; they are checked as Core serves
 * them with this mod installed, and the old config's Toggle carries over ({@code ToggleMigration}).
 */
public final class LookFeelSelfTest implements SelfTest {

    private static final String ID = BuzzyBeesBackport.MOD_ID;
    private static final String CORE = "backportcore";

    @Override
    public String name() {
        return "lookfeel";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        migration(c);
        panorama(c);
        splashes(c);
        options(c);
        lang(c);
    }

    private static void registration(Check c) {
        Contribution v115 = Presentation.contribution("1.15");
        Contribution v1152 = Presentation.contribution("1.15.2");
        c.ok(v115 != null && v115.owners().contains(ID), "1.15 is registered by " + ID + ": " + v115);
        c.ok(v1152 != null && v1152.owners().contains(ID), "1.15.2 is registered by " + ID + ": " + v1152);
        c.eq(GameVersion.parse("1.15.2"), Presentation.newest(), "the newest installed version (this mod alone)");
        c.eq(LoadingRevision.GRID_1_14, Presentation.loadingRevision(), "the world-loading screen stays 1.14's grid at 1.15");
        // Core's finished presentation module: 1.15.2's assets, warning and vanilla text, Core's 1.15 table.
        // Backport Core's own since 1.4.7 (1.15.2's mojang.png is 1.14.4's file).
        String[][] assets = {
                {"textures/gui/widgets.png", "256x256", "look/1.15/"}, {"textures/gui/checkbox.png", "32x64", "look/1.15/"},
                {"textures/gui/title/mojang.png", "512x512", "look/1.14/"},
        };
        for (String[] a : assets) {
            ResourceLocation src = Presentation.asset(a[0]);
            c.eq(new ResourceLocation(CORE, a[2] + a[0]), src, "Core serves vanilla's " + a[0] + " from 1.15.2's");
            c.ok(LookFeelSelfTest.class.getResource("/assets/" + ID + "/" + a[0]) == null, a[0] + " is no longer in this mod's jar");
            int[] size;
            try {
                size = src == null ? null : pngSize(src);
            } catch (IOException e) {
                size = null;
            }
            c.ok(size != null && a[1].equals(size[0] + "x" + size[1]), a[0] + " is 1.15.2's: " + Arrays.toString(size));
        }
        c.eq(new ResourceLocation(CORE, "look/1.15/textures/gui/accessibility.png"), Presentation.titleAccessibilityIcon(), "the title Accessibility icon");
        String[] warn = Presentation.multiplayerWarning();
        c.ok(warn != null && "multiplayerWarning.header".equals(warn[0]), "1.15.2's multiplayer warning: " + Arrays.toString(warn));
        c.ok(Presentation.vanillaTexts().contains(new ResourceLocation(CORE, "look/1.15/vanilla_text")), "1.15's vanilla text is registered (Core's)");
        c.ok(com.exiledradio.backportcore.presentation.VanillaRevisions.whiteButtonText()
                && com.exiledradio.backportcore.presentation.VanillaRevisions.hardcoreTitleButton(), "Core's 1.15 table: white labels, hardcore title button");
        c.eq(com.exiledradio.backportcore.presentation.VanillaRevisions.Startup.BAR_1_14, com.exiledradio.backportcore.presentation.VanillaRevisions.startup(),
                "the startup screen is 1.14's (1.15 kept it)");
    }

    private static void migration(Check c) {
        List<String> cfg = Arrays.asList("tweaks {", "    B:enabled=true", "    B:toggleSneak=true", "    B:toggleSprint=false", "}");
        c.ok(com.exiledradio.buzzybeesbackport.lookfeel.client.ToggleMigration.wasOn(cfg, "toggleSneak"), "an old config's toggleSneak=true is read");
        c.ok(!com.exiledradio.buzzybeesbackport.lookfeel.client.ToggleMigration.wasOn(cfg, "toggleSprint"), "toggleSprint=false is not");
        c.ok(!com.exiledradio.buzzybeesbackport.lookfeel.client.ToggleMigration.wasOn(cfg, "enabled2"), "a missing key is not");
    }

    private static void panorama(Check c) throws IOException {
        List<Presentation.Panorama> all = Presentation.panoramas();
        Presentation.Panorama last = all.isEmpty() ? null : all.get(all.size() - 1);
        if (!c.ok(last != null && last.version.equals(GameVersion.parse("1.15")), "the newest panorama is 1.15's: " + all.size())) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_" + i + ".png"), last.faces[i], "panorama face " + i);
            int[] size = pngSize(last.faces[i]);
            c.ok(size != null && size[0] == 1024 && size[1] == 1024, "face " + i + " is 1.15.2's 1024x1024 image: " + Arrays.toString(size));
        }
        ResourceLocation overlay = Presentation.panoramaOverlay();
        c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"), overlay, "the panorama overlay");
        int[] size = overlay == null ? null : pngSize(overlay);
        c.ok(size != null && size[0] == 16 && size[1] == 128, "1.15.2's overlay is the 16x128 gradient: " + Arrays.toString(size));
    }

    private static final String[] ADDED = {"In case it isn't obvious, foxes aren't players.", "Buzzy Bees!",
            "Minecraft Java Edition presents: Disgusting Bugs"};

    private static void splashes(Check c) throws IOException {
        List<String> base = lines("/assets/minecraft/texts/splashes.txt");
        c.ok(base.size() > 300, "1.12.2's splashes.txt is readable: " + base.size());
        List<String> pool = Splashes.pool(base, Presentation.installed(), f -> lines(f));
        for (String s : ADDED) {
            c.ok(!base.contains(s) && pool.contains(s), "the splash pool has 1.15's \"" + s + "\"");
        }
        // Since Backport Core 1.4.7 the 1.13 and 1.14 splash changes are Core's own and come with any later backport.
        Set<String> expected = new LinkedHashSet<String>(base);
        expected.addAll(Arrays.asList("Thank you for the fish!", "All rumors are true!", "Truly gone fishing!"));
        expected.removeAll(lines(new ResourceLocation(CORE, "look/1.14/texts/splashes_removed.txt")));
        expected.addAll(lines(new ResourceLocation(CORE, "look/1.14/texts/splashes_added.txt")));
        expected.add("{PLAYER} IS YOU");
        expected.addAll(Arrays.asList(ADDED));
        c.eq(expected.size(), pool.size(), "pool size: 1.12.2's set with 1.13's and 1.14's changes plus 1.15's three (1.15 removed none)");
    }

    private static void options(Check c) {
        Map<String, PresentationOption> byId = new HashMap<String, PresentationOption>();
        for (PresentationOption o : Presentation.options()) {
            byId.put(o.id(), o);
        }
        PresentationOption[] mine = {Look115.TOGGLE_SNEAK, Look115.TOGGLE_SPRINT};
        String[] labels = {"key.sneak", "key.sprint"};
        for (int i = 0; i < mine.length; i++) {
            PresentationOption o = mine[i];
            c.ok(byId.get(o.id()) == o, o.id() + " is registered");
            c.eq(OptionScreen.ACCESSIBILITY, o.screen(), o.id() + "'s screen");
            c.eq(GameVersion.parse("1.15"), o.since(), o.id() + "'s since");
            c.eq(labels[i], o.labelKey(), o.id() + "'s label");
        }
        c.eq("AUTO_JUMP", Look115.TOGGLE_SNEAK.after(), "Sneak follows Auto-Jump");
        c.eq(Look115.TOGGLE_SNEAK.id(), Look115.TOGGLE_SPRINT.after(), "Sprint follows Sneak");
        c.eq("hold", Look115.TOGGLE_SNEAK.get(), "Sneak defaults to Hold");
        c.eq("options.key.hold", Look115.TOGGLE_SPRINT.valueKey(), "Sprint shows Hold");
        List<String> access = new ArrayList<String>();
        for (PresentationOption o : Presentation.optionsOn(OptionScreen.ACCESSIBILITY)) {
            access.add(o.id());
        }
        c.ok(access.indexOf(Look115.TOGGLE_SPRINT.id()) == access.indexOf(Look115.TOGGLE_SNEAK.id()) + 1
                && access.indexOf(Look115.TOGGLE_SNEAK.id()) >= 0, "Accessibility Settings lists Sneak, then Sprint: " + access);
    }

    private static final String[][] LANG = {
            {"options.key.hold", "Hold"},
            {"options.key.toggle", "Toggle"},
            {"title.singleplayer", "Singleplayer"},
            {"title.multiplayer.other", "Multiplayer (3rd-party)"},
            {"chat.link.open", "Open in Browser"},
    };

    private static final String[] LANGUAGES = {"de_de", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp",
            "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};

    private static void lang(Check c) throws IOException {
        Map<String, String> en = langFile("en_us");
        for (String[] kv : LANG) {
            c.eq(kv[1], en.get(kv[0]), "en_us " + kv[0]);
        }
        for (String code : LANGUAGES) {
            Map<String, String> l = langFile(code);
            int have = 0;
            for (String[] kv : LANG) {
                if (l.get(kv[0]) != null && !l.get(kv[0]).isEmpty()) {
                    have++;
                }
            }
            c.eq(LANG.length, have, code + " has 1.15's option, title and reworded vanilla keys");
        }
    }

    // ------------------------------------------------------------------------------------------

    /** Backport Core's lang file and its 1.15 vanilla text for one language (Core's since Core 1.4.7). */
    private static Map<String, String> langFile(String code) throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        for (String folder : new String[] {"lang/", "look/1.15/vanilla_text/"}) {
            for (String line : lines(new ResourceLocation(CORE, folder + code + ".lang"))) {
                int eq = line.indexOf('=');
                if (eq > 0 && !line.startsWith("#")) {
                    out.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        }
        return out;
    }

    private static List<String> lines(ResourceLocation rl) throws IOException {
        return lines("/assets/" + rl.getNamespace() + "/" + rl.getPath());
    }

    private static List<String> lines(String path) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("not on the classpath: " + path);
        }
        List<String> out = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    out.add(line);
                }
            }
        } finally {
            r.close();
        }
        return out;
    }

    private static int[] pngSize(ResourceLocation rl) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream("/assets/" + rl.getNamespace() + "/" + rl.getPath());
        if (in == null) {
            return null;
        }
        byte[] b = read(in);
        if (b.length < 24 || b[1] != 'P' || b[2] != 'N' || b[3] != 'G') {
            return null;
        }
        return new int[] {readInt(b, 16), readInt(b, 20)};
    }

    private static int readInt(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
    }

    private static byte[] read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}

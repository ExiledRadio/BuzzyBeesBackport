package com.exiledradio.buzzybeesbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.option.CycleOption;
import com.exiledradio.backportcore.presentation.option.OptionStore;
import com.exiledradio.backportcore.presentation.versions.Look115;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Up to 1.1.1 this mod's config had {@code tweaks.toggleSneak} and {@code tweaks.toggleSprint}, which the Sneak and
 * Sprint options on Accessibility Settings showed and set. Since 1.1.2 the options and the toggle are Backport Core's
 * (1.4.7), with the same ids, and the config switches are gone. An options.txt that has the options saved already
 * holds the config's value (every launch copied it there); where it has none, a {@code true} left in this mod's config
 * file becomes Toggle, once. Called from the client setup; names no client type (the server self-test reads it).
 */
public final class ToggleMigration {

    private ToggleMigration() {
    }

    /** Client pre-init. */
    public static void migrate() {
        File file = new File(Loader.instance().getConfigDir(), BuzzyBeesBackport.MOD_ID + ".cfg");
        if (!file.isFile()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            migrate(lines, "toggleSneak", Look115.TOGGLE_SNEAK);
            migrate(lines, "toggleSprint", Look115.TOGGLE_SPRINT);
        } catch (IOException | RuntimeException e) {
            BuzzyBeesBackport.LOGGER.warn("Could not read the old Toggle Sneak and Toggle Sprint settings from {}", file, e);
        }
    }

    /** @return true when {@code key} was on in the old config and the option had no saved value: it is Toggle now */
    static boolean migrate(List<String> configLines, String key, CycleOption option) {
        if (OptionStore.get(option.id()) != null || !wasOn(configLines, key)) {
            return false;
        }
        option.set("toggle");
        BuzzyBeesBackport.LOGGER.info("{}: Toggle, from this mod's old config ({})", option.id(), key);
        return true;
    }

    /** @return true when the Forge config lines have {@code B:<key>=true} */
    public static boolean wasOn(List<String> configLines, String key) {
        for (String line : configLines) {
            if (line.trim().equalsIgnoreCase("B:" + key + "=true")) {
                return true;
            }
        }
        return false;
    }
}

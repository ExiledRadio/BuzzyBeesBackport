package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Called from the patched {@code EntityPlayer.onLivingUpdate} (patch P5, {@code asm/ShoulderRidingTransformer})
 * in place of the {@code isRiding()} of its shoulder-drop test: 1.15 keeps parrots on the shoulders of a
 * riding player. Falling, water, flying, sleeping and being hurt still drop them, as in both versions.
 */
public final class ShoulderParrots {

    private ShoulderParrots() {
    }

    public static boolean ridingDrops(EntityPlayer player) {
        if (Tweaks.on() && ModConfig.tweaks.parrotsWhileRiding) {
            return false;
        }
        return player.isRiding();
    }
}

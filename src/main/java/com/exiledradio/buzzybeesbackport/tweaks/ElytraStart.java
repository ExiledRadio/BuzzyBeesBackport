package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;

/**
 * Called from the patched {@code NetHandlerPlayServer.processEntityAction} (patch P3,
 * {@code asm/ElytraStartTransformer}) in place of its {@code player.motionY} read, the "falling" half of
 * 1.12.2's start-gliding test. 1.15's {@code tryToStartFallFlying} asks only that the player is off the
 * ground, not already gliding, not in water and without Levitation; the rest of the test is vanilla's own.
 * The client half (sending the request while rising) is {@code tweaks.client.ElytraClient}.
 */
public final class ElytraStart {

    private ElytraStart() {
    }

    public static boolean on() {
        return Tweaks.on() && ModConfig.tweaks.elytraJump;
    }

    /** @return the speed vanilla compares with 0: "falling" for 1.15's rule, the real one when switched off */
    public static double fallSpeed(EntityPlayerMP player) {
        if (on()) {
            return player.isPotionActive(MobEffects.LEVITATION) ? 1.0D : -1.0D;
        }
        return player.motionY;
    }
}

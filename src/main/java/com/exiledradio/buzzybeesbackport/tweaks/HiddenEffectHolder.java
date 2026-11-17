package com.exiledradio.buzzybeesbackport.tweaks;

import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;

/**
 * What patch P6 ({@code asm/HiddenEffectsTransformer}) adds to {@code PotionEffect}: 1.15.2's hidden effect (the
 * weaker effect of the same kind waiting under this one) and setters for the details it takes back when this one
 * runs out. Every {@code PotionEffect} implements it once the patch is in; {@link HiddenEffects#holder} casts.
 */
public interface HiddenEffectHolder {

    @Nullable
    PotionEffect buzzybeesbackport$hidden();

    void buzzybeesbackport$setHidden(@Nullable PotionEffect hidden);

    /** Sets duration, amplifier, ambient and particles at once (1.15.2's {@code setDetailsFrom}, less the potion). */
    void buzzybeesbackport$set(int duration, int amplifier, boolean ambient, boolean particles);

    void buzzybeesbackport$setDuration(int duration);
}

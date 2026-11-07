package com.exiledradio.buzzybeesbackport.common;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;

/**
 * Soft integrations with other mods of the family, none of them required, and this mod's entries in
 * Backport Core's shared tables.
 *
 * <p>Calls into other family mods go through registry names or reflection, so this mod never links
 * against another mod's classes and loads the same with or without it. Backport Core is required and is
 * called directly.
 */
public final class Compat {

    private Compat() {
    }

    /** Called from the mod's pre-init. */
    public static void preInit() {
        // Rule 1: these blocks first shipped in 1.15, so this mod owns them. They stay ours even when
        // their stream is switched off (nobody else registers them then).
        BlockOwnership.declare(BuzzyBeesBackport.MOD_ID, "bee_nest", "beehive", "honey_block", "honeycomb_block");
        BlockOwnership.provides(BuzzyBeesBackport.MOD_ID, () -> ModConfig.honey.enabled || ModConfig.bees.enabled);
    }
}

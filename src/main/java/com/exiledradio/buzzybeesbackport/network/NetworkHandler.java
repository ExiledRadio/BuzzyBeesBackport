package com.exiledradio.buzzybeesbackport.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * The mod's one network channel.
 *
 * <h2>Packet ids</h2>
 * Each stream owns a block of five ids (see {@code docs/PLAN.md}) and registers its packets here, inside
 * its own block, so two streams can never pick the same number and merges stay mechanical:
 *
 * <pre>
 *   0-4   common          15-19 nests         30-34 migrate
 *   5-9   honey           20-24 golems
 *   10-14 bees            25-29 tweaks        35-39 campfire
 * </pre>
 *
 * <p>The channel name is short because Forge caps channel names at 20 characters, one fewer than the
 * mod id.
 */
public final class NetworkHandler {

    public static final String CHANNEL = "bbbackport";

    public static SimpleNetworkWrapper channel;

    private NetworkHandler() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        // Streams register their packets below, in id order.
        channel.registerMessage(com.exiledradio.buzzybeesbackport.campfire.PacketCampfireDowse.Handler.class,
                com.exiledradio.buzzybeesbackport.campfire.PacketCampfireDowse.class, 35, net.minecraftforge.fml.relauncher.Side.CLIENT);
    }
}

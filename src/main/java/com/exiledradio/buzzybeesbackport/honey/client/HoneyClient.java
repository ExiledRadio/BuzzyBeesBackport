package com.exiledradio.buzzybeesbackport.honey.client;

import com.exiledradio.buzzybeesbackport.honey.HoneyPistons;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Client half of the honey stream: honey pistons in the client's world, which moves the player. */
public final class HoneyClient {

    private HoneyClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new HoneyClient());
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.isGamePaused()) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            HoneyPistons.before(mc.world);
        } else {
            HoneyPistons.after(mc.world);
        }
    }
}

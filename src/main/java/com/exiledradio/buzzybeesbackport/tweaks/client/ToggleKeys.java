package com.exiledradio.buzzybeesbackport.tweaks.client;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

/**
 * 1.15's accessibility options "Sneak: Toggle" and "Sprint: Toggle", as the client switches
 * {@code toggleSneak} and {@code toggleSprint}.
 *
 * <p>1.15's {@code ToggleableKeyBinding}: in toggle mode a press of the key flips the binding's held state
 * and a release does nothing. 1.12.2 sets a binding's held state straight from each key event and when a
 * screen opens or closes, so the toggled state is put back after each event for that key and at the start
 * of every client tick. Everything that reads the key (sneaking, sprinting, other mods) then sees it held.
 */
public final class ToggleKeys {

    private static final Field PRESSED = ReflectionHelper.findField(KeyBinding.class, "pressed", "field_74513_e");

    private final Toggle sneak = new Toggle();
    private final Toggle sprint = new Toggle();

    private static final class Toggle {
        /** The toggled held state. */
        boolean down;
        /** Whether toggle mode was on at the last tick (to let go of the key once it is switched off). */
        boolean active;
    }

    private static boolean enabled() {
        return ModConfig.tweaks.enabled;
    }

    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        int code = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        handle(code, Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent());
    }

    @SubscribeEvent
    public void mouse(InputEvent.MouseInputEvent event) {
        int button = Mouse.getEventButton();
        if (button >= 0) {
            handle(button - 100, Mouse.getEventButtonState());
        }
    }

    private void handle(int code, boolean press) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!enabled() || code == 0) {
            return;
        }
        event(mc, mc.gameSettings.keyBindSneak, ModConfig.tweaks.toggleSneak, this.sneak, code, press);
        event(mc, mc.gameSettings.keyBindSprint, ModConfig.tweaks.toggleSprint, this.sprint, code, press);
    }

    private static void event(Minecraft mc, KeyBinding binding, boolean option, Toggle toggle, int code, boolean press) {
        if (!option || binding.getKeyCode() != code) {
            return;
        }
        if (press && mc.currentScreen == null && mc.player != null && binding.isActiveAndMatches(code)) {
            toggle.down = !toggle.down;
        }
        set(binding, toggle.down);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        tick(mc, mc.gameSettings.keyBindSneak, enabled() && ModConfig.tweaks.toggleSneak, this.sneak);
        tick(mc, mc.gameSettings.keyBindSprint, enabled() && ModConfig.tweaks.toggleSprint, this.sprint);
    }

    private static void tick(Minecraft mc, KeyBinding binding, boolean option, Toggle toggle) {
        if (option) {
            if (mc.player == null) {
                toggle.down = false; // a new world starts with the key let go
            }
            set(binding, toggle.down);
            toggle.active = true;
        } else if (toggle.active) {
            toggle.active = false;
            toggle.down = false;
            set(binding, false);
        }
    }

    private static void set(KeyBinding binding, boolean down) {
        try {
            PRESSED.setBoolean(binding, down);
        } catch (IllegalAccessException ignored) {
            // The key keeps vanilla's hold behavior.
        }
    }
}

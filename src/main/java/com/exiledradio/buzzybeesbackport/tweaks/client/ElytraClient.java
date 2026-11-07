package com.exiledradio.buzzybeesbackport.tweaks.client;

import com.exiledradio.buzzybeesbackport.tweaks.ElytraStart;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

/**
 * Client half of 1.15's elytra start: pressing jump in the air opens the elytra while still rising.
 *
 * <p>Forge's input event fires in {@code EntityPlayerSP.onLivingUpdate} right after the movement keys are
 * read and before vanilla's own elytra test, with the same motion and ground state that test sees. 1.12.2
 * sends {@code START_FALL_FLYING} only while falling; this sends it in the other case 1.15 allows
 * (rising or level), with 1.15's other conditions: a fresh jump press, not flying, riding or on a ladder, not
 * toggling creative flight with the same press, off the ground, not in water, no Levitation and a usable
 * elytra. The server accepts it through patch P3.
 */
public final class ElytraClient {

    private static final Field FLY_TOGGLE = ReflectionHelper.findField(EntityPlayer.class, "flyToggleTimer", "field_71101_bC");
    private static final Field AUTO_JUMP = ReflectionHelper.findField(EntityPlayerSP.class, "autoJumpTime", "field_189812_cs");

    /** Was jump held at the last input update (vanilla's own {@code flag}, including auto-jump). */
    private boolean wasJumping;

    @SubscribeEvent
    public void input(InputUpdateEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerSP)) {
            return;
        }
        EntityPlayerSP player = (EntityPlayerSP) event.getEntityPlayer();
        boolean jump = event.getMovementInput().jump;
        boolean pressed = jump && !this.wasJumping;
        this.wasJumping = jump || intField(AUTO_JUMP, player) > 0;
        if (!pressed || !ElytraStart.on() || player.motionY < 0.0D) {
            return; // falling: vanilla sends it itself
        }
        if (player.onGround || player.isElytraFlying() || player.capabilities.isFlying || player.isRiding() || player.isOnLadder()
                || player.isInWater() || player.isPotionActive(MobEffects.LEVITATION)) {
            return;
        }
        if (player.capabilities.allowFlying && intField(FLY_TOGGLE, player) != 0) {
            return; // this press toggles creative flight
        }
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (chest.getItem() == Items.ELYTRA && ItemElytra.isUsable(chest)) {
            player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
        }
    }

    private static int intField(Field field, Object owner) {
        try {
            return field.getInt(owner);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }
}

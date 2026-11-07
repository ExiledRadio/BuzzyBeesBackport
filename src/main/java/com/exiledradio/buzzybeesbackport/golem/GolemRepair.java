package com.exiledradio.buzzybeesbackport.golem;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.common.Content;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Stream I: iron golems crack as they lose health and are repaired with iron ingots, as 1.15's
 * {@code IronGolemEntity}.
 *
 * <p>Works on every {@code EntityIronGolem}: built by players, spawned by villages (Village &amp; Pillage's
 * golem spawner makes vanilla golems, so its golems need nothing more) or by other mods. Done with events,
 * so the golem class itself is untouched.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class GolemRepair {

    /** Health healed by one iron ingot: four bring a golem from 1 to its full 100. */
    public static final float INGOT_HEAL = 25.0F;

    public static final SoundEvent GOLEM_DAMAGE = sound("entity.iron_golem.damage");
    public static final SoundEvent GOLEM_REPAIR = sound("entity.iron_golem.repair");

    /** 1.15's crack stages: none down to 75% health, then low, medium below 50%, high below 25%. */
    public enum Cracks {
        NONE, LOW, MEDIUM, HIGH;

        public static Cracks of(float healthFraction) {
            if (healthFraction < 0.25F) {
                return HIGH;
            }
            if (healthFraction < 0.5F) {
                return MEDIUM;
            }
            if (healthFraction < 0.75F) {
                return LOW;
            }
            return NONE;
        }

        public static Cracks of(EntityLivingBase golem) {
            return of(golem.getHealth() / golem.getMaxHealth());
        }
    }

    private GolemRepair() {
    }

    private static SoundEvent sound(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(GOLEM_DAMAGE, GOLEM_REPAIR);
    }

    /** The crack sound when a hit takes the golem into a new stage. */
    @SubscribeEvent
    public static void damaged(LivingDamageEvent event) {
        EntityLivingBase golem = event.getEntityLiving();
        if (!ModConfig.golems.enabled || !(golem instanceof EntityIronGolem) || golem.world.isRemote) {
            return;
        }
        Cracks before = Cracks.of(golem);
        float after = Math.max(0.0F, golem.getHealth() - event.getAmount());
        if (after > 0.0F && Cracks.of(after / golem.getMaxHealth()) != before) {
            golem.playSound(GOLEM_DAMAGE, 1.0F, 1.0F);
        }
    }

    /** An iron ingot heals 25; refused at full health, kept in Creative. */
    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!ModConfig.golems.enabled || !(event.getTarget() instanceof EntityIronGolem)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        if (held.getItem() != Items.IRON_INGOT) {
            return;
        }
        if (repair((EntityIronGolem) event.getTarget(), player, held)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            player.swingArm(event.getHand());
        }
    }

    /** @return true if the golem was healed (on the client: if it would be) */
    public static boolean repair(EntityIronGolem golem, EntityPlayer player, ItemStack ingot) {
        if (golem.getHealth() >= golem.getMaxHealth() || !golem.isEntityAlive()) {
            return false;
        }
        if (golem.world.isRemote) {
            return true;
        }
        golem.heal(INGOT_HEAL);
        float pitch = 1.0F + (golem.getRNG().nextFloat() - golem.getRNG().nextFloat()) * 0.2F;
        golem.playSound(GOLEM_REPAIR, 1.0F, pitch);
        if (!player.capabilities.isCreativeMode) {
            ingot.shrink(1);
        }
        return true;
    }
}

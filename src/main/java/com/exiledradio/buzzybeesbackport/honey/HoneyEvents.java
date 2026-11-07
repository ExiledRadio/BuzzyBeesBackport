package com.exiledradio.buzzybeesbackport.honey;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The honey block's effects that are not the block's own methods, and the honey bottle's sound.
 *
 * <ul>
 *   <li><b>Half jumps</b> from honey: 1.15's jump factor 0.5 applies to the jump's base height (0.42),
 *       not to Jump Boost's extra.</li>
 *   <li><b>Through a carpet or slab</b>: 1.15 reads an entity's speed and jump factor from the block it is
 *       in, or, if that block has none, from the block half a block below its feet - so honey under a
 *       carpet still slows and holds. The block's own collision handles an entity on bare honey.</li>
 *   <li><b>The drinking sound</b>: 1.12.2 plays its generic drink for every drink; a player drinking honey
 *       hears 1.15's honey sound instead.</li>
 *   <li>Honey pistons on the server world ({@link HoneyPistons}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class HoneyEvents {

    private HoneyEvents() {
    }

    /** @return true if 1.15 would give this entity the honey block's speed and jump factor */
    static boolean onHoney(EntityLivingBase entity, boolean bareToo) {
        if (HoneyContent.honeyBlock == null) {
            return false;
        }
        World world = entity.world;
        BlockPos at = new BlockPos(entity.posX, entity.posY, entity.posZ);
        if (world.getBlockState(at).getBlock() == HoneyContent.honeyBlock) {
            // Standing on bare honey: the feet are inside its 15/16-high box.
            return bareToo;
        }
        // Anything else has a factor of 1, so the block half a block down counts: honey under a carpet or slab.
        BlockPos under = new BlockPos(entity.posX, entity.posY - 0.5000001D, entity.posZ);
        return world.getBlockState(under).getBlock() == HoneyContent.honeyBlock;
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!onHoney(entity, true)) {
            return;
        }
        double boost = entity.isPotionActive(MobEffects.JUMP_BOOST)
                ? 0.1D * (entity.getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) : 0.0D;
        double base = entity.motionY - boost;
        entity.motionY = base * BlockHoney.JUMP_FACTOR + boost;
    }

    /** Honey under a carpet or slab slows what walks on it; bare honey does it in its collision method. */
    @SubscribeEvent
    public static void walk(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.onGround && onHoney(entity, false)) {
            entity.motionX *= BlockHoney.SPEED_FACTOR;
            entity.motionZ *= BlockHoney.SPEED_FACTOR;
        }
    }

    @SubscribeEvent
    public static void drinkSound(PlaySoundAtEntityEvent event) {
        if (event.getSound() == SoundEvents.ENTITY_GENERIC_DRINK && event.getEntity() instanceof EntityPlayer
                && HoneyContent.honeyBottle != null) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            if (player.isHandActive() && player.getActiveItemStack().getItem() == HoneyContent.honeyBottle) {
                event.setSound(HoneyContent.HONEY_DRINK);
            }
        }
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            HoneyPistons.before(event.world);
        } else {
            HoneyPistons.after(event.world);
        }
    }
}

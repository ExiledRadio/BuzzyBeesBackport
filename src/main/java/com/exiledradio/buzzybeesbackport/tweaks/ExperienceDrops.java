package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.15's experience drop timing: a dying mob or player drops its experience at once, with its loot
 * ({@code LivingEntity.spawnDrops} calls {@code dropExperience}), not 20 ticks later at the end of the death
 * animation as 1.12.2's {@code onDeathUpdate} does.
 *
 * <p>At death (Forge's death event, last, when nobody cancelled it) the experience is worked out with
 * 1.12.2's own rule and amount, through Forge's experience event so other mods still change it, and the
 * orbs are spawned where the entity died; the later drop is then cancelled. Mobs whose class has its own
 * death animation code (the ender dragon, other mods' bosses) are left alone.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class ExperienceDrops {

    private static final Field RECENTLY_HIT = ReflectionHelper.findField(EntityLivingBase.class, "recentlyHit", "field_70718_bc");
    private static final Field ATTACKING_PLAYER = ReflectionHelper.findField(EntityLivingBase.class, "attackingPlayer", "field_70717_bb");
    private static final Method EXPERIENCE = ReflectionHelper.findMethod(EntityLivingBase.class, "getExperiencePoints", "func_70693_a",
            EntityPlayer.class);
    private static final Method CAN_DROP_LOOT = ReflectionHelper.findMethod(EntityLivingBase.class, "canDropLoot", "func_146066_aG");
    private static final Method IS_PLAYER = ReflectionHelper.findMethod(EntityLivingBase.class, "isPlayer", "func_70684_aJ");

    /** Entities whose experience was dropped at death; their end-of-animation drop is cancelled. */
    private static final Set<EntityLivingBase> DROPPED = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<EntityLivingBase, Boolean>()));
    private static final Map<Class<?>, Boolean> VANILLA_DEATH = new ConcurrentHashMap<Class<?>, Boolean>();

    private ExperienceDrops() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        EntityLivingBase dying = event.getEntityLiving();
        World world = dying.world;
        if (world.isRemote || !Tweaks.on() || !ModConfig.tweaks.xpWithLoot || DROPPED.contains(dying) || !vanillaDeath(dying.getClass())) {
            return;
        }
        try {
            boolean drops = (Boolean) IS_PLAYER.invoke(dying)
                    || RECENTLY_HIT.getInt(dying) > 0 && (Boolean) CAN_DROP_LOOT.invoke(dying) && world.getGameRules().getBoolean("doMobLoot");
            int xp = 0;
            if (drops) {
                EntityPlayer attacker = (EntityPlayer) ATTACKING_PLAYER.get(dying);
                xp = ForgeEventFactory.getExperienceDrop(dying, attacker, (Integer) EXPERIENCE.invoke(dying, attacker));
            }
            DROPPED.add(dying);
            while (xp > 0) {
                int orb = EntityXPOrb.getXPSplit(xp);
                xp -= orb;
                world.spawnEntity(new EntityXPOrb(world, dying.posX, dying.posY, dying.posZ, orb));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            BuzzyBeesBackport.LOGGER.debug("Experience for {} left to 1.12.2's timing.", dying, e);
        }
    }

    /** The end-of-animation drop of an entity that already dropped its experience at death. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void later(LivingExperienceDropEvent event) {
        if (DROPPED.contains(event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    /** @return true if no class between this one and {@code EntityLivingBase} has its own death animation code */
    static boolean vanillaDeath(Class<?> type) {
        Boolean known = VANILLA_DEATH.get(type);
        if (known != null) {
            return known;
        }
        boolean vanilla = true;
        try {
            for (Class<?> c = type; c != null && c != EntityLivingBase.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getParameterTypes().length == 0 && ("onDeathUpdate".equals(m.getName()) || "func_70609_aI".equals(m.getName()))) {
                        vanilla = false;
                    }
                }
            }
        } catch (LinkageError e) {
            vanilla = false; // a class that cannot even list its methods here is left alone
        }
        VANILLA_DEATH.put(type, vanilla);
        return vanilla;
    }
}

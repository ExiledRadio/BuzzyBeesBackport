package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;

/**
 * 1.15.2: a weaker effect is kept, hidden, under a stronger one of the same kind, and comes back when the stronger
 * one runs out. A beacon's Haste II no longer wipes a Haste I potion with eight minutes left; the potion's time
 * runs on underneath and is there again when the beacon's runs out.
 *
 * <p>Called from patch P6 ({@code asm/HiddenEffectsTransformer}). Each method is 1.15.2's {@code MobEffectInstance}
 * code ({@code update}, {@code tick}, {@code tickDownDuration}, the {@code HiddenEffect} tag) over 1.12.2's
 * {@code PotionEffect}. Off, every call answers as 1.12.2 does; hidden effects already saved stay in the save and
 * wait, but are not used.
 */
public final class HiddenEffects {

    private HiddenEffects() {
    }

    public static boolean on() {
        return Tweaks.on() && ModConfig.tweaks.hiddenEffects;
    }

    public static HiddenEffectHolder holder(PotionEffect effect) {
        return (HiddenEffectHolder) (Object) effect;
    }

    @Nullable
    public static PotionEffect hidden(PotionEffect effect) {
        return holder(effect).buzzybeesbackport$hidden();
    }

    // -------------------------------------------------------------------------------------- adding one

    /**
     * {@code EntityLivingBase.addPotionEffect} calls this instead of {@code existing.combine(added)}.
     *
     * <p>On a client, the effect added is the server's whole new state of it (the server combined already and sent
     * the result), so it is taken as it is, as 1.15.2's client does ({@code forceAddEffect}); 1.12.2's combine would
     * keep a stronger effect the server has just let run out. On the server, 1.15.2's {@code update}.
     */
    public static void combine(PotionEffect existing, PotionEffect added, EntityLivingBase entity) {
        if (!on() || !((Object) existing instanceof HiddenEffectHolder)) {
            existing.combine(added);
            return;
        }
        if (entity.world.isRemote) {
            holder(existing).buzzybeesbackport$set(added.getDuration(), added.getAmplifier(), added.getIsAmbient(), added.doesShowParticles());
            return;
        }
        update(existing, added);
    }

    /** 1.15.2's {@code MobEffectInstance.update}: merges {@code other} into {@code self}. */
    public static boolean update(PotionEffect self, PotionEffect other) {
        HiddenEffectHolder h = holder(self);
        int duration = self.getDuration();
        int amplifier = self.getAmplifier();
        boolean ambient = self.getIsAmbient();
        boolean particles = self.doesShowParticles();
        boolean changed = false;
        if (other.getAmplifier() > amplifier) {
            if (other.getDuration() < duration) {
                // The weaker, longer effect waits under the stronger one.
                PotionEffect below = h.buzzybeesbackport$hidden();
                PotionEffect kept = new PotionEffect(self);
                holder(kept).buzzybeesbackport$setHidden(below);
                h.buzzybeesbackport$setHidden(kept);
            }
            amplifier = other.getAmplifier();
            duration = other.getDuration();
            changed = true;
        } else if (other.getDuration() > duration) {
            if (other.getAmplifier() == amplifier) {
                duration = other.getDuration();
                changed = true;
            } else if (h.buzzybeesbackport$hidden() == null) {
                h.buzzybeesbackport$setHidden(new PotionEffect(other));
            } else {
                update(h.buzzybeesbackport$hidden(), other);
            }
        }
        if (!other.getIsAmbient() && ambient || changed) {
            ambient = other.getIsAmbient();
            changed = true;
        }
        if (other.doesShowParticles() != particles) {
            particles = other.doesShowParticles();
            changed = true;
        }
        h.buzzybeesbackport$set(duration, amplifier, ambient, particles);
        return changed;
    }

    // --------------------------------------------------------------------------------------------- ticking

    /**
     * Run first in {@code PotionEffect.onUpdate}: -1 lets 1.12.2's run (no hidden effect, or the setting off), else
     * the answer (1 still going, 0 run out). 1.15.2's {@code tick}: the effect works, every effect in the chain
     * counts down, and when this one reaches 0 the hidden one takes its place, with what time it has left.
     */
    public static int tick(PotionEffect effect, EntityLivingBase entity) {
        HiddenEffectHolder h = holder(effect);
        if (h.buzzybeesbackport$hidden() == null || !on()) {
            return -1;
        }
        if (effect.getDuration() > 0) {
            if (effect.getPotion().isReady(effect.getDuration(), effect.getAmplifier())) {
                effect.performEffect(entity);
            }
            for (PotionEffect e = effect; e != null; e = hidden(e)) {
                holder(e).buzzybeesbackport$setDuration(e.getDuration() - 1);
            }
            PotionEffect next = h.buzzybeesbackport$hidden();
            if (effect.getDuration() == 0 && next != null) {
                int oldAmplifier = effect.getAmplifier();
                h.buzzybeesbackport$set(next.getDuration(), next.getAmplifier(), next.getIsAmbient(), next.doesShowParticles());
                effect.setCurativeItems(next.getCurativeItems());
                h.buzzybeesbackport$setHidden(hidden(next));
                changed(entity, effect, oldAmplifier);
            }
        }
        return effect.getDuration() > 0 ? 1 : 0;
    }

    /**
     * What 1.15.2's {@code onEffectUpdated(effect, true)} does when the hidden effect comes back: the attribute
     * modifiers follow the new amplifier, the particles are recomputed and a player is told.
     */
    private static void changed(EntityLivingBase entity, PotionEffect effect, int oldAmplifier) {
        if (entity.world.isRemote) {
            return;
        }
        effect.getPotion().removeAttributesModifiersFromEntity(entity, entity.getAttributeMap(), oldAmplifier);
        effect.getPotion().applyAttributesModifiersToEntity(entity, entity.getAttributeMap(), effect.getAmplifier());
        try {
            ObfuscationReflectionHelper.setPrivateValue(EntityLivingBase.class, entity, true, "field_70752_e");
        } catch (RuntimeException ignored) {
            // Only the particle color waits for the next change.
        }
        if (entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection != null) {
            ((EntityPlayerMP) entity).connection.sendPacket(new SPacketEntityEffect(entity.getEntityId(), effect));
        }
    }

    // ----------------------------------------------------------------------------------------------- saving

    /**
     * Run as {@code PotionEffect.writeCustomPotionEffectToNBT} returns: the hidden effect, if any, under
     * {@code HiddenEffect}, written the same way (so a chain nests), as 1.15.2 saves it.
     */
    public static NBTTagCompound write(PotionEffect effect, NBTTagCompound nbt) {
        PotionEffect hidden = hidden(effect);
        if (hidden != null) {
            nbt.setTag("HiddenEffect", hidden.writeCustomPotionEffectToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    /** Run as {@code PotionEffect.readCustomPotionEffectFromNBT} returns: reads {@code HiddenEffect} back. */
    @Nullable
    public static PotionEffect read(@Nullable PotionEffect effect, NBTTagCompound nbt) {
        if (effect != null && nbt.hasKey("HiddenEffect", 10)) {
            NBTTagCompound tag = nbt.getCompoundTag("HiddenEffect");
            if (!tag.hasKey("Id", 99)) {
                // 1.15.2 itself writes the Id; a hidden effect is always the same kind as the one it is under.
                tag = tag.copy();
                tag.setByte("Id", nbt.getByte("Id"));
            }
            holder(effect).buzzybeesbackport$setHidden(PotionEffect.readCustomPotionEffectFromNBT(tag));
        }
        return effect;
    }
}

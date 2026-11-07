package com.exiledradio.buzzybeesbackport.honey;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * The honey bottle: 6 hunger, 0.1 saturation modifier (1.2 saturation), drunk in 40 ticks, stacks to 16,
 * cures Poison and leaves a glass bottle - in the grid too, so a honey block's recipe hands the bottles back.
 *
 * <p>The drinking sound is vanilla's generic one for every drink in 1.12.2; {@link HoneyEvents} swaps it
 * for 1.15's honey sound.
 */
public class ItemHoneyBottle extends ItemFood {

    public ItemHoneyBottle() {
        super(6, 0.1F, false);
        setMaxStackSize(16);
        setContainerItem(Items.GLASS_BOTTLE);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        boolean creative = entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isCreativeMode;
        ItemStack kept = creative ? stack.copy() : null;
        ItemStack rest = super.onItemUseFinish(stack, world, entity);
        if (!world.isRemote) {
            entity.removePotionEffect(MobEffects.POISON);
        }
        if (creative) {
            // ItemFood always uses one up; Creative keeps it, as 1.15 does.
            return kept;
        }
        if (rest.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.inventory.addItemStackToInventory(bottle)) {
                player.dropItem(bottle, false);
            }
        }
        return rest;
    }
}

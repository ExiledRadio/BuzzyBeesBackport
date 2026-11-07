package com.exiledradio.buzzybeesbackport.bee;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A nest or hive as an item. Silk Touch and Creative keep its bees and honey level in
 * {@code BlockEntityTag}, which 1.12.2's {@link ItemBlock} hands to the placed tile entity on its own.
 * The tooltip shows both (current Java, 1.21.2), and a nest with bees stacks no further than one.
 */
public class ItemBeehive extends ItemBlock {

    public ItemBeehive(Block block) {
        super(block);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return hasContents(stack) ? 1 : super.getItemStackLimit(stack);
    }

    private static boolean hasContents(ItemStack stack) {
        NBTTagCompound tag = stack.getSubCompound("BlockEntityTag");
        return tag != null && (tag.getTagList("Bees", 10).tagCount() > 0 || tag.getInteger("HoneyLevel") > 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound tag = stack.getSubCompound("BlockEntityTag");
        int bees = tag == null ? 0 : tag.getTagList("Bees", 10).tagCount();
        int honey = tag == null ? 0 : tag.getInteger("HoneyLevel");
        tooltip.add(I18n.format("tooltip.buzzybeesbackport.beehive.bees", bees, TileEntityBeehive.MAX_BEES));
        tooltip.add(I18n.format("tooltip.buzzybeesbackport.beehive.honey", honey, TileEntityBeehive.MAX_HONEY));
    }
}

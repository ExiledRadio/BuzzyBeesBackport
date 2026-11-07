package com.exiledradio.buzzybeesbackport.bee;

import com.exiledradio.buzzybeesbackport.common.Owners;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.15's dispenser uses on hives: shears take 3 honeycombs (dropped), a glass bottle takes a honey bottle.
 * Both only from a full hive in front; anything else falls through to the behavior that was there before,
 * so sheep shearing and other mods' bottle behaviors keep working. Bees leave as from a normal harvest by
 * no one: they are not set on anybody.
 */
final class BeeDispensing {

    private BeeDispensing() {
    }

    static void register() {
        final IBehaviorDispenseItem shears = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.SHEARS);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.SHEARS, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos front = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
                if (!world.isRemote && isFullHive(world, front)) {
                    world.playSound(null, front, BeeContent.HIVE_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    BlockBeehive.dropHoneycomb(world, front);
                    BlockBeehive.takeHoney(world, world.getBlockState(front), front, null, TileEntityBeehive.Release.BEE_RELEASED);
                    world.playEvent(1000, source.getBlockPos(), 0);
                    if (stack.attemptDamageItem(1, world.rand, null)) {
                        stack.setCount(0);
                    }
                    return stack;
                }
                return shears.dispense(source, stack);
            }
        });

        final IBehaviorDispenseItem bottles = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.GLASS_BOTTLE);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.GLASS_BOTTLE, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
                BlockPos front = source.getBlockPos().offset(facing);
                Item honey = Owners.ours("honey_bottle");
                if (!world.isRemote && honey != null && isFullHive(world, front)) {
                    world.playSound(null, front, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    BlockBeehive.takeHoney(world, world.getBlockState(front), front, null, TileEntityBeehive.Release.BEE_RELEASED);
                    world.playEvent(1000, source.getBlockPos(), 0);
                    return fill(source, stack, new ItemStack(honey));
                }
                return bottles.dispense(source, stack);
            }
        });
    }

    static boolean isFullHive(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockBeehive && BlockBeehive.honeyLevel(world, pos) >= TileEntityBeehive.MAX_HONEY;
    }

    /** 1.15's {@code fillBottle}: a single bottle becomes the filled one; otherwise it goes in the dispenser or out. */
    static ItemStack fill(IBlockSource source, ItemStack empty, ItemStack filled) {
        empty.shrink(1);
        if (empty.isEmpty()) {
            return filled.copy();
        }
        TileEntityDispenser dispenser = source.getBlockTileEntity();
        if (dispenser.addItemStack(filled.copy()) < 0) {
            new BehaviorDefaultDispenseItem().dispense(source, filled.copy());
        }
        return empty;
    }
}

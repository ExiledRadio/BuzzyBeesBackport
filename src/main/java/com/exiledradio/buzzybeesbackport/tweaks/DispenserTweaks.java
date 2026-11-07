package com.exiledradio.buzzybeesbackport.tweaks;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.15's dispenser changes. Each wraps the behavior that was there before and falls through to it, so
 * anything it does not handle (and other mods' behaviors registered earlier) still works.
 * <ul>
 *   <li>a glass bottle facing water fills with water (after the bee stream's hive check);</li>
 *   <li>an armor stand is placed in front, facing away from the dispenser;</li>
 *   <li>a fire charge lights an unlit campfire in front (Village &amp; Pillage's, by name and {@code lit}).</li>
 * </ul>
 */
final class DispenserTweaks {

    private static final ResourceLocation CAMPFIRE = new ResourceLocation("villagerbackport", "campfire");
    /** This mod's identical copy, registered when Village &amp; Pillage is absent. */
    private static final ResourceLocation OUR_CAMPFIRE = new ResourceLocation("buzzybeesbackport", "campfire");

    private DispenserTweaks() {
    }

    static BlockPos front(IBlockSource source) {
        return source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
    }

    static void register() {
        final IBehaviorDispenseItem bottles = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.GLASS_BOTTLE);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.GLASS_BOTTLE, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos front = front(source);
                IBlockState state = world.getBlockState(front);
                if (!world.isRemote && state.getMaterial() == Material.WATER && !hive(world, front)) {
                    world.playSound(null, front, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.playEvent(1000, source.getBlockPos(), 0);
                    return fill(source, stack, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER));
                }
                return bottles.dispense(source, stack);
            }
        });

        final IBehaviorDispenseItem stands = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.ARMOR_STAND);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.ARMOR_STAND, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
                BlockPos front = front(source);
                if (world.isRemote) {
                    return stack;
                }
                EntityArmorStand stand = new EntityArmorStand(world, front.getX() + 0.5D, front.getY(), front.getZ() + 0.5D);
                AxisAlignedBB box = stand.getEntityBoundingBox();
                if (!world.getCollisionBoxes(null, box).isEmpty() || !world.getEntitiesWithinAABBExcludingEntity(null, box).isEmpty()) {
                    return stands.dispense(source, stack);
                }
                float yaw = facing.getAxis().isHorizontal() ? facing.getHorizontalAngle() : 0.0F;
                stand.setLocationAndAngles(front.getX() + 0.5D, front.getY(), front.getZ() + 0.5D, yaw, 0.0F);
                world.spawnEntity(stand);
                world.playSound(null, stand.posX, stand.posY, stand.posZ, SoundEvents.ENTITY_ARMORSTAND_PLACE, SoundCategory.BLOCKS, 0.75F, 0.8F);
                world.playEvent(1000, source.getBlockPos(), 0);
                stack.shrink(1);
                return stack;
            }
        });

        final IBehaviorDispenseItem charges = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.FIRE_CHARGE);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.FIRE_CHARGE, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos front = front(source);
                IBlockState state = world.getBlockState(front);
                IProperty<Boolean> lit = litProperty(state);
                if (!world.isRemote && lit != null && !state.getValue(lit)) {
                    world.setBlockState(front, state.withProperty(lit, Boolean.TRUE), 3);
                    world.playSound(null, front, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F,
                            (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F);
                    stack.shrink(1);
                    return stack;
                }
                return charges.dispense(source, stack);
            }
        });
    }

    private static boolean hive(World world, BlockPos pos) {
        ResourceLocation id = world.getBlockState(pos).getBlock().getRegistryName();
        return id != null && ("buzzybeesbackport:beehive".equals(id.toString()) || "buzzybeesbackport:bee_nest".equals(id.toString()));
    }

    /** @return a campfire's {@code lit} property, or null for anything else */
    @SuppressWarnings("unchecked")
    static IProperty<Boolean> litProperty(IBlockState state) {
        if (!CAMPFIRE.equals(state.getBlock().getRegistryName()) && !OUR_CAMPFIRE.equals(state.getBlock().getRegistryName())) {
            return null;
        }
        for (IProperty<?> p : state.getPropertyKeys()) {
            if ("lit".equals(p.getName()) && p.getValueClass() == Boolean.class) {
                return (IProperty<Boolean>) p;
            }
        }
        return null;
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

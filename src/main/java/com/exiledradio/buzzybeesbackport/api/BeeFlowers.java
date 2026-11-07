package com.exiledradio.buzzybeesbackport.api;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockStem;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * What bees count as flowers and what they grow: 1.15's {@code flowers}, {@code tall_flowers} and
 * {@code bee_growables} tags, which 1.12.2 lacks.
 *
 * <p>Other mods - later backports of the family with their own flowers and crops - add to it:
 * {@code BeeFlowers.addFlower(state -> ...)}, {@code BeeFlowers.addFlowerItem(stack -> ...)} and
 * {@code BeeFlowers.addGrowable(...)}, from pre-init or init. Reach it by reflection or a soft dependency;
 * it never changes shape.
 */
public final class BeeFlowers {

    private static final List<Predicate<IBlockState>> FLOWERS = new ArrayList<Predicate<IBlockState>>();
    private static final List<Predicate<ItemStack>> FLOWER_ITEMS = new ArrayList<Predicate<ItemStack>>();
    private static final List<Predicate<IBlockState>> GROWABLES = new ArrayList<Predicate<IBlockState>>();

    /** Village & Pillage's 1.14 flowers, by name: the mod is optional. */
    private static final String[] FAMILY_FLOWERS = {
            "villagerbackport:cornflower", "villagerbackport:lily_of_the_valley", "villagerbackport:wither_rose"};
    /** Village & Pillage's sweet berry bush, grown by bees as 1.15 does. */
    private static final ResourceLocation SWEET_BERRY_BUSH = new ResourceLocation("villagerbackport", "sweet_berry_bush");

    private BeeFlowers() {
    }

    public static void addFlower(Predicate<IBlockState> flower) {
        FLOWERS.add(flower);
    }

    public static void addFlowerItem(Predicate<ItemStack> item) {
        FLOWER_ITEMS.add(item);
    }

    public static void addGrowable(Predicate<IBlockState> growable) {
        GROWABLES.add(growable);
    }

    /** @return true if a bee pollinates this state: small flowers, and tall flowers except a sunflower's stem */
    public static boolean isFlower(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockFlower) {
            return true;
        }
        if (block instanceof BlockDoublePlant) {
            BlockDoublePlant.EnumPlantType type = state.getValue(BlockDoublePlant.VARIANT);
            if (type == BlockDoublePlant.EnumPlantType.SUNFLOWER) {
                return state.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.UPPER;
            }
            return type == BlockDoublePlant.EnumPlantType.SYRINGA || type == BlockDoublePlant.EnumPlantType.ROSE
                    || type == BlockDoublePlant.EnumPlantType.PAEONIA;
        }
        ResourceLocation id = block.getRegistryName();
        if (id != null) {
            String name = id.toString();
            for (String family : FAMILY_FLOWERS) {
                if (family.equals(name)) {
                    return true;
                }
            }
        }
        for (Predicate<IBlockState> extra : FLOWERS) {
            if (extra.test(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if this item breeds and tempts bees: any flower item (1.15's {@code flowers} item tag),
     * wither rose included
     */
    public static boolean isFlowerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof ItemBlock) {
            Block block = ((ItemBlock) item).getBlock();
            if (block instanceof BlockFlower) {
                return true;
            }
            if (block instanceof BlockDoublePlant) {
                int meta = stack.getMetadata();
                return meta == BlockDoublePlant.EnumPlantType.SUNFLOWER.getMeta()
                        || meta == BlockDoublePlant.EnumPlantType.SYRINGA.getMeta()
                        || meta == BlockDoublePlant.EnumPlantType.ROSE.getMeta()
                        || meta == BlockDoublePlant.EnumPlantType.PAEONIA.getMeta();
            }
            ResourceLocation id = block.getRegistryName();
            if (id != null) {
                for (String family : FAMILY_FLOWERS) {
                    if (family.equals(id.toString())) {
                        return true;
                    }
                }
            }
        }
        for (Predicate<ItemStack> extra : FLOWER_ITEMS) {
            if (extra.test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the grown-by-one state if a pollinating bee grows this one (1.15's {@code bee_growables}:
     * crops, melon and pumpkin stems, sweet berry bushes), else null
     */
    @Nullable
    public static IBlockState grow(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            BlockCrops crops = (BlockCrops) block;
            if (crops.isMaxAge(state)) {
                return null;
            }
            PropertyInteger age = age(state);
            return age == null ? null : state.withProperty(age, state.getValue(age) + 1);
        }
        if (block instanceof BlockStem) {
            int age = state.getValue(BlockStem.AGE);
            return age < 7 ? state.withProperty(BlockStem.AGE, age + 1) : null;
        }
        if (SWEET_BERRY_BUSH.equals(block.getRegistryName())) {
            PropertyInteger age = age(state);
            return age != null && state.getValue(age) < 3 ? state.withProperty(age, state.getValue(age) + 1) : null;
        }
        for (Predicate<IBlockState> extra : GROWABLES) {
            if (extra.test(state)) {
                PropertyInteger age = age(state);
                if (age != null && age.getAllowedValues().contains(state.getValue(age) + 1)) {
                    return state.withProperty(age, state.getValue(age) + 1);
                }
            }
        }
        return null;
    }

    @Nullable
    private static PropertyInteger age(IBlockState state) {
        for (IProperty<?> p : state.getPropertyKeys()) {
            if ("age".equals(p.getName()) && p instanceof PropertyInteger) {
                return (PropertyInteger) p;
            }
        }
        return null;
    }
}

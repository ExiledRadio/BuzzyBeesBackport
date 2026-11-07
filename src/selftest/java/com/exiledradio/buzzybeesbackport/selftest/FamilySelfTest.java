package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.api.BeeFlowers;
import com.exiledradio.buzzybeesbackport.api.BeeSmoke;
import com.exiledradio.buzzybeesbackport.tweaks.BellRedstone;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Rule 2 integrations with Village &amp; Pillage Backport, run only when it is installed
 * ({@code -Pselftest=family} with its jar in {@code eclipse/mods}).
 */
public final class FamilySelfTest implements SelfTest {

    @Override
    public String name() {
        return "family";
    }

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("villagerbackport", name));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState with(IBlockState state, String property, Comparable value) {
        for (IProperty<?> p : state.getPropertyKeys()) {
            if (p.getName().equals(property)) {
                return state.withProperty((IProperty) p, value);
            }
        }
        return state;
    }

    @Override
    public void run(Check c) {
        if (!Loader.isModLoaded("villagerbackport")) {
            c.ok(true, "Village & Pillage not installed: family checks skipped");
            return;
        }
        Block campfire = block("campfire");
        c.ok(campfire != null && "villagerbackport".equals(campfire.getRegistryName().getNamespace()), "V&P's campfire is the one in use");
        c.ok(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("buzzybeesbackport", "campfire")) == null
                || !"buzzybeesbackport".equals(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("buzzybeesbackport", "campfire")).getRegistryName().getNamespace()),
                "our campfire copy is not registered beside it");

        // Bees calmed by V&P's campfire.
        BlockPos base = c.area();
        BlockPos hive = base.add(5, 6, 5);
        c.world.setBlockState(hive.down(4), with(campfire.getDefaultState(), "lit", Boolean.TRUE), 2);
        c.ok(BeeSmoke.isSmokey(c.world, hive), "a lit V&P campfire 4 below calms the hive");
        c.world.setBlockState(hive.down(4), with(campfire.getDefaultState(), "lit", Boolean.FALSE), 2);
        c.ok(!BeeSmoke.isSmokey(c.world, hive), "an unlit one does not");

        // A dispenser's fire charge lights it.
        BlockPos disp = hive.down(4).west();
        c.world.setBlockState(disp, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST), 2);
        ItemStack charge = new ItemStack(Items.FIRE_CHARGE, 2);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.FIRE_CHARGE).dispense(new BlockSourceImpl(c.world, disp), charge);
        IBlockState lit = c.world.getBlockState(hive.down(4));
        boolean isLit = false;
        for (IProperty<?> p : lit.getPropertyKeys()) {
            if (p.getName().equals("lit")) {
                isLit = Boolean.TRUE.equals(lit.getValue(p));
            }
        }
        c.ok(isLit && charge.getCount() == 1, "a dispenser's fire charge lights V&P's campfire");

        // Flowers and berries.
        c.ok(BeeFlowers.isFlower(block("cornflower").getDefaultState()), "a cornflower is a flower to bees");
        c.ok(BeeFlowers.isFlower(block("wither_rose").getDefaultState()), "so is a wither rose");
        Block bush = block("sweet_berry_bush");
        IBlockState young = with(bush.getDefaultState(), "age", 1);
        IBlockState grown = BeeFlowers.grow(young);
        c.ok(grown != null && grown.getBlock() == bush, "bees grow V&P's sweet berry bush a stage");

        // The bell rings on the rising edge of redstone power.
        BlockPos bell = base.add(12, 0, 12);
        c.world.setBlockState(bell, block("bell").getDefaultState(), 3);
        int before = BellRedstone.rings();
        c.world.setBlockState(bell.east(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.eq(before + 1, BellRedstone.rings(), "powering the bell rings it once");
        c.world.setBlockState(bell.east().up(), Blocks.STONE.getDefaultState(), 3);
        c.eq(before + 1, BellRedstone.rings(), "staying powered does not ring it again");
        c.world.setBlockToAir(bell.east());
        c.world.setBlockState(bell.east(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.eq(before + 2, BellRedstone.rings(), "powering it again rings it again");
    }
}

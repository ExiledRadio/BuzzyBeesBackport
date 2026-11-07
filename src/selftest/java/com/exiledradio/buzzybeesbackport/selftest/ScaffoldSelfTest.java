package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;

/** The shared code every stream builds on. */
public final class ScaffoldSelfTest implements SelfTest {

    @Override
    public String name() {
        return "scaffold";
    }

    @Override
    public void run(Check check) {
        BlockPos area = check.area();
        check.ok(check.world.isAirBlock(area.up()), "test area is clear");

        check.ok(Loader.isModLoaded("backportcore"), "Backport Core is loaded");
        for (String block : new String[] {"bee_nest", "beehive", "honey_block", "honeycomb_block"}) {
            check.eq(BuzzyBeesBackport.MOD_ID, BlockOwnership.owner(block), "this mod owns " + block);
        }
        check.ok(BlockOwnership.shouldRegister(BuzzyBeesBackport.MOD_ID, "honey_block"), "this mod registers its own honey block");
    }
}

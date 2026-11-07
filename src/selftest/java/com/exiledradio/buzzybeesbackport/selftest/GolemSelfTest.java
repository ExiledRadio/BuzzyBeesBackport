package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.golem.GolemRepair;
import com.exiledradio.buzzybeesbackport.golem.GolemRepair.Cracks;

import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;

/** Stream I: iron golem cracks and repair. */
public final class GolemSelfTest implements SelfTest {

    @Override
    public String name() {
        return "golems";
    }

    @Override
    public void run(Check c) {
        c.eq(Cracks.NONE, Cracks.of(1.0F), "full health: no cracks");
        c.eq(Cracks.NONE, Cracks.of(0.75F), "75%: none yet");
        c.eq(Cracks.LOW, Cracks.of(0.74F), "below 75%: low");
        c.eq(Cracks.MEDIUM, Cracks.of(0.49F), "below 50%: medium");
        c.eq(Cracks.HIGH, Cracks.of(0.24F), "below 25%: high");

        BlockPos at = c.area().up();
        EntityIronGolem golem = new EntityIronGolem(c.world);
        golem.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        c.world.spawnEntity(golem);
        EntityPlayerMP player = Players.real(c.world, "golemfixer");
        c.world.spawnEntity(player);
        player.setPosition(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
        player.setGameType(GameType.SURVIVAL);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT, 10));

        c.eq(EnumActionResult.PASS, player.interactOn(golem, EnumHand.MAIN_HAND), "a golem at full health refuses an ingot");
        c.eq(10, player.getHeldItemMainhand().getCount(), "and the ingot is kept");
        golem.setHealth(1.0F);
        c.eq(Cracks.HIGH, Cracks.of(golem), "a golem at 1 health is badly cracked");
        for (int i = 0; i < 4; i++) {
            c.eq(EnumActionResult.SUCCESS, player.interactOn(golem, EnumHand.MAIN_HAND), "ingot " + (i + 1) + " repairs");
        }
        c.eq(golem.getMaxHealth(), golem.getHealth(), "four ingots bring a golem from 1 to full");
        c.eq(6, player.getHeldItemMainhand().getCount(), "four ingots used");
        c.eq(Cracks.NONE, Cracks.of(golem), "and the cracks are gone");
        c.eq(EnumActionResult.PASS, player.interactOn(golem, EnumHand.MAIN_HAND), "a fifth is refused");

        golem.setHealth(50.0F);
        player.setGameType(GameType.CREATIVE);
        player.interactOn(golem, EnumHand.MAIN_HAND);
        c.eq(75.0F, golem.getHealth(), "one ingot heals 25");
        c.eq(6, player.getHeldItemMainhand().getCount(), "Creative keeps the ingot");
        player.setGameType(GameType.SURVIVAL);

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT));
        c.eq(EnumActionResult.PASS, player.interactOn(golem, EnumHand.MAIN_HAND), "gold does nothing");
        c.eq(75.0F, golem.getHealth(), "health unchanged");

        // Hits: health goes down through the stages (the crack sound plays at each new stage).
        golem.setHealth(golem.getMaxHealth());
        golem.attackEntityFrom(net.minecraft.util.DamageSource.GENERIC, 30.0F);
        c.eq(Cracks.LOW, Cracks.of(golem), "30 damage: low cracks");
        c.ok(GolemRepair.INGOT_HEAL == 25.0F, "ingot heal 25");
        golem.setDead();
        c.world.removeEntity(player);
    }
}

package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.asm.HiddenEffectsTransformer;
import com.exiledradio.buzzybeesbackport.asm.WorldTweaksTransformer;
import com.exiledradio.buzzybeesbackport.tweaks.HiddenEffects;
import com.exiledradio.buzzybeesbackport.tweaks.WorldTweaks;

import net.minecraft.block.BlockPortal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * 1.15's behavior changes to existing things (docs/PATCHES.md P6 to P9): hidden effects, portals above y 128, mobs
 * and lava, farmland under a moving block. The Hardcore death screen is client only and has no headless test.
 */
public final class MechanicsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mechanics";
    }

    @Override
    public void run(Check c) throws Exception {
        // Loading a class runs its transformer.
        String[] loaded = {PotionEffect.class.getName(), net.minecraft.entity.EntityLivingBase.class.getName(), Teleporter.class.getName(),
                WalkNodeProcessor.class.getName(), net.minecraft.block.BlockFarmland.class.getName()};
        c.eq(5, loaded.length, "patched classes loaded");
        c.ok(HiddenEffectsTransformer.applied, "patch P6 applied to PotionEffect");
        c.ok(HiddenEffectsTransformer.appliedAdd, "patch P6 applied to EntityLivingBase.addPotionEffect");
        c.ok(WorldTweaksTransformer.appliedP7, "patch P7 applied to Teleporter");
        c.ok(WorldTweaksTransformer.appliedP8, "patch P8 applied to WalkNodeProcessor");
        c.ok(WorldTweaksTransformer.appliedP9, "patch P9 applied to BlockFarmland");
        hidden(c);
        portals(c);
        lava(c);
        farmland(c);
    }

    // -------------------------------------------------------------------------------------- hidden effects

    private static PotionEffect haste(EntityZombie zombie) {
        return zombie.getActivePotionEffect(MobEffects.HASTE);
    }

    private void hidden(Check c) {
        BlockPos base = c.area();
        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setPosition(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);

        // A Haste I potion, then a beacon's Haste II (ambient, shorter).
        zombie.addPotionEffect(new PotionEffect(MobEffects.HASTE, 9600, 0));
        zombie.addPotionEffect(new PotionEffect(MobEffects.HASTE, 260, 1, true, true));
        PotionEffect top = haste(zombie);
        c.eq(1, top.getAmplifier(), "the beacon's Haste II is on top");
        c.eq(260, top.getDuration(), "the beacon's duration");
        PotionEffect below = HiddenEffects.hidden(top);
        c.ok(below != null && below.getAmplifier() == 0 && below.getDuration() == 9600 && !below.getIsAmbient(),
                "the potion's Haste I waits under it: " + below);

        // The beacon renews (same amplifier, longer): only its time changes.
        zombie.addPotionEffect(new PotionEffect(MobEffects.HASTE, 280, 1, true, true));
        c.eq(280, haste(zombie).getDuration(), "a renewed beacon effect");
        c.ok(HiddenEffects.hidden(haste(zombie)) == below, "the hidden potion is kept through a renewal");

        // Saved and read back as 1.15.2 saves it.
        NBTTagCompound tag = top.writeCustomPotionEffectToNBT(new NBTTagCompound());
        c.ok(tag.hasKey("HiddenEffect", 10) && tag.getCompoundTag("HiddenEffect").getInteger("Duration") == 9600
                && tag.getCompoundTag("HiddenEffect").getByte("Amplifier") == 0, "HiddenEffect saved: " + tag);
        PotionEffect read = PotionEffect.readCustomPotionEffectFromNBT(tag);
        PotionEffect readBelow = read == null ? null : HiddenEffects.hidden(read);
        c.ok(readBelow != null && readBelow.getDuration() == 9600 && readBelow.getAmplifier() == 0, "HiddenEffect read back: " + readBelow);
        NBTTagCompound saved = new NBTTagCompound();
        zombie.writeToNBT(saved);
        EntityZombie loaded = new EntityZombie(c.world);
        loaded.readFromNBT(saved);
        c.ok(haste(loaded) != null && HiddenEffects.hidden(haste(loaded)) != null, "a mob's hidden effect survives saving");

        // The beacon runs out: Haste I comes back with the time it has left.
        for (int i = 0; i < 280; i++) {
            if (!haste(zombie).onUpdate(zombie)) {
                break;
            }
        }
        PotionEffect back = haste(zombie);
        c.eq(0, back.getAmplifier(), "after the beacon, Haste I is back");
        c.eq(9600 - 280, back.getDuration(), "the potion's time ran on underneath");
        c.ok(!back.getIsAmbient() && HiddenEffects.hidden(back) == null, "the potion is itself again, nothing under it");

        // A weaker, longer effect added under a stronger one waits too.
        zombie.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 1));
        zombie.addPotionEffect(new PotionEffect(MobEffects.SPEED, 2000, 0));
        PotionEffect speed = zombie.getActivePotionEffect(MobEffects.SPEED);
        c.ok(speed.getAmplifier() == 1 && speed.getDuration() == 100, "Speed II stays on top of a longer Speed I");
        c.ok(HiddenEffects.hidden(speed) != null && HiddenEffects.hidden(speed).getDuration() == 2000, "the longer Speed I waits under it");
        IAttributeInstance movement = zombie.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier before = movement.getModifier(java.util.UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635"));
        c.ok(before != null && Math.abs(before.getAmount() - 0.4) < 1e-6, "Speed II's modifier: " + before);
        for (int i = 0; i < 100; i++) {
            speed.onUpdate(zombie);
        }
        AttributeModifier after = movement.getModifier(java.util.UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635"));
        c.ok(speed.getAmplifier() == 0 && speed.getDuration() == 1900, "Speed I came back: " + speed);
        c.ok(after != null && Math.abs(after.getAmount() - 0.2) < 1e-6, "the speed modifier follows Speed I: " + after);

        // Off: 1.12.2's combine, the potion is gone.
        ModConfig.tweaks.hiddenEffects = false;
        try {
            EntityZombie plain = new EntityZombie(c.world);
            plain.addPotionEffect(new PotionEffect(MobEffects.HASTE, 9600, 0));
            plain.addPotionEffect(new PotionEffect(MobEffects.HASTE, 260, 1, true, true));
            c.ok(HiddenEffects.hidden(haste(plain)) == null && haste(plain).getAmplifier() == 1, "setting off: the beacon replaces the potion");
        } finally {
            ModConfig.tweaks.hiddenEffects = true;
        }
    }

    // --------------------------------------------------------------------------------------------- portals

    private void portals(Check c) {
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        c.eq(128, nether.getActualHeight(), "the Nether's actual height");
        c.eq(256, WorldTweaks.portalSearchTop(nether), "the portal search starts at the top of the Nether");
        ModConfig.tweaks.portalLinkHeight = false;
        try {
            c.eq(128, WorldTweaks.portalSearchTop(nether), "setting off: the search starts at 128");
        } finally {
            ModConfig.tweaks.portalLinkHeight = true;
        }
        // A portal built above the Nether's roof, at y 150; an entity arrives 40 blocks away at y 64.
        BlockPos frame = new BlockPos(6000, 150, 6000);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 5; y++) {
                boolean edge = x == 0 || x == 3 || y == 0 || y == 4;
                nether.setBlockState(frame.add(x, y, 0), edge ? Blocks.OBSIDIAN.getDefaultState()
                        : Blocks.PORTAL.getDefaultState().withProperty(BlockPortal.AXIS, EnumFacing.Axis.X), 2 | 16);
            }
        }
        EntityPig pig = new EntityPig(nether);
        pig.setPosition(6040.5, 64, 6000.5);
        ReflectionHelper.setPrivateValue(Entity.class, pig, new Vec3d(0.5, 0.0, 0.0), "lastPortalVec", "field_181017_ao");
        ReflectionHelper.setPrivateValue(Entity.class, pig, EnumFacing.NORTH, "teleportDirection", "field_181018_ap");
        boolean found = nether.getDefaultTeleporter().placeInExistingPortal(pig, 0.0F);
        c.ok(found, "an entity finds the portal above y 128");
        c.ok(found && pig.posY > 140 && Math.abs(pig.posX - 6001.5) < 2, "it arrives at that portal: " + pig.getPositionVector());
    }

    // ---------------------------------------------------------------------------------------------- lava

    private void lava(Check c) {
        BlockPos base = c.area();
        BlockPos p = base.add(8, 0, 8);
        // Lava below the next block over, one down (1.12.2 looks only at the same height).
        c.world.setBlockState(p.east().down(), Blocks.LAVA.getDefaultState(), 2 | 16);
        WalkNodeProcessor walk = new WalkNodeProcessor();
        c.eq(PathNodeType.DANGER_FIRE, walk.getPathNodeType(c.world, p.getX(), p.getY(), p.getZ()), "a node beside lava one down");
        // Lava beside, at the same height.
        BlockPos q = base.add(16, 0, 16);
        c.world.setBlockState(q.east().east(), Blocks.STONE.getDefaultState(), 2 | 16);
        c.world.setBlockState(q.add(1, 0, 1), Blocks.LAVA.getDefaultState(), 2 | 16);
        c.eq(PathNodeType.DANGER_FIRE, walk.getPathNodeType(c.world, q.getX(), q.getY(), q.getZ()), "a node diagonally beside lava");
        c.eq(PathNodeType.WALKABLE, walk.getPathNodeType(c.world, p.getX() - 4, p.getY(), p.getZ()), "a node away from lava");
        ModConfig.tweaks.lavaPathing = false;
        try {
            c.eq(PathNodeType.WALKABLE, walk.getPathNodeType(c.world, p.getX(), p.getY(), p.getZ()), "setting off: lava one down is not seen");
            c.eq(PathNodeType.WALKABLE, walk.getPathNodeType(c.world, q.getX(), q.getY(), q.getZ()), "setting off: lava beside is not a danger");
        } finally {
            ModConfig.tweaks.lavaPathing = true;
        }
    }

    // ------------------------------------------------------------------------------------------ farmland

    private void farmland(Check c) {
        BlockPos base = c.area();
        BlockPos soil = base.add(4, -1, 4);
        c.world.setBlockState(soil, Blocks.FARMLAND.getDefaultState(), 2);
        c.world.setBlockState(soil.up(), Blocks.PISTON_EXTENSION.getDefaultState(), 3);
        c.eq(Blocks.FARMLAND, c.world.getBlockState(soil).getBlock(), "farmland under a block a piston is moving");
        c.world.setBlockState(soil.up(), Blocks.STONE.getDefaultState(), 3);
        c.eq(Blocks.DIRT, c.world.getBlockState(soil).getBlock(), "farmland under stone turns to dirt");

        BlockPos other = base.add(8, -1, 4);
        c.world.setBlockState(other, Blocks.FARMLAND.getDefaultState(), 2);
        ModConfig.tweaks.farmlandPistons = false;
        try {
            c.world.setBlockState(other.up(), Blocks.PISTON_EXTENSION.getDefaultState(), 3);
            c.eq(Blocks.DIRT, c.world.getBlockState(other).getBlock(), "setting off: a moving block turns farmland to dirt");
        } finally {
            ModConfig.tweaks.farmlandPistons = true;
            c.world.setBlockToAir(soil.up());
            c.world.setBlockToAir(other.up());
        }

        // A real push: a piston moves a block over farmland; the farmland is farmland while the block moves.
        BlockPos row = base.add(12, -1, 10);
        c.world.setBlockState(row.east(), Blocks.FARMLAND.getDefaultState(), 2);
        c.world.setBlockState(row.up(), Blocks.HAY_BLOCK.getDefaultState(), 2);
        c.world.setBlockState(row.up().west(), Blocks.PISTON.getDefaultState().withProperty(net.minecraft.block.BlockPistonBase.FACING, EnumFacing.EAST), 2);
        // Powered without a neighbor update, then extended at once (a powered piston's queued block event).
        c.world.setBlockState(row.up().west().west(), Blocks.REDSTONE_BLOCK.getDefaultState(), 2);
        BlockPos piston = row.up().west();
        c.ok(Blocks.PISTON.eventReceived(c.world.getBlockState(piston), c.world, piston, 0, EnumFacing.EAST.getIndex()), "the piston extends");
        c.eq(Blocks.PISTON_EXTENSION, c.world.getBlockState(row.east().up()).getBlock(), "the hay is moving over the farmland");
        c.eq(Blocks.FARMLAND, c.world.getBlockState(row.east()).getBlock(), "the farmland stays while it moves over");
    }
}

package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.tweaks.PlayerTweaks;

import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockCake;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.Map;
import java.util.Random;

/** Stream X: 1.15's smaller changes. */
public final class TweaksSelfTest implements SelfTest {

    @Override
    public String name() {
        return "tweaks";
    }

    private EntityPlayerMP player(Check c, WorldServer world, String name, BlockPos at) {
        EntityPlayerMP p = Players.real(world, name);
        world.spawnEntity(p);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setGameType(GameType.SURVIVAL);
        ReflectionHelper.setPrivateValue(EntityPlayerMP.class, p, 0, "respawnInvulnerabilityTicks", "field_147101_bU");
        return p;
    }

    @Override
    public void run(Check c) {
        blocksAndItems(c);
        placing(c);
        mobs(c);
        players(c);
        dispensers(c);
    }

    private void blocksAndItems(Check c) {
        c.eq(3.0F, Blocks.END_BRICKS.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "end stone bricks hardness 3");
        c.ok(Math.abs(Blocks.END_BRICKS.getExplosionResistance(null) - 9.0F) < 1e-4, "end stone bricks blast resistance 9: " + Blocks.END_BRICKS.getExplosionResistance(null));
        c.eq(1200, TileEntityFurnace.getItemBurnTime(new ItemStack(Items.BOAT)), "a boat burns for 6 items");
        c.eq(1200, TileEntityFurnace.getItemBurnTime(new ItemStack(Items.BIRCH_BOAT)), "any wooden boat");
        // Large ferns: about 1 in 8 drops wheat seeds.
        IBlockState fern = Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.FERN)
                .withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.LOWER);
        int seeds = 0;
        for (int i = 0; i < 800; i++) {
            NonNullList<ItemStack> drops = NonNullList.create();
            BlockEvent.HarvestDropsEvent event = new BlockEvent.HarvestDropsEvent(c.world, BlockPos.ORIGIN, fern, 0, 1.0F, drops, null, false);
            MinecraftForge.EVENT_BUS.post(event);
            for (ItemStack s : event.getDrops()) {
                if (s.getItem() == Items.WHEAT_SEEDS) {
                    seeds++;
                }
            }
        }
        c.ok(seeds > 60 && seeds < 150, "large ferns drop wheat seeds about 1 in 8: " + seeds + "/800");
        // Attached stems already drop seeds in 1.12.2 (same block as the growing stem).
        NonNullList<ItemStack> stem = NonNullList.create();
        Blocks.MELON_STEM.getDrops(stem, c.world, BlockPos.ORIGIN, Blocks.MELON_STEM.getDefaultState().withProperty(net.minecraft.block.BlockStem.AGE, 7), 0);
        c.ok(!stem.isEmpty(), "a grown melon stem drops seeds (vanilla 1.12.2 already)");
    }

    private void placing(Check c) {
        // Wet sponge in the Nether.
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        BlockPos np = new BlockPos(0, 120, 0);
        nether.getChunk(np);
        for (BlockPos p : BlockPos.getAllInBox(np.add(-2, -1, -2), np.add(2, 3, 2))) {
            nether.setBlockState(p, p.getY() == np.getY() - 1 ? Blocks.NETHERRACK.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
        }
        EntityPlayerMP inNether = player(c, nether, "spongenether", np.east(2));
        ItemStack wet = new ItemStack(Blocks.SPONGE, 1, 1);
        inNether.setHeldItem(EnumHand.MAIN_HAND, wet);
        wet.onItemUse(inNether, nether, np.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        IBlockState sponge = nether.getBlockState(np);
        c.ok(sponge.getBlock() == Blocks.SPONGE && !sponge.getValue(BlockSponge.WET), "a wet sponge placed in the Nether dries: " + sponge);
        nether.removeEntity(inNether);
        BlockPos base = c.area();
        EntityPlayerMP placer = player(c, c.world, "spongeworld", base.add(3, 1, 5));
        ItemStack wet2 = new ItemStack(Blocks.SPONGE, 1, 1);
        placer.setHeldItem(EnumHand.MAIN_HAND, wet2);
        wet2.onItemUse(placer, c.world, base.add(5, -1, 5), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(c.world.getBlockState(base.add(5, 0, 5)).getValue(BlockSponge.WET), "in the Overworld it stays wet");

        // Rails along the player's facing.
        placer.rotationYaw = -90.0F; // east
        ItemStack rails = new ItemStack(Blocks.RAIL, 4);
        placer.setHeldItem(EnumHand.MAIN_HAND, rails);
        rails.onItemUse(placer, c.world, base.add(10, -1, 10), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        com.exiledradio.buzzybeesbackport.tweaks.Tweaks.facePendingRails(c.world);
        c.eq(BlockRailBase.EnumRailDirection.EAST_WEST, c.world.getBlockState(base.add(10, 0, 10)).getValue(net.minecraft.block.BlockRail.SHAPE),
                "a lone rail placed facing east runs east-west");
        placer.rotationYaw = 180.0F; // north
        rails.onItemUse(placer, c.world, base.add(14, -1, 10), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        com.exiledradio.buzzybeesbackport.tweaks.Tweaks.facePendingRails(c.world);
        c.eq(BlockRailBase.EnumRailDirection.NORTH_SOUTH, c.world.getBlockState(base.add(14, 0, 10)).getValue(net.minecraft.block.BlockRail.SHAPE),
                "facing north it runs north-south");
        placer.rotationYaw = -90.0F;
        c.world.setBlockState(base.add(18, 0, 9), Blocks.RAIL.getDefaultState(), 3);
        rails.onItemUse(placer, c.world, base.add(18, -1, 10), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        com.exiledradio.buzzybeesbackport.tweaks.Tweaks.facePendingRails(c.world);
        c.eq(BlockRailBase.EnumRailDirection.NORTH_SOUTH, c.world.getBlockState(base.add(18, 0, 10)).getValue(net.minecraft.block.BlockRail.SHAPE),
                "a rail next to another still connects to it");
        c.world.removeEntity(placer);
    }

    @SuppressWarnings("unchecked")
    private void mobs(Check c) {
        Map<Class<? extends Entity>, ?> sounds = (Map<Class<? extends Entity>, ?>) ReflectionHelper.getPrivateValue(EntityParrot.class, null, "MIMIC_SOUNDS");
        c.ok(!sounds.containsKey(EntityEnderman.class), "parrots no longer imitate endermen");
        c.ok(sounds.containsKey(EntityZombie.class), "but still zombies");

        BlockPos base = c.area();
        EntityPlayerMP player = player(c, c.world, "eggs", base.add(4, 1, 4));
        EntityZombie adult = new EntityZombie(c.world);
        adult.setPosition(base.getX() + 6.5, base.getY() + 1, base.getZ() + 4.5);
        c.world.spawnEntity(adult);
        ItemStack egg = new ItemStack(Items.SPAWN_EGG, 2);
        ItemMonsterPlacer.applyEntityIdToItemStack(egg, EntityList.getKey(EntityZombie.class));
        player.setHeldItem(EnumHand.MAIN_HAND, egg);
        c.eq(EnumActionResult.SUCCESS, player.interactOn(adult, EnumHand.MAIN_HAND), "a zombie egg on a zombie is used");
        int babies = 0;
        for (EntityZombie z : c.world.getEntitiesWithinAABB(EntityZombie.class, adult.getEntityBoundingBox().grow(2))) {
            if (z.isChild() && !z.isDead) {
                babies++;
            }
        }
        c.eq(1, babies, "and makes a baby zombie");
        c.eq(1, player.getHeldItemMainhand().getCount(), "using one egg");
        ItemStack husk = new ItemStack(Items.SPAWN_EGG);
        ItemMonsterPlacer.applyEntityIdToItemStack(husk, EntityList.getKey(net.minecraft.entity.monster.EntityHusk.class));
        player.setHeldItem(EnumHand.MAIN_HAND, husk);
        player.interactOn(adult, EnumHand.MAIN_HAND);
        c.eq(1, husk.getCount(), "a husk egg on a zombie does nothing");
        c.world.removeEntity(player);

        // Babies among animals spawned with a chunk.
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(null, c.world, new Random(), 0, 0, false));
        int young = 0;
        for (int i = 0; i < 400; i++) {
            EntityCow cow = new EntityCow(c.world);
            cow.setPosition(base.getX() + 16 + (i % 10), base.getY() + 1, base.getZ() + 16 + (i / 40));
            c.world.spawnEntity(cow);
            if (cow.isChild()) {
                young++;
            }
            cow.setDead();
        }
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(null, c.world, new Random(), 0, 0, false));
        c.ok(young > 5 && young < 45, "about 5% of cows spawned with a chunk are calves: " + young + "/400");
        EntityCow later = new EntityCow(c.world);
        later.setPosition(base.getX() + 16, base.getY() + 1, base.getZ() + 16);
        c.world.spawnEntity(later);
        c.ok(!later.isChild(), "a cow spawned otherwise is not touched");
        later.setDead();
    }

    private void players(Check c) {
        BlockPos base = c.area();
        EntityPlayerMP player = player(c, c.world, "tweakplayer", base.add(4, 1, 4));
        // Eating in Creative.
        player.setGameType(GameType.CREATIVE);
        player.getFoodStats().setFoodLevel(20);
        ItemStack bread = new ItemStack(Items.BREAD);
        player.setHeldItem(EnumHand.MAIN_HAND, bread);
        EnumActionResult r = player.interactionManager.processRightClick(player, c.world, bread, EnumHand.MAIN_HAND);
        c.ok(r == EnumActionResult.SUCCESS && player.isHandActive(), "a full player in Creative can eat bread: " + r);
        player.resetActiveHand();
        BlockPos cake = base.add(6, 1, 4);
        c.world.setBlockState(cake, Blocks.CAKE.getDefaultState(), 3);
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, cake, EnumFacing.UP, new Vec3d(0.5, 1, 0.5)));
        c.eq(1, c.world.getBlockState(cake).getValue(BlockCake.BITES).intValue(), "and cake");
        player.setGameType(GameType.SURVIVAL);

        // Beds by day: too early to sleep, but the respawn point is set.
        c.world.setWorldTime(1000L);
        c.world.calculateInitialSkylight();
        BlockPos foot = base.add(4, 1, 6);
        IBlockState bed = Blocks.BED.getDefaultState().withProperty(BlockBed.FACING, EnumFacing.SOUTH);
        c.world.setBlockState(foot, bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT), 3);
        c.world.setBlockState(foot.south(), bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD), 3);
        EntityPlayer.SleepResult result = player.trySleep(foot.south());
        c.eq(EntityPlayer.SleepResult.NOT_POSSIBLE_NOW, result, "too early to sleep");
        c.eq(foot.south(), player.getBedLocation(player.dimension), "but the bed is now the respawn point");

        // Game rules.
        for (String rule : new String[] {PlayerTweaks.FALL_DAMAGE, PlayerTweaks.FIRE_DAMAGE, PlayerTweaks.DROWNING_DAMAGE,
                PlayerTweaks.IMMEDIATE_RESPAWN, PlayerTweaks.INSOMNIA}) {
            c.ok(c.world.getGameRules().hasRule(rule), "game rule " + rule + " exists");
        }
        c.ok(!c.world.getGameRules().getBoolean(PlayerTweaks.IMMEDIATE_RESPAWN), "doImmediateRespawn is off by default");
        c.ok(player.attackEntityFrom(DamageSource.FALL, 2.0F), "fall damage by default");
        player.hurtResistantTime = 0;
        c.world.getGameRules().setOrCreateGameRule(PlayerTweaks.FALL_DAMAGE, "false");
        c.world.getGameRules().setOrCreateGameRule(PlayerTweaks.FIRE_DAMAGE, "false");
        c.world.getGameRules().setOrCreateGameRule(PlayerTweaks.DROWNING_DAMAGE, "false");
        c.ok(!player.attackEntityFrom(DamageSource.FALL, 2.0F), "no fall damage with fallDamage false");
        c.ok(!player.attackEntityFrom(DamageSource.LAVA, 2.0F), "no lava damage with fireDamage false");
        c.ok(!player.attackEntityFrom(DamageSource.DROWN, 2.0F), "no drowning with drowningDamage false");
        c.ok(player.attackEntityFrom(DamageSource.GENERIC, 1.0F), "other damage still hurts");
        for (String rule : new String[] {PlayerTweaks.FALL_DAMAGE, PlayerTweaks.FIRE_DAMAGE, PlayerTweaks.DROWNING_DAMAGE}) {
            c.world.getGameRules().setOrCreateGameRule(rule, "true");
        }
        c.world.removeEntity(player);
    }

    private void dispensers(Check c) {
        BlockPos base = c.area();
        BlockPos disp = base.add(8, 1, 8);
        c.world.setBlockState(disp, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST), 2);
        BlockSourceImpl source = new BlockSourceImpl(c.world, disp);
        c.world.setBlockState(disp.east(), Blocks.WATER.getDefaultState(), 2);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        ItemStack out = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.GLASS_BOTTLE).dispense(source, bottle);
        c.ok(out.getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(out) == PotionTypes.WATER, "a dispenser fills a bottle from water: " + out);
        c.world.setBlockToAir(disp.east());
        ItemStack stand = new ItemStack(Items.ARMOR_STAND, 2);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.ARMOR_STAND).dispense(source, stand);
        boolean placed = false;
        for (EntityArmorStand s : c.world.getEntitiesWithinAABB(EntityArmorStand.class, new net.minecraft.util.math.AxisAlignedBB(disp.east()).grow(0.5))) {
            placed |= !s.isDead;
            s.setDead();
        }
        c.ok(placed, "a dispenser places an armor stand");
        c.eq(1, stand.getCount(), "using one");
    }
}

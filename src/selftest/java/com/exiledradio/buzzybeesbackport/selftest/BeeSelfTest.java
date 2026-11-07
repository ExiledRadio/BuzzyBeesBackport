package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.api.BeeFlowers;
import com.exiledradio.buzzybeesbackport.api.BeeSmoke;
import com.exiledradio.buzzybeesbackport.bee.BeeContent;
import com.exiledradio.buzzybeesbackport.bee.BlockBeehive;
import com.exiledradio.buzzybeesbackport.bee.EntityBee;
import com.exiledradio.buzzybeesbackport.bee.TileEntityBeehive;
import com.exiledradio.buzzybeesbackport.common.Advancements;
import com.exiledradio.buzzybeesbackport.common.Owners;

import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;

import java.util.List;

/** Stream B: bees, bee nests and beehives. */
public final class BeeSelfTest implements SelfTest {

    @Override
    public String name() {
        return "bees";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(BeeContent.on(), "bee stream registered")) {
            return;
        }
        c.world.getMinecraftServer().setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
        day(c);
        registration(c);
        flowersAndSmoke(c);
        hiveOccupants(c);
        harvesting(c);
        breaking(c);
        dispensers(c);
        combat(c);
        crops(c);
        flyingHome(c);
        pollinating(c);
    }

    private void day(Check c) {
        c.world.setWorldTime(1000L);
        c.world.calculateInitialSkylight();
    }

    private void night(Check c) {
        c.world.setWorldTime(18000L);
        c.world.calculateInitialSkylight();
    }

    private EntityBee bee(Check c, BlockPos at) {
        EntityBee bee = new EntityBee(c.world);
        bee.setPosition(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5);
        c.world.spawnEntity(bee);
        return bee;
    }

    private TileEntityBeehive hive(Check c, BlockPos pos, BlockBeehive block, EnumFacing facing) {
        c.world.setBlockState(pos, block.getDefaultState().withProperty(BlockBeehive.FACING, facing), 3);
        return (TileEntityBeehive) c.world.getTileEntity(pos);
    }

    private void registration(Check c) {
        c.eq("buzzybeesbackport:bee", String.valueOf(EntityList.getKey(EntityBee.class)), "bee entity registered");
        c.ok(EntityList.ENTITY_EGGS.containsKey(EntityList.getKey(EntityBee.class)), "bee spawn egg exists");
        EntityBee bee = new EntityBee(c.world);
        c.eq(10.0D, bee.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "bee health 10");
        c.eq(0.6D, bee.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).getBaseValue(), "bee flying speed 0.6");
        c.eq(2.0D, bee.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "bee attack 2");
        c.ok(Math.abs(bee.width - 0.7F) < 1e-6 && Math.abs(bee.height - 0.6F) < 1e-6, "bee is 0.7 x 0.6");
        c.eq(net.minecraft.entity.EnumCreatureAttribute.ARTHROPOD, bee.getCreatureAttribute(), "bees take Bane of Arthropods");
        c.eq(0.3F, BeeContent.beeNest.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "nest hardness 0.3");
        c.eq(0.6F, BeeContent.beehive.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "hive hardness 0.6");
        c.eq(30, Blocks.FIRE.getEncouragement(BeeContent.beeNest), "nest catches fire like 1.15");
        for (EnumFacing f : EnumFacing.HORIZONTALS) {
            IBlockState s = BeeContent.beehive.getDefaultState().withProperty(BlockBeehive.FACING, f);
            c.eq(s, BeeContent.beehive.getStateFromMeta(BeeContent.beehive.getMetaFromState(s)), "hive facing " + f + " survives metadata");
        }
        Item comb = Owners.ours("honeycomb");
        if (comb != null) {
            ItemStack p = new ItemStack(Blocks.PLANKS, 1, 2);
            ItemStack h = new ItemStack(comb);
            Crafting.expect(c, Crafting.grid(3, 3, p, p, p, h, h, h, p, p, p), new ItemStack(BeeContent.beehive), "6 planks and 3 honeycomb make a beehive");
        }
        DamageSource sting = new EntityDamageSource(BeeContent.STING_DAMAGE, bee);
        EntityPlayerMP victim = Players.real(c.world, "stungname");
        c.eq("death.attack.buzzybeesbackport.sting", ((net.minecraft.util.text.TextComponentTranslation) sting.getDeathMessage(victim)).getKey(), "stung to death message");
    }

    private void flowersAndSmoke(Check c) {
        c.ok(BeeFlowers.isFlower(Blocks.YELLOW_FLOWER.getDefaultState()), "a dandelion is a flower");
        c.ok(BeeFlowers.isFlower(Blocks.RED_FLOWER.getDefaultState()), "a poppy is a flower");
        IBlockState sunflower = Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.SUNFLOWER);
        c.ok(BeeFlowers.isFlower(sunflower.withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.UPPER)), "a sunflower's head is a flower");
        c.ok(!BeeFlowers.isFlower(sunflower.withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.LOWER)), "a sunflower's stem is not");
        c.ok(BeeFlowers.isFlower(Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.ROSE)), "a rose bush is a flower");
        c.ok(!BeeFlowers.isFlower(Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.GRASS)), "tall grass is not");
        c.ok(!BeeFlowers.isFlower(Blocks.TALLGRASS.getDefaultState()), "grass is not");
        c.ok(BeeFlowers.isFlowerItem(new ItemStack(Blocks.YELLOW_FLOWER)), "a dandelion breeds bees");
        c.ok(BeeFlowers.isFlowerItem(new ItemStack(Blocks.DOUBLE_PLANT, 1, 0)), "a sunflower breeds bees");
        c.ok(!BeeFlowers.isFlowerItem(new ItemStack(Blocks.DOUBLE_PLANT, 1, 2)), "tall grass does not");
        c.ok(!BeeFlowers.isFlowerItem(new ItemStack(Items.WHEAT)), "wheat does not");
        c.ok(new EntityBee(c.world).isBreedingItem(new ItemStack(Blocks.RED_FLOWER)), "the bee breeds with a poppy");

        // Smoke: a stand-in campfire (lit furnace), since the campfire's mod is not in this test.
        BeeSmoke.addSource(state -> state.getBlock() == Blocks.LIT_FURNACE);
        BlockPos base = c.area();
        BlockPos hive = base.add(5, 8, 5);
        c.ok(!BeeSmoke.isSmokey(c.world, hive), "nothing under the hive: not calm");
        c.world.setBlockState(hive.down(2), Blocks.NETHERRACK.getDefaultState(), 2);
        c.world.setBlockState(hive.down(), Blocks.FIRE.getDefaultState(), 2);
        c.ok(BeeSmoke.isSmokey(c.world, hive), "fire right under the hive calms it");
        c.world.setBlockToAir(hive.down());
        c.world.setBlockToAir(hive.down(2));
        c.world.setBlockState(hive.down(5), Blocks.LIT_FURNACE.getDefaultState(), 2);
        c.ok(BeeSmoke.isSmokey(c.world, hive), "a campfire 5 below calms it");
        c.world.setBlockState(hive.down(4), Blocks.STONE.getDefaultState(), 2);
        c.ok(BeeSmoke.isSmokey(c.world, hive), "a block right on the campfire lets the smoke through");
        c.world.setBlockToAir(hive.down(4));
        c.world.setBlockState(hive.down(2), Blocks.STONE.getDefaultState(), 2);
        c.ok(!BeeSmoke.isSmokey(c.world, hive), "a block with a gap above the campfire stops the smoke");
        c.world.setBlockToAir(hive.down(2));
        c.world.setBlockState(hive.down(4), Blocks.CARPET.getDefaultState(), 2);
        c.ok(BeeSmoke.isSmokey(c.world, hive), "a carpet right on the campfire lets the smoke through");
        c.world.setBlockToAir(hive.down(4));
        c.world.setBlockState(hive.down(3), Blocks.CARPET.getDefaultState(), 2);
        c.ok(!BeeSmoke.isSmokey(c.world, hive), "a carpet higher up stops it (1.15's smoke column)");
        c.world.setBlockToAir(hive.down(5));
        c.world.setBlockState(hive.down(6), Blocks.LIT_FURNACE.getDefaultState(), 2);
        c.world.setBlockToAir(hive.down(3));
        c.ok(!BeeSmoke.isSmokey(c.world, hive), "a campfire 6 below is too far");
    }

    private void hiveOccupants(Check c) {
        day(c);
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 4, 8);
        TileEntityBeehive hive = hive(c, pos, BeeContent.beehive, EnumFacing.SOUTH);
        c.ok(hive != null, "a hive has its tile entity");
        EntityBee nectar = bee(c, pos.south());
        nectar.setHasNectar(true);
        nectar.setCustomNameTag("Buzz");
        nectar.setHealth(7.0F);
        hive.addBee(nectar, true);
        c.ok(nectar.isDead, "a bee going in leaves the world");
        c.eq(1, hive.getBeeCount(), "the hive has one bee");
        for (int i = 0; i < 2401; i++) {
            hive.update();
        }
        c.eq(1, hive.getBeeCount(), "a nectar bee rests at least 2400 ticks");
        hive.update();
        hive.update();
        c.eq(0, hive.getBeeCount(), "then it leaves");
        List<EntityBee> out = live(c, pos, 2);
        EntityBee left = out.isEmpty() ? null : out.get(0);
        c.ok(left != null && left.posZ > pos.getZ() + 1.0, "it leaves through the front (south)");
        if (left != null) {
            c.ok(!left.hasNectar(), "it left its nectar behind");
            c.eq("Buzz", left.getCustomNameTag(), "it keeps its name");
            c.eq(7.0F, left.getHealth(), "it keeps its health");
            c.ok(!left.getUniqueID().equals(nectar.getUniqueID()), "it gets a new identity");
        }
        c.ok(hive.getHoneyLevel() == 1 || hive.getHoneyLevel() == 2, "honey went up: " + hive.getHoneyLevel());
        c.eq(hive.getHoneyLevel(), BeeContent.beehive.getComparatorInputOverride(c.world.getBlockState(pos), c.world, pos), "comparators read the honey level");
        c.eq(hive.getHoneyLevel(), c.world.getBlockState(pos).getActualState(c.world, pos).getValue(BlockBeehive.HONEY_LEVEL), "the block shows the honey level");
        for (EntityBee b : out) {
            b.setDead();
        }

        // Three fit, a fourth does not.
        for (int i = 0; i < 4; i++) {
            hive.addBee(bee(c, pos.south(2)), false);
        }
        c.eq(3, hive.getBeeCount(), "a hive holds 3 bees");
        // Night keeps them in; a blocked front keeps them in; day lets them out.
        night(c);
        for (int i = 0; i < 700; i++) {
            hive.update();
        }
        c.eq(3, hive.getBeeCount(), "bees stay in at night");
        day(c);
        c.world.setBlockState(pos.south(), Blocks.STONE.getDefaultState(), 2);
        hive.update();
        c.eq(3, hive.getBeeCount(), "bees stay in with the front blocked");
        c.world.setBlockToAir(pos.south());
        hive.update();
        c.eq(0, hive.getBeeCount(), "by day with the front open they leave");
        // Saving.
        hive.addBee(bee(c, pos.south(2)), true);
        hive.setHoneyLevel(4);
        NBTTagCompound tag = hive.writeToNBT(new NBTTagCompound());
        TileEntityBeehive copy = new TileEntityBeehive();
        copy.readFromNBT(tag);
        c.ok(copy.getBeeCount() == 1 && copy.getHoneyLevel() == 4, "a hive saves its bees and honey");
        // Fire next to the hive throws them out, even at night.
        night(c);
        c.world.setBlockState(pos.up(), Blocks.FIRE.getDefaultState(), 3);
        c.eq(0, hive.getBeeCount(), "fire beside the hive throws the bees out");
        c.world.setBlockToAir(pos.up());
        day(c);
        killBees(c, pos);
    }

    /** Bees still in the world (entities set dead stay in the lists until the next world tick). */
    private List<EntityBee> live(Check c, BlockPos pos, int r) {
        List<EntityBee> out = new java.util.ArrayList<EntityBee>();
        for (EntityBee b : c.world.getEntitiesWithinAABB(EntityBee.class, new net.minecraft.util.math.AxisAlignedBB(pos).grow(r))) {
            if (!b.isDead) {
                out.add(b);
            }
        }
        return out;
    }

    private void killBees(Check c, BlockPos pos) {
        for (EntityBee b : c.world.getEntitiesWithinAABB(EntityBee.class, new net.minecraft.util.math.AxisAlignedBB(pos).grow(16))) {
            b.setDead();
        }
    }

    private void harvesting(Check c) {
        day(c);
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 6, 8);
        TileEntityBeehive hive = hive(c, pos, BeeContent.beehive, EnumFacing.SOUTH);
        EntityPlayerMP player = Players.real(c.world, "harvester");
        c.world.spawnEntity(player);
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5);
        player.setGameType(GameType.SURVIVAL);
        Item comb = Owners.ours("honeycomb");
        Item honey = Owners.ours("honey_bottle");
        // Not full: nothing.
        hive.setHoneyLevel(4);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        c.ok(!BeeContent.beehive.onBlockActivated(c.world, pos, c.world.getBlockState(pos), player, EnumHand.MAIN_HAND, EnumFacing.SOUTH, 0.5F, 0.5F, 0.5F),
                "shears on a hive that is not full do nothing");
        if (comb == null || honey == null) {
            c.fail("honey stream off: harvest not tested");
            return;
        }
        // Full, no smoke, bees inside: 3 honeycombs, bees out and angry at the player.
        hive.setHoneyLevel(5);
        hive.addBee(bee(c, pos.south(2)), false);
        hive.addBee(bee(c, pos.south(2)), false);
        BeeContent.beehive.onBlockActivated(c.world, pos, c.world.getBlockState(pos), player, EnumHand.MAIN_HAND, EnumFacing.SOUTH, 0.5F, 0.5F, 0.5F);
        c.eq(3, Drops.count(c.world, pos, comb), "shears take 3 honeycombs");
        c.eq(0, hive.getHoneyLevel(), "and empty the hive");
        c.eq(1, player.getHeldItemMainhand().getItemDamage(), "the shears wear");
        c.eq(0, hive.getBeeCount(), "the bees come out");
        List<EntityBee> angry = live(c, pos, 4);
        boolean allOnPlayer = !angry.isEmpty();
        for (EntityBee b : angry) {
            allOnPlayer &= b.getAttackTarget() == player;
        }
        c.ok(allOnPlayer, "and go for the player: " + angry.size());
        killBees(c, pos);
        // Full, with fire under it: a honey bottle, calm bees, Bee Our Guest.
        hive.setHoneyLevel(5);
        hive.addBee(bee(c, pos.south(2)), false);
        c.world.setBlockState(pos.down(2), Blocks.NETHERRACK.getDefaultState(), 2);
        c.world.setBlockState(pos.down(), Blocks.FIRE.getDefaultState(), 2);
        c.ok(c.world.getBlockState(pos.down()).getBlock() == Blocks.FIRE, "fire lit under the hive");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
        BeeContent.beehive.onBlockActivated(c.world, pos, c.world.getBlockState(pos), player, EnumHand.MAIN_HAND, EnumFacing.SOUTH, 0.5F, 0.5F, 0.5F);
        c.eq(1, player.getHeldItemMainhand().getCount(), "one glass bottle used");
        c.ok(player.inventory.hasItemStack(new ItemStack(honey)), "a honey bottle in the inventory");
        c.eq(0, hive.getHoneyLevel(), "the hive is empty");
        c.eq(1, hive.getBeeCount(), "calm bees stay in");
        c.ok(Advancements.has(player, "husbandry/safely_harvest_honey"), "Bee Our Guest");
        c.world.setBlockToAir(pos.down());
        c.world.removeEntity(player);
    }

    private ItemStack silkAxe() {
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        axe.addEnchantment(Enchantments.SILK_TOUCH, 1);
        return axe;
    }

    private void breaking(Check c) {
        day(c);
        BlockPos base = c.area();
        EntityPlayerMP player = Players.real(c.world, "breaker");
        c.world.spawnEntity(player);
        player.setGameType(GameType.SURVIVAL);
        // Silk Touch on a nest with 3 bees: the nest with its bees, Total Beelocation.
        BlockPos nest = base.add(6, 5, 6);
        TileEntityBeehive nestHive = hive(c, nest, BeeContent.beeNest, EnumFacing.SOUTH);
        for (int i = 0; i < 3; i++) {
            nestHive.addBee(bee(c, nest.south(2)), false);
        }
        nestHive.setHoneyLevel(3);
        player.setPosition(nest.getX() + 0.5, nest.getY(), nest.getZ() + 2.5);
        player.setHeldItem(EnumHand.MAIN_HAND, silkAxe());
        c.ok(EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, player.getHeldItemMainhand()) == 1, "silk touch axe");
        player.interactionManager.tryHarvestBlock(nest);
        c.ok(c.world.isAirBlock(nest), "the nest is broken");
        List<net.minecraft.entity.item.EntityItem> items = c.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                new net.minecraft.util.math.AxisAlignedBB(nest).grow(2));
        ItemStack dropped = items.isEmpty() ? ItemStack.EMPTY : items.get(0).getItem();
        NBTTagCompound tag = dropped.getSubCompound("BlockEntityTag");
        c.ok(dropped.getItem() == Item.getItemFromBlock(BeeContent.beeNest) && tag != null && tag.getTagList("Bees", 10).tagCount() == 3,
                "Silk Touch drops the nest with its 3 bees");
        c.ok(tag != null && tag.getInteger("HoneyLevel") == 3, "and its honey");
        c.ok(Advancements.has(player, "husbandry/silk_touch_nest"), "Total Beelocation");
        c.eq(1, dropped.getMaxStackSize(), "a nest with bees does not stack");
        for (net.minecraft.entity.item.EntityItem e : items) {
            e.setDead();
        }
        // Placing it back restores them.
        c.world.setBlockState(nest, BeeContent.beeNest.getDefaultState(), 3);
        ItemBlock.setTileEntityNBT(c.world, player, nest, dropped);
        TileEntityBeehive back = (TileEntityBeehive) c.world.getTileEntity(nest);
        c.ok(back.getBeeCount() == 3 && back.getHoneyLevel() == 3, "placed back, it has its bees and honey");
        // By hand, no Silk Touch: a nest drops nothing and the bees come out angry.
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WOODEN_AXE));
        player.interactionManager.tryHarvestBlock(nest);
        c.eq(0, Drops.count(c.world, nest, Item.getItemFromBlock(BeeContent.beeNest)), "a nest broken without Silk Touch drops nothing");
        List<EntityBee> out = live(c, nest, 4);
        c.eq(3, out.size(), "its 3 bees come out");
        boolean angry = true;
        for (EntityBee b : out) {
            angry &= b.getAttackTarget() == player;
        }
        c.ok(angry, "and go for the player");
        killBees(c, nest);
        // A hive drops itself empty.
        BlockPos hivePos = base.add(14, 5, 6);
        TileEntityBeehive hive = hive(c, hivePos, BeeContent.beehive, EnumFacing.SOUTH);
        hive.addBee(bee(c, hivePos.south(2)), false);
        player.setPosition(hivePos.getX() + 0.5, hivePos.getY(), hivePos.getZ() + 2.5);
        player.interactionManager.tryHarvestBlock(hivePos);
        c.eq(1, Drops.count(c.world, hivePos, Item.getItemFromBlock(BeeContent.beehive)), "a hive broken by hand drops itself");
        killBees(c, hivePos);
        c.world.removeEntity(player);
    }

    private void dispensers(Check c) {
        Item comb = Owners.ours("honeycomb");
        Item honey = Owners.ours("honey_bottle");
        if (comb == null || honey == null) {
            return;
        }
        day(c);
        BlockPos base = c.area();
        BlockPos disp = base.add(8, 1, 8);
        BlockPos pos = disp.east();
        c.world.setBlockState(disp, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST), 2);
        TileEntityBeehive hive = hive(c, pos, BeeContent.beehive, EnumFacing.SOUTH);
        hive.setHoneyLevel(5);
        BlockSourceImpl source = new BlockSourceImpl(c.world, disp);
        ItemStack shears = new ItemStack(Items.SHEARS);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.SHEARS).dispense(source, shears);
        c.eq(3, Drops.count(c.world, pos, comb), "a dispenser's shears take 3 honeycombs");
        c.eq(0, hive.getHoneyLevel(), "and empty the hive");
        hive.setHoneyLevel(5);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        ItemStack result = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.GLASS_BOTTLE).dispense(source, bottle);
        c.eq(honey, result.getItem(), "a dispenser's last glass bottle becomes a honey bottle");
        c.eq(0, hive.getHoneyLevel(), "and empties the hive");
        // Not a hive: the bottle is just dispensed (the old behavior).
        c.world.setBlockToAir(pos);
        ItemStack two = new ItemStack(Items.GLASS_BOTTLE, 2);
        ItemStack left = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.GLASS_BOTTLE).dispense(source, two);
        c.eq(1, left.getCount(), "with no hive, a bottle is dispensed as before");
        Drops.count(c.world, pos, Items.GLASS_BOTTLE);
    }

    private void combat(Check c) {
        day(c);
        BlockPos base = c.area();
        BlockPos at = base.add(10, 3, 10);
        EntityPlayerMP player = Players.real(c.world, "stung");
        c.world.spawnEntity(player);
        player.setGameType(GameType.SURVIVAL);
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 3.5);
        // New players cannot be hurt for 60 ticks.
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(EntityPlayerMP.class, player, 0,
                "respawnInvulnerabilityTicks", "field_147101_bU");
        EntityBee hit = bee(c, at);
        EntityBee mate = bee(c, at.east(3));
        EntityBee far = bee(c, at.east(28).up(12));
        // The world, not onUpdate, counts ticksExisted; 1.12's revenge goal ignores a hit at tick 0.
        hit.ticksExisted = 100;
        mate.ticksExisted = 100;
        hit.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F);
        c.ok(hit.getRevengeTarget() == player, "the hit bee knows who hit it");
        for (int t = 0; t < 8; t++) {
            hit.onUpdate();
            mate.onUpdate();
        }
        c.ok(hit.isAngry() && hit.getAttackTarget() == player, "a hit bee turns on the player");
        c.ok(hit.getAngerTime() >= EntityBee.ANGER_MIN - 5 && hit.getAngerTime() <= EntityBee.ANGER_MAX, "for 20-39 seconds: " + hit.getAngerTime());
        c.eq(player.getUniqueID(), hit.getAngerTarget(), "and remembers who");
        c.ok(mate.getAttackTarget() == player, "a bee nearby joins in");
        c.ok(far.getAttackTarget() == null, "a bee far up does not (out of the 10-block height)");
        // Sting: Poison 10 s on Normal, the stinger lost, calm afterwards.
        boolean stung = hit.attackEntityAsMob(player);
        c.ok(stung, "the sting lands");
        PotionEffect poison = player.getActivePotionEffect(MobEffects.POISON);
        c.ok(poison != null && poison.getDuration() == 200, "Poison for 10 seconds on Normal");
        c.ok(hit.hasStung() && !hit.isAngry(), "the bee loses its stinger and calms down");
        c.world.getMinecraftServer().setDifficultyForAllWorlds(EnumDifficulty.HARD);
        player.removePotionEffect(MobEffects.POISON);
        mate.attackEntityAsMob(player);
        poison = player.getActivePotionEffect(MobEffects.POISON);
        c.ok(poison != null && poison.getDuration() == 360, "Poison for 18 seconds on Hard");
        c.world.getMinecraftServer().setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
        // A stung bee dies within a minute or so.
        int ticks = 0;
        while (!hit.isDead && hit.getHealth() > 0 && ticks < 1400) {
            hit.onUpdate();
            ticks++;
        }
        c.ok(hit.getHealth() <= 0 || hit.isDead, "a stung bee dies: after " + ticks + " ticks");
        // Saving the bee.
        mate.setHivePos(at.down());
        mate.setFlowerPos(at.up());
        mate.setHasNectar(true);
        NBTTagCompound tag = new NBTTagCompound();
        mate.writeToNBT(tag);
        EntityBee copy = new EntityBee(c.world);
        copy.readFromNBT(tag);
        c.ok(at.down().equals(copy.getHivePos()) && at.up().equals(copy.getFlowerPos()) && copy.hasNectar() && copy.hasStung(),
                "a bee saves its hive, flower, nectar and stinger");
        // Water hurts after a second.
        BlockPos pool = base.add(3, 0, 3);
        c.world.setBlockState(pool, Blocks.WATER.getDefaultState(), 2);
        c.world.setBlockState(pool.up(), Blocks.WATER.getDefaultState(), 2);
        EntityBee wet = bee(c, pool);
        wet.setPosition(pool.getX() + 0.5, pool.getY() + 0.3, pool.getZ() + 0.5);
        for (int t = 0; t < 40; t++) {
            wet.onUpdate();
            wet.setPosition(pool.getX() + 0.5, pool.getY() + 0.3, pool.getZ() + 0.5);
        }
        c.ok(wet.getHealth() < 10.0F, "a bee in water takes damage: " + wet.getHealth());
        killBees(c, at);
        far.setDead();
        c.world.removeEntity(player);
    }

    private void crops(Check c) {
        BlockPos base = c.area();
        BlockPos farm = base.add(5, 0, 5);
        c.world.setBlockState(farm, Blocks.FARMLAND.getDefaultState(), 2);
        c.world.setBlockState(farm.up(), Blocks.WHEAT.getDefaultState(), 2);
        c.world.setBlockState(farm.east(), Blocks.FARMLAND.getDefaultState(), 2);
        c.world.setBlockState(farm.east().up(), Blocks.MELON_STEM.getDefaultState(), 2);
        EntityBee bee = bee(c, farm.up(2));
        bee.setPosition(farm.getX() + 0.5, farm.getY() + 2.1, farm.getZ() + 0.5);
        bee.setHasNectar(true);
        invokeGrow(c, bee);
        c.eq(1, c.world.getBlockState(farm.up()).getValue(BlockCrops.AGE).intValue(), "a bee grows the wheat below it one stage");
        c.eq(1, bee.getCropsGrownSincePollination(), "and counts it");
        bee.setPosition(farm.getX() + 1.5, farm.getY() + 2.1, farm.getZ() + 0.5);
        invokeGrow(c, bee);
        c.eq(1, c.world.getBlockState(farm.east().up()).getValue(net.minecraft.block.BlockStem.AGE).intValue(), "and a melon stem");
        c.ok(BeeFlowers.grow(Blocks.WHEAT.getDefaultState().withProperty(BlockCrops.AGE, 7)) == null, "ripe wheat does not grow");
        bee.setDead();
    }

    private void invokeGrow(Check c, EntityBee bee) {
        try {
            java.lang.reflect.Method m = EntityBee.class.getDeclaredMethod("growBelow");
            m.setAccessible(true);
            m.invoke(bee);
        } catch (ReflectiveOperationException e) {
            c.fail("growBelow: " + e);
        }
    }

    /** Real AI: a bee carrying nectar finds its way into a hive 6 blocks off. */
    private void flyingHome(Check c) {
        day(c);
        BlockPos base = c.area();
        BlockPos pos = base.add(16, 3, 16);
        TileEntityBeehive hive = hive(c, pos, BeeContent.beehive, EnumFacing.SOUTH);
        EntityBee bee = bee(c, pos.west(6));
        bee.setHasNectar(true);
        int ticks = 0;
        while (!bee.isDead && ticks < 2400) {
            bee.onUpdate();
            ticks++;
        }
        c.ok(hive.getBeeCount() == 1, "a bee with nectar finds and enters a hive 6 blocks away: " + (bee.isDead ? "in after " + ticks + " ticks" : "still out, hive " + bee.getHivePos()));
        killBees(c, pos);
    }

    /** Real AI: a bee in a flower patch ends up with nectar. */
    private void pollinating(Check c) {
        day(c);
        if (c.world.isRaining()) {
            c.fail("raining: pollination not tested");
            return;
        }
        BlockPos base = c.area();
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                c.world.setBlockState(base.add(x, 0, z), Blocks.GRASS.getDefaultState(), 2);
                c.world.setBlockState(base.add(x, 1, z), Blocks.YELLOW_FLOWER.getDefaultState(), 2);
            }
        }
        EntityBee bee = bee(c, base.add(3, 3, 3));
        int ticks = 0;
        while (!bee.hasNectar() && ticks < 3000 && !bee.isDead) {
            bee.onUpdate();
            ticks++;
        }
        c.ok(bee.hasNectar(), "a bee among flowers gathers nectar: " + (bee.hasNectar() ? "after " + ticks + " ticks" : "none in 3000 ticks, flower " + bee.getFlowerPos()));
        c.ok(bee.hasFlower() && BeeFlowers.isFlower(c.world.getBlockState(bee.getFlowerPos())), "and remembers its flower");
        bee.setDead();
    }
}

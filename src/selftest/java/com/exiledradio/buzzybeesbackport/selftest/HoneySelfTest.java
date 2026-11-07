package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.common.Advancements;
import com.exiledradio.buzzybeesbackport.honey.BlockHoney;
import com.exiledradio.buzzybeesbackport.honey.HoneyContent;
import com.exiledradio.buzzybeesbackport.honey.HoneyPistons;

import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.state.BlockPistonStructureHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Stream H: honey bottle, honeycomb, honey block, honeycomb block. */
public final class HoneySelfTest implements SelfTest {

    @Override
    public String name() {
        return "honey";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(HoneyContent.on(), "honey stream registered")) {
            return;
        }
        registration(c);
        recipes(c);
        bottle(c);
        sliding(c);
        landing(c);
        walkingAndJumping(c);
        drinkSound(c);
        pistons(c);
    }

    private void registration(Check c) {
        ItemStack bottle = new ItemStack(HoneyContent.honeyBottle);
        c.eq(16, bottle.getMaxStackSize(), "honey bottle stacks to 16");
        c.eq(Items.GLASS_BOTTLE, HoneyContent.honeyBottle.getContainerItem(), "honey bottle leaves a glass bottle");
        c.eq(64, new ItemStack(HoneyContent.honeycomb).getMaxStackSize(), "honeycomb stacks to 64");
        c.eq("item.buzzybeesbackport.honey_bottle", HoneyContent.honeyBottle.getTranslationKey(), "bottle name key");
        IBlockState honey = HoneyContent.honeyBlock.getDefaultState();
        IBlockState comb = HoneyContent.honeycombBlock.getDefaultState();
        c.eq(0.0F, honey.getBlockHardness(c.world, BlockPos.ORIGIN), "honey block hardness 0");
        c.eq(0.6F, comb.getBlockHardness(c.world, BlockPos.ORIGIN), "honeycomb block hardness 0.6");
        c.ok(Math.abs(HoneyContent.honeycombBlock.getExplosionResistance(null) - 0.6F) < 1e-4, "honeycomb block blast resistance 0.6");
        c.ok(HoneyContent.honeyBlock.isStickyBlock(honey), "honey is sticky to pistons");
        c.ok(!honey.isNormalCube(), "honey block is not a normal cube (does not conduct redstone)");
        c.ok(!honey.isOpaqueCube(), "honey block is see-through");
        AxisAlignedBB box = honey.getCollisionBoundingBox(c.world, BlockPos.ORIGIN);
        c.ok(box != null && Math.abs(box.minX - 1.0 / 16) < 1e-9 && Math.abs(box.maxY - 15.0 / 16) < 1e-9, "honey collision box inset 1/16: " + box);
        c.eq(HoneyContent.HONEY, HoneyContent.honeyBlock.getSoundType(), "honey block sounds");
        c.eq("buzzybeesbackport:block.honey_block.slide", HoneyContent.HONEY_SLIDE.getRegistryName().toString(), "slide sound registered");
        NonNullList<ItemStack> drops = NonNullList.create();
        HoneyContent.honeyBlock.getDrops(drops, c.world, BlockPos.ORIGIN, honey, 0);
        c.ok(drops.size() == 1 && drops.get(0).getItem() == net.minecraft.item.Item.getItemFromBlock(HoneyContent.honeyBlock), "honey block drops itself");
    }

    private void recipes(Check c) {
        ItemStack b = new ItemStack(HoneyContent.honeyBottle);
        ItemStack g = new ItemStack(Items.GLASS_BOTTLE);
        InventoryCrafting four = Crafting.grid(2, 2, b, b, b, b);
        Crafting.expect(c, four, new ItemStack(HoneyContent.honeyBlock), "4 honey bottles make a honey block");
        int bottles = 0;
        for (ItemStack rest : CraftingManager.getRemainingItems(four, c.world)) {
            if (rest.getItem() == Items.GLASS_BOTTLE) {
                bottles += rest.getCount();
            }
        }
        c.eq(4, bottles, "the honey block recipe hands back 4 glass bottles");
        Crafting.expect(c, Crafting.grid(3, 2, new ItemStack(HoneyContent.honeyBlock), g, g, g, g, ItemStack.EMPTY),
                new ItemStack(HoneyContent.honeyBottle, 4), "honey block and 4 bottles make 4 honey bottles");
        Crafting.expect(c, Crafting.grid(1, 1, b), new ItemStack(Items.SUGAR, 3), "a honey bottle makes 3 sugar");
        ItemStack h = new ItemStack(HoneyContent.honeycomb);
        Crafting.expect(c, Crafting.grid(2, 2, h, h, h, h), new ItemStack(HoneyContent.honeycombBlock), "4 honeycombs make a honeycomb block");
    }

    private void bottle(Check c) {
        BlockPos at = c.area().up();
        // A real player: potion effects send a packet a fake player has no connection for.
        EntityPlayerMP player = Players.real(c.world, "honeydrink");
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.getFoodStats().setFoodLevel(10);
        float saturation = player.getFoodStats().getSaturationLevel();
        player.addPotionEffect(new PotionEffect(MobEffects.POISON, 200));
        ItemStack one = new ItemStack(HoneyContent.honeyBottle);
        c.eq(40, HoneyContent.honeyBottle.getMaxItemUseDuration(one), "drunk in 40 ticks");
        c.eq(EnumAction.DRINK, HoneyContent.honeyBottle.getItemUseAction(one), "drunk, not eaten");
        ItemStack left = HoneyContent.honeyBottle.onItemUseFinish(one, c.world, player);
        c.eq(16, player.getFoodStats().getFoodLevel(), "honey restores 6 hunger");
        c.ok(Math.abs(player.getFoodStats().getSaturationLevel() - saturation - 1.2F) < 1e-4, "honey gives 1.2 saturation: " + player.getFoodStats().getSaturationLevel());
        c.ok(!player.isPotionActive(MobEffects.POISON), "honey cures Poison");
        c.eq(Items.GLASS_BOTTLE, left.getItem(), "the last bottle becomes a glass bottle");
        ItemStack two = new ItemStack(HoneyContent.honeyBottle, 2);
        ItemStack after = HoneyContent.honeyBottle.onItemUseFinish(two, c.world, player);
        c.eq(1, after.getCount(), "one of two bottles drunk");
        c.ok(player.inventory.hasItemStack(new ItemStack(Items.GLASS_BOTTLE)), "the empty bottle goes to the inventory");
    }

    private EntityPig pig(Check c, double x, double y, double z) {
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(x, y, z);
        pig.setNoAI(true);
        c.world.spawnEntity(pig);
        return pig;
    }

    private void sliding(Check c) {
        BlockPos base = c.area();
        BlockPos honey = base.add(10, 1, 10);
        for (int i = 0; i < 8; i++) {
            c.world.setBlockState(honey.up(i), HoneyContent.honeyBlock.getDefaultState(), 2);
        }
        // Unit: a pig pressed against the east face, falling fast.
        EntityPig pig = pig(c, honey.getX() + 1 + 0.45 - 0.03, honey.getY() + 0.3, honey.getZ() + 0.5);
        pig.onGround = false;
        pig.motionY = -0.6;
        pig.motionX = 0.2;
        pig.fallDistance = 5.0F;
        HoneyContent.honeyBlock.onEntityCollision(c.world, honey, c.world.getBlockState(honey), pig);
        c.ok(Math.abs(pig.motionY + 0.05) < 1e-9, "sliding caps the fall at 0.05 a tick: " + pig.motionY);
        c.ok(Math.abs(pig.motionX - 0.2 * (0.05 / 0.6)) < 1e-9, "sliding scales sideways motion with the fall: " + pig.motionX);
        c.eq(0.0F, pig.fallDistance, "sliding resets fall distance");
        // Not sliding: over the middle (standing on it), or falling slowly.
        EntityPig slow = pig(c, honey.getX() + 1 + 0.45 - 0.03, honey.getY() + 0.3, honey.getZ() + 0.5);
        slow.onGround = false;
        slow.motionY = -0.05;
        HoneyContent.honeyBlock.onEntityCollision(c.world, honey, c.world.getBlockState(honey), slow);
        c.ok(Math.abs(slow.motionY + 0.05) < 1e-9 && slow.motionX == 0, "a slow fall is left alone");

        // Integration: a pig dropped beside the honey column, against it, ticked for real.
        EntityPig faller = pig(c, honey.getX() + 1 + 0.45 - 0.03, honey.getY() + 7.5, honey.getZ() + 0.5);
        double fastest = 0;
        for (int t = 0; t < 40 && !faller.onGround; t++) {
            faller.onUpdate();
            if (t > 5) {
                fastest = Math.min(fastest, faller.motionY);
            }
        }
        c.ok(fastest > -0.13, "a pig against honey never falls faster than a slide: " + fastest);
        c.ok(faller.fallDistance < 1.0F, "and lands without fall distance: " + faller.fallDistance);
        // The advancement: on a whole second of world time.
        EntityPlayerMP player = Players.real(c.world, "slide");
        c.world.spawnEntity(player);
        player.setPosition(honey.getX() + 1 + 0.3 - 0.03, honey.getY() + 0.3, honey.getZ() + 0.5);
        player.onGround = false;
        player.motionY = -0.5;
        c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() / 20 * 20 + 20);
        HoneyContent.honeyBlock.onEntityCollision(c.world, honey, c.world.getBlockState(honey), player);
        c.ok(Advancements.has(player, "adventure/honey_block_slide"), "sliding gives Sticky Situation");
        c.world.removeEntity(player);
    }

    private void landing(Check c) {
        BlockPos base = c.area();
        BlockPos honey = base.add(4, 0, 4);
        c.world.setBlockState(honey, HoneyContent.honeyBlock.getDefaultState(), 2);
        EntityPig onHoney = pig(c, honey.getX() + 0.5, honey.getY() + 0.9375, honey.getZ() + 0.5);
        EntityPig onStone = pig(c, honey.getX() + 3.5, honey.getY() + 1, honey.getZ() + 0.5);
        HoneyContent.honeyBlock.onFallenUpon(c.world, honey, onHoney, 20.0F);
        Blocks.STONE.onFallenUpon(c.world, honey.east(3), onStone, 20.0F);
        c.eq(4.0F, onHoney.getMaxHealth() - onHoney.getHealth(), "a 20-block fall onto honey does a fifth (4)");
        c.ok(onStone.getHealth() <= 0.0F, "the same fall onto stone kills a 10-health pig (17 damage): " + onStone.getHealth());
    }

    private void walkingAndJumping(Check c) {
        BlockPos base = c.area();
        BlockPos honey = base.add(4, 0, 4);
        c.world.setBlockState(honey, HoneyContent.honeyBlock.getDefaultState(), 2);
        // On top: 0.4 speed.
        EntityPig pig = pig(c, honey.getX() + 0.5, honey.getY() + 0.9375, honey.getZ() + 0.5);
        pig.onGround = true;
        pig.motionX = 0.3;
        HoneyContent.honeyBlock.onEntityCollision(c.world, honey, c.world.getBlockState(honey), pig);
        c.ok(Math.abs(pig.motionX - 0.12) < 1e-9, "walking on honey is 0.4 speed: " + pig.motionX);
        pig.motionY = 0.42;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(pig));
        c.ok(Math.abs(pig.motionY - 0.21) < 1e-9, "jumping from honey is half height: " + pig.motionY);
        pig.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 100, 0));
        pig.motionY = 0.52;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(pig));
        c.ok(Math.abs(pig.motionY - 0.31) < 1e-9, "Jump Boost's extra is not halved: " + pig.motionY);
        // Bare honey: the update event must not slow it a second time.
        pig.motionX = 0.3;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(pig));
        c.ok(Math.abs(pig.motionX - 0.3) < 1e-9, "bare honey is slowed once, by the block: " + pig.motionX);

        // Through a carpet.
        BlockPos honey2 = base.add(10, 0, 4);
        c.world.setBlockState(honey2, HoneyContent.honeyBlock.getDefaultState(), 2);
        c.world.setBlockState(honey2.up(), Blocks.CARPET.getDefaultState(), 2);
        EntityPig carpeted = pig(c, honey2.getX() + 0.5, honey2.getY() + 1 + 1.0 / 16, honey2.getZ() + 0.5);
        carpeted.onGround = true;
        carpeted.motionX = 0.3;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(carpeted));
        c.ok(Math.abs(carpeted.motionX - 0.12) < 1e-9, "honey under a carpet slows: " + carpeted.motionX);
        carpeted.motionY = 0.42;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(carpeted));
        c.ok(Math.abs(carpeted.motionY - 0.21) < 1e-9, "honey under a carpet halves the jump: " + carpeted.motionY);
        // Control: carpet on stone.
        BlockPos stone = base.add(16, 0, 4);
        c.world.setBlockState(stone, Blocks.STONE.getDefaultState(), 2);
        c.world.setBlockState(stone.up(), Blocks.CARPET.getDefaultState(), 2);
        EntityPig plain = pig(c, stone.getX() + 0.5, stone.getY() + 1 + 1.0 / 16, stone.getZ() + 0.5);
        plain.onGround = true;
        plain.motionX = 0.3;
        plain.motionY = 0.42;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(plain));
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(plain));
        c.ok(plain.motionX == 0.3 && plain.motionY == 0.42, "carpet on stone changes nothing");
    }

    private void drinkSound(Check c) {
        FakePlayer player = c.player("honeysound", c.area().up());
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(HoneyContent.honeyBottle));
        player.setActiveHand(EnumHand.MAIN_HAND);
        PlaySoundAtEntityEvent event = new PlaySoundAtEntityEvent(player, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.5F, 1.0F);
        MinecraftForge.EVENT_BUS.post(event);
        c.eq(HoneyContent.HONEY_DRINK, event.getSound(), "drinking honey sounds like honey");
        player.resetActiveHand();
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.POTIONITEM));
        player.setActiveHand(EnumHand.MAIN_HAND);
        PlaySoundAtEntityEvent potion = new PlaySoundAtEntityEvent(player, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.5F, 1.0F);
        MinecraftForge.EVENT_BUS.post(potion);
        c.eq(SoundEvents.ENTITY_GENERIC_DRINK, potion.getSound(), "a potion keeps the generic drink");
        player.resetActiveHand();
    }

    private void pistons(Check c) {
        BlockPos base = c.area();
        // Sticky like slime: a piston pushing honey takes the stone touching the honey's side.
        BlockPos piston = base.add(4, 1, 4);
        c.world.setBlockState(piston, Blocks.PISTON.getDefaultState().withProperty(net.minecraft.block.BlockPistonBase.FACING, EnumFacing.EAST), 2);
        c.world.setBlockState(piston.east(), HoneyContent.honeyBlock.getDefaultState(), 2);
        c.world.setBlockState(piston.east().north(), Blocks.STONE.getDefaultState(), 2);
        BlockPistonStructureHelper helper = new BlockPistonStructureHelper(c.world, piston, EnumFacing.EAST, true);
        c.ok(helper.canMove() && helper.getBlocksToMove().contains(piston.east().north()), "honey drags the stone beside it");

        carryAndLaunch(c, true);
        carryAndLaunch(c, false);
    }

    /**
     * A honey (or, as the control, slime) block moving east one block on its own piston tile entity, a pig
     * standing on it and a pig in front of it.
     */
    private void carryAndLaunch(Check c, boolean honey) {
        BlockPos base = c.area();
        BlockPos from = base.add(8, 1, 8);
        BlockPos to = from.east();
        IBlockState state = honey ? HoneyContent.honeyBlock.getDefaultState() : Blocks.SLIME_BLOCK.getDefaultState();
        c.world.setBlockState(to, Blocks.PISTON_EXTENSION.getDefaultState().withProperty(BlockPistonMoving.FACING, EnumFacing.EAST), 2);
        TileEntity tile = BlockPistonMoving.createTilePiston(state, EnumFacing.EAST, true, false);
        c.world.setTileEntity(to, tile);
        TileEntityPiston te = (TileEntityPiston) c.world.getTileEntity(to);
        EntityPig rider = pig(c, from.getX() + 0.5, from.getY() + (honey ? 0.9375 : 1.0), from.getZ() + 0.5);
        rider.onGround = true;
        EntityPig front = pig(c, to.getX() + 0.9, to.getY(), to.getZ() + 0.5);
        double startX = rider.posX;
        double frontMotion = 0;
        for (int t = 0; t < 3; t++) {
            // Each step on its own game tick: 1.12 caps piston moves at 0.51 a tick per entity.
            c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() + 1);
            HoneyPistons.before(c.world);
            te.update();
            HoneyPistons.after(c.world);
            frontMotion = Math.max(frontMotion, front.motionX);
            rider.onGround = true;
        }
        if (honey) {
            c.ok(Math.abs(rider.posX - startX - 1.0) < 0.05, "a pig on pushed honey rides along one block: moved " + (rider.posX - startX));
            c.ok(front.motionX != 1.0, "honey does not launch what it pushes: motionX " + front.motionX);
        } else {
            c.ok(Math.abs(rider.posX - startX) < 0.05, "control: a pig on pushed slime stays: moved " + (rider.posX - startX));
            c.ok(frontMotion == 1.0, "control: slime launches what it pushes: motionX " + frontMotion);
        }
    }
}

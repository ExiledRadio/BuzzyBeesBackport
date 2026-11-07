package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.asm.ElytraStartTransformer;
import com.exiledradio.buzzybeesbackport.asm.FireworkAngleTransformer;
import com.exiledradio.buzzybeesbackport.asm.ShoulderRidingTransformer;
import com.exiledradio.buzzybeesbackport.asm.WorkbenchShiftTransformer;
import com.exiledradio.buzzybeesbackport.tweaks.AdvancementTweaks;
import com.exiledradio.buzzybeesbackport.tweaks.DispensedFireworks;

import io.netty.buffer.Unpooled;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/** Stream X, second pass: the 1.15 changes that needed patches P2-P5, and the two advancements. */
public final class Tweaks2SelfTest implements SelfTest {

    @Override
    public String name() {
        return "tweaks";
    }

    private static EntityPlayerMP player(WorldServer world, String name, BlockPos at) {
        EntityPlayerMP p = Players.real(world, name);
        world.spawnEntity(p);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setGameType(GameType.SURVIVAL);
        ReflectionHelper.setPrivateValue(EntityPlayerMP.class, p, 0, "respawnInvulnerabilityTicks", "field_147101_bU");
        return p;
    }

    @Override
    public void run(Check c) throws Exception {
        // Loading a class runs its transformer.
        String[] loaded = {ContainerWorkbench.class.getName(), EntityFireworkRocket.class.getName(),
                net.minecraft.network.NetHandlerPlayServer.class.getName(), net.minecraft.entity.player.EntityPlayer.class.getName()};
        c.eq(4, loaded.length, "patched classes loaded");
        c.ok(WorkbenchShiftTransformer.applied, "patch P2 applied to the crafting table container");
        c.ok(ElytraStartTransformer.applied, "patch P3 applied to the server's entity-action handler");
        c.ok(FireworkAngleTransformer.applied, "patch P4 applied to firework rockets");
        c.ok(ShoulderRidingTransformer.applied, "patch P5 applied to the player");
        crafting(c);
        elytra(c);
        fireworks(c);
        parrots(c);
        experience(c);
        advancements(c);
    }

    // ---- experience with the loot ----

    private static int orbs(Check c, net.minecraft.entity.monster.EntityZombie zombie) {
        int xp = 0;
        for (net.minecraft.entity.item.EntityXPOrb orb : c.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityXPOrb.class,
                zombie.getEntityBoundingBox().grow(3.0D))) {
            if (!orb.isDead) {
                xp += orb.getXpValue();
            }
        }
        return xp;
    }

    private static net.minecraft.entity.monster.EntityZombie killed(Check c, EntityPlayerMP p, BlockPos at) {
        net.minecraft.entity.monster.EntityZombie zombie = new net.minecraft.entity.monster.EntityZombie(c.world);
        zombie.setPosition(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        c.world.spawnEntity(zombie);
        zombie.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(p), 1000.0F);
        return zombie;
    }

    private void experience(Check c) {
        BlockPos base = c.area();
        EntityPlayerMP p = player(c.world, "xp", base.add(2, 0, 2));
        net.minecraft.entity.monster.EntityZombie zombie = killed(c, p, base.add(8, 0, 8));
        c.ok(zombie.getHealth() <= 0.0F, "a player kills a zombie");
        int now = orbs(c, zombie);
        c.ok(now >= 5, "its experience drops at once, with the loot: " + now);
        for (int i = 0; i < 20; i++) {
            zombie.onEntityUpdate();
        }
        c.eq(now, orbs(c, zombie), "and not again at the end of the death animation");
        ModConfig.tweaks.xpWithLoot = false;
        try {
            net.minecraft.entity.monster.EntityZombie other = killed(c, p, base.add(20, 0, 20));
            c.eq(0, orbs(c, other), "switched off, nothing at death");
            for (int i = 0; i < 20; i++) {
                other.onEntityUpdate();
            }
            c.ok(orbs(c, other) >= 5, "but at the end of the animation, as in 1.12.2: " + orbs(c, other));
        } finally {
            ModConfig.tweaks.xpWithLoot = true;
        }
        for (Entity e : c.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(base).expand(32, 8, 32).grow(0, 8, 0))) {
            if (!(e instanceof EntityPlayerMP)) {
                e.setDead();
            }
        }
        c.world.removeEntity(p);
    }

    // ---- P2 ----

    private void crafting(Check c) {
        BlockPos base = c.area();
        EntityPlayerMP p = player(c.world, "craftshift", base.add(4, 1, 4));
        c.world.setBlockState(base.add(5, 1, 5), Blocks.CRAFTING_TABLE.getDefaultState(), 2);
        ContainerWorkbench table = new ContainerWorkbench(p.inventory, c.world, base.add(5, 1, 5));
        // Container slots: 0 result, 1-9 grid, 10-36 inventory (player slots 9-35), 37-45 hotbar (0-8).
        p.inventory.setInventorySlotContents(9, new ItemStack(Blocks.COBBLESTONE, 64));
        table.transferStackInSlot(p, 10);
        c.ok(table.getSlot(1).getStack().getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE) && table.getSlot(1).getStack().getCount() == 64,
                "shift-clicking cobblestone in the inventory puts it in the first grid slot: " + table.getSlot(1).getStack());
        c.ok(p.inventory.getStackInSlot(9).isEmpty(), "and takes it from the inventory");
        p.inventory.setInventorySlotContents(0, new ItemStack(Blocks.PLANKS, 10));
        table.transferStackInSlot(p, 37);
        c.eq(10, table.getSlot(2).getStack().getCount(), "planks from the hotbar go to the next free grid slot");
        p.inventory.setInventorySlotContents(1, new ItemStack(Blocks.PLANKS, 5));
        table.transferStackInSlot(p, 38);
        c.eq(15, table.getSlot(2).getStack().getCount(), "more planks join the planks in the grid");
        // A full grid: the old behavior (inventory to hotbar).
        Item[] fillers = {Items.STICK, Items.BONE, Items.FEATHER, Items.STRING, Items.FLINT, Items.PAPER, Items.CLAY_BALL};
        for (int i = 0; i < fillers.length; i++) {
            table.getSlot(3 + i).putStack(new ItemStack(fillers[i], 64));
        }
        table.getSlot(2).putStack(new ItemStack(Blocks.PLANKS, 64));
        p.inventory.setInventorySlotContents(10, new ItemStack(Blocks.DIRT, 20));
        table.transferStackInSlot(p, 11);
        int dirtInHotbar = 0;
        for (int i = 0; i < 9; i++) {
            if (p.inventory.getStackInSlot(i).getItem() == Item.getItemFromBlock(Blocks.DIRT)) {
                dirtInHotbar += p.inventory.getStackInSlot(i).getCount();
            }
        }
        c.eq(20, dirtInHotbar, "with no room in the grid, inventory dirt goes to the hotbar as before");
        // The switch off: 1.12.2's behavior.
        for (int i = 1; i <= 9; i++) {
            table.getSlot(i).putStack(ItemStack.EMPTY);
        }
        ModConfig.tweaks.craftingShiftClick = false;
        try {
            p.inventory.setInventorySlotContents(11, new ItemStack(Blocks.GRAVEL, 7));
            table.transferStackInSlot(p, 12);
            c.ok(table.getSlot(1).getStack().isEmpty(), "switched off, shift-click skips the grid");
        } finally {
            ModConfig.tweaks.craftingShiftClick = true;
        }
        table.onContainerClosed(p);
        c.world.removeEntity(p);
    }

    // ---- P3 ----

    private static CPacketEntityAction startGliding(EntityPlayerMP p) throws Exception {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeVarInt(p.getEntityId());
        buf.writeEnumValue(CPacketEntityAction.Action.START_FALL_FLYING);
        buf.writeVarInt(0);
        CPacketEntityAction packet = new CPacketEntityAction();
        packet.readPacketData(buf);
        return packet;
    }

    private static boolean tryGlide(EntityPlayerMP p, double motionY) throws Exception {
        p.clearElytraFlying();
        p.onGround = false;
        p.motionY = motionY;
        p.connection.processEntityAction(startGliding(p));
        return p.isElytraFlying();
    }

    private void elytra(Check c) throws Exception {
        BlockPos base = c.area();
        EntityPlayerMP p = player(c.world, "elytra", base.add(8, 6, 8));
        p.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        c.ok(tryGlide(p, 0.3D), "the server opens elytra for a player still rising after a jump");
        c.ok(tryGlide(p, -0.3D), "and, as before, for a falling one");
        p.addPotionEffect(new PotionEffect(MobEffects.LEVITATION, 100));
        c.ok(!tryGlide(p, -0.3D), "not with Levitation (1.15)");
        p.removePotionEffect(MobEffects.LEVITATION);
        p.setItemStackToSlot(EntityEquipmentSlot.CHEST, ItemStack.EMPTY);
        c.ok(!tryGlide(p, 0.3D), "not without elytra");
        p.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        ModConfig.tweaks.elytraJump = false;
        try {
            c.ok(!tryGlide(p, 0.3D), "switched off, rising is refused as in 1.12.2");
            c.ok(tryGlide(p, -0.3D), "switched off, falling still works");
        } finally {
            ModConfig.tweaks.elytraJump = true;
        }
        p.clearElytraFlying();
        c.world.removeEntity(p);
    }

    // ---- P4 ----

    private EntityFireworkRocket dispense(Check c, BlockPos disp, EnumFacing facing, ItemStack stack) {
        c.world.setBlockState(disp, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, facing), 2);
        BlockSourceImpl source = new BlockSourceImpl(c.world, disp);
        ItemStack left = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.FIREWORKS).dispense(source, stack);
        c.eq(stack.getCount(), left.getCount(), "the dispenser hands back the rest");
        List<EntityFireworkRocket> rockets = c.world.getEntitiesWithinAABB(EntityFireworkRocket.class, new AxisAlignedBB(disp).grow(2.0D));
        return rockets.isEmpty() ? null : rockets.get(0);
    }

    private void fireworks(Check c) {
        BlockPos base = c.area();
        BlockPos disp = base.add(4, 4, 16);
        ItemStack two = new ItemStack(Items.FIREWORKS, 2);
        EntityFireworkRocket east = dispense(c, disp, EnumFacing.EAST, two);
        c.eq(1, two.getCount(), "a dispensed rocket uses one");
        if (c.ok(east != null, "a dispenser facing east fires a rocket")) {
            c.ok(east.motionX > 0.45D && east.motionX < 0.55D && Math.abs(east.motionY) < 0.05D && Math.abs(east.motionZ) < 0.05D,
                    "east at speed 0.5: " + east.motionX + " " + east.motionY + " " + east.motionZ);
            c.ok(Math.abs(east.posX - (disp.getX() + 0.5D + 0.375D)) < 0.01D && Math.abs(east.posY - (disp.getY() + 0.5D - 0.125D)) < 0.01D,
                    "from the dispenser's face: " + east.posX + " " + east.posY);
            c.ok(DispensedFireworks.shot(east) != null, "marked as shot at an angle");
            double mx = east.motionX;
            double my = east.motionY;
            double x0 = east.posX;
            east.onUpdate();
            east.onUpdate();
            c.ok(Math.abs(east.motionX - mx) < 1e-9 && Math.abs(east.motionY - my) < 1e-9,
                    "it flies straight without speeding up or rising: " + east.motionX + " " + east.motionY);
            c.ok(Math.abs(east.posX - x0 - 2.0D * mx) < 1e-6, "moving 0.5 a tick: " + (east.posX - x0));
            east.setDead();
        }
        EntityFireworkRocket up = dispense(c, base.add(10, 4, 16), EnumFacing.UP, new ItemStack(Items.FIREWORKS));
        if (c.ok(up != null, "a dispenser facing up fires a rocket")) {
            c.ok(up.motionY > 0.45D && up.motionY < 0.55D, "upward at speed 0.5: " + up.motionY);
            double my = up.motionY;
            up.onUpdate();
            c.ok(Math.abs(up.motionY - my) < 1e-9, "at a steady speed (shot at an angle too)");
            up.setDead();
        }
        // Any other rocket keeps 1.12.2's flight.
        EntityFireworkRocket plain = new EntityFireworkRocket(c.world, base.getX() + 20.5D, base.getY() + 4, base.getZ() + 16.5D, new ItemStack(Items.FIREWORKS));
        c.world.spawnEntity(plain);
        double my = plain.motionY;
        plain.onUpdate();
        c.ok(Math.abs(plain.motionY - (my + 0.04D)) < 1e-9, "a rocket used by hand still speeds up: " + my + " -> " + plain.motionY);
        c.ok(DispensedFireworks.shot(plain) == null, "and is not marked");
        plain.setDead();
    }

    // ---- P5 ----

    private void parrots(Check c) {
        BlockPos base = c.area();
        EntityPlayerMP p = player(c.world, "parrot", base.add(6, 0, 6));
        p.onGround = true;
        EntityParrot parrot = new EntityParrot(c.world);
        parrot.setPosition(p.posX, p.posY, p.posZ);
        c.world.spawnEntity(parrot);
        parrot.setTamedBy(p);
        c.ok(parrot.setEntityOnShoulder(p), "a parrot sits on the player's shoulder");
        EntityBoat boat = new EntityBoat(c.world, p.posX, p.posY, p.posZ);
        c.world.spawnEntity(boat);
        c.ok(p.startRiding(boat, true), "the player gets in a boat");
        p.fallDistance = 0.0F;
        p.onLivingUpdate();
        c.ok(!p.getLeftShoulderEntity().isEmpty(), "the parrot stays on the shoulder while riding");
        ModConfig.tweaks.parrotsWhileRiding = false;
        try {
            p.onLivingUpdate();
            c.ok(p.getLeftShoulderEntity().isEmpty(), "switched off, riding drops it as in 1.12.2");
        } finally {
            ModConfig.tweaks.parrotsWhileRiding = true;
        }
        p.dismountRidingEntity();
        for (Entity e : c.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(base).grow(0, 0, 0).expand(16, 8, 16))) {
            if (e instanceof EntityParrot || e instanceof EntityBoat) {
                e.setDead();
            }
        }
        c.world.removeEntity(p);
    }

    // ---- advancements ----

    private void advancements(Check c) throws Exception {
        MinecraftServer server = c.world.getMinecraftServer();
        Advancement two = server.getAdvancementManager().getAdvancement(AdvancementTweaks.TWO_BY_TWO);
        Advancement diet = server.getAdvancementManager().getAdvancement(AdvancementTweaks.BALANCED_DIET);
        c.ok(two.getCriteria().containsKey(AdvancementTweaks.BEE_CRITERION), "Two by Two has a bee criterion");
        c.ok(Arrays.asList(two.getRequirements()[two.getRequirements().length - 1]).contains(AdvancementTweaks.BEE_CRITERION),
                "and needs it");
        c.ok(two.getCriteria().containsKey("bred_cow") && two.getRequirements().length == 11, "keeping vanilla's ten: " + two.getRequirements().length);
        c.ok(diet.getCriteria().containsKey(AdvancementTweaks.HONEY_CRITERION) && diet.getRequirements().length == 36,
                "A Balanced Diet needs a honey bottle too (35 + 1): " + diet.getRequirements().length);

        BlockPos base = c.area();
        EntityPlayerMP p = player(c.world, "adv", base.add(4, 1, 4));
        EntityCow cow1 = new EntityCow(c.world);
        EntityCow cow2 = new EntityCow(c.world);
        EntityCow calf = new EntityCow(c.world);
        CriteriaTriggers.BRED_ANIMALS.trigger(p, cow1, cow2, calf);
        c.ok(p.getAdvancements().getProgress(two).getCriterionProgress("bred_cow").isObtained(), "breeding cows counts for cows");
        c.ok(!p.getAdvancements().getProgress(two).getCriterionProgress(AdvancementTweaks.BEE_CRITERION).isObtained(), "not for bees");
        Entity bee1 = EntityList.createEntityByIDFromName(new ResourceLocation("buzzybeesbackport", "bee"), c.world);
        Entity bee2 = EntityList.createEntityByIDFromName(new ResourceLocation("buzzybeesbackport", "bee"), c.world);
        Entity baby = EntityList.createEntityByIDFromName(new ResourceLocation("buzzybeesbackport", "bee"), c.world);
        CriteriaTriggers.BRED_ANIMALS.trigger(p, (EntityAnimal) bee1, (EntityAnimal) bee2, (EntityAnimal) baby);
        c.ok(p.getAdvancements().getProgress(two).getCriterionProgress(AdvancementTweaks.BEE_CRITERION).isObtained(), "breeding bees counts for Two by Two");

        Item honey = ForgeRegistries.ITEMS.getValue(new ResourceLocation("buzzybeesbackport", "honey_bottle"));
        ItemStack bottle = new ItemStack(honey);
        p.getFoodStats().setFoodLevel(10);
        honey.onItemUseFinish(bottle, c.world, p);
        c.ok(p.getAdvancements().getProgress(diet).getCriterionProgress(AdvancementTweaks.HONEY_CRITERION).isObtained(),
                "drinking a honey bottle counts for A Balanced Diet");

        // /reload builds new advancement objects: they get the criteria again, and saved progress is kept.
        p.getAdvancements().save();
        server.getAdvancementManager().reload();
        server.getPlayerList().reloadResources();
        AdvancementTweaks.check(server);
        Advancement two2 = server.getAdvancementManager().getAdvancement(AdvancementTweaks.TWO_BY_TWO);
        c.ok(two2 != two && two2.getCriteria().containsKey(AdvancementTweaks.BEE_CRITERION), "after a reload Two by Two has the bee again");
        c.ok(p.getAdvancements().getProgress(two2).getCriterionProgress(AdvancementTweaks.BEE_CRITERION).isObtained(),
                "and the player's bee progress is kept");
        Advancement diet2 = server.getAdvancementManager().getAdvancement(AdvancementTweaks.BALANCED_DIET);
        c.ok(p.getAdvancements().getProgress(diet2).getCriterionProgress(AdvancementTweaks.HONEY_CRITERION).isObtained(),
                "and the honey bottle progress");
        File saved = new File(new File(c.world.getSaveHandler().getWorldDirectory(), "advancements"), p.getUniqueID() + ".json");
        c.world.removeEntity(p);
        saved.delete();
    }
}

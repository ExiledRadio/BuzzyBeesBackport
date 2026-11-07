package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.buzzybeesbackport.api.BeeSmoke;
import com.exiledradio.buzzybeesbackport.campfire.BlockCampfire;
import com.exiledradio.buzzybeesbackport.campfire.CampfireContent;
import com.exiledradio.buzzybeesbackport.campfire.CampfireRecipes;
import com.exiledradio.buzzybeesbackport.campfire.PacketCampfireDowse;
import com.exiledradio.buzzybeesbackport.campfire.TileEntityCampfire;
import com.exiledradio.buzzybeesbackport.common.ModCreativeTab;
import com.exiledradio.buzzybeesbackport.common.Owners;
import com.exiledradio.buzzybeesbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Stream W: the copy of Village &amp; Pillage's campfire, registered when that mod is absent. */
public final class CampfireSelfTest implements SelfTest {

    private static final ResourceLocation OURS = new ResourceLocation("buzzybeesbackport", "campfire");
    private static final ResourceLocation OWNERS = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "campfire");

    private BlockCampfire campfire;

    @Override
    public String name() {
        return "campfire";
    }

    @Override
    public void run(Check c) {
        if (Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            ownerPresent(c);
            return;
        }
        if (!registration(c)) {
            return;
        }
        states(c);
        lighting(c);
        cooking(c);
        breaking(c);
        signalFire(c);
        damage(c);
        projectiles(c);
        remap(c);
        bees(c);
    }

    private EntityPlayerMP player(Check c, String name, BlockPos at) {
        EntityPlayerMP p = Players.real(c.world, name);
        c.world.spawnEntity(p);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setGameType(GameType.SURVIVAL);
        ReflectionHelper.setPrivateValue(EntityPlayerMP.class, p, 0, "respawnInvulnerabilityTicks", "field_147101_bU");
        return p;
    }

    /** The server's own right-click path, as a player's packet takes it. */
    private static void click(EntityPlayerMP player, WorldServer world, ItemStack held, BlockPos at, EnumFacing side) {
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        player.interactionManager.processRightClickBlock(player, world, held, EnumHand.MAIN_HAND, at, side, 0.5F, 0.5F, 0.5F);
    }

    private IBlockState lit(boolean lit, EnumFacing facing) {
        return this.campfire.getDefaultState().withProperty(BlockCampfire.LIT, lit).withProperty(BlockCampfire.FACING, facing);
    }

    private boolean isLit(Check c, BlockPos pos) {
        IBlockState s = c.world.getBlockState(pos);
        return s.getBlock() == this.campfire && s.getValue(BlockCampfire.LIT);
    }

    private TileEntityCampfire tile(Check c, BlockPos pos) {
        TileEntity t = c.world.getTileEntity(pos);
        return t instanceof TileEntityCampfire ? (TileEntityCampfire) t : null;
    }

    // ------------------------------------------------------------------------------------------

    private boolean registration(Check c) {
        c.ok(!Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE), "Village & Pillage is not installed");
        c.ok(CampfireContent.enabled(), "so the copy is enabled");
        this.campfire = CampfireContent.campfire;
        if (!c.ok(this.campfire != null, "the campfire is registered")) {
            return false;
        }
        Block named = ForgeRegistries.BLOCKS.getValue(OURS);
        c.ok(named == this.campfire, "under buzzybeesbackport:campfire");
        c.ok(!ForgeRegistries.BLOCKS.containsKey(OWNERS), "nothing under the owner's name");
        c.ok(Owners.block(Owners.VILLAGE_AND_PILLAGE, "campfire") == this.campfire, "lookups give ours");
        Item item = Item.getItemFromBlock(this.campfire);
        c.eq(OURS, item.getRegistryName(), "its item");
        c.eq("tile.buzzybeesbackport.campfire", this.campfire.getTranslationKey(), "translation key");
        c.ok(this.campfire.getCreativeTab() == ModCreativeTab.TAB, "in the creative tab");
        c.eq(OURS, TileEntity.getKey(TileEntityCampfire.class), "tile entity id");
        c.ok(this.campfire.createTileEntity(c.world, this.campfire.getDefaultState()) instanceof TileEntityCampfire, "makes its tile entity");
        c.ok(CampfireContent.crackle != null
                && ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("buzzybeesbackport", "block.campfire.crackle")) == CampfireContent.crackle,
                "crackle sound registered");
        c.eq(2.0F, this.campfire.getDefaultState().getBlockHardness(c.world, BlockPos.ORIGIN), "hardness 2");
        c.eq("axe", this.campfire.getHarvestTool(this.campfire.getDefaultState()), "axe");
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack e = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(3, 3, e, stick, e, stick, new ItemStack(Items.COAL), stick,
                new ItemStack(Blocks.LOG), new ItemStack(Blocks.LOG), new ItemStack(Blocks.LOG)), new ItemStack(this.campfire), "crafted from coal");
        Crafting.expect(c, Crafting.grid(3, 3, e, stick, e, stick, new ItemStack(Items.COAL, 1, 1), stick,
                new ItemStack(Blocks.LOG, 1, 2), new ItemStack(Blocks.LOG2), new ItemStack(Blocks.LOG, 1, 1)), new ItemStack(this.campfire), "or charcoal, any logs");
        // Packet 35 survives its own encoding, and sending with nobody to hear it is harmless.
        c.ok(NetworkHandler.channel != null, "network channel");
        ByteBuf buf = Unpooled.buffer();
        new PacketCampfireDowse(new BlockPos(5, 70, -9), true).toBytes(buf);
        PacketCampfireDowse back = new PacketCampfireDowse();
        back.fromBytes(buf);
        c.ok(back.pos().equals(new BlockPos(5, 70, -9)) && back.signal(), "dowse packet round trip");
        return true;
    }

    private void states(Check c) {
        Set<Integer> metas = new HashSet<Integer>();
        int n = 0;
        boolean all = true;
        for (IBlockState s : this.campfire.getBlockState().getValidStates()) {
            int meta = this.campfire.getMetaFromState(s);
            metas.add(meta);
            all &= this.campfire.getStateFromMeta(meta) == s;
            n++;
        }
        c.eq(16, n, "16 states (facing, lit, signal_fire)");
        c.eq(16, metas.size(), "each its own metadata");
        c.ok(all, "every state survives a metadata round trip");
        IBlockState def = this.campfire.getDefaultState();
        c.ok(def.getValue(BlockCampfire.LIT) && !def.getValue(BlockCampfire.SIGNAL_FIRE) && def.getValue(BlockCampfire.FACING) == EnumFacing.NORTH,
                "default: lit, facing north, no signal");
        c.eq(15, lit(true, EnumFacing.NORTH).getLightValue(), "light 15 lit");
        c.eq(0, lit(false, EnumFacing.NORTH).getLightValue(), "light 0 out");
        c.ok(BlockCampfire.isSmokeSource(lit(true, EnumFacing.EAST)) && !BlockCampfire.isSmokeSource(lit(false, EnumFacing.EAST)), "smoke source only lit");
        c.eq(EnumFacing.EAST, lit(true, EnumFacing.NORTH).withRotation(Rotation.CLOCKWISE_90).getValue(BlockCampfire.FACING), "rotates");
        c.eq(PathNodeType.DAMAGE_FIRE, this.campfire.getAiPathNodeType(lit(true, EnumFacing.NORTH), c.world, BlockPos.ORIGIN, null), "mobs avoid a lit one");
        c.eq(null, this.campfire.getAiPathNodeType(lit(false, EnumFacing.NORTH), c.world, BlockPos.ORIGIN, null), "not an unlit one");
    }

    private void lighting(Check c) {
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 0, 8);
        EntityPlayerMP player = player(c, "campfirelighter", base.add(8, 0, 11));
        player.rotationYaw = 180.0F;
        ItemStack held = new ItemStack(this.campfire, 2);
        click(player, c.world, held, pos.down(), EnumFacing.UP);
        IBlockState placed = c.world.getBlockState(pos);
        c.ok(placed.getBlock() == this.campfire && placed.getValue(BlockCampfire.LIT) && placed.getValue(BlockCampfire.FACING) == EnumFacing.NORTH,
                "placed by a player: lit, facing the way they look " + placed);
        c.eq(1, held.getCount(), "one used");
        c.ok(tile(c, pos) != null, "with its tile entity");
        TileEntityCampfire tile = tile(c, pos);

        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        click(player, c.world, shovel, pos, EnumFacing.DOWN);
        c.ok(isLit(c, pos), "a shovel from below does nothing");
        click(player, c.world, shovel, pos, EnumFacing.UP);
        c.ok(!isLit(c, pos), "a shovel puts it out");
        c.eq(1, shovel.getItemDamage(), "and wears");
        c.ok(tile(c, pos) == tile, "the tile entity survives going out");

        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        click(player, c.world, flint, pos, EnumFacing.UP);
        c.ok(isLit(c, pos), "flint and steel lights it");
        c.eq(1, flint.getItemDamage(), "and wears");

        ItemStack water = new ItemStack(Items.WATER_BUCKET);
        click(player, c.world, water, pos, EnumFacing.UP);
        c.ok(!isLit(c, pos), "a water bucket puts it out");
        c.ok(player.getHeldItemMainhand().getItem() == Items.BUCKET, "leaving an empty bucket");
        c.ok(c.world.getBlockState(pos.up()).getBlock() != Blocks.WATER && c.world.getBlockState(pos.up()).getBlock() != Blocks.FLOWING_WATER,
                "and pours no water");

        ItemStack charge = new ItemStack(Items.FIRE_CHARGE, 2);
        click(player, c.world, charge, pos, EnumFacing.UP);
        c.ok(isLit(c, pos), "a fire charge lights it");
        c.eq(1, charge.getCount(), "using one");
        c.world.removeEntity(player);
    }

    private void cooking(Check c) {
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 0, 8);
        c.world.setBlockState(pos, lit(true, EnumFacing.NORTH), 3);
        TileEntityCampfire tile = tile(c, pos);
        EntityPlayerMP player = player(c, "campfirecook", base.add(8, 0, 11));

        c.ok(CampfireRecipes.cooks(new ItemStack(Items.BEEF)) && CampfireRecipes.cooks(new ItemStack(Items.POTATO))
                && CampfireRecipes.cooks(new ItemStack(Items.FISH, 1, 1)) && CampfireRecipes.cooks(new ItemStack(Items.CHICKEN)),
                "cooks raw beef, potatoes, salmon, chicken");
        c.ok(!CampfireRecipes.cooks(new ItemStack(Items.CHORUS_FRUIT)) && !CampfireRecipes.cooks(new ItemStack(Blocks.IRON_ORE))
                && !CampfireRecipes.cooks(new ItemStack(Items.COOKED_BEEF)), "not chorus fruit, iron ore or cooked beef");

        click(player, c.world, new ItemStack(Items.STICK), pos, EnumFacing.UP);
        c.ok(tile.items().get(0).isEmpty(), "a stick does not go on");
        ItemStack beef = new ItemStack(Items.BEEF, 2);
        click(player, c.world, beef, pos, EnumFacing.UP);
        c.ok(tile.items().get(0).getItem() == Items.BEEF && tile.items().get(0).getCount() == 1, "one raw beef goes on");
        c.eq(1, beef.getCount(), "from the player's stack");
        for (int i = 0; i < 599; i++) {
            tile.update();
        }
        c.eq(0, Drops.count(c.world, pos, Items.COOKED_BEEF), "not done at 599 ticks");
        tile.update();
        c.eq(1, Drops.count(c.world, pos, Items.COOKED_BEEF), "steak pops off at 600 ticks");
        c.ok(tile.items().get(0).isEmpty(), "and the spot is free");

        tile.placeFood(new ItemStack(Items.POTATO), 600);
        for (int i = 0; i < 100; i++) {
            tile.update();
        }
        c.eq(100, tile.cookingTime(0), "100 ticks of cooking");
        BlockCampfire.extinguish(c.world, pos, c.world.getBlockState(pos), false);
        for (int i = 0; i < 10; i++) {
            tile.update();
        }
        c.eq(80, tile.cookingTime(0), "out: the timer runs back twice as fast");
        c.ok(tile.items().get(0).getItem() == Items.POTATO, "and the food stays on");
        BlockCampfire.ignite(c.world, pos, c.world.getBlockState(pos));
        tile.update();
        c.eq(81, tile.cookingTime(0), "relit, it carries on");

        NBTTagCompound tag = tile.writeToNBT(new NBTTagCompound());
        TileEntityCampfire copy = new TileEntityCampfire();
        copy.readFromNBT(tag);
        c.ok(copy.items().get(0).getItem() == Items.POTATO && copy.cookingTime(0) == 81, "food and timer are saved");

        for (int i = 1; i < 4; i++) {
            c.ok(tile.placeFood(new ItemStack(Items.MUTTON), 600), "spot " + i);
        }
        c.ok(!tile.placeFood(new ItemStack(Items.MUTTON), 600), "four spots, no fifth");
        c.world.removeEntity(player);
    }

    private void breaking(Check c) {
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 0, 8);
        EntityPlayerMP player = player(c, "campfirebreaker", base.add(8, 0, 11));
        c.world.setBlockState(pos, lit(true, EnumFacing.NORTH), 3);
        tile(c, pos).placeFood(new ItemStack(Items.PORKCHOP), 600);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        player.interactionManager.tryHarvestBlock(pos);
        c.ok(c.world.isAirBlock(pos), "broken");
        int coal = 0;
        int pork = 0;
        for (EntityItem e : c.world.getEntitiesWithinAABB(EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(pos).grow(2))) {
            if (e.getItem().getItem() == Items.COAL && e.getItem().getMetadata() == 1) {
                coal += e.getItem().getCount();
            }
            if (e.getItem().getItem() == Items.PORKCHOP) {
                pork += e.getItem().getCount();
            }
            e.setDead();
        }
        c.eq(2, coal, "drops 2 charcoal");
        c.eq(1, pork, "and what was cooking");

        c.world.setBlockState(pos, lit(true, EnumFacing.NORTH), 3);
        ItemStack silk = new ItemStack(Items.IRON_AXE);
        silk.addEnchantment(Enchantments.SILK_TOUCH, 1);
        player.setHeldItem(EnumHand.MAIN_HAND, silk);
        player.interactionManager.tryHarvestBlock(pos);
        c.eq(1, Drops.count(c.world, pos, Item.getItemFromBlock(this.campfire)), "Silk Touch drops the campfire");
        c.world.removeEntity(player);
    }

    private void signalFire(Check c) {
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 1, 8);
        c.world.setBlockState(pos.down(), Blocks.HAY_BLOCK.getDefaultState(), 3);
        EntityPlayerMP player = player(c, "campfiresignal", base.add(8, 0, 11));
        click(player, c.world, new ItemStack(this.campfire), pos.down(), EnumFacing.UP);
        IBlockState s = c.world.getBlockState(pos);
        c.ok(s.getBlock() == this.campfire && s.getValue(BlockCampfire.SIGNAL_FIRE), "placed on hay: a signal fire " + s);
        c.world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 3);
        c.ok(!c.world.getBlockState(pos).getValue(BlockCampfire.SIGNAL_FIRE), "hay replaced: no longer a signal fire");
        c.world.setBlockState(pos.down(), Blocks.HAY_BLOCK.getDefaultState(), 3);
        c.ok(c.world.getBlockState(pos).getValue(BlockCampfire.SIGNAL_FIRE), "hay back: a signal fire again");
        c.ok(c.world.getBlockState(pos).getValue(BlockCampfire.LIT), "and still lit");
        c.world.removeEntity(player);
    }

    /** Moves an entity standing in the campfire by a hair, which runs the block collisions as walking does. */
    private float standIn(Check c, Entity entity, BlockPos pos) {
        entity.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        c.world.spawnEntity(entity);
        float before = entity instanceof net.minecraft.entity.EntityLivingBase ? ((net.minecraft.entity.EntityLivingBase) entity).getHealth() : 0;
        entity.move(MoverType.SELF, 0.0D, -0.01D, 0.0D);
        float after = entity instanceof net.minecraft.entity.EntityLivingBase ? ((net.minecraft.entity.EntityLivingBase) entity).getHealth() : 0;
        return before - after;
    }

    private void damage(Check c) {
        BlockPos base = c.area();
        BlockPos litPos = base.add(4, 0, 4);
        BlockPos outPos = base.add(12, 0, 4);
        c.world.setBlockState(litPos, lit(true, EnumFacing.NORTH), 3);
        c.world.setBlockState(outPos, lit(false, EnumFacing.NORTH), 3);
        EntityPig pig = new EntityPig(c.world);
        c.eq(1.0F, standIn(c, pig, litPos), "a pig standing in a lit campfire takes 1");
        c.ok(!pig.isBurning(), "and is not set alight");
        c.eq(0.0F, standIn(c, new EntityPig(c.world), outPos), "none from an unlit one");
        c.eq(0.0F, standIn(c, new EntityPigZombie(c.world), base.add(4, 0, 4)), "none to a mob immune to fire");
        EntityZombie walker = new EntityZombie(c.world);
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FROST_WALKER, 1);
        walker.setItemStackToSlot(EntityEquipmentSlot.FEET, boots);
        c.eq(0.0F, standIn(c, walker, litPos), "none with Frost Walker boots");
        EntityItem stick = new EntityItem(c.world, 0, 0, 0, new ItemStack(Items.STICK));
        standIn(c, stick, litPos);
        c.ok(!stick.isDead, "an item lying in it is not burned");
        for (Entity e : c.world.getEntitiesWithinAABB(Entity.class, new net.minecraft.util.math.AxisAlignedBB(base, base.add(16, 4, 16)))) {
            if (!(e instanceof EntityPlayerMP)) {
                e.setDead();
            }
        }
    }

    private RayTraceResult hit(BlockPos pos) {
        return new RayTraceResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5), EnumFacing.UP, pos);
    }

    private void projectiles(Check c) {
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 0, 8);
        c.world.setBlockState(pos, lit(false, EnumFacing.NORTH), 3);
        EntityTippedArrow cold = new EntityTippedArrow(c.world, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent.Arrow(cold, hit(pos)));
        c.ok(!isLit(c, pos), "a plain arrow does not light it");
        EntityTippedArrow burning = new EntityTippedArrow(c.world, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
        burning.setFire(5);
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent.Arrow(burning, hit(pos)));
        c.ok(isLit(c, pos), "a burning arrow lights it");

        EntityPotion splash = new EntityPotion(c.world, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                PotionUtils.addPotionToItemStack(new ItemStack(Items.SPLASH_POTION), PotionTypes.WATER));
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent.Throwable(splash, hit(pos)));
        c.ok(!isLit(c, pos), "a splash water bottle puts it out");

        EntitySmallFireball ball = new EntitySmallFireball(c.world, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0, -1, 0);
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent.Fireball(ball, hit(pos)));
        c.ok(isLit(c, pos), "a small fireball lights it (mobGriefing on)");
    }

    private void remap(Check c) {
        @SuppressWarnings("unchecked")
        IForgeRegistry<Block> blocks = (IForgeRegistry<Block>) (Object) ForgeRegistries.BLOCKS;
        RegistryEvent.MissingMappings.Mapping<Block> m = new RegistryEvent.MissingMappings.Mapping<Block>(blocks, blocks, OWNERS, 4000);
        RegistryEvent.MissingMappings.Mapping<Block> other = new RegistryEvent.MissingMappings.Mapping<Block>(blocks, blocks,
                new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "lantern"), 4001);
        CampfireContent.remapOwner(new RegistryEvent.MissingMappings<Block>(new ResourceLocation("minecraft", "blocks"), blocks, Arrays.asList(m, other)));
        c.eq(RegistryEvent.MissingMappings.Action.REMAP, m.getAction(), "the owner's campfire remapped");
        c.ok(m.getTarget() == this.campfire, "onto ours");
        c.ok(other.getAction() != RegistryEvent.MissingMappings.Action.REMAP, "the owner's other blocks left alone");

        @SuppressWarnings("unchecked")
        IForgeRegistry<Item> items = (IForgeRegistry<Item>) (Object) ForgeRegistries.ITEMS;
        RegistryEvent.MissingMappings.Mapping<Item> mi = new RegistryEvent.MissingMappings.Mapping<Item>(items, items, OWNERS, 5000);
        CampfireContent.remapOwnerItems(new RegistryEvent.MissingMappings<Item>(new ResourceLocation("minecraft", "items"), items, Arrays.asList(mi)));
        c.ok(mi.getAction() == RegistryEvent.MissingMappings.Action.REMAP && mi.getTarget() == Item.getItemFromBlock(this.campfire), "and its item");
    }

    private void bees(Check c) {
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 0, 8);
        c.world.setBlockState(pos, lit(true, EnumFacing.NORTH), 3);
        c.ok(BeeSmoke.isSmokeSource(c.world.getBlockState(pos)), "bees know our lit campfire");
        c.ok(BeeSmoke.isSmokey(c.world, pos.up(5)), "a hive 5 above our lit campfire is calm");
        c.ok(!BeeSmoke.isSmokey(c.world, pos.up(6)), "6 above is too far");
        c.world.setBlockState(pos, lit(false, EnumFacing.NORTH), 3);
        c.ok(!BeeSmoke.isSmokeSource(c.world.getBlockState(pos)), "not unlit");
        c.ok(!BeeSmoke.isSmokey(c.world, pos.up(5)), "a hive above an unlit campfire is not calm");
    }

    /** With Village &amp; Pillage installed (its jar in eclipse/mods): no copy, lookups give the owner's. */
    private void ownerPresent(Check c) {
        c.ok(!CampfireContent.enabled(), "no copy with the owner installed");
        c.ok(CampfireContent.campfire == null && CampfireContent.crackle == null, "nothing registered");
        c.ok(!ForgeRegistries.BLOCKS.containsKey(OURS), "no buzzybeesbackport:campfire");
        Block owner = Owners.block(Owners.VILLAGE_AND_PILLAGE, "campfire");
        c.ok(owner != null && OWNERS.equals(owner.getRegistryName()), "lookups give the owner's campfire");
        if (owner != null) {
            BlockPos pos = c.area().add(8, 0, 8);
            c.world.setBlockState(pos, owner.getDefaultState(), 3);
            c.ok(BeeSmoke.isSmokey(c.world, pos.up(5)), "the owner's lit campfire calms bees");
        }
    }
}

package com.exiledradio.buzzybeesbackport.bee;

import com.exiledradio.buzzybeesbackport.api.BeeSmoke;
import com.exiledradio.buzzybeesbackport.common.Advancements;
import com.exiledradio.buzzybeesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The bee nest and the beehive, from 1.16.5's {@code BeehiveBlock}: one class, {@link #nest} telling them
 * apart (a nest drops nothing without Silk Touch; a hive drops itself empty).
 *
 * <p>Metadata holds only the facing. The honey level is the tile entity's, read into {@code honey_level} by
 * {@link #getActualState}; change it only through {@link TileEntityBeehive#setHoneyLevel}.
 */
public class BlockBeehive extends Block {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyInteger HONEY_LEVEL = PropertyInteger.create("honey_level", 0, 5);

    public final boolean nest;

    public BlockBeehive(boolean nest) {
        super(Material.WOOD, nest ? MapColor.YELLOW : MapColor.WOOD);
        this.nest = nest;
        setHardness(nest ? 0.3F : 0.6F);
        setSoundType(SoundType.WOOD);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(HONEY_LEVEL, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HONEY_LEVEL);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        int level = te instanceof TileEntityBeehive ? ((TileEntityBeehive) te).getHoneyLevel() : 0;
        return state.withProperty(HONEY_LEVEL, level);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityBeehive();
    }

    @Nullable
    static TileEntityBeehive hive(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileEntityBeehive ? (TileEntityBeehive) te : null;
    }

    /** @return the honey level at a position, 0 if it is not a hive */
    public static int honeyLevel(IBlockAccess world, BlockPos pos) {
        TileEntityBeehive hive = hive(world, pos);
        return hive == null ? 0 : hive.getHoneyLevel();
    }

    // ---- placing ----

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    // ---- harvesting honey ----

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (honeyLevel(world, pos) < TileEntityBeehive.MAX_HONEY) {
            return false;
        }
        Item honeycomb = Owners.ours("honeycomb");
        Item honeyBottle = Owners.ours("honey_bottle");
        boolean harvested = false;
        boolean bottle = false;
        if (held.getItem() instanceof ItemShears && honeycomb != null) {
            if (!world.isRemote) {
                world.playSound(null, player.posX, player.posY, player.posZ, BeeContent.HIVE_SHEAR, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                dropHoneycomb(world, pos);
                held.damageItem(1, player);
            }
            harvested = true;
        } else if (held.getItem() == Items.GLASS_BOTTLE && honeyBottle != null) {
            if (!world.isRemote) {
                held.shrink(1);
                world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                ItemStack honey = new ItemStack(honeyBottle);
                if (held.isEmpty()) {
                    player.setHeldItem(hand, honey);
                } else if (!player.inventory.addItemStackToInventory(honey)) {
                    player.dropItem(honey, false);
                }
            }
            harvested = true;
            bottle = true;
        }
        if (!harvested) {
            return false;
        }
        if (!world.isRemote) {
            boolean smoked = BeeSmoke.isSmokey(world, pos);
            if (smoked && bottle && player instanceof EntityPlayerMP) {
                Advancements.grant((EntityPlayerMP) player, "husbandry/safely_harvest_honey");
            }
            if (!smoked) {
                TileEntityBeehive hive = hive(world, pos);
                if (hive != null && !hive.isEmpty()) {
                    angerNearbyBees(world, pos);
                }
                takeHoney(world, state, pos, player, TileEntityBeehive.Release.EMERGENCY);
            } else {
                resetHoney(world, pos);
            }
        }
        return true;
    }

    /** 3 honeycombs, dropped at the hive (shears by hand or from a dispenser). */
    public static void dropHoneycomb(World world, BlockPos pos) {
        Item honeycomb = Owners.ours("honeycomb");
        if (honeycomb != null) {
            spawnAsEntity(world, pos, new ItemStack(honeycomb, 3));
        }
    }

    /** Empties the hive's honey and lets the bees out for this reason, setting them on the player if not calm. */
    public static void takeHoney(World world, IBlockState state, BlockPos pos, @Nullable EntityPlayer player, TileEntityBeehive.Release reason) {
        resetHoney(world, pos);
        TileEntityBeehive hive = hive(world, pos);
        if (hive != null) {
            hive.releaseAll(player, state, reason);
        }
    }

    public static void resetHoney(World world, BlockPos pos) {
        TileEntityBeehive hive = hive(world, pos);
        if (hive != null) {
            hive.setHoneyLevel(0);
        }
    }

    /** Every bee within 8 blocks (6 up and down) without a target turns on a random nearby player. */
    static void angerNearbyBees(World world, BlockPos pos) {
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(8.0D, 6.0D, 8.0D);
        List<EntityBee> bees = world.getEntitiesWithinAABB(EntityBee.class, box);
        if (bees.isEmpty()) {
            return;
        }
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, box);
        if (players.isEmpty()) {
            return;
        }
        for (EntityBee bee : bees) {
            if (bee.getAttackTarget() == null) {
                bee.setAttackTarget(players.get(world.rand.nextInt(players.size())));
            }
        }
    }

    // ---- breaking ----

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
        boolean silk = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;
        TileEntityBeehive hive = te instanceof TileEntityBeehive ? (TileEntityBeehive) te : null;
        if (!world.isRemote) {
            if (silk) {
                spawnAsEntity(world, pos, withContents(hive));
                if (this.nest && hive != null && hive.getBeeCount() == TileEntityBeehive.MAX_BEES && player instanceof EntityPlayerMP) {
                    Advancements.grant((EntityPlayerMP) player, "husbandry/silk_touch_nest");
                }
            } else {
                if (!this.nest) {
                    spawnAsEntity(world, pos, new ItemStack(this));
                }
                if (hive != null) {
                    hive.setWorld(world);
                    hive.releaseAll(player, state, TileEntityBeehive.Release.EMERGENCY);
                }
                angerNearbyBees(world, pos);
            }
        }
    }

    /** @return this block as an item carrying the hive's bees and honey level (Silk Touch, Creative) */
    public ItemStack withContents(@Nullable TileEntityBeehive hive) {
        ItemStack stack = new ItemStack(this);
        if (hive != null && (!hive.isEmpty() || hive.getHoneyLevel() > 0)) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("Bees", hive.writeBees());
            tag.setInteger("HoneyLevel", hive.getHoneyLevel());
            stack.setTagInfo("BlockEntityTag", tag);
        }
        return stack;
    }

    /** Creative: a hive with bees or honey drops itself with them, as 1.15. */
    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!world.isRemote && player.capabilities.isCreativeMode && world.getGameRules().getBoolean("doTileDrops")) {
            TileEntityBeehive hive = hive(world, pos);
            if (hive != null && (!hive.isEmpty() || hive.getHoneyLevel() > 0)) {
                EntityItem item = new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), withContents(hive));
                item.setDefaultPickupDelay();
                world.spawnEntity(item);
            }
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public void getDrops(net.minecraft.util.NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        // Breaking by hand drops through harvestBlock. Explosions and other removals: a hive drops itself
        // (empty), a nest nothing, as 1.15's loot tables.
        if (!this.nest) {
            drops.add(new ItemStack(this));
        }
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    /** Explosions let the bees out (1.15's TNT, creeper and wither rule). */
    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntityBeehive hive = hive(world, pos);
        if (hive != null && !world.isRemote) {
            hive.releaseAll(null, world.getBlockState(pos), TileEntityBeehive.Release.EMERGENCY);
        }
        super.onBlockExploded(world, pos, explosion);
    }

    /** Fire next to the hive throws the bees out (1.15's {@code updatePostPlacement}). */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (!world.isRemote && world.getBlockState(from).getBlock() instanceof BlockFire) {
            TileEntityBeehive hive = hive(world, pos);
            if (hive != null) {
                hive.releaseAll(null, state, TileEntityBeehive.Release.EMERGENCY);
            }
        }
    }

    // ---- redstone, pistons ----

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return honeyLevel(world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.BLOCK;
    }

    // ---- looks ----

    /** A full hive drips honey from its underside when the block below leaves room (1.15). */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (honeyLevel(world, pos) >= TileEntityBeehive.MAX_HONEY) {
            com.exiledradio.buzzybeesbackport.bee.client.BeeClient.hiveDrip(world, pos);
        }
    }
}

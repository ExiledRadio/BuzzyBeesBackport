package com.exiledradio.buzzybeesbackport.bee;

import com.exiledradio.buzzybeesbackport.api.BeeSmoke;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A bee nest's or beehive's bees, honey level and remembered flower, from 1.16.5's
 * {@code BeehiveTileEntity}.
 *
 * <p>The honey level (0-5) lives here rather than in the block's metadata, which has room only for the
 * facing; the block reads it into its {@code honey_level} property for drawing and comparators. It is synced
 * to clients with the tile's update packet.
 */
public class TileEntityBeehive extends TileEntity implements ITickable {

    public static final int MAX_BEES = 3;
    public static final int MAX_HONEY = 5;

    /** Why bees leave: honey made, done resting, or thrown out (broken, harvested, on fire). */
    public enum Release {
        HONEY_DELIVERED, BEE_RELEASED, EMERGENCY
    }

    private static final class Inhabitant {
        final NBTTagCompound data;
        int ticksInHive;
        final int minOccupationTicks;

        Inhabitant(NBTTagCompound data, int ticksInHive, int minOccupationTicks) {
            stripIdentity(data);
            this.data = data;
            this.ticksInHive = ticksInHive;
            this.minOccupationTicks = minOccupationTicks;
        }
    }

    private final List<Inhabitant> bees = new ArrayList<Inhabitant>();
    @Nullable
    private BlockPos flowerPos;
    private int honeyLevel;

    // ---- honey ----

    public int getHoneyLevel() {
        return this.honeyLevel;
    }

    /** Sets the honey level, redraws the block and tells comparators. */
    public void setHoneyLevel(int level) {
        level = Math.max(0, Math.min(MAX_HONEY, level));
        if (level == this.honeyLevel) {
            return;
        }
        this.honeyLevel = level;
        markDirty();
        if (this.world != null && !this.world.isRemote) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
            this.world.updateComparatorOutputLevel(this.pos, getBlockType());
        }
    }

    // ---- occupants ----

    public int getBeeCount() {
        return this.bees.size();
    }

    public boolean isEmpty() {
        return this.bees.isEmpty();
    }

    public boolean isFull() {
        return this.bees.size() >= MAX_BEES;
    }

    /** @return true if bees here are calm: smoke from below (see {@link BeeSmoke#isSmokey}) */
    public boolean isSedated() {
        return this.world != null && BeeSmoke.isSmokey(this.world, this.pos);
    }

    /** @return true if fire touches the hive's 3x3x3 neighborhood, which throws bees out and stops them entering */
    public boolean isNearFire() {
        if (this.world == null) {
            return false;
        }
        for (BlockPos p : BlockPos.getAllInBoxMutable(this.pos.add(-1, -1, -1), this.pos.add(1, 1, 1))) {
            if (this.world.getBlockState(p).getBlock() instanceof BlockFire) {
                return true;
            }
        }
        return false;
    }

    /** A bee going in: kept as its saved data, resting 2400 ticks with nectar or 600 without. */
    public void addBee(Entity entity, boolean hasNectar) {
        addBee(entity, hasNectar, 0);
    }

    public void addBee(Entity entity, boolean hasNectar, int ticksInHive) {
        if (isFull()) {
            return;
        }
        entity.dismountRidingEntity();
        entity.removePassengers();
        NBTTagCompound data = new NBTTagCompound();
        if (!entity.writeToNBTOptional(data)) {
            return;
        }
        this.bees.add(new Inhabitant(data, ticksInHive, hasNectar ? 2400 : 600));
        if (entity instanceof EntityBee) {
            EntityBee bee = (EntityBee) entity;
            if (bee.hasFlower() && (this.flowerPos == null || this.world.rand.nextBoolean())) {
                this.flowerPos = bee.getFlowerPos();
            }
        }
        this.world.playSound(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), BeeContent.HIVE_ENTER,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        entity.setDead();
        markDirty();
    }

    /** Adds a bee from saved data (generation, tests). */
    public void addBeeData(NBTTagCompound data, int ticksInHive, boolean hasNectar) {
        if (!isFull()) {
            this.bees.add(new Inhabitant(data, ticksInHive, hasNectar ? 2400 : 600));
            markDirty();
        }
    }

    /**
     * Lets every bee out that can leave, and sets those near the player on them unless the hive is sedated.
     * Emergencies (breaking, harvesting, fire) let them out even at night, in rain and with the front blocked.
     */
    public List<EntityBee> releaseAll(@Nullable EntityPlayer player, IBlockState state, Release reason) {
        List<EntityBee> out = new ArrayList<EntityBee>();
        Iterator<Inhabitant> it = this.bees.iterator();
        while (it.hasNext()) {
            if (release(state, it.next(), out, reason)) {
                it.remove();
            }
        }
        if (!out.isEmpty()) {
            markDirty();
        }
        if (player != null) {
            boolean sedated = isSedated();
            for (EntityBee bee : out) {
                if (player.getDistanceSq(bee) <= 16.0D) {
                    if (!sedated) {
                        bee.setAttackTarget(player);
                    } else {
                        bee.setCannotEnterHiveTicks(400);
                    }
                }
            }
        }
        return out;
    }

    /** 1.15's day-cycle test: no night in the Nether or the End, where bees work around the clock. */
    static boolean isNight(World world) {
        return world.provider.isSurfaceWorld() && !world.isDaytime();
    }

    private boolean release(IBlockState state, Inhabitant inhabitant, @Nullable List<EntityBee> out, Release reason) {
        if ((isNight(this.world) || this.world.isRaining()) && reason != Release.EMERGENCY) {
            return false;
        }
        NBTTagCompound data = inhabitant.data.copy();
        data.removeTag("Passengers");
        data.removeTag("Leash");
        stripIdentity(data);
        EnumFacing facing = state.getBlock() instanceof BlockBeehive ? state.getValue(BlockBeehive.FACING) : EnumFacing.NORTH;
        BlockPos front = this.pos.offset(facing);
        IBlockState frontState = this.world.getBlockState(front);
        AxisAlignedBB frontBox = frontState.getCollisionBoundingBox(this.world, front);
        boolean blocked = frontBox != null && frontBox != Block.NULL_AABB;
        if (blocked && reason != Release.EMERGENCY) {
            return false;
        }
        Entity entity = EntityList.createEntityFromNBT(data, this.world);
        if (!(entity instanceof EntityBee)) {
            // 1.15's beehive_inhabitors tag holds only bees; anything else stays.
            return false;
        }
        EntityBee bee = (EntityBee) entity;
        if (this.flowerPos != null && !bee.hasFlower() && this.world.rand.nextFloat() < 0.9F) {
            bee.setFlowerPos(this.flowerPos);
        }
        if (reason == Release.HONEY_DELIVERED) {
            bee.onHoneyDelivered();
            if (this.honeyLevel < MAX_HONEY) {
                int add = this.world.rand.nextInt(100) == 0 ? 2 : 1;
                setHoneyLevel(Math.min(MAX_HONEY, this.honeyLevel + add));
            }
        }
        bee.catchUpAfterHive(inhabitant.ticksInHive);
        if (out != null) {
            out.add(bee);
        }
        double out1 = blocked ? 0.0D : 0.55D + bee.width / 2.0F;
        double x = this.pos.getX() + 0.5D + out1 * facing.getXOffset();
        double y = this.pos.getY() + 0.5D - bee.height / 2.0F;
        double z = this.pos.getZ() + 0.5D + out1 * facing.getZOffset();
        bee.setLocationAndAngles(x, y, z, bee.rotationYaw, bee.rotationPitch);
        this.world.playSound(null, this.pos, BeeContent.HIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return this.world.spawnEntity(bee);
    }

    private static void stripIdentity(NBTTagCompound data) {
        data.removeTag("UUIDMost");
        data.removeTag("UUIDLeast");
        data.removeTag("UUID");
    }

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }
        IBlockState state = this.world.getBlockState(this.pos);
        Iterator<Inhabitant> it = this.bees.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Inhabitant bee = it.next();
            if (bee.ticksInHive > bee.minOccupationTicks) {
                Release reason = bee.data.getBoolean("HasNectar") ? Release.HONEY_DELIVERED : Release.BEE_RELEASED;
                if (release(state, bee, null, reason)) {
                    it.remove();
                    changed = true;
                    continue;
                }
            }
            bee.ticksInHive++;
        }
        if (changed) {
            markDirty();
        }
        if (!this.bees.isEmpty() && this.world.rand.nextDouble() < 0.005D) {
            this.world.playSound(null, this.pos.getX() + 0.5D, this.pos.getY(), this.pos.getZ() + 0.5D, BeeContent.HIVE_WORK,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    // ---- index ----

    @Override
    public void onLoad() {
        if (this.world != null && !this.world.isRemote) {
            HiveIndex.add(this.world, this.pos);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (this.world != null && !this.world.isRemote) {
            HiveIndex.remove(this.world, this.pos);
        }
    }

    @Override
    public void onChunkUnload() {
        if (this.world != null && !this.world.isRemote) {
            HiveIndex.remove(this.world, this.pos);
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // ---- saving and syncing ----

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.bees.clear();
        NBTTagList list = tag.getTagList("Bees", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            this.bees.add(new Inhabitant(entry.getCompoundTag("EntityData"), entry.getInteger("TicksInHive"),
                    entry.getInteger("MinOccupationTicks")));
        }
        this.flowerPos = tag.hasKey("FlowerPos", 10) ? NBTUtil.getPosFromTag(tag.getCompoundTag("FlowerPos")) : null;
        this.honeyLevel = Math.max(0, Math.min(MAX_HONEY, tag.getInteger("HoneyLevel")));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Bees", writeBees());
        if (this.flowerPos != null) {
            tag.setTag("FlowerPos", NBTUtil.createPosTag(this.flowerPos));
        }
        tag.setInteger("HoneyLevel", this.honeyLevel);
        return tag;
    }

    /** @return the bees as 1.15 saves them: {@code EntityData}, {@code TicksInHive}, {@code MinOccupationTicks} */
    public NBTTagList writeBees() {
        NBTTagList list = new NBTTagList();
        for (Inhabitant bee : this.bees) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("EntityData", bee.data.copy());
            entry.setInteger("TicksInHive", bee.ticksInHive);
            entry.setInteger("MinOccupationTicks", bee.minOccupationTicks);
            list.appendTag(entry);
        }
        return list;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setInteger("HoneyLevel", this.honeyLevel);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.honeyLevel = tag.getInteger("HoneyLevel");
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.honeyLevel = packet.getNbtCompound().getInteger("HoneyLevel");
        if (this.world != null) {
            this.world.markBlockRangeForRenderUpdate(this.pos, this.pos);
        }
    }
}

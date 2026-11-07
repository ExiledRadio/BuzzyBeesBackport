package com.exiledradio.buzzybeesbackport.selftest;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.backportcore.migrate.FutureMcConvert;
import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.buzzybeesbackport.bee.BeeContent;
import com.exiledradio.buzzybeesbackport.bee.BlockBeehive;
import com.exiledradio.buzzybeesbackport.bee.EntityBee;
import com.exiledradio.buzzybeesbackport.bee.TileEntityBeehive;
import com.exiledradio.buzzybeesbackport.migrate.FutureMcBees;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/** Stream F: the other bee mod's blocks, hives and bees becoming ours. */
public final class FutureMcSelfTest implements SelfTest {

    @Override
    public String name() {
        return "futuremc";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(BeeContent.on(), "bee stream on")) {
            return;
        }
        // The recorded metadata table, and what each value turns into.
        String[] facings = {"south", "west", "north", "east"};
        for (int meta = 0; meta < 8; meta++) {
            Map<String, String> props = FutureMcTable.recordedState("beehive", meta);
            c.ok(props != null && facings[meta & 3].equals(props.get("facing")) && String.valueOf(meta >= 4).equals(props.get("full")),
                    "their beehive meta " + meta + " recorded: " + props);
            FutureMcTable.Target target = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, "beehive"));
            c.ok(target != null && target.block == BeeContent.beehive, "their beehive becomes ours");
            if (target != null && props != null) {
                FutureMcConvert.Converted converted = FutureMcConvert.state(target, props);
                c.eq(EnumFacing.byName(facings[meta & 3]), converted.state.getValue(BlockBeehive.FACING), "meta " + meta + " keeps its facing");
            }
        }
        for (String name : new String[] {"bee_nest", "honey_block", "honeycomb_block"}) {
            FutureMcTable.Target target = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, name));
            c.ok(target != null && name.equals(target.block.getRegistryName().getPath())
                    && "buzzybeesbackport".equals(target.block.getRegistryName().getNamespace()), "their " + name + " becomes ours");
        }
        for (String item : new String[] {"honey_bottle", "honeycomb"}) {
            FutureMcTable.ItemTarget t = FutureMcTable.item(new ResourceLocation(FutureMcTable.FUTURE_MC, item), 0);
            c.ok(t != null && t.item.getRegistryName() != null && "buzzybeesbackport".equals(t.item.getRegistryName().getNamespace()),
                    "their " + item + " becomes ours");
        }
        c.ok(!DuplicateBlocks.isHidden(BeeContent.beehive), "without the other mod nothing of ours is hidden");

        // Their hive's saved data: two bees, full of honey, a flower.
        BlockPos base = c.area();
        BlockPos pos = base.add(8, 2, 8);
        c.world.setBlockState(pos, BeeContent.beehive.getDefaultState().withProperty(BlockBeehive.FACING, EnumFacing.SOUTH), 3);
        TileEntityBeehive ours = (TileEntityBeehive) c.world.getTileEntity(pos);
        NBTTagCompound theirs = new NBTTagCompound();
        theirs.setString("id", "futuremc:beehive");
        NBTTagList bees = new NBTTagList();
        for (int i = 0; i < 2; i++) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("id", "futuremc:bee");
            data.setFloat("Health", 8.0F);
            data.setBoolean("HasNectar", i == 0);
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("EntityData", data);
            entry.setInteger("TicksInHive", 100);
            entry.setInteger("MinOccupationTicks", 600);
            bees.appendTag(entry);
        }
        theirs.setTag("Bees", bees);
        theirs.setInteger("HoneyLevel", 5);
        theirs.setTag("FlowerPos", NBTUtil.createPosTag(pos.up(3)));
        FutureMcBees.fillHive(ours, theirs);
        c.eq(2, ours.getBeeCount(), "their hive's 2 bees move in");
        c.eq(5, ours.getHoneyLevel(), "and its honey");
        c.world.setWorldTime(1000L);
        c.world.calculateInitialSkylight();
        java.util.List<EntityBee> out = ours.releaseAll(null, c.world.getBlockState(pos), TileEntityBeehive.Release.EMERGENCY);
        c.eq(2, out.size(), "and come out as our bees");
        for (EntityBee b : out) {
            c.eq(8.0F, b.getHealth(), "keeping their health");
            b.setDead();
        }
        // A free-flying bee of theirs.
        NBTTagCompound free = new NBTTagCompound();
        free.setString("id", "futuremc:bee");
        free.setFloat("Health", 6.0F);
        net.minecraft.nbt.NBTTagList posList = new net.minecraft.nbt.NBTTagList();
        posList.appendTag(new net.minecraft.nbt.NBTTagDouble(pos.getX() + 0.5));
        posList.appendTag(new net.minecraft.nbt.NBTTagDouble(pos.getY() + 3));
        posList.appendTag(new net.minecraft.nbt.NBTTagDouble(pos.getZ() + 0.5));
        free.setTag("Pos", posList);
        try {
            java.lang.reflect.Method m = FutureMcBees.class.getDeclaredMethod("asOurBee", NBTTagCompound.class);
            m.setAccessible(true);
            Entity bee = EntityList.createEntityFromNBT((NBTTagCompound) m.invoke(null, free), c.world);
            c.ok(bee instanceof EntityBee && ((EntityBee) bee).getHealth() == 6.0F, "a free bee of theirs loads as ours");
        } catch (ReflectiveOperationException e) {
            c.fail("asOurBee: " + e);
        }
    }
}

package com.exiledradio.buzzybeesbackport.migrate;

import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.exiledradio.buzzybeesbackport.bee.TileEntityBeehive;
import com.exiledradio.buzzybeesbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Carries bees and honey across when a world the other bee mod was removed from loads.
 *
 * <p>Backport Core turns that mod's nests and hives into ours and gives them fresh tile entities, but it
 * only moves items. Its hives save bees almost as ours do ({@code Bees} with {@code EntityData},
 * {@code TicksInHive}, {@code MinOccupationTicks}; {@code HoneyLevel}; {@code FlowerPos}), so after Core's
 * pass this copies them into our tile entity, the bees renamed to ours. Free-flying bees of that mod, which
 * vanilla drops on loading because nothing can read them, are respawned as ours at the end of the tick.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class FutureMcBees {

    private static final String THEIR_BEE = FutureMcTable.FUTURE_MC + ":bee";
    private static final Map<World, List<NBTTagCompound>> SPAWN = Collections.synchronizedMap(new WeakHashMap<World, List<NBTTagCompound>>());

    private FutureMcBees() {
    }

    private static boolean active() {
        return ModConfig.compat.convertRemoved && !Loader.isModLoaded(FutureMcTable.FUTURE_MC);
    }

    /** After Backport Core's conversion of the same chunk (it runs at normal priority). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void loaded(ChunkDataEvent.Load event) {
        if (!active() || event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        NBTTagCompound level = event.getData().getCompoundTag("Level");
        Chunk chunk = event.getChunk();
        NBTTagList tiles = level.getTagList("TileEntities", 10);
        for (int i = 0; i < tiles.tagCount(); i++) {
            NBTTagCompound tile = tiles.getCompoundTagAt(i);
            if (!tile.getString("id").startsWith(FutureMcTable.FUTURE_MC + ":")) {
                continue;
            }
            BlockPos pos = new BlockPos(tile.getInteger("x"), tile.getInteger("y"), tile.getInteger("z"));
            TileEntity ours = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
            if (ours instanceof TileEntityBeehive) {
                fillHive((TileEntityBeehive) ours, tile);
                chunk.markDirty();
            }
        }
        NBTTagList entities = level.getTagList("Entities", 10);
        List<NBTTagCompound> bees = new ArrayList<NBTTagCompound>();
        for (int i = 0; i < entities.tagCount(); i++) {
            NBTTagCompound e = entities.getCompoundTagAt(i);
            if (THEIR_BEE.equals(e.getString("id"))) {
                bees.add(asOurBee(e));
            }
        }
        if (!bees.isEmpty()) {
            List<NBTTagCompound> list = SPAWN.get(event.getWorld());
            if (list == null) {
                list = new ArrayList<NBTTagCompound>();
                SPAWN.put(event.getWorld(), list);
            }
            list.addAll(bees);
            chunk.markDirty();
        }
    }

    /** Copies their hive's bees, honey and flower into ours. */
    public static void fillHive(TileEntityBeehive ours, NBTTagCompound theirs) {
        NBTTagCompound tag = ours.writeToNBT(new NBTTagCompound());
        NBTTagList bees = new NBTTagList();
        NBTTagList list = theirs.getTagList("Bees", 10);
        for (int i = 0; i < list.tagCount() && i < TileEntityBeehive.MAX_BEES; i++) {
            NBTTagCompound bee = list.getCompoundTagAt(i).copy();
            bee.setTag("EntityData", asOurBee(bee.getCompoundTag("EntityData")));
            bees.appendTag(bee);
        }
        tag.setTag("Bees", bees);
        tag.setInteger("HoneyLevel", Math.max(0, Math.min(TileEntityBeehive.MAX_HONEY, theirs.getInteger("HoneyLevel"))));
        if (theirs.hasKey("FlowerPos", 10)) {
            tag.setTag("FlowerPos", theirs.getCompoundTag("FlowerPos"));
        }
        ours.readFromNBT(tag);
    }

    /** Their bee's saved data as ours: the same vanilla fields and 1.15's own names for the rest. */
    static NBTTagCompound asOurBee(NBTTagCompound theirs) {
        NBTTagCompound bee = theirs.copy();
        bee.setString("id", Content.id("bee").toString());
        return bee;
    }

    @SubscribeEvent
    public static void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        List<NBTTagCompound> due = SPAWN.remove(event.world);
        if (due == null) {
            return;
        }
        for (NBTTagCompound tag : due) {
            Entity bee = EntityList.createEntityFromNBT(tag, event.world);
            if (bee != null) {
                event.world.spawnEntity(bee);
            }
        }
        BuzzyBeesBackport.LOGGER.info("Moved {} bees over from a mod no longer installed.", due.size());
    }
}

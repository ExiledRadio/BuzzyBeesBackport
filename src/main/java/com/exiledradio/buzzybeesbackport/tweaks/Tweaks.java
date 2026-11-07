package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBoat;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Stream X: 1.15's smaller changes to blocks, items and mobs, each behind its own {@code tweaks} switch.
 * The player-facing rules (beds, game rules, Creative eating) are in {@link PlayerTweaks}, the dispensers in
 * {@link DispenserTweaks}, the bell in {@link BellRedstone}. The ones that need a bytecode patch
 * ({@code docs/PATCHES.md}, P2-P5) have their hooks in {@link CraftingShift}, {@link ElytraStart},
 * {@link DispensedFireworks} and {@link ShoulderParrots}; the advancements are {@link AdvancementTweaks}.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class Tweaks {

    /** Worlds in the middle of populating a chunk: animals joining then are its generation spawns. */
    private static final Set<World> POPULATING = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<World, Boolean>()));

    private Tweaks() {
    }

    static boolean on() {
        return ModConfig.tweaks.enabled;
    }

    /** From the mod's init, after every registry (dispenser behaviors wrap the bee stream's). */
    public static void init() {
        if (!on()) {
            return;
        }
        if (ModConfig.tweaks.endStoneBricks) {
            // 1.15: hardness 3, blast resistance 9. 1.12.2 keeps resistance x3 and divides by 5 when read.
            Blocks.END_BRICKS.setHardness(3.0F).setResistance(15.0F);
        }
        if (ModConfig.tweaks.parrotImitation) {
            removeImitations();
        }
        if (ModConfig.tweaks.dispensers) {
            DispenserTweaks.register();
        }
        if (ModConfig.tweaks.dispensedFireworks) {
            DispensedFireworks.register();
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeImitations() {
        try {
            Map<Class<? extends Entity>, ?> sounds = (Map<Class<? extends Entity>, ?>) ReflectionHelper.getPrivateValue(
                    EntityParrot.class, null, "MIMIC_SOUNDS");
            sounds.remove(EntityEnderman.class);
            sounds.remove(EntityPolarBear.class);
            sounds.remove(EntityWolf.class);
            sounds.remove(EntityPigZombie.class);
        } catch (RuntimeException e) {
            BuzzyBeesBackport.LOGGER.warn("Could not change what parrots imitate.", e);
        }
    }

    @SubscribeEvent
    public static void fuel(FurnaceFuelBurnTimeEvent event) {
        if (on() && ModConfig.tweaks.boatFuel && event.getItemStack().getItem() instanceof ItemBoat) {
            event.setBurnTime(1200);
        }
    }

    /** Wet sponge in the Nether: dry at once, with a hiss and a puff (1.15's {@code WetSpongeBlock}). */
    @SubscribeEvent
    public static void placed(BlockEvent.PlaceEvent event) {
        if (!on()) {
            return;
        }
        World world = event.getWorld();
        IBlockState state = event.getPlacedBlock();
        BlockPos pos = event.getPos();
        if (ModConfig.tweaks.spongeDries && state.getBlock() == Blocks.SPONGE && state.getValue(BlockSponge.WET)
                && world.provider.doesWaterVaporize()) {
            world.setBlockState(pos, state.withProperty(BlockSponge.WET, false), 3);
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, (1.0F + world.rand.nextFloat() * 0.2F) * 0.7F);
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        12, 0.4, 0.4, 0.4, 0.0);
            }
        }
        if (ModConfig.tweaks.railFacing && state.getBlock() instanceof BlockRailBase && event.getPlayer() != null && !world.isRemote) {
            // After the event Forge runs the rail's own onBlockAdded, which picks north-south when nothing
            // connects; so the facing is applied at the end of the tick.
            synchronized (RAILS) {
                RAILS.add(new Object[] {world, pos.toImmutable(), event.getPlayer().getHorizontalFacing()});
            }
        }
    }

    private static final java.util.List<Object[]> RAILS = new java.util.ArrayList<Object[]>();

    @SubscribeEvent
    public static void worldTick(net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent event) {
        if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END && !event.world.isRemote) {
            facePendingRails(event.world);
        }
    }

    /** Turns the rails placed in this world this tick (also called by the self-test). */
    public static void facePendingRails(World world) {
        java.util.List<Object[]> mine = new java.util.ArrayList<Object[]>();
        synchronized (RAILS) {
            java.util.Iterator<Object[]> it = RAILS.iterator();
            while (it.hasNext()) {
                Object[] r = it.next();
                if (r[0] == world) {
                    mine.add(r);
                    it.remove();
                }
            }
        }
        for (Object[] r : mine) {
            faceRail(world, (BlockPos) r[1], (EnumFacing) r[2]);
        }
    }

    /** A straight rail that connected to nothing turns to run the way the player looked (1.15). */
    static void faceRail(World world, BlockPos pos, EnumFacing facing) {
        IBlockState now = world.getBlockState(pos);
        if (!(now.getBlock() instanceof BlockRailBase)) {
            return;
        }
        BlockRailBase rail = (BlockRailBase) now.getBlock();
        BlockRailBase.EnumRailDirection dir = now.getValue(rail.getShapeProperty());
        if (dir != BlockRailBase.EnumRailDirection.NORTH_SOUTH || facing.getAxis() != EnumFacing.Axis.X) {
            return;
        }
        for (EnumFacing side : new EnumFacing[] {EnumFacing.NORTH, EnumFacing.SOUTH}) {
            BlockPos n = pos.offset(side);
            if (BlockRailBase.isRailBlock(world, n) || BlockRailBase.isRailBlock(world, n.up()) || BlockRailBase.isRailBlock(world, n.down())) {
                return;
            }
        }
        if (rail.getShapeProperty().getAllowedValues().contains(BlockRailBase.EnumRailDirection.EAST_WEST)) {
            world.setBlockState(pos, now.withProperty(rail.getShapeProperty(), BlockRailBase.EnumRailDirection.EAST_WEST), 2);
        }
    }

    /** Large ferns drop wheat seeds 1 in 8, like tall grass (1.15). */
    @SubscribeEvent
    public static void drops(BlockEvent.HarvestDropsEvent event) {
        if (!on() || !ModConfig.tweaks.fernSeeds || event.isSilkTouching()) {
            return;
        }
        IBlockState state = event.getState();
        if (state.getBlock() == Blocks.DOUBLE_PLANT && state.getValue(BlockDoublePlant.VARIANT) == BlockDoublePlant.EnumPlantType.FERN
                && state.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.LOWER
                && event.getDrops().isEmpty() && event.getWorld().rand.nextInt(8) == 0) {
            event.getDrops().add(new ItemStack(Items.WHEAT_SEEDS));
        }
    }

    /** A zombie-kind's spawn egg on an adult of its kind makes a baby (1.15). */
    @SubscribeEvent
    public static void spawnEgg(PlayerInteractEvent.EntityInteract event) {
        if (!on() || !ModConfig.tweaks.spawnEggBabies || !(event.getTarget() instanceof EntityZombie)) {
            return;
        }
        ItemStack egg = event.getItemStack();
        if (!(egg.getItem() instanceof ItemMonsterPlacer)) {
            return;
        }
        EntityZombie adult = (EntityZombie) event.getTarget();
        ResourceLocation eggId = ItemMonsterPlacer.getNamedIdFrom(egg);
        if (adult.isChild() || eggId == null || !eggId.equals(EntityList.getKey(adult))) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        Entity made = EntityList.createEntityByIDFromName(eggId, world);
        if (made instanceof EntityZombie) {
            EntityZombie baby = (EntityZombie) made;
            baby.setChild(true);
            baby.setLocationAndAngles(adult.posX, adult.posY, adult.posZ, 0.0F, 0.0F);
            if (egg.hasDisplayName()) {
                baby.setCustomNameTag(egg.getDisplayName());
            }
            world.spawnEntity(baby);
            if (!event.getEntityPlayer().capabilities.isCreativeMode) {
                egg.shrink(1);
            }
        }
    }

    @SubscribeEvent
    public static void populating(PopulateChunkEvent.Pre event) {
        POPULATING.add(event.getWorld());
    }

    @SubscribeEvent
    public static void populated(PopulateChunkEvent.Post event) {
        POPULATING.remove(event.getWorld());
    }

    /** Animals spawned with a new chunk are sometimes babies (1.15's group spawns): 5% or 10% by kind. */
    @SubscribeEvent
    public static void joined(EntityJoinWorldEvent event) {
        if (!on() || !ModConfig.tweaks.babyAnimals || event.getWorld().isRemote || !POPULATING.contains(event.getWorld())) {
            return;
        }
        if (!(event.getEntity() instanceof EntityAgeable)) {
            return;
        }
        EntityAgeable animal = (EntityAgeable) event.getEntity();
        float chance = babyChance(animal);
        if (chance > 0.0F && animal.ticksExisted == 0 && !animal.isChild() && event.getWorld().rand.nextFloat() < chance) {
            animal.setGrowingAge(-24000);
        }
    }

    static float babyChance(Entity animal) {
        if (animal instanceof EntityWolf || animal instanceof AbstractHorse || animal instanceof EntityLlama) {
            return 0.10F;
        }
        if (animal instanceof EntityCow || animal instanceof EntitySheep || animal instanceof EntityPig || animal instanceof EntityChicken) {
            return 0.05F;
        }
        ResourceLocation id = EntityList.getKey(animal);
        return id != null && "villagerbackport:fox".equals(id.toString()) ? 0.05F : 0.0F;
    }
}

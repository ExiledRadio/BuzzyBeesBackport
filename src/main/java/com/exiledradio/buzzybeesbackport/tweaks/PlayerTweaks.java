package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;

import net.minecraft.block.BlockCake;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.15's player-side changes: beds setting the respawn point when used, eating in Creative, and the five
 * game rules 1.15 added.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class PlayerTweaks {

    public static final String FALL_DAMAGE = "fallDamage";
    public static final String FIRE_DAMAGE = "fireDamage";
    public static final String DROWNING_DAMAGE = "drowningDamage";
    public static final String IMMEDIATE_RESPAWN = "doImmediateRespawn";
    public static final String INSOMNIA = "doInsomnia";

    /** Players to respawn next tick (doImmediateRespawn). */
    private static final List<UUID> RESPAWN = new ArrayList<UUID>();

    private PlayerTweaks() {
    }

    // ---- beds ----

    /**
     * 1.15's {@code trySleep}: in the Overworld, with the bed in reach, the respawn point is set (and said)
     * before it checks the time of day and for monsters, so trying to sleep by day still sets it.
     */
    @SubscribeEvent
    public static void sleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!Tweaks.on() || !ModConfig.tweaks.bedSpawn || player.world.isRemote || player.isPlayerSleeping() || !player.isEntityAlive()
                || !player.world.provider.isSurfaceWorld()) {
            return;
        }
        BlockPos bed = event.getPos();
        IBlockState state = player.world.getBlockState(bed);
        EnumFacing facing = state.getBlock() instanceof BlockHorizontal ? state.getValue(BlockHorizontal.FACING) : null;
        if (!inReach(player, bed) && (facing == null || !inReach(player, bed.offset(facing.getOpposite())))) {
            return;
        }
        setSpawn(player, bed);
    }

    private static boolean inReach(EntityPlayer player, BlockPos pos) {
        return Math.abs(player.posX - pos.getX()) <= 3.0D && Math.abs(player.posY - pos.getY()) <= 2.0D
                && Math.abs(player.posZ - pos.getZ()) <= 3.0D;
    }

    /** Sets the bed as the respawn point; "Respawn point set" only when it changed. */
    static void setSpawn(EntityPlayer player, BlockPos bed) {
        BlockPos old = player.getBedLocation(player.dimension);
        if (bed.equals(old) && !player.isSpawnForced(player.dimension)) {
            return;
        }
        player.setSpawnPoint(bed, false);
        player.sendMessage(new TextComponentTranslation("block.buzzybeesbackport.set_spawn"));
    }

    // ---- eating in Creative ----

    @SubscribeEvent
    public static void eat(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();
        if (Tweaks.on() && ModConfig.tweaks.creativeEating && player.capabilities.isCreativeMode
                && stack.getItem() instanceof ItemFood && !player.canEat(false)) {
            player.setActiveHand(event.getHand());
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void cake(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        if (!Tweaks.on() || !ModConfig.tweaks.creativeEating || !player.capabilities.isCreativeMode
                || !(state.getBlock() instanceof BlockCake) || player.canEat(false)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) {
            return;
        }
        // 1.12.2's own slice, without the hunger check it cannot pass in Creative.
        player.addStat(StatList.CAKE_SLICES_EATEN);
        player.getFoodStats().addStats(2, 0.1F);
        int bites = state.getValue(BlockCake.BITES);
        if (bites < 6) {
            world.setBlockState(event.getPos(), state.withProperty(BlockCake.BITES, bites + 1), 3);
        } else {
            world.setBlockToAir(event.getPos());
        }
    }

    // ---- game rules ----

    @SubscribeEvent
    public static void load(WorldEvent.Load event) {
        if (!Tweaks.on() || !ModConfig.tweaks.gameRules || event.getWorld().isRemote) {
            return;
        }
        GameRules rules = event.getWorld().getGameRules();
        for (String rule : new String[] {FALL_DAMAGE, FIRE_DAMAGE, DROWNING_DAMAGE, INSOMNIA}) {
            if (!rules.hasRule(rule)) {
                rules.addGameRule(rule, "true", GameRules.ValueType.BOOLEAN_VALUE);
            }
        }
        if (!rules.hasRule(IMMEDIATE_RESPAWN)) {
            rules.addGameRule(IMMEDIATE_RESPAWN, "false", GameRules.ValueType.BOOLEAN_VALUE);
        }
    }

    static boolean rule(World world, String rule) {
        GameRules rules = world.getGameRules();
        return !rules.hasRule(rule) || rules.getBoolean(rule);
    }

    /** The damage rules apply to players, as 1.15's. */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent event) {
        if (!Tweaks.on() || !ModConfig.tweaks.gameRules || !(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        World world = event.getEntityLiving().world;
        DamageSource source = event.getSource();
        if (source == DamageSource.FALL && !rule(world, FALL_DAMAGE)
                || source.isFireDamage() && !rule(world, FIRE_DAMAGE)
                || source == DamageSource.DROWN && !rule(world, DROWNING_DAMAGE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (Tweaks.on() && ModConfig.tweaks.gameRules && event.getEntityLiving() instanceof EntityPlayerMP
                && event.getEntityLiving().world.getGameRules().hasRule(IMMEDIATE_RESPAWN)
                && event.getEntityLiving().world.getGameRules().getBoolean(IMMEDIATE_RESPAWN)) {
            synchronized (RESPAWN) {
                RESPAWN.add(event.getEntityLiving().getUniqueID());
            }
        }
    }

    /** doImmediateRespawn: the server respawns the player as if they had pressed Respawn. */
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        List<UUID> due;
        synchronized (RESPAWN) {
            if (RESPAWN.isEmpty()) {
                return;
            }
            due = new ArrayList<UUID>(RESPAWN);
            RESPAWN.clear();
        }
        for (UUID id : due) {
            EntityPlayerMP player = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(id);
            if (player != null && player.getHealth() <= 0.0F && player.connection != null) {
                player.connection.processClientStatus(new CPacketClientStatus(CPacketClientStatus.State.PERFORM_RESPAWN));
            }
        }
    }

    private static final ResourceLocation PHANTOM = new ResourceLocation("updateaquaticbackport", "phantom");

    /** doInsomnia off: no phantoms (Update Aquatic's, when its spawner goes through Forge's spawn check). */
    @SubscribeEvent
    public static void spawn(LivingSpawnEvent.CheckSpawn event) {
        if (Tweaks.on() && ModConfig.tweaks.gameRules && PHANTOM.equals(net.minecraft.entity.EntityList.getKey(event.getEntity()))
                && !rule(event.getWorld(), INSOMNIA)) {
            event.setResult(Event.Result.DENY);
        }
    }
}

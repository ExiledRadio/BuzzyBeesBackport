package com.exiledradio.buzzybeesbackport.bee;

import com.exiledradio.buzzybeesbackport.api.BeeFlowers;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The bee, from 1.16.5's {@code BeeEntity} (1.15's bee with 1.16's neutral-mob anger), rewritten for
 * 1.12.2's AI.
 *
 * <h2>Goals, by priority</h2>
 * 0 sting, 1 enter the hive, 2 breed, 3 follow a flower, 4 pollinate, 5 follow the parent, find a hive,
 * go to the hive, 6 go back to a remembered flower, 7 grow crops, 8 wander, 9 swim. Targets: 1 revenge
 * (calling hive mates), 2 the player it is angry at, 3 forget a dead target.
 *
 * <p>Timers and bookkeeping run on the entity tick, never inside a goal's {@code shouldExecute} (a goal
 * behind another holding the movement bit is never asked). Goal lists are filled once, in the constructor.
 */
public class EntityBee extends EntityAnimal {

    private static final DataParameter<Byte> FLAGS = EntityDataManager.createKey(EntityBee.class, DataSerializers.BYTE);
    private static final DataParameter<Integer> ANGER = EntityDataManager.createKey(EntityBee.class, DataSerializers.VARINT);

    private static final int FLAG_ROLL = 2;
    private static final int FLAG_HAS_STUNG = 4;
    private static final int FLAG_NECTAR = 8;

    /** Anger lasts a random 20-39 seconds (1.16's neutral mobs). */
    public static final int ANGER_MIN = 400;
    public static final int ANGER_MAX = 780;

    @Nullable
    private UUID angerTarget;
    private float rollAmount;
    private float rollAmountO;
    private int timeSinceSting;
    int ticksWithoutNectarSinceExitingHive;
    private int stayOutOfHiveCountdown;
    private int numCropsGrownSincePollination;
    int remainingCooldownBeforeLocatingNewHive;
    int remainingCooldownBeforeLocatingNewFlower;
    @Nullable
    BlockPos savedFlowerPos;
    @Nullable
    BlockPos hivePos;
    private int underWaterTicks;

    final PollinateGoal pollinateGoal;
    final GoToHiveGoal goToHiveGoal;

    public EntityBee(World world) {
        super(world);
        setSize(0.7F, 0.6F);
        this.moveHelper = new BeeFlyHelper(this);
        // lookHelper is private in 1.12.2's EntityLiving.
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(EntityLiving.class, this,
                new BeeLookHelper(this), "lookHelper", "field_70749_g");
        setPathPriority(PathNodeType.DANGER_FIRE, -1.0F);
        setPathPriority(PathNodeType.WATER, -1.0F);
        setPathPriority(PathNodeType.FENCE, -1.0F);
        this.pollinateGoal = new PollinateGoal();
        this.goToHiveGoal = new GoToHiveGoal();
        this.tasks.addTask(0, new StingGoal(this));
        this.tasks.addTask(1, new EnterHiveGoal());
        this.tasks.addTask(2, new EntityAIMate(this, 1.0D));
        this.tasks.addTask(3, new FlowerTemptGoal(this));
        this.tasks.addTask(4, this.pollinateGoal);
        this.tasks.addTask(5, new EntityAIFollowParent(this, 1.25D));
        this.tasks.addTask(5, new LocateHiveGoal());
        this.tasks.addTask(5, this.goToHiveGoal);
        this.tasks.addTask(6, new GoToKnownFlowerGoal());
        this.tasks.addTask(7, new GrowCropGoal());
        this.tasks.addTask(8, new WanderGoal());
        this.tasks.addTask(9, new EntityAISwimming(this));
        this.targetTasks.addTask(1, new HurtByTargetGoal(this));
        this.targetTasks.addTask(2, new AngryAtPlayerGoal(this));
        this.targetTasks.addTask(3, new ForgetDeadTargetGoal(this));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(FLAGS, (byte) 0);
        this.dataManager.register(ANGER, 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue(0.6D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        PathNavigateFlying nav = new PathNavigateFlying(this, world) {
            @Override
            public boolean canEntityStandOnPos(BlockPos pos) {
                return !this.world.isAirBlock(pos.down());
            }

            @Override
            public void onUpdateNavigation() {
                if (!EntityBee.this.pollinateGoal.isPollinating()) {
                    super.onUpdateNavigation();
                }
            }
        };
        nav.setCanOpenDoors(false);
        nav.setCanFloat(false);
        nav.setCanEnterDoors(true);
        return nav;
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        return this.world.isAirBlock(pos) ? 10.0F : 0.0F;
    }

    // ---- flags ----

    private boolean beeFlag(int bit) {
        return (this.dataManager.get(FLAGS) & bit) != 0;
    }

    private void setBeeFlag(int bit, boolean on) {
        byte b = this.dataManager.get(FLAGS);
        this.dataManager.set(FLAGS, (byte) (on ? b | bit : b & ~bit));
    }

    public boolean hasNectar() {
        return beeFlag(FLAG_NECTAR);
    }

    public void setHasNectar(boolean nectar) {
        if (nectar) {
            this.ticksWithoutNectarSinceExitingHive = 0;
        }
        setBeeFlag(FLAG_NECTAR, nectar);
    }

    public boolean hasStung() {
        return beeFlag(FLAG_HAS_STUNG);
    }

    public void setHasStung(boolean stung) {
        setBeeFlag(FLAG_HAS_STUNG, stung);
    }

    private boolean isRolling() {
        return beeFlag(FLAG_ROLL);
    }

    // ---- anger ----

    public int getAngerTime() {
        return this.dataManager.get(ANGER);
    }

    public void setAngerTime(int ticks) {
        this.dataManager.set(ANGER, ticks);
    }

    public boolean isAngry() {
        return getAngerTime() > 0;
    }

    @Nullable
    public UUID getAngerTarget() {
        return this.angerTarget;
    }

    public void startAngerTimer() {
        setAngerTime(ANGER_MIN + this.rand.nextInt(ANGER_MAX - ANGER_MIN + 1));
    }

    /** 1.16's {@code resetTargets}: calm down completely. */
    public void stopBeingAngry() {
        setRevengeTarget(null);
        this.angerTarget = null;
        setAttackTarget(null);
        setAngerTime(0);
    }

    /** 1.16's {@code IAngerable.updateAnger}, once a tick on the server. */
    private void updateAnger() {
        EntityLivingBase target = getAttackTarget();
        if ((target == null || !target.isEntityAlive()) && this.angerTarget != null) {
            Entity was = ((net.minecraft.world.WorldServer) this.world).getEntityFromUuid(this.angerTarget);
            if (was instanceof EntityLiving) {
                stopBeingAngry();
                return;
            }
        }
        if (target != null && !target.getUniqueID().equals(this.angerTarget)) {
            this.angerTarget = target.getUniqueID();
            startAngerTimer();
        }
        if (getAngerTime() > 0) {
            setAngerTime(getAngerTime() - 1);
            if (getAngerTime() == 0) {
                stopBeingAngry();
            }
        }
    }

    boolean shouldAttack(EntityLivingBase target) {
        return target.getUniqueID().equals(this.angerTarget);
    }

    // ---- hive and flower ----

    @Nullable
    public BlockPos getHivePos() {
        return this.hivePos;
    }

    public boolean hasHive() {
        return this.hivePos != null;
    }

    public void setHivePos(@Nullable BlockPos pos) {
        this.hivePos = pos;
    }

    @Nullable
    public BlockPos getFlowerPos() {
        return this.savedFlowerPos;
    }

    public boolean hasFlower() {
        return this.savedFlowerPos != null;
    }

    public void setFlowerPos(@Nullable BlockPos pos) {
        this.savedFlowerPos = pos;
    }

    public void setCannotEnterHiveTicks(int ticks) {
        this.stayOutOfHiveCountdown = ticks;
    }

    public int getCropsGrownSincePollination() {
        return this.numCropsGrownSincePollination;
    }

    /** Leaving the hive with honey made: the nectar is gone. */
    public void onHoneyDelivered() {
        setHasNectar(false);
        this.numCropsGrownSincePollination = 0;
    }

    /** Time spent in the hive counts toward growing up and the breeding cooldown (1.15). */
    public void catchUpAfterHive(int ticksInHive) {
        int age = getGrowingAge();
        if (age < 0) {
            setGrowingAge(Math.min(0, age + ticksInHive));
        } else if (age > 0) {
            setGrowingAge(Math.max(0, age - ticksInHive));
        }
        this.ticksWithoutNectarSinceExitingHive = 0;
    }

    boolean isHiveValid() {
        if (this.hivePos == null) {
            return false;
        }
        TileEntity te = this.world.getTileEntity(this.hivePos);
        return te instanceof TileEntityBeehive;
    }

    private boolean isHiveNearFire() {
        if (this.hivePos == null) {
            return false;
        }
        TileEntity te = this.world.getTileEntity(this.hivePos);
        return te instanceof TileEntityBeehive && ((TileEntityBeehive) te).isNearFire();
    }

    /** 1.15's {@code canEnterHive}: tired, rain, night, or carrying nectar - unless kept out, busy or angry. */
    boolean wantsToEnterHive() {
        if (this.stayOutOfHiveCountdown > 0 || this.pollinateGoal.isPollinating() || hasStung() || getAttackTarget() != null) {
            return false;
        }
        boolean time = this.ticksWithoutNectarSinceExitingHive > 3600 || this.world.isRaining()
                || TileEntityBeehive.isNight(this.world) || hasNectar();
        return time && !isHiveNearFire();
    }

    boolean isFlower(BlockPos pos) {
        return this.world.isBlockLoaded(pos) && BeeFlowers.isFlower(this.world.getBlockState(pos));
    }

    /** Block to block, as 1.15's {@code isCloserThan}. */
    boolean isWithin(BlockPos pos, int distance) {
        return pos.distanceSq(new BlockPos(this)) < (double) distance * distance;
    }

    /** From the block's middle to the bee itself. */
    boolean isNearCentre(BlockPos pos, double distance) {
        return pos.distanceSq(this.posX - 0.5D, this.posY - 0.5D, this.posZ - 0.5D) < distance * distance;
    }

    boolean isTooFar(BlockPos pos) {
        return !isWithin(pos, 32);
    }

    /** Heads toward a far position in short random hops (1.15's {@code pathfindRandomlyTowards}). */
    void pathRandomlyTowards(BlockPos target) {
        Vec3d goal = new Vec3d(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        BlockPos here = new BlockPos(this);
        int xz = 6;
        int y = 8;
        int distance = Math.abs(here.getX() - target.getX()) + Math.abs(here.getY() - target.getY()) + Math.abs(here.getZ() - target.getZ());
        if (distance < 15) {
            xz = distance / 2;
            y = distance / 2;
        }
        Vec3d hop = RandomPositionGenerator.findRandomTargetBlockTowards(this, Math.max(1, xz), Math.max(1, y), goal);
        if (hop != null) {
            getNavigator().tryMoveToXYZ(hop.x, hop.y, hop.z, 1.0D);
        }
    }

    // ---- ticking ----

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote && hasNectar() && this.numCropsGrownSincePollination < 10 && this.rand.nextFloat() < 0.05F) {
            int n = this.rand.nextInt(2) + 1;
            for (int i = 0; i < n; i++) {
                com.exiledradio.buzzybeesbackport.bee.client.BeeClient.nectar(this.world,
                        this.posX - 0.3D + this.rand.nextDouble() * 0.6D, this.posY + this.height * 0.5D,
                        this.posZ - 0.3D + this.rand.nextDouble() * 0.6D);
            }
        }
        this.rollAmountO = this.rollAmount;
        this.rollAmount = isRolling() ? Math.min(1.0F, this.rollAmount + 0.2F) : Math.max(0.0F, this.rollAmount - 0.24F);
    }

    /** @return the roll toward a close target, for the model */
    public float getRollAmount(float partialTicks) {
        return this.rollAmountO + (this.rollAmount - this.rollAmountO) * partialTicks;
    }

    @Override
    protected void updateAITasks() {
        if (isInWater()) {
            this.underWaterTicks++;
        } else {
            this.underWaterTicks = 0;
        }
        if (this.underWaterTicks > 20) {
            attackEntityFrom(DamageSource.DROWN, 1.0F);
        }
        if (hasStung()) {
            this.timeSinceSting++;
            if (this.timeSinceSting % 5 == 0 && this.rand.nextInt(MathHelper.clamp(1200 - this.timeSinceSting, 1, 1200)) == 0) {
                attackEntityFrom(DamageSource.GENERIC, getHealth());
            }
        }
        if (!hasNectar()) {
            this.ticksWithoutNectarSinceExitingHive++;
        }
        updateAnger();
        super.updateAITasks();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            if (this.stayOutOfHiveCountdown > 0) {
                this.stayOutOfHiveCountdown--;
            }
            if (this.remainingCooldownBeforeLocatingNewHive > 0) {
                this.remainingCooldownBeforeLocatingNewHive--;
            }
            if (this.remainingCooldownBeforeLocatingNewFlower > 0) {
                this.remainingCooldownBeforeLocatingNewFlower--;
            }
            EntityLivingBase target = getAttackTarget();
            setBeeFlag(FLAG_ROLL, isAngry() && !hasStung() && target != null && target.getDistanceSq(this) < 4.0D);
            if (this.ticksExisted % 20 == 0 && !isHiveValid()) {
                this.hivePos = null;
            }
        }
    }

    // ---- combat ----

    @Override
    public boolean attackEntityAsMob(Entity target) {
        DamageSource sting = new EntityDamageSource(BeeContent.STING_DAMAGE, this);
        boolean hit = target.attackEntityFrom(sting, (float) (int) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
        if (hit) {
            applyEnchantments(this, target);
            if (target instanceof EntityLivingBase) {
                int seconds = this.world.getDifficulty() == EnumDifficulty.NORMAL ? 10
                        : this.world.getDifficulty() == EnumDifficulty.HARD ? 18 : 0;
                if (seconds > 0) {
                    ((EntityLivingBase) target).addPotionEffect(new PotionEffect(MobEffects.POISON, seconds * 20, 0));
                }
            }
            setHasStung(true);
            stopBeingAngry();
            playSound(BeeContent.BEE_STING, 1.0F, 1.0F);
        }
        return hit;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable(source)) {
            return false;
        }
        if (!this.world.isRemote) {
            this.pollinateGoal.stopPollinating();
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.ARTHROPOD;
    }

    // ---- breeding ----

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return BeeFlowers.isFlowerItem(stack);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return new EntityBee(this.world);
    }

    // ---- flying body ----

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected void updateFallState(double y, boolean onGround, IBlockState state, BlockPos pos) {
    }

    @Override
    protected void handleJumpWater() {
        this.motionY += 0.01D;
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.5F;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.Block block) {
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    // ---- sounds ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BeeContent.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BeeContent.BEE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    // ---- saving ----

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (this.hivePos != null) {
            tag.setTag("HivePos", NBTUtil.createPosTag(this.hivePos));
        }
        if (this.savedFlowerPos != null) {
            tag.setTag("FlowerPos", NBTUtil.createPosTag(this.savedFlowerPos));
        }
        tag.setBoolean("HasNectar", hasNectar());
        tag.setBoolean("HasStung", hasStung());
        tag.setInteger("TicksSincePollination", this.ticksWithoutNectarSinceExitingHive);
        tag.setInteger("CannotEnterHiveTicks", this.stayOutOfHiveCountdown);
        tag.setInteger("CropsGrownSincePollination", this.numCropsGrownSincePollination);
        tag.setInteger("AngerTime", getAngerTime());
        if (this.angerTarget != null) {
            tag.setUniqueId("AngryAt", this.angerTarget);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        this.hivePos = tag.hasKey("HivePos", 10) ? NBTUtil.getPosFromTag(tag.getCompoundTag("HivePos")) : null;
        this.savedFlowerPos = tag.hasKey("FlowerPos", 10) ? NBTUtil.getPosFromTag(tag.getCompoundTag("FlowerPos")) : null;
        super.readEntityFromNBT(tag);
        setHasNectar(tag.getBoolean("HasNectar"));
        setHasStung(tag.getBoolean("HasStung"));
        this.ticksWithoutNectarSinceExitingHive = tag.getInteger("TicksSincePollination");
        this.stayOutOfHiveCountdown = tag.getInteger("CannotEnterHiveTicks");
        this.numCropsGrownSincePollination = tag.getInteger("CropsGrownSincePollination");
        setAngerTime(tag.getInteger("AngerTime"));
        this.angerTarget = tag.hasUniqueId("AngryAt") ? tag.getUniqueId("AngryAt") : null;
    }

    // ===================================================================================================
    // Movement helpers
    // ===================================================================================================

    /** 1.15's {@code FlyingMovementController(bee, 20, true)}: flies to its target and hovers when idle. */
    static final class BeeFlyHelper extends EntityMoveHelper {
        BeeFlyHelper(EntityLiving entity) {
            super(entity);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.action == Action.MOVE_TO) {
                this.action = Action.WAIT;
                this.entity.setNoGravity(true);
                double dx = this.posX - this.entity.posX;
                double dy = this.posY - this.entity.posY;
                double dz = this.posZ - this.entity.posZ;
                if (dx * dx + dy * dy + dz * dz < 2.500000277905201E-7D) {
                    this.entity.setMoveVertical(0.0F);
                    this.entity.setMoveForward(0.0F);
                    return;
                }
                float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
                this.entity.rotationYaw = limitAngle(this.entity.rotationYaw, yaw, 90.0F);
                float speed = (float) (this.speed * this.entity.getEntityAttribute(this.entity.onGround
                        ? SharedMonsterAttributes.MOVEMENT_SPEED : SharedMonsterAttributes.FLYING_SPEED).getAttributeValue());
                this.entity.setAIMoveSpeed(speed);
                double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
                float pitch = (float) (-(MathHelper.atan2(dy, horizontal) * (180D / Math.PI)));
                this.entity.rotationPitch = limitAngle(this.entity.rotationPitch, pitch, 20.0F);
                this.entity.setMoveVertical(dy > 0.0D ? speed : -speed);
            } else {
                // Hovering: unlike a parrot, a bee keeps flying when it stops.
                this.entity.setNoGravity(true);
                this.entity.setMoveVertical(0.0F);
                this.entity.setMoveForward(0.0F);
            }
        }
    }

    /** Does not turn to look around while angry (1.15). */
    static final class BeeLookHelper extends EntityLookHelper {
        private final EntityBee bee;

        BeeLookHelper(EntityBee bee) {
            super(bee);
            this.bee = bee;
        }

        @Override
        public void onUpdateLook() {
            if (!this.bee.isAngry()) {
                super.onUpdateLook();
            }
        }
    }

    // ===================================================================================================
    // Goals
    // ===================================================================================================

    /** A goal that never runs while the bee is angry (1.15's {@code PassiveGoal}). */
    abstract class PassiveGoal extends EntityAIBase {
        abstract boolean canBeeUse();

        abstract boolean canBeeContinueToUse();

        @Override
        public boolean shouldExecute() {
            return canBeeUse() && !isAngry();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return canBeeContinueToUse() && !isAngry();
        }
    }

    final class StingGoal extends EntityAIAttackMelee {
        StingGoal(EntityCreature bee) {
            super(bee, 1.4D, true);
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && isAngry() && !hasStung();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && isAngry() && !hasStung();
        }
    }

    final class EnterHiveGoal extends PassiveGoal {
        @Override
        boolean canBeeUse() {
            if (hasHive() && wantsToEnterHive() && isNearCentre(EntityBee.this.hivePos, 2.0D)) {
                TileEntity te = EntityBee.this.world.getTileEntity(EntityBee.this.hivePos);
                if (te instanceof TileEntityBeehive) {
                    if (!((TileEntityBeehive) te).isFull()) {
                        return true;
                    }
                    EntityBee.this.hivePos = null;
                }
            }
            return false;
        }

        @Override
        boolean canBeeContinueToUse() {
            return false;
        }

        @Override
        public void startExecuting() {
            TileEntity te = EntityBee.this.world.getTileEntity(EntityBee.this.hivePos);
            if (te instanceof TileEntityBeehive) {
                ((TileEntityBeehive) te).addBee(EntityBee.this, hasNectar());
            }
        }
    }

    /**
     * Follows a player holding a flower within 10 blocks, stopping 2.5 away (1.15's {@code TemptGoal} at 1.25
     * speed). Our own: 1.12.2's tempt goal only takes walking mobs.
     */
    final class FlowerTemptGoal extends EntityAIBase {
        private final EntityBee bee;
        @Nullable
        private EntityPlayer player;
        private int delay;

        FlowerTemptGoal(EntityBee bee) {
            this.bee = bee;
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (this.delay > 0) {
                this.delay--;
                return false;
            }
            this.player = this.bee.world.getClosestPlayerToEntity(this.bee, 10.0D);
            return this.player != null && !this.player.isSpectator()
                    && (BeeFlowers.isFlowerItem(this.player.getHeldItemMainhand()) || BeeFlowers.isFlowerItem(this.player.getHeldItemOffhand()));
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecuteAgain();
        }

        private boolean shouldExecuteAgain() {
            return this.player != null && this.player.isEntityAlive() && this.bee.getDistanceSq(this.player) < 100.0D
                    && (BeeFlowers.isFlowerItem(this.player.getHeldItemMainhand()) || BeeFlowers.isFlowerItem(this.player.getHeldItemOffhand()));
        }

        @Override
        public void resetTask() {
            this.player = null;
            this.bee.getNavigator().clearPath();
            this.delay = 100;
        }

        @Override
        public void updateTask() {
            this.bee.getLookHelper().setLookPositionWithEntity(this.player, this.bee.getHorizontalFaceSpeed() + 20, this.bee.getVerticalFaceSpeed());
            if (this.bee.getDistanceSq(this.player) < 6.25D) {
                this.bee.getNavigator().clearPath();
            } else {
                this.bee.getNavigator().tryMoveToEntityLiving(this.player, 1.25D);
            }
        }
    }

    /** Looks up a hive with room within 20 blocks, every 200 ticks while homeless and wanting home. */
    final class LocateHiveGoal extends PassiveGoal {
        @Override
        boolean canBeeUse() {
            return EntityBee.this.remainingCooldownBeforeLocatingNewHive == 0 && !hasHive() && wantsToEnterHive();
        }

        @Override
        boolean canBeeContinueToUse() {
            return false;
        }

        @Override
        public void startExecuting() {
            EntityBee.this.remainingCooldownBeforeLocatingNewHive = 200;
            List<BlockPos> hives = findNearbyHivesWithSpace();
            if (hives.isEmpty()) {
                return;
            }
            for (BlockPos pos : hives) {
                if (!EntityBee.this.goToHiveGoal.isBlacklisted(pos)) {
                    EntityBee.this.hivePos = pos;
                    return;
                }
            }
            EntityBee.this.goToHiveGoal.clearBlacklist();
            EntityBee.this.hivePos = hives.get(0);
        }

        private List<BlockPos> findNearbyHivesWithSpace() {
            final BlockPos here = new BlockPos(EntityBee.this);
            List<BlockPos> out = new ArrayList<BlockPos>();
            for (BlockPos pos : HiveIndex.near(EntityBee.this.world, here, 20)) {
                TileEntity te = EntityBee.this.world.getTileEntity(pos);
                if (te instanceof TileEntityBeehive && !((TileEntityBeehive) te).isFull()) {
                    out.add(pos);
                }
            }
            Collections.sort(out, new Comparator<BlockPos>() {
                @Override
                public int compare(BlockPos a, BlockPos b) {
                    return Double.compare(a.distanceSq(here), b.distanceSq(here));
                }
            });
            return out;
        }
    }

    /** Flies to its hive: hops while far, a direct path within 16 blocks, gives up on a stuck path. */
    final class GoToHiveGoal extends PassiveGoal {
        private int travellingTicks = EntityBee.this.world.rand.nextInt(10);
        private final List<BlockPos> blacklist = new ArrayList<BlockPos>();
        @Nullable
        private Path lastPath;
        private int ticksStuck;

        GoToHiveGoal() {
            setMutexBits(1);
        }

        @Override
        boolean canBeeUse() {
            return EntityBee.this.hivePos != null && !getLeashed() && wantsToEnterHive()
                    && !hasReachedTarget(EntityBee.this.hivePos)
                    && EntityBee.this.world.getBlockState(EntityBee.this.hivePos).getBlock() instanceof BlockBeehive;
        }

        @Override
        boolean canBeeContinueToUse() {
            return canBeeUse();
        }

        @Override
        public void startExecuting() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
        }

        @Override
        public void resetTask() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            getNavigator().clearPath();
        }

        @Override
        public void updateTask() {
            if (EntityBee.this.hivePos == null) {
                return;
            }
            this.travellingTicks++;
            if (this.travellingTicks > 600) {
                dropAndBlacklist();
            } else if (getNavigator().noPath()) {
                if (!isWithin(EntityBee.this.hivePos, 16)) {
                    if (isTooFar(EntityBee.this.hivePos)) {
                        dropHive();
                    } else {
                        pathRandomlyTowards(EntityBee.this.hivePos);
                    }
                } else if (!pathTo(EntityBee.this.hivePos)) {
                    dropAndBlacklist();
                } else if (this.lastPath != null && getNavigator().getPath() != null && getNavigator().getPath().isSamePath(this.lastPath)) {
                    if (++this.ticksStuck > 60) {
                        dropHive();
                        this.ticksStuck = 0;
                    }
                } else {
                    this.lastPath = getNavigator().getPath();
                }
            }
        }

        private boolean pathTo(BlockPos pos) {
            getNavigator().tryMoveToXYZ(pos.getX(), pos.getY(), pos.getZ(), 1.0D);
            Path path = getNavigator().getPath();
            return path != null && reaches(path, pos);
        }

        boolean isBlacklisted(BlockPos pos) {
            return this.blacklist.contains(pos);
        }

        void clearBlacklist() {
            this.blacklist.clear();
        }

        private void dropAndBlacklist() {
            if (EntityBee.this.hivePos != null) {
                this.blacklist.add(EntityBee.this.hivePos);
                while (this.blacklist.size() > 3) {
                    this.blacklist.remove(0);
                }
            }
            dropHive();
        }

        private void dropHive() {
            EntityBee.this.hivePos = null;
            EntityBee.this.remainingCooldownBeforeLocatingNewHive = 200;
        }

        private boolean hasReachedTarget(BlockPos pos) {
            if (isWithin(pos, 2)) {
                return true;
            }
            Path path = getNavigator().getPath();
            return path != null && reaches(path, pos) && path.isFinished();
        }
    }

    /** @return true if a path ends at (next to) this block */
    static boolean reaches(Path path, BlockPos pos) {
        PathPoint end = path.getFinalPathPoint();
        return end != null && Math.abs(end.x - pos.getX()) <= 1 && Math.abs(end.y - pos.getY()) <= 1 && Math.abs(end.z - pos.getZ()) <= 1;
    }

    /** Goes back to the flower it remembers, after 2400 ticks without nectar. */
    final class GoToKnownFlowerGoal extends PassiveGoal {
        private int travellingTicks = EntityBee.this.world.rand.nextInt(10);

        GoToKnownFlowerGoal() {
            setMutexBits(1);
        }

        @Override
        boolean canBeeUse() {
            return EntityBee.this.savedFlowerPos != null && !getLeashed() && EntityBee.this.ticksWithoutNectarSinceExitingHive > 2400
                    && isFlower(EntityBee.this.savedFlowerPos) && !isWithin(EntityBee.this.savedFlowerPos, 2);
        }

        @Override
        boolean canBeeContinueToUse() {
            return canBeeUse();
        }

        @Override
        public void startExecuting() {
            this.travellingTicks = 0;
        }

        @Override
        public void resetTask() {
            this.travellingTicks = 0;
            getNavigator().clearPath();
        }

        @Override
        public void updateTask() {
            if (EntityBee.this.savedFlowerPos == null) {
                return;
            }
            if (++this.travellingTicks > 600) {
                EntityBee.this.savedFlowerPos = null;
            } else if (getNavigator().noPath()) {
                if (isTooFar(EntityBee.this.savedFlowerPos)) {
                    EntityBee.this.savedFlowerPos = null;
                } else {
                    pathRandomlyTowards(EntityBee.this.savedFlowerPos);
                }
            }
        }
    }

    /** With nectar and a hive, about one try in 30 ticks to grow a crop 1-2 blocks below, 10 per load. */
    final class GrowCropGoal extends PassiveGoal {
        @Override
        boolean canBeeUse() {
            if (EntityBee.this.numCropsGrownSincePollination >= 10) {
                return false;
            }
            if (EntityBee.this.rand.nextFloat() < 0.3F) {
                return false;
            }
            return hasNectar() && isHiveValid();
        }

        @Override
        boolean canBeeContinueToUse() {
            return canBeeUse();
        }

        @Override
        public void updateTask() {
            if (EntityBee.this.rand.nextInt(30) == 0) {
                growBelow();
            }
        }
    }

    /** One growth attempt: the crop 1, then 2, blocks below the bee. */
    void growBelow() {
        BlockPos here = new BlockPos(this);
        for (int i = 1; i <= 2; i++) {
            BlockPos at = here.down(i);
            IBlockState state = this.world.getBlockState(at);
            IBlockState grown = BeeFlowers.grow(state);
            if (grown != null) {
                this.world.playEvent(2005, at, 0);
                this.world.setBlockState(at, grown);
                this.numCropsGrownSincePollination++;
            }
        }
    }

    /** Finds a flower within 5 blocks, hovers over it for 400 ticks and takes its nectar. */
    final class PollinateGoal extends PassiveGoal {
        private int successfulPollinatingTicks;
        private int lastSoundPlayedTick;
        private boolean pollinating;
        @Nullable
        private Vec3d hoverPos;
        private int pollinatingTicks;

        PollinateGoal() {
            setMutexBits(1);
        }

        @Override
        boolean canBeeUse() {
            if (EntityBee.this.remainingCooldownBeforeLocatingNewFlower > 0 || hasNectar() || EntityBee.this.world.isRaining()) {
                return false;
            }
            if (EntityBee.this.rand.nextFloat() < 0.7F) {
                return false;
            }
            BlockPos flower = findFlower(5);
            if (flower == null) {
                return false;
            }
            EntityBee.this.savedFlowerPos = flower;
            getNavigator().tryMoveToXYZ(flower.getX() + 0.5D, flower.getY() + 0.5D, flower.getZ() + 0.5D, 1.2D);
            return true;
        }

        @Override
        boolean canBeeContinueToUse() {
            if (!this.pollinating || !hasFlower() || EntityBee.this.world.isRaining()) {
                return false;
            }
            if (hasPollinatedLongEnough()) {
                return EntityBee.this.rand.nextFloat() < 0.2F;
            }
            if (EntityBee.this.ticksExisted % 20 == 0 && !isFlower(EntityBee.this.savedFlowerPos)) {
                EntityBee.this.savedFlowerPos = null;
                return false;
            }
            return true;
        }

        boolean hasPollinatedLongEnough() {
            return this.successfulPollinatingTicks > 400;
        }

        boolean isPollinating() {
            return this.pollinating;
        }

        void stopPollinating() {
            this.pollinating = false;
        }

        @Override
        public void startExecuting() {
            this.successfulPollinatingTicks = 0;
            this.pollinatingTicks = 0;
            this.lastSoundPlayedTick = 0;
            this.pollinating = true;
            EntityBee.this.ticksWithoutNectarSinceExitingHive = 0;
        }

        @Override
        public void resetTask() {
            if (hasPollinatedLongEnough()) {
                setHasNectar(true);
            }
            this.pollinating = false;
            getNavigator().clearPath();
            EntityBee.this.remainingCooldownBeforeLocatingNewFlower = 200;
        }

        @Override
        public void updateTask() {
            if (EntityBee.this.savedFlowerPos == null) {
                return;
            }
            this.pollinatingTicks++;
            if (this.pollinatingTicks > 600) {
                EntityBee.this.savedFlowerPos = null;
                return;
            }
            Vec3d over = new Vec3d(EntityBee.this.savedFlowerPos.getX() + 0.5D, EntityBee.this.savedFlowerPos.getY() + 0.6D,
                    EntityBee.this.savedFlowerPos.getZ() + 0.5D);
            Vec3d at = getPositionVector();
            if (over.distanceTo(at) > 1.0D) {
                this.hoverPos = over;
                setWantedPos();
                return;
            }
            if (this.hoverPos == null) {
                this.hoverPos = over;
            }
            boolean close = at.distanceTo(this.hoverPos) <= 0.1D;
            boolean move = true;
            if (!close && this.pollinatingTicks > 600) {
                EntityBee.this.savedFlowerPos = null;
                return;
            }
            if (close) {
                if (EntityBee.this.rand.nextInt(25) == 0) {
                    this.hoverPos = new Vec3d(over.x + offset(), over.y, over.z + offset());
                    getNavigator().clearPath();
                } else {
                    move = false;
                }
                getLookHelper().setLookPosition(over.x, over.y, over.z, 10.0F, getVerticalFaceSpeed());
            }
            if (move) {
                setWantedPos();
            }
            this.successfulPollinatingTicks++;
            if (EntityBee.this.rand.nextFloat() < 0.05F && this.successfulPollinatingTicks > this.lastSoundPlayedTick + 60) {
                this.lastSoundPlayedTick = this.successfulPollinatingTicks;
                playSound(BeeContent.BEE_POLLINATE, 1.0F, 1.0F);
            }
        }

        private void setWantedPos() {
            getMoveHelper().setMoveTo(this.hoverPos.x, this.hoverPos.y, this.hoverPos.z, 0.35D);
        }

        private float offset() {
            return (EntityBee.this.rand.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
        }

        /** 1.15's search order: the bee's own level first, outward in rings, then up and down alternately. */
        @Nullable
        private BlockPos findFlower(int range) {
            BlockPos here = new BlockPos(EntityBee.this);
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int i = 0; i <= range; i = i > 0 ? -i : 1 - i) {
                for (int j = 0; j < range; j++) {
                    for (int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                        for (int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                            p.setPos(here.getX() + k, here.getY() + i - 1, here.getZ() + l);
                            if (here.distanceSq(p) < (double) range * range && isFlower(p)) {
                                return p.toImmutable();
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    /** Flies to random air nearby, heading home when more than 22 blocks from its hive. */
    final class WanderGoal extends EntityAIBase {
        WanderGoal() {
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return getNavigator().noPath() && EntityBee.this.rand.nextInt(10) == 0;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            Vec3d dir;
            if (isHiveValid() && !isWithin(EntityBee.this.hivePos, 22)) {
                dir = new Vec3d(EntityBee.this.hivePos.getX() + 0.5D - EntityBee.this.posX, EntityBee.this.hivePos.getY() + 0.5D - EntityBee.this.posY,
                        EntityBee.this.hivePos.getZ() + 0.5D - EntityBee.this.posZ).normalize();
            } else {
                dir = getLook(0.0F);
            }
            Vec3d toward = getPositionVector().add(dir.scale(8.0D));
            Vec3d target = RandomPositionGenerator.findRandomTargetBlockTowards(EntityBee.this, 8, 7, toward);
            if (target == null) {
                target = RandomPositionGenerator.findRandomTarget(EntityBee.this, 8, 4);
            }
            if (target != null) {
                getNavigator().tryMoveToXYZ(target.x, target.y, target.z, 1.0D);
            }
        }
    }

    /** Revenge, calling every bee in range that can see the attacker (1.15's {@code AngerGoal}). */
    static final class HurtByTargetGoal extends EntityAIHurtByTarget {
        private final EntityBee bee;

        HurtByTargetGoal(EntityBee bee) {
            super(bee, true);
            this.bee = bee;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.bee.isAngry() && super.shouldContinueExecuting();
        }

        @Override
        protected void setEntityAttackTarget(EntityCreature creature, EntityLivingBase target) {
            if (creature instanceof EntityBee && this.taskOwner.canEntityBeSeen(target)) {
                creature.setAttackTarget(target);
            }
        }
    }

    /** Goes for the player it is angry at (1.15's {@code AttackPlayerGoal}). */
    static final class AngryAtPlayerGoal extends EntityAINearestAttackableTarget<EntityPlayer> {
        private final EntityBee bee;

        AngryAtPlayerGoal(final EntityBee bee) {
            super(bee, EntityPlayer.class, 10, true, false, target -> target != null && bee.shouldAttack(target));
            this.bee = bee;
        }

        private boolean beeCanTarget() {
            return this.bee.isAngry() && !this.bee.hasStung();
        }

        @Override
        public boolean shouldExecute() {
            return beeCanTarget() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (beeCanTarget() && this.bee.getAttackTarget() != null) {
                return super.shouldContinueExecuting();
            }
            this.targetEntity = null;
            return false;
        }
    }

    /** Forgets a player it was angry at who died (1.16's {@code ResetAngerGoal}). */
    static final class ForgetDeadTargetGoal extends EntityAIBase {
        private final EntityBee bee;

        ForgetDeadTargetGoal(EntityBee bee) {
            this.bee = bee;
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = this.bee.getAttackTarget();
            return target instanceof EntityPlayer && !target.isEntityAlive();
        }

        @Override
        public void startExecuting() {
            this.bee.stopBeingAngry();
        }
    }
}

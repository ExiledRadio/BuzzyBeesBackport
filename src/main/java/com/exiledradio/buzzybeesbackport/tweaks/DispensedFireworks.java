package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.asm.FireworkAngleTransformer;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * 1.15's dispensed firework rockets: shot the way the dispenser faces, at speed 0.5 with inaccuracy 1
 * ({@code shoot(dx, dy, dz, 0.5F, 1.0F)}), and "shot at an angle", so they fly straight instead of
 * speeding up and rising (patch P4, which calls {@link #accelerate}). Placed at the dispenser's face as
 * current Java does.
 *
 * <p>1.12.2 has no shot-at-angle flag on the rocket, so it rides in the rocket's firework item, which is
 * saved with the rocket and sent to clients with it: a {@value #TAG} compound holding the launch motion. The
 * client takes that motion on the rocket's first tick (the spawn packet carries none for rockets). The
 * item's {@code Fireworks} tag, its explosions and its flight time are untouched.
 */
public final class DispensedFireworks {

    public static final String TAG = "BuzzyBeesShotAtAngle";

    private static final DataParameter<ItemStack> FIREWORK_ITEM = ReflectionHelper.getPrivateValue(EntityFireworkRocket.class, null,
            "FIREWORK_ITEM", "field_184566_a");

    private DispensedFireworks() {
    }

    /** From the patched {@code onUpdate}: vanilla's speed-up, except for a rocket shot at an angle. */
    public static void accelerate(EntityFireworkRocket rocket) {
        NBTTagCompound shot = shot(rocket);
        if (shot == null) {
            rocket.motionX *= 1.15D;
            rocket.motionZ *= 1.15D;
            rocket.motionY += 0.04D;
        } else if (rocket.world.isRemote && rocket.ticksExisted <= 1) {
            rocket.motionX = shot.getDouble("X");
            rocket.motionY = shot.getDouble("Y");
            rocket.motionZ = shot.getDouble("Z");
        }
    }

    /** @return the rocket's launch motion if it was shot at an angle, else null */
    public static NBTTagCompound shot(EntityFireworkRocket rocket) {
        ItemStack item = rocket.getDataManager().get(FIREWORK_ITEM);
        if (item.isEmpty() || !item.hasTagCompound() || !item.getTagCompound().hasKey(TAG, 10)) {
            return null;
        }
        return item.getTagCompound().getCompoundTag(TAG);
    }

    /** Replaces vanilla's dispenser behavior for rockets, unless the patch is missing or another mod replaced it. */
    static void register() {
        // Loading the class runs the transformer, if it has not run yet.
        EntityFireworkRocket.class.getName();
        if (!FireworkAngleTransformer.applied) {
            BuzzyBeesBackport.LOGGER.warn("Patch P4 is not applied: dispensers keep firing rockets straight up.");
            return;
        }
        IBehaviorDispenseItem old = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.FIREWORKS);
        if (old != null && !old.getClass().getName().startsWith("net.minecraft.")) {
            BuzzyBeesBackport.LOGGER.info("Another mod fires rockets from dispensers ({}); leaving it.", old.getClass().getName());
            return;
        }
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.FIREWORKS, new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                fire(source, stack);
                stack.shrink(1);
                return stack;
            }

            @Override
            protected void playDispenseSound(IBlockSource source) {
                source.getWorld().playEvent(1004, source.getBlockPos(), 0);
            }
        });
    }

    /** 1.15's dispensed rocket, placed as current Java places it; also used by the self-test. */
    public static EntityFireworkRocket fire(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();
        EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
        EntityFireworkRocket rocket = new EntityFireworkRocket(world, source.getX(), source.getY(), source.getZ(), stack);
        double half = 0.5000099999997474D;
        rocket.setPosition(source.getX() + facing.getXOffset() * (half - rocket.width / 2.0D),
                source.getY() + facing.getYOffset() * (half - rocket.height / 2.0D) - rocket.height / 2.0D,
                source.getZ() + facing.getZOffset() * (half - rocket.width / 2.0D));
        shoot(rocket, facing.getXOffset(), facing.getYOffset(), facing.getZOffset(), 0.5F, 1.0F);
        ItemStack item = rocket.getDataManager().get(FIREWORK_ITEM);
        item = item.isEmpty() ? new ItemStack(Items.FIREWORKS) : item.copy();
        NBTTagCompound shot = new NBTTagCompound();
        shot.setDouble("X", rocket.motionX);
        shot.setDouble("Y", rocket.motionY);
        shot.setDouble("Z", rocket.motionZ);
        item.setTagInfo(TAG, shot);
        rocket.getDataManager().set(FIREWORK_ITEM, item);
        world.spawnEntity(rocket);
        return rocket;
    }

    /** 1.15's {@code FireworkRocketEntity.shoot}. */
    private static void shoot(EntityFireworkRocket rocket, double x, double y, double z, float velocity, float inaccuracy) {
        Vec3d v = new Vec3d(x, y, z).normalize().add(rocket.world.rand.nextGaussian() * 0.0075F * inaccuracy,
                rocket.world.rand.nextGaussian() * 0.0075F * inaccuracy, rocket.world.rand.nextGaussian() * 0.0075F * inaccuracy)
                .scale(velocity);
        rocket.motionX = v.x;
        rocket.motionY = v.y;
        rocket.motionZ = v.z;
        float horizontal = MathHelper.sqrt(v.x * v.x + v.z * v.z);
        rocket.rotationYaw = (float) (MathHelper.atan2(v.x, v.z) * (180D / Math.PI));
        rocket.rotationPitch = (float) (MathHelper.atan2(v.y, horizontal) * (180D / Math.PI));
        rocket.prevRotationYaw = rocket.rotationYaw;
        rocket.prevRotationPitch = rocket.rotationPitch;
    }
}

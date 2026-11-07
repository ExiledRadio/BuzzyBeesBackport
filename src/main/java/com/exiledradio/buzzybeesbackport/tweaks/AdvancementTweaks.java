package com.exiledradio.buzzybeesbackport.tweaks;

import com.exiledradio.buzzybeesbackport.BuzzyBeesBackport;
import com.exiledradio.buzzybeesbackport.ModConfig;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.15's additions to two vanilla advancements: <i>Two by Two</i> ({@code husbandry/bred_all_animals}) also
 * needs a bee bred ({@code bred_bee}), and <i>A Balanced Diet</i> ({@code husbandry/balanced_diet}) a honey
 * bottle eaten ({@code honey_bottle}). Each is one more criterion and one more requirement, as 1.15's files.
 *
 * <p>1.12.2 has no way to add to another namespace's advancement file, so the loaded advancement objects
 * get the criterion (their two final fields, by reflection). The server's advancement manager builds new
 * objects when it loads (server start, {@code /reload}), so each server tick checks whether the loaded
 * object is still the one changed; a new one is changed too and every player's progress is read again from
 * their file (what {@code /reload} itself has just done), so their listeners include the new criterion and
 * progress saved for it is kept. Another mod adding its own criteria the same way is kept.
 */
@Mod.EventBusSubscriber(modid = BuzzyBeesBackport.MOD_ID)
public final class AdvancementTweaks {

    public static final ResourceLocation TWO_BY_TWO = new ResourceLocation("minecraft", "husbandry/bred_all_animals");
    public static final ResourceLocation BALANCED_DIET = new ResourceLocation("minecraft", "husbandry/balanced_diet");
    public static final String BEE_CRITERION = "bred_bee";
    public static final String HONEY_CRITERION = "honey_bottle";
    private static final ResourceLocation BEE = new ResourceLocation(BuzzyBeesBackport.MOD_ID, "bee");
    private static final ResourceLocation HONEY_BOTTLE = new ResourceLocation(BuzzyBeesBackport.MOD_ID, "honey_bottle");

    private static final Field CRITERIA = ReflectionHelper.findField(Advancement.class, "criteria", "field_192080_e");
    private static final Field REQUIREMENTS = ReflectionHelper.findField(Advancement.class, "requirements", "field_192081_f");

    /** The advancement objects last changed (the manager's current ones once checked). */
    private static Advancement twoByTwo;
    private static Advancement balancedDiet;
    private static boolean failed;

    private AdvancementTweaks() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            check(server);
        }
    }

    /** Changes the loaded advancements if they are new; public for the self-test. */
    public static void check(MinecraftServer server) {
        if (!Tweaks.on() || !ModConfig.tweaks.advancementBees || failed || server.worlds.length == 0 || server.worlds[0] == null) {
            return;
        }
        Advancement two = server.getAdvancementManager().getAdvancement(TWO_BY_TWO);
        Advancement diet = server.getAdvancementManager().getAdvancement(BALANCED_DIET);
        if (two == twoByTwo && diet == balancedDiet) {
            return;
        }
        boolean changed = false;
        try {
            if (two != null && two != twoByTwo && ForgeRegistries.ENTITIES.containsKey(BEE)) {
                JsonObject type = new JsonObject();
                type.addProperty("type", BEE.toString());
                JsonObject conditions = new JsonObject();
                conditions.add("parent", type);
                changed |= add(two, BEE_CRITERION, CriteriaTriggers.BRED_ANIMALS.deserializeInstance(conditions, null));
            }
            if (diet != null && diet != balancedDiet && ForgeRegistries.ITEMS.containsKey(HONEY_BOTTLE)) {
                JsonObject item = new JsonObject();
                item.addProperty("item", HONEY_BOTTLE.toString());
                JsonObject conditions = new JsonObject();
                conditions.add("item", item);
                changed |= add(diet, HONEY_CRITERION, CriteriaTriggers.CONSUME_ITEM.deserializeInstance(conditions, null));
            }
        } catch (RuntimeException | IllegalAccessException e) {
            failed = true;
            BuzzyBeesBackport.LOGGER.warn("Could not add bees and honey to Two by Two and A Balanced Diet; they keep 1.12.2's criteria.", e);
            return;
        }
        twoByTwo = two;
        balancedDiet = diet;
        if (changed) {
            // Listeners and progress were built from the old criteria: read them again (from the files).
            server.getPlayerList().reloadResources();
        }
    }

    /** @return true if the criterion was added (false if the advancement already had it) */
    @SuppressWarnings("unchecked")
    private static boolean add(Advancement advancement, String name, ICriterionInstance instance) throws IllegalAccessException {
        Map<String, Criterion> criteria = (Map<String, Criterion>) CRITERIA.get(advancement);
        if (criteria.containsKey(name)) {
            return false;
        }
        Map<String, Criterion> more = new LinkedHashMap<String, Criterion>(criteria);
        more.put(name, new Criterion(instance));
        String[][] requirements = (String[][]) REQUIREMENTS.get(advancement);
        String[][] longer = Arrays.copyOf(requirements, requirements.length + 1);
        longer[requirements.length] = new String[] {name};
        CRITERIA.set(advancement, ImmutableMap.copyOf(more));
        REQUIREMENTS.set(advancement, longer);
        return true;
    }
}

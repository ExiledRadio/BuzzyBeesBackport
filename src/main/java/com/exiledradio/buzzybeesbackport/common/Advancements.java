package com.exiledradio.buzzybeesbackport.common;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Grants this mod's advancements from code.
 *
 * <p>1.15's triggers ({@code slide_down_block}, {@code bee_nest_destroyed}, the smoky location check) do not
 * exist in 1.12.2, and registering new trigger types needs a private vanilla method. Each of our
 * advancements instead has one {@code minecraft:impossible} criterion, and the code that sees the event
 * grants it here. They sit in vanilla's tabs through their {@code parent}.
 */
public final class Advancements {

    private Advancements() {
    }

    /**
     * Grants every remaining criterion of {@code buzzybeesbackport:<path>}.
     *
     * @return true if the player did not have it before
     */
    public static boolean grant(EntityPlayerMP player, String path) {
        Advancement advancement = player.server.getAdvancementManager().getAdvancement(Content.id(path));
        if (advancement == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancements().getProgress(advancement);
        if (progress.isDone()) {
            return false;
        }
        // Copied first: granting changes the progress the iterable reads from.
        java.util.List<String> remaining = new java.util.ArrayList<String>();
        for (String criterion : progress.getRemaningCriteria()) {
            remaining.add(criterion);
        }
        for (String criterion : remaining) {
            player.getAdvancements().grantCriterion(advancement, criterion);
        }
        return true;
    }

    /** @return true if the player has {@code buzzybeesbackport:<path>} */
    public static boolean has(EntityPlayerMP player, String path) {
        Advancement advancement = player.server.getAdvancementManager().getAdvancement(Content.id(path));
        return advancement != null && player.getAdvancements().getProgress(advancement).isDone();
    }
}

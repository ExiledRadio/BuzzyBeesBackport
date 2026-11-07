"""The tweaks stream's config switches: field, display name, comment. One list, read by generate.py (lang lines)
and by the one-time ModConfig writer below, so the comment and the config screen's tooltip never differ.
"""

# (field, name, comment[, default]); the default is true unless given.
SWITCHES = [
    ("endStoneBricks", "End Stone Bricks Hardness", "End stone bricks take as long to mine as stone (hardness 3) and resist explosions like it (9)."),
    ("boatFuel", "Boats as Fuel", "A boat burns long enough to smelt 6 items."),
    ("spongeDries", "Sponges Dry in the Nether", "A wet sponge placed in the Nether dries at once."),
    ("railFacing", "Rails Face the Player", "A rail with nothing to connect to is placed along the way you are facing."),
    ("fernSeeds", "Large Ferns Drop Seeds", "Large ferns drop wheat seeds like tall grass."),
    ("spawnEggBabies", "Spawn Eggs Make Baby Zombies", "Using a spawn egg on an adult zombie, husk, zombie villager, zombie pigman or drowned of its kind spawns a baby."),
    ("babyAnimals", "Babies in Animal Groups", "Animals spawning in groups are sometimes babies: 5% for cows, sheep, pigs, chickens and foxes, 10% for wolves, horses and llamas."),
    ("parrotImitation", "Parrots Imitate Fewer Mobs", "Parrots no longer imitate endermen, polar bears, wolves or zombie pigmen."),
    ("creativeEating", "Eat in Creative", "Food and cake can be eaten in Creative mode."),
    ("bedSpawn", "Beds Set Spawn on Use", "Using a bed sets your respawn point and says so, even when it is too early to sleep."),
    ("gameRules", "1.15 Game Rules", "Adds the game rules fallDamage, fireDamage, drowningDamage, doImmediateRespawn and doInsomnia."),
    ("dispensers", "Dispenser Changes", "Dispensers fill glass bottles from water, place armor stands and light campfires with fire charges."),
    ("bellRedstone", "Bells Ring on Redstone", "A bell rings when it is powered by redstone."),
    ("splashTexts", "1.15 Splash Texts", "The title screen can show 1.15's three new splash texts."),
    ("craftingShiftClick", "Shift-Click into the Crafting Grid", "At a crafting table, shift-clicking a stack in your inventory moves it into the crafting grid, and only to the other part of the inventory when the grid has no room."),
    ("elytraJump", "Open Elytra While Rising", "Elytra can be opened right after a jump, while you are still rising."),
    ("toggleSneak", "Toggle Sneak", "Client: the sneak key turns sneaking on and off instead of being held.", False),
    ("toggleSprint", "Toggle Sprint", "Client: the sprint key turns sprinting on and off instead of being held.", False),
    ("dispensedFireworks", "Dispensed Fireworks Fly Forward", "A firework rocket fired from a dispenser flies straight the way the dispenser faces."),
    ("advancementBees", "Bees in Husbandry Advancements", "Two by Two also needs bees bred, and A Balanced Diet also needs a honey bottle."),
    ("parrotsWhileRiding", "Parrots Stay on Shoulders", "Parrots stay on your shoulders while you ride a horse, boat, minecart or anything else."),
    ("xpWithLoot", "Experience Drops With Loot", "A dying mob or player drops its experience at once, with its loot, instead of after the death animation."),
]


def java():
    out = []
    for entry in SWITCHES:
        field, name, comment = entry[:3]
        default = entry[3] if len(entry) > 3 else True
        out.append('        @Config.Comment("%s")' % comment.replace('"', '\\"'))
        out.append('        @Config.LangKey("config.buzzybeesbackport.tweaks.%s")' % field)
        out.append("        public boolean %s = %s;" % (field, "true" if default else "false"))
        out.append("")
    return "\n".join(out).rstrip("\n")


def lang():
    lines = []
    for entry in SWITCHES:
        field, name, comment = entry[:3]
        lines.append("config.buzzybeesbackport.tweaks.%s=%s" % (field, name))
        lines.append("config.buzzybeesbackport.tweaks.%s.tooltip=%s" % (field, comment))
    return lines


if __name__ == "__main__":
    print(java())

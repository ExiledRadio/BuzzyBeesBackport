# Buzzy Bees Backport

**All of 1.15 Buzzy Bees on 1.12.2: bees, bee nests and beehives, honey and honeycomb, cracked iron golems, and the rest of the update.**

Bees live in nests on oak and birch trees, fly out to the flowers, come home with nectar and fill their nest with honey. They grow the crops they fly over, hide from rain and night, and turn on anyone who hits one of them or breaks their home. Harvest honey bottles and honeycomb from a full nest, build beehives to keep them yourself, calm them with a campfire, and slide down honey blocks. Iron golems show cracks as they are hurt and can be repaired with iron ingots. And 1.15's smaller changes come along: end stone bricks as hard as stone, boats that burn longer, rails that face you, eating in Creative, the new game rules and more.

It is ported from the decompiled game code, so the bees' timings, distances and chances, the honey block's physics, the golems' crack stages and the nest generation rates are the ones the real game uses. Where today's Minecraft behaves differently from 1.15, today's wins.

**Everything 1.15 added is in this one mod.** It needs one library, Backport Core, which holds the code the update backports share, and Backport Core needs Fluidlogged API. All three are needed on the client and the server.

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, questions and release pings.

***

## Bees

*   **Bees** fly between flowers and their nest. A bee hovers over a flower for twenty seconds to gather its nectar, then flies home and makes honey. Pollinated bees shed drops of nectar and grow the crops they fly over: wheat, carrots, potatoes, beetroot, melon and pumpkin stems, and sweet berries.
*   They go home at night and in the rain, and stay out in the Nether and the End only as long as they need to.
*   **Neutral**: hit one and it, and the bees around it, chase you. A sting poisons you (10 seconds on Normal, 18 on Hard), and the bee loses its stinger and dies a minute later. Bees take damage in water.
*   **Breeding and following**: any flower breeds them, and they follow you while you hold one. Babies grow up in a day.
*   1.15's model, angry red eyes, nectar texture, buzzing that grows louder as they fly faster, and a spawn egg.

## Nests and hives

*   **Bee nests** grow on oak and birch trees: on 1 tree in 20 in plains and sunflower plains, 1 in 50 in flower forests and 1 in 500 in forests and birch forests, each with 2 or 3 bees. An oak or birch sapling that grows with a flower nearby gets one 1 time in 20, anywhere.
*   **Beehives**: 6 planks and 3 honeycombs. Both hold up to 3 bees and fill with honey as nectar-carrying bees come home.
*   **Harvesting**: when a nest or hive is full, shears give 3 honeycombs and a glass bottle gives a honey bottle. A lit campfire up to five blocks below (its smoke has to reach the hive), or fire right underneath, keeps the bees calm. Otherwise they come out angry.
*   **Silk Touch** moves a nest or hive with its bees and honey, shown in the tooltip. Without it a nest drops nothing but angry bees, and a beehive drops itself empty. Fire next to a nest drives the bees out.
*   Comparators read the honey level. Dispensers with shears or glass bottles harvest a full hive. Full hives drip honey.
*   Advancements: *Bee Our Guest* and *Total Beelocation*.

## Honey

*   **Honey bottle**: 6 hunger, cures Poison, gives the bottle back, stacks to 16. One makes 3 sugar.
*   **Honey block**: 4 honey bottles (you keep the bottles). Walking on it is slow and jumping from it is half as high, even through a carpet or slab on top. Fall against its side and you slide down slowly; land on it and you take a fifth of the fall damage. Pistons move it and what touches it, like slime, but honey and slime do not stick to each other, and whatever stands on a moving honey block rides along. It does not carry redstone. Advancement *Sticky Situation*.
*   **Honeycomb** and the decorative **honeycomb block**.

## Iron golems

*   Iron golems **crack** as they lose health, in three stages, and **iron ingots repair** them: each heals a quarter. Works on every iron golem, village ones included.

## The rest of 1.15

Each of these has its own switch in the config:

*   End stone bricks are as hard as stone. Boats burn for 6 items. A wet sponge placed in the Nether dries at once.
*   A rail with nothing to connect to runs the way you face. Large ferns drop wheat seeds.
*   A zombie, husk, zombie villager or zombie pigman spawn egg used on an adult of its kind makes a baby.
*   Some of the animals that spawn with new land are babies.
*   Parrots no longer imitate endermen, polar bears, wolves or zombie pigmen.
*   Food and cake can be eaten in Creative mode.
*   Using a bed sets your respawn point and tells you, even when it is too early to sleep.
*   Game rules `fallDamage`, `fireDamage`, `drowningDamage`, `doImmediateRespawn` and `doInsomnia`.
*   Dispensers fill glass bottles from water, place armor stands and light campfires with fire charges.
*   Bells ring when powered by redstone.
*   The title screen has 1.15's three new splash texts.
*   Shift-clicking a stack in the crafting table moves it into the grid.
*   Elytra open right after a jump, while you are still rising.
*   Toggle sneak and toggle sprint, as 1.15's accessibility options (off by default).
*   Firework rockets from a dispenser fly the way it faces.
*   *Two by Two* needs bees, and *A Balanced Diet* needs honey bottles.
*   Parrots stay on your shoulder while you ride.
*   Experience drops at the same moment as the loot.

## With the other backports

*   **Village & Pillage Backport**: its campfires calm bees and dispensers light them, bees pollinate its cornflowers, lilies of the valley and wither roses and grow its sweet berry bushes, its bells ring on redstone, and its village golems crack and are repaired like any other.
*   Without it, this mod brings its own campfire, the same block, so bees can be calmed either way.
*   **Update Aquatic Backport**: `doInsomnia` stops its phantoms, and drowned spawn eggs make baby drowned.

## Languages

English, Spanish (Spain and Latin America), Brazilian Portuguese, French, German, Russian, Simplified and Traditional Chinese, Japanese, Korean, Italian, Polish, Turkish, Ukrainian and Dutch. Every block, item and mob name is the game's own.

## Configuration

`config/buzzybeesbackport.cfg`, also in the in-game config screen. Every part of the mod has its own switch: bees, honey, nest generation, iron golems, and each of the smaller changes.

## Requirements

*   Minecraft 1.12.2, Forge 14.23.5.2847 or newer.
*   Backport Core 1.3.0 or newer, and Fluidlogged API 3.3.3 or newer (Backport Core's own requirement). Both on the client and the server.
*   The mod contains a small coremod of its own for five vanilla behaviors 1.12.2 has no hook for: honey and slime not sticking together, shift-clicking into the crafting grid, elytra opening while rising, dispensed fireworks and parrots on the shoulder while riding. The last four switch off with their settings; the honey one only ever touches honey blocks. Each steps aside, with a line in the log, if another mod has changed the same code.

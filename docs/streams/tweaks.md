# Stream X: other 1.15 changes

Package `tweaks` (client half in `tweaks/client`). Config `tweaks`: `enabled` (restart) and one switch per
change (`tools/tweaks/switches.py` holds the list, its comments, config-screen names and defaults). No
blocks, items, entities or packets. The second pass added four bytecode patches, P2-P5 (`docs/PATCHES.md`).

## Changelog section

*   End stone bricks are as hard as stone and resist explosions like it.
*   Boats burn long enough to smelt 6 items.
*   A wet sponge placed in the Nether dries at once.
*   A rail with nothing to connect to is placed along the way you face.
*   Large ferns drop wheat seeds, like tall grass.
*   A zombie, husk, zombie villager, zombie pigman or drowned spawn egg used on an adult of its kind makes a
    baby.
*   Animals spawning with new chunks are sometimes babies: 5% of cows, sheep, pigs, chickens and foxes, 10%
    of wolves, horses and llamas.
*   Parrots no longer imitate endermen, polar bears, wolves or zombie pigmen.
*   Food and cake can be eaten in Creative mode.
*   Using a bed sets your respawn point and says "Respawn point set", even when it is too early to sleep.
*   New game rules: `fallDamage`, `fireDamage`, `drowningDamage` (players), `doImmediateRespawn`, and
    `doInsomnia` (phantoms).
*   Dispensers fill glass bottles from water, place armor stands, and light campfires with fire charges.
*   Bells ring when powered by redstone.
*   The title screen can show 1.15's three new splash texts.
*   At a crafting table, shift-clicking a stack in the inventory moves it into the crafting grid.
*   Elytra can be opened right after a jump, while still rising.
*   Toggle sneak and toggle sprint (off by default, as in 1.15): the key turns sneaking or sprinting on and
    off instead of being held.
*   Firework rockets fired from a dispenser fly straight the way it faces.
*   *Two by Two* also needs bees bred, and *A Balanced Diet* a honey bottle.
*   Parrots stay on your shoulders while you ride.
*   Experience drops at once with a mob's loot, not at the end of its death animation.

## How it works

*   **Numbers**: end stone bricks hardness 3 and blast resistance 9 (1.12.2 stores resistance x3 and divides
    by 5, so it is set as 15); boats 1200 ticks; ferns 1 in 8 like 1.12.2's own tall grass; babies by 1.15's
    group chances, applied to animals that join a world while a chunk is being populated (where 1.12.2's
    animals spawn).
*   **Rails**: Forge runs the rail's own `onBlockAdded` after the place event, and it picks north-south when
    nothing connects, so the player's facing is applied at the end of the tick.
*   **Beds**: 1.15's `trySleep` sets the respawn point (with the message, if it changed) after the distance
    check and before the time-of-day and monster checks; done on Forge's sleep event with 1.12.2's own reach
    rule.
*   **Game rules** are added to each world when it loads, with 1.15's defaults. `doImmediateRespawn`: the
    server respawns the player the tick after death, as if Respawn were pressed, and the client closes the
    death screen once alive. `doInsomnia` off denies Update Aquatic's phantoms where they go through Forge's
    spawn check (Rule 2; that mod's own spawner should also read the rule).
*   **Dispensers** wrap the behavior that was there before (for glass bottles, the bee stream's hive check
    comes first), so nothing else is lost.
*   **Bells** and **campfires** are Village & Pillage's (Rule 2): the bell is rung through its public
    `ringByVillager` on the rising edge of power (on Forge's neighbor-notify event), the campfire lit through
    its `lit` property.
*   **Parrots**: Forge's `EntityParrot.MIMIC_SOUNDS` map, by reflection at init.
*   **Splash texts** stay English, as vanilla's splashes are.
*   **Crafting table** (patch P2): vanilla's container keeps working for every mod; only its shift-click
    tries the grid first (1.15's `if (!merge(1, 10)) { old move }`).
*   **Elytra** (patch P3 on the server, `ElytraClient` on the client): the server's "falling" test is
    replaced by 1.15's conditions (adds: no Levitation); the client sends vanilla's start packet on a fresh
    jump press while rising, from Forge's input event (same timing and state as vanilla's own test), and not
    when the press toggles creative flight, when riding or on a ladder (1.15's client conditions).
*   **Toggle sneak and sprint** (`ToggleKeys`, client): 1.15's `ToggleableKeyBinding` - a press flips the
    binding's held state, a release does nothing, and the state survives opening screens. Set after every
    event for that key and at the start of each client tick; reset when leaving a world. Config switches
    only (1.12.2 has no Accessibility screen).
*   **Fireworks** (patch P4): position at the dispenser's face (current Java), `shoot(facing, 0.5, 1.0)`,
    no speed-up for those rockets; the flag rides in the rocket's firework item. Replaces vanilla's dispenser
    behavior only if it is still vanilla's.
*   **Advancements** (`AdvancementTweaks`, no patch): the loaded advancement objects get one criterion and
    one requirement each (`bred_bee`: `bred_animals` with parent `buzzybeesbackport:bee`; `honey_bottle`:
    `consume_item`), by reflection on their two final fields. Every server tick compares the loaded objects
    with the ones changed, so a `/reload` (new objects) is caught the same tick; then players' advancement
    progress is read again from their files, as `/reload` itself just did, so the listeners see the new
    criteria and saved progress for them is kept. Skipped if the bee or the honey bottle is not registered.
*   **Parrots** (patch P5): the riding part of the shoulder-drop test only.
*   **Experience** (`ExperienceDrops`): on Forge's death event (lowest priority, not cancelled) with 1.12.2's
    rule and amount, through Forge's experience event; the end-of-animation drop is cancelled for those
    entities. Classes with their own death animation (`onDeathUpdate`: the ender dragon, modded bosses) are
    left alone. This also gives players their experience drop with `doImmediateRespawn` on, which 1.12.2's
    timing would skip.

## Tested headlessly

First pass: `./gradlew runServer -Pselftest=all` on port 25805: **`SELFTEST DONE pass=262 fail=0`** (tweaks
37), `check_assets` 0 errors. Second pass (`Tweaks2SelfTest`, also `-Pselftest=tweaks`): after this list.

*   End stone bricks 3 / 9; oak and birch boats 1200 ticks; 800 large-fern harvests give about 1 in 8 seeds;
    a grown melon stem already drops seeds (no change needed).
*   A real player places a wet sponge in the Nether: dry; in the Overworld: wet. Rails placed facing east run
    east-west, facing north north-south, and next to another rail still connect to it.
*   Parrots: endermen gone from the imitation list, zombies kept. A zombie egg on a zombie makes one baby and
    uses one egg; a husk egg on a zombie does nothing. 400 cows spawned during a chunk population: about 5%
    calves; a cow spawned otherwise untouched.
*   Creative: bread can be started on a full stomach; a cake loses a slice.
*   A real player trying a bed by day: "too early", and the bed is the respawn point.
*   All five game rules exist, `doImmediateRespawn` off by default; with the three damage rules off a player
    takes no fall, lava or drowning damage, and still takes other damage.
*   Dispensers: a glass bottle facing water becomes a water bottle; an armor stand is placed, using one.

Second pass, port 25809: **`SELFTEST DONE pass=313 fail=0`**, `check_assets` 0 errors. The first pass's
zombie-egg and melon-stem checks were made safe to rerun (a baby left from an earlier run, a 1-in-10 empty
harvest).

*   Patches P2-P5 report themselves applied.
*   Crafting table (a real player's `ContainerWorkbench`): inventory cobblestone goes to grid slot 1, hotbar
    planks to the next free one and then merge; with no room in the grid, dirt goes from the inventory to
    the hotbar; switched off, the grid is skipped.
*   Elytra (vanilla's start packet through the server's own handler): rising and falling players start
    gliding; not with Levitation or without elytra; switched off, rising is refused, falling still works.
*   Fireworks: a dispenser facing east fires one rocket at 0.5 east from its face that keeps its speed and
    moves 0.5 a tick; facing up, steady 0.5 upward; a rocket made otherwise still gains 0.04 a tick.
*   Parrots: one on a player's shoulder stays through a tick in a boat; switched off, it drops.
*   Experience: a zombie killed by a player drops its 5 experience at once and nothing more after 20 death
    ticks; switched off, nothing at death and the orbs after 20 ticks.
*   Advancements: *Two by Two* has 11 requirements with `bred_bee`, *A Balanced Diet* 36 with
    `honey_bottle`; bred cows count for cows and not bees, bred bees count for bees; drinking a honey bottle
    counts; after an advancement reload the new objects get the criteria again and a player's saved bee and
    honey progress is kept.
*   The dev client starts to the title screen with all five patches (no crash from the client classes'
    reflection).

## Not tested

*   With Village & Pillage installed: the bell ringing on redstone and the fire charge lighting its campfire.
    With Update Aquatic installed: `doInsomnia` and its drowned egg.
*   `doImmediateRespawn` end to end (needs a player in the server's player list), the death screen closing,
    the splash texts and the bed message text (client).
*   In a client: toggle sneak and toggle sprint, the elytra opening while rising (the client half,
    `ElytraClient`), dispensed rockets as a client sees them (first-tick motion), the parrot on the shoulder
    while riding as rendered, the advancement screen showing the new criteria.
*   The patches in a real (SRG-named) instance; they match both name sets, as P1 does. Other mods that
    transform the same four classes: not checked.
*   Breeding bees through the real breeding goal for *Two by Two* (the trigger was called directly).

## Not doing

*   **Farmland under piston-moved blocks**: 1.15 lets the moving piston block stand on farmland and checks
    one tick later (a scheduled tick); 1.12.2's farmland uses the same `updateTick` for its random moisture
    ticks, so a scheduled check would also run the drying logic early. Only seen with piston contraptions
    passing over fields; left as 1.12.2.
*   Mobs avoiding lava better (pathfinding), the hand-swing animations, experience orb translucency, grass
    tint smoothing: as the plan says.

## Still to do in this stream

Nothing from the plan's list; the remaining 1.15 changes are under *Not doing*.

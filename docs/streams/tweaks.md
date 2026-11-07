# Stream X: other 1.15 changes (first pass)

Package `tweaks` (client half in `tweaks/client`). Config `tweaks`: `enabled` (restart) and one switch per
change (`tools/tweaks/switches.py` holds the list, its comments and config-screen names). No blocks, items,
entities or packets yet.

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

## Tested headlessly

`./gradlew runServer -Pselftest=all` on port 25805: **`SELFTEST DONE pass=262 fail=0`** (tweaks 37),
`check_assets` 0 errors.

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

## Not tested

*   With Village & Pillage installed: the bell ringing on redstone and the fire charge lighting its campfire.
    With Update Aquatic installed: `doInsomnia` and its drowned egg.
*   `doImmediateRespawn` end to end (needs a player in the server's player list), the death screen closing,
    the splash texts and the bed message text (client).

## Still to do in this stream

Closest to vanilla, with patches allowed where needed:

*   crafting table shift-click into the grid;
*   elytra opening right after a jump;
*   toggle sneak and toggle sprint;
*   firework rockets from dispensers flying the way the dispenser faces;
*   *Two by Two* needing bees and *A Balanced Diet* needing honey bottles;
*   parrots staying on the shoulder while riding (a patch to `EntityPlayer`);
*   the remaining visible ones the plan had set aside: mobs avoiding lava better, farmland under
    piston-placed blocks, the hand-swing animations, experience orbs dropping with the loot.

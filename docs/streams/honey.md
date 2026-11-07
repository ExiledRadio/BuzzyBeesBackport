# Stream H: honey

Package `honey`. Config `honey` (`enabled`). Built in the first session.

## Changelog section

*   **Honey bottle**: drink it for 6 hunger; it cures Poison and gives the bottle back. Stacks to 16. One
    makes 3 sugar.
*   **Honeycomb**: four make a **honeycomb block**.
*   **Honey block**: four honey bottles (you keep the bottles), and back into four bottles with four glass
    bottles. Walking on it is slow and jumping from it is half as high, even through a carpet or slab on
    top. Fall against its side and you slide down slowly instead of falling; land on it and you take a fifth
    of the fall damage. Pistons move it and everything touching it, like slime, and whatever stands on it
    rides along; it doesn't bounce things the way slime does. It doesn't carry redstone.
*   Advancement *Sticky Situation* for sliding down a honey block.

## How it works

*   **Numbers from 1.16.5's `HoneyBlock`** (unchanged since 1.15): collision box 1/16 in from the sides and
    the top; speed factor 0.4; jump factor 0.5 on the jump's base 0.42 (Jump Boost's extra is not halved);
    slide when not on the ground, below 15/16, falling faster than 0.08 a tick and touching a side (the
    centre more than 0.4375 + half the width from the middle), capped at 0.05 a tick with sideways motion
    scaled by the same amount; landing with damage multiplier 0.2. Honey bottle: food 6, saturation
    modifier 0.1, 40 ticks, stack 16. Honeycomb block: hardness 0.6, coral block sounds.
*   **Through carpets**: 1.15 reads the factors from the entity's own block, or from the block half a
    block below its feet when its own has none. `HoneyEvents` does that on the living tick and on jumps;
    bare honey slows in its own collision method.
*   **Pistons**: `isStickyBlock` makes 1.12.2's piston drag what touches the honey. 1.12.2 also throws
    entities hit by any sticky block (meant for slime), so `HoneyPistons` takes that launch back off at
    the end of the tick; at the start of the tick it moves what stands on a sideways-moving honey block
    by the piston's step (1.15's carrying). It runs on the server world and the client world. Piston
    progress is read from its field: `TileEntityPiston.getProgress` is client-only.
*   **Sound**: 1.12.2 plays its generic drink sound for every drink; a player drinking honey gets
    1.15's honey sound instead, through `PlaySoundAtEntityEvent`.
*   **Advancement**: 1.12.2 has no slide trigger, so `common/Advancements.grant` gives it from the slide
    code; its criterion is `minecraft:impossible`, and it sits in vanilla's Adventure tab.
*   **Known difference** (*Still open* 1 in the plan): honey and slime stick to each other here. 1.12.2's
    piston only asks a block whether it is sticky, not what it touches.

## Tested headlessly

`./gradlew runServer -Pselftest=scaffold,honey` on port 25801, then `python tools/check_assets.py`:
**`SELFTEST DONE pass=58 fail=0`**, **2 blocks, 2 states, 4 item models, 63 sound files, 0 errors**.
`python tools/lang/vanilla_keys.py`: 8 vanilla names (blocks, items, the advancement, two subtitles).

*   Registration, stack sizes, container item, translation keys, hardness and blast resistance, sticky,
    not a normal cube, not opaque, the inset collision box, sounds, the honey block dropping itself.
*   Recipes: 4 bottles make a honey block and hand back 4 glass bottles; honey block and 4 glass bottles
    make 4 honey bottles; a bottle makes 3 sugar; 4 honeycombs make a honeycomb block.
*   Drinking (real server player): +6 hunger, +1.2 saturation, Poison cured, the last bottle becomes a
    glass bottle, one of two bottles leaves a glass bottle in the inventory.
*   Sliding: the capped fall, the scaled sideways motion, fall distance reset; a slow fall left alone; a pig
    dropped 7 blocks against a honey column and ticked for real never falls faster than a slide and lands
    with no fall distance; a real player sliding gets *Sticky Situation*.
*   Landing: a 20-block fall onto honey does 4 damage; onto stone kills the 10-health pig.
*   Walking at 0.4 speed, half jumps, Jump Boost's extra kept, bare honey slowed once; honey under a carpet
    slows and halves jumps; a carpet on stone changes nothing.
*   The honey drink sound replaces the generic one; a potion keeps the generic one.
*   Pistons: a pushed honey block drags the stone beside it; a pig on a honey block moved by a piston tile
    entity rides one block; a pig in front is not launched. Control: slime leaves the rider and launches
    the pig in front (motion 1.0), so the test does see vanilla's behaviour.
*   The server booting proves no client type is reached from server code.

## Not tested

*   Anything visual: the translucent model, the inner cube, faces hidden between honey blocks, item models,
    the slide and landing particles (sent with `spawnParticle`, drawn by the client).
*   Sounds playing, and the client half of the piston carrying (`honey/client/HoneyClient`, same code as the
    server half on the client's world).
*   A real piston contraption in a running world (the test drives the piston tile entity directly).
*   Not done: mobs preferring not to path over honey (1.12.2's path node types come from the block a mob
    would stand in, not the one under it, so there is no patch-free hook), and sprinting on honey (1.15
    only slows it).
*   Languages other than English: stream L.

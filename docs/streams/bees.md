# Stream B: bees, nests and hives

Package `bee` (client half in `bee/client`), API in `api` (`BeeFlowers`, `BeeSmoke`). Config `bees` (`enabled`).
Entity id 1. No packets (entity data and the tile's update packet carry everything). Built in the first session.

## Changelog section

*   **Bees**: they fly between flowers and their nest, gather nectar, and make honey in the nest. A bee
    carrying pollen grows the crops it flies over: wheat, carrots, potatoes, beetroot, melon and pumpkin
    stems, and sweet berries. They go home at night, in rain, and to make honey, and stay in the Nether and
    the End no longer than it takes. They breed and follow you for any flower.
*   Bees are peaceful until hit. Hit one and it and the bees around it chase you; a sting poisons you
    (10 seconds on Normal, 18 on Hard) and the bee dies a minute later. Water hurts them.
*   **Bee nests** and **beehives** (6 planks and 3 honeycombs) hold up to 3 bees and fill with honey. When
    full, use shears for 3 honeycombs or a glass bottle for a honey bottle. A campfire up to 5 blocks below, or
    fire right under it, keeps the bees calm; otherwise they come out angry. Dispensers with shears or glass
    bottles harvest a full hive in front of them. Comparators read the honey level.
*   Breaking a nest without Silk Touch gets you nothing but angry bees; a beehive drops itself. With Silk
    Touch both keep their bees and honey, shown in the tooltip. Fire next to a nest drives the bees out.
*   Advancements *Bee Our Guest* and *Total Beelocation*. Bee spawn egg.

## How it works

*   **From 1.16.5's code** (the bee barely changed after 1.15.2): health 10, size 0.7 x 0.6, flying speed
    0.6, ground 0.3, attack 2 (vanilla's difficulty scaling makes it 3 on Hard), follow range 48; goals and
    their numbers (pollinating more than 400 ticks within 5 blocks, 600-tick give-ups, 200-tick cooldowns,
    rest 2400 ticks with nectar and 600 without, crops 1-2 below at 1 in 30, 10 per load, 22-block wander
    leash, 20-block hive search, 32-block "too far", blacklist of 3 hives); anger 400-780 ticks (1.16's
    neutral mobs); honey +1 (1% +2) per nectar bee leaving.
*   **1.12.2 stand-ins**: the hive search uses `HiveIndex` (hives register by chunk on load) instead of 1.15's
    points of interest. Bees fly with our own move helper (hovering when idle, unlike 1.12.2's parrot one),
    `PathNavigateFlying`, and a tempt goal of our own (1.12.2's only takes walking mobs). The look helper is
    private in 1.12.2 and is set by reflection.
*   **Honey level in the tile entity**: 1.12.2's 4 metadata bits hold only the facing, so `honey_level` is read
    from the tile for drawing and comparators, and synced with its update packet.
*   **Calming** (`api/BeeSmoke`): fire right under the hive, or a lit campfire up to 5 below whose smoke rises
    through the middle of the column (one solid block allowed only right on the campfire). The campfire is
    Village & Pillage's, by name and `lit` (Rule 2); others add theirs with `BeeSmoke.addSource`.
*   **Flowers** (`api/BeeFlowers`): every `BlockFlower`, lilac, rose bush, peony, a sunflower's top, and
    Village & Pillage's cornflower, lily of the valley and wither rose by name; growables are `BlockCrops`,
    stems and V&P's sweet berry bush. Later backports add theirs.
*   **Advancements** are granted from code; 1.12.2 lacks 1.15's triggers.
*   **Particles**: 1.12.2's drip sprites in 1.15's honey and nectar colors (client).

## Tested headlessly

`./gradlew runServer -Pselftest=scaffold,honey,bees` on port 25802, then `python tools/check_assets.py`:
**`SELFTEST DONE pass=162 fail=0`** (scaffold 7, honey 58, bees 97), **4 blocks, 50 states, 6 item models,
110 sound files, 0 errors**. `python tools/lang/vanilla_keys.py`: 28 vanilla names, 27 of our own.

*   Registration: entity id and egg, attributes, size, arthropod, hardness, fire spread, facing through
    metadata, the beehive recipe with any planks, the "stung to death" message key.
*   Flowers and breeding items, including what is not one (tall grass, a sunflower's stem, wheat).
*   Smoke: fire under the hive; a campfire 5 below; a block right on it lets smoke through, one higher stops
    it; a carpet right on it passes, a carpet higher stops it; 6 below is too far.
*   Hive occupants: a bee going in leaves the world; rests 2400 ticks with nectar, then leaves through the
    front with its name and health, a new identity and no nectar, and the honey goes up; comparator and block
    state follow the level; 3 fit, a 4th does not; bees stay in at night and with the front blocked; leave by
    day; the hive saves bees and honey; fire next to the hive throws them out at night too.
*   Harvesting by a real player: shears on a non-full hive do nothing; on a full hive 3 honeycombs, the shears
    wear, the hive empties and the bees come out and go for the player; with fire under it a glass bottle gives
    a honey bottle, the bees stay in, and *Bee Our Guest* is granted.
*   Breaking through the real survival harvest path: Silk Touch on a nest with 3 bees drops it with its bees
    and honey (stack of 1) and grants *Total Beelocation*; placing it back restores them; no Silk Touch drops
    nothing and the 3 bees come out angry at the player; a hive drops itself.
*   Dispensers: shears take 3 honeycombs, the last glass bottle becomes a honey bottle, with no hive the bottle
    is dispensed as before.
*   Combat: a hit bee turns on the player for 400-780 ticks and remembers who; a bee nearby joins in, one 12
    blocks higher does not; the sting poisons 10 s on Normal and 18 s on Hard, the bee loses its stinger and
    calms down, and dies within 1400 ticks; a bee saves hive, flower, nectar and stinger; water hurts.
*   Crops: wheat and a melon stem grow a stage; ripe wheat does not.
*   **Real AI, ticked**: a bee with nectar finds and enters a hive 6 blocks away within 2400 ticks; a bee in a
    flower patch gathers nectar within 3000 ticks and remembers its flower.

## Not tested

*   Anything visual: the bee model and its animation, the four textures, nest and hive models, the drip and
    nectar particles, the tooltip text. The flight buzz and other sounds playing.
*   Following and breeding by real AI, the wander goal's shape, and long-run behavior in a live world (bees
    living for days, many hives).
*   With Village & Pillage installed: its campfire calming (the test uses a stand-in source through the same
    API) and bees growing its sweet berry bushes.
*   Not done: current Java's wither rose withering bees (1.21.4), stingers drawn in a stung player,
    1.21.2's wandering tweaks. Generation is stream G.

# Stream F: Future MC pairs and migration

Package `migrate`, generator `tools/futuremc/states.py`, table `migration/futuremc_states.json`. Config `compat`
(`convertRemoved`, `duplicates`). Quiet compatibility (Rule 3): no user-facing text names the other mod.

## Changelog section

*   Worlds that used another mod's bees, nests, hives, honey and honeycomb blocks keep them when that mod is
    removed: they turn into this mod's, hives keeping their bees and honey, and its bees flying about become
    ours. With both installed, the duplicates are hidden (`compat.duplicates`).

## How it works

*   Backport Core's `FutureMcTable` contributor: same-name counterparts for bee_nest, beehive, honey_block,
    honeycomb_block, honey_bottle and honeycomb; the hives' `full` property dropped (ours keeps the exact
    level in the tile entity). The metadata table comes from that mod 0.2.21's own `BeeHiveBlock`
    (facing in bits 0-1, `full` in bit 4), read with javap, not from `/futuremc dump`.
*   Core converts the blocks of a world the mod was removed from on chunk load and gives them fresh tile
    entities, moving only items. `FutureMcBees` runs after it on the same event and copies that mod's hive
    data (its format is nearly 1.15's: `Bees`, `HoneyLevel`, `FlowerPos`) into ours, the bees renamed;
    free-flying bees, which vanilla drops when it cannot read them, are respawned as ours at the end of the
    tick.
*   `DuplicateBlocks`: with both installed, `compat.duplicates` OURS (default) hides theirs, THEIRS ours,
    BOTH nothing; the two items without blocks are hidden the same way.

## Tested headlessly

`-Pselftest=all`: **299 pass, 0 fail** (futuremc 37): every metadata value of their beehive recorded and
converted with its facing; nest, honey and honeycomb blocks and both items map to ours; nothing hidden
without the other mod; their hive data (2 bees, honey 5, a flower) fills our hive and the bees come out as
ours with their health; a free bee of theirs loads as ours.

## Not tested

*   In a real instance with the other mod installed: the duplicate hiding, `/futuremc check` and
    `/futuremc convert` (that command converts blocks with fresh tile entities and has no hook for hive data
    yet: bees and honey inside hives converted that way are lost; see `docs/CORE_REQUESTS.md`).
*   A real world the mod was removed from (the chunk-load path is exercised only through its helpers).

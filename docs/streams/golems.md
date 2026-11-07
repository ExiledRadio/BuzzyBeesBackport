# Stream I: iron golems

Package `golem` (client layer in `golem/client`). Config `golems` (`enabled`, takes effect at once). No entities,
packets or blocks. Built in the first session.

## Changelog section

*   **Iron golems crack** as they lose health: light cracks below three quarters, more below half, heavy below a
    quarter, with a cracking sound at each stage.
*   **Repair them with iron ingots**: each heals a quarter of their health, so four bring one back from the
    brink. Works on every iron golem, built or village-born.

## How it works

*   Numbers from 1.16.5's `IronGolemEntity`: crack stages below 75%, 50% and 25% of health; an ingot heals 25
    (a golem has 100), refused at full health, kept in Creative; the repair sound's pitch 1.0 +/- 0.2.
*   All by events, so `EntityIronGolem` itself is untouched and every golem gets it: right-clicking with an
    iron ingot (`PlayerInteractEvent.EntityInteract`), the crack sound when a hit crosses a stage
    (`LivingDamageEvent`). Village & Pillage's golem spawner makes vanilla golems, so its golems crack and
    repair with nothing more (Rule 2 needs no code here).
*   The cracks are a render layer added at init to whatever renderer draws iron golems (vanilla's, or another
    mod's that still takes layers; the log says which).
*   1.15 also moved the golem's attack damage to an attribute; that changes nothing a player sees.

## Tested headlessly

`./gradlew runServer -Pselftest=scaffold,honey,bees,nests,golems` on port 25804: **`SELFTEST DONE pass=217
fail=0`** (golems 22), `check_assets` 0 errors (115 sound files).

*   The stage thresholds.
*   Through a real player's `interactOn`: full health refuses the ingot and keeps it; from 1 health four
    ingots succeed one by one and bring the golem to full, using four; a fifth is refused; one heals 25 in
    Creative without using it; gold does nothing; 30 damage from full shows low cracks.

## Not tested

*   The crack layer drawn on the golem (client), and the sounds playing.

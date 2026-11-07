# Stream W: the campfire (a copy of Village & Pillage's)

Package `campfire` (client half in `campfire/client`). Config `campfire`: `enabled` (restart), `cookTime`,
`extraFoods`, `excludedFoods`, the owner's own four. Packet 35 (`PacketCampfireDowse`). No entities. Built from
the family's "every mod fully works on its own" rule: bees are calmed by a lit campfire under their hive, and
the campfire belongs to Village & Pillage Backport.

## Changelog section

*   **Campfire**, when Village & Pillage Backport is not installed (with it, its campfire is used and this mod
    adds none). Crafted from 3 sticks, coal or charcoal and 3 logs. It lights the area (light 15), cooks up to
    four raw foods at once in 30 seconds without fuel, hurts what stands in it, sends its smoke three times as
    high over a hay bale, and keeps bees calm when it is lit up to 5 blocks under their hive.
*   Light it with flint and steel, a fire charge or a burning arrow; put it out with a shovel, a water bucket or
    a splash water bottle. Food on it stays while it is out. It drops 2 charcoal, or itself with Silk Touch.
*   A world that had Village & Pillage's campfires and is opened without that mod keeps them, as this mod's.

## How it works

*   **Copied, not rewritten.** `tools/campfire/copy_from_owner.py` copies the owner's (Village & Pillage 2.4.0)
    `BlockCampfire`, `CampfireEvents`, `CampfireRecipes`, `PacketCampfireDowse`, `TileEntityCampfire` and the
    three client classes, with the package, mod class and mod id renamed and nothing else but two mechanical
    edits (`NetworkHandler.channel` is a field here; the packet is id 35, not 18). The blockstate, models,
    textures, the twelve smoke frames, the crackle sounds and the recipe come the same way. Rerun it after the
    owner changes its campfire. Same three properties (`facing`, `lit`, `signal_fire`), same metadata, same
    registry path, so the owner could take ours over by renaming.
*   **This mod's own**: `CampfireContent` (registration through `common/Content`, gated on
    `campfire.enabled` and `!Loader.isModLoaded("villagerbackport")`; the missing-mappings remap of
    `villagerbackport:campfire`, block and item, onto ours) and `client/CampfireClient` (the food renderer and
    the smoke sprites; the item model comes from `ClientSetup` like every other item).
*   **Two asset differences**, neither visible: the blockstate lists `signal_fire` (16 variants, each pair
    pointing at the owner's model for that facing and `lit`) where the owner ignores it with a custom state
    mapper, because this mod's asset check reads the default mapping; and the recipe's condition is Forge's
    `minecraft:item_exists` on our campfire instead of the owner's `CampfireCondition` factory (true in exactly
    the same games), so the owner's condition class is not copied.
*   **With the owner installed** nothing registers; the copied event handlers stay subscribed but act only on
    this package's `BlockCampfire`, which never exists then. Bees read the owner's campfire by name.
*   **Bees**: `api/BeeSmoke.isSmokeSource` recognizes `buzzybeesbackport:campfire` by name and `lit`, beside
    `villagerbackport:campfire`.

## Tested headlessly

`./gradlew runServer -Pselftest=all` on port 25808 (fresh world), then `python tools/check_assets.py`:
**`SELFTEST DONE pass=348 fail=0`** (campfire 86 of them, every other stream still passing), **5 blocks, 66
states, 7 item models, 121 sound files, 0 errors**. `./gradlew build` last; the jar's campfire classes carry
`func_` names.

*   **Owner absent** (`-Pselftest=campfire`: **`pass=86 fail=0`**): registered under
    `buzzybeesbackport:campfire` with its item, tile entity id, crackle sound, translation key, creative tab,
    hardness 2, axe; nothing under `villagerbackport:campfire`; `Owners.block` gives ours; crafted from coal or
    charcoal and any logs. Packet 35 encodes and decodes.
*   States: 16, each its own metadata, all surviving a metadata round trip; default lit, north, no signal;
    light 15 lit and 0 out; rotation; mobs path around a lit one only.
*   **By a real player through the server's right-click path**: placed lit and facing the way the player
    looks; a shovel from below does nothing, from above puts it out and wears; the tile entity survives; flint
    and steel lights it and wears; a water bucket puts it out, leaves an empty bucket and pours no water; a fire
    charge lights it and is used up.
*   Cooking: raw beef, potatoes, salmon and chicken are accepted, chorus fruit, iron ore and cooked beef are
    not, a stick does not go on; one beef from the player's stack goes on, is not done at 599 ticks and pops
    off as steak at 600; out, the timer runs back twice as fast and the food stays; relit it carries on; food
    and timers are saved; four spots, no fifth.
*   Breaking with an axe: 2 charcoal and what was cooking; Silk Touch drops the campfire.
*   Signal fire: placed on hay it is one; hay replaced, it is not; hay back, it is again, still lit.
*   Damage, through an entity's own movement: a pig in a lit campfire takes 1 and is not set alight; none from
    an unlit one, none to a zombie pigman, none with Frost Walker boots; an item lying in it is not burned.
*   Projectiles (Forge's impact events): a plain arrow does nothing, a burning arrow lights it, a splash water
    bottle puts it out, a small fireball lights it.
*   The owner's missing block and item mappings are remapped onto ours; the owner's other names are left alone.
*   **Bees**: `BeeSmoke.isSmokeSource` knows our lit campfire and not an unlit one; a hive 5 above our lit
    campfire is calm, 6 above is not, above an unlit one is not.
*   **Owner installed** (its 2.4.0 dev jar in `eclipse/mods`, fresh world, `-Pselftest=campfire,bees`:
    **`pass=109 fail=0`**): no copy registered ("The campfire comes from Village & Pillage Backport"),
    no `buzzybeesbackport:campfire`, lookups give the owner's, and the owner's lit campfire calms a hive 5 above.

## Not tested

*   Anything visual or audible (no client): the models and their animated fire, the smoke plumes and the
    dowsing burst (packet 35 is only encoded, decoded and sent to nobody), the food drawn on the logs, the
    crackle, lava sparks.
*   With Village & Pillage installed, a world with this mod's campfires: the owner has no remap of
    `buzzybeesbackport:campfire` onto its own yet (as with Update Aquatic's woodset copies), so such a world
    stops at FML's missing-blocks prompt. Rule 1 puts that remap in the owner (or Backport Core).
*   A real world that lost Village & Pillage: the remap is tested through the event, not by loading such a
    world. What was cooking on the owner's campfires is lost then (1.12.2 has no missing-mapping event for
    tile entities, so `villagerbackport:campfire` tile entities are skipped and ours start empty).

## For the integrator

*   `tweaks/DispenserTweaks.litProperty` lights only `villagerbackport:campfire` from a dispenser's fire
    charge; it should also accept `buzzybeesbackport:campfire` (not changed here: tweaks' file). Without it a
    dispensed fire charge still lights our campfire through the copied `CampfireEvents` fireball handler,
    but only with `mobGriefing` on.
*   Future MC's campfire alongside ours is not handled (stream F).
*   `TweaksSelfTest.mobs` leaves its zombies in the world, so a second `-Pselftest=all` run on the same
    `eclipse/selftest` world fails "and makes a baby zombie: expected 1, got 2" (the last run's baby is still
    there). Seen here; passes on a fresh world. Not this stream's file.
*   The campfire is not in Backport Core's `BlockOwnership` table; it could be declared there as
    `villagerbackport`'s.

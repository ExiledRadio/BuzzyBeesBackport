# Buzzy Bees Backport: plan

Everything Java Edition 1.15 (Buzzy Bees, with 1.15.1 and 1.15.2) added or changed, for 1.12.2 Forge, grouped
into streams. Each stream has its own package, config category, entity and packet id blocks, and worktree.

Source: minecraft.wiki's *Java Edition 1.15* page and the pages it links (*Bee*, *Bee Nest*, *Beehive*, *Honey
Block*, *Iron Golem*). Where current Java behaves differently from 1.15, current Java wins (marked **now:**).
Numbers the wiki leaves out come from 1.16.5's decompiled code (the bee code barely changed after 1.15.2; the
cached Forge 36.2.39 decompile, SRG names renamed with the 1.14.3 MCP snapshot) and, for older vanilla
behavior, 1.14.4's (`MC1144Reference`). Assets come from the 1.18.2 client jar and the 1.18 asset index
(`tools/assets.py`), whose bee, hive and honey textures and sounds are 1.15's.

## Scope at a glance

| In | Out (and why) |
| --- | --- |
| Bees with all their AI, bee nests and beehives, honey bottle, honeycomb, honey block, honeycomb block, their generation, harvesting, comparators, dispensers, particles, sounds and advancements | Data packs, predicates, `/data storage`, `/spectate`, `/schedule`, loot table functions, tags as files: 1.12.2 cannot take them and players never see them |
| Iron golem cracking and iron-ingot repair | 3D biomes, 64-bit particle positions, chunk loading and rendering performance, OpenGL 2.0: technical |
| 1.15's smaller visible changes that need no patches (stream X) | Hardcore's death screen buttons, narrator, debug screen, the title panorama, Yiddish: launcher or client chrome |
| | Mobs avoiding lava better, farmland under piston-placed blocks, hand-swing animations for twenty interactions, experience orb translucency, grass tint smoothing: small or pathfinding-deep; listed under X's *Not doing* |

## Content owned by another mod of the family

Village & Pillage Backport (`villagerbackport`) owns these, and 1.15 changed some of them:

| Thing | 1.15's change | Where it is done |
| --- | --- | --- |
| Campfire | Shovel puts it out; its smoke calms bees | Shovel: already in V&P. Calming: here, **Rule 2**: V&P's lit campfire within 5 blocks below a hive, read by registry name and its `lit` property when `villagerbackport` is installed; without it, vanilla fire directly under the hive (1.15's other calming rule). Fire charges from dispensers lighting a campfire: here (X), only if V&P lacks it |
| Bell | Rings when powered by redstone | Here (X), **Rule 2**: V&P's bell has no redstone handling; we ring it through its public ring method by reflection on a rising edge |
| Composter | Crafted from slabs | Already in V&P (`slabWood`) |
| Scaffolding | Burns for 2 items | V&P's; checked in X, raised by Forge's fuel event only if V&P left it lower |
| Stripped wood | Crafted from stripped logs | V&P's (and UA's copies); checked in X |
| Sweet berry bush | Grows in giant tree taigas; bees grow it | Generation: V&P (already current Java). Bee growth: here, by name and its `age` property |
| Foxes | Spawn in all taigas | V&P (already current Java) |
| Villagers | Nitwit badge removed; kicked out of a bed a player uses; librarian bookshelf trade | V&P (badge and bed already 1.15; trade is V&P's table) |
| Flowers: cornflower, lily of the valley, wither rose | Bees pollinate and breed with them; wither rose withers bees (**now:**, 1.21.4) | Here: any `BlockFlower` plus V&P's three by name |
| Iron golems | Cracking and repair | Here: V&P's golems are vanilla `EntityIronGolem`, so they crack and are repaired with no extra code. Its village golem spawner is unaffected |
| Enchantment glint's 1.15 look | Texture | Backport Core's texture packs (already there) |

Update Aquatic Backport (`updateaquaticbackport`) owns phantoms and drowned: `doInsomnia` and the drowned
baby-from-spawn-egg are Rule 2 integrations here, by registry name.

Later updates' bee changes stay with them: nests on mangrove (Wild Update), cherry trees and pink petals
(Trails & Tales), meadows and azaleas (Caves & Cliffs), soul campfire calming (Nether Update). They add their
trees, flowers and smoke sources through this mod's small API (`api/`), Rule 2, until Backport Core has a
shared registry (`CORE_REQUESTS.md`).

Nothing 1.15 added is owned elsewhere, so this mod declares `bee_nest`, `beehive`, `honey_block` and
`honeycomb_block` with `BlockOwnership.declare` and registers them unconditionally.

## Streams

Each stream owns one package under `com.exiledradio.buzzybeesbackport`, one config category with an `enabled`
switch (already in `ModConfig`), one block of packet ids and one block of entity ids. No stream calls another's
classes; one that needs another's item looks it up with `common/Owners.ours(name)`, null-safe.

| # | Stream | Package | Config | Entity ids | Packet ids | Port | Needs |
| --- | --- | --- | --- | --- | --- | --- | --- |
| H | Honey | `honey` | `honey` | none | 5-9 | 25801 | nothing |
| B | Bees, nests and hives | `bee` | `bees` | 1-4 | 10-14 | 25802 | H's items by name (null-safe) |
| G | Nest generation | `nestgen` | `nests` | none | 15-19 | 25803 | B's nest block by name |
| I | Iron golems | `golem` | `golems` | none | 20-24 | 25804 | nothing |
| X | Other 1.15 changes | `tweaks` | `tweaks` | none | 25-29 | 25805 | nothing |
| F | Future MC pairs and migration | `migrate` | `compat` | none | 30-34 | 25806 | Backport Core; states recorded in a real instance with Future MC |
| L | Languages (Rule 4) | none (`tools/lang`) | none | none | none | none | the family's language tool |

The main worktree's dev server uses port 25800. Entity ids are this mod's `EntityRegistry.registerModEntity`
numbers.

### H. Honey

- **Honey bottle**: food 6, saturation modifier 0.1 (1.2 saturation), drunk in 40 ticks, stacks to 16, leaves a
  glass bottle, removes Poison. Crafts into 3 sugar; 4 make a honey block, the bottles staying in the grid.
- **Honeycomb**: crafts beehives (with B) and honeycomb blocks (4).
- **Honey block** (hardness 0, slime-block sounds, the honey block's own sounds): 4 honey bottles, back into 4
  bottles with 4 glass bottles. Collision box inset 1/16 at the sides and 1/16 lower on top, so:
  - walking on it slows to 0.4x and sprinting stops; jumping from it is cut to 0.5x (`LivingJumpEvent`);
  - an entity touching a side while falling slides down slowly (capped fall speed, 1.16.5's numbers), with
    particles; the *Sticky Situation* advancement on the first slide;
  - landing on it takes a fifth of the fall damage (`onFallenUpon`);
  - mobs avoid walking on it (path node cost), it does not conduct redstone (not a normal cube);
  - pistons move it and the blocks touching it like slime (Forge's `isStickyBlock`), but not slime: patch
    P1 (`docs/PATCHES.md`);
  - an entity on a moving honey block moves with it (1.12.2's piston already carries entities);
  - stickiness through carpets and slabs (the collision under them is checked one block down).
- **Honeycomb block**: 4 honeycombs, hardness 0.6, decorative.
- Particles: dripping, falling and landing honey (client, also used by B's full hives).
- No compost values: 1.15 gave honey items none.

### B. Bees, nests and hives

- **Bee** (`bee`, 10 HP, 0.7 x 0.6, flying movement with a hovering navigator, speed 0.6 flying / 0.3 ground,
  falls slowly, no fall damage, arthropod for Bane of Arthropods). Babies, breeding with any flower (6000-tick
  cooldown, baby grows in 24000 ticks), following players holding a flower, hovering near still players.
- **AI**, each from 1.16.5's goals, rewritten for 1.12.2's `EntityAIBase` (bookkeeping on the entity tick, never
  in `shouldExecute`; goal lists edited only on join):
  - wander near the hive (about 22 blocks, **now:** 1.16), homeless bees searching for a hive within 20 blocks
    (vanilla's POI search, here a scan of loaded tile entities near the bee, cached);
  - pollinate: find a flower within 5 blocks, hover on it for over 400 ticks (cancelled by damage), gain nectar,
    pollen particles and texture; a remembered flower position;
  - grow crops: with nectar and a hive, about every 30 ticks a 1-in-30 chance to advance a crop 1-2 blocks below
    (`BlockCrops`, stems, V&P's sweet berry bush by name), at most 10 per nectar load;
  - go home: with nectar, at night, in rain, or after 2400 ticks out; enter from any side; hive full or gone
    makes it search again;
  - inside the hive: 2400 ticks minimum with nectar, 600 without; leaves only from the front, only if open;
    stays in at night and in rain (not in the Nether or End); each nectar bee leaving adds a honey level (1% two);
  - anger (**now:** random 400-799 ticks, the neutral-mob anger of 1.16): attacked bees and their hive mates
    within range turn on the attacker; red eyes; angry buzz;
  - sting: 2 damage (3 on Hard), Poison 10 s on Normal, 18 s on Hard, none on Easy/Peaceful; loses its stinger
    and dies after 1200 + rand(20) ticks (vanilla's check), particles; *was stung to death* death message;
  - water: avoids it; 1 damage every 10 ticks after 20 ticks in water;
  - wither rose: Wither when it touches one (**now:**).
- **Bee nest** (`bee_nest`, hardness 0.3, flammability 30/5, axe) and **beehive** (`beehive`, hardness 0.6,
  6 planks + 3 honeycombs): `facing` and `honey_level` 0-5, a tile entity with up to 3 bees (their whole NBT,
  ticks inside, minimum ticks) and a flower position.
  - Shears at level 5: 3 honeycombs (wear 1). Glass bottle at level 5: a honey bottle. Both reset to 0.
  - Calm: fire directly beneath, or a lit campfire up to 5 blocks below with the smoke not blocked (1.16.5's
    rule: at most one solid block, directly above the campfire). Otherwise harvesting angers the bees inside
    and within 8 blocks.
  - Breaking: without Silk Touch a nest drops nothing and releases angry bees; a hive drops itself empty and
    releases angry bees. With Silk Touch both drop themselves with their bees and honey level (**now:** no
    anger, 1.15.2). In Creative a hive with bees or honey drops itself with them.
  - Tooltip: bees and honey level (**now:**, 1.21.2).
  - Comparators read the honey level; pistons can't move them; burning releases the bees.
  - Dispensers: shears harvest honeycombs (dropped), glass bottles take a honey bottle.
  - Full hives drip honey below them (client).
- **Advancements**: *Bee Our Guest* (bottle without angering, campfire under it) and *Total Beelocation* (Silk
  Touch a nest with 3 bees), in vanilla's Husbandry tab through their `parent`, granted from code
  (`common/Advancements`; 1.12.2 lacks 1.15's triggers). *Two by Two* and *A Balanced Diet* gaining bees and
  honey: see X.
- Spawn egg. Sounds: 18 events (bee loop, aggressive loop, pollinate, sting, hurt, death, hive enter/exit/work,
  honey drink, honey block sounds...). Renderer, model and the three textures (normal, angry, nectar, each with
  a stingless variant) from 1.15's model, client-only.
- Entity id 1 (`bee`). Packets 10-14: only if entity data watchers cannot carry the state (they should).

### G. Nest generation

- On oak and birch trees in newly generated chunks: a post-population scan of the chunk's decoration area finds
  each tree's trunk base (a log on dirt or grass with leaves above) and, by the biome's chance, puts a nest with
  2-3 bees on the trunk's south side under the lowest leaves, where there is air in front (1.15's decorator rule).
  Plains and sunflower plains 5%, flower forest 2%, forest, birch forest and the tall birch forests 0.2%
  (**now:** 1.15.2's table). Modded biomes: Forge's dictionary (`PLAINS` without `SAVANNA`/`SNOWY`/`DRY` at 5%,
  `FOREST` with `DENSE`-less temperate forests at 0.2%), with a config list to override per biome.
- From saplings: an oak or birch sapling with a flower within 2 blocks at its own level grows a nest 5% of the
  time, any biome and dimension (`SaplingGrowTreeEvent` records it; the nest is placed after the tree, at the end
  of the same tick).
- API for later updates' trees and biomes (`api/BeeNests.addTree / addBiomeChance`).

### I. Iron golems

- Cracking at under 75%, 50% and 25% health: three overlay textures, a render layer added to vanilla's golem
  renderer at client init (works on any `EntityIronGolem`, V&P's village golems included), a crack sound at each
  new stage (`entity.iron_golem.damage`).
- Repair: right-click with an iron ingot heals 25 HP (one ingot from 1 HP to full is four), uses the ingot unless
  in Creative, sound `entity.iron_golem.repair` with pitch 1.0-1.4, refused at full health. Works on
  player-built and village golems alike (1.15 does both).
- 1.15's iron golem attack damage (7.5 + rand(15) as before; 1.15 only moved it to an attribute, invisible).

### X. Other 1.15 changes

Each behind its own switch in `tweaks`:

- **End stone bricks**: hardness 3, blast resistance 9 (also V&P's end stone brick slabs, stairs and walls by
  name, only if V&P left them lower).
- **Boats** burn for 6 items (1200 ticks), by Forge's fuel event.
- **Wet sponge** dries when placed in the Nether, with the sound and smoke.
- **Rails** are placed along the player's facing when there is nothing to connect to.
- **Large ferns** drop wheat seeds like large grass; **attached melon and pumpkin stems** drop seeds (checked
  against 1.12.2 first; only what differs is changed).
- **Spawn eggs** on adult zombies, husks, zombie pigmen and zombie villagers (and UA's drowned, by name) make
  babies.
- **Babies in spawn groups**: 5% for cows, sheep, pigs, chickens (and V&P's foxes, by name), 10% for wolves,
  llamas and horses, on natural chunk spawns (`LivingSpawnEvent.SpecialSpawn`).
- **Parrots** no longer imitate endermen, polar bears, wolves or zombie pigmen (their imitation map, by
  reflection at init).
- **Food in Creative**: every food, cake included, can be eaten in Creative.
- **Beds**: "Respawn point set" when a bed sets it; trying to sleep in the day sets the spawn point.
- **Game rules**: `drowningDamage`, `fallDamage`, `fireDamage`, `doImmediateRespawn` (the client skips the death
  screen), `doInsomnia` (stops UA's phantoms spawning, by name, when UA is installed).
- **Dispensers**: glass bottles fill from water in front (water bottles); armor stands placed; fire charges
  light V&P's campfire (Rule 2, if V&P does not already); firework rockets fly the way the dispenser faces.
- **Bells ring when powered** (V&P's bell, Rule 2).
- **Crafting table**: shift-clicking from the inventory moves the stack into the grid. Needs our own container
  for vanilla's crafting table (no patch); steps aside when the crafting table block is not vanilla's class (a
  mod that replaced it).
- **Elytra** start gliding right after a jump, while still rising (client tick + one packet in 25-29).
- **Toggle sneak and toggle sprint** (1.15's accessibility options), client config.
- **Splash texts**: the three 1.15 ones, added to the title screen's pick.
- **Advancements**: *Two by Two* needs bees and *A Balanced Diet* honey bottles, by editing the loaded vanilla
  advancements at server start through public API if that holds; dropped if it would need a patch.
- *Not doing*: mobs avoiding lava better, farmland under piston-placed blocks, hand-swing animations, experience
  orb translucency and their drop timing, grass tint smoothing, anvil and grindstone statistics, status effect
  sprites, parrots staying on the shoulder while riding (vanilla drops them in `EntityPlayer.onLivingUpdate`:
  needs a patch), iron doors needing a pickaxe (already true in 1.12.2), birch in dark forests (1.12.2 has it).

### F. Future MC pairs and migration

Future MC has bees, nests, hives, honey blocks, honeycomb blocks, honey bottles and honeycombs. With Backport
Core's `FutureMcTable` contributor and `DuplicateBlocks`: hide Future MC's duplicates (ours kept by default),
convert a world Future MC was removed from, `/futuremc convert`. Its bee entity: converted on chunk load to ours
with health, name, age, hive and flower positions; its generation and spawns turned off while ours runs. The
recorded states need `/futuremc dump` in a real instance with Future MC (the dev environment can't load its
coremod): `G:\VBPTest`-style instance, done last.

### L. Languages

Rule 4's eighteen languages through the family's tool (`tools/lang/build_langs.py`, Backport Core `languages`
branch until merged). Vanilla names from Mojang's 1.15+ files; our own text translated by the tool's process.

## Shared files and rules

As Update Aquatic's plan, which this mod follows exactly:

- **`ModConfig.java`**: every category exists from the scaffold; a stream edits only its own class. Category
  classes extend `Object`. Every field has `@Config.LangKey("config.buzzybeesbackport.<category>.<field>")` and a
  name and a `.tooltip` line in `en_us.lang`.
- **`NetworkHandler.java`**: packets only with ids from your block, in id order.
- **`BuzzyBeesBackport.java`**: at most one line per lifecycle hook per stream.
- **`selftest/SelfTestRunner.java`**: one line in `TESTS`.
- **`en_us.lang`, `sounds.json`**: written only by a stream's generator through `tools/assets.py`
  (`merge_lang` / `merge_sounds`), each in its own `# --- <stream> ---` block.
- **`common/`**: read freely; changes are the integrator's (this session).
- **Registration** through `common/Content`; other streams' items with `Owners.ours("name")`.
- **Docs**: `docs/streams/<stream>.md` with changelog section, how it works, tested, not tested.
- **Closest to vanilla.** Bytecode patches are allowed where vanilla parity needs them: this mod's own
  coremod (`asm`, one transformer per patch, each in `docs/PATCHES.md`), no MixinBooter.
- **Client-only types** only in `client/` classes, reached by a call that names no client type.

## Build and test

- `./gradlew build` deploys nowhere unless `bbbDeployDir` is set in `~/.gradle/gradle.properties`.
- `./gradlew runServer -Pselftest=<stream>` on the stream's port (its worktree's `eclipse/server.properties`),
  then `python tools/check_assets.py` with 0 errors, then `./gradlew build` last; the jar's classes must carry
  `func_` names.
- Free RAM checked before every Gradle run or server (8 GB minimum); Gradle and servers at `-Xmx2G`; never
  `cleanRunServer`; kill only our own server by PID.

## Integrations (Rule 2), all soft

- **V&P campfire** calms bees; **V&P bell** rings on redstone; **V&P sweet berry bushes** grow from bees; **V&P
  flowers** feed and are pollinated by bees; **V&P golems** crack and are repaired (vanilla entity).
- **V&P composter**: nothing (1.15 gave honey no compost value).
- **UA**: `doInsomnia` stops its phantoms; spawn eggs make baby drowned.
- **Backport Core**: ownership declarations, Future MC tables, (later) a shared smoke-source and flower registry.

## Decided

0. **Waterlogging** (Backport Core 1.1.0 over Fluidlogged API): none of 1.15's blocks is waterloggable in
   vanilla (bee nest, beehive, honey block, honeycomb block), so none uses Core's `Waterloggable`. The campfire
   copy stays identical to Village & Pillage's, and follows it when that mod makes its campfire waterloggable.

1. **Honey and slime do not stick to each other**, as in vanilla: the family's rule is closest to vanilla,
   with bytecode patches where needed (2026-11-07). Patch P1 in `docs/PATCHES.md`.

## Order and status

1. Scaffold, this plan: **done**.
2. H honey, B bees/nests/hives, G nest generation, I iron golems: **done** (see `docs/streams/`).
   Patch P1 (honey and slime): **done** (`docs/PATCHES.md`).
3. X other 1.15 changes: **first pass done** (fourteen changes); the rest is listed in `docs/streams/tweaks.md`.
4. L languages: **first pass done** (Mojang's names in all 18); our own 57 keys still to translate.
5. W (new, from the family's "every mod works on its own" rule): an identical copy of Village & Pillage's
   campfire, registered only when it is absent, so bees can be calmed by a campfire without it. Fire under
   the hive calms them meanwhile.
6. F Future MC pairs: needs a real instance with Future MC to record its states with `/futuremc dump`.

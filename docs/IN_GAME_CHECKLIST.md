# Buzzy Bees Backport 1.0.0: what needs a look in game

Everything server-side is covered by the headless self-test (437 checks alone, 366 with Village & Pillage
2.4.0). These need a real client, which only the user can look at:

## Bees
*   The bee model: body, stinger (gone after a sting), antennae, wings beating while flying and folded at
    rest, the bob while flying, the roll toward a close target, babies at half size.
*   The four textures: normal, angry (red eyes), with nectar (pollen dots), angry with nectar.
*   The flight buzz getting louder and higher with speed, switching to the angry buzz; pollinating, sting,
    hurt and death sounds; subtitles.
*   Nectar drops falling from pollinated bees.

## Nests and hives
*   Nest and hive models (empty and full fronts, all four facings), the item models and the tooltip
    ("Bees: 2 / 3", "Honey: 5 / 5").
*   Honey dripping from a full hive's underside and landing with its sound; enter, exit, work and shear sounds.

## Honey
*   The honey block: translucent, the inner cube, no faces between two honey blocks; slide and landing
    particles; the honey sounds; the honey drink sound.
*   Honey bottle and honeycomb item textures; the honeycomb block.
*   Standing on a honey block pushed sideways by a piston (the client half of the carrying).

## Iron golems
*   The three crack stages drawn over a golem (log line "Iron golem cracks added to ..."), the crack and
    repair sounds.

## Campfire (only without Village & Pillage)
*   Model and animated fire, smoke plumes (higher over hay), the dousing burst, food drawn on the logs,
    the crackle and lava sparks.

## Other 1.15 changes
*   The three splash texts on the title screen (they come up rarely, as vanilla's do).
*   The death screen closing by itself with `doImmediateRespawn` on.
*   Toggle sneak and toggle sprint (config `tweaks.toggleSneak` / `toggleSprint`, off by default).
*   Elytra opening on a jump press while still rising.
*   Firework rockets from a dispenser flying the way it faces, as seen by the client.
*   Parrots staying on the shoulder while riding.
*   The Two by Two and A Balanced Diet advancement screens listing bees and honey bottles.
*   "Respawn point set" in chat when using a bed.

## Languages
*   A look at a few languages in game; the Spanish text read by a native speaker (translator notes in the
    session report: "Activado", "picarse"/"minarse", "Textos de inicio", decimals in es_mx copies).

## Not tested anywhere yet
*   The five patches in a real (SRG-named) instance next to other coremods; Fluidlogged API loaded fine with
    them in the dev environment.
*   Future MC installed beside this mod (duplicate hiding, `/futuremc check`), and a real world it was
    removed from.

## Look and feel (with Backport Core 1.2.0)

Self-tests: `gradlew runServer -Pselftest=lookfeel` (registration, assets, splash pool, the two options, lang)
and `gradlew runClient -Pselftest=client` (title screen, options kept equal to the config, window title; log only).

- [ ] **Title screen**: 1.15's panorama (bees and nests in a flower forest) turns slowly, with 1.13's gradient over
      it. With a newer update backport installed, the newer panorama shows instead.
- [ ] **Splashes**: "Buzzy Bees!", "In case it isn't obvious, foxes aren't players.", "Minecraft Java Edition
      presents: Disgusting Bugs" turn up over several launches.
- [ ] **Accessibility Settings**: after Auto-Jump, "Sneak: Hold" and "Sprint: Hold"; clicking switches to Toggle,
      and the mod's config screen (tweaks) shows the same; sneak then stays on after one press.
- [ ] **Hardcore death**: the buttons read "Spectate world" and "Title screen"; Title screen saves and leaves to
      the title screen, and the world is still in the list.
- [ ] **Window title**: "Minecraft* 1.12.2" on the title screen, "... - Singleplayer" in a world, "... - Multiplayer
      (3rd-party)" on a server, "... - Multiplayer (LAN)" in a world opened to LAN.
- [ ] **Chat link prompt**: "Open in Browser".
- [ ] World loading still shows 1.14's chunk grid; menu music unchanged (1.15 kept menu1 to menu4).

## 1.15's pieces in Backport Core's finished presentation module (Core 1.2.0)

- [ ] **Buttons**: white labels, hovered or not, and 1.15's white outline on the hovered button (widgets.png).
- [ ] **Startup**: the white Mojang screen with a full red bar as loading ends, fading into the title screen.
- [ ] **Multiplayer** from the title screen: "Online play is not rated" with Proceed / Back and "Do not show this
      screen again" (1.15.2's check box); ticked, it never shows again.
- [ ] **Hardcore death**: the second button reads "Title screen" and leaves the world (saved) for the title screen.
- [ ] **F3**: the second line has no "(N chunk updates)".
- [ ] **Text**: "Open in Browser" on a chat link.

## Creative tabs (with Backport Core 1.1.0)

The places are in `docs/CREATIVE_TABS.md`; a dev client already checked the lists, the search tab and JEI.

- [ ] There is no tab for this mod; every vanilla tab keeps its icon, name, background and scroll bar.
- [ ] A few places: bee nest and beehive at the end of Decoration Blocks, the honey block in Redstone near the pistons, the honey bottle at the end of Foodstuffs, the bee egg after the bat egg.
- [ ] The search tab finds the mod's items, and JEI lists them.
- [ ] Other mods' items are still in their tabs, in their own order.
- [ ] With another update backport installed too, its items and this mod's sit together by kind (stripped logs, slabs).

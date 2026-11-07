# Bytecode patches

The family's standing rule (2026-11-07): closest to vanilla, and bytecode patching is allowed where vanilla
parity needs it. This mod has its own small coremod, `asm/BuzzyBeesCore` (no MixinBooter), with one
transformer per patch. Each patch below says what it changes, why no event or API could do it, how it fails
safe, and how it is tested.

The coremod is named in the jar's manifest (`FMLCorePlugin`, `FMLCorePluginContainsFMLMod: true`) and, for
`runServer`/`runClient`, by `-Dfml.coreMods.load` (`build.gradle`). Transformers match both MCP and SRG
method names, so the same code runs in the dev environment and in a real instance. A transformer that does
not find the exact shape it expects changes nothing and logs a warning; it never half-patches a class.

## P1. Honey and slime do not stick to each other

*   **Class**: `net.minecraft.block.state.BlockPistonStructureHelper`. **Transformer**:
    `asm/PistonStickTransformer`. **Hooks**: `honey/PistonStick`. **Since**: 0.1.0.
*   **Vanilla 1.15**: `PistonBlockStructureHelper.canStick` - a honey block and a slime block side by side do
    not pull each other; every other pair touching a sticky block is pulled. Flying machines rely on it.
*   **Why a patch**: 1.12.2 Forge only asks a block `isStickyBlock(state)`, with no neighbor, and drags
    everything touching a sticky block. No event fires while the piston works out what to move.
*   **What changes** (two call sites, nothing else):
    1.  `addBranchingBlocks`: its one `addBlockLine(neighbor, side)` call becomes
        `PistonStick.branch(this, neighbor, side)`, which returns "fine, nothing to add" for a neighbor that
        cannot stick to the sticky block, and otherwise calls `addBlockLine` as before.
    2.  `addBlockLine`: the second of its three `Block.isAir` calls, the one in the loop that collects the
        blocks a sticky block pulls behind it, becomes `PistonStick.airOrUnstuck(block, state, world, pos,
        this)`: air as before, or a block that cannot stick to the one in front of it, which ends the line
        as 1.15 does.
*   **Fails safe**: if either method is missing, or the counts are not exactly one `addBlockLine` call and
    three `isAir` calls, nothing is patched and `P1 ... patch skipped` is logged. Honey then sticks to slime,
    as 1.12.2 would.
*   **Other mods**: anything else touching this class keeps working as long as those two calls are where
    vanilla has them. Fluidlogged API does not transform this class (it is about fluids, not piston
    structures); not yet checked against it in a real instance.
*   **Tested** (`-Pselftest=honey`): the patch reports itself applied; honey pushed beside slime leaves the
    slime and still drags stone; slime pushed beside honey leaves the honey and still drags slime; honey
    dragged sideways pulls the honey behind it but not slime.

## P2. Shift-clicking into the crafting grid

*   **Class**: `net.minecraft.inventory.ContainerWorkbench`. **Transformer**: `asm/WorkbenchShiftTransformer`.
    **Hooks**: `tweaks/CraftingShift`. **Switch**: `tweaks.craftingShiftClick`. **Since**: 0.1.0.
*   **Vanilla 1.15**: `WorkbenchContainer.transferStackInSlot` - a stack shift-clicked in the inventory or
    hotbar goes into the crafting grid (merging, then the first empty slot); only if nothing fits does it
    move between inventory and hotbar as before.
*   **Why a patch**: the rule lives in the container's own method; replacing vanilla's container would
    break every mod that opens, extends or looks for `ContainerWorkbench`. No event fires on shift-click.
*   **What changes**: of `transferStackInSlot`'s four `mergeItemStack` calls, the second (inventory to
    hotbar, `37, 46, false`) and the third (hotbar to inventory, `10, 37, false`) become
    `CraftingShift.merge(this, stack, start, end, reverse)`: the grid (`1, 10, false`) first, then the
    original call, as 1.15's `if (!merge(grid)) { old }`. Taking the result and emptying the grid are
    untouched. Subclasses that call `super.transferStackInSlot` get it too, with vanilla's slot layout.
*   **Fails safe**: unless there are exactly four calls and the two have exactly those constants, nothing is
    patched and `P2 ... patch skipped` is logged; shift-click then behaves as 1.12.2's.
*   **Tested** (`-Pselftest=tweaks`): applied; inventory cobblestone goes to grid slot 1; hotbar planks to
    the next free slot and then merge with them; with all nine grid slots taken by other items, dirt goes
    from the inventory to the hotbar; switched off, the grid is skipped.

## P3. Elytra open while rising (server)

*   **Class**: `net.minecraft.network.NetHandlerPlayServer`. **Transformer**: `asm/ElytraStartTransformer`.
    **Hooks**: `tweaks/ElytraStart`. **Switch**: `tweaks.elytraJump`. **Since**: 0.1.0.
*   **Vanilla 1.15**: `PlayerEntity.tryToStartFallFlying` - off the ground, not already gliding, not in
    water, no Levitation, a usable elytra. 1.12.2 also requires `motionY < 0` (falling).
*   **Why a patch**: the test is inside `processEntityAction`'s `START_FALL_FLYING` case; no event fires.
    A packet of our own would have worked for this mod's client only and duplicated vanilla's handling.
*   **What changes**: the method's single `player.motionY` read becomes `ElytraStart.fallSpeed(player)`:
    `-1` (falling) while switched on, or `+1` with Levitation (refused, as 1.15); the real `motionY`
    switched off. The rest of vanilla's test (ground, gliding, water, elytra) is unchanged.
*   **Client half** (no patch): `tweaks/client/ElytraClient` sends vanilla's `START_FALL_FLYING` packet in the
    case 1.12.2's client leaves out (a fresh jump press while rising or level), from Forge's input event,
    which fires just before vanilla's own test with the same motion and ground state.
*   **Fails safe**: unless the method has exactly one `motionY` read, on the `EntityPlayerMP` field, nothing is
    patched and `P3 ... patch skipped` is logged; the server then refuses a rising start and closes the
    elytra, as 1.12.2 does.
*   **Tested**: applied; a rising player with elytra starts gliding, a falling one too; not with Levitation,
    not without elytra; switched off, rising is refused and falling still works. The client half is not
    tested headlessly.

## P4. Dispensed firework rockets fly straight

*   **Class**: `net.minecraft.entity.item.EntityFireworkRocket`. **Transformer**:
    `asm/FireworkAngleTransformer`. **Hooks**: `tweaks/DispensedFireworks`. **Switch**:
    `tweaks.dispensedFireworks`. **Since**: 0.1.0.
*   **Vanilla 1.15**: a rocket "shot at an angle" (from a dispenser or a crossbow) skips the speed-up
    (`motion * (1.15, 1, 1.15) + (0, 0.04, 0)`) every tick and flies straight at the speed it was shot with.
    The dispenser shoots it the way it faces at 0.5 with inaccuracy 1.
*   **Why a patch**: the speed-up is in `onUpdate`, before `move`, on both sides; a rocket shot sideways
    without it curves up and speeds off within a few ticks, and the client would disagree with the server.
*   **What changes**: the three statements of the speed-up (`motionX *= 1.15`, `motionZ *= 1.15`,
    `motionY += 0.04`) become `DispensedFireworks.accelerate(this)`, which does the same for any rocket not
    shot at an angle. Labels and line numbers are kept; the removed range has no jumps or frames.
*   **The flag**: 1.12.2's rocket has none, so it rides in the rocket's firework item (a
    `BuzzyBeesShotAtAngle` compound with the launch motion), which the rocket saves and sends to clients.
    Its `Fireworks` tag (explosions, flight time) is untouched. The client takes the launch motion on the
    rocket's first tick, since the spawn packet carries none for rockets.
*   **Fails safe**: unless the method has exactly two `1.15`, one `0.04` and one `move` call, in that shape,
    nothing is patched and `P4 ... patch skipped` is logged; the dispenser behavior is then not replaced
    (rockets go straight up as in 1.12.2). The behavior is also left alone if another mod replaced
    vanilla's.
*   **Tested**: applied; a dispenser facing east fires one rocket, using one, at 0.5 east from its face, which
    keeps its speed and moves 0.5 a tick; facing up, 0.5 upward and steady; a rocket made otherwise still
    gains 0.04 a tick and is not marked.

## P5. Parrots stay on the shoulder while riding

*   **Class**: `net.minecraft.entity.player.EntityPlayer`. **Transformer**: `asm/ShoulderRidingTransformer`.
    **Hooks**: `tweaks/ShoulderParrots`. **Switch**: `tweaks.parrotsWhileRiding`. **Since**: 0.1.0.
*   **Vanilla 1.15**: `PlayerEntity.livingTick` drops shoulder parrots on falling more than 0.5, water,
    flying or sleeping; 1.12.2 also drops them when the player rides anything.
*   **Why a patch**: the test is inline in `onLivingUpdate`; no event fires when shoulder entities drop.
*   **What changes**: the `isRiding()` call of that test (the last one before the single
    `spawnShoulderEntities()` call, with only `isInWater()` between them) becomes
    `ShoulderParrots.ridingDrops(this)`: false while switched on, `isRiding()` switched off. The method's
    other `isRiding()` calls are untouched.
*   **Fails safe**: if there is not exactly one `spawnShoulderEntities()` call with that `isRiding()` just
    before it, nothing is patched and `P5 ... patch skipped` is logged; parrots then jump off as in 1.12.2.
*   **Tested**: applied; a parrot on a player's shoulder stays through a tick in a boat; switched off, it
    drops.

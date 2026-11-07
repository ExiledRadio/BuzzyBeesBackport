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

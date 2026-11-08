# Creative tabs

Buzzy Bees Backport has no creative tab of its own. Each block and item is in the vanilla tab Minecraft 1.19.2 gives it, next to its vanilla neighbor in 1.19.2's order, through Backport Core's creative placement (`creative.CreativePlacement`; the rule and the table are in Backport Core's `docs/CREATIVE_TABS.md`). Items are placed by their vanilla name: `common/Content` places every block with an item and every item, and `bee/BeeContent` the bee's spawn egg.

"After" and "Before" are the stacks either side of it with this mod and Backport Core alone, from a dev client's `-Dbackportcore.creativeDump` (an item is put after its nearest 1.19.2 neighbor before it, or before the one after it, whichever is of its kind). With other backports installed an item moves to stay by its 1.19.2 neighbor: stripped logs follow the warped stem once the Nether Update adds it, and so on.

## Decoration Blocks

| Item | After | Before |
|---|---|---|
| `campfire` | `end_crystal` | `bee_nest` (this mod) |
| `bee_nest` | `campfire` (this mod) | `beehive` (this mod) |
| `beehive` | `bee_nest` (this mod) | `honeycomb_block` (this mod) |
| `honeycomb_block` | `beehive` (this mod) | (end of the tab) |

## Redstone

| Item | After | Before |
|---|---|---|
| `honey_block` | `acacia_fence_gate` | `observer` |

## Miscellaneous

| Item | After | Before |
|---|---|---|
| `bee spawn egg` | `bat spawn egg` | `blaze spawn egg` |
| `honeycomb` | `music_disc_wait` | (end of the tab) |

## Foodstuffs

| Item | After | Before |
|---|---|---|
| `honey_bottle` | `beetroot_soup` | (end of the tab) |

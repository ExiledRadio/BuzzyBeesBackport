# What Buzzy Bees Backport needs from Backport Core

Written by Buzzy Bees Backport for Backport Core's owner. Backport Core's repo is not edited from here; each
item says what this mod does meanwhile.

## 1. Smoke sources (for bees, and every later campfire)

Bees are calmed by a lit campfire under their hive. Village & Pillage owns the campfire, Nether Update will own
the soul campfire, and other mods have campfires of their own. One registry lets every family mod ask "is this
block smoking?" without knowing the others.

```java
package com.exiledradio.backportcore.smoke;

public final class SmokeSources {
    /** Registers a test for "this state gives off campfire smoke". Pre-init or init. */
    public static void register(String modid, java.util.function.Predicate<IBlockState> smoking);
    /** @return true if any registered test says this state smokes */
    public static boolean isSmoking(IBlockState state);
    /**
     * 1.15's CampfireBlock.isSmokeyPos: a smoking block within `depth` blocks below `pos`, the smoke blocked
     * by any solid block except one directly above the campfire.
     */
    public static boolean isSmokeyPos(World world, BlockPos pos, int depth);
}
```

Village & Pillage would register its campfire (`BlockCampfire.isSmokeSource`), Nether Update its soul campfire.

**Meanwhile**: `bee/Smoke.java` here checks `villagerbackport:campfire`'s `lit` property by name, plus anything
added through this mod's `api/BeeSmoke.addSource`.

## 2. Flower and crop "tags"

1.15 added the `flowers`, `tall_flowers`, `crops` and `bee_growables` block tags; later updates add to them
(azaleas, pink petals, torchflowers, cave vines). 1.12.2 has no tags. A small shared registry:

```java
package com.exiledradio.backportcore.tag;

public final class BlockTags {
    public static final String FLOWERS = "flowers", TALL_FLOWERS = "tall_flowers",
            CROPS = "crops", BEE_GROWABLES = "bee_growables";
    public static void add(String tag, java.util.function.Predicate<IBlockState> member);
    public static boolean is(String tag, IBlockState state);
}
```

with vanilla's 1.12.2 members filled in by Core (`BlockFlower`, the four double-plant flowers, `BlockCrops`,
`BlockStem`).

**Meanwhile**: `api/BeeFlowers` here, with the same shape.

## 3. 1.15's vanilla retextures for the texture packs

For the `modern` pack (`tools/modern-textures`): 1.15 changed

- the dragon fireball (`entity/enderdragon/dragon_fireball`), now like the fire charge;
- the ender dragon: no separate wing-bottom texture, the black dot on the wing removed (`entity/enderdragon/dragon`);
- the highlighted button (`gui/widgets`): white outline, no blue tint (1.15's look);
- the enchantment glint (already in the pack).

New textures this mod adds itself (iron golem cracks, bees, hives) are its own, not Core's.

## 4. Nothing needed now, noted for later

- **Ownership**: `BlockOwnership.declare("buzzybeesbackport", "bee_nest", "beehive", "honey_block",
  "honeycomb_block")` is enough as it is.
- **Future MC**: the existing `FutureMcTable` and `DuplicateBlocks` cover blocks and items. Future MC's bee is an
  entity: converting entities on chunk load is not in Core yet. Stream F does it here; if another family mod
  needs entity conversion too (Future MC's foxes and pandas are V&P's), it should move to Core as
  `FutureMcTable.Contributor.entity(theirId, (oldNbt) -> newEntity)`.

## 5. Future MC: tile data, not only items

`FutureMcConvert.fill` moves only items into a counterpart's tile entity. Hives keep bees and a honey level.
Wanted: `FutureMcTable.Contributor.tileData((TileEntity ours, NBTTagCompound theirs) -> void)`, called by both
`fill` paths (the removed-mod chunk pass and `/futuremc convert`).

**Meanwhile**: `migrate/FutureMcBees` covers the removed-mod path itself, after Core's pass on the same
chunk-load event. `/futuremc convert` with the other mod installed still loses bees and honey inside hives.

## 6. The campfire in the ownership table

This mod now carries an identical copy of Village & Pillage's campfire (`buzzybeesbackport:campfire`), registered
only without it. `BlockOwnership` should list `campfire` as Village & Pillage's, and the owner-side remap of
other mods' copies (the half of Rule 1 not built yet) should include it: a world with our campfires stops at
FML's missing-blocks prompt when Village & Pillage is added later. Same gap as Update Aquatic's copies.

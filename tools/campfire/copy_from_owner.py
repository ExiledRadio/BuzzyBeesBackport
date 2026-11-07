"""Copies the owner's campfire (Village & Pillage Backport) into this mod, so our copy matches the owner's exactly.

The family's rule "every mod fully works on its own": bees are calmed by a lit campfire under the hive, and the
campfire belongs to Village & Pillage. When it is absent this mod registers an identical copy under
buzzybeesbackport:campfire (same states, same metadata, same registry path). Copying the owner's classes and
assets - instead of rewriting them - is what keeps them the same. Rerun after the owner changes its campfire.

Copied, with only the package, the mod class and the mod id renamed:
  BlockCampfire, CampfireEvents, CampfireRecipes, PacketCampfireDowse, TileEntityCampfire,
  client/CampfireParticles, client/ParticleCampfireSmoke, client/TileEntityCampfireRenderer;
  the blockstate, models, textures, smoke particle frames, crackle sounds and the crafting recipe.
This mod's own (not copied, because registration differs): CampfireContent and client/CampfireClient.
Not copied: CampfireCondition (the recipe uses Forge's minecraft:item_exists instead, which is true exactly
when CampfireContent registered the campfire).

Two mechanical differences, both forced by this mod's shared files, are applied by rename() below:
  * NetworkHandler.channel() is a public static field here: NetworkHandler.channel.
  * The packet's id is 35 here (this stream's block 35-39), 18 in the owner.
The blockstate also lists signal_fire explicitly (16 variants, each signal_fire value pointing at the owner's
model for that facing and lit), where the owner ignores it with a custom state mapper: this mod's asset check
reads the default state mapping, and the drawn result is the same.

    python -I tools/campfire/copy_from_owner.py [path to the Village & Pillage workspace]
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

OWNER = sys.argv[1] if len(sys.argv) > 1 else "E:/MincraftModding/Workspaces/RLCraftVillagerTrading-core-extraction"
OWNER_ID = "villagerbackport"
OWNER_PKG = "com.exiledradio.villagerbackport"
OUR_PKG = "com.exiledradio.buzzybeesbackport"
OWNER_JAVA = OWNER + "/src/main/java/com/exiledradio/villagerbackport/campfire"
OWNER_ASSETS = OWNER + "/src/main/resources/assets/villagerbackport"
OUR_JAVA = os.path.join(assets.ROOT, "src", "main", "java", "com", "exiledradio", "buzzybeesbackport", "campfire")

CLASSES = ["BlockCampfire", "CampfireEvents", "CampfireRecipes", "PacketCampfireDowse", "TileEntityCampfire",
           "client/CampfireParticles", "client/ParticleCampfireSmoke", "client/TileEntityCampfireRenderer"]

HEADER = ("// Copied from the owning mod (Village & Pillage Backport) by tools/campfire/copy_from_owner.py. Do not\n"
          "// edit here: the copy must stay identical to the owner's campfire, so the owner can take ours over by\n"
          "// renaming it. Only the package, the mod class, the mod id and the packet channel accessor differ.\n")

# The config category, mirrored from the owner's ModConfig.Campfire: field, display name, comment (one line each,
# joined with spaces for the config screen's tooltip). ModConfig.Campfire carries the same comments.
CONFIG = [
    ("enabled", "Enabled",
     "Whether this mod adds its copy of the campfire when Village & Pillage Backport is not installed. "
     "With it installed, its campfire is used and this does nothing."),
    ("cookTime", "Cooking Time",
     "How long one item takes to cook, in ticks. 1.14's is 600 (30 seconds), three times a furnace."),
    ("extraFoods", "Extra Foods",
     "What the campfire cooks. By default: every furnace recipe whose input and output are both food, "
     "which is 1.14's own list (raw meat, raw fish, potatoes) plus any mod's raw foods. "
     "Add an item the default leaves out as 'modid:item' (cooked as the furnace would) or "
     "'modid:item=modid:result' (cooked into that). Add ':meta' after either to pick one variant, "
     "e.g. 'minecraft:fish:1=minecraft:cooked_fish:1'."),
    ("excludedFoods", "Excluded Foods",
     "Items the campfire refuses, even though the rule above would cook them, as 'modid:item' or "
     "'modid:item:meta'."),
]


def rename_java(text):
    text = text.replace(OWNER_PKG, OUR_PKG)
    text = re.sub(r"\bVillagerBackport\b", "BuzzyBeesBackport", text)
    text = text.replace("NetworkHandler.channel()", "NetworkHandler.channel")
    text = text.replace(" * Packet 18: ", " * Packet 35: ")
    return text


def rename(text):
    return text.replace(OWNER_ID + ":", assets.MODID + ":")


def copy_classes():
    for c in CLASSES:
        src = open("%s/%s.java" % (OWNER_JAVA, c), encoding="utf-8").read()
        out = rename_java(src)
        if OWNER_ID in out or "VillagerBackport" in out:
            raise SystemExit("%s still names the owner after renaming" % c)
        path = os.path.join(OUR_JAVA, *c.split("/")) + ".java"
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(HEADER + out)


def copy_texture(tex):
    tpath = tex.split(":", 1)[1]
    found = False
    for ext in (".png", ".png.mcmeta"):
        src = "%s/textures/%s%s" % (OWNER_ASSETS, tpath, ext)
        if os.path.exists(src):
            assets.write("textures/%s%s" % (tpath, ext), open(src, "rb").read())
            found = True
    if not found:
        raise SystemExit("owner has no texture " + tex)


def copy_model(path):
    data = json.load(open("%s/models/%s.json" % (OWNER_ASSETS, path), encoding="utf-8"))
    for tex in data.get("textures", {}).values():
        if tex.startswith(OWNER_ID + ":"):
            copy_texture(tex)
    if data.get("parent", "").startswith(OWNER_ID + ":"):
        raise SystemExit("owner model parent not handled: " + data["parent"])
    assets.write("models/%s.json" % path, rename(json.dumps(data, indent=2)) + "\n")


def copy_assets():
    state = json.load(open(OWNER_ASSETS + "/blockstates/campfire.json", encoding="utf-8"))
    variants = {}
    models = set()
    for key, value in state["variants"].items():
        models.add(value["model"])
        for signal in ("false", "true"):
            variants["%s,signal_fire=%s" % (key, signal)] = value
    assets.write("blockstates/campfire.json", rename(json.dumps({"variants": variants}, indent=2)) + "\n")
    for m in sorted(models):
        copy_model("block/" + m.split(":", 1)[1])
    copy_model("item/campfire")
    for i in range(12):
        copy_texture("%s:particle/big_smoke_%d" % (OWNER_ID, i))

    sounds = json.load(open(OWNER_ASSETS + "/sounds.json", encoding="utf-8"))
    crackle = json.loads(rename(json.dumps(sounds["block.campfire.crackle"])))
    crackle["subtitle"] = "subtitles.%s.block.campfire.crackle" % assets.MODID
    for s in sounds["block.campfire.crackle"]["sounds"]:
        path = (s["name"] if isinstance(s, dict) else s).split(":", 1)[1]
        assets.write("sounds/%s.ogg" % path, open("%s/sounds/%s.ogg" % (OWNER_ASSETS, path), "rb").read())
    assets.merge_sounds("campfire", {"block.campfire.crackle": crackle})

    recipe = json.load(open(OWNER_ASSETS + "/recipes/campfire.json", encoding="utf-8"))
    recipe["conditions"] = [{"type": "minecraft:item_exists", "item": assets.MODID + ":campfire"}]
    assets.write("recipes/campfire.json", rename(json.dumps(recipe, indent=2)) + "\n")

    m = assets.MODID
    lines = ["config.%s.campfire=%s" % (m, assets.name("block.minecraft.campfire")),
             "config.%s.campfire.tooltip=The campfire, when Village & Pillage Backport is not installed." % m]
    for field, title, comment in CONFIG:
        lines.append("config.%s.campfire.%s=%s" % (m, field, title))
        lines.append("config.%s.campfire.%s.tooltip=%s" % (m, field, comment))
    lines.append("tile.%s.campfire.name=%s" % (m, assets.name("block.minecraft.campfire")))
    lines.append("subtitles.%s.block.campfire.crackle=%s" % (m, assets.name("subtitles.block.campfire.crackle")))
    assets.merge_lang("campfire", lines)
    print("copied %d classes, %d block models, %d variants" % (len(CLASSES), len(models), len(variants)))


if __name__ == "__main__":
    copy_classes()
    copy_assets()

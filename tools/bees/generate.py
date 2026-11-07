"""Bee stream assets: nest and hive blockstates and models, bee textures, sounds, names, the beehive recipe
and the two advancements.

    python tools/bees/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs(model):
    return model.replace(":block/", ":")


for block in ("bee_nest", "beehive"):
    empty = assets.port_model("block/%s" % block)
    full = assets.port_model("block/%s_honey" % block)
    vanilla = assets.jar_json("assets/minecraft/blockstates/%s.json" % block)
    variants = {}
    for key, v in vanilla["variants"].items():
        out = dict(v)
        out["model"] = bs(full if v["model"].endswith("_honey") else empty)
        variants[key] = out
    assets.write_json("blockstates/%s.json" % block, {"variants": variants})
    assets.item_from_block(block)

for tex in ("bee", "bee_angry", "bee_nectar", "bee_angry_nectar", "bee_stinger"):
    assets.texture("entity/bee/%s" % tex)

events = {}
for e in ["entity.bee.loop", "entity.bee.loop_aggressive", "entity.bee.pollinate", "entity.bee.sting",
          "entity.bee.hurt", "entity.bee.death", "block.beehive.enter", "block.beehive.exit",
          "block.beehive.work", "block.beehive.shear", "block.beehive.drip"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("bees", events)

assets.write_json("recipes/beehive.json", {
    "conditions": [{"type": "minecraft:item_exists", "item": M + ":honeycomb"},
                   {"type": "minecraft:item_exists", "item": M + ":beehive"}],
    "type": "minecraft:crafting_shaped", "pattern": ["PPP", "HHH", "PPP"],
    "key": {"P": {"type": "forge:ore_dict", "ore": "plankWood"}, "H": {"item": M + ":honeycomb"}},
    "result": {"item": M + ":beehive"}})


def advancement(path, icon, name):
    assets.write_json("advancements/husbandry/%s.json" % path, {
        "parent": "minecraft:husbandry/root",
        "display": {
            "icon": {"item": icon},
            "title": {"translate": "advancements.%s.%s.title" % (M, name)},
            "description": {"translate": "advancements.%s.%s.description" % (M, name)},
            "frame": "task", "show_toast": True, "announce_to_chat": True, "hidden": False},
        "criteria": {name: {"trigger": "minecraft:impossible"}},
        "requirements": [[name]]})


# Granted from code (common/Advancements): 1.12.2 has neither 1.15 trigger.
advancement("safely_harvest_honey", M + ":honey_bottle", "safely_harvest_honey")
advancement("silk_touch_nest", M + ":bee_nest", "silk_touch_nest")

names = {
    "tile.%s.bee_nest.name" % M: assets.name("block.minecraft.bee_nest"),
    "tile.%s.beehive.name" % M: assets.name("block.minecraft.beehive"),
    "entity.%s.bee.name" % M: assets.name("entity.minecraft.bee"),
    "death.attack.%s.sting" % M: assets.name("death.attack.sting"),
    "death.attack.%s.sting.player" % M: assets.name("death.attack.sting.player"),
}
for a in ("safely_harvest_honey", "silk_touch_nest"):
    for part in ("title", "description"):
        names["advancements.%s.%s.%s" % (M, a, part)] = assets.name("advancements.husbandry.%s.%s" % (a, part))
ours = [
    # Our own text (current Java shows these since 1.21.2; 1.18's files have no line for them).
    "tooltip.%s.beehive.bees=Bees: %%s / %%s" % M,
    "tooltip.%s.beehive.honey=Honey: %%s / %%s" % M,
]
assets.merge_lang("bees", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events) + ours)
print("bees: %d sound events" % len(events))

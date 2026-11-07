"""Honey stream assets: blockstates, models, textures, sounds, names, recipes and the advancement.

    python tools/honey/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs(model):
    # Blockstate model names drop the block/ folder; 1.12.2 adds it back.
    return model.replace(":block/", ":")


honey = assets.port_model("block/honey_block")
comb = assets.port_model("block/honeycomb_block")
assets.write_json("blockstates/honey_block.json", {"variants": {"normal": {"model": bs(honey)}}})
assets.write_json("blockstates/honeycomb_block.json", {"variants": {"normal": {"model": bs(comb)}}})
assets.item_from_block("honey_block")
assets.item_from_block("honeycomb_block")
assets.item_generated("honey_bottle", assets.texture("item/honey_bottle"))
assets.item_generated("honeycomb", assets.texture("item/honeycomb"))

events = {}
for e in ["block.honey_block.break", "block.honey_block.fall", "block.honey_block.hit", "block.honey_block.place",
          "block.honey_block.slide", "block.honey_block.step", "item.honey_bottle.drink",
          "block.coral_block.break", "block.coral_block.fall", "block.coral_block.hit", "block.coral_block.place",
          "block.coral_block.step"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("honey", events)


def exists(item):
    return [{"type": "minecraft:item_exists", "item": item}]


assets.write_json("recipes/honey_block.json", {
    "conditions": exists(M + ":honey_block"),
    "type": "minecraft:crafting_shaped", "pattern": ["SS", "SS"],
    "key": {"S": {"item": M + ":honey_bottle"}},
    "result": {"item": M + ":honey_block"}})
assets.write_json("recipes/honey_bottle.json", {
    "conditions": exists(M + ":honey_bottle"),
    "type": "minecraft:crafting_shapeless",
    "ingredients": [{"item": M + ":honey_block"}] + [{"item": "minecraft:glass_bottle"}] * 4,
    "result": {"item": M + ":honey_bottle", "count": 4}})
assets.write_json("recipes/sugar_from_honey_bottle.json", {
    "conditions": exists(M + ":honey_bottle"),
    "type": "minecraft:crafting_shapeless", "group": "sugar",
    "ingredients": [{"item": M + ":honey_bottle"}],
    "result": {"item": "minecraft:sugar", "count": 3}})
assets.write_json("recipes/honeycomb_block.json", {
    "conditions": exists(M + ":honeycomb"),
    "type": "minecraft:crafting_shaped", "pattern": ["HH", "HH"],
    "key": {"H": {"item": M + ":honeycomb"}},
    "result": {"item": M + ":honeycomb_block"}})

# 1.15's adventure/honey_block_slide, in the Adventure tab. Granted from code (common/Advancements):
# 1.12.2 has no slide_down_block trigger.
assets.write_json("advancements/adventure/honey_block_slide.json", {
    "parent": "minecraft:adventure/root",
    "display": {
        "icon": {"item": M + ":honey_block"},
        "title": {"translate": "advancements.%s.honey_block_slide.title" % M},
        "description": {"translate": "advancements.%s.honey_block_slide.description" % M},
        "frame": "task", "show_toast": True, "announce_to_chat": True, "hidden": False},
    "criteria": {"honey_block_slide": {"trigger": "minecraft:impossible"}},
    "requirements": [["honey_block_slide"]]})

names = {
    "tile.%s.honey_block.name" % M: assets.name("block.minecraft.honey_block"),
    "tile.%s.honeycomb_block.name" % M: assets.name("block.minecraft.honeycomb_block"),
    "item.%s.honey_bottle.name" % M: assets.name("item.minecraft.honey_bottle"),
    "item.%s.honeycomb.name" % M: assets.name("item.minecraft.honeycomb"),
    "advancements.%s.honey_block_slide.title" % M: assets.name("advancements.adventure.honey_block_slide.title"),
    "advancements.%s.honey_block_slide.description" % M: assets.name("advancements.adventure.honey_block_slide.description"),
}
assets.merge_lang("honey", sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events))
print("honey: %d sound events" % len(events))

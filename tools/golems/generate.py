"""Iron golem stream assets: the three crack textures and the damage and repair sounds.

    python tools/golems/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

for stage in ("low", "medium", "high"):
    assets.texture("entity/iron_golem/iron_golem_crackiness_%s" % stage)
events = {}
for e in ["entity.iron_golem.damage", "entity.iron_golem.repair"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("golems", events)
assets.merge_lang("golems", assets.subtitle_lines(events))
print("golems: %d sound events" % len(events))

"""Tweaks stream language lines: its config switches and the bed message.

    python tools/tweaks/generate.py
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
sys.path.insert(0, HERE)
import assets  # noqa: E402
import switches  # noqa: E402

M = assets.MODID
lines = switches.lang()
lines.append("block.%s.set_spawn=%s" % (M, assets.name("block.minecraft.set_spawn")))
assets.merge_lang("tweaks", lines)
print("tweaks: %d lines" % len(lines))

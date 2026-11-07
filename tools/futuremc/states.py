"""Writes assets/buzzybeesbackport/migration/futuremc_states.json: what each metadata value of Future MC
0.2.21's bee blocks meant, in the format of Backport Core's recorded state tables.

Read from Future MC 0.2.21's own BeeHiveBlock (getStateFromMeta / getMetaFromState, checked with javap):
the horizontal facing in bits 0-1 (EnumFacing.byHorizontalIndex: 0 south, 1 west, 2 north, 3 east) and
`full` in bit 4 (value 4). The honey block and honeycomb block have no properties.

    python -I tools/futuremc/states.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

HORIZONTAL = ["south", "west", "north", "east"]
hive = {}
for full in (0, 4):
    for i, facing in enumerate(HORIZONTAL):
        hive[str(full | i)] = {"facing": facing, "full": "true" if full else "false"}
states = {
    "bee_nest": hive,
    "beehive": hive,
    "honey_block": {"0": {}},
    "honeycomb_block": {"0": {}},
}
assets.write_json("migration/futuremc_states.json", states)
print("futuremc_states.json: %d blocks" % len(states))

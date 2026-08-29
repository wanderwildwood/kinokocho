#!/usr/bin/env python3
"""
Fills the gaps in what the dangerous rows actually say.

An audit of the ten lethal and severe taxa found *Galerina marginata* — the one that
kills people who thought they had picked honey mushrooms — carrying no field note and no
hazard note at all. Its page said it could kill and then had nothing further to offer,
which is the worst possible combination: a warning with no way to act on it.

Two others had no onset. Onset is not decoration on this screen. The whole shape of an
amatoxin poisoning is that the person feels fine for most of a day and then feels ill
long after anybody would connect the two, and somebody reading a page at two in the
morning needs the number.

Idempotent. Only writes a field that is empty or a placeholder.

    python3 tools/fix-hazards.py
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")

FIX = {
    "galerina_marginata": dict(
        note="A small brown mushroom on rotting wood, with a ring or a ring zone on the "
             "stem and a rust-brown spore print. Nothing about it looks dangerous, which "
             "is the danger. Take the print before concluding anything at all about a "
             "small brown mushroom growing on wood.",
        hazard=dict(
            toxinClass="amatoxin",
            onset="6-24 hours",
            note="The same amatoxins as a destroying angel, in a mushroom the size of a "
                 "thumbnail. People have died from a small quantity picked among honey "
                 "mushrooms. The delay is the trap: you feel fine all day, then ill long "
                 "after anybody would connect it to a meal.",
        ),
    ),
    "amanita_rubescens": dict(
        hazard=dict(onset="1-4 hours"),
    ),
    "amanita_brunnescens": dict(
        hazard=dict(
            onset="Not well recorded",
            note="Treated as poisonous, and the toxin is not well characterised. In this "
                 "genus that is where the matter ends: the commonest Amanita in these "
                 "woods is not the one to experiment on.",
        ),
    ),
}


def main():
    pack = json.load(open(PACK))
    by = {t["id"]: t for t in pack["taxa"]}
    written = []
    for tid, fields in FIX.items():
        assert tid in by, tid
        taxon = by[tid]
        for key, value in fields.items():
            if key == "hazard":
                for hk, hv in value.items():
                    current = taxon["hazard"].get(hk, "")
                    if not current or current in ("—", "unknown", "none_known"):
                        taxon["hazard"][hk] = hv
                        written.append(f"{tid}.hazard.{hk}")
            elif not taxon.get(key):
                taxon[key] = value
                written.append(f"{tid}.{key}")

    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"{len(written)} fields written")
    for w in written:
        print(f"    {w}")


if __name__ == "__main__":
    main()

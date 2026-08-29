#!/usr/bin/env python3
"""
Writes the "what to look for" line on the rows that had none.

Six taxa had nothing at all in the field, which on the candidate page means the section a
reader looks at first is simply not there. One of them was *Chlorophyllum molybdites*,
which is the commonest cause of mushroom poisoning in North America and grows on lawns —
a row that says "makes people ill" and nothing about how to recognise it is not doing the
one job it has.

Three more said almost nothing. *Scleroderma citrinum*'s entire note was "Very likely to
be met", which is a fact about the pack rather than about the mushroom, and left out the
solid purple-black interior that is the whole of telling it from a puffball.

Idempotent: only writes where the note is missing or shorter than what is here.

    python3 tools/fix-notes.py
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")

NOTES = {
    "chlorophyllum_molybdites":
        "A big pale parasol on lawns and playing fields, often in rings and arcs. The "
        "gills stay white for days and then turn dull green, and the print is green — no "
        "other large parasol here does that. It is the commonest cause of mushroom "
        "poisoning in North America, and the reason is simply that it grows where people "
        "walk. Wait for the print.",
    "cantharellus_cinnabarinus":
        "Small and vivid pinkish-red the whole way through, ridges included. Blunt "
        "forking ridges rather than true gills, and the colour washes out in the sun "
        "while staying pink in the shade.",
    "laetiporus_sulphureus":
        "Overlapping sulphur shelves on standing hardwood: orange above, bright yellow "
        "pores below. Soft and damp while it is worth anything, chalky and tough within a "
        "week.",
    "laetiporus_cincinnatus":
        "A rosette at the foot of an oak rather than shelves up the trunk, and the pores "
        "underneath are white where the common one's are yellow.",
    "grifola_frondosa":
        "A dense clump of small grey-brown spoon-shaped caps at the foot of an oak, on "
        "branching pale stalks with the pores running down them. It returns to the same "
        "tree year after year, which is worth writing the place down for.",
    "meripilus_sumstinei":
        "A large rosette at the base of a hardwood that goes black wherever it is "
        "handled. That staining is the identification and it happens while you watch — "
        "press the pores with a thumb and wait a minute.",
    "scleroderma_citrinum":
        "A hard, thick-skinned ball on the ground, ochre and coarsely scaly. Cut it: it "
        "is solid and purple-black inside almost from the start, where a puffball of the "
        "same size is uniform white paste. That cut is the whole of telling them apart.",
    "armillaria_mellea":
        "Clustered on hardwood with long black bootlace rhizomorphs under the bark, a "
        "ring with a pastel-yellow edge, and a white spore print. The white print is what "
        "separates it from the deadly Galerina growing on the same wood.",
    "suillus_americanus":
        "Slimy yellow cap and slimy stem, always under white pine, with a ring zone and "
        "coarse angular pores. The slime stains fingers and, for some people, the skin "
        "that handles it.",
}


def main():
    pack = json.load(open(PACK))
    by = {t["id"]: t for t in pack["taxa"]}
    written = []
    for tid, note in NOTES.items():
        assert tid in by, tid
        if len(by[tid].get("note") or "") < len(note):
            by[tid]["note"] = note
            written.append(tid)
    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")
    missing = [t["id"] for t in pack["taxa"] if not t.get("note")]
    print(f"{len(written)} notes written")
    print(f"{len(missing)} taxa still without one: {missing}")


if __name__ == "__main__":
    main()

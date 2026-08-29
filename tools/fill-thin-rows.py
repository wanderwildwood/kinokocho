#!/usr/bin/env python3
"""
Fills in the taxa the pack barely describes.

The rows run from eight characters to thirty, median twenty-one, and the thin ones are
not obscure — they are the common puffball, the earthball, chicken of the woods, the
black-staining polypore. A thin row is not neutral: the key spends its questions on
characters the row says nothing about, and the candidate page has nothing to lay against
what the reader saw.

One of these was a safety gap rather than a thin one. *Scleroderma citrinum* is confused
with a puffball, it makes people ill, and the thing that settles it is that the inside is
solid purple-black where a puffball is white — and `flesh_colour` was not recorded on it
at all. The lookalike entry named the surface and the texture and not the one character
anybody would actually use.

Values are field characters a person can check, from published descriptions. Idempotent:
a character already present is left alone.

    python3 tools/fill-thin-rows.py
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")


def s(*values, freq="always"):
    return [{"value": v, "frequency": freq} for v in values]


def mixed(always=(), usually=(), sometimes=()):
    return (s(*always) + s(*usually, freq="usually") + s(*sometimes, freq="sometimes"))


FILL = {
    "lycoperdon_perlatum": {
        "cap_surface": mixed(usually=["warty"], sometimes=["smooth"]),
        "flesh_colour": mixed(usually=["white"], sometimes=["olive", "dark_brown"]),
        "flesh_consistency": mixed(usually=["soft"], sometimes=["powdery"]),
        "growth_habit": mixed(usually=["few", "troop"], sometimes=["single"]),
        "habitat": mixed(usually=["broadleaf_wood", "mixed_wood"],
                         sometimes=["conifer_wood", "disturbed"]),
        "associated_tree": mixed(usually=["broadleaf"], sometimes=["conifer"]),
        "odour": mixed(usually=["none"], sometimes=["mushroomy"]),
        "taste": mixed(usually=["mild"]),
        "bruising": mixed(usually=["no"]),
    },
    "scleroderma_citrinum": {
        # The one that matters. Solid and purple-black inside, where a puffball of the
        # same size and shape is white paste throughout.
        "flesh_colour": mixed(always=["black"], usually=["purple", "white"]),
        "flesh_consistency": mixed(usually=["fibrous"], sometimes=["powdery"]),
        "growth_habit": mixed(usually=["few", "troop"]),
        "habitat": mixed(usually=["broadleaf_wood", "mixed_wood"], sometimes=["heath"]),
        "associated_tree": mixed(usually=["broadleaf"], sometimes=["conifer"]),
        "odour": mixed(usually=["unpleasant_other"]),
        "cap_margin": mixed(usually=["straight"]),
    },
    "laetiporus_cincinnatus": {
        "cap_surface": mixed(usually=["smooth", "velvety"], sometimes=["wrinkled"]),
        "flesh_consistency": mixed(usually=["soft"], sometimes=["fibrous", "leathery"]),
        "flesh_colour": mixed(usually=["white", "cream"]),
        "habitat": mixed(usually=["broadleaf_wood"], sometimes=["wood_pasture"]),
        "odour": mixed(usually=["none"], sometimes=["mushroomy"]),
        "taste": mixed(usually=["mild"]),
        "cap_margin": mixed(usually=["wavy"]),
    },
    "bondarzewia_berkeleyi": {
        "cap_margin": mixed(usually=["wavy"], sometimes=["straight"]),
        "flesh_consistency": mixed(usually=["fibrous"], sometimes=["woody", "leathery"]),
        "flesh_colour": mixed(usually=["white", "cream"]),
        "habitat": mixed(usually=["broadleaf_wood"], sometimes=["wood_pasture"]),
        # The peppery aftertaste is the field character for this one.
        "taste": mixed(usually=["acrid"], sometimes=["mild"]),
        "odour": mixed(usually=["none"], sometimes=["mushroomy"]),
    },
    "meripilus_sumstinei": {
        "cap_surface": mixed(usually=["velvety"], sometimes=["wrinkled", "smooth"]),
        "cap_margin": mixed(usually=["wavy"]),
        "flesh_consistency": mixed(usually=["soft"], sometimes=["fibrous"]),
        "flesh_colour": mixed(usually=["white"], sometimes=["black"]),
        "habitat": mixed(usually=["broadleaf_wood"], sometimes=["wood_pasture", "urban"]),
        "odour": mixed(usually=["none"], sometimes=["mushroomy"]),
        "taste": mixed(usually=["mild"]),
    },
    "gyromitra_caroliniana": {
        "flesh_colour": mixed(usually=["white", "cream"]),
        "growth_habit": mixed(usually=["single", "few"]),
        "habitat": mixed(usually=["broadleaf_wood"], sometimes=["mixed_wood", "disturbed"]),
        "associated_tree": mixed(usually=["broadleaf"]),
        "odour": mixed(usually=["none"], sometimes=["mushroomy"]),
        "substrate_wood": mixed(usually=["dead", "buried"], sometimes=["broadleaf"]),
    },
    "artomyces_pyxidatus": {
        "flesh_consistency": mixed(usually=["fibrous"], sometimes=["brittle"]),
        "flesh_colour": mixed(usually=["white", "cream"]),
        "habitat": mixed(usually=["broadleaf_wood"], sometimes=["mixed_wood"]),
        "odour": mixed(usually=["none"]),
        "taste": mixed(usually=["acrid"], sometimes=["mild"]),
        "growth_habit": mixed(usually=["troop", "few"]),
    },
}

# The earthball's lookalike entry named the surface and the texture, and not the one
# character anybody standing over it would actually use.
DISCRIMINATORS = {
    ("scleroderma_citrinum", "lycoperdon_perlatum"):
        ["flesh_colour", "flesh_consistency", "cap_surface"],
    ("calvatia_craniiformis", "scleroderma_citrinum"):
        ["flesh_colour", "flesh_consistency", "cap_surface"],
}


def main():
    pack = json.load(open(PACK))
    by = {t["id"]: t for t in pack["taxa"]}
    added = 0
    for tid, characters in FILL.items():
        assert tid in by, tid
        row = by[tid]["characters"]
        for cid, states in characters.items():
            if cid not in row:
                row[cid] = states
                added += 1
    fixed = 0
    for (src, dst), discriminators in DISCRIMINATORS.items():
        for look in by[src].get("lookalikes", []):
            if look["taxon"] == dst and look["discriminators"] != discriminators:
                look["discriminators"] = discriminators
                fixed += 1

    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")

    counts = sorted(len(t["characters"]) for t in pack["taxa"])
    print(f"{added} characters added, {fixed} discriminator lists corrected")
    print(f"thinnest row is now {counts[0]}, median {counts[len(counts) // 2]}")


if __name__ == "__main__":
    main()

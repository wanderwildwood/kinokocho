#!/usr/bin/env python3
"""Give the schema a way to say a mushroom has no cap.

Twice in one sitting the key could not tell two mushrooms apart because the thing that
separates them is a thing it cannot say. Mutinus elegans is a bare spike and Satyrus
rugulosus has a distinct wrinkled cap, and both are stinkhorns out of a white egg; the
lion's mane is an unbranched cushion and the coral tooth is branched. In both cases the
pack said nothing about the missing part, and an absent character reads as *unknown*
rather than as *absent* — so eight stinkhorn pairings had no character that separated
them at all and could not be cross-linked.

The schema already has the shape of the answer. stipe_presence asks "Is there a stem?"
and gates every other stem character on it. Caps had no equivalent, which is also why
nothing stops the app asking somebody holding a puffball what shape its cap is.

What this does NOT do is gate the other cap characters on it, and that is deliberate.
cap_shape is doing double duty in this pack: the cup fungi record funnel and depressed,
which is genuinely how a cup is described, and the jellies and flask fungi use it the same
way. Gating cap_shape on a cap being present would delete real information from about a
dozen rows to tidy up a name. The overload is worth resolving on purpose, separately,
and not on the way past.
"""

import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCHEMA = ROOT / "app/src/main/assets/schema/characters-v1.json"
PACK = ROOT / "app/src/main/assets/packs/southern-appalachia-v1.json"

CHARACTER = collections.OrderedDict([
    ("id", "cap_presence"),
    ("label", "Is there a cap?"),
    ("noun", "Cap"),
    ("group", "cap"),
    ("hint", "A shelf or a bracket counts as a cap. A ball, a coral, a crust, a cup or "
             "a stinkhorn's bare spike does not."),
    ("kind", "state"),
    ("cardinality", "multi"),
    ("availability", "field"),
    ("power", 4),
    ("values", [
        collections.OrderedDict([("id", "present"),
                                 ("label", "A cap or a shelf")]),
        collections.OrderedDict([("id", "absent"),
                                 ("label", "No cap")]),
    ]),
])

# Written out rather than derived. Whether a thing has a cap is a judgement about the
# mushroom, and deriving it from which characters somebody happened to fill in would
# encode the gaps rather than the mushrooms.
ABSENT = {
    # Balls and earthballs: the spore mass is the whole fruitbody.
    "apioperdon_pyriforme", "calvatia_craniiformis", "lycoperdon_perlatum",
    "scleroderma_citrinum", "scleroderma_polyrhizum",
    "calostoma_cinnabarinum", "calostoma_lutescens",
    # Stinkhorns whose slime is carried on the spike, the columns or the arms.
    "clathrus_columnatus", "mutinus_elegans", "pseudocolus_fusiformis",
    # Corals and clubs.
    "artomyces_pyxidatus", "clavulina_coralloides", "ramaria_stricta",
    "xylaria_polymorpha",
    # Jellies and ears.
    "auricularia_americana", "dacrymyces_chrysospermus", "exidia_crenata",
    "tremella_mesenterica",
    # Cups and nests: the whole fruitbody is the cup.
    "chlorociboria_aeruginascens", "cyathus_striatus", "legaliana_badia",
    "sarcoscypha_dudleyi",
    # Cushions, crusts and a parasite that coats its host.
    "daldinia_childiae", "hypoxylon_fragiforme", "hypomyces_lactifluorum",
    "xylobolus_frustulatus",
    # Spines hanging from a cushion or a branched mass, with nothing overhanging them.
    "hericium_coralloides", "hericium_erinaceus",
}


def main():
    with open(SCHEMA, encoding="utf-8") as handle:
        schema = json.load(handle, object_pairs_hook=collections.OrderedDict)

    ids = [c["id"] for c in schema["characters"]]
    if "cap_presence" not in ids:
        schema["characters"].insert(ids.index("cap_shape"), CHARACTER)
        with open(SCHEMA, "w", encoding="utf-8") as handle:
            json.dump(schema, handle, indent=2, ensure_ascii=False)
            handle.write("\n")
        print("  + cap_presence added to the schema")
    else:
        print("  cap_presence is already in the schema")

    with open(PACK, encoding="utf-8") as handle:
        pack = json.load(handle, object_pairs_hook=collections.OrderedDict)

    unknown = ABSENT - {t["id"] for t in pack["taxa"]}
    if unknown:
        raise SystemExit(f"not in the pack: {sorted(unknown)}")

    absent = present = 0
    for taxon in pack["taxa"]:
        value = "absent" if taxon["id"] in ABSENT else "present"
        taxon["characters"]["cap_presence"] = [
            collections.OrderedDict(value=value, frequency="always")
        ]
        if value == "absent":
            absent += 1
        else:
            present += 1

    with open(PACK, "w", encoding="utf-8") as handle:
        json.dump(pack, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print(f"  {present} have a cap, {absent} do not.")


if __name__ == "__main__":
    main()

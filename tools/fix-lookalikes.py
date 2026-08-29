#!/usr/bin/env python3
"""
Fixes two references that pointed at nothing, and adds the character one of them needed.

Both were found by checking every lookalike against what actually exists, which nothing
had done:

1. **Amanita bisporigera declared a lookalike `agaricus_spp` that is not in the pack.**
   The engine boosts whatever a hazard's lookalikes say would separate them, so for the
   most lethal species here that list resolved to nothing. Agaricus campestris is added
   as a real taxon — it is the white mushroom in grass that the destroying angel is
   genuinely mistaken for, and the reference now points at it.

2. **Omphalotus declared the discriminator `flesh_colour_note`, which is not a
   character.** Jack-o'-lantern against chanterelle is the highest-volume dangerous
   confusion in this region — 519 against 503 observations, overlapping July to
   September — and it is settled by cutting the flesh and looking: orange through, or
   white. The schema could record the colour of a cap, a gill, a latex and a bruise,
   but not of the flesh itself, so the one character that settles the pair could not be
   written down at all.

   `flesh_colour` is added, and both sides of the pair encode it.

    python3 tools/fix-lookalikes.py
"""

import collections
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "app", "src", "main", "assets")
SCHEMA = os.path.join(ASSETS, "schema", "characters-v1.json")
PACK = os.path.join(ASSETS, "packs", "southern-appalachia-v1.json")


def rows(**spec):
    return [collections.OrderedDict([("value", v), ("frequency", f)])
            for v, f in spec.items()]


def add_flesh_colour(schema):
    ids = [c["id"] for c in schema["characters"]]
    if "flesh_colour" in ids:
        print("  flesh_colour already present")
        return False
    ch = collections.OrderedDict([
        ("id", "flesh_colour"),
        ("label", "What colour is the flesh inside?"),
        ("hint", "Cut it through and look at once. This is what settles an orange "
                 "chanterelle-like mushroom against a jack-o'-lantern, and it takes "
                 "one cut."),
        ("kind", "state"),
        ("cardinality", "multi"),
        ("availability", "field"),
        ("power", 4),
        ("valuesFrom", "colours"),
        ("note", "Distinct from bruising_colour, which is what the flesh becomes after "
                 "exposure. This is what it is on the instant of cutting."),
    ])
    # Beside flesh_consistency, because they are the same cut and the same second.
    i = ids.index("flesh_consistency") + 1
    schema["characters"].insert(i, ch)
    print("  added flesh_colour after flesh_consistency")
    return True


AGARICUS = dict(
    id="agaricus_campestris",
    scientificName="Agaricus campestris",
    commonName="field mushroom",
    hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                note="", source="—"),
    note="Pink gills that darken to chocolate, a brown spore print, and no sac at the "
         "base. Each of those alone separates it from a destroying angel, which keeps "
         "white gills, drops a white print, and stands in a bag.",
    characters=collections.OrderedDict(
        fruitbody_type=rows(gilled_stemmed="always"),
        substrate=rows(soil="always", grass="usually"),
        stipe_presence=rows(central="always"),
        stipe_base=rows(equal="usually", tapering="sometimes"),
        stipe_surface=rows(smooth="usually", fibrous="sometimes"),
        stipe_flesh=rows(solid="usually", stuffed="sometimes"),
        ring=rows(skirt="usually", zone="sometimes"),
        gill_attachment=rows(free="always"),
        gill_spacing=rows(crowded="usually", close="sometimes"),
        gill_edge=rows(even="usually"),
        cap_shape=rows(convex="usually", flat="usually"),
        cap_margin=rows(incurved="usually", straight="sometimes"),
        cap_surface=rows(dry="usually", smooth="usually", silky="sometimes",
                         scaly="sometimes"),
        cap_colour=rows(white="usually", cream="usually", tan="sometimes"),
        veil_remnants=rows(none="usually"),
        hymenophore_colour=rows(pink="usually", red_brown="usually",
                                dark_brown="usually", white="sometimes"),
        flesh_colour=rows(white="always"),
        flesh_consistency=rows(soft="always"),
        latex=rows(none="always"),
        bruising=rows(no="usually", yes="sometimes"),
        odour=rows(mushroomy="usually", none="sometimes"),
        taste=rows(mild="usually"),
        habitat=rows(improved_grass="usually", unimproved_grass="usually",
                     wood_pasture="sometimes", disturbed="sometimes"),
        spore_print=rows(brown="always"),
        spore_print_fine=rows(purple_brown="usually"),
        growth_habit=rows(troop="usually", few="usually", ring="sometimes"),
    ),
    measurements=dict(cap_width_mm=[30, 100], stipe_height_mm=[30, 80]),
    season=dict(months=[6, 7, 8, 9, 10, 11]),
    lookalikes=[dict(taxon="amanita_bisporigera",
                     discriminators=["spore_print", "hymenophore_colour",
                                     "stipe_base", "ring"],
                     note="A destroying angel keeps white gills at every age, drops a "
                          "white print, and sits in a membranous sac. If the base was "
                          "not dug up, that question is not answered.")],
    sources=["GBIF occurrence counts over a southern Blue Ridge polygon",
             "North American Mycological Association poisoning references"],
    reviewed=False,
)

# The flesh colour of the pair the region actually loses people to, plus the few others
# where it is diagnostic enough to be worth a reader's cut.
FLESH = {
    "omphalotus_illudens": dict(orange="always"),
    "cantharellus_lateritius": dict(white="usually", cream="usually"),
    "cantharellus_cinnabarinus": dict(white="usually", cream="sometimes"),
    "hygrophoropsis_aurantiaca": dict(cream="usually", orange="sometimes"),
    "amanita_bisporigera": dict(white="always"),
    "armillaria_mellea": dict(white="usually", cream="sometimes"),
    "galerina_marginata": dict(tan="usually", yellow_brown="sometimes"),
    "laetiporus_sulphureus": dict(white="usually", cream="usually"),
    "suillellus_subvelutipes": dict(yellow="usually"),
    "tylopilus_felleus": dict(white="usually"),
    "russula_virescens": dict(white="always"),
    "lactarius_indigo": dict(blue="usually"),
    "lactifluus_volemus": dict(white="usually", cream="sometimes"),
    "pleurotus_ostreatus": dict(white="always"),
    "pleurocybella_porrigens": dict(white="always"),
}


def main():
    with open(SCHEMA) as f:
        schema = json.load(f, object_pairs_hook=collections.OrderedDict)
    with open(PACK) as f:
        pack = json.load(f, object_pairs_hook=collections.OrderedDict)

    add_flesh_colour(schema)

    have = {t["id"] for t in pack["taxa"]}
    if AGARICUS["id"] not in have:
        pack["taxa"].append(collections.OrderedDict(AGARICUS))
        print(f"  added {AGARICUS['id']}")

    fixed = 0
    for t in pack["taxa"]:
        for l in (t.get("lookalikes") or []):
            if l["taxon"] == "agaricus_spp":
                l["taxon"] = "agaricus_campestris"
                if "ring" not in l["discriminators"]:
                    l["discriminators"].append("ring")
                fixed += 1
            if "flesh_colour_note" in l.get("discriminators", []):
                l["discriminators"] = [
                    "flesh_colour" if d == "flesh_colour_note" else d
                    for d in l["discriminators"]
                ]
                l["note"] = ("Cut it through and look at the flesh at once: a "
                             "jack-o'-lantern is orange all the way in, a chanterelle "
                             "is white or pale. Arguing about whether the ridges count "
                             "as gills settles nothing.")
                fixed += 1
    print(f"  repaired {fixed} broken references")

    added = 0
    for t in pack["taxa"]:
        spec = FLESH.get(t["id"])
        if spec and "flesh_colour" not in t["characters"]:
            t["characters"]["flesh_colour"] = rows(**spec)
            added += 1
    print(f"  encoded flesh_colour on {added} taxa")

    with open(SCHEMA, "w") as f:
        json.dump(schema, f, indent=2, ensure_ascii=False)
        f.write("\n")
    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"  pack holds {len(pack['taxa'])} taxa")


if __name__ == "__main__":
    main()

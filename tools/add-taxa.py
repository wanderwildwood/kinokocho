#!/usr/bin/env python3
"""
Adds taxa to the Southern Appalachia pack.

Chosen to fix things the pack could not do rather than to make it longer. Before this
ran, eight characters in the schema were exercised by no taxon at all — the whole latex
group among them, which meant the key could ask "does it ooze anything" and learn
nothing whatever the answer was. A character no taxon uses is not neutral; it is a
question that wastes a reader's attention in a wood.

Three jobs:

1. **Wake the dead characters.** Lactarius and Lactifluus give latex, latex_colour,
   latex_change and latex_change_colour something to discriminate. Russula gives
   brittle flesh a home.
2. **Complete the safety pairs.** Gyromitra was carried with nothing to be confused
   with; Morchella is the thing people are actually out looking for in April.
   Pleurocybella has killed people who thought it was Pleurotus, and only one of the
   two was here.
3. **Cover what is common enough to be met**, from the GBIF and iNat counts already
   recorded in the pack's notes.

Every row is macro-morphology of the kind any field guide agrees on, encoded into this
schema's own structure. Facts are not copyrightable and no text is reproduced. Every
taxon carries reviewed:false, which is true and not a placeholder: none of this has
been checked by a mycologist.

    python3 tools/add-taxa.py
"""

import collections
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")


def ch(**kw):
    """Characters, written as value -> frequency so the source stays readable."""
    out = collections.OrderedDict()
    for cid, spec in kw.items():
        if isinstance(spec, str):
            spec = {spec: "always"}
        out[cid] = [collections.OrderedDict([("value", v), ("frequency", f)])
                    for v, f in spec.items()]
    return out


TAXA = [
    # -- the spring pair -----------------------------------------------------
    dict(
        id="morchella_americana",
        scientificName="Morchella americana",
        commonName="yellow morel",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="Raw or undercooked morels upset many people. Never eaten raw.",
                    source="North American Mycological Association references"),
        note="Hollow from the top of the head to the bottom of the stem, in one "
             "continuous space. Cut it lengthwise and look: that is the test, and it is "
             "the only field character that settles this against a false morel.",
        characters=ch(
            fruitbody_type="morel_like",
            substrate={"soil": "always", "litter": "sometimes"},
            stipe_presence="central",
            stipe_flesh="hollow",
            cap_surface={"wrinkled": "always", "dry": "usually"},
            cap_colour={"tan": "usually", "yellow_brown": "usually",
                        "cream": "sometimes"},
            flesh_consistency={"brittle": "usually", "soft": "sometimes"},
            odour={"none": "usually", "mushroomy": "sometimes"},
            growth_habit={"few": "usually", "troop": "sometimes",
                          "single": "sometimes"},
            habitat={"broadleaf_wood": "usually", "disturbed": "sometimes"},
            associated_tree={"broadleaf": "usually"},
            spore_print="pale",
            bruising="no",
            latex="none",
        ),
        measurements=dict(cap_width_mm=[30, 90], stipe_height_mm=[30, 90]),
        season=dict(months=[3, 4, 5]),
        lookalikes=[dict(taxon="neogyromitra_caroliniana",
                         discriminators=["stipe_flesh", "cap_surface"],
                         note="A false morel is chambered or cottony inside, not one "
                              "hollow space, and its head is folded like a brain rather "
                              "than pitted with ridges.")],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon",
                 "North American Mycological Association poisoning references"],
        reviewed=False,
    ),
    dict(
        id="morchella_angusticeps",
        scientificName="Morchella angusticeps",
        commonName="black morel",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="Reported to disagree with more people than the yellow morel, "
                         "particularly with alcohol. Never eaten raw.",
                    source="North American Mycological Association references"),
        note="Ridges darken toward black as it ages while the pits stay paler. Hollow "
             "throughout, like all true morels.",
        characters=ch(
            fruitbody_type="morel_like",
            substrate={"soil": "always", "litter": "sometimes"},
            stipe_presence="central",
            stipe_flesh="hollow",
            cap_surface={"wrinkled": "always", "dry": "usually"},
            cap_colour={"grey": "sometimes", "dark_brown": "usually",
                        "black": "usually", "tan": "sometimes"},
            cap_colour_pattern={"darker_centre": "sometimes"},
            flesh_consistency={"brittle": "usually"},
            odour={"none": "usually"},
            growth_habit={"few": "usually", "single": "sometimes"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
            spore_print="pale",
            bruising="no",
            latex="none",
        ),
        measurements=dict(cap_width_mm=[25, 70], stipe_height_mm=[30, 80]),
        season=dict(months=[3, 4, 5]),
        lookalikes=[dict(taxon="neogyromitra_caroliniana",
                         discriminators=["stipe_flesh", "cap_surface"],
                         note="Chambered inside rather than hollow, and folded rather "
                              "than pitted.")],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- the latex group, which nothing exercised ----------------------------
    dict(
        id="lactarius_indigo",
        scientificName="Lactarius indigo",
        commonName="indigo milkcap",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="", source="—"),
        note="Blue latex. Nothing else here does that, so one cut settles it.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate="soil",
            stipe_presence="central",
            gill_attachment={"adnate": "usually", "decurrent": "usually"},
            gill_spacing={"close": "usually", "crowded": "sometimes"},
            cap_shape={"depressed": "usually", "funnel": "sometimes",
                       "convex": "sometimes"},
            cap_surface={"smooth": "usually", "viscid": "sometimes",
                         "zoned": "usually"},
            cap_colour={"blue": "usually", "grey": "sometimes"},
            cap_colour_pattern={"uniform": "sometimes"},
            hymenophore_colour={"blue": "usually"},
            latex="present",
            latex_colour={"blue": "always"},
            latex_change={"yes": "usually"},
            latex_change_colour={"green": "usually"},
            bruising={"yes": "usually"},
            bruising_where={"cap": "usually", "hymenophore": "usually"},
            bruising_colour={"green": "usually"},
            flesh_consistency={"brittle": "usually"},
            stipe_surface={"smooth": "usually", "punctate": "sometimes"},
            odour={"none": "usually", "mushroomy": "sometimes"},
            taste={"mild": "usually"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
            associated_tree={"broadleaf": "usually", "conifer": "sometimes"},
            spore_print="pale",
            growth_habit={"few": "usually", "single": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[20, 80]),
        season=dict(months=[6, 7, 8, 9, 10]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),
    dict(
        id="lactifluus_volemus",
        scientificName="Lactifluus volemus",
        commonName="weeping milkcap",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="", source="—"),
        note="Bleeds white latex freely at the slightest damage, and the latex stains "
             "everything it touches brown. Smells of fish as it ages.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate="soil",
            stipe_presence="central",
            gill_attachment={"adnate": "usually", "decurrent": "sometimes"},
            gill_spacing={"close": "usually", "crowded": "sometimes"},
            cap_shape={"convex": "usually", "depressed": "usually",
                       "flat": "sometimes"},
            cap_surface={"dry": "usually", "velvety": "sometimes",
                         "smooth": "sometimes"},
            cap_colour={"orange": "usually", "red_brown": "usually",
                        "tan": "sometimes"},
            hymenophore_colour={"white": "usually", "cream": "usually"},
            latex="present",
            latex_colour={"white": "always"},
            latex_change={"yes": "usually"},
            latex_change_colour={"yellow_brown": "usually", "dark_brown": "sometimes"},
            bruising={"yes": "usually"},
            bruising_where={"hymenophore": "usually", "flesh": "usually"},
            bruising_colour={"yellow_brown": "usually"},
            flesh_consistency={"brittle": "usually"},
            odour={"foetid": "usually", "unpleasant_other": "sometimes"},
            taste={"mild": "usually"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
            associated_tree={"broadleaf": "usually"},
            spore_print="pale",
            growth_habit={"few": "usually", "single": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[50, 120], stipe_height_mm=[40, 100]),
        season=dict(months=[6, 7, 8, 9]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- brittle flesh, which one taxon exercised ----------------------------
    dict(
        id="russula_virescens",
        scientificName="Russula virescens",
        commonName="green-cracking russula",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="", source="—"),
        note="Flesh snaps like chalk rather than tearing in strands — that is what a "
             "Russula is, and it is easier to feel than to read about. No latex.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate="soil",
            stipe_presence="central",
            gill_attachment={"free": "sometimes", "adnexed": "usually",
                             "adnate": "sometimes"},
            gill_spacing={"close": "usually"},
            gill_edge={"even": "usually"},
            cap_shape={"convex": "sometimes", "flat": "usually",
                       "depressed": "usually"},
            cap_surface={"cracked": "always", "dry": "usually"},
            cap_colour={"green": "usually", "olive": "usually"},
            cap_colour_pattern={"uniform": "usually"},
            hymenophore_colour={"white": "usually", "cream": "sometimes"},
            flesh_consistency={"brittle": "always"},
            stipe_surface={"smooth": "usually"},
            stipe_flesh={"solid": "usually", "stuffed": "sometimes"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually", "mushroomy": "sometimes"},
            taste={"mild": "usually"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
            associated_tree={"broadleaf": "usually"},
            spore_print="pale",
            growth_habit={"few": "usually", "single": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[50, 120], stipe_height_mm=[40, 90]),
        season=dict(months=[6, 7, 8, 9]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- the pair that has actually killed people here -----------------------
    dict(
        id="pleurocybella_porrigens",
        scientificName="Pleurocybella porrigens",
        commonName="angel wings",
        hazard=dict(toxinClass="unknown_encephalopathy", severity="LETHAL",
                    onset="days to weeks",
                    note="Long eaten without concern, then implicated in a cluster of "
                         "fatal encephalopathy. The mechanism is still not settled and "
                         "there is no known safe preparation.",
                    source="North American Mycological Association poisoning references"),
        note="On conifer, and here that means high up — above about 5,000 feet. Thin, "
             "pure white, and without the substantial flesh of an oyster mushroom.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate="wood",
            substrate_wood={"conifer": "always", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence={"absent_attached": "usually", "lateral": "sometimes"},
            gill_attachment={"no_stem": "usually", "decurrent": "sometimes"},
            gill_spacing={"crowded": "usually", "close": "sometimes"},
            cap_shape={"flat": "usually"},
            cap_margin={"wavy": "usually", "inrolled": "sometimes"},
            cap_surface={"smooth": "usually", "dry": "usually"},
            cap_colour={"white": "always"},
            hymenophore_colour={"white": "always"},
            flesh_consistency={"soft": "usually", "brittle": "sometimes"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually", "mushroomy": "sometimes"},
            habitat={"conifer_wood": "always"},
            associated_tree={"conifer": "always"},
            spore_print="pale",
            growth_habit={"tiered": "usually", "troop": "usually"},
        ),
        measurements=dict(cap_width_mm=[20, 100]),
        season=dict(months=[8, 9, 10, 11]),
        lookalikes=[dict(taxon="pleurotus_ostreatus",
                         discriminators=["substrate_wood", "associated_tree",
                                         "flesh_consistency"],
                         note="An oyster mushroom is on broadleaf wood and is thick and "
                              "fleshy. This is on conifer, and it is thin. If the wood "
                              "is not certain, do not resolve it in the field.")],
        sources=["North American Mycological Association poisoning references",
                 "iNaturalist occurrence records above 5,000 ft in the southern Blue Ridge"],
        reviewed=False,
    ),
    dict(
        id="pleurotus_ostreatus",
        scientificName="Pleurotus ostreatus",
        commonName="oyster mushroom",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="", source="—"),
        note="Broadleaf wood, thick fleshy caps, gills running down onto what stem "
             "there is.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "usually",
                            "living": "sometimes"},
            stipe_presence={"lateral": "usually", "absent_attached": "usually"},
            gill_attachment={"decurrent": "usually", "no_stem": "sometimes"},
            gill_spacing={"close": "usually"},
            gill_edge={"even": "usually", "wavy": "sometimes"},
            cap_shape={"flat": "usually", "depressed": "sometimes"},
            cap_margin={"inrolled": "sometimes", "wavy": "usually"},
            cap_surface={"smooth": "usually", "dry": "usually"},
            cap_colour={"grey": "usually", "cream": "usually", "tan": "sometimes",
                        "white": "sometimes"},
            hymenophore_colour={"white": "usually", "cream": "usually"},
            flesh_consistency={"soft": "usually", "fibrous": "sometimes"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"mushroomy": "usually", "aniseed": "sometimes"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
            associated_tree={"broadleaf": "usually"},
            spore_print="pale",
            growth_habit={"tiered": "usually", "caespitose": "usually"},
        ),
        measurements=dict(cap_width_mm=[50, 200]),
        season=dict(months=[4, 5, 6, 9, 10, 11, 12]),
        lookalikes=[dict(taxon="pleurocybella_porrigens",
                         discriminators=["substrate_wood", "associated_tree",
                                         "flesh_consistency"],
                         note="Angel wings is on conifer and is thin and pure white.")],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),
]


def main():
    with open(PACK) as f:
        pack = json.load(f, object_pairs_hook=collections.OrderedDict)

    have = {t["id"] for t in pack["taxa"]}
    added = []
    for t in TAXA:
        if t["id"] in have:
            print(f"  skip {t['id']} (already present)")
            continue
        pack["taxa"].append(collections.OrderedDict(t))
        added.append(t["id"])

    # Gyromitra was filed as a cup because there was nowhere else to put it. Now
    # there is, and it is the same state as the thing it is confused with — which is
    # the point: they must be told apart by the knife test, not by the first question.
    for t in pack["taxa"]:
        if t["id"] == "neogyromitra_caroliniana":
            t["characters"]["fruitbody_type"] = [
                collections.OrderedDict([("value", "morel_like"), ("frequency", "always")])
            ]
            t["characters"]["stipe_flesh"] = [
                collections.OrderedDict([("value", "chambered"), ("frequency", "usually")]),
                collections.OrderedDict([("value", "stuffed"), ("frequency", "sometimes")]),
            ]
            t["characters"]["cap_surface"] = [
                collections.OrderedDict([("value", "wrinkled"), ("frequency", "always")]),
            ]
            t["lookalikes"] = [
                collections.OrderedDict([
                    ("taxon", "morchella_americana"),
                    ("discriminators", ["stipe_flesh", "cap_surface"]),
                    ("note", "A true morel is one hollow space from top to bottom and "
                             "its head is pitted with ridges. This is chambered inside "
                             "and folded like a brain."),
                ])
            ]
            print("  re-typed neogyromitra_caroliniana as morel_like")

    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"added {len(added)}: {', '.join(added)}")
    print(f"pack now holds {len(pack['taxa'])} taxa")


if __name__ == "__main__":
    main()

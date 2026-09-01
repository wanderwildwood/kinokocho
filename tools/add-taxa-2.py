#!/usr/bin/env python3
"""
Second batch: make every top-level answer lead somewhere.

fruitbody_type is the question the key always asks first, and eight of its states had
no taxon behind them. Answering "tubes underneath, soft, with a stem" — a bolete, one
of the commonest things anyone picks up here — emptied the candidate list entirely.
That does not read as "the pack does not cover this yet"; it reads as a broken app, on
the very first question.

So this batch is chosen by shape rather than by interest: at least one real,
common Southern Appalachian species behind every top-level answer. Moving Gyromitra to
morel_like in batch one emptied cup_disc, which is fixed here too.

It also picks up states that were unreachable and should not have been: an equal stem
base (the commonest base there is), a dark spore print, a bitter taste, and leathery
and woody flesh.

Same rules as batch one: macro-morphology any field guide agrees on, encoded into this
schema's own structure, no text reproduced, reviewed:false throughout because none of
it has been checked by a mycologist.

    python3 tools/add-taxa-2.py
"""

import collections
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")


def ch(**kw):
    out = collections.OrderedDict()
    for cid, spec in kw.items():
        if isinstance(spec, str):
            spec = {spec: "always"}
        out[cid] = [collections.OrderedDict([("value", v), ("frequency", f)])
                    for v, f in spec.items()]
    return out


NONE = dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—", note="",
            source="—")

TAXA = [
    # -- bolete --------------------------------------------------------------
    dict(
        id="tylopilus_felleus",
        scientificName="Tylopilus felleus",
        commonName="bitter bolete",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="Not poisonous, but bitter enough to ruin a whole pan.",
                    source="—"),
        note="A coarse dark net over the upper stem, and pores that age pink. Bitter "
             "on the tongue-tip, which is how it is told from a porcini.",
        characters=ch(
            fruitbody_type="bolete",
            substrate={"soil": "always"},
            stipe_presence="central",
            stipe_base={"bulbous": "usually", "equal": "sometimes"},
            stipe_surface={"reticulate": "always"},
            stipe_flesh={"solid": "always"},
            cap_shape={"convex": "usually", "flat": "sometimes"},
            cap_surface={"dry": "usually", "smooth": "usually",
                         "velvety": "sometimes"},
            cap_colour={"tan": "usually", "yellow_brown": "usually",
                        "red_brown": "sometimes"},
            hymenophore_colour={"white": "sometimes", "pink": "usually"},
            flesh_consistency={"soft": "usually"},
            bruising={"yes": "sometimes"},
            bruising_where={"hymenophore": "usually"},
            bruising_colour={"pink": "sometimes"},
            latex="none",
            ring="absent",
            odour={"none": "usually", "mushroomy": "sometimes"},
            taste={"bitter": "always"},
            habitat={"broadleaf_wood": "usually", "conifer_wood": "sometimes",
                     "mixed_wood": "usually"},
            spore_print={"pink": "usually", "brown": "sometimes"},
            growth_habit={"single": "usually", "few": "usually"},
        ),
        measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[40, 120]),
        season=dict(months=[6, 7, 8, 9, 10]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),
    dict(
        id="neoboletus_subvelutipes",
        scientificName="Neoboletus subvelutipes",
        commonName="red-mouth bolete",
        hazard=dict(toxinClass="gastrointestinal", severity="GI", onset="1-3 hours",
                    note="Red-pored boletes that stain blue instantly are not eaten.",
                    source="North American Mycological Association references"),
        note="Dark red pores, and flesh that goes blue the moment it is cut — fast "
             "enough that you see it happen rather than find it later.",
        characters=ch(
            fruitbody_type="bolete",
            substrate={"soil": "always"},
            stipe_presence="central",
            stipe_base={"equal": "usually", "tapering": "sometimes"},
            stipe_surface={"punctate": "usually", "hairy": "sometimes"},
            stipe_flesh={"solid": "always"},
            cap_shape={"convex": "usually"},
            cap_surface={"velvety": "usually", "dry": "usually"},
            cap_colour={"red_brown": "usually", "dark_brown": "sometimes",
                        "olive": "sometimes"},
            hymenophore_colour={"red": "always", "orange": "sometimes"},
            bruising={"yes": "always"},
            bruising_where={"cap": "usually", "hymenophore": "always",
                            "flesh": "always", "stipe": "usually"},
            bruising_colour={"blue": "always"},
            bruising_speed={"instant": "always"},
            flesh_consistency={"soft": "usually"},
            latex="none",
            ring="absent",
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
            spore_print={"brown": "usually"},
            growth_habit={"single": "usually", "few": "usually"},
        ),
        measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[50, 120]),
        season=dict(months=[6, 7, 8, 9]),
        lookalikes=[],
        sources=["North American Mycological Association references"],
        reviewed=False,
    ),

    # -- toothed -------------------------------------------------------------
    dict(
        id="hericium_erinaceus",
        scientificName="Hericium erinaceus",
        commonName="lion's mane",
        hazard=NONE,
        note="A white cushion hanging from a wound in a hardwood, covered in long "
             "spines. No cap and no stem to speak of.",
        characters=ch(
            fruitbody_type="toothed",
            substrate="wood",
            substrate_wood={"broadleaf": "always", "living": "usually",
                            "dead": "sometimes"},
            stipe_presence={"absent_attached": "always"},
            cap_colour={"white": "usually", "cream": "sometimes"},
            hymenophore_colour={"white": "usually", "cream": "sometimes"},
            flesh_consistency={"soft": "usually", "fibrous": "sometimes"},
            latex="none",
            bruising={"yes": "sometimes"},
            bruising_where={"flesh": "usually"},
            bruising_colour={"yellow_brown": "sometimes"},
            ring="absent",
            odour={"none": "usually", "mushroomy": "sometimes"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale",
            growth_habit={"single": "usually"},
        ),
        measurements=dict(cap_width_mm=[80, 250]),
        season=dict(months=[8, 9, 10, 11]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),
    dict(
        id="hydnum_repandum",
        scientificName="Hydnum repandum",
        commonName="hedgehog mushroom",
        hazard=NONE,
        note="A pale irregular cap with soft spines underneath instead of gills, and "
             "the spines rub off easily with a thumb.",
        characters=ch(
            fruitbody_type="toothed",
            substrate={"soil": "always", "litter": "sometimes"},
            stipe_presence={"central": "usually", "off_centre": "usually"},
            stipe_base={"equal": "usually"},
            stipe_surface={"smooth": "usually"},
            stipe_flesh={"solid": "usually"},
            cap_shape={"convex": "usually", "flat": "usually",
                       "depressed": "sometimes"},
            cap_margin={"wavy": "usually", "inrolled": "sometimes"},
            cap_surface={"dry": "usually", "smooth": "usually",
                         "velvety": "sometimes"},
            cap_colour={"cream": "usually", "tan": "usually", "orange": "sometimes"},
            hymenophore_colour={"cream": "usually", "white": "sometimes"},
            flesh_consistency={"brittle": "usually", "soft": "sometimes"},
            latex="none",
            bruising={"yes": "sometimes"},
            bruising_where={"flesh": "usually"},
            bruising_colour={"orange": "sometimes"},
            ring="absent",
            odour={"none": "usually", "mushroomy": "sometimes"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "usually",
                     "conifer_wood": "sometimes"},
            spore_print="pale",
            growth_habit={"few": "usually", "troop": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[30, 100], stipe_height_mm=[30, 80]),
        season=dict(months=[8, 9, 10, 11]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- coral / club --------------------------------------------------------
    dict(
        id="artomyces_pyxidatus",
        scientificName="Artomyces pyxidatus",
        commonName="crown-tipped coral",
        hazard=NONE,
        note="Every branch ends in a tiny crown of points rather than a blunt tip. On "
             "wood, which separates it from the corals that grow out of soil.",
        characters=ch(
            fruitbody_type="coral_club",
            substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence={"absent_sitting": "usually"},
            cap_colour={"white": "sometimes", "cream": "usually",
                        "tan": "sometimes"},
            flesh_consistency={"brittle": "usually", "fibrous": "sometimes"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale",
            growth_habit={"troop": "usually", "caespitose": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[20, 130]),
        season=dict(months=[5, 6, 7, 8, 9]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- cup -----------------------------------------------------------------
    dict(
        id="sarcoscypha_dudleyi",
        scientificName="Sarcoscypha dudleyi",
        commonName="scarlet elf cup",
        hazard=NONE,
        note="A scarlet cup on buried hardwood sticks, and one of the first things up "
             "in the year — often while there is still snow about.",
        characters=ch(
            fruitbody_type="cup_disc",
            substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always",
                            "buried": "usually"},
            stipe_presence={"absent_sitting": "usually", "central": "sometimes"},
            cap_shape={"depressed": "usually", "funnel": "sometimes"},
            cap_surface={"smooth": "usually"},
            cap_colour={"red": "always"},
            flesh_consistency={"leathery": "sometimes", "soft": "usually"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale",
            growth_habit={"few": "usually", "troop": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[10, 50]),
        season=dict(months=[1, 2, 3, 4]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- resupinate / crust --------------------------------------------------
    dict(
        id="stereum_ostrea",
        scientificName="Stereum ostrea",
        commonName="false turkey tail",
        hazard=NONE,
        note="Zoned like a turkey tail but smooth underneath — no pores at all. That "
             "is the whole difference, and it is on the underside.",
        characters=ch(
            fruitbody_type={"resupinate": "usually", "polypore": "sometimes"},
            substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence={"absent_attached": "always"},
            cap_shape={"flat": "usually"},
            cap_margin={"wavy": "usually"},
            cap_surface={"zoned": "always", "velvety": "usually", "hairy": "sometimes"},
            cap_colour={"tan": "usually", "red_brown": "usually",
                        "cream": "sometimes", "orange": "sometimes"},
            cap_colour_pattern={"paler_margin": "usually"},
            hymenophore_colour={"cream": "usually", "tan": "sometimes"},
            flesh_consistency={"leathery": "always"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale",
            growth_habit={"tiered": "usually", "troop": "usually"},
        ),
        measurements=dict(cap_width_mm=[10, 70]),
        season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
        lookalikes=[dict(taxon="trametes_versicolor",
                         discriminators=["fruitbody_type"],
                         note="Turkey tail has a white pore surface underneath; this is "
                              "smooth.")],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),
    dict(
        id="trametes_versicolor",
        scientificName="Trametes versicolor",
        commonName="turkey tail",
        hazard=NONE,
        note="Thin, tough, banded in concentric zones, with a white surface of very "
             "fine pores underneath.",
        characters=ch(
            fruitbody_type="polypore",
            substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence={"absent_attached": "always"},
            cap_shape={"flat": "usually"},
            cap_margin={"wavy": "usually", "straight": "sometimes"},
            cap_surface={"zoned": "always", "velvety": "usually"},
            cap_colour={"grey": "usually", "tan": "usually", "dark_brown": "usually",
                        "cream": "usually", "olive": "sometimes"},
            cap_colour_pattern={"paler_margin": "usually"},
            hymenophore_colour={"white": "usually", "cream": "usually"},
            flesh_consistency={"leathery": "always"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale",
            growth_habit={"tiered": "usually", "troop": "usually"},
        ),
        measurements=dict(cap_width_mm=[10, 80]),
        season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
        lookalikes=[dict(taxon="stereum_ostrea",
                         discriminators=["fruitbody_type"],
                         note="False turkey tail is smooth underneath, with no pores.")],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- jelly ---------------------------------------------------------------
    dict(
        id="tremella_mesenterica",
        scientificName="Tremella mesenterica",
        commonName="witch's butter",
        hazard=NONE,
        note="Yellow gelatinous folds on dead hardwood. Shrivels to a hard scab in dry "
             "weather and comes back with rain.",
        characters=ch(
            fruitbody_type="jelly",
            substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence={"absent_attached": "always"},
            cap_colour={"yellow": "usually", "orange": "usually"},
            flesh_consistency={"gelatinous": "always"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale",
            growth_habit={"single": "usually", "few": "usually"},
        ),
        measurements=dict(cap_width_mm=[10, 60]),
        season=dict(months=[1, 2, 3, 9, 10, 11, 12]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- flask ---------------------------------------------------------------
    dict(
        id="daldinia_childiae",
        scientificName="Daldinia childiae",
        commonName="carbon balls",
        hazard=NONE,
        note="A hard black ball on dead hardwood. Cut it and it is banded in "
             "concentric rings like a tree stump.",
        characters=ch(
            fruitbody_type="flask",
            substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence={"absent_attached": "always"},
            cap_shape={"convex": "usually"},
            cap_surface={"smooth": "usually"},
            cap_colour={"black": "usually", "dark_brown": "sometimes"},
            flesh_consistency={"woody": "always"},
            latex="none",
            bruising="no",
            ring="absent",
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print={"dark": "usually"},
            growth_habit={"few": "usually", "troop": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[10, 50]),
        season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
        lookalikes=[],
        sources=["GBIF occurrence counts over a southern Blue Ridge polygon"],
        reviewed=False,
    ),

    # -- a dark spore print, and a stem that is simply straight ---------------
    dict(
        id="coprinus_comatus",
        scientificName="Coprinus comatus",
        commonName="shaggy mane",
        hazard=dict(toxinClass="none_known", severity="NONE_KNOWN", onset="—",
                    note="Dissolves into black liquid within a day of picking. Not a "
                         "hazard, but it will not keep.",
                    source="—"),
        note="A tall white shaggy column that turns itself to black ink from the "
             "bottom of the cap upwards. Grassy, disturbed ground, often on verges.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate={"soil": "always", "grass": "usually"},
            stipe_presence="central",
            stipe_base={"equal": "usually", "bulbous": "sometimes"},
            stipe_surface={"smooth": "usually", "fibrous": "sometimes"},
            stipe_flesh={"hollow": "usually", "chambered": "sometimes"},
            ring={"movable": "sometimes", "zone": "sometimes", "skirt": "sometimes"},
            gill_attachment={"free": "always"},
            gill_spacing={"crowded": "always"},
            gill_edge={"even": "usually"},
            cap_shape={"egg": "always", "bell": "sometimes"},
            cap_margin={"appendiculate": "sometimes", "straight": "usually"},
            cap_surface={"scaly": "always", "dry": "usually"},
            cap_colour={"white": "usually", "cream": "usually", "tan": "sometimes"},
            veil_remnants={"fibrous_scales": "usually"},
            hymenophore_colour={"white": "sometimes", "pink": "sometimes",
                                "black": "usually"},
            flesh_consistency={"soft": "usually"},
            latex="none",
            bruising={"yes": "usually"},
            bruising_where={"hymenophore": "usually"},
            bruising_colour={"black": "usually"},
            odour={"none": "usually", "mushroomy": "sometimes"},
            habitat={"improved_grass": "usually", "disturbed": "usually",
                     "urban": "sometimes", "garden": "sometimes"},
            spore_print={"dark": "always"},
            growth_habit={"troop": "usually", "caespitose": "sometimes",
                          "few": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[20, 60], stipe_height_mm=[80, 250]),
        season=dict(months=[5, 6, 7, 8, 9, 10, 11]),
        lookalikes=[],
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

    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"added {len(added)}")
    for a in added:
        print(f"    {a}")
    print(f"pack now holds {len(pack['taxa'])} taxa")


if __name__ == "__main__":
    main()

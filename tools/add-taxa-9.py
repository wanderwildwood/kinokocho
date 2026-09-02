#!/usr/bin/env python3
"""
Ninth batch: winter, which this pack barely had.

Counting what the pack can show month by month gives a hundred and eleven taxa in
September and eighteen in February. That is not what the woods do — it is what a pack of
sixty-six gilled mushrooms does, because gilled mushrooms are a summer and autumn
business. A person who opens the month page in January is told there is almost nothing
about, while they are standing in front of a fallen oak carrying four kinds of bracket, a
crust and a jelly, all of which are there and all of which are findable precisely because
nothing else is.

So these eight were chosen by winter abundance rather than overall abundance: the
proportion of each one's North Carolina records that fall in December, January and
February. Exidia crenata is fifty-eight per cent winter. Clathrus and Calostoma scored
well too and are held back for a batch of their own, because stinkhorns and the stalked
puffballs want writing up together.

They teach each other, which is the other reason for taking them as a group. Four tough
brackets, two crusts and two jellies is a key exercise in itself: the brackets are told
apart by what the underside does — pores, slots, a maze, blades — and the crusts by
whether the surface is cracked into tiles. Three of them are also taken for turkey tail,
which is in the pack and sought after.

Seasons say all twelve months for the ones recorded in all twelve. These are fungi that
persist rather than fruit and vanish, and that persistence is the whole reason they are
worth a winter page.

Same rules as every batch: macro-morphology any guide agrees on, no text reproduced,
reviewed false. Names checked against GBIF and iNaturalist before they were written down,
and every discriminator checked to be a character that genuinely separates the pair.

    python3 tools/add-taxa-9.py
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


def hz(cls="none_known", sev="NONE_KNOWN", onset="—", note="", src="—"):
    return dict(toxinClass=cls, severity=sev, onset=onset, note=note, source=src)


INAT = ["iNaturalist occurrence records, southern Blue Ridge"]
NAMA = ["North American Mycological Association poisoning references"]
MORPH = ["Macromorphology as standard North American field guides agree it — "
         "written out, not reproduced"]

ALL_YEAR = "Recorded here in every month: it persists rather than fruits and passes."

ON_WOOD = dict(substrate="wood", stipe_presence={"absent_attached": "always"},
               latex="none", spore_print="pale")

TAXA = [
    dict(id="stereum_complicatum", scientificName="Stereum complicatum",
         commonName="crowded parchment", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Far too thin and tough to be eaten. Nothing documented against it.",
                   NAMA[0]),
         note="Small tan-orange brackets fused edge to edge in dense sheets along fallen "
              "hardwood branches, often running for a foot or more. Smooth underneath "
              "like the other parchments, and crowded together in a way the larger ones "
              "are not.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"chanterelle_like": "always"},
                       substrate_wood={"broadleaf": "always", "dead": "always",
                                       "bark": "usually"},
                       cap_surface={"zoned": "usually", "velvety": "usually",
                                    "dry": "always"},
                       # Orange is the character; tan and brown happen and are not what
                       # this one is. Written with frequencies rather than as a flat list
                       # because Stereum lobatum is the tan one, and if both simply claim
                       # tan then neither can ever be told from the other by colour.
                       cap_colour={"orange": "always", "tan": "sometimes",
                                   "yellow_brown": "sometimes"},
                       cap_margin={"wavy": "usually"},
                       flesh_consistency={"leathery": "always"},
                       odour={"none": "usually"},
                       growth_habit={"fused": "always"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[5, 25]),
         season=dict(months=list(range(1, 13)), note=ALL_YEAR),
         lookalikes=[
             dict(taxon="trametes_versicolor",
                  discriminators=["fruitbody_type"],
                  note="Turkey tail has pores underneath. This is smooth, and much "
                       "smaller."),
             dict(taxon="stereum_lobatum",
                  discriminators=["growth_habit"],
                  note="The same smooth underside on both. This one grows fused into "
                       "crowded sheets; the other makes separate lobed brackets."),
         ],
         sources=INAT + MORPH, reviewed=False),

    dict(id="exidia_crenata", scientificName="Exidia crenata",
         commonName="amber jelly roll", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Eaten in places for texture rather than flavour. Nothing documented "
                   "against it.", NAMA[0]),
         note="Dark brown gelatinous folds on fallen hardwood, firm and rubbery when wet "
              "and shrunk to a thin varnish-like crust when dry. It comes back with rain, "
              "which is why it is a winter fungus here.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"jelly": "always"},
                       substrate_wood={"broadleaf": "always", "dead": "always",
                                       "bare_wood": "usually"},
                       cap_surface={"wrinkled": "usually", "smooth": "usually"},
                       cap_colour={"dark_brown": "always", "black": "usually"},
                       flesh_consistency={"gelatinous": "always"},
                       flesh_colour={"dark_brown": "usually"},
                       odour={"none": "usually"},
                       growth_habit={"fused": "usually", "troop": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[10, 60]),
         season=dict(months=[11, 12, 1, 2, 3, 4],
                     note="A wet-season jelly: it disappears through the summer and "
                          "returns with the winter rain."),
         lookalikes=[dict(taxon="tremella_mesenterica",
                          discriminators=["cap_colour"],
                          note="Witch's butter is yellow to orange. This is brown to "
                               "nearly black. Both are folded jellies on dead hardwood "
                               "and neither has any other shape to go on.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="dacrymyces_chrysospermus", scientificName="Dacrymyces chrysospermus",
         commonName="orange jelly spot", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Nothing documented against it. Small and watery, and not collected.",
                   NAMA[0]),
         note="Orange gelatinous cushions on dead conifer wood, usually bursting out "
              "along the seams where the bark has gone. Pull one off and the point it was "
              "attached by is white, which witch's butter does not do.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"jelly": "always"},
                       substrate_wood={"conifer": "always", "dead": "always",
                                       "bare_wood": "usually"},
                       cap_surface={"wrinkled": "usually", "smooth": "usually"},
                       cap_colour={"orange": "always", "yellow": "usually"},
                       flesh_consistency={"gelatinous": "always"},
                       flesh_colour={"orange": "usually", "white": "usually"},
                       odour={"none": "usually"},
                       growth_habit={"troop": "always", "fused": "usually"},
                       habitat={"conifer_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"conifer": "always"}),
         measurements=dict(cap_width_mm=[3, 20]),
         season=dict(months=list(range(1, 13)), note=ALL_YEAR),
         lookalikes=[dict(taxon="tremella_mesenterica",
                          discriminators=["associated_tree"],
                          note="Witch's butter is the same orange and grows on dead "
                               "hardwood; this one is on conifer, and has a white patch "
                               "where it was attached.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="trametes_gibbosa", scientificName="Trametes gibbosa",
         commonName="lumpy bracket", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Tough and not eaten. Nothing documented against it.", NAMA[0]),
         note="A thick whitish bracket, often lumpy where it meets the wood and commonly "
              "green with algae across the top by the end of a wet winter. The pores are "
              "stretched into slots running outwards rather than round.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"polypore": "always"},
                       substrate_wood={"broadleaf": "always", "dead": "always",
                                       "bare_wood": "usually"},
                       hymenophore_colour={"white": "usually", "cream": "usually"},
                       cap_surface={"velvety": "usually", "dry": "always",
                                    "zoned": "sometimes"},
                       cap_colour={"white": "always", "green": "usually"},
                       flesh_consistency={"leathery": "always", "woody": "sometimes"},
                       odour={"none": "usually"},
                       growth_habit={"single": "usually", "few": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[50, 200]),
         season=dict(months=list(range(1, 13)), note=ALL_YEAR),
         lookalikes=[dict(taxon="trametes_versicolor",
                          discriminators=["cap_colour"],
                          note="Turkey tail is banded in browns, greys and buff and is "
                               "thin. This is thick, white, and goes green with algae "
                               "rather than banded.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="daedaleopsis_confragosa", scientificName="Daedaleopsis confragosa",
         commonName="thin-walled maze polypore", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Tough and not eaten. Nothing documented against it.", NAMA[0]),
         note="A reddish-brown bracket, most often on willow and birch, whose underside "
              "is a maze of stretched pores rather than round holes. Press the underside "
              "of a fresh one and the mark goes red.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"polypore": "always"},
                       substrate_wood={"broadleaf": "always", "dead": "always",
                                       "living": "sometimes"},
                       hymenophore_colour={"white": "usually", "tan": "usually"},
                       cap_surface={"zoned": "usually", "wrinkled": "usually",
                                    "dry": "always"},
                       cap_colour={"red_brown": "always", "pink": "sometimes"},
                       bruising={"yes": "always"},
                       bruising_where={"hymenophore": "always"},
                       bruising_colour={"red": "usually", "red_brown": "usually"},
                       flesh_consistency={"leathery": "always"},
                       odour={"none": "usually"},
                       growth_habit={"single": "usually", "tiered": "usually"},
                       habitat={"broadleaf_wood": "usually", "wetland": "sometimes"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[40, 150]),
         season=dict(months=list(range(1, 13)), note=ALL_YEAR),
         lookalikes=[dict(taxon="trametes_versicolor",
                          discriminators=["cap_colour"],
                          note="Turkey tail is banded in greys and buff with fine round "
                               "pores. This is reddish-brown with a maze underneath that "
                               "bruises red.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="trametes_conchifer", scientificName="Trametes conchifer",
         commonName="little nest polypore", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Far too small and tough to be eaten. Nothing documented against it.",
                   NAMA[0]),
         note="A small white bracket that grows out of the rim of a shallow cup it made "
              "first, so a twig carries what looks like a little nest with a shelf coming "
              "off it. Once seen it is not mistaken for anything.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"polypore": "always"},
                       substrate_wood={"broadleaf": "always", "dead": "always",
                                       "bare_wood": "usually"},
                       hymenophore_colour={"white": "usually", "cream": "usually"},
                       cap_surface={"zoned": "usually", "smooth": "usually",
                                    "dry": "always"},
                       cap_colour={"white": "always"},
                       flesh_consistency={"leathery": "always"},
                       odour={"none": "usually"},
                       growth_habit={"troop": "usually", "single": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[5, 30]),
         season=dict(months=list(range(1, 13)), note=ALL_YEAR),
         lookalikes=[dict(taxon="trametes_versicolor",
                          discriminators=["cap_colour"],
                          note="Both are small zoned brackets on fallen hardwood. This "
                               "one is white and sits on a cup it grew from; turkey tail "
                               "is banded in colour and has no cup.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="xylobolus_frustulatus", scientificName="Xylobolus frustulatus",
         commonName="ceramic parchment", prevalence="occasional", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "A hard crust on old wood. Nothing documented against it.", NAMA[0]),
         note="A hard pale crust on long-dead bare oak, cracked into small tiles with "
              "dark lines between them, like crazed glaze. It is stuck fast and does not "
              "peel; the tiles are the whole identification.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"resupinate": "always"},
                       substrate_wood={"broadleaf": "always", "dead": "always",
                                       "bare_wood": "always"},
                       cap_surface={"cracked": "always", "dry": "always"},
                       cap_colour={"cream": "usually", "grey": "usually",
                                   "tan": "sometimes"},
                       flesh_consistency={"woody": "always"},
                       odour={"none": "usually"},
                       growth_habit={"fused": "always"},
                       habitat={"broadleaf_wood": "usually"},
                       associated_tree={"broadleaf": "always"}),
         measurements=dict(cap_width_mm=[20, 300]),
         season=dict(months=list(range(1, 13)), note=ALL_YEAR),
         lookalikes=[dict(taxon="stereum_complicatum",
                          discriminators=["fruitbody_type"],
                          note="The parchments make thin brackets with a free edge. This "
                               "is flat against the wood and cracked into tiles.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="gloeophyllum_sepiarium", scientificName="Gloeophyllum sepiarium",
         commonName="conifer mazegill", prevalence="occasional", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Tough and not eaten. Nothing documented against it.", NAMA[0]),
         note="A rusty-brown bracket on cut conifer — stacked timber, rails, stumps — "
              "with a hairy top and an underside of blades and slots rather than round "
              "pores. The growing edge is usually a brighter orange than the rest.",
         characters=ch(**ON_WOOD,
                       fruitbody_type={"polypore": "always"},
                       substrate_wood={"conifer": "always", "dead": "always",
                                       "bare_wood": "usually"},
                       hymenophore_colour={"yellow_brown": "usually",
                                           "orange": "sometimes"},
                       cap_surface={"hairy": "always", "zoned": "usually",
                                    "dry": "always"},
                       cap_colour={"red_brown": "always", "orange": "usually",
                                   "dark_brown": "usually"},
                       flesh_consistency={"leathery": "always"},
                       odour={"none": "usually"},
                       growth_habit={"tiered": "usually", "troop": "usually"},
                       habitat={"conifer_wood": "usually", "disturbed": "sometimes"},
                       associated_tree={"conifer": "always"}),
         measurements=dict(cap_width_mm=[20, 100]),
         season=dict(months=list(range(1, 13)), note=ALL_YEAR),
         lookalikes=[dict(taxon="daedaleopsis_confragosa",
                          discriminators=["associated_tree"],
                          note="Both have a maze underneath. This is on conifer and rusty "
                               "brown; the other is on hardwood and greyer, and bruises "
                               "red.")],
         sources=INAT + MORPH, reviewed=False),
]


def main():
    with open(PACK, encoding="utf-8") as handle:
        pack = json.load(handle, object_pairs_hook=collections.OrderedDict)

    have = {t["id"] for t in pack["taxa"]}
    added = 0
    for taxon in TAXA:
        if taxon["id"] in have:
            print(f"  already here: {taxon['id']}")
            continue
        pack["taxa"].append(collections.OrderedDict(taxon))
        added += 1
        print(f"  + {taxon['scientificName']}")

    pack["taxa"].sort(key=lambda t: t["scientificName"])
    with open(PACK, "w", encoding="utf-8") as handle:
        json.dump(pack, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print(f"\n{added} added; the pack now holds {len(pack['taxa'])}.")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Seventh batch: the two genera you cannot walk past in July and the pack could not hold.

A genus audit of the hundred found *Russula* with one species in it and the milkcaps with
three. In Appalachian summer woods those are the two most abundant gilled genera on the
forest floor — a person using this app in July meets them constantly, and the pack could
barely represent either. Everything keyed out as a russula was going to land on
*R. virescens* or nothing.

They also teach each other, which is why they are in one batch. A russula and a milkcap
are the same shape and are told apart in two seconds by breaking a gill: milk or no milk.
Then within each, the character that settles it is one nobody thinks to use — taste for
the russulas, and what the milk does to the air for the milkcaps.

Three more fill holes that showed up in the same audit: a second Tylopilus so the bitter
bolete has something to be bitter against, the first Gymnopilus, and the small chanterelle
that gets called a young big one.

Same rules as every batch: macro-morphology any guide agrees on, no text reproduced,
reviewed false.

    python3 tools/add-taxa-7.py
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


GBIF = ["GBIF occurrence counts over a southern Blue Ridge polygon"]
NAMA = ["North American Mycological Association poisoning references"]
MORPH = ["Macromorphology as standard North American field guides agree it — "
         "written out, not reproduced"]

RUSSULA = dict(
    fruitbody_type="gilled_stemmed", substrate="soil",
    stipe_presence="central", stipe_base={"equal": "usually"},
    ring={"absent": "always"}, latex="none",
    flesh_consistency={"brittle": "always"},
)

MILKCAP = dict(
    fruitbody_type="gilled_stemmed", substrate="soil",
    stipe_presence="central", stipe_base={"equal": "usually",
                                          "tapering": "sometimes"},
    ring={"absent": "always"}, latex={"present": "always"},
    flesh_consistency={"brittle": "always"},
)

TAXA = [
    dict(id="russula_compacta", scientificName="Russula compacta",
         commonName="firm russula", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Not eaten here, mostly on account of the smell.", NAMA[0]),
         note="Heavy and firm for a russula, whitish then browning all over as it ages "
              "or is handled. The smell turns strongly of old fish or shellfish as it "
              "goes over, which is what settles it — most russulas smell of nothing much.",
         characters=ch(**RUSSULA,
                       stipe_surface={"smooth": "usually"},
                       stipe_flesh={"solid": "always"},
                       gill_attachment={"adnate": "usually", "adnexed": "sometimes"},
                       gill_spacing={"close": "usually"},
                       hymenophore_colour={"white": "usually", "cream": "usually",
                                           "tan": "sometimes"},
                       cap_shape={"convex": "usually", "flat": "usually",
                                  "depressed": "sometimes"},
                       cap_margin={"straight": "usually"},
                       cap_surface={"dry": "usually", "smooth": "usually",
                                    "cracked": "sometimes"},
                       cap_colour={"white": "usually", "tan": "usually",
                                   "red_brown": "usually"},
                       flesh_colour={"white": "always", "dark_brown": "usually"},
                       bruising={"yes": "always"},
                       bruising_where={"flesh": "always", "cap": "usually",
                                       "hymenophore": "usually"},
                       bruising_colour={"red_brown": "always", "dark_brown": "usually"},
                       bruising_speed={"slow": "usually"},
                       odour={"foetid": "usually", "unpleasant_other": "usually"},
                       taste={"mild": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"},
                       spore_print="pale",
                       growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[40, 110]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[dict(taxon="russula_virescens",
                          discriminators=["cap_colour", "cap_surface", "bruising"],
                          note="The green-cracking russula is green and cracked into "
                               "patches, and it does not brown where it is handled.")],
         sources=GBIF + MORPH, reviewed=False),

    dict(id="russula_variata", scientificName="Russula variata",
         commonName="variable russula", prevalence="common", sought=True,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Eaten by some. Russula is a genus where the acrid ones are the "
                   "trouble and taste is how they are told apart.", NAMA[0]),
         note="Gills that fork repeatedly from the stem outwards — run a finger across "
              "them and count the forks. The cap is mottled green, purple and buff in "
              "the same specimen, which is where the name comes from and why colour will "
              "not settle it.",
         characters=ch(**RUSSULA,
                       stipe_surface={"smooth": "usually"},
                       stipe_flesh={"solid": "usually", "stuffed": "sometimes"},
                       gill_attachment={"adnate": "usually", "decurrent": "sometimes"},
                       gill_extras={"forked": "always"},
                       gill_spacing={"close": "usually", "crowded": "sometimes"},
                       hymenophore_colour={"white": "always", "cream": "sometimes"},
                       cap_shape={"convex": "usually", "depressed": "usually"},
                       cap_margin={"straight": "usually", "striate": "sometimes"},
                       cap_surface={"dry": "usually", "smooth": "usually",
                                    "viscid": "sometimes"},
                       cap_colour={"green": "usually", "purple": "usually",
                                   "cream": "sometimes", "olive": "sometimes"},
                       cap_colour_pattern={"darker_centre": "usually"},
                       flesh_colour={"white": "always"},
                       bruising={"no": "usually"},
                       odour={"none": "usually"},
                       taste={"mild": "usually", "acrid": "sometimes"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"},
                       spore_print="pale",
                       growth_habit={"few": "usually", "troop": "sometimes"}),
         measurements=dict(cap_width_mm=[50, 140], stipe_height_mm=[40, 100]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[dict(taxon="russula_virescens",
                          discriminators=["gill_extras", "cap_surface"],
                          note="The green-cracking russula does not fork its gills and "
                               "its cap breaks into distinct patches.")],
         sources=GBIF + MORPH, reviewed=False),

    dict(id="russula_silvicola", scientificName="Russula silvicola",
         commonName="woodland sickener", prevalence="common", sought=False,
         hazard=hz("unknown", "GI", "30 minutes to 3 hours",
                   "Fiercely acrid, and it makes people vomit. The taste is the warning "
                   "and it arrives long before you have swallowed anything.", NAMA[0]),
         note="Bright scarlet cap, pure white stem and gills, and a taste like chewing a "
              "chilli — a fragment on the tongue is unmistakable within seconds. The red "
              "washes out in rain to pink or nearly white, so colour alone settles "
              "nothing.",
         characters=ch(**RUSSULA,
                       stipe_surface={"smooth": "usually"},
                       stipe_flesh={"stuffed": "usually", "hollow": "sometimes"},
                       gill_attachment={"adnexed": "usually", "adnate": "sometimes"},
                       gill_spacing={"close": "usually"},
                       hymenophore_colour={"white": "always"},
                       cap_shape={"convex": "usually", "flat": "usually",
                                  "depressed": "sometimes"},
                       cap_margin={"striate": "usually", "straight": "sometimes"},
                       cap_surface={"viscid": "usually", "smooth": "usually"},
                       cap_colour={"red": "always", "pink": "sometimes",
                                   "white": "sometimes"},
                       flesh_colour={"white": "always"},
                       bruising={"no": "usually"},
                       odour={"none": "usually", "fruity": "sometimes"},
                       taste={"acrid": "always"},
                       habitat={"conifer_wood": "usually", "mixed_wood": "usually",
                                "broadleaf_wood": "sometimes"},
                       spore_print="pale",
                       growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[25, 90], stipe_height_mm=[30, 90]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[dict(taxon="russula_mariae",
                          discriminators=["taste", "cap_surface", "stipe_surface"],
                          note="Both are red. Mariae is velvety and dry with a flushed "
                               "stem and a mild taste; this one is sticky, white-stemmed "
                               "and violently acrid.")],
         sources=GBIF + NAMA + MORPH, reviewed=False),

    dict(id="russula_mariae", scientificName="Russula mariae",
         commonName="purple-bloom russula", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Eaten by some. In a genus told apart by taste, a russula nobody has "
                   "tasted is a russula nobody knows.", NAMA[0]),
         note="Dry and velvety rather than sticky, deep purple-red with a powdery bloom, "
              "and the stem is flushed pink rather than white. Common under oaks, and one "
              "of the few russulas that can be recognised without tasting it.",
         characters=ch(**RUSSULA,
                       stipe_surface={"powdery": "usually", "smooth": "sometimes"},
                       stipe_flesh={"stuffed": "usually"},
                       gill_attachment={"adnate": "usually", "adnexed": "sometimes"},
                       gill_spacing={"close": "usually"},
                       hymenophore_colour={"white": "usually", "cream": "usually"},
                       cap_shape={"convex": "usually", "flat": "usually",
                                  "depressed": "sometimes"},
                       cap_margin={"straight": "usually"},
                       cap_surface={"velvety": "always", "dry": "always",
                                    "powdery": "usually"},
                       cap_colour={"purple": "always", "red": "usually",
                                   "pink": "sometimes"},
                       flesh_colour={"white": "always", "pink": "sometimes"},
                       bruising={"no": "usually"},
                       odour={"none": "usually"},
                       taste={"mild": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
                       associated_tree={"broadleaf": "always"},
                       spore_print="pale",
                       growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[20, 80], stipe_height_mm=[30, 80]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[], sources=GBIF + MORPH, reviewed=False),

    dict(id="lactarius_corrugis", scientificName="Lactarius corrugis",
         commonName="corrugated milkcap", prevalence="common", sought=True,
         hazard=hz(),
         note="Dark red-brown and wrinkled like old leather, bleeding copious white milk "
              "that browns within a minute and stains everything it touches. The wrinkles "
              "and the browning milk are the pair that settle it.",
         characters=ch(**MILKCAP,
                       stipe_surface={"hairy": "usually", "smooth": "sometimes"},
                       stipe_flesh={"solid": "always"},
                       gill_attachment={"adnate": "usually", "decurrent": "sometimes"},
                       gill_spacing={"close": "usually", "crowded": "sometimes"},
                       hymenophore_colour={"cream": "usually", "orange": "usually",
                                           "tan": "sometimes"},
                       cap_shape={"convex": "usually", "depressed": "usually"},
                       cap_margin={"incurved": "usually"},
                       cap_surface={"wrinkled": "always", "velvety": "usually",
                                    "dry": "always"},
                       cap_colour={"red_brown": "always", "dark_brown": "usually",
                                   "orange": "sometimes"},
                       flesh_colour={"white": "always"},
                       latex_colour={"white": "always"},
                       latex_change={"yes": "always"},
                       latex_change_colour={"dark_brown": "always"},
                       bruising={"yes": "always"},
                       bruising_where={"flesh": "usually", "hymenophore": "usually"},
                       bruising_colour={"dark_brown": "always"},
                       odour={"none": "usually", "mushroomy": "sometimes"},
                       taste={"mild": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
                       associated_tree={"broadleaf": "always"},
                       spore_print="pale",
                       growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[50, 140], stipe_height_mm=[40, 110]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[dict(taxon="lactifluus_volemus",
                          discriminators=["cap_surface", "odour"],
                          note="The weeping milkcap is smooth rather than wrinkled and "
                               "smells strongly of fish. Both bleed white milk that "
                               "browns.")],
         sources=GBIF + MORPH, reviewed=False),

    dict(id="lactifluus_hygrophoroides", scientificName="Lactifluus hygrophoroides",
         commonName="hygrophorus milkcap", prevalence="common", sought=True,
         hazard=hz(),
         note="Orange-brown and velvety, with gills so widely spaced you can count them "
              "at arm's length. White milk that does not change colour. The spacing is "
              "the character — nothing else here has gills like that.",
         characters=ch(**MILKCAP,
                       stipe_surface={"hairy": "usually", "smooth": "sometimes"},
                       stipe_flesh={"solid": "always"},
                       gill_attachment={"adnate": "usually", "decurrent": "usually"},
                       gill_spacing={"distant": "always"},
                       hymenophore_colour={"white": "usually", "cream": "usually"},
                       cap_shape={"convex": "usually", "depressed": "usually"},
                       cap_margin={"incurved": "usually", "wavy": "sometimes"},
                       cap_surface={"velvety": "always", "dry": "always",
                                    "wrinkled": "sometimes"},
                       cap_colour={"orange": "always", "red_brown": "usually",
                                   "tan": "sometimes"},
                       flesh_colour={"white": "always"},
                       latex_colour={"white": "always"},
                       latex_change={"no": "always"},
                       bruising={"no": "usually"},
                       odour={"none": "usually"},
                       taste={"mild": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
                       associated_tree={"broadleaf": "always"},
                       spore_print="pale",
                       growth_habit={"few": "usually", "troop": "sometimes"}),
         measurements=dict(cap_width_mm=[30, 110], stipe_height_mm=[25, 80]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[dict(taxon="lactifluus_volemus",
                          discriminators=["gill_spacing", "odour", "latex_change"],
                          note="The weeping milkcap has crowded gills, smells of fish, "
                               "and its milk browns. This one has gills you can count "
                               "and milk that stays white.")],
         sources=GBIF + MORPH, reviewed=False),

    dict(id="lactifluus_subvellereus", scientificName="Lactifluus subvellereus",
         commonName="white acrid milkcap", prevalence="common", sought=False,
         hazard=hz("unknown", "GI", "1-3 hours",
                   "Acrid enough to make people ill. The taste says so at once.",
                   NAMA[0]),
         note="A big chalk-white funnel with crowded gills and a short stubby stem, "
              "bleeding copious white milk that is fiercely acrid on the tongue. Common "
              "under oaks and often half-buried in leaf litter.",
         characters=ch(**MILKCAP,
                       stipe_surface={"hairy": "usually", "smooth": "sometimes"},
                       stipe_flesh={"solid": "always"},
                       gill_attachment={"adnate": "usually", "decurrent": "usually"},
                       gill_spacing={"crowded": "always"},
                       hymenophore_colour={"white": "always", "cream": "sometimes"},
                       cap_shape={"depressed": "usually", "funnel": "usually",
                                  "convex": "sometimes"},
                       cap_margin={"inrolled": "always"},
                       cap_surface={"velvety": "usually", "dry": "always"},
                       cap_colour={"white": "always", "cream": "usually"},
                       flesh_colour={"white": "always"},
                       latex_colour={"white": "always"},
                       latex_change={"no": "usually"},
                       bruising={"yes": "sometimes"},
                       bruising_where={"hymenophore": "usually"},
                       bruising_colour={"tan": "sometimes"},
                       odour={"none": "usually"},
                       taste={"acrid": "always"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
                       associated_tree={"broadleaf": "always"},
                       spore_print="pale",
                       growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[60, 160], stipe_height_mm=[20, 60]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[], sources=GBIF + NAMA + MORPH, reviewed=False),

    dict(id="tylopilus_plumbeoviolaceus", scientificName="Tylopilus plumbeoviolaceus",
         commonName="violet-grey bolete", prevalence="occasional", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Not eaten, on account of being inedibly bitter rather than toxic.",
                   NAMA[0]),
         note="Deep violet fading to grey-brown, over pores that start white and go "
              "pink. Bitter enough that a fragment on the tongue ends the question. The "
              "stem keeps its violet longer than the cap does.",
         characters=ch(
             fruitbody_type="bolete", substrate="soil",
             stipe_presence="central", stipe_base={"equal": "usually",
                                                   "bulbous": "sometimes"},
             stipe_surface={"smooth": "usually", "reticulate": "sometimes"},
             stipe_flesh={"solid": "always"}, ring={"absent": "always"},
             hymenophore_colour={"white": "usually", "pink": "usually"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_margin={"incurved": "usually"},
             cap_surface={"dry": "always", "velvety": "usually"},
             cap_colour={"purple": "always", "grey": "usually",
                         "dark_brown": "sometimes"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
             latex="none", bruising={"no": "usually"},
             odour={"none": "usually"}, taste={"bitter": "always"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             associated_tree={"broadleaf": "always"},
             spore_print={"pink": "always"}, spore_print_fine={"pink_salmon": "always"},
             growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[50, 140], stipe_height_mm=[60, 130]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[dict(taxon="tylopilus_felleus",
                          discriminators=["cap_colour", "stipe_surface"],
                          note="The bitter bolete is plain brown with a coarse dark net "
                               "on the stem. Both are bitter, which is the point: taste "
                               "settles a bolete faster than anything else.")],
         sources=GBIF + MORPH, reviewed=False),

    dict(id="gymnopilus_luteofolius", scientificName="Gymnopilus luteofolius",
         commonName="yellow-gilled rustgill", prevalence="occasional", sought=False,
         hazard=hz("psilocybin", "INTOXICATION", "20 minutes to 2 hours",
                   "Some collections are psychoactive and some are not, which is its own "
                   "reason to leave it alone.", NAMA[0]),
         note="Clustered on dead hardwood and woodchips, purple-red and scaly when young, "
              "fading to tawny. Yellow gills that rust with the spores, and a rust-brown "
              "print — bitter, and nothing about it is subtle.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="wood",
             substrate_wood={"dead": "always", "broadleaf": "usually",
                             "woodchip": "usually", "conifer": "sometimes"},
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"fibrous": "usually", "scaly": "sometimes"},
             stipe_flesh={"stuffed": "usually", "hollow": "sometimes"},
             ring={"zone": "usually", "absent": "sometimes"},
             gill_attachment={"adnate": "usually", "notched": "sometimes"},
             gill_spacing={"close": "usually", "crowded": "sometimes"},
             hymenophore_colour={"yellow": "usually", "red_brown": "usually"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_margin={"incurved": "usually"},
             cap_surface={"scaly": "always", "dry": "always", "silky": "usually"},
             cap_colour={"purple": "usually", "red": "usually", "tan": "usually"},
             flesh_colour={"yellow": "usually", "red_brown": "sometimes"},
             flesh_consistency={"fibrous": "usually"},
             latex="none", bruising={"yes": "sometimes"},
             bruising_where={"flesh": "usually"},
             bruising_colour={"red_brown": "sometimes"},
             odour={"none": "usually"}, taste={"bitter": "always"},
             habitat={"broadleaf_wood": "usually", "disturbed": "usually",
                      "garden": "sometimes", "urban": "sometimes"},
             spore_print={"brown": "always"}, spore_print_fine={"rust": "always"},
             growth_habit={"caespitose": "always", "troop": "usually"}),
         measurements=dict(cap_width_mm=[20, 90], stipe_height_mm=[30, 100]),
         season=dict(months=[6, 7, 8, 9, 10, 11]),
         lookalikes=[dict(taxon="galerina_marginata",
                          discriminators=["hymenophore_colour", "taste", "size"],
                          note="Both cluster on dead wood and print rust-brown. Galerina "
                               "is small, plain tawny and mild; this is larger, scaly, "
                               "purple-tinged and bitter. Getting it wrong is fatal — "
                               "take the print and look at the gill colour.")],
         sources=GBIF + NAMA + MORPH, reviewed=False),

    dict(id="cantharellus_minor", scientificName="Cantharellus minor",
         commonName="small chanterelle", prevalence="common", sought=True,
         hazard=hz(),
         note="A chanterelle the size of a fingernail, orange throughout, coming up in "
              "troops through moss and leaf litter. It is not a young big one — it is "
              "full-grown at two centimetres, and there will be dozens.",
         characters=ch(
             fruitbody_type="chanterelle_like", substrate={"soil": "always",
                                                           "moss": "usually",
                                                           "litter": "usually"},
             stipe_presence="central", stipe_base={"tapering": "usually"},
             stipe_surface={"smooth": "always"}, stipe_flesh={"hollow": "sometimes",
                                                              "stuffed": "usually"},
             ring={"absent": "always"},
             gill_attachment={"decurrent": "always"},
             gill_extras={"forked": "usually", "veined": "usually"},
             gill_spacing={"distant": "usually"},
             hymenophore_colour={"orange": "always", "yellow": "sometimes"},
             cap_shape={"convex": "sometimes", "flat": "usually",
                        "depressed": "usually"},
             cap_margin={"wavy": "usually", "uplifted": "sometimes"},
             cap_surface={"smooth": "always", "dry": "always"},
             cap_colour={"orange": "always", "yellow": "sometimes"},
             flesh_colour={"orange": "usually", "cream": "sometimes"},
             flesh_consistency={"soft": "usually", "fibrous": "sometimes"},
             latex="none", bruising={"no": "usually"},
             odour={"fruity": "sometimes", "none": "usually"},
             taste={"mild": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             associated_tree={"broadleaf": "usually"},
             spore_print="pale",
             growth_habit={"troop": "always", "few": "sometimes"}),
         measurements=dict(cap_width_mm=[5, 30], stipe_height_mm=[10, 50]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[dict(taxon="hygrophoropsis_aurantiaca",
                          discriminators=["gill_extras", "gill_spacing", "substrate"],
                          note="The false chanterelle has thin crowded gills that break "
                               "away, and grows on wood or conifer litter. This has blunt "
                               "ridges and comes out of moss and soil.")],
         sources=GBIF + MORPH, reviewed=False),
]


def main():
    with open(PACK) as f:
        pack = json.load(f, object_pairs_hook=collections.OrderedDict)
    have = {t["id"] for t in pack["taxa"]}
    added = []
    for t in TAXA:
        if t["id"] in have:
            print(f"  skip {t['id']}")
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

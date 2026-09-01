#!/usr/bin/env python3
"""
Sixth batch: the fourteen things you cannot walk past here and the pack did not hold.

Chosen the same way the fifth was — what a person actually meets — but this time from
what is conspicuously absent rather than what is structurally broken. Dead man's
fingers, split gill and the violet-toothed polypore are on every fallen hardwood in
these mountains and were in none of the eighty-six rows. So was the cleft-foot Amanita,
which is the commonest Amanita in Appalachian oak woods.

Three earn their place by what they teach:

- **Schizophyllum commune** has gills split lengthwise down every edge, which is the
  clearest possible instance of a gill edge being a character at all.
- **Marasmius rotula** hangs its gills on a free collar around the stem rather than
  attaching them to it — a state the schema has no word for, and the drawing carries it.
- **Baorangia bicolor** bruises blue in seconds, standing next to the red-pored bolete
  it is confused with, which does the same thing faster and makes people ill.

Same rules as every batch: macro-morphology any guide agrees on, no text reproduced,
reviewed false.

    python3 tools/add-taxa-6.py
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

TAXA = [
    dict(id="xylaria_polymorpha", scientificName="Xylaria polymorpha",
         commonName="dead man's fingers", prevalence="common", sought=False,
         hazard=hz(),
         note="Black clubs standing out of rotting hardwood like fingers, hard as wood "
              "and white inside when cut. It does not rot away, so it is there in every "
              "month of the year.",
         characters=ch(
             fruitbody_type={"coral_club": "usually", "flask": "sometimes"},
             substrate="wood", substrate_wood={"broadleaf": "always", "dead": "always",
                                               "buried": "sometimes"},
             stipe_presence={"central": "usually"},
             cap_surface={"wrinkled": "usually", "cracked": "sometimes",
                          "powdery": "sometimes"},
             cap_colour={"black": "always", "grey": "sometimes"},
             flesh_colour={"white": "always", "black": "usually"},
             flesh_consistency={"woody": "always", "leathery": "sometimes"},
             latex="none", bruising={"no": "always"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
             associated_tree={"broadleaf": "always"},
             growth_habit={"few": "usually", "caespitose": "usually"}),
         measurements=dict(cap_width_mm=[10, 30], stipe_height_mm=[30, 100]),
         season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
         lookalikes=[dict(taxon="daldinia_childiae",
                          discriminators=["cap_shape", "flesh_colour"],
                          note="Carbon balls are round and ringed inside in concentric "
                               "bands. These are upright clubs, white inside.")],
         sources=GBIF, reviewed=False),

    dict(id="schizophyllum_commune", scientificName="Schizophyllum commune",
         commonName="split gill", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Tough and not eaten here. Rarely implicated in infection in people "
                   "already seriously unwell.", NAMA[0]),
         note="Every gill edge is split down its length and rolls back in dry weather, "
              "which nothing else does. Small, grey, hairy, and on almost any dead "
              "hardwood.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="wood",
             substrate_wood={"broadleaf": "usually", "dead": "always",
                             "bark": "sometimes", "conifer": "sometimes"},
             stipe_presence={"absent_attached": "always"},
             gill_attachment={"no_stem": "always"},
             gill_edge={"serrate": "always", "different_colour": "sometimes"},
             gill_spacing={"distant": "usually", "close": "sometimes"},
             hymenophore_colour={"grey": "usually", "white": "sometimes",
                                 "pink": "sometimes"},
             cap_shape={"flat": "usually"}, cap_margin={"wavy": "usually"},
             cap_surface={"hairy": "always", "dry": "always"},
             cap_colour={"grey": "always", "white": "usually", "tan": "sometimes"},
             flesh_consistency={"leathery": "always"},
             flesh_colour={"white": "usually", "grey": "usually"},
             latex="none", bruising={"no": "always"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "urban": "sometimes",
                      "disturbed": "sometimes"},
             spore_print="pale", growth_habit={"troop": "usually", "tiered": "usually"}),
         measurements=dict(cap_width_mm=[10, 40]),
         season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
         lookalikes=[dict(taxon="panellus_stipticus",
                          discriminators=["gill_edge", "cap_surface"],
                          note="The bitter oyster has ordinary even gill edges and a "
                               "smoother cap. Split gill is hairy and every edge is "
                               "split in two.")],
         sources=GBIF, reviewed=False),

    dict(id="trichaptum_biforme", scientificName="Trichaptum biforme",
         commonName="violet-toothed polypore", prevalence="common", sought=False,
         hazard=hz(),
         note="Thin tiered brackets on dead hardwood with a purple underside that breaks "
              "up into teeth as it ages. The violet fades to buff in old ones, and the "
              "torn toothy underside is what is left to go on.",
         characters=ch(
             fruitbody_type={"polypore": "usually", "toothed": "usually"},
             substrate="wood",
             substrate_wood={"broadleaf": "always", "dead": "always", "bark": "usually"},
             stipe_presence={"absent_attached": "always"},
             hymenophore_colour={"purple": "usually", "white": "sometimes",
                                 "tan": "sometimes"},
             cap_shape={"flat": "usually"},
             cap_margin={"wavy": "usually", "straight": "sometimes"},
             cap_surface={"hairy": "always", "zoned": "usually", "dry": "always"},
             cap_colour={"white": "usually", "grey": "usually", "green": "sometimes"},
             flesh_consistency={"leathery": "always"},
             flesh_colour={"white": "always"},
             latex="none", bruising={"no": "always"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"tiered": "always", "troop": "usually"}),
         measurements=dict(cap_width_mm=[10, 60]),
         season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
         lookalikes=[dict(taxon="trametes_versicolor",
                          discriminators=["hymenophore_colour", "fruitbody_type"],
                          note="Turkey tail keeps fine round white pores. This one is "
                               "violet underneath when fresh and tears into teeth.")],
         sources=GBIF, reviewed=False),

    dict(id="coprinellus_micaceus", scientificName="Coprinellus micaceus",
         commonName="mica cap", prevalence="common", sought=True,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Eaten by some. Several small inky caps react badly with alcohol; "
                   "this is not the species usually named for it, which is not the same "
                   "as a clean bill.", NAMA[0]),
         note="Dense clusters at the foot of hardwood stumps, tan and grooved, dusted "
              "when young with mica-like flecks that rain washes off. Gills blacken and "
              "the cap dissolves within a day.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="wood",
             substrate_wood={"broadleaf": "always", "dead": "always", "buried": "usually"},
             stipe_presence="central", stipe_flesh={"hollow": "always"},
             stipe_surface={"smooth": "usually", "powdery": "sometimes"},
             ring={"absent": "always"}, stipe_base={"equal": "usually"},
             gill_attachment={"adnexed": "usually", "free": "sometimes"},
             gill_spacing={"crowded": "always"},
             hymenophore_colour={"white": "usually", "grey": "usually", "black": "usually"},
             cap_shape={"egg": "usually", "bell": "always"},
             cap_margin={"striate": "always", "uplifted": "sometimes"},
             cap_surface={"powdery": "usually", "smooth": "sometimes",
                          "striate": "always"},
             cap_colour={"tan": "always", "yellow_brown": "usually"},
             veil_remnants={"warts": "usually", "none": "sometimes"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "urban": "usually",
                      "disturbed": "usually"},
             spore_print={"dark": "always"}, spore_print_fine={"black": "always"},
             growth_habit={"caespitose": "always", "troop": "usually"}),
         measurements=dict(cap_width_mm=[10, 40], stipe_height_mm=[30, 80]),
         season=dict(months=[3, 4, 5, 6, 7, 8, 9, 10, 11]),
         lookalikes=[dict(taxon="coprinus_comatus",
                          discriminators=["size", "cap_shape", "growth_habit"],
                          note="Shaggy mane is much larger, cylindrical and shaggy, and "
                               "stands alone in grass rather than clustered on wood.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="amanita_brunnescens", scientificName="Amanita brunnescens",
         commonName="cleft-foot amanita", prevalence="common", sought=False,
         hazard=hz("unknown", "SEVERE", "—",
                   "Treated as poisonous. In this genus that is where the matter ends.",
                   NAMA[0]),
         note="The commonest Amanita in oak woods here. The bulb at the base is split "
              "vertically, as though cleft with a knife, and the whole mushroom stains "
              "reddish brown where handled or where a slug has been.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="soil",
             stipe_presence="central",
             stipe_base={"marginate": "always", "bulbous": "usually"},
             stipe_surface={"smooth": "usually", "powdery": "sometimes"},
             ring={"skirt": "always"},
             gill_attachment={"free": "always"}, gill_spacing={"crowded": "usually"},
             cap_shape={"convex": "usually", "flat": "usually"},
             cap_margin={"straight": "usually"},
             cap_surface={"warty": "usually", "viscid": "sometimes",
                          "smooth": "sometimes"},
             cap_colour={"dark_brown": "usually", "grey": "usually", "white": "sometimes"},
             veil_remnants={"warts": "usually", "none": "sometimes"},
             hymenophore_colour={"white": "always"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
             latex="none", bruising={"yes": "always"},
             bruising_where={"stipe": "always", "cap": "usually", "flesh": "usually"},
             bruising_colour={"red_brown": "always", "dark_brown": "usually"},
             bruising_speed={"slow": "usually"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             associated_tree={"broadleaf": "usually"},
             spore_print="pale", growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[40, 110], stipe_height_mm=[60, 140]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=["stipe_base", "bruising", "cap_colour"],
                          note="A destroying angel is white throughout, does not stain, "
                               "and stands in a loose sac. This has a cleft bulb with no "
                               "free rim, and browns where it is handled."),
                     dict(taxon="amanita_citrina",
                          discriminators=["odour", "bruising", "cap_colour"],
                          note="The false death cap smells of raw potato and does not "
                               "brown; its bulb is round rather than cleft.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="strobilomyces_strobilaceus", scientificName="Strobilomyces strobilaceus",
         commonName="old man of the woods", prevalence="occasional", sought=True,
         hazard=hz(),
         note="Shaggy black scales all over a grey cap, on a stem shaggy to match. "
              "Cut flesh goes red and then black within a minute, which nothing else "
              "here does in that order.",
         characters=ch(
             fruitbody_type="bolete", substrate="soil",
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"scaly": "always", "fibrous": "usually"},
             stipe_flesh={"solid": "always"},
             ring={"zone": "sometimes", "absent": "usually"},
             hymenophore_colour={"white": "usually", "grey": "usually",
                                 "black": "sometimes"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_margin={"appendiculate": "usually"},
             cap_surface={"scaly": "always", "dry": "always", "hairy": "usually"},
             cap_colour={"black": "usually", "grey": "usually", "white": "sometimes"},
             flesh_colour={"white": "usually", "red": "usually", "black": "usually"},
             flesh_consistency={"soft": "always"},
             latex="none", bruising={"yes": "always"},
             bruising_where={"flesh": "always", "hymenophore": "usually"},
             bruising_colour={"red": "always", "black": "always"},
             bruising_speed={"minute": "always"},
             odour={"none": "usually", "mushroomy": "sometimes"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             associated_tree={"broadleaf": "usually"},
             spore_print={"dark": "usually"}, spore_print_fine={"black": "usually"},
             growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[40, 150], stipe_height_mm=[60, 140]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="baorangia_bicolor", scientificName="Baorangia bicolor",
         commonName="two-colored bolete", prevalence="common", sought=True,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Widely eaten here and it upsets some people. The genus holds species "
                   "that do worse.", NAMA[0]),
         note="Rose-red cap over a yellow pore surface, the stem red below and yellow "
              "at the top. Bruises blue slowly and unevenly rather than flashing.",
         characters=ch(
             fruitbody_type="bolete", substrate="soil",
             stipe_presence="central", stipe_base={"equal": "usually",
                                                   "tapering": "sometimes"},
             stipe_surface={"smooth": "usually", "punctate": "sometimes"},
             stipe_flesh={"solid": "always"}, ring={"absent": "always"},
             hymenophore_colour={"yellow": "always"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_margin={"incurved": "usually"},
             cap_surface={"velvety": "usually", "dry": "always", "cracked": "sometimes"},
             cap_colour={"red": "usually", "pink": "usually", "red_brown": "sometimes"},
             flesh_colour={"yellow": "always", "blue": "usually"},
             flesh_consistency={"soft": "always"},
             latex="none", bruising={"yes": "always"},
             bruising_where={"hymenophore": "always", "flesh": "usually"},
             bruising_colour={"blue": "always"},
             bruising_speed={"minute": "usually", "slow": "sometimes"},
             odour={"none": "usually", "mushroomy": "sometimes"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
             associated_tree={"broadleaf": "always"},
             spore_print={"brown": "always"}, spore_print_fine={"ochre": "usually"},
             growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[50, 120]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[dict(taxon="neoboletus_subvelutipes",
                          discriminators=["hymenophore_colour", "bruising_speed",
                                          "stipe_surface"],
                          note="The red-mouth bolete has red pore mouths and goes "
                               "blue-black the instant it is cut. This one has yellow "
                               "pores and blues slowly.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="cortinarius_iodes", scientificName="Cortinarius iodes",
         commonName="viscid violet cortinarius", prevalence="occasional", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Not eaten. The genus contains species whose damage to the kidneys "
                   "appears weeks later, so a webcap is not a thing to try.", NAMA[0]),
         note="Slimy violet cap developing yellow spots, over pale violet gills that go "
              "rust-brown with the spores. A cobweb veil when young leaves a rusty band, "
              "never a skirt.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="soil",
             stipe_presence="central", stipe_base={"bulbous": "usually",
                                                   "equal": "sometimes"},
             stipe_surface={"slimy": "usually", "fibrous": "sometimes"},
             ring={"zone": "always"},
             gill_attachment={"adnate": "usually", "notched": "sometimes"},
             gill_spacing={"close": "usually"},
             hymenophore_colour={"purple": "usually", "red_brown": "usually"},
             cap_shape={"convex": "usually", "flat": "sometimes",
                        "umbonate": "sometimes"},
             cap_margin={"incurved": "usually"},
             cap_surface={"viscid": "always", "smooth": "usually"},
             cap_colour={"purple": "always", "yellow": "sometimes"},
             cap_colour_pattern={"darker_centre": "sometimes", "uniform": "usually"},
             flesh_colour={"white": "usually", "purple": "sometimes"},
             flesh_consistency={"soft": "always"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             associated_tree={"broadleaf": "usually"},
             spore_print={"brown": "always"}, spore_print_fine={"rust": "always"},
             growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[20, 70], stipe_height_mm=[40, 90]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[dict(taxon="collybia_nuda",
                          discriminators=["spore_print", "ring", "cap_surface"],
                          note="A blewit prints pale pink and has no veil at all. Any "
                               "purple mushroom printing rust-brown is a webcap, and "
                               "webcaps are left alone.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="marasmius_rotula", scientificName="Marasmius rotula",
         commonName="pinwheel", prevalence="common", sought=False,
         hazard=hz(),
         note="Tiny white parachutes on black wiry stems, on twigs and buried sticks. "
              "The gills stop at a free collar around the stem instead of touching it, "
              "which is the whole of the identification.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"wood": "always",
                                                         "litter": "usually"},
             substrate_wood={"dead": "always", "broadleaf": "usually",
                             "buried": "sometimes"},
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"smooth": "always"}, stipe_flesh={"hollow": "always"},
             ring={"absent": "always"},
             gill_attachment={"free": "always"}, gill_spacing={"distant": "always"},
             gill_extras={"veined": "sometimes"},
             hymenophore_colour={"white": "always"},
             cap_shape={"convex": "usually", "umbilicate": "usually"},
             cap_margin={"striate": "always"},
             cap_surface={"striate": "always", "dry": "always"},
             cap_colour={"white": "always", "cream": "usually"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
             latex="none", bruising={"no": "always"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             spore_print="pale", growth_habit={"troop": "always"}),
         measurements=dict(cap_width_mm=[3, 20], stipe_height_mm=[20, 80]),
         season=dict(months=[4, 5, 6, 7, 8, 9, 10, 11]),
         lookalikes=[dict(taxon="marasmius_oreades",
                          discriminators=["size", "substrate", "gill_attachment"],
                          note="The fairy ring champignon is far bigger, grows in grass, "
                               "and its gills reach the stem. This is a few millimetres "
                               "across on a twig.")],
         sources=GBIF, reviewed=False),

    dict(id="clavulina_coralloides", scientificName="Clavulina coralloides",
         commonName="crested coral", prevalence="common", sought=False,
         hazard=hz(),
         note="White coral with flattened tips finely crested like antlers. Often half "
              "of it is greyed and blackened by a parasitic mould, which is normal and "
              "not decay.",
         characters=ch(
             fruitbody_type="coral_club", substrate={"soil": "always",
                                                     "litter": "usually"},
             stipe_presence={"central": "usually"},
             cap_colour={"white": "always", "cream": "usually", "grey": "sometimes"},
             cap_surface={"smooth": "usually", "wrinkled": "sometimes"},
             flesh_colour={"white": "always"},
             flesh_consistency={"brittle": "usually", "fibrous": "sometimes"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             taste={"mild": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually",
                      "conifer_wood": "sometimes"},
             spore_print="pale", growth_habit={"few": "usually", "troop": "usually"}),
         measurements=dict(cap_width_mm=[10, 50], stipe_height_mm=[20, 80]),
         season=dict(months=[6, 7, 8, 9, 10, 11]),
         lookalikes=[dict(taxon="ramaria_stricta",
                          discriminators=["cap_colour", "substrate", "taste"],
                          note="Upright coral is tan to ochre, grows straight out of "
                               "wood, and tastes bitter. This is white, on soil, and "
                               "crested at the tips.")],
         sources=GBIF, reviewed=False),

    dict(id="ganoderma_tsugae", scientificName="Ganoderma tsugae",
         commonName="hemlock varnish shelf", prevalence="occasional", sought=True,
         hazard=hz(),
         note="A lacquered red shelf with a white margin, on dead hemlock. Soft and "
              "wet-looking in spring, and hard by autumn; the varnish is real and will "
              "not rub off.",
         characters=ch(
             fruitbody_type="polypore", substrate="wood",
             substrate_wood={"conifer": "always", "dead": "always"},
             stipe_presence={"lateral": "usually", "absent_attached": "sometimes"},
             hymenophore_colour={"white": "usually", "cream": "sometimes"},
             cap_shape={"flat": "usually"},
             cap_margin={"straight": "usually", "wavy": "sometimes"},
             cap_surface={"smooth": "always", "zoned": "usually", "viscid": "sometimes"},
             cap_colour={"red": "always", "red_brown": "usually", "white": "usually",
                         "orange": "sometimes"},
             flesh_colour={"white": "usually", "tan": "sometimes"},
             flesh_consistency={"leathery": "usually", "woody": "sometimes"},
             latex="none", bruising={"yes": "sometimes"},
             bruising_where={"hymenophore": "usually"},
             bruising_colour={"dark_brown": "usually"},
             odour={"none": "usually"},
             habitat={"conifer_wood": "always", "mixed_wood": "sometimes"},
             associated_tree={"conifer": "always"},
             spore_print={"brown": "always"},
             growth_habit={"single": "usually", "tiered": "sometimes"}),
         measurements=dict(cap_width_mm=[50, 300]),
         season=dict(months=[4, 5, 6, 7, 8, 9, 10]),
         lookalikes=[dict(taxon="ganoderma_applanatum",
                          discriminators=["cap_surface", "cap_colour", "associated_tree"],
                          note="The artist's conk is dull grey-brown and unvarnished, on "
                               "hardwood, and its white underside bruises brown to a "
                               "fingernail. This is lacquered red, on hemlock.")],
         sources=GBIF, reviewed=False),

    dict(id="hericium_coralloides", scientificName="Hericium coralloides",
         commonName="comb tooth", prevalence="occasional", sought=True,
         hazard=hz(),
         note="A branched white mass carrying short teeth in rows along the branches, "
              "rather than one cushion with a long beard. On dead hardwood, often high "
              "up a standing trunk.",
         characters=ch(
             fruitbody_type="toothed", substrate="wood",
             substrate_wood={"broadleaf": "always", "dead": "always"},
             stipe_presence={"absent_attached": "always"},
             hymenophore_colour={"white": "always", "cream": "sometimes"},
             cap_colour={"white": "always", "cream": "usually"},
             cap_surface={"smooth": "usually"},
             flesh_colour={"white": "always"},
             flesh_consistency={"soft": "always", "fibrous": "sometimes"},
             latex="none", bruising={"yes": "sometimes"},
             bruising_where={"flesh": "usually"},
             bruising_colour={"yellow": "sometimes", "tan": "sometimes"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"single": "usually"}),
         measurements=dict(cap_width_mm=[50, 250]),
         season=dict(months=[8, 9, 10, 11]),
         lookalikes=[dict(taxon="hericium_erinaceus",
                          discriminators=["fruitbody_type", "growth_habit", "size"],
                          note="Lion's mane is one unbranched cushion hung with long "
                               "spines. This is a branched framework with short teeth "
                               "in rows.")],
         sources=GBIF, reviewed=False),

    dict(id="chlorociboria_aeruginascens", scientificName="Chlorociboria aeruginascens",
         commonName="green stain", prevalence="common", sought=False,
         hazard=hz(),
         note="The wood is the sign: fallen hardwood stained blue-green through and "
              "through, all year. The tiny turquoise cups themselves appear only in wet "
              "weather and are easy to miss.",
         characters=ch(
             fruitbody_type="cup_disc", substrate="wood",
             substrate_wood={"broadleaf": "always", "dead": "always",
                             "bare_wood": "always"},
             stipe_presence={"central": "usually", "absent_sitting": "sometimes"},
             cap_shape={"depressed": "usually", "flat": "sometimes"},
             cap_margin={"wavy": "usually"},
             cap_surface={"smooth": "always"},
             cap_colour={"green": "always", "blue": "usually"},
             flesh_colour={"green": "always"},
             flesh_consistency={"gelatinous": "usually", "soft": "sometimes"},
             latex="none", bruising={"no": "always"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"troop": "always"}),
         measurements=dict(cap_width_mm=[2, 10]),
         season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="candolleomyces_candolleanus", scientificName="Candolleomyces candolleanus",
         commonName="pale brittlestem", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "A large genus of small brown mushrooms, several of them unpleasant "
                   "and hard to tell apart.", NAMA[0]),
         note="Clustered on buried hardwood in gardens and paths. Pale honey drying to "
              "off-white, the cap edge hung with veil fragments when young, and the "
              "whole thing so brittle it snaps rather than bends.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"wood": "always",
                                                         "soil": "usually"},
             substrate_wood={"buried": "always", "broadleaf": "usually",
                             "dead": "always"},
             stipe_presence="central", stipe_flesh={"hollow": "always"},
             stipe_surface={"smooth": "usually", "fibrous": "sometimes"},
             ring={"absent": "usually", "zone": "sometimes"},
             stipe_base={"equal": "usually"},
             gill_attachment={"adnate": "usually", "adnexed": "sometimes"},
             gill_spacing={"crowded": "always"},
             hymenophore_colour={"white": "usually", "grey": "usually",
                                 "purple": "usually"},
             cap_shape={"bell": "usually", "convex": "usually", "flat": "sometimes"},
             cap_margin={"appendiculate": "always", "striate": "sometimes"},
             cap_surface={"smooth": "usually", "silky": "sometimes"},
             cap_colour={"cream": "usually", "tan": "usually", "white": "usually"},
             veil_remnants={"none": "usually"},
             flesh_colour={"white": "always"}, flesh_consistency={"brittle": "always"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"disturbed": "usually", "garden": "usually", "urban": "usually",
                      "broadleaf_wood": "sometimes"},
             spore_print={"dark": "always"}, spore_print_fine={"purple_brown": "always"},
             growth_habit={"caespitose": "always", "troop": "usually"}),
         measurements=dict(cap_width_mm=[20, 70], stipe_height_mm=[40, 90]),
         season=dict(months=[4, 5, 6, 7, 8, 9, 10]),
         lookalikes=[dict(taxon="galerina_marginata",
                          discriminators=["spore_print_fine", "ring", "flesh_consistency"],
                          note="Galerina prints rust-brown and has a ring; this prints "
                               "purple-brown and has only veil fragments at the cap edge. "
                               "Take the print. The mistake is fatal.")],
         sources=GBIF + NAMA, reviewed=False),
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

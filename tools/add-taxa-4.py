#!/usr/bin/env python3
"""
Fourth batch: close the gap between what is drawn and what exists.

Seventeen species had drawings but no taxon, so tapping their picture emptied the
candidate list. Adding them also fills most of the states still unreachable — a slimy
stem, a powdery one, a navel in the cap, a cortina rather than a ring — because each
was drawn from the species that shows it.

Two are here for a substrate nothing occupied: Panaeolus for dung, Pholiota
highlandensis for burnt ground. Both are ordinary finds, and a substrate a person can
plainly see should never be a dead end.

Amanita phalloides is included and labelled "not expected here". iNaturalist records
none of it in these mountains, so carrying it as a live candidate would be
misinformation; leaving it out entirely would be worse, because it is the one name
everybody has heard and the app should be able to say where it stands.

Same rules throughout: macro-morphology any field guide agrees on, no text reproduced,
reviewed:false because none of this has been checked by a mycologist.

    python3 tools/add-taxa-4.py
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
INAT = ["iNaturalist occurrence records for the southern Blue Ridge"]

TAXA = [
    dict(id="boletus_edulis", scientificName="Boletus edulis", commonName="porcini",
         hazard=hz(), note="Stout, pale net over the top of a fat stem, and white pores "
                           "ageing yellow then olive. Mild, never bitter.",
         characters=ch(
             fruitbody_type="bolete", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"bulbous": "usually",
                                                   "equal": "sometimes"},
             stipe_surface={"reticulate": "always"}, stipe_flesh={"solid": "always"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_surface={"smooth": "usually", "viscid": "sometimes"},
             cap_colour={"tan": "usually", "red_brown": "usually",
                         "yellow_brown": "sometimes"},
             hymenophore_colour={"white": "usually", "yellow": "usually",
                                 "olive": "sometimes"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
             odour={"mushroomy": "usually"}, taste={"mild": "always"},
             habitat={"conifer_wood": "usually", "mixed_wood": "usually"},
             spore_print="brown", growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[70, 250], stipe_height_mm=[60, 200]),
         season=dict(months=[7, 8, 9, 10]),
         lookalikes=[dict(taxon="tylopilus_felleus", discriminators=["taste",
                                                                    "hymenophore_colour"],
                          note="The bitter bolete has a dark coarse net and pores that "
                               "age pink, and one touch on the tongue settles it.")],
         sources=GBIF, reviewed=False),

    dict(id="lactarius_deliciosus", scientificName="Lactarius deliciosus",
         commonName="saffron milkcap", hazard=hz(),
         note="Orange latex that stains green where it dries, and a cap zoned in "
              "concentric bands.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"equal": "usually",
                                                   "tapering": "sometimes"},
             stipe_surface={"punctate": "usually", "smooth": "sometimes"},
             stipe_flesh={"hollow": "usually", "stuffed": "sometimes"},
             ring={"absent": "always"},
             gill_attachment={"decurrent": "usually", "adnate": "sometimes"},
             gill_spacing={"close": "usually", "crowded": "sometimes"},
             cap_shape={"depressed": "always", "funnel": "sometimes"},
             cap_margin={"inrolled": "usually"},
             cap_surface={"zoned": "always", "viscid": "sometimes"},
             cap_colour={"orange": "always"},
             hymenophore_colour={"orange": "always"},
             latex="present", latex_colour={"orange": "always"},
             latex_change={"yes": "usually"}, latex_change_colour={"green": "usually"},
             flesh_colour={"orange": "usually", "cream": "sometimes"},
             flesh_consistency={"brittle": "always"},
             bruising={"yes": "usually"}, bruising_where={"cap": "usually",
                                                          "hymenophore": "usually"},
             bruising_colour={"green": "always"}, bruising_speed={"slow": "usually"},
             odour={"fruity": "sometimes", "none": "usually"}, taste={"mild": "usually"},
             habitat={"conifer_wood": "always"}, associated_tree={"conifer": "always"},
             spore_print="pale", growth_habit={"few": "usually", "troop": "sometimes"}),
         measurements=dict(cap_width_mm=[40, 140], stipe_height_mm=[30, 80]),
         season=dict(months=[8, 9, 10, 11]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="suillus_americanus", scientificName="Suillus americanus",
         commonName="chicken-fat suillus",
         hazard=hz("gastrointestinal", "GI", "1-3 hours",
                   "The slime layer upsets some people; it is usually peeled off.",
                   NAMA[0]),
         note="Slimy cap and slimy stem, always under white pine.",
         characters=ch(
             fruitbody_type="bolete", substrate={"soil": "always", "litter": "usually"},
             stipe_presence="central", stipe_base={"equal": "usually",
                                                   "tapering": "sometimes"},
             stipe_surface={"slimy": "always", "punctate": "usually"},
             stipe_flesh={"solid": "usually"},
             ring={"zone": "sometimes", "absent": "usually"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_margin={"appendiculate": "sometimes", "incurved": "usually"},
             cap_surface={"viscid": "always"},
             cap_colour={"yellow": "always", "orange": "sometimes"},
             hymenophore_colour={"yellow": "always"},
             flesh_colour={"yellow": "always"}, flesh_consistency={"soft": "usually"},
             latex="none",
             bruising={"yes": "usually"}, bruising_where={"hymenophore": "usually",
                                                          "flesh": "usually"},
             bruising_colour={"red_brown": "usually"},
             odour={"none": "usually"},
             habitat={"conifer_wood": "always", "mixed_wood": "sometimes"},
             associated_tree={"conifer": "always"},
             spore_print="brown", growth_habit={"troop": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[30, 100], stipe_height_mm=[40, 90]),
         season=dict(months=[7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="xerocomellus_chrysenteron", scientificName="Xerocomellus chrysenteron",
         commonName="red cracking bolete", hazard=hz(),
         note="A dry velvety cap that cracks open to show red in the splits.",
         characters=ch(
             fruitbody_type="bolete", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"tapering": "usually"},
             stipe_surface={"fibrous": "usually"}, stipe_flesh={"solid": "usually"},
             ring={"absent": "always"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_surface={"velvety": "always", "cracked": "always", "dry": "always"},
             cap_colour={"olive": "usually", "red_brown": "usually",
                         "dark_brown": "sometimes"},
             hymenophore_colour={"yellow": "always", "olive": "sometimes"},
             flesh_colour={"yellow": "usually", "cream": "sometimes"},
             flesh_consistency={"soft": "usually"}, latex="none",
             bruising={"yes": "usually"}, bruising_where={"hymenophore": "usually",
                                                          "flesh": "sometimes"},
             bruising_colour={"blue": "usually"}, bruising_speed={"minute": "usually"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             spore_print="brown", growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[30, 90], stipe_height_mm=[40, 90]),
         season=dict(months=[7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="cortinarius_armillatus", scientificName="Cortinarius armillatus",
         commonName="red-banded webcap",
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Cortinarius is a genus with lethal members elsewhere; not one to "
                   "eat from on a guess.", NAMA[0]),
         note="A cortina, not a ring: a web of fibres that leaves only a rusty band "
              "where the spores caught on it. Red belts around the stem base.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"bulbous": "usually"},
             stipe_surface={"fibrous": "usually"}, stipe_flesh={"solid": "usually"},
             ring={"zone": "always"},
             gill_attachment={"adnexed": "usually", "adnate": "sometimes"},
             gill_spacing={"distant": "usually"},
             cap_shape={"convex": "usually", "umbonate": "usually"},
             cap_surface={"dry": "usually", "silky": "usually", "scaly": "sometimes"},
             cap_colour={"red_brown": "always", "tan": "sometimes"},
             hymenophore_colour={"tan": "sometimes", "red_brown": "usually"},
             flesh_colour={"cream": "usually"}, flesh_consistency={"fibrous": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"radish": "sometimes", "none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             associated_tree={"broadleaf": "usually"},
             spore_print="brown", growth_habit={"few": "usually", "troop": "sometimes"}),
         measurements=dict(cap_width_mm=[50, 130], stipe_height_mm=[70, 160]),
         season=dict(months=[8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="hygrocybe_conica", scientificName="Hygrocybe conica",
         commonName="blackening waxcap",
         hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="A sharp cone in bright colours that goes black wherever it is touched, "
              "and eventually all over.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always",
                                                         "grass": "usually"},
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"fibrous": "usually", "grooved": "sometimes"},
             stipe_flesh={"hollow": "usually"}, ring={"absent": "always"},
             gill_attachment={"free": "sometimes", "adnexed": "usually"},
             gill_spacing={"distant": "usually"},
             cap_shape={"conical": "always"},
             cap_margin={"straight": "usually", "wavy": "sometimes"},
             cap_surface={"viscid": "sometimes", "smooth": "usually"},
             cap_colour={"red": "usually", "orange": "usually", "yellow": "sometimes",
                         "black": "usually"},
             hymenophore_colour={"yellow": "usually", "cream": "sometimes"},
             flesh_colour={"yellow": "usually"}, flesh_consistency={"brittle": "usually"},
             latex="none",
             bruising={"yes": "always"}, bruising_where={"cap": "always",
                                                         "stipe": "always",
                                                         "hymenophore": "usually"},
             bruising_colour={"black": "always"}, bruising_speed={"minute": "usually"},
             odour={"none": "usually"},
             habitat={"unimproved_grass": "usually", "improved_grass": "usually",
                      "mixed_wood": "sometimes"},
             spore_print="pale", growth_habit={"few": "usually", "troop": "usually"}),
         measurements=dict(cap_width_mm=[20, 70], stipe_height_mm=[40, 100]),
         season=dict(months=[7, 8, 9, 10, 11]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="arrhenia_epichysium", scientificName="Arrhenia epichysium",
         commonName="brown goblet", hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="Small, grey-brown, with a sharp navel at the centre of the cap and "
              "gills running down the stem. On rotting wood and moss.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"wood": "always",
                                                         "moss": "sometimes"},
             substrate_wood={"dead": "always", "bare_wood": "usually"},
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"smooth": "usually"}, stipe_flesh={"hollow": "usually"},
             ring={"absent": "always"},
             gill_attachment={"decurrent": "always"},
             gill_spacing={"distant": "usually"},
             cap_shape={"umbilicate": "always", "depressed": "usually"},
             cap_margin={"translucent_striate": "usually", "wavy": "sometimes"},
             cap_surface={"smooth": "usually", "dry": "usually"},
             cap_colour={"grey": "usually", "dark_brown": "usually"},
             hymenophore_colour={"grey": "usually", "cream": "sometimes"},
             flesh_colour={"grey": "usually"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"mixed_wood": "usually", "conifer_wood": "sometimes"},
             spore_print="pale", growth_habit={"troop": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[10, 40], stipe_height_mm=[15, 50]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="cystoderma_amianthinum", scientificName="Cystoderma amianthinum",
         commonName="earthy powdercap",
         hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="Cap and stem both dusted with a granular powder that a finger wipes "
              "off, and the powder stops abruptly at the ring.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"moss": "usually",
                                                         "litter": "usually",
                                                         "soil": "sometimes"},
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"powdery": "always"}, stipe_flesh={"hollow": "usually"},
             ring={"zone": "usually", "skirt": "sometimes"},
             gill_attachment={"adnate": "usually", "adnexed": "sometimes"},
             gill_spacing={"crowded": "always"},
             cap_shape={"convex": "usually", "umbonate": "usually"},
             cap_margin={"straight": "usually"},
             cap_surface={"powdery": "always", "wrinkled": "sometimes", "dry": "always"},
             cap_colour={"yellow": "usually", "orange": "usually", "tan": "sometimes"},
             hymenophore_colour={"white": "usually", "cream": "usually"},
             flesh_colour={"cream": "usually"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"none": "usually", "unpleasant_other": "sometimes"},
             habitat={"conifer_wood": "usually", "mixed_wood": "usually"},
             spore_print="pale", growth_habit={"troop": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[15, 50], stipe_height_mm=[25, 70]),
         season=dict(months=[8, 9, 10, 11]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="gymnopus_dryophilus", scientificName="Gymnopus dryophilus",
         commonName="russet toughshank", hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="Very common in leaf litter under oaks; a tough thin stem and crowded "
              "gills meeting it square.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"litter": "always",
                                                         "soil": "sometimes"},
             stipe_presence="central", stipe_base={"equal": "usually",
                                                   "rhizomorphs": "sometimes"},
             stipe_surface={"smooth": "usually"}, stipe_flesh={"hollow": "usually"},
             ring={"absent": "always"},
             gill_attachment={"adnate": "always", "adnexed": "sometimes"},
             gill_spacing={"crowded": "always"},
             cap_shape={"convex": "usually", "flat": "usually"},
             cap_surface={"smooth": "usually", "dry": "usually"},
             cap_colour={"tan": "usually", "red_brown": "usually", "cream": "sometimes"},
             cap_colour_pattern={"paler_margin": "usually"},
             hymenophore_colour={"white": "usually", "cream": "usually"},
             flesh_colour={"cream": "usually"}, flesh_consistency={"fibrous": "usually"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"troop": "always", "few": "sometimes"}),
         measurements=dict(cap_width_mm=[20, 60], stipe_height_mm=[30, 80]),
         season=dict(months=[5, 6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="pluteus_petasatus", scientificName="Pluteus petasatus",
         commonName="scaly shield", hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="Free gills that turn pink as the spores ripen, on wood chips and buried "
              "wood. The pink print is the character.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="wood",
             substrate_wood={"broadleaf": "usually", "woodchip": "usually",
                             "dead": "always", "buried": "sometimes"},
             stipe_presence={"central": "usually", "off_centre": "sometimes"},
             stipe_base={"equal": "usually", "bulbous": "sometimes"},
             stipe_surface={"fibrous": "usually"}, stipe_flesh={"solid": "usually"},
             ring={"absent": "always"},
             gill_attachment={"free": "always"}, gill_spacing={"crowded": "usually"},
             cap_shape={"convex": "usually", "flat": "usually"},
             cap_surface={"scaly": "usually", "silky": "usually", "dry": "usually"},
             cap_colour={"white": "usually", "cream": "usually", "tan": "sometimes"},
             cap_colour_pattern={"darker_centre": "usually"},
             veil_remnants={"peeling_skin": "sometimes", "none": "usually"},
             hymenophore_colour={"white": "sometimes", "pink": "usually"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "disturbed": "usually",
                      "urban": "sometimes"},
             spore_print={"pink": "always"},
             growth_habit={"few": "usually", "caespitose": "sometimes"}),
         measurements=dict(cap_width_mm=[40, 150], stipe_height_mm=[40, 120]),
         season=dict(months=[5, 6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="peziza_badia", scientificName="Peziza badia", commonName="bay cup",
         hazard=hz("gastrointestinal", "GI", "1-3 hours",
                   "Raw Peziza upsets people.", NAMA[0]),
         note="A brown cup sitting straight on the soil, often on a bank or a path "
              "edge.",
         characters=ch(
             fruitbody_type="cup_disc", substrate={"soil": "always"},
             stipe_presence={"absent_sitting": "always"},
             cap_shape={"depressed": "usually", "funnel": "sometimes"},
             cap_margin={"wavy": "usually", "straight": "sometimes"},
             cap_surface={"smooth": "usually"},
             cap_colour={"dark_brown": "usually", "olive": "sometimes",
                         "red_brown": "usually"},
             flesh_colour={"tan": "usually"}, flesh_consistency={"brittle": "usually"},
             latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "disturbed": "usually",
                      "mixed_wood": "sometimes"},
             spore_print="pale", growth_habit={"few": "usually", "troop": "usually"}),
         measurements=dict(cap_width_mm=[20, 80]),
         season=dict(months=[5, 6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="ramaria_stricta", scientificName="Ramaria stricta",
         commonName="upright coral",
         hazard=hz("gastrointestinal", "GI", "1-3 hours", "", NAMA[0]),
         note="Straight parallel branches from a common base on buried wood, bruising "
              "brown where handled.",
         characters=ch(
             fruitbody_type="coral_club", substrate="wood",
             substrate_wood={"broadleaf": "usually", "dead": "always",
                             "buried": "usually"},
             stipe_presence={"absent_sitting": "usually"},
             cap_colour={"cream": "usually", "tan": "usually", "yellow": "sometimes"},
             flesh_colour={"cream": "usually"},
             flesh_consistency={"fibrous": "usually", "leathery": "sometimes"},
             latex="none",
             bruising={"yes": "usually"}, bruising_where={"flesh": "usually"},
             bruising_colour={"red_brown": "usually"},
             ring={"absent": "always"},
             odour={"aniseed": "sometimes", "none": "usually"},
             taste={"bitter": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             spore_print="pale", growth_habit={"few": "usually", "troop": "sometimes"}),
         measurements=dict(cap_width_mm=[30, 100]),
         season=dict(months=[7, 8, 9, 10, 11]),
         lookalikes=[dict(taxon="artomyces_pyxidatus",
                          discriminators=["fruitbody_type"],
                          note="Crown-tipped coral ends every branch in a ring of "
                               "points; these branches end blunt.")],
         sources=GBIF, reviewed=False),

    dict(id="trametes_hirsuta", scientificName="Trametes hirsuta",
         commonName="hairy bracket", hazard=hz(),
         note="Like a turkey tail but shaggy with distinct hairs, and greyer.",
         characters=ch(
             fruitbody_type="polypore", substrate="wood",
             substrate_wood={"broadleaf": "always", "dead": "always",
                             "bare_wood": "usually"},
             stipe_presence={"absent_attached": "always"},
             cap_shape={"flat": "usually"}, cap_margin={"wavy": "sometimes",
                                                        "straight": "usually"},
             cap_surface={"hairy": "always", "zoned": "usually", "dry": "always"},
             cap_colour={"grey": "usually", "cream": "usually", "tan": "sometimes"},
             hymenophore_colour={"cream": "usually", "grey": "sometimes"},
             flesh_colour={"cream": "usually"},
             flesh_consistency={"leathery": "always"},
             latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"tiered": "usually", "troop": "usually"}),
         measurements=dict(cap_width_mm=[20, 100]),
         season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
         lookalikes=[dict(taxon="trametes_versicolor",
                          discriminators=["cap_surface"],
                          note="Turkey tail is velvety and banded in colour; this is "
                               "coarsely hairy and mostly grey.")],
         sources=GBIF, reviewed=False),

    dict(id="hypoxylon_fragiforme", scientificName="Hypoxylon fragiforme",
         commonName="beech woodwart", hazard=hz(),
         note="Hard pink-brown cushions crowded on fallen beech, going black with age.",
         characters=ch(
             fruitbody_type="flask", substrate="wood",
             substrate_wood={"broadleaf": "always", "dead": "always",
                             "bark": "usually"},
             stipe_presence={"absent_attached": "always"},
             cap_shape={"convex": "usually"},
             cap_surface={"smooth": "usually", "warty": "sometimes"},
             cap_colour={"red_brown": "usually", "pink": "sometimes",
                         "black": "usually"},
             flesh_colour={"black": "always"}, flesh_consistency={"woody": "always"},
             latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print={"dark": "usually"},
             growth_habit={"troop": "always", "fused": "sometimes"}),
         measurements=dict(cap_width_mm=[2, 10]),
         season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="panaeolus_papilionaceus", scientificName="Panaeolus papilionaceus",
         commonName="petticoat mottlegill",
         hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="On dung and dunged grass. A bell cap with white veil fragments hanging "
              "from the edge, and gills mottled black as the spores ripen unevenly.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"dung": "always",
                                                         "grass": "usually"},
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"powdery": "sometimes", "smooth": "usually"},
             stipe_flesh={"hollow": "always"}, ring={"absent": "always"},
             gill_attachment={"adnate": "usually", "adnexed": "sometimes"},
             gill_spacing={"close": "usually"},
             cap_shape={"bell": "always", "conical": "sometimes"},
             cap_margin={"appendiculate": "always"},
             cap_surface={"smooth": "usually", "dry": "usually", "cracked": "sometimes"},
             cap_colour={"grey": "usually", "tan": "sometimes"},
             cap_colour_pattern={"paler_margin": "usually"},
             veil_remnants={"none": "usually"},
             hymenophore_colour={"grey": "sometimes", "black": "usually"},
             flesh_colour={"grey": "usually"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"improved_grass": "usually", "wood_pasture": "usually"},
             spore_print={"dark": "always"},
             growth_habit={"troop": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[15, 50], stipe_height_mm=[60, 150]),
         season=dict(months=[4, 5, 6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="pholiota_highlandensis", scientificName="Pholiota highlandensis",
         commonName="bonfire scalycap", hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="On old fire sites, and almost nowhere else. Burnt ground is the "
              "character.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"burnt": "always",
                                                         "soil": "usually"},
             stipe_presence="central", stipe_base={"equal": "usually"},
             stipe_surface={"scaly": "usually", "fibrous": "usually"},
             stipe_flesh={"hollow": "sometimes", "solid": "usually"},
             ring={"zone": "usually", "absent": "sometimes"},
             gill_attachment={"adnate": "usually"}, gill_spacing={"crowded": "usually"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_surface={"viscid": "usually", "smooth": "usually"},
             cap_colour={"yellow_brown": "usually", "tan": "usually"},
             cap_colour_pattern={"darker_centre": "usually"},
             hymenophore_colour={"cream": "sometimes", "red_brown": "usually"},
             flesh_colour={"yellow": "usually"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "sometimes", "disturbed": "usually",
                      "mixed_wood": "sometimes"},
             spore_print="brown",
             growth_habit={"troop": "usually", "caespitose": "sometimes"}),
         measurements=dict(cap_width_mm=[15, 50], stipe_height_mm=[20, 60]),
         season=dict(months=[5, 6, 7, 8, 9, 10, 11]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="inocybe_rimosa", scientificName="Inocybe rimosa",
         commonName="split fibrecap",
         hazard=hz("muscarine", "SEVERE", "15 minutes to 2 hours",
                   "Muscarine: sweating, tears, salivation, slowed heart. Treatable "
                   "with atropine, but it needs a hospital.", NAMA[0]),
         note="A straw-coloured cone of radiating fibres that splits at the edge, and "
              "a smell people describe as sperm-like or mealy. Inocybe is the "
              "muscarine genus and almost nothing in it is safely told apart in the "
              "field.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always",
                                                         "litter": "usually"},
             stipe_presence="central", stipe_base={"equal": "usually",
                                                   "bulbous": "sometimes"},
             stipe_surface={"fibrous": "always"}, stipe_flesh={"solid": "usually"},
             ring={"absent": "always"},
             gill_attachment={"adnexed": "usually", "free": "sometimes"},
             gill_spacing={"crowded": "usually"},
             gill_edge={"different_colour": "sometimes", "even": "usually"},
             cap_shape={"conical": "always", "umbonate": "usually"},
             cap_margin={"straight": "usually", "wavy": "sometimes"},
             cap_surface={"silky": "always", "cracked": "usually", "dry": "always"},
             cap_colour={"tan": "usually", "yellow_brown": "usually",
                         "cream": "sometimes"},
             hymenophore_colour={"cream": "sometimes", "olive": "sometimes",
                                 "tan": "usually"},
             flesh_colour={"white": "usually"}, flesh_consistency={"fibrous": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"unpleasant_other": "always", "farinaceous": "sometimes"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually",
                      "disturbed": "sometimes"},
             spore_print="brown",
             growth_habit={"troop": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[20, 70], stipe_height_mm=[30, 90]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF + NAMA, reviewed=False),

    dict(id="cortinarius_alboviolaceus", scientificName="Cortinarius alboviolaceus",
         commonName="pearly webcap",
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Cortinarius; not eaten on a guess.", NAMA[0]),
         note="Silvery lilac all over when young, with a swollen base and a cortina "
              "that leaves a rusty band.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"bulbous": "always"},
             stipe_surface={"fibrous": "usually"},
             stipe_flesh={"solid": "usually"},
             ring={"zone": "usually"},
             gill_attachment={"adnexed": "usually", "adnate": "sometimes"},
             gill_spacing={"close": "usually"},
             cap_shape={"convex": "usually", "umbonate": "sometimes"},
             cap_surface={"silky": "always", "dry": "always"},
             cap_colour={"purple": "usually", "white": "usually", "grey": "sometimes"},
             hymenophore_colour={"purple": "sometimes", "red_brown": "usually"},
             flesh_colour={"purple": "sometimes", "white": "usually"},
             flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="brown", growth_habit={"few": "usually", "troop": "sometimes"}),
         measurements=dict(cap_width_mm=[30, 90], stipe_height_mm=[50, 110]),
         season=dict(months=[8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="amanita_phalloides", scientificName="Amanita phalloides",
         commonName="death cap",
         hazard=dict(toxinClass="amatoxin", severity="LETHAL", onset="6-24 hours",
                     note="The most lethal mushroom in the world, and the one every "
                          "person has heard of. Not expected in these mountains.",
                     source=NAMA[0]),
         note="**Not expected here.** iNaturalist records none in the southern Blue "
              "Ridge — it follows planted trees in towns and on the coasts. Carried so "
              "the app can say where it stands rather than stay silent about the one "
              "name everybody knows. The local destroying angels are the real risk, "
              "and they are white rather than olive.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"sac_volva": "always"},
             stipe_surface={"smooth": "usually", "fibrous": "sometimes"},
             ring={"skirt": "always"},
             gill_attachment={"free": "always"}, gill_spacing={"crowded": "usually"},
             cap_shape={"egg": "sometimes", "convex": "usually", "flat": "usually"},
             cap_margin={"straight": "usually"},
             cap_surface={"smooth": "usually", "viscid": "sometimes",
                          "silky": "usually"},
             cap_colour={"olive": "always", "green": "usually", "yellow": "sometimes"},
             cap_colour_pattern={"darker_centre": "usually"},
             veil_remnants={"none": "usually"},
             hymenophore_colour={"white": "always"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
             latex="none", bruising={"no": "usually"},
             odour={"sweet": "sometimes", "none": "usually"},
             habitat={"broadleaf_wood": "usually", "urban": "usually",
                      "garden": "sometimes"},
             associated_tree={"broadleaf": "usually"},
             spore_print="pale", growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[70, 150]),
         season=dict(months=[8, 9, 10, 11]),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=["cap_colour"],
                          note="The destroying angel is pure white and is the one "
                               "actually found here. This is olive to green.")],
         sources=INAT + NAMA, reviewed=False),
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

#!/usr/bin/env python3
"""
Third batch: the species already drawn, but not yet in the pack.

Every character drawing was made from a named species chosen because it shows that
character plainly — Tricholoma for a notched gill, Amanita muscaria for a ringed bulb,
Hymenopellis for a rooting base. Thirty-one of those fifty-one species were not in the
pack, which produced a quiet version of the same fault batch two fixed: a reader taps
the picture of a ringed volva, which is a real thing they are holding, and the candidate
list empties, because nothing in the pack has that state.

So the exemplar list is the taxon list. These were picked for showing a character
clearly, which is the same reason they are worth carrying.

Accuracy rules are unchanged: macro-morphology any field guide agrees on, encoded into
this schema's own structure, no text reproduced, reviewed:false throughout because none
of it has been checked by a mycologist.

    python3 tools/add-taxa-3.py
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
    dict(
        id="amanita_muscaria_guessowii",
        scientificName="Amanita muscaria",
        commonName="fly agaric",
        hazard=hz("ibotenic_acid_muscimol", "INTOXICATION", "30 minutes to 3 hours",
                  "Delirium and sedation rather than organ damage. Rarely fatal, "
                  "reliably unpleasant.", NAMA[0]),
        note="The bulb wears rings of volval scales rather than sitting in a bag — "
             "that is what separates it from the deadly white Amanitas, and it is at "
             "the base, so the base has to come up.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
            stipe_presence="central", stipe_base={"volva_rings": "always"},
            stipe_surface={"smooth": "usually", "fibrous": "sometimes"},
            stipe_flesh={"stuffed": "usually", "hollow": "sometimes"},
            ring={"skirt": "always"},
            gill_attachment={"free": "always"}, gill_spacing={"crowded": "usually"},
            gill_extras={"lamellulae": "usually"},
            cap_shape={"egg": "sometimes", "convex": "usually", "flat": "usually"},
            cap_margin={"striate": "usually", "incurved": "sometimes"},
            cap_surface={"warty": "always", "viscid": "sometimes"},
            cap_colour={"yellow": "usually", "orange": "usually", "red": "sometimes"},
            cap_colour_pattern={"darker_centre": "usually"},
            veil_remnants={"warts": "always"},
            hymenophore_colour={"white": "always", "cream": "sometimes"},
            flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
            latex="none", bruising={"no": "usually"},
            odour={"none": "usually"},
            habitat={"conifer_wood": "usually", "mixed_wood": "usually",
                     "broadleaf_wood": "sometimes"},
            associated_tree={"conifer": "usually", "broadleaf": "sometimes"},
            spore_print="pale",
            growth_habit={"few": "usually", "troop": "usually", "ring": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[50, 200], stipe_height_mm=[50, 180]),
        season=dict(months=[7, 8, 9, 10, 11]),
        lookalikes=[dict(taxon="amanita_bisporigera",
                         discriminators=["stipe_base", "cap_colour", "veil_remnants"],
                         note="A destroying angel is white throughout and stands in a "
                              "membranous sac. This is coloured and its bulb wears "
                              "rings of scales.")],
        sources=GBIF + NAMA, reviewed=False,
    ),
    dict(
        id="amanita_jacksonii",
        scientificName="Amanita jacksonii",
        commonName="American Caesar's mushroom",
        hazard=hz(),
        note="A red-orange cap with a deeply lined margin, a movable ring, and a white "
             "sac at the base. All three together, or leave it: the base and the ring "
             "are what separate it from Amanita parcivolvata, which has neither.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
            stipe_presence="central", stipe_base={"sac_volva": "always"},
            stipe_surface={"fibrous": "usually", "smooth": "sometimes"},
            ring={"movable": "usually", "skirt": "usually"},
            gill_attachment={"free": "always"}, gill_spacing={"crowded": "usually"},
            cap_shape={"egg": "sometimes", "convex": "usually", "flat": "usually"},
            cap_margin={"striate": "always"},
            cap_surface={"smooth": "usually", "viscid": "sometimes"},
            cap_colour={"red": "usually", "orange": "usually", "yellow": "sometimes"},
            cap_colour_pattern={"darker_centre": "usually"},
            veil_remnants={"none": "usually", "single_patch": "sometimes"},
            hymenophore_colour={"yellow": "usually", "orange": "sometimes"},
            flesh_colour={"white": "usually", "yellow": "sometimes"},
            flesh_consistency={"soft": "always"}, latex="none",
            bruising={"no": "usually"}, odour={"none": "usually"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
            associated_tree={"broadleaf": "usually"},
            spore_print="pale", growth_habit={"single": "usually", "few": "usually"},
        ),
        measurements=dict(cap_width_mm=[60, 150], stipe_height_mm=[80, 180]),
        season=dict(months=[6, 7, 8, 9]),
        lookalikes=[dict(taxon="amanita_parcivolvata",
                         discriminators=["ring", "stipe_base"],
                         note="Amanita parcivolvata has no ring at all and no sac — "
                              "only loose volval material scattered at the base.")],
        sources=GBIF, reviewed=False,
    ),
    dict(
        id="amanita_citrina",
        scientificName="Amanita citrina",
        commonName="false death cap",
        hazard=hz("unknown", "UNKNOWN", "—",
                  "Not regarded as deadly, and not eaten: it is too easily confused "
                  "with things that are.", NAMA[0]),
        note="An abruptly bulbous base with a distinct rim and gutter, and a smell of "
             "raw potato.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
            stipe_presence="central", stipe_base={"marginate": "always"},
            ring={"skirt": "always"}, gill_attachment={"free": "always"},
            gill_spacing={"crowded": "usually"},
            cap_shape={"convex": "usually", "flat": "usually"},
            cap_surface={"smooth": "usually", "viscid": "sometimes"},
            cap_colour={"cream": "usually", "yellow": "usually", "white": "sometimes"},
            veil_remnants={"single_patch": "usually", "warts": "sometimes"},
            hymenophore_colour={"white": "always"},
            flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
            latex="none", bruising={"no": "usually"},
            odour={"potato": "always"},
            habitat={"broadleaf_wood": "usually", "conifer_wood": "sometimes"},
            spore_print="pale", growth_habit={"few": "usually", "single": "usually"},
        ),
        measurements=dict(cap_width_mm=[40, 100], stipe_height_mm=[50, 120]),
        season=dict(months=[7, 8, 9, 10]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="tricholoma_sejunctum",
        scientificName="Tricholoma sejunctum",
        commonName="deceiving knight",
        hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
        note="Gills notched sharply where they meet the stem — run a thumbnail along "
             "one and the cut is obvious.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
            stipe_presence="central", stipe_base={"equal": "usually"},
            stipe_surface={"fibrous": "usually", "smooth": "sometimes"},
            stipe_flesh={"solid": "usually"}, ring={"absent": "always"},
            gill_attachment={"notched": "always"}, gill_spacing={"close": "usually"},
            gill_edge={"even": "usually"},
            cap_shape={"convex": "usually", "umbonate": "usually"},
            cap_surface={"silky": "usually", "viscid": "sometimes",
                         "smooth": "sometimes"},
            cap_colour={"olive": "usually", "yellow": "usually", "grey": "sometimes"},
            cap_colour_pattern={"darker_centre": "usually"},
            hymenophore_colour={"white": "usually"},
            flesh_colour={"white": "usually"}, flesh_consistency={"soft": "usually"},
            latex="none", bruising={"no": "usually"},
            odour={"farinaceous": "usually"}, taste={"farinaceous": "usually"},
            habitat={"mixed_wood": "usually", "broadleaf_wood": "usually"},
            spore_print="pale", growth_habit={"few": "usually", "troop": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[40, 100], stipe_height_mm=[40, 100]),
        season=dict(months=[8, 9, 10]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="hymenopellis_radicata",
        scientificName="Hymenopellis radicata",
        commonName="rooting shank",
        hazard=hz(),
        note="The stem carries on underground as a long tapering root, often as far "
             "again as the part above. Pull gently or it snaps and the character is "
             "lost.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate={"soil": "always", "wood": "sometimes"},
            substrate_wood={"broadleaf": "usually", "buried": "usually",
                            "dead": "usually"},
            stipe_presence="central", stipe_base={"rooting": "always"},
            stipe_surface={"fibrous": "usually", "smooth": "sometimes"},
            stipe_flesh={"solid": "usually"}, ring={"absent": "always"},
            gill_attachment={"adnexed": "usually", "adnate": "sometimes"},
            gill_spacing={"distant": "usually"},
            cap_shape={"convex": "usually", "flat": "usually", "umbonate": "usually"},
            cap_surface={"viscid": "usually", "wrinkled": "usually"},
            cap_colour={"tan": "usually", "yellow_brown": "usually",
                        "grey": "sometimes"},
            hymenophore_colour={"white": "always"},
            flesh_colour={"white": "always"}, flesh_consistency={"soft": "usually"},
            latex="none", bruising={"no": "usually"},
            odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale", growth_habit={"single": "usually", "few": "usually"},
        ),
        measurements=dict(cap_width_mm=[30, 100], stipe_height_mm=[60, 200]),
        season=dict(months=[6, 7, 8, 9, 10]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="mycena_galericulata",
        scientificName="Mycena galericulata",
        commonName="common bonnet",
        hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
        note="Small, bell-shaped, and the flesh is thin enough that the gills show "
             "through the cap as lines.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate="wood",
            substrate_wood={"broadleaf": "usually", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence="central", stipe_base={"rhizomorphs": "sometimes",
                                                  "equal": "usually"},
            stipe_surface={"smooth": "usually"}, stipe_flesh={"hollow": "usually"},
            ring={"absent": "always"},
            gill_attachment={"adnexed": "usually", "adnate": "sometimes"},
            gill_spacing={"distant": "usually", "close": "sometimes"},
            gill_extras={"veined": "usually"},
            gill_edge={"different_colour": "sometimes", "even": "usually"},
            cap_shape={"bell": "always", "conical": "sometimes",
                       "umbonate": "usually"},
            cap_margin={"translucent_striate": "always"},
            cap_surface={"smooth": "usually", "dry": "usually"},
            cap_colour={"grey": "usually", "tan": "sometimes", "cream": "sometimes"},
            hymenophore_colour={"white": "usually", "grey": "sometimes",
                                "pink": "sometimes"},
            flesh_colour={"white": "usually", "grey": "sometimes"},
            flesh_consistency={"soft": "usually"},
            latex={"watery": "sometimes", "none": "usually"},
            bruising={"no": "usually"}, odour={"farinaceous": "sometimes",
                                               "none": "usually"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "sometimes"},
            spore_print="pale",
            growth_habit={"caespitose": "usually", "troop": "usually"},
        ),
        measurements=dict(cap_width_mm=[10, 50], stipe_height_mm=[30, 100]),
        season=dict(months=[5, 6, 7, 8, 9, 10, 11]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="marasmius_oreades",
        scientificName="Marasmius oreades",
        commonName="fairy ring champignon",
        hazard=hz(),
        note="In arcs and rings on mown grass, with a tough wiry stem that will not "
             "snap and gills that stay well apart.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate={"grass": "always", "soil": "usually"},
            stipe_presence="central", stipe_base={"equal": "usually"},
            stipe_surface={"smooth": "usually", "hairy": "sometimes"},
            stipe_flesh={"solid": "usually"}, ring={"absent": "always"},
            gill_attachment={"free": "sometimes", "adnexed": "usually"},
            gill_spacing={"distant": "always"},
            cap_shape={"convex": "usually", "flat": "usually", "umbonate": "usually"},
            cap_margin={"striate": "sometimes", "straight": "usually"},
            cap_surface={"smooth": "usually", "dry": "usually"},
            cap_colour={"tan": "usually", "cream": "usually",
                        "yellow_brown": "sometimes"},
            hymenophore_colour={"cream": "usually", "white": "sometimes"},
            flesh_colour={"white": "usually", "cream": "sometimes"},
            flesh_consistency={"fibrous": "usually"},
            latex="none", bruising={"no": "usually"},
            odour={"almond": "sometimes", "sweet": "sometimes", "none": "usually"},
            habitat={"improved_grass": "always", "unimproved_grass": "usually",
                     "garden": "usually", "urban": "sometimes"},
            spore_print="pale",
            growth_habit={"ring": "always", "troop": "usually"},
        ),
        measurements=dict(cap_width_mm=[15, 50], stipe_height_mm=[30, 80]),
        season=dict(months=[5, 6, 7, 8, 9, 10]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="pholiota_squarrosa",
        scientificName="Pholiota squarrosa",
        commonName="shaggy scalycap",
        hazard=hz("gastrointestinal", "GI", "1-3 hours",
                  "Upsets many people, especially with alcohol.", NAMA[0]),
        note="Dry, and shaggy with recurved scales on both cap and stem, in big "
             "clusters at the foot of hardwoods.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate="wood",
            substrate_wood={"broadleaf": "usually", "living": "sometimes",
                            "dead": "usually"},
            stipe_presence="central", stipe_base={"equal": "usually",
                                                  "tapering": "sometimes"},
            stipe_surface={"scaly": "always"}, stipe_flesh={"solid": "usually"},
            ring={"zone": "usually", "skirt": "sometimes"},
            gill_attachment={"adnate": "usually", "notched": "sometimes"},
            gill_spacing={"crowded": "usually"},
            cap_shape={"convex": "usually", "flat": "sometimes"},
            cap_surface={"scaly": "always", "dry": "always"},
            cap_colour={"yellow_brown": "usually", "tan": "usually",
                        "orange": "sometimes"},
            veil_remnants={"fibrous_scales": "always"},
            hymenophore_colour={"cream": "sometimes", "yellow": "usually",
                                "red_brown": "usually"},
            flesh_colour={"yellow": "usually", "cream": "sometimes"},
            flesh_consistency={"fibrous": "usually"},
            latex="none", bruising={"no": "usually"},
            odour={"radish": "sometimes", "garlic": "sometimes"},
            taste={"bitter": "sometimes"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="brown",
            growth_habit={"caespitose": "always", "fused": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[30, 120], stipe_height_mm=[50, 150]),
        season=dict(months=[8, 9, 10, 11]),
        lookalikes=[], sources=GBIF + NAMA, reviewed=False,
    ),
    dict(
        id="panellus_stipticus",
        scientificName="Panellus stipticus",
        commonName="bitter oyster",
        hazard=hz("gastrointestinal", "GI", "1-3 hours",
                  "Bitter and purgative.", NAMA[0]),
        note="Small, tough, kidney-shaped brackets on hardwood, with a stubby stem at "
             "one side. The gills glow faintly green in the dark in this part of the "
             "world, which is not a field character but is worth knowing.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always",
                            "bare_wood": "usually"},
            stipe_presence={"lateral": "always"},
            stipe_surface={"hairy": "usually", "smooth": "sometimes"},
            ring={"absent": "always"},
            gill_attachment={"decurrent": "usually", "adnate": "sometimes"},
            gill_spacing={"close": "usually", "crowded": "sometimes"},
            gill_extras={"veined": "sometimes"},
            cap_shape={"flat": "usually", "depressed": "sometimes"},
            cap_margin={"inrolled": "usually", "wavy": "sometimes"},
            cap_surface={"dry": "always", "hairy": "usually", "scaly": "sometimes"},
            cap_colour={"tan": "usually", "cream": "usually",
                        "yellow_brown": "sometimes"},
            hymenophore_colour={"tan": "usually", "cream": "sometimes"},
            flesh_colour={"cream": "usually"}, flesh_consistency={"leathery": "always"},
            latex="none", bruising={"no": "usually"},
            odour={"none": "usually"}, taste={"bitter": "always"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale",
            growth_habit={"tiered": "usually", "troop": "always"},
        ),
        measurements=dict(cap_width_mm=[5, 30]),
        season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="lentinellus_ursinus",
        scientificName="Lentinellus ursinus",
        commonName="bear lentinellus",
        hazard=hz("unknown", "UNKNOWN", "—",
                  "Too acrid to eat rather than known to be poisonous.", "—"),
        note="Gill edges cut into ragged teeth, like a saw that has been used badly. "
             "Very acrid on the tongue-tip.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate="wood",
            substrate_wood={"broadleaf": "always", "dead": "always"},
            stipe_presence={"absent_attached": "always"},
            ring={"absent": "always"},
            gill_attachment={"no_stem": "always"},
            gill_spacing={"close": "usually", "crowded": "sometimes"},
            gill_edge={"serrate": "always"},
            cap_shape={"flat": "usually"}, cap_margin={"wavy": "usually"},
            cap_surface={"hairy": "always", "dry": "always"},
            cap_colour={"tan": "usually", "red_brown": "usually",
                        "dark_brown": "sometimes"},
            hymenophore_colour={"cream": "usually", "tan": "sometimes"},
            flesh_colour={"cream": "usually"},
            flesh_consistency={"leathery": "usually", "fibrous": "sometimes"},
            latex="none", bruising={"no": "usually"},
            odour={"none": "usually"}, taste={"acrid": "always"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="pale", growth_habit={"tiered": "usually", "troop": "usually"},
        ),
        measurements=dict(cap_width_mm=[20, 100]),
        season=dict(months=[6, 7, 8, 9, 10, 11]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="entoloma_abortivum",
        scientificName="Entoloma abortivum",
        commonName="aborted entoloma",
        hazard=hz("unknown", "UNKNOWN", "—",
                  "Eaten by some; reports of upset are common enough to note.", "—"),
        note="Often found as pale lumpy masses beside the ordinary form — those are "
             "Armillaria that this fungus has overrun, which is why they turn up "
             "together.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate={"soil": "usually", "wood": "sometimes",
                       "another_fungus": "sometimes"},
            substrate_wood={"broadleaf": "usually", "dead": "usually",
                            "buried": "sometimes"},
            stipe_presence="central", stipe_base={"equal": "usually"},
            stipe_surface={"fibrous": "usually"}, ring={"absent": "always"},
            gill_attachment={"decurrent": "usually", "adnate": "sometimes"},
            gill_spacing={"close": "usually"},
            cap_shape={"convex": "usually", "flat": "usually",
                       "depressed": "sometimes"},
            cap_margin={"inrolled": "usually", "wavy": "sometimes"},
            cap_surface={"dry": "usually", "silky": "usually"},
            cap_colour={"grey": "usually", "tan": "sometimes"},
            hymenophore_colour={"grey": "sometimes", "pink": "usually"},
            flesh_colour={"white": "usually"}, flesh_consistency={"soft": "usually"},
            latex="none", bruising={"no": "usually"},
            odour={"farinaceous": "usually"}, taste={"farinaceous": "usually"},
            habitat={"broadleaf_wood": "always"},
            spore_print={"pink": "always"},
            growth_habit={"troop": "usually", "caespitose": "usually"},
        ),
        measurements=dict(cap_width_mm=[40, 100], stipe_height_mm=[35, 100]),
        season=dict(months=[8, 9, 10, 11]),
        lookalikes=[dict(taxon="armillaria_mellea",
                         discriminators=["spore_print", "hymenophore_colour", "ring"],
                         note="Armillaria drops a white print and has a ring; this "
                              "drops pink and has none.")],
        sources=GBIF, reviewed=False,
    ),
    dict(
        id="paxillus_involutus",
        scientificName="Paxillus involutus",
        commonName="brown roll-rim",
        hazard=hz("immune_haemolytic", "SEVERE", "hours, after repeated meals",
                  "Sensitisation builds over repeated meals and then one meal "
                  "destroys red blood cells. There is no safe preparation and no "
                  "way to know who is sensitised.", NAMA[0]),
        note="The margin is rolled hard under, and everything bruises brown — cap, "
             "gills, stem. Gills can be pushed off the cap with a thumb, which no "
             "true gilled mushroom allows.",
        characters=ch(
            fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
            stipe_presence="central", stipe_base={"equal": "usually",
                                                  "tapering": "sometimes"},
            stipe_surface={"smooth": "usually"}, stipe_flesh={"solid": "usually"},
            ring={"absent": "always"},
            gill_attachment={"decurrent": "always"},
            gill_spacing={"crowded": "usually"}, gill_extras={"forked": "usually"},
            cap_shape={"convex": "sometimes", "flat": "usually",
                       "depressed": "usually"},
            cap_margin={"inrolled": "always"},
            cap_surface={"velvety": "usually", "viscid": "sometimes",
                         "dry": "sometimes"},
            cap_colour={"yellow_brown": "usually", "olive": "sometimes",
                        "red_brown": "usually"},
            hymenophore_colour={"cream": "usually", "yellow_brown": "usually"},
            flesh_colour={"cream": "usually", "yellow": "sometimes"},
            flesh_consistency={"soft": "usually"},
            latex="none",
            bruising={"yes": "always"},
            bruising_where={"cap": "usually", "hymenophore": "always",
                            "stipe": "usually", "flesh": "usually"},
            bruising_colour={"red_brown": "always", "dark_brown": "usually"},
            bruising_speed={"minute": "usually", "instant": "sometimes"},
            odour={"none": "usually", "unpleasant_other": "sometimes"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "usually",
                     "disturbed": "sometimes"},
            spore_print="brown",
            growth_habit={"few": "usually", "troop": "usually"},
        ),
        measurements=dict(cap_width_mm=[40, 150], stipe_height_mm=[30, 80]),
        season=dict(months=[7, 8, 9, 10]),
        lookalikes=[], sources=GBIF + NAMA, reviewed=False,
    ),
    dict(
        id="ganoderma_applanatum",
        scientificName="Ganoderma applanatum",
        commonName="artist's conk",
        hazard=hz(),
        note="A hard perennial shelf whose white pore surface bruises brown at a "
             "fingernail and keeps the mark — which is where the name comes from.",
        characters=ch(
            fruitbody_type="polypore", substrate="wood",
            substrate_wood={"broadleaf": "usually", "dead": "usually",
                            "living": "sometimes"},
            stipe_presence={"absent_attached": "always"},
            cap_shape={"flat": "usually"}, cap_margin={"straight": "usually"},
            cap_surface={"zoned": "usually", "wrinkled": "usually", "dry": "always"},
            cap_colour={"grey": "usually", "dark_brown": "usually",
                        "tan": "sometimes"},
            hymenophore_colour={"white": "always", "cream": "sometimes"},
            flesh_colour={"red_brown": "usually", "dark_brown": "sometimes"},
            flesh_consistency={"woody": "always"},
            latex="none",
            bruising={"yes": "always"},
            bruising_where={"hymenophore": "always"},
            bruising_colour={"dark_brown": "always"},
            bruising_speed={"instant": "usually"},
            ring={"absent": "always"}, odour={"none": "usually"},
            habitat={"broadleaf_wood": "always"},
            associated_tree={"broadleaf": "always"},
            spore_print="brown",
            growth_habit={"single": "usually", "tiered": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[100, 600]),
        season=dict(months=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="clitocybe_gibba",
        scientificName="Clitocybe gibba",
        commonName="common funnel",
        hazard=hz("unknown", "UNKNOWN", "—",
                  "The genus contains species with muscarine; not a genus to eat from "
                  "on a guess.", NAMA[0]),
        note="A shallow funnel with gills running well down the stem, and a margin "
             "that turns up with age.",
        characters=ch(
            fruitbody_type="gilled_stemmed",
            substrate={"litter": "always", "soil": "usually"},
            stipe_presence="central", stipe_base={"equal": "usually"},
            stipe_surface={"smooth": "usually", "fibrous": "sometimes"},
            stipe_flesh={"stuffed": "usually"}, ring={"absent": "always"},
            gill_attachment={"decurrent": "always"},
            gill_spacing={"crowded": "usually"},
            cap_shape={"funnel": "always", "depressed": "usually"},
            cap_margin={"uplifted": "usually", "wavy": "sometimes"},
            cap_surface={"dry": "usually", "smooth": "usually"},
            cap_colour={"tan": "usually", "pink": "sometimes", "cream": "sometimes"},
            hymenophore_colour={"white": "usually", "cream": "usually"},
            flesh_colour={"white": "usually"}, flesh_consistency={"soft": "usually"},
            latex="none", bruising={"no": "usually"},
            odour={"almond": "sometimes", "none": "usually"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
            spore_print="pale", growth_habit={"troop": "usually", "ring": "sometimes"},
        ),
        measurements=dict(cap_width_mm=[30, 90], stipe_height_mm=[30, 90]),
        season=dict(months=[6, 7, 8, 9, 10]),
        lookalikes=[], sources=GBIF, reviewed=False,
    ),
    dict(
        id="hypomyces_lactifluorum",
        scientificName="Hypomyces lactifluorum",
        commonName="lobster mushroom",
        hazard=hz("unknown", "UNKNOWN", "—",
                  "The mould is harmless; what it has overrun is not always known, "
                  "which is the whole of the risk.", NAMA[0]),
        note="Not a mushroom but a mould covering one, so completely that the gills "
             "are gone and the surface is a hard orange crust. What is underneath "
             "cannot be told, and that is the reason for caution.",
        characters=ch(
            fruitbody_type={"other": "usually", "resupinate": "sometimes"},
            substrate={"another_fungus": "always"},
            stipe_presence={"central": "usually", "off_centre": "sometimes"},
            cap_shape={"depressed": "usually", "funnel": "sometimes"},
            cap_surface={"powdery": "usually", "dry": "always",
                         "wrinkled": "sometimes"},
            cap_colour={"orange": "always", "red": "usually"},
            flesh_colour={"white": "always"},
            flesh_consistency={"brittle": "usually"},
            latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
            odour={"none": "usually", "fruity": "sometimes"},
            habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
            spore_print="pale", growth_habit={"few": "usually", "single": "usually"},
        ),
        measurements=dict(cap_width_mm=[50, 150]),
        season=dict(months=[7, 8, 9, 10]),
        lookalikes=[], sources=GBIF + NAMA, reviewed=False,
    ),
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

#!/usr/bin/env python3
"""
Fifth batch: the ones a person actually trips over here.

Earlier batches were chosen by what was structurally broken — dead characters, empty
top-level answers, species that had drawings but no taxon. Those are fixed, so this one
is chosen the other way round: common Southern Appalachian fungi that a person walking
out of the door in August will meet, weighted towards the ones that teach a character
by contrast.

Three pairs here exist to be told apart from something already in the pack:

- **Amanita vaginata** has a sac volva and *no ring*; jacksonii has both. Between them
  and bisporigera, the reader sees that "sac at the base" is not by itself an answer.
- **Armillaria tabescens** is the honey mushroom without a ring, beside *A. mellea*
  which has one — and both grow in the same clusters on the same wood as the deadly
  Galerina.
- **Agaricus xanthodermus** stains yellow and smells of phenol, next to
  *A. campestris* which does neither. It is the reason "a field mushroom" is not a safe
  thing to conclude.

Same rules: macro-morphology any guide agrees on, no text reproduced, reviewed:false.

    python3 tools/add-taxa-5.py
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
    dict(id="amanita_rubescens", scientificName="Amanita rubescens",
         commonName="blusher",
         hazard=hz("haemolysin", "SEVERE", "—",
                   "Raw flesh is toxic; the toxin breaks down with cooking. Not a "
                   "mushroom to be casual about in a genus like this one.", NAMA[0]),
         note="Bruises and ages wine-red everywhere — cap, gills, stem, and especially "
              "wherever a slug has been. No sac at the base, and a ring that is grooved "
              "on top.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"bulbous": "always"},
             stipe_surface={"fibrous": "usually", "smooth": "sometimes"},
             ring={"skirt": "always"},
             gill_attachment={"free": "usually", "adnexed": "sometimes"},
             gill_spacing={"crowded": "usually"},
             cap_shape={"convex": "usually", "flat": "usually"},
             cap_margin={"straight": "usually", "striate": "sometimes"},
             cap_surface={"warty": "always", "viscid": "sometimes"},
             cap_colour={"red_brown": "usually", "tan": "usually", "pink": "sometimes"},
             veil_remnants={"warts": "always"},
             hymenophore_colour={"white": "usually", "pink": "sometimes"},
             flesh_colour={"white": "usually", "pink": "usually"},
             flesh_consistency={"soft": "always"}, latex="none",
             bruising={"yes": "always"},
             bruising_where={"cap": "usually", "stipe": "always", "flesh": "always",
                             "hymenophore": "sometimes"},
             bruising_colour={"pink": "always", "red": "usually"},
             bruising_speed={"slow": "usually", "minute": "sometimes"},
             odour={"none": "usually"},
             habitat={"mixed_wood": "usually", "broadleaf_wood": "usually",
                      "conifer_wood": "sometimes"},
             spore_print="pale", growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[60, 150]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF + NAMA, reviewed=False),

    dict(id="amanita_vaginata", scientificName="Amanita vaginata",
         commonName="grisette",
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Eaten in places, and in a genus where a mistake is fatal.", NAMA[0]),
         note="A sac at the base and **no ring at all**, with a deeply lined cap edge. "
              "Worth knowing precisely because it proves a volva alone settles nothing: "
              "the destroying angel has one too, and a ring besides.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate={"soil": "always"},
             stipe_presence="central", stipe_base={"sac_volva": "always"},
             stipe_surface={"smooth": "usually", "powdery": "sometimes"},
             stipe_flesh={"hollow": "sometimes", "stuffed": "usually"},
             ring={"absent": "always"},
             gill_attachment={"free": "always"}, gill_spacing={"crowded": "usually"},
             cap_shape={"egg": "sometimes", "bell": "usually", "flat": "usually",
                        "umbonate": "sometimes"},
             cap_margin={"striate": "always"},
             cap_surface={"smooth": "usually", "viscid": "sometimes"},
             cap_colour={"grey": "usually", "tan": "sometimes", "cream": "sometimes"},
             veil_remnants={"none": "usually", "single_patch": "sometimes"},
             hymenophore_colour={"white": "always"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "always"},
             latex="none", bruising={"no": "usually"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             spore_print="pale", growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[40, 100], stipe_height_mm=[70, 180]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=["ring", "cap_margin"],
                          note="A destroying angel has a ring and an unlined cap edge. "
                               "This has a lined edge and no ring. Both stand in a sac, "
                               "so the sac decides nothing on its own.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="cantharellus_appalachiensis", scientificName="Cantharellus appalachiensis",
         commonName="Appalachian chanterelle", hazard=hz(),
         note="Smaller and browner than the golden chanterelle, with a darker cap and "
              "the same blunt forking ridges running down the stem.",
         characters=ch(
             fruitbody_type="chanterelle_like",
             substrate={"soil": "always", "litter": "usually"},
             stipe_presence="central", stipe_base={"tapering": "usually"},
             stipe_surface={"smooth": "usually"}, ring={"absent": "always"},
             gill_attachment={"decurrent": "always"},
             gill_extras={"forked": "always", "veined": "usually"},
             gill_spacing={"distant": "usually"},
             cap_shape={"depressed": "usually", "funnel": "sometimes"},
             cap_margin={"wavy": "always"},
             cap_surface={"smooth": "usually", "dry": "usually"},
             cap_colour={"yellow_brown": "usually", "tan": "usually",
                         "olive": "sometimes"},
             cap_colour_pattern={"darker_centre": "usually"},
             hymenophore_colour={"yellow": "usually", "cream": "sometimes"},
             flesh_colour={"white": "usually", "yellow": "sometimes"},
             flesh_consistency={"fibrous": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"fruity": "sometimes", "none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"troop": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[15, 60], stipe_height_mm=[30, 80]),
         season=dict(months=[6, 7, 8, 9]),
         lookalikes=[dict(taxon="omphalotus_illudens",
                          discriminators=["flesh_colour", "gill_attachment",
                                          "growth_habit"],
                          note="Cut it: a jack-o'-lantern is orange all the way through "
                               "and grows in fused clumps on wood.")],
         sources=GBIF, reviewed=False),

    dict(id="craterellus_fallax", scientificName="Craterellus fallax",
         commonName="black trumpet", hazard=hz(),
         note="A thin dark trumpet, hollow to the base, easy to walk past. The outside "
              "is smooth or faintly wrinkled rather than gilled.",
         characters=ch(
             fruitbody_type="chanterelle_like",
             substrate={"soil": "always", "moss": "sometimes", "litter": "usually"},
             stipe_presence="central", stipe_flesh={"hollow": "always"},
             stipe_base={"tapering": "usually"}, ring={"absent": "always"},
             gill_attachment={"decurrent": "always"},
             cap_shape={"funnel": "always"}, cap_margin={"wavy": "always"},
             cap_surface={"wrinkled": "usually", "smooth": "sometimes",
                          "dry": "usually"},
             cap_colour={"black": "usually", "dark_brown": "usually",
                         "grey": "sometimes"},
             hymenophore_colour={"grey": "usually", "black": "sometimes"},
             flesh_colour={"grey": "usually", "black": "sometimes"},
             flesh_consistency={"fibrous": "usually", "leathery": "sometimes"},
             latex="none", bruising={"no": "usually"},
             odour={"fruity": "sometimes", "none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"troop": "always", "caespitose": "sometimes"}),
         measurements=dict(cap_width_mm=[10, 70], stipe_height_mm=[30, 90]),
         season=dict(months=[7, 8, 9]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="auricularia_americana", scientificName="Auricularia americana",
         commonName="wood ear", hazard=hz(),
         note="A rubbery brown ear on dead wood, smooth on one face and faintly veined "
              "on the other. Dries to a hard flake and comes back with rain.",
         characters=ch(
             fruitbody_type="jelly", substrate="wood",
             substrate_wood={"dead": "always", "conifer": "usually",
                             "broadleaf": "sometimes", "bare_wood": "usually"},
             stipe_presence={"absent_attached": "always"},
             cap_shape={"depressed": "usually"}, cap_margin={"wavy": "always"},
             cap_surface={"smooth": "usually", "wrinkled": "sometimes",
                          "velvety": "sometimes"},
             cap_colour={"red_brown": "usually", "dark_brown": "usually",
                         "tan": "sometimes"},
             flesh_colour={"red_brown": "usually"},
             flesh_consistency={"gelatinous": "always"},
             latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
             odour={"none": "usually"},
             habitat={"conifer_wood": "usually", "mixed_wood": "usually",
                      "broadleaf_wood": "sometimes"},
             spore_print="pale", growth_habit={"troop": "usually", "tiered": "usually"}),
         measurements=dict(cap_width_mm=[20, 100]),
         season=dict(months=[3, 4, 5, 6, 7, 8, 9, 10, 11]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="cerioporus_squamosus", scientificName="Cerioporus squamosus",
         commonName="dryad's saddle", hazard=hz(),
         note="Big flat scales in rings on a fan-shaped cap, a black stem at one side, "
              "and a smell of watermelon rind. One of the first large fungi of spring.",
         characters=ch(
             fruitbody_type="polypore", substrate="wood",
             substrate_wood={"broadleaf": "always", "dead": "usually",
                             "living": "sometimes"},
             stipe_presence={"lateral": "always", "off_centre": "sometimes"},
             stipe_surface={"smooth": "usually"},
             cap_shape={"depressed": "usually", "flat": "usually"},
             cap_margin={"wavy": "usually", "incurved": "sometimes"},
             cap_surface={"scaly": "always", "dry": "always", "zoned": "usually"},
             cap_colour={"cream": "usually", "tan": "usually", "yellow_brown": "usually"},
             hymenophore_colour={"white": "usually", "cream": "usually"},
             flesh_colour={"white": "always"},
             flesh_consistency={"fibrous": "usually", "leathery": "sometimes"},
             latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
             odour={"fruity": "usually", "none": "sometimes"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"few": "usually", "tiered": "sometimes"}),
         measurements=dict(cap_width_mm=[80, 400]),
         season=dict(months=[4, 5, 6, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="armillaria_tabescens", scientificName="Armillaria tabescens",
         commonName="ringless honey mushroom",
         hazard=hz("gastrointestinal", "GI", "1-3 hours",
                   "Upsets a good many people, and always when undercooked.", NAMA[0]),
         note="The honey mushroom without a ring. Big clumps at the foot of hardwoods, "
              "a white spore print, and gills running a little down the stem.",
         characters=ch(
             fruitbody_type="gilled_stemmed",
             substrate={"wood": "always", "soil": "sometimes"},
             substrate_wood={"broadleaf": "always", "buried": "usually",
                             "dead": "usually", "living": "sometimes"},
             stipe_presence="central", stipe_base={"tapering": "usually",
                                                   "equal": "sometimes"},
             stipe_surface={"fibrous": "always"}, ring={"absent": "always"},
             gill_attachment={"decurrent": "usually", "adnate": "sometimes"},
             gill_spacing={"close": "usually"},
             cap_shape={"convex": "usually", "flat": "usually",
                        "umbonate": "sometimes"},
             cap_surface={"scaly": "usually", "dry": "always"},
             cap_colour={"tan": "usually", "yellow_brown": "usually",
                         "red_brown": "sometimes"},
             hymenophore_colour={"white": "usually", "cream": "usually",
                                 "pink": "sometimes"},
             flesh_colour={"white": "always"}, flesh_consistency={"fibrous": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"none": "usually"},
             habitat={"broadleaf_wood": "always"},
             associated_tree={"broadleaf": "always"},
             spore_print="pale", growth_habit={"caespitose": "always", "fused": "usually"}),
         measurements=dict(cap_width_mm=[30, 100], stipe_height_mm=[50, 200]),
         season=dict(months=[8, 9, 10, 11]),
         lookalikes=[dict(taxon="galerina_marginata",
                          discriminators=["spore_print", "ring", "hymenophore_colour"],
                          note="Galerina drops a rusty brown print and has a ring zone. "
                               "This drops white and has no ring at all. A print settles "
                               "it and nothing in the field does.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="hypholoma_fasciculare", scientificName="Hypholoma fasciculare",
         commonName="sulphur tuft",
         hazard=hz("gastrointestinal", "GI", "5-10 hours",
                   "Severe and long-lasting for a gastrointestinal poisoning.", NAMA[0]),
         note="Sulphur-yellow clumps on dead wood with a greenish tinge to the gills as "
              "they age, and very bitter. Grows in exactly the places the honey mushroom "
              "and Galerina do.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="wood",
             substrate_wood={"broadleaf": "usually", "conifer": "sometimes",
                             "dead": "always", "buried": "sometimes"},
             stipe_presence="central", stipe_base={"tapering": "usually"},
             stipe_surface={"fibrous": "usually"}, stipe_flesh={"hollow": "sometimes"},
             ring={"zone": "usually", "absent": "sometimes"},
             gill_attachment={"adnate": "usually", "adnexed": "sometimes"},
             gill_spacing={"crowded": "always"},
             cap_shape={"convex": "usually", "flat": "sometimes"},
             cap_surface={"smooth": "usually", "dry": "usually"},
             cap_colour={"yellow": "always", "orange": "sometimes"},
             cap_colour_pattern={"darker_centre": "usually"},
             hymenophore_colour={"yellow": "usually", "green": "usually",
                                 "olive": "usually"},
             flesh_colour={"yellow": "always"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"none": "usually"}, taste={"bitter": "always"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
             spore_print={"dark": "usually", "brown": "sometimes"},
             growth_habit={"caespitose": "always", "troop": "usually"}),
         measurements=dict(cap_width_mm=[20, 70], stipe_height_mm=[40, 120]),
         season=dict(months=[6, 7, 8, 9, 10, 11]),
         lookalikes=[dict(taxon="armillaria_mellea",
                          discriminators=["taste", "hymenophore_colour", "spore_print"],
                          note="A honey mushroom is mild with white gills and a white "
                               "print. This is bitter, the gills go green, and the print "
                               "is dark.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="agaricus_xanthodermus", scientificName="Agaricus xanthodermus",
         commonName="yellow-stainer",
         hazard=hz("gastrointestinal", "GI", "1-3 hours",
                   "The commonest mushroom poisoning in places where field mushrooms "
                   "are picked, because it looks like one.", NAMA[0]),
         note="Looks like a field mushroom until you scratch the base of the stem: it "
              "goes chrome yellow within seconds, and it smells of ink or phenol, "
              "strongest when cooking.",
         characters=ch(
             fruitbody_type="gilled_stemmed",
             substrate={"soil": "always", "grass": "usually"},
             stipe_presence="central", stipe_base={"bulbous": "usually",
                                                   "marginate": "sometimes"},
             stipe_surface={"smooth": "usually"}, ring={"skirt": "always"},
             gill_attachment={"free": "always"}, gill_spacing={"crowded": "always"},
             cap_shape={"convex": "usually", "flat": "usually"},
             cap_margin={"incurved": "usually", "straight": "sometimes"},
             cap_surface={"smooth": "usually", "dry": "usually", "scaly": "sometimes"},
             cap_colour={"white": "usually", "cream": "usually", "grey": "sometimes"},
             hymenophore_colour={"pink": "usually", "dark_brown": "usually",
                                 "white": "sometimes"},
             flesh_colour={"white": "always", "yellow": "usually"},
             flesh_consistency={"soft": "always"}, latex="none",
             bruising={"yes": "always"},
             bruising_where={"stipe_base": "always", "cap": "usually",
                             "flesh": "usually"},
             bruising_colour={"yellow": "always"},
             bruising_speed={"instant": "always"},
             odour={"phenolic": "always", "chemical": "usually"},
             habitat={"improved_grass": "usually", "garden": "usually",
                      "urban": "usually", "disturbed": "usually"},
             spore_print="brown", spore_print_fine={"purple_brown": "usually"},
             growth_habit={"troop": "usually", "ring": "sometimes",
                           "caespitose": "sometimes"}),
         measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[60, 150]),
         season=dict(months=[6, 7, 8, 9, 10, 11]),
         lookalikes=[dict(taxon="agaricus_campestris",
                          discriminators=["bruising_colour", "bruising_where", "odour"],
                          note="Scratch the very base of the stem. A field mushroom does "
                               "not go chrome yellow in seconds, and does not smell of "
                               "ink.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="pluteus_cervinus", scientificName="Pluteus cervinus",
         commonName="deer shield", hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="On dead wood, gills free and turning pink, and a radish smell. Very "
              "common and easy to pass over.",
         characters=ch(
             fruitbody_type="gilled_stemmed", substrate="wood",
             substrate_wood={"broadleaf": "usually", "dead": "always",
                             "buried": "sometimes", "woodchip": "sometimes"},
             stipe_presence="central", stipe_base={"equal": "usually",
                                                   "bulbous": "sometimes"},
             stipe_surface={"fibrous": "always"}, stipe_flesh={"solid": "usually"},
             ring={"absent": "always"},
             gill_attachment={"free": "always"}, gill_spacing={"crowded": "usually"},
             cap_shape={"convex": "usually", "flat": "usually",
                        "umbonate": "sometimes"},
             cap_surface={"silky": "usually", "smooth": "sometimes"},
             cap_colour={"dark_brown": "usually", "grey": "sometimes",
                         "tan": "sometimes"},
             cap_colour_pattern={"darker_centre": "usually"},
             hymenophore_colour={"white": "sometimes", "pink": "usually"},
             flesh_colour={"white": "always"}, flesh_consistency={"soft": "usually"},
             latex="none", bruising={"no": "usually"},
             odour={"radish": "usually", "none": "sometimes"},
             habitat={"broadleaf_wood": "always"},
             spore_print={"pink": "always"},
             growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[40, 120], stipe_height_mm=[50, 120]),
         season=dict(months=[4, 5, 6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="clitocybe_nuda", scientificName="Clitocybe nuda", commonName="blewit",
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Upsets some people, and raw it upsets most.", NAMA[0]),
         note="Violet all over when young — cap, gills and stem — fading to tan. Pale "
              "pink spore print, and a smell some people call frozen orange juice.",
         characters=ch(
             fruitbody_type="gilled_stemmed",
             substrate={"litter": "always", "soil": "usually"},
             stipe_presence="central", stipe_base={"bulbous": "usually",
                                                   "equal": "sometimes"},
             stipe_surface={"fibrous": "usually", "smooth": "sometimes"},
             ring={"absent": "always"},
             gill_attachment={"adnexed": "usually", "notched": "usually",
                              "adnate": "sometimes"},
             gill_spacing={"crowded": "always"},
             cap_shape={"convex": "usually", "flat": "usually",
                        "depressed": "sometimes"},
             cap_margin={"inrolled": "usually", "wavy": "sometimes"},
             cap_surface={"smooth": "usually", "viscid": "sometimes"},
             cap_colour={"purple": "usually", "tan": "usually", "pink": "sometimes"},
             hymenophore_colour={"purple": "usually", "pink": "sometimes"},
             flesh_colour={"purple": "sometimes", "cream": "usually"},
             flesh_consistency={"soft": "usually"}, latex="none",
             bruising={"no": "usually"},
             odour={"sweet": "usually", "fruity": "usually"},
             habitat={"broadleaf_wood": "usually", "mixed_wood": "usually",
                      "disturbed": "sometimes", "garden": "sometimes"},
             spore_print={"pink": "usually", "pale": "sometimes"},
             growth_habit={"troop": "usually", "ring": "sometimes"}),
         measurements=dict(cap_width_mm=[50, 150], stipe_height_mm=[40, 100]),
         season=dict(months=[9, 10, 11, 12]),
         lookalikes=[dict(taxon="cortinarius_alboviolaceus",
                          discriminators=["spore_print", "ring"],
                          note="A violet Cortinarius drops a rusty brown print and has a "
                               "cortina band. This drops pale pink and has neither.")],
         sources=GBIF + NAMA, reviewed=False),

    dict(id="phallus_ravenelii", scientificName="Phallus ravenelii",
         commonName="stinkhorn", hazard=hz("unknown", "UNKNOWN", "—", "", "—"),
         note="Found by smell before sight: rotting meat, with flies on a slimy dark "
              "cap. Comes out of a white 'egg' in the ground or in woodchips.",
         characters=ch(
             fruitbody_type="gasteroid",
             substrate={"soil": "usually", "wood": "sometimes", "litter": "sometimes"},
             substrate_wood={"woodchip": "usually", "buried": "usually",
                             "dead": "usually"},
             stipe_presence="central", stipe_base={"sac_volva": "always"},
             stipe_surface={"punctate": "usually", "smooth": "sometimes"},
             stipe_flesh={"hollow": "always"},
             cap_shape={"bell": "usually", "conical": "sometimes"},
             cap_surface={"viscid": "always", "wrinkled": "sometimes"},
             cap_colour={"olive": "usually", "dark_brown": "usually",
                         "black": "sometimes"},
             flesh_colour={"white": "always"},
             flesh_consistency={"soft": "usually", "gelatinous": "sometimes"},
             latex="none", bruising={"no": "usually"}, ring={"absent": "always"},
             odour={"foetid": "always"},
             habitat={"disturbed": "usually", "garden": "usually",
                      "broadleaf_wood": "usually", "urban": "sometimes"},
             spore_print={"dark": "usually"},
             growth_habit={"single": "usually", "few": "usually"}),
         measurements=dict(cap_width_mm=[20, 50], stipe_height_mm=[80, 200]),
         season=dict(months=[6, 7, 8, 9, 10]),
         lookalikes=[], sources=GBIF, reviewed=False),

    dict(id="calvatia_craniiformis", scientificName="Calvatia craniiformis",
         commonName="skull-shaped puffball", hazard=hz(),
         note="A big pale puffball shaped like a skull or a loaf, on a narrow base. "
              "White inside when young and a brown dust when old.",
         characters=ch(
             fruitbody_type="gasteroid",
             substrate={"soil": "always", "litter": "usually", "grass": "sometimes"},
             stipe_presence={"absent_sitting": "always"},
             cap_shape={"convex": "usually"},
             cap_surface={"smooth": "usually", "wrinkled": "sometimes",
                          "cracked": "sometimes"},
             cap_colour={"white": "usually", "cream": "usually", "tan": "usually"},
             flesh_colour={"white": "usually", "yellow_brown": "sometimes"},
             flesh_consistency={"soft": "usually", "powdery": "usually"},
             latex="none",
             bruising={"yes": "sometimes"}, bruising_where={"flesh": "usually"},
             bruising_colour={"yellow_brown": "sometimes"},
             ring={"absent": "always"}, odour={"none": "usually"},
             habitat={"broadleaf_wood": "usually", "disturbed": "sometimes",
                      "improved_grass": "sometimes"},
             spore_print={"brown": "usually"},
             growth_habit={"few": "usually", "single": "usually"}),
         measurements=dict(cap_width_mm=[80, 200]),
         season=dict(months=[7, 8, 9, 10]),
         lookalikes=[dict(taxon="scleroderma_citrinum",
                          discriminators=["flesh_colour", "flesh_consistency",
                                          "cap_surface"],
                          note="An earthball is hard, thick-skinned and black-purple "
                               "inside almost from the start. Cut every puffball open.")],
         sources=GBIF, reviewed=False),
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

#!/usr/bin/env python3
"""
Eighth batch: the mushrooms this state actually records, that the pack could not hold.

Chosen from evidence rather than from memory, which is the only defensible way to add to
this file. iNaturalist's research-grade records for North Carolina were ranked by species
and diffed against the pack; lichens, rusts and sooty moulds were set aside because the
character schema describes caps, gills and stems and has nothing true to say about them.
What was left is two hundred and eighteen macrofungi the pack does not carry, in order of
how often somebody here has actually found one.

These seven are not simply the top seven. Five of them earn their place by what they let
the app *say* about something already in the pack:

  Stereum lobatum and Trametes betulina are both taken for turkey tail, which is in the
  pack and sought after, and both are told from it by looking underneath — no pores at
  all on one, folds like gills on the other.

  Phyllotopsis nidulans is taken for the oyster mushroom and smells of rotting cabbage,
  which is the whole of telling them apart.

  Apioperdon pyriforme and Scleroderma polyrhizum join the puffball group, where the
  warning that matters is the destroying angel in its egg stage and the habit of cutting
  every one of them top to bottom.

The other two fill genus holes the same audit showed: the fly agaric this region
actually has, and a Suillus.

Leucocoprinus birnbaumii, the yellow parasol out of every flowerpot, was written and then
taken out again. It makes people ill and there is nothing in this pack it is honestly
confused with, and the rule that anything harmful must carry a confusion is a good rule:
satisfying it by inventing a lookalike would be worse than leaving the mushroom out until
the small pale parasols it really does get taken for are in here too.

Seasons are the months holding at least eight per cent of that taxon's own North Carolina
records, except where the records run through the year and the fungus is a bracket that
persists, in which case they say so.

Same rules as every batch: macro-morphology any guide agrees on, no text reproduced,
reviewed false. Names checked against GBIF and iNaturalist before they were written down.

    python3 tools/add-taxa-8.py
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

# A bracket on dead wood, which is most of this batch's first half.
BRACKET = dict(substrate="wood", stipe_presence={"absent_attached": "always"},
               growth_habit={"tiered": "usually", "troop": "usually"},
               latex="none", spore_print="pale")

TAXA = [
    dict(id="stereum_lobatum", scientificName="Stereum lobatum",
         commonName="false turkey-tail", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Too thin and leathery to be eaten. Nothing documented against it.",
                   NAMA[0]),
         note="Turn it over. Turkey tail has a white surface finely pitted with pores you "
              "can see with a lens; this is smooth underneath, with nothing to see at "
              "all. The top is banded in much the same browns and tans, so the top will "
              "not settle it and the underside settles it at once.",
         characters=ch(**BRACKET,
                       # Not "polypore", though it is a tough bracket. Hedging
                       # both ways here made the pack unable to say the one thing that
                       # separates it from turkey tail, which is that there are no pores.
                       fruitbody_type={"chanterelle_like": "always",
                                       "resupinate": "sometimes"},
                       substrate_wood={"broadleaf": "usually", "dead": "always",
                                       "bare_wood": "usually"},
                       cap_surface={"zoned": "always", "velvety": "usually",
                                    "dry": "always", "hairy": "sometimes"},
                       cap_colour={"tan": "usually", "yellow_brown": "usually",
                                   "grey": "sometimes", "cream": "sometimes"},
                       cap_margin={"wavy": "usually"},
                       flesh_consistency={"leathery": "always"},
                       odour={"none": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[10, 70]),
         season=dict(months=list(range(1, 13)),
                     note="A bracket that persists, and recorded here in every month."),
         lookalikes=[dict(taxon="trametes_versicolor",
                          discriminators=["fruitbody_type"],
                          note="Turkey tail has pores underneath and this has a smooth "
                               "surface. Both are banded on top and that band is no "
                               "help.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="trametes_betulina", scientificName="Trametes betulina",
         commonName="gilled polypore", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "Tough and not eaten. Nothing documented against it.", NAMA[0]),
         note="A tough banded bracket whose underside is folded into blades running out "
              "from the attachment, so it looks gilled and is not. The blades are part of "
              "the same leathery flesh and will not come away as a gill does.",
         characters=ch(**BRACKET,
                       # Blades, not pores — which is what a reader sees and what
                       # tells it from turkey tail, whatever its relatives are.
                       fruitbody_type={"gilled_stemmed": "always"},
                       substrate_wood={"broadleaf": "usually", "dead": "always",
                                       "bare_wood": "usually"},
                       gill_attachment={"no_stem": "always"},
                       gill_spacing={"distant": "usually"},
                       hymenophore_colour={"white": "usually", "cream": "usually",
                                           "tan": "sometimes"},
                       cap_surface={"zoned": "always", "hairy": "usually",
                                    "dry": "always"},
                       cap_colour={"grey": "usually", "cream": "usually",
                                   "tan": "usually"},
                       flesh_consistency={"leathery": "always"},
                       odour={"none": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[20, 100]),
         season=dict(months=list(range(1, 13)),
                     note="A bracket that persists, and recorded here in every month."),
         lookalikes=[dict(taxon="trametes_versicolor",
                          discriminators=["fruitbody_type"],
                          note="Turkey tail has fine pores; this has blades wide enough "
                               "to count. Otherwise they are the same banded bracket on "
                               "the same dead hardwood.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="phyllotopsis_nidulans", scientificName="Phyllotopsis nidulans",
         commonName="stinking orange oyster", prevalence="occasional", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Nothing documented either way, and nobody eats it: the smell is the "
                   "reason.", NAMA[0]),
         note="A stemless orange shelf on dead wood, densely hairy on top, with orange "
              "gills. The smell is the thing — strong and foul, of rotting cabbage or "
              "worse, from a mushroom that otherwise looks like a bright oyster.",
         characters=ch(substrate="wood",
                       fruitbody_type={"gilled_stemmed": "always"},
                       stipe_presence={"absent_attached": "always"},
                       substrate_wood={"broadleaf": "usually", "conifer": "sometimes",
                                       "dead": "always"},
                       gill_attachment={"no_stem": "always"},
                       gill_spacing={"crowded": "usually"},
                       hymenophore_colour={"orange": "always", "yellow": "usually"},
                       cap_surface={"hairy": "always", "dry": "always"},
                       cap_colour={"orange": "always", "yellow": "usually"},
                       cap_margin={"inrolled": "usually"},
                       flesh_consistency={"soft": "usually"},
                       odour={"foetid": "always", "unpleasant_other": "usually"},
                       latex="none",
                       growth_habit={"tiered": "usually", "caespitose": "usually"},
                       spore_print="pink",
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"}),
         measurements=dict(cap_width_mm=[20, 80]),
         season=dict(months=[10, 11, 12, 1, 2],
                     note="Late autumn into winter, with the cold months holding most of "
                          "the records here."),
         lookalikes=[dict(taxon="pleurotus_ostreatus",
                          discriminators=["cap_colour", "odour", "hymenophore_colour"],
                          note="The oyster is white to grey-brown, smooth, and smells "
                               "faintly of aniseed or of nothing. This is orange, felted "
                               "with hair, and smells bad enough to settle it across the "
                               "room.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="apioperdon_pyriforme", scientificName="Apioperdon pyriforme",
         commonName="pear-shaped puffball", prevalence="common", sought=True,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Eaten by some while it is white throughout. The rule for every "
                   "puffball is the same and is the reason to cut it.", NAMA[0]),
         note="The one puffball that grows on wood, usually in crowds on a rotting stump "
              "with white cords running out of the base into it. Cut it top to bottom: "
              "white and uniform like a marshmallow, or it is not this and may not be a "
              "puffball at all.",
         characters=ch(fruitbody_type={"gasteroid": "always"},
                       substrate={"wood": "always", "litter": "sometimes"},
                       substrate_wood={"dead": "always", "broadleaf": "usually",
                                       "conifer": "sometimes", "buried": "sometimes"},
                       stipe_presence={"absent_sitting": "always"},
                       stipe_base={"rhizomorphs": "usually"},
                       cap_surface={"smooth": "usually", "powdery": "sometimes",
                                    "dry": "always"},
                       cap_colour={"white": "usually", "cream": "usually",
                                   "tan": "usually", "yellow_brown": "usually"},
                       flesh_colour={"white": "usually", "olive": "usually",
                                     "yellow_brown": "usually"},
                       flesh_consistency={"soft": "usually", "powdery": "usually"},
                       latex="none",
                       growth_habit={"troop": "always", "caespitose": "usually"},
                       odour={"none": "usually", "mushroomy": "sometimes"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"}),
         measurements=dict(cap_width_mm=[15, 45]),
         season=dict(months=[9, 10, 11, 12, 1],
                     note="Autumn into winter; October and November hold most of it."),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=["substrate", "fruitbody_type", "stipe_base"],
                          note="A destroying angel in its egg stage is a white ball in "
                               "the soil, and cut in half it shows the outline of a cap "
                               "and gills inside. This grows on wood and is uniform paste "
                               "throughout. Cut every one.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="scleroderma_polyrhizum", scientificName="Scleroderma polyrhizum",
         commonName="many-rooted earthball", prevalence="common", sought=False,
         hazard=hz("other", "GI", "30 minutes to 2 hours",
                   "Almost every case is a puffball mistaken. Vomiting and cramps.",
                   NAMA[0]),
         note="A hard pale ball half buried in bare or trodden ground, which splits open "
              "from the top into a coarse star as it matures. Thick rind, and black-purple "
              "spore mass inside almost from the start — a puffball is white paste "
              "throughout at the size you would want to pick one.",
         characters=ch(fruitbody_type={"gasteroid": "always"},
                       substrate={"soil": "always"},
                       stipe_presence={"absent_sitting": "always"},
                       stipe_base={"rhizomorphs": "usually", "buried": "usually"},
                       cap_surface={"cracked": "usually", "dry": "always",
                                    "scaly": "sometimes"},
                       cap_colour={"white": "usually", "cream": "usually",
                                   "tan": "usually"},
                       flesh_colour={"black": "always", "purple": "usually"},
                       flesh_consistency={"powdery": "usually"},
                       latex="none",
                       growth_habit={"few": "usually", "single": "usually"},
                       habitat={"disturbed": "usually", "improved_grass": "usually",
                                "broadleaf_wood": "sometimes", "urban": "sometimes"}),
         measurements=dict(cap_width_mm=[40, 150]),
         season=dict(months=[8, 9, 10, 11, 12, 1],
                     note="Late summer through winter on hard ground."),
         lookalikes=[dict(taxon="lycoperdon_perlatum",
                          discriminators=["flesh_colour"],
                          note="A puffball is white and uniform inside while it is worth "
                               "anything. This has a thick rind and is dark inside almost "
                               "at once.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="amanita_persicina", scientificName="Amanita persicina",
         commonName="peach-coloured fly agaric", prevalence="common", sought=False,
         hazard=hz("ibotenic", "INTOXICATION", "30 minutes to 3 hours",
                   "Delirium and sedation rather than organ damage. Rarely fatal, "
                   "reliably unpleasant.", NAMA[0]),
         note="The fly agaric this region actually has: peach to melon-orange rather than "
              "scarlet, often fading to nearly cream at the margin, with pale warts that "
              "wash off in rain. The base is bulbous with rings of scales rather than a "
              "bag, which is the character that separates the whole group from the deadly "
              "white ones.",
         characters=ch(fruitbody_type={"gilled_stemmed": "always"},
                       substrate={"soil": "always", "litter": "usually"},
                       stipe_presence={"central": "always"},
                       stipe_base={"bulbous": "always", "volva_rings": "usually"},
                       stipe_surface={"smooth": "usually", "scaly": "sometimes"},
                       ring={"skirt": "usually"},
                       veil_remnants={"warts": "usually", "none": "sometimes"},
                       gill_attachment={"free": "always"},
                       gill_spacing={"crowded": "usually"},
                       hymenophore_colour={"white": "always", "cream": "usually"},
                       cap_shape={"convex": "usually", "flat": "usually"},
                       cap_surface={"viscid": "usually", "warty": "usually"},
                       cap_colour={"orange": "always", "yellow": "usually",
                                   "cream": "sometimes"},
                       cap_margin={"striate": "usually"},
                       flesh_colour={"white": "always"},
                       flesh_consistency={"soft": "usually"},
                       latex="none",
                       spore_print="pale",
                       odour={"none": "usually"},
                       growth_habit={"single": "usually", "few": "usually"},
                       habitat={"conifer_wood": "usually", "mixed_wood": "usually",
                                "broadleaf_wood": "sometimes"},
                       associated_tree={"conifer": "usually", "broadleaf": "sometimes"}),
         measurements=dict(cap_width_mm=[45, 150], stipe_height_mm=[60, 160]),
         season=dict(months=[5, 6, 8, 9, 10, 11],
                     note="A long season with a heavy peak in October."),
         # No discriminators, and that is the honest answer: nothing in this
         # schema separates them. They are the same mushroom to a reader, and the
         # note says so rather than naming a character that would not settle it.
         lookalikes=[dict(taxon="amanita_muscaria_guessowii",
                          discriminators=[],
                          note="The yellow-orange fly agaric of the north is lemon to "
                               "egg-yolk; this is peach to melon. They are the same "
                               "mushroom in every other respect and the line between them "
                               "is geography as much as colour.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="suillus_spraguei", scientificName="Suillus spraguei",
         commonName="painted suillus", prevalence="common", sought=True,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Eaten by some. Suillus is a genus that disagrees with a fair number "
                   "of people.", NAMA[0]),
         note="A bolete shaggy with dark red fibres over a yellow ground, with a ring and "
              "yellow pores that stain brown where pressed. It goes with white pine and "
              "very little else, so the tree is half the identification.",
         characters=ch(fruitbody_type={"bolete": "always"},
                       substrate={"soil": "always", "litter": "usually"},
                       stipe_presence={"central": "always"},
                       stipe_base={"equal": "usually"},
                       stipe_surface={"fibrous": "usually", "scaly": "usually"},
                       ring={"skirt": "usually", "zone": "sometimes"},
                       hymenophore_colour={"yellow": "always", "olive": "sometimes"},
                       cap_shape={"convex": "usually", "flat": "sometimes"},
                       cap_surface={"hairy": "always", "scaly": "usually",
                                    "dry": "usually"},
                       cap_colour={"red": "always", "red_brown": "usually",
                                   "yellow": "sometimes"},
                       bruising={"yes": "always"},
                       bruising_where={"hymenophore": "usually", "flesh": "usually"},
                       bruising_colour={"red_brown": "usually", "dark_brown": "usually"},
                       flesh_colour={"yellow": "always"},
                       flesh_consistency={"soft": "usually"},
                       latex="none",
                       spore_print="brown",
                       odour={"none": "usually", "mushroomy": "sometimes"},
                       growth_habit={"few": "usually", "troop": "usually"},
                       habitat={"conifer_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"conifer": "always"}),
         measurements=dict(cap_width_mm=[30, 120], stipe_height_mm=[40, 120]),
         season=dict(months=[5, 6, 7, 8, 9, 10],
                     note="Through the summer under white pine, heaviest in August."),
         lookalikes=[],
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

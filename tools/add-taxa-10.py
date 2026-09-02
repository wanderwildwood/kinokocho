#!/usr/bin/env python3
"""
Tenth batch: the ones that come out of an egg, and the two that look like a sweet.

Held back from the ninth batch on purpose, because they want writing up together. Four
stinkhorns, two Calostoma and a bird's nest are all the same trick — spores made in a mass
inside something rather than dropped off gills — and the app had exactly one of them.

The reason they matter is not the smell. It is the egg.

A stinkhorn begins as a white ball half buried in the ground, and so does a destroying
angel. That is the confusion this pack exists to catch, and the one stinkhorn already in
it recorded no confusion at all: its own note says "comes out of a white egg in the
ground" and its lookalike list was empty, so a reader who found an egg was told nothing.
This batch links every one of them to Amanita bisporigera and patches the same link onto
Phallus ravenelii, which should have had it from the start.

Cut lengthwise, the two are not alike at all. A stinkhorn egg is a jelly layer around an
olive core and smells of nothing much yet. An Amanita egg is firm and white and shows the
outline of a cap, gills and stem, drawn small. That is the whole of it, and it is why the
habit worth teaching is cutting every white ball top to bottom — the same habit the
puffballs are here to teach.

Clathrus columnatus is also a winter fungus here, which the ninth batch was short of.

Same rules as every batch: macro-morphology any guide agrees on, no text reproduced,
reviewed false. Names checked against GBIF and iNaturalist and recorded in
verified-names.json before they were written down, and every discriminator checked to be a
character that genuinely separates the pair.

    python3 tools/add-taxa-10.py
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

# Everything that rises out of a volva. The stipe has to be called central for the schema
# to let the volva be recorded at all — stipe_base depends on it — and the volva is the
# character this whole batch is here for.
FROM_EGG = dict(
    fruitbody_type="gasteroid",
    stipe_presence={"central": "always"},
    stipe_base={"sac_volva": "always"},
    latex="none",
    spore_print="dark",
)

# What separates any stinkhorn egg from a destroying angel egg, cut top to bottom.
EGG_NOTE = ("Both begin as a white ball half buried in the ground. Cut it lengthwise: "
            "this is a jelly layer around an olive core, and an Amanita egg is firm and "
            "white with the outline of a cap, gills and stem drawn small inside.")
# Not smell, which was the obvious choice and is wrong: a destroying angel that has stood
# a few days smells foul too, and the pack says so. What separates them is what each one
# turns into — a mass of dark spore slime against gills and a white print.
EGG_APART = ["fruitbody_type", "spore_print"]

TAXA = [
    dict(id="mutinus_elegans", scientificName="Mutinus elegans",
         commonName="devil's dipstick", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Nothing documented either way. The egg stage is eaten in places; the "
                   "mature fungus is not.", NAMA[0]),
         note="A single tapering orange-pink spike out of a white egg, with no separate "
              "cap — the dark slimy spore mass is smeared over the upper third of the "
              "spike itself. Flies find it before you do.",
         characters=ch(**FROM_EGG,
                       substrate={"soil": "usually", "litter": "usually",
                                  "wood": "sometimes"},
                       substrate_wood={"woodchip": "usually", "buried": "usually"},
                       # Pitted all over, which is what separates this spike from the
                       # grooved columns of Clathrus. At equal strength on both they tied
                       # and this one lost the alphabet, and a taxon that cannot be
                       # reached by answering truthfully is a row too thin to be found.
                       stipe_surface={"punctate": "always", "smooth": "sometimes"},
                       stipe_flesh={"hollow": "always"},
                       # Pink first and definite. This and the column stinkhorn were
                       # written as the same orange mushroom in every character they
                       # share, so answering one truthfully reached the other and this
                       # one could never be arrived at. They are not the same colour in
                       # the hand: this is the pink-orange spike, that is the red-orange
                       # arch.
                       #
                       # And no hedging. Allowing each of them a little of the other's
                       # hue cost them the top place rather than winning it: a row that
                       # admits more colours is a vaguer answer to "pink", and scores
                       # below a tighter row that also says pink. Both were beaten by the
                       # wrinkly stinkhorn on their own colour that way.
                       flesh_colour={"pink": "always", "orange": "always",
                                     "white": "usually"},
                       flesh_consistency={"soft": "usually", "gelatinous": "usually"},
                       odour={"foetid": "always"},
                       bruising={"no": "always"},
                       ring={"absent": "always"},
                       growth_habit={"single": "usually", "few": "usually"},
                       habitat={"disturbed": "usually", "garden": "usually",
                                "broadleaf_wood": "usually", "urban": "sometimes"}),
         measurements=dict(stipe_height_mm=[70, 170]),
         # Summer into autumn, not high summer alone. Written as May to August it was
         # out of season on a September walk and lost every tie to the wrinkly stinkhorn,
         # which it otherwise matches character for character — and October records of
         # this are ordinary.
         season=dict(months=[5, 6, 7, 8, 9, 10],
                     note="Summer and into autumn, and gone within a day or two of the "
                          "flies finding it."),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=EGG_APART, note=EGG_NOTE)],
         sources=INAT + MORPH, reviewed=False),

    dict(id="satyrus_rugulosus", scientificName="Satyrus rugulosus",
         commonName="wrinkly stinkhorn", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Nothing documented either way, and not collected.", NAMA[0]),
         note="A slender stinkhorn with a distinct wrinkled cap on a pale pink to reddish "
              "stalk, out of a white egg. Smaller and neater than the common stinkhorn, "
              "and the reddening of the stalk is what marks it.",
         characters=ch(**FROM_EGG,
                       substrate={"soil": "usually", "litter": "usually",
                                  "wood": "sometimes"},
                       substrate_wood={"woodchip": "usually", "buried": "usually"},
                       stipe_surface={"punctate": "usually", "smooth": "usually"},
                       stipe_flesh={"hollow": "always"},
                       cap_shape={"conical": "usually", "bell": "usually"},
                       cap_surface={"wrinkled": "always", "viscid": "always"},
                       cap_colour={"dark_brown": "usually", "olive": "usually"},
                       # Pink stalk, and no white. That is the whole difference from
                       # Ravenel's stinkhorn, which is the other capped one here and is
                       # white — claiming white too left the two of them with nothing
                       # that separated them.
                       flesh_colour={"pink": "always", "red": "usually"},
                       flesh_consistency={"soft": "usually", "gelatinous": "usually"},
                       odour={"foetid": "always"},
                       bruising={"no": "always"},
                       ring={"absent": "always"},
                       growth_habit={"single": "usually", "few": "usually"},
                       habitat={"disturbed": "usually", "garden": "usually",
                                "broadleaf_wood": "usually", "urban": "sometimes"}),
         measurements=dict(stipe_height_mm=[50, 150]),
         season=dict(months=[5, 6, 7, 8, 9, 10],
                     note="Warm months, in mulch and garden ground as much as in wood."),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=EGG_APART, note=EGG_NOTE)],
         sources=INAT + MORPH, reviewed=False),

    dict(id="clathrus_columnatus", scientificName="Clathrus columnatus",
         commonName="column stinkhorn", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Nothing documented either way, and not collected.", NAMA[0]),
         note="Two to five orange columns rising out of a white egg and meeting at the "
              "top, like a small arch or cage, with the dark spore slime held on their "
              "inner faces. Nothing else here has that shape.",
         characters=ch(**FROM_EGG,
                       substrate={"soil": "usually", "litter": "usually",
                                  "wood": "sometimes"},
                       substrate_wood={"woodchip": "usually", "buried": "usually"},
                       stipe_surface={"grooved": "always", "punctate": "sometimes"},
                       stipe_flesh={"hollow": "always"},
                       flesh_colour={"red": "always", "orange": "always",
                                     "white": "usually"},
                       flesh_consistency={"soft": "usually", "gelatinous": "usually"},
                       odour={"foetid": "always"},
                       bruising={"no": "always"},
                       ring={"absent": "always"},
                       growth_habit={"single": "usually", "few": "usually"},
                       habitat={"garden": "usually", "disturbed": "usually",
                                "improved_grass": "usually", "urban": "usually"}),
         measurements=dict(stipe_height_mm=[30, 90]),
         season=dict(months=[9, 10, 11, 12, 1, 2, 3, 4],
                     note="The cool half of the year, in mulch and lawns, though a warm "
                          "wet spell can bring it up at either shoulder."),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=EGG_APART, note=EGG_NOTE)],
         sources=INAT + MORPH, reviewed=False),

    dict(id="pseudocolus_fusiformis", scientificName="Pseudocolus fusiformis",
         commonName="stinky squid", prevalence="occasional", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Nothing documented either way, and not collected.", NAMA[0]),
         note="Three or four orange arms on a short white stalk, joined at the tips so "
              "the whole thing stands like a claw out of its egg. The spore slime is "
              "carried on the inner surfaces of the arms.",
         characters=ch(**FROM_EGG,
                       substrate={"litter": "usually", "wood": "usually",
                                  "soil": "sometimes"},
                       substrate_wood={"woodchip": "always", "buried": "usually"},
                       stipe_surface={"smooth": "usually"},
                       stipe_flesh={"hollow": "always"},
                       flesh_colour={"orange": "always", "white": "usually"},
                       flesh_consistency={"soft": "usually", "gelatinous": "usually"},
                       odour={"foetid": "always"},
                       bruising={"no": "always"},
                       ring={"absent": "always"},
                       # In numbers, and that is the character. The other stinkhorns
                       # here come up singly or a few together; this one floods a fresh
                       # mulch bed and can put up dozens. Recorded as merely "usually" it
                       # was the one thing separating it from them and it was not saying
                       # so, which left it reachable only after the two it shares its
                       # orange with.
                       growth_habit={"troop": "always", "few": "usually"},
                       habitat={"garden": "usually", "disturbed": "usually",
                                "broadleaf_wood": "sometimes"}),
         measurements=dict(stipe_height_mm=[30, 80]),
         season=dict(months=[5, 6, 7, 8, 9],
                     note="Summer, and almost always in fresh woodchip."),
         lookalikes=[dict(taxon="amanita_bisporigera",
                          discriminators=EGG_APART, note=EGG_NOTE)],
         sources=INAT + MORPH, reviewed=False),

    dict(id="calostoma_cinnabarinum", scientificName="Calostoma cinnabarinum",
         commonName="stalked puffball-in-aspic", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Nothing documented either way. Too small and too odd to be "
                   "collected.", NAMA[0]),
         note="A scarlet ball the size of a marble, sitting on a spongy latticed stalk "
              "and coated in clear jelly when fresh, on bare clay banks. The mouth at the "
              "top is a ring of red teeth. Nothing else looks remotely like it.",
         characters=ch(fruitbody_type={"gasteroid": "always"},
                       substrate={"soil": "always"},
                       stipe_presence={"central": "always"},
                       stipe_base={"buried": "usually", "equal": "usually"},
                       stipe_surface={"grooved": "usually", "punctate": "usually"},
                       cap_colour={"red": "always", "orange": "sometimes"},
                       cap_surface={"viscid": "usually", "smooth": "usually"},
                       flesh_colour={"yellow": "usually", "white": "usually"},
                       flesh_consistency={"gelatinous": "usually", "powdery": "usually"},
                       latex="none",
                       bruising={"no": "always"},
                       ring={"absent": "always"},
                       odour={"none": "usually"},
                       spore_print="pale",
                       growth_habit={"troop": "usually", "few": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[8, 20], stipe_height_mm=[10, 40]),
         season=dict(months=[7, 8, 9],
                     note="High summer on bare banks, often after heavy rain."),
         lookalikes=[dict(taxon="calostoma_lutescens",
                          discriminators=["cap_colour"],
                          note="The collared one is yellow and has a distinct rim of "
                               "tissue where the ball meets the stalk. This one is "
                               "scarlet and has none.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="calostoma_lutescens", scientificName="Calostoma lutescens",
         commonName="collared calostoma", prevalence="common", sought=False,
         hazard=hz("unknown", "UNKNOWN", "—",
                   "Nothing documented either way, and not collected.", NAMA[0]),
         note="A yellow ball on a spongy stalk with a distinct collar of tissue at the "
              "join, on bare woodland banks. The mouth is a small red star at the top of "
              "an otherwise yellow head.",
         characters=ch(fruitbody_type={"gasteroid": "always"},
                       substrate={"soil": "always"},
                       stipe_presence={"central": "always"},
                       stipe_base={"buried": "usually", "equal": "usually"},
                       stipe_surface={"grooved": "usually", "punctate": "usually"},
                       cap_colour={"yellow": "always", "cream": "sometimes"},
                       cap_surface={"smooth": "usually", "powdery": "sometimes"},
                       flesh_colour={"yellow": "usually", "white": "usually"},
                       flesh_consistency={"powdery": "usually"},
                       latex="none",
                       bruising={"no": "always"},
                       ring={"absent": "always"},
                       odour={"none": "usually"},
                       spore_print="pale",
                       growth_habit={"troop": "usually", "few": "usually"},
                       habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
                       associated_tree={"broadleaf": "usually"}),
         measurements=dict(cap_width_mm=[6, 15], stipe_height_mm=[10, 35]),
         season=dict(months=[9, 10, 11, 12, 1, 2, 3],
                     note="Autumn through the winter, later than the scarlet one."),
         lookalikes=[dict(taxon="calostoma_cinnabarinum",
                          discriminators=["cap_colour"],
                          note="The scarlet one is bright red and coated in jelly when "
                               "fresh. This is yellow, with a collar where the ball meets "
                               "the stalk.")],
         sources=INAT + MORPH, reviewed=False),

    dict(id="cyathus_striatus", scientificName="Cyathus striatus",
         commonName="fluted bird's nest", prevalence="common", sought=False,
         hazard=hz("none_known", "NONE_KNOWN", "—",
                   "A few millimetres across. Nothing documented against it.", NAMA[0]),
         note="Tiny brown cups on woodchip and twigs, grooved down the inside, each "
              "holding a few flat grey discs like eggs in a nest. Rain knocks the discs "
              "out, which is the whole arrangement.",
         characters=ch(fruitbody_type={"cup_disc": "always", "gasteroid": "usually"},
                       substrate={"wood": "always", "litter": "usually"},
                       substrate_wood={"woodchip": "always", "dead": "always",
                                       "broadleaf": "usually"},
                       stipe_presence={"absent_sitting": "always"},
                       cap_colour={"grey": "usually", "dark_brown": "usually",
                                   "tan": "usually"},
                       cap_surface={"hairy": "usually", "striate": "always"},
                       flesh_colour={"grey": "usually", "dark_brown": "usually"},
                       flesh_consistency={"leathery": "usually"},
                       latex="none",
                       odour={"none": "usually"},
                       spore_print="pale",
                       growth_habit={"troop": "always", "fused": "sometimes"},
                       habitat={"garden": "usually", "disturbed": "usually",
                                "broadleaf_wood": "usually"}),
         measurements=dict(cap_width_mm=[5, 10]),
         season=dict(months=[7, 8, 9, 10],
                     note="Late summer and autumn, in fresh mulch above all."),
         lookalikes=[],
         sources=INAT + MORPH, reviewed=False),
]

# The stinkhorn already in the pack should have carried the egg confusion from the start.
# Its own note says it comes out of a white egg and its lookalike list was empty, so a
# reader who found one was told nothing at all.
PATCH = {
    "phallus_ravenelii": dict(taxon="amanita_bisporigera",
                              discriminators=EGG_APART, note=EGG_NOTE),
}

# Five stinkhorns, all of them orange or pink, all of them foetid, all of them out of a
# white egg in mulch — and every one pointed only at the death angel, never at each other.
# The confusion a reader actually has in the garden is which stinkhorn this is, and the
# difference is the shape rather than any character the key can ask about. So each names
# the others and says what it is instead.
SHAPE = {
    "mutinus_elegans": "a single unbranched spike with no separate cap, the slime "
                       "smeared over its upper third",
    "satyrus_rugulosus": "a distinct wrinkled cap on a slender stalk",
    "clathrus_columnatus": "two to five columns joined at the top into an arch",
    "pseudocolus_fusiformis": "three or four arms joined at their tips, like a claw",
    "phallus_ravenelii": "a thimble-shaped cap with a smooth, unpitted surface",
}

# Which character actually separates a given pair is not something to assert by hand. A
# discriminator that names a character both mushrooms answer the same way is worse than
# none: the candidate page prints it as advice, and the key pulls it forward ahead of its
# information gain, so a reader is sent to look at the one thing that cannot tell them
# apart. So the pair is asked of the data, under the same always-or-usually rule the test
# uses, and only characters that genuinely settle it are named.
# cap_presence leads, because for these five it is the difference a reader sees first:
# three of them are a bare spike, columns or arms, and two have a real cap on top.
SHAPE_CANDIDATES = ["cap_presence", "stipe_surface", "flesh_colour", "cap_shape",
                    "cap_surface", "cap_colour", "growth_habit", "fruitbody_type"]


def definite(taxon, character_id):
    """The values a reader can rely on — what the key treats as this taxon's answer."""
    return {s["value"] for s in taxon["characters"].get(character_id, [])
            if s["frequency"] in ("always", "usually")}


def separating(one, other):
    """Characters where the two give answers that cannot both be right."""
    found = []
    for character_id in SHAPE_CANDIDATES:
        mine, theirs = definite(one, character_id), definite(other, character_id)
        if mine and theirs and not (mine & theirs):
            found.append(character_id)
    return found


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

    unseparated = []
    for tid, shape in SHAPE.items():
        target = next((t for t in pack["taxa"] if t["id"] == tid), None)
        if target is None:
            continue
        existing = {l["taxon"] for l in target.get("lookalikes", [])}
        for other, other_shape in SHAPE.items():
            if other == tid or other in existing:
                continue
            partner = next(t for t in pack["taxa"] if t["id"] == other)
            apart = separating(target, partner)
            if not apart:
                # These two differ by whether there is a cap at all, and the schema has no
                # way to say "this one has none" — an absent character reads as unknown,
                # not as absent. Naming a discriminator anyway would be a lie on the page.
                unseparated.append(f"{tid} vs {other}")
                continue
            target.setdefault("lookalikes", []).append(collections.OrderedDict(
                taxon=other,
                discriminators=apart,
                note=f"Both come out of a white egg and both stink. This one is {shape}; "
                     f"{partner['scientificName']} is {other_shape}."))
        print(f"  ~ {target['scientificName']}: now names the other stinkhorns")

    for tid, lookalike in PATCH.items():
        target = next((t for t in pack["taxa"] if t["id"] == tid), None)
        if target is None:
            continue
        existing = {l["taxon"] for l in target.get("lookalikes", [])}
        if lookalike["taxon"] not in existing:
            target.setdefault("lookalikes", []).append(collections.OrderedDict(lookalike))
            print(f"  ~ {target['scientificName']}: now names the egg confusion")

    if unseparated:
        print("\n  no character in the schema separates these pairs, so they are not "
              "cross-linked:")
        for pair in unseparated:
            print(f"    {pair}")

    pack["taxa"].sort(key=lambda t: t["scientificName"])
    with open(PACK, "w", encoding="utf-8") as handle:
        json.dump(pack, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print(f"\n{added} added; the pack now holds {len(pack['taxa'])}.")


if __name__ == "__main__":
    main()

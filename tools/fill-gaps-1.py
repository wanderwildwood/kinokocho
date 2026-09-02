#!/usr/bin/env python3
"""Fill in characters that hazardous mushrooms were simply missing.

Grouping the pack by fruitbody type and asking which characters most of a group carries
turns up rows that are thinner than their neighbours for no reason. That is the shape of
the *Meripilus* bug — a taxon that agreed with everything it was asked and had fewer
places to agree — and it lands worst here: the destroying angel had no habitat, no growth
habit and no flesh consistency, and *Galerina marginata* had no habitat either. Both can
kill. A reader answering "in grass, in a ring" gets nothing from a row that never said
where it grows.

Not every silence is a gap. A stemless bracket has no stipe base and no ring, and filling
those would invent a stem. Only taxa that actually have the part are touched here, which
is why the list is written out by hand rather than taken from the audit.

Nothing here overwrites an existing value. If a character is already scored the taxon is
left alone, so this can be re-run.
"""

import collections
import json
from pathlib import Path

PACK = Path(__file__).resolve().parent.parent / \
    "app/src/main/assets/packs/southern-appalachia-v1.json"

# Ordered: the value a reader is most likely to meet comes first, because that is the one
# the key walks. And kept tight — a row that admits every possibility answers nothing.
FILL = {
    # Kills. Had no habitat and no growth habit at all, so answering where it was found
    # could never bring it up.
    "amanita_bisporigera": dict(
        flesh_consistency={"soft": "always"},
        habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
        growth_habit={"single": "usually", "few": "usually"}),

    # Kills, and the whole danger is that it grows where an edible wood mushroom grows.
    "galerina_marginata": dict(
        flesh_consistency={"soft": "always"},
        habitat={"conifer_wood": "usually", "mixed_wood": "usually",
                 "broadleaf_wood": "usually"}),

    "hygrophoropsis_aurantiaca": dict(
        stipe_base={"equal": "usually", "tapering": "sometimes"},
        ring={"absent": "always"},
        flesh_consistency={"soft": "always"},
        odour={"none": "usually"},
        habitat={"conifer_wood": "usually", "mixed_wood": "usually"},
        growth_habit={"few": "usually", "troop": "usually"}),

    # The commonest cause of mushroom poisoning in the country, and it had no habitat —
    # which for this one is the single most telling thing about it. It is a lawn mushroom.
    "chlorophyllum_molybdites": dict(
        stipe_base={"bulbous": "usually", "equal": "sometimes"},
        flesh_colour={"white": "always"},
        flesh_consistency={"soft": "always"},
        odour={"none": "usually"},
        habitat={"improved_grass": "always", "urban": "usually", "garden": "usually"}),

    "amanita_multisquamosa": dict(
        cap_shape={"convex": "usually", "flat": "usually", "umbonate": "sometimes"},
        flesh_colour={"white": "always"},
        flesh_consistency={"soft": "always"},
        odour={"none": "usually"},
        habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
        growth_habit={"single": "usually", "few": "usually"}),

    "amanita_flavoconia": dict(
        cap_shape={"convex": "usually", "flat": "usually"},
        flesh_colour={"white": "always", "yellow": "sometimes"},
        flesh_consistency={"soft": "always"},
        odour={"none": "usually"},
        habitat={"mixed_wood": "usually", "broadleaf_wood": "usually",
                 "conifer_wood": "usually"},
        growth_habit={"single": "usually", "few": "usually"}),

    "amanita_parcivolvata": dict(
        flesh_colour={"white": "always"},
        flesh_consistency={"soft": "always"},
        odour={"none": "usually"},
        habitat={"broadleaf_wood": "usually", "mixed_wood": "usually"},
        growth_habit={"single": "usually", "few": "usually"}),

    "amanita_persicina": dict(bruising={"no": "usually"}),

    # Mistaken for chanterelles, and it grows on wood where they grow from soil.
    "omphalotus_illudens": dict(
        stipe_base={"tapering": "usually", "equal": "sometimes"},
        odour={"none": "usually"},
        habitat={"broadleaf_wood": "always", "mixed_wood": "usually"}),

    "armillaria_mellea": dict(
        flesh_consistency={"fibrous": "usually", "soft": "usually"},
        habitat={"broadleaf_wood": "always", "mixed_wood": "usually"}),

    "leucoagaricus_americanus": dict(
        flesh_colour={"white": "always"},
        flesh_consistency={"soft": "always"},
        odour={"none": "usually"},
        habitat={"disturbed": "usually", "garden": "usually", "urban": "usually"},
        growth_habit={"caespitose": "usually", "few": "usually"}),

    "scleroderma_polyrhizum": dict(
        odour={"unpleasant_other": "usually"},
        bruising={"yes": "usually"}),

    "apioperdon_pyriforme": dict(bruising={"no": "usually"}),

    "laetiporus_sulphureus": dict(
        flesh_consistency={"soft": "usually", "fibrous": "usually"},
        odour={"none": "usually"},
        habitat={"broadleaf_wood": "always", "mixed_wood": "usually"}),

    "grifola_frondosa": dict(
        flesh_consistency={"fibrous": "usually", "soft": "usually"},
        habitat={"broadleaf_wood": "always", "mixed_wood": "usually"}),

    # Central-stemmed, so the base is a real character they were simply missing.
    "russula_virescens": dict(stipe_base={"equal": "always"}),
    "lactarius_indigo": dict(stipe_base={"equal": "usually", "tapering": "sometimes"}),
    "lactifluus_volemus": dict(stipe_base={"equal": "usually", "tapering": "sometimes"}),
    "suillus_spraguei": dict(stipe_flesh={"solid": "usually", "stuffed": "sometimes"}),

    "cantharellus_cinnabarinus": dict(
        flesh_consistency={"fibrous": "usually", "soft": "usually"},
        associated_tree={"broadleaf": "usually", "unknown": "sometimes"},
        growth_habit={"few": "usually", "troop": "usually"}),

    "gloeophyllum_sepiarium": dict(bruising={"no": "usually"}),
    "trametes_conchifer": dict(bruising={"no": "usually"}),
    "trametes_gibbosa": dict(bruising={"no": "usually"}),
    "coprinus_comatus": dict(flesh_colour={"white": "always"}),

    # Central-stemmed and ringless, which is worth saying rather than leaving blank:
    # "no ring" is an observation a reader makes and these two could not receive it.
    "lactarius_indigo": dict(ring={"absent": "always"}),
    "lactifluus_volemus": dict(ring={"absent": "always"}),

    # Stemless shelves. Their missing cap_shape is left missing on purpose — they are
    # fan-shaped brackets and the schema's cap shapes are all shapes of a round cap on a
    # stem, so any of them would be a worse answer than silence.
    "phyllotopsis_nidulans": dict(
        flesh_colour={"orange": "usually", "cream": "sometimes"},
        bruising={"no": "usually"}),
    "trametes_betulina": dict(
        flesh_colour={"white": "usually", "cream": "usually"},
        bruising={"no": "usually"}),
}


# Characters rewritten rather than filled, where what was recorded was true but did not
# say the thing that separates the mushroom from its neighbour.
REWRITE = {
    # The two Hericium were written as the same fungus: identical on every character the
    # key asks, so answering one truthfully reached the other and the lion's mane could
    # not be arrived at at all. They are not hard to tell apart in the wood. This one is
    # an unbranched cushion of long spines on a *living* hardwood, usually high up on a
    # wound; H. coralloides is a branched, coral-like thing on a fallen dead log. The
    # wood was already recorded and the telling half of it was listed third, behind the
    # broadleaf both of them share.
    #
    # The branching itself still cannot be said — the schema has no character for the
    # gross form of a fungus that has no cap — so the substrate carries it.
    "hericium_erinaceus": dict(
        substrate_wood={"living": "usually", "broadleaf": "always", "dead": "sometimes"},
        flesh_colour={"white": "always"}),
    "hericium_coralloides": dict(
        substrate_wood={"dead": "always", "broadleaf": "always"}),
}


def main():
    with open(PACK, encoding="utf-8") as handle:
        pack = json.load(handle, object_pairs_hook=collections.OrderedDict)

    by_id = {t["id"]: t for t in pack["taxa"]}
    added = 0
    for taxon_id, characters in FILL.items():
        taxon = by_id.get(taxon_id)
        if taxon is None:
            print(f"  ? {taxon_id} is not in the pack")
            continue
        written = []
        for character_id, states in characters.items():
            if character_id in taxon["characters"]:
                continue  # Never overwrite what somebody already judged.
            taxon["characters"][character_id] = [
                collections.OrderedDict(value=value, frequency=frequency)
                for value, frequency in states.items()
            ]
            written.append(character_id)
            added += 1
        if written:
            print(f"  + {taxon['scientificName']:32} {', '.join(written)}")

    rewritten = 0
    for taxon_id, characters in REWRITE.items():
        taxon = by_id.get(taxon_id)
        if taxon is None:
            continue
        for character_id, states in characters.items():
            taxon["characters"][character_id] = [
                collections.OrderedDict(value=value, frequency=frequency)
                for value, frequency in states.items()
            ]
            rewritten += 1
        print(f"  ~ {taxon['scientificName']:32} {', '.join(characters)}")

    with open(PACK, "w", encoding="utf-8") as handle:
        json.dump(pack, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print(f"\n{added} characters filled in, {rewritten} rewritten.")


if __name__ == "__main__":
    main()

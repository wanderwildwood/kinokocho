#!/usr/bin/env python3
"""
Writes the confusions the pack was missing.

An audit of eighty-six taxa found fifty-seven with no lookalikes at all, and nine
dangerous ones that nothing in the pack was confused with — including *Hygrophoropsis
aurantiaca*, the false chanterelle, while all four chanterelles also had an empty list.
That is the one confusion a chanterelle hunter in these mountains actually makes.

It matters in two places. The candidate page's "Confused with" section is the thing a
reader checks last and trusts most, and it was empty on two thirds of the pack. And the
key asks a settling question ahead of its information gain only when it has a
hand-declared discriminator to work from; with none, it falls back to guessing from the
data, which is weaker precisely where it should be strongest.

Each entry names what tells the two apart, in characters the schema actually has, so the
key can use them and the page can print them. The note says how, in the words a person
would use standing over it.

Idempotent: a pair already present in either direction is left alone.

    python3 tools/add-lookalikes.py
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")

# (from, to, [discriminators], note)
PAIRS = [
    # The chanterelle's two real problems, and it had neither.
    ("cantharellus_lateritius", "hygrophoropsis_aurantiaca",
     ["gill_extras", "gill_spacing", "substrate", "flesh_consistency"],
     "The false chanterelle has true gills: thin, crowded, repeatedly forked, and they "
     "break away from the flesh if you push them. A chanterelle has blunt ridges that "
     "will not come off, and it comes up from soil rather than from wood or conifer "
     "litter."),
    ("cantharellus_cinnabarinus", "hygrophoropsis_aurantiaca",
     ["gill_extras", "gill_spacing", "substrate", "flesh_consistency"],
     "Thin crowded gills that break away, on wood or conifer litter, against blunt "
     "ridges that will not come off, from soil."),
    ("cantharellus_appalachiensis", "hygrophoropsis_aurantiaca",
     ["gill_extras", "gill_spacing", "substrate", "flesh_consistency"],
     "Thin crowded gills that break away, on wood or conifer litter, against blunt "
     "ridges that will not come off, from soil."),
    ("cantharellus_lateritius", "omphalotus_illudens",
     ["gill_attachment", "gill_spacing", "growth_habit", "substrate"],
     "The jack-o'-lantern comes up in tight clusters from wood or buried roots, and its "
     "gills are true, crowded and sharp-edged. Chanterelles stand singly or a few "
     "together, out of soil."),
    ("cantharellus_cinnabarinus", "omphalotus_illudens",
     ["gill_attachment", "growth_habit", "substrate"],
     "Clustered on wood with true crowded gills, against a few together from soil with "
     "blunt ridges."),

    # The oyster's neighbours on the same log.
    ("pleurotus_ostreatus", "panellus_stipticus",
     ["size", "taste", "gill_spacing", "stipe_presence"],
     "The bitter oyster is small — thumbnail to a few centimetres — with a tough, "
     "cinnamon cap and a fiercely bitter taste. An oyster mushroom is broad, pale and "
     "mild."),
    ("pleurotus_ostreatus", "lentinellus_ursinus",
     ["gill_edge", "taste", "cap_surface"],
     "Bear lentinellus has ragged, saw-toothed gill edges and an acrid taste; the cap is "
     "hairy toward the base. An oyster's gill edges are even."),
    ("pleurotus_ostreatus", "omphalotus_illudens",
     ["growth_habit", "gill_attachment", "cap_colour", "stipe_presence"],
     "The jack-o'-lantern is orange throughout and grows in dense clusters with a "
     "definite stem, its gills running down it. An oyster is pale and grows shelved "
     "off the wood with little or no stem."),

    # Honey mushroom country: three things on the same stump.
    ("armillaria_mellea", "pholiota_squarrosa",
     ["cap_surface", "spore_print", "hymenophore_colour"],
     "The shaggy scalycap is covered in coarse recurved scales on both cap and stem, "
     "and drops a brown print. The honey mushroom drops white."),
    ("armillaria_mellea", "cortinarius_armillatus",
     ["ring", "spore_print", "stipe_surface"],
     "The red-banded webcap has rusty bands around the stem rather than a membranous "
     "ring, a cobwebby veil when young, and a rust-brown print."),
    ("mycena_galericulata", "galerina_marginata",
     ["spore_print", "ring", "stipe_surface"],
     "Galerina has a ring or a ring zone and a rust-brown print. The common bonnet has "
     "neither ring nor coloured print. Getting this one wrong is fatal, so take the "
     "print before deciding."),
    ("gymnopus_dryophilus", "galerina_marginata",
     ["spore_print", "ring", "substrate"],
     "Both are small, tan and on or near wood. Galerina has a ring zone and a rust-brown "
     "print; russet toughshank has no ring and a white one."),

    # Amanita among Amanitas.
    ("amanita_rubescens", "amanita_multisquamosa",
     ["bruising", "bruising_colour", "stipe_base", "cap_colour"],
     "The blusher stains dull wine-red where it is bruised or eaten by slugs, and the "
     "flesh reddens on cutting. The panther group does not redden and is dangerously "
     "more toxic."),
    ("amanita_citrina", "amanita_phalloides",
     ["odour", "cap_colour", "stipe_base"],
     "The false death cap smells strongly of raw potato and has a rimmed round bulb. The "
     "death cap has a sac-like volva and no potato smell. Do not settle this on colour."),
    ("tricholoma_sejunctum", "amanita_phalloides",
     ["stipe_base", "ring", "spore_print"],
     "Both can be greenish with white gills. The death cap has a sac at the base and a "
     "skirt on the stem; the deceiving knight has neither. Dig the base out."),
    ("russula_virescens", "amanita_phalloides",
     ["stipe_base", "ring", "flesh_consistency"],
     "Any green-capped, white-gilled mushroom deserves the base dug out. Russula flesh is "
     "brittle and snaps like chalk, and there is no ring and no sac."),
    ("amanita_flavoconia", "amanita_muscaria_guessowii",
     ["cap_colour", "veil_remnants", "stipe_base"],
     "Both carry yellow warts. Flavoconia is smaller with a yellow-orange cap and a "
     "ragged basal collar; the fly agaric has a bulb ringed with concentric scales."),

    # The button that is not a puffball. This is the lethal one that looks like nothing.
    ("lycoperdon_perlatum", "amanita_bisporigera",
     ["flesh_colour", "flesh_consistency", "stipe_base"],
     "An Amanita in its egg stage is a white ball in the soil. Cut every puffball from "
     "top to bottom before anything else: a puffball is uniform white paste throughout, "
     "an Amanita egg shows the outline of a cap, gills and stem inside."),
    ("calvatia_craniiformis", "amanita_bisporigera",
     ["flesh_colour", "flesh_consistency", "stipe_base"],
     "Cut it in half from top to bottom. Uniform white throughout is a puffball; the "
     "silhouette of a mushroom inside it is a deadly Amanita that has not opened yet."),

    # Boletes.
    ("boletus_edulis", "neoboletus_subvelutipes",
     ["bruising", "bruising_colour", "hymenophore_colour", "bruising_speed"],
     "The red-mouth bolete has red pore mouths and turns blue-black almost the instant it "
     "is cut. Porcini has whitish to olive pores and does not stain blue."),
    ("boletus_edulis", "xerocomellus_chrysenteron",
     ["cap_surface", "stipe_surface", "size", "bruising"],
     "The red cracking bolete is small, its cap cracks to show red flesh in the fissures, "
     "and it bruises blue. Porcini is heavy, uncracked and unstaining, with a netted "
     "stem."),
    ("suillus_americanus", "boletus_edulis",
     ["cap_surface", "associated_tree", "ring", "size"],
     "Chicken-fat suillus is small, slimy, yellow and tied to white pine, often with a "
     "ring zone. Porcini is dry, brown and heavy."),

    # Small brown things in grass.
    ("marasmius_oreades", "inocybe_rimosa",
     ["spore_print", "odour", "cap_shape", "cap_surface"],
     "Fibrecaps have a radially split, silky-fibrous cap, an umbo, a dull earthy or "
     "spermatic smell, and a dull brown print. The fairy ring champignon is smooth, "
     "smells faintly of almonds and prints white. Fibrecaps are muscarine poisonings."),
    ("clitocybe_gibba", "paxillus_involutus",
     ["cap_margin", "bruising", "bruising_colour", "spore_print"],
     "The brown roll-rim has a margin rolled tightly under, gills that bruise rust-brown "
     "to the touch and lift away from the cap, and a brown print. The common funnel does "
     "none of that."),

    # Cups, jellies, crusts, brackets.
    ("sarcoscypha_dudleyi", "peziza_badia",
     ["cap_colour", "substrate", "flesh_consistency"],
     "The scarlet elf cup is bright red inside and sits on wet fallen wood in late "
     "winter. The bay cup is brown, on soil, and later in the year."),
    ("auricularia_americana", "tremella_mesenterica",
     ["fruitbody_type", "cap_shape", "cap_colour"],
     "Witch's butter is a bright yellow, folded, brain-like blob. Wood ear is brown, "
     "ear-shaped and thinner, with a distinct upper and lower surface."),
    ("daldinia_childiae", "hypoxylon_fragiforme",
     ["substrate_wood", "size", "flesh_consistency"],
     "Cut it: carbon balls are ringed inside in concentric silvery bands. Beech woodwart "
     "is smaller, pinkish when young, and on beech."),
    ("trametes_hirsuta", "stereum_ostrea",
     ["fruitbody_type", "hymenophore_colour"],
     "Look underneath. Stereum has a smooth underside with no pores at all."),
    ("grifola_frondosa", "bondarzewia_berkeleyi",
     ["size", "flesh_consistency", "hymenophore_colour"],
     "Berkeley's polypore is much larger, with thick, tough, cream-coloured fronds and "
     "coarse pores. Hen of the woods is a mass of small grey-brown spoon-shaped caps."),

    # Milkcaps, which the latex settles.
    ("lactarius_deliciosus", "lactifluus_volemus",
     ["latex_colour", "latex_change", "odour"],
     "Saffron milkcap bleeds carrot-orange latex that greens where it dries. The weeping "
     "milkcap bleeds copious white latex, stains brown, and smells strongly of fish."),
    ("lactarius_indigo", "lactarius_deliciosus",
     ["latex_colour", "cap_colour"],
     "The indigo milkcap is blue throughout and bleeds blue latex. There is nothing else "
     "it can be confused with once it is cut."),

    # A big white acrid milkcap against a big pale mild one.
    ("lactarius_subvellereus", "lactifluus_hygrophoroides",
     ["gill_spacing", "taste", "cap_colour"],
     "Both are pale milkcaps of much the same size. The hygrophorus milkcap has gills "
     "you can count at arm's length and mild milk; this one has crowded gills and milk "
     "that burns the tongue."),

    # Chanterelle-adjacent, and the two morels.
    ("hydnum_repandum", "cantharellus_lateritius",
     ["fruitbody_type", "gill_attachment"],
     "Look underneath: the hedgehog has soft spines that rub off, the chanterelle has "
     "blunt ridges running down the stem."),
    ("morchella_angusticeps", "morchella_americana",
     ["cap_colour", "habitat", "associated_tree"],
     "Both are hollow from tip to base. The black morel has dark ridges that darken "
     "further with age and comes earlier, often in mixed or burnt woodland; the yellow "
     "morel is paler with ridges no darker than its pits."),
]


def main():
    pack = json.load(open(PACK))
    by = {t["id"]: t for t in pack["taxa"]}
    known = set()
    for t in pack["taxa"]:
        for look in t.get("lookalikes", []):
            known.add((t["id"], look["taxon"]))

    added, skipped = 0, []
    for src, dst, discriminators, note in PAIRS:
        assert src in by, src
        assert dst in by, dst
        if (src, dst) in known or (dst, src) in known:
            skipped.append(f"{src}->{dst}")
            continue
        by[src].setdefault("lookalikes", []).append({
            "taxon": dst,
            "discriminators": discriminators,
            "note": note,
        })
        known.add((src, dst))
        added += 1

    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")

    total = sum(len(t.get("lookalikes", [])) for t in pack["taxa"])
    linked = {t["id"] for t in pack["taxa"] if t.get("lookalikes")}
    for t in pack["taxa"]:
        for look in t.get("lookalikes", []):
            linked.add(look["taxon"])
    print(f"{added} added, {len(skipped)} already there")
    print(f"{total} confusions over {len(pack['taxa'])} taxa; "
          f"{len(linked)} taxa now appear in at least one")


if __name__ == "__main__":
    main()

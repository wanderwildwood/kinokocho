#!/usr/bin/env python3
"""
Marks each taxon with how often it is actually met, and whether people go looking for it.

Two facts the pack did not carry, both needed by the month view. Without prevalence that
page listed every one of eighty-six rows that had ever been recorded in the month, which
is a calendar of everything and therefore of nothing. Without `sought` it could only warn
- so the one section a forager would read named nothing but poisons.

**`sought` is not an edibility field and must never become one.** It says people go
looking for this one. It is not on the taxon because the key needs it - the key never
reads it - and nothing anywhere in this app says a mushroom is safe to eat. The reason
it is worth having is that a page which can only ever warn is a page people stop
reading, and the destroying angel is on the same list as the chanterelle precisely
because that is the list somebody will actually open in September.

Ratings are for southern Appalachia and are a walker's judgement, not a survey.
Idempotent: run it again after adding taxa and it fills in what is missing.

    python3 tools/add-prevalence.py
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")

# Met on most walks in its season, in the right habitat.
COMMON = {
    "agaricus_campestris", "amanita_bisporigera", "amanita_flavoconia",
    "amanita_muscaria_guessowii", "amanita_rubescens", "amanita_vaginata",
    "armillaria_mellea", "desarmillaria_tabescens", "auricularia_americana",
    "cantharellus_appalachiensis", "cantharellus_cinnabarinus",
    "cantharellus_lateritius", "chlorophyllum_molybdites", "coprinus_comatus",
    "craterellus_fallax", "daldinia_childiae", "galerina_marginata",
    "ganoderma_applanatum", "grifola_frondosa", "gymnopus_dryophilus",
    "hypholoma_fasciculare", "hypoxylon_fragiforme", "laetiporus_sulphureus",
    "lycoperdon_perlatum", "marasmius_oreades", "meripilus_sumstinei",
    "mycena_galericulata", "omphalotus_illudens", "panellus_stipticus",
    "pholiota_squarrosa", "pleurotus_ostreatus", "pluteus_cervinus",
    "russula_virescens", "scleroderma_citrinum", "stereum_ostrea",
    "trametes_hirsuta", "trametes_versicolor", "tremella_mesenterica",
    "tylopilus_felleus", "xerocomellus_chrysenteron",
    # Sixth batch: the things on every fallen hardwood here.
    "xylaria_polymorpha", "schizophyllum_commune", "trichaptum_biforme",
    "coprinellus_micaceus", "amanita_brunnescens", "baorangia_bicolor",
    "marasmius_rotula", "clavulina_coralloides", "chlorociboria_aeruginascens",
    "candolleomyces_candolleanus",
    # Seventh batch: the two genera you cannot walk past in July.
    "russula_compacta", "russula_variata", "russula_silvicola", "russula_mariae",
    "lactarius_corrugis", "lactifluus_hygrophoroides", "lactifluus_subvellereus",
    "cantharellus_minor",
}

# Found regularly, but you go looking rather than tripping over it.
OCCASIONAL = {
    "amanita_citrina", "amanita_jacksonii", "amanita_multisquamosa",
    "artomyces_pyxidatus", "boletus_edulis", "bondarzewia_berkeleyi",
    "calvatia_craniiformis", "cerioporus_squamosus", "infundibulicybe_gibba",
    "collybia_nuda", "cortinarius_alboviolaceus", "cystoderma_amianthinum",
    "entoloma_abortivum", "hericium_erinaceus", "hydnum_repandum",
    "hygrocybe_conica", "hygrophoropsis_aurantiaca", "hymenopellis_radicata",
    "hypomyces_lactifluorum", "pseudosperma_rimosum", "lactarius_indigo",
    "lactifluus_volemus", "laetiporus_cincinnatus", "leucoagaricus_americanus",
    "morchella_americana", "panaeolus_papilionaceus", "legaliana_badia",
    "phallus_ravenelii", "pluteus_petasatus", "ramaria_stricta",
    "sarcoscypha_dudleyi", "neoboletus_subvelutipes", "suillus_americanus",
    "tricholoma_sejunctum",
    "strobilomyces_strobilaceus", "cortinarius_iodes", "ganoderma_tsugae",
    "hericium_coralloides", "tylopilus_plumbeoviolaceus", "gymnopilus_luteofolius",
}

# People go out to look for these. NOT a claim that any of them is safe to eat.
SOUGHT = {
    "agaricus_campestris", "amanita_jacksonii", "armillaria_mellea",
    "auricularia_americana", "boletus_edulis", "bondarzewia_berkeleyi",
    "calvatia_craniiformis", "cantharellus_appalachiensis",
    "cantharellus_cinnabarinus", "cantharellus_lateritius",
    "cerioporus_squamosus", "collybia_nuda", "coprinus_comatus",
    "craterellus_fallax", "entoloma_abortivum", "grifola_frondosa",
    "hericium_erinaceus", "hydnum_repandum", "hypomyces_lactifluorum",
    "lactarius_deliciosus", "lactarius_indigo", "lactifluus_volemus",
    "laetiporus_cincinnatus", "laetiporus_sulphureus", "lycoperdon_perlatum",
    "marasmius_oreades", "morchella_americana", "morchella_angusticeps",
    "pleurotus_ostreatus", "russula_virescens", "trametes_versicolor",
    "coprinellus_micaceus", "strobilomyces_strobilaceus", "baorangia_bicolor",
    "ganoderma_tsugae", "hericium_coralloides", "russula_variata",
    "lactarius_corrugis", "lactifluus_hygrophoroides", "cantharellus_minor",
}


def main():
    pack = json.load(open(PACK))
    changed = 0
    unrated = []
    for taxon in pack["taxa"]:
        tid = taxon["id"]
        if tid in COMMON:
            prevalence = "common"
        elif tid in OCCASIONAL:
            prevalence = "occasional"
        else:
            prevalence = "uncommon"
            if tid not in SOUGHT:
                unrated.append(tid)
        if taxon.get("prevalence") != prevalence:
            taxon["prevalence"] = prevalence
            changed += 1
        sought = tid in SOUGHT
        if taxon.get("sought") != sought:
            taxon["sought"] = sought
            changed += 1

    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"{changed} fields written over {len(pack['taxa'])} taxa")
    print(f"  common {sum(1 for t in pack['taxa'] if t['prevalence'] == 'common')}, "
          f"occasional {sum(1 for t in pack['taxa'] if t['prevalence'] == 'occasional')}, "
          f"uncommon {sum(1 for t in pack['taxa'] if t['prevalence'] == 'uncommon')}")
    print(f"  sought {sum(1 for t in pack['taxa'] if t['sought'])}")
    if unrated:
        print("  fell through to uncommon, check these are meant to be: "
              + ", ".join(unrated))


if __name__ == "__main__":
    main()

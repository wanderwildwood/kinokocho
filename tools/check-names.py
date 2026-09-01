#!/usr/bin/env python3
"""Check every name in a region pack against outside taxonomic authorities.

The pack is the one file in this repository that a language model wrote, and the thing
it is most likely to get wrong is not a shape a test can see. PackIntegrityTest already
asserts that names are well-formed binomials, that an id and its name agree, that
lookalikes resolve. All of that passes for a name that is well-formed, consistent, and
does not exist.

That is not hypothetical. This found "Suillellus subvelutipes" in the pack: a real
species under a genus it was never combined with. GBIF matched only the genus and
iNaturalist had nothing at all, while every test in the suite was green. It is
Neoboletus subvelutipes, and the porcini's own page warned a reader about a mushroom
they could not have looked up.

Not a unit test, deliberately. It needs the network, it depends on other people's
databases changing under it, and a suite that fails because somebody else published a
revision is a suite people learn to ignore. Run it when the pack changes.

    python3 tools/check-names.py

Exit status is 1 if any name resolves nowhere, which is the case worth acting on.
Synonyms are reported and are not failures: fungal taxonomy is genuinely unsettled, the
field guides are behind the databases, and which name to print is a judgment about
readers rather than a fact to be looked up.
"""

import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

PACK = Path(__file__).resolve().parent.parent / (
    "app/src/main/assets/packs/southern-appalachia-v1.json"
)
UA = "kinokocho-name-check (https://github.com/wanderwildwood/kinokocho)"
PLACE = 30  # North Carolina, which is what this pack is for.

# Below this many records here, a name is worth a second look — but only alongside the
# sibling test in displaced_by_sibling, never on its own.
PRESENT = 3

# ...and a congener needs at least this many before its abundance means anything.
SIBLING = 200


def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def gbif(name):
    """ACCEPTED, a synonym's target, or None when the binomial does not exist.

    Two lookups, because one is not enough. `species/match` in strict mode answers "is
    this a name in GBIF's backbone", and it says NONE for perfectly real combinations
    the backbone has not adopted — it did so for Neoboletus subvelutipes and Collybia
    nuda, which are what iNaturalist calls those two mushrooms. A checker that reports
    those as fictional is a checker somebody stops reading, and then it is worth less
    than nothing, because the one real case is buried among its false alarms.

    So a strict miss falls through to the name index, which knows published names the
    backbone has not ranked.
    """
    query = urllib.parse.urlencode({"name": name, "kingdom": "Fungi", "strict": "true"})
    found = get(f"https://api.gbif.org/v1/species/match?{query}")
    if found.get("matchType") != "NONE":
        return found.get("status"), found.get("species") or found.get("scientificName")

    search = urllib.parse.urlencode({"q": name, "rank": "SPECIES", "limit": 20})
    for result in get(f"https://api.gbif.org/v1/species/search?{search}").get("results", []):
        published = (result.get("species") or result.get("scientificName") or "")
        if published.lower().startswith(name.lower()) and result.get("kingdom") == "Fungi":
            return result.get("taxonomicStatus"), None
    return None, None


def displaced_by_sibling(name, common):
    """Is a mushroom in the same genus far commoner here, under a different epithet?

    Absence on its own says very little. Plenty of real, correctly named species have no
    records in one state — Amanita phalloides has none here and is in this pack on
    purpose, as a warning about something that would matter enormously if it turned up.
    Reporting every one of those buries the case that matters in a list nobody reads.

    The case that matters has a shape: the pack's name has no records here *and* a
    congener has thousands. That is what a species split looks like from the outside.
    Desarmillaria tabescens has none in this state and Desarmillaria caespitosa has three
    and a half thousand, because the ringless honey mushrooms growing here were separated
    off under the second name. Following a synonym chain gives the current name for a
    taxon; it does not tell you the taxon is the one that grows where the pack is about.
    """
    wanted = name.replace(" var. ", " ")
    found = get(
        "https://api.inaturalist.org/v1/taxa?"
        + urllib.parse.urlencode({"q": wanted, "per_page": 5})
    ).get("results", [])
    exact = [t for t in found if t.get("name", "").lower() == wanted.lower()]
    if not exact:
        return None
    mine = inat_records_here(exact[0]["id"])
    if mine >= PRESENT:
        return None

    genus = wanted.split()[0]
    time.sleep(1.1)
    genus_hit = get(
        "https://api.inaturalist.org/v1/taxa?"
        + urllib.parse.urlencode({"q": genus, "rank": "genus", "per_page": 5})
    ).get("results", [])
    genus_hit = [t for t in genus_hit if t.get("name") == genus]
    if not genus_hit:
        return None
    time.sleep(1.1)
    counts = get(
        "https://api.inaturalist.org/v1/observations/species_counts?"
        + urllib.parse.urlencode(
            {"taxon_id": genus_hit[0]["id"], "place_id": PLACE,
             "quality_grade": "research", "per_page": 5}
        )
    ).get("results", [])
    # And the sibling has to be the *same mushroom* under another name, not merely
    # another member of a large genus. Amanita parcivolvata being everywhere says nothing
    # about Amanita citrina; they are two species and both belong here. What says
    # something is a congener carrying the same common name, which is what a population
    # separated off under a new epithet looks like: Desarmillaria caespitosa and
    # D. tabescens are both the ringless honey mushroom, and only one of them grows here.
    ours = (common or "").strip().lower()
    if not ours:
        return None
    for row in counts:
        sibling, seen = row["taxon"]["name"], row["count"]
        theirs = (row["taxon"].get("preferred_common_name") or "").strip().lower()
        if sibling.lower() != wanted.lower() and seen >= SIBLING and theirs == ours:
            return mine, sibling, seen
    return None


def inat_records_here(taxon_id):
    """Research-grade observations of this taxon in the pack's own region."""
    time.sleep(1.1)
    query = urllib.parse.urlencode(
        {"taxon_id": taxon_id, "place_id": PLACE, "quality_grade": "research", "per_page": 0}
    )
    return get(f"https://api.inaturalist.org/v1/observations?{query}").get("total_results", 0)


def inat(name):
    """What iNaturalist calls it, which is what an identifier there will recognise."""
    # Varieties are written "Amanita muscaria guessowii" there, without the "var.".
    query = urllib.parse.urlencode(
        {"q": name.replace(" var. ", " "), "per_page": 5}
    )
    time.sleep(1.1)  # Their published rate limit is about one request a second.
    results = get(f"https://api.inaturalist.org/v1/taxa?{query}").get("results", [])
    wanted = name.replace(" var. ", " ").lower()
    if any(r.get("name", "").lower() == wanted for r in results):
        return None
    return results[0]["name"] if results else "(nothing)"


def main():
    taxa = json.loads(PACK.read_text())["taxa"]
    nowhere, synonyms, elsewhere, absentees = [], [], [], []

    for taxon in taxa:
        name = taxon["scientificName"]
        try:
            status, accepted = gbif(name)
        except Exception as exc:  # noqa: BLE001 - a lookup failing is not a bad name
            print(f"  ? {name}: could not ask GBIF ({exc})")
            continue

        try:
            other = inat(name)
        except Exception:  # noqa: BLE001
            other = None

        # "Exists nowhere" means both authorities have never heard of it, not that one
        # of them prefers another name. iNaturalist carrying it as an active taxon is
        # proof enough that somebody published it — and iNaturalist is where a reader
        # of this app will go looking.
        if status is None and other is not None:
            nowhere.append(name)
        elif status == "SYNONYM" and accepted:
            synonyms.append((name, accepted))
        if other:
            elsewhere.append((name, other))
        else:
            try:
                displaced = displaced_by_sibling(name, taxon.get("commonName"))
            except Exception:  # noqa: BLE001
                displaced = None
            if displaced:
                absentees.append((name, *displaced))

    print(f"\n{len(taxa)} names checked.\n")

    if absentees:
        print("Possibly the wrong species for this region. Each of these has almost no")
        print("records here while a mushroom in the same genus has a great many, which")
        print("is what a species split looks like from outside — the pack may be naming")
        print("the population that grows somewhere else.")
        for name, mine, sibling, seen in absentees:
            print(f"    {name:<34} {mine:>5} here")
            print(f"    {'':<34} {seen:>5} for {sibling}")
        print()

    if nowhere:
        print("NOT A PUBLISHED NAME — GBIF cannot match the binomial at all:")
        for name in nowhere:
            print(f"    {name}")
        print()

    if synonyms:
        print("Synonyms. Real names; GBIF prefers another. A judgment, not a fault:")
        for name, accepted in synonyms:
            print(f"    {name:<38} GBIF: {accepted}")
        print()

    if elsewhere:
        print("Not iNaturalist's name for it, so somebody looking it up there will")
        print("have to guess:")
        for name, other in elsewhere:
            print(f"    {name:<38} iNat: {other}")
        print()

    if not (nowhere or synonyms or elsewhere):
        print("Every name resolves, and matches both authorities.")

    return 1 if nowhere else 0


if __name__ == "__main__":
    sys.exit(main())

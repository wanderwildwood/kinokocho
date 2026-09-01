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


def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def gbif(name):
    """ACCEPTED, a synonym's target, or None when the binomial does not exist."""
    query = urllib.parse.urlencode({"name": name, "kingdom": "Fungi", "strict": "true"})
    found = get(f"https://api.gbif.org/v1/species/match?{query}")
    if found.get("matchType") == "NONE":
        return None, None
    return found.get("status"), found.get("species") or found.get("scientificName")


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
    nowhere, synonyms, elsewhere = [], [], []

    for taxon in taxa:
        name = taxon["scientificName"]
        try:
            status, accepted = gbif(name)
        except Exception as exc:  # noqa: BLE001 - a lookup failing is not a bad name
            print(f"  ? {name}: could not ask GBIF ({exc})")
            continue

        if status is None:
            nowhere.append(name)
        elif status == "SYNONYM":
            synonyms.append((name, accepted))

        try:
            other = inat(name)
        except Exception:  # noqa: BLE001
            other = None
        if other:
            elsewhere.append((name, other))

    print(f"\n{len(taxa)} names checked.\n")

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

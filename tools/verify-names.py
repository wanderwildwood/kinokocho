#!/usr/bin/env python3
"""Record, for every name in the pack, the outside authority that says it is real.

This exists because of the one failure mode that would matter more than any other. The
pack is the file a language model wrote, and a language model's characteristic mistake is
not a typo — it is a confident, well-formed, entirely plausible species that does not
exist. `Suillellus subvelutipes` sat in here for weeks: correct Latin, real genus, real
epithet, a combination nobody ever published. Every test in the suite was green, because
every test asked whether the name had the right *shape*.

`check-names.py` will find that, and only if somebody remembers to run it. Remembering is
not a control. So this writes down what it found, and `PackIntegrityTest` refuses any
taxon that is not in the file it writes — which means a name that has never been checked
against GBIF or iNaturalist cannot reach the app at all, whether or not anybody thought
to run anything.

The test that enforces it is offline and always runs. The network lives here, where a
flaky lookup delays a change rather than breaking a build.

    python3 tools/verify-names.py          # check every name, rewrite the record
    python3 tools/verify-names.py --new    # only names not already recorded

A name is recorded only when the Catalogue of Life matches it at species rank, GBIF's
backbone accepts it, Index Fungorum has it published, or iNaturalist carries it as an
active taxon. Any of those is somebody outside this repository saying the mushroom
exists. None of them is this repository agreeing with itself.

The Catalogue of Life is asked first because GBIF has stopped building its backbone: it
is kept for backwards compatibility and its identifiers still resolve, but it no longer
takes in new names, and GBIF now points at COL's eXtended release instead. A frozen
authority fails in the one direction that matters here — a genuinely new combination
would come back unknown and read exactly like a fabricated one.

Where COL says a name is a synonym the accepted name is written down beside it. That is
a note, not an instruction: which name to print is a judgment about readers, and the
field guides are behind the databases on purpose.
"""

import json
import sys
import time
import urllib.parse
import urllib.request
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "app/src/main/assets/packs/southern-appalachia-v1.json"
RECORD = ROOT / "app/src/test/resources/verified-names.json"
UA = "kinokocho-name-verify (https://github.com/wanderwildwood/kinokocho)"


def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


COL = "3LXR"  # Catalogue of Life, latest eXtended release — the dataset GBIF now defers to


def col(name):
    """The Catalogue of Life, matched at species rank.

    A match that lands on a higher rank is the failure this whole file exists for. It is
    what a fabricated binomial does: `Suillellus subvelutipes` matches the genus
    Suillellus perfectly well and stops there.
    """
    # Asking for species rank on `Amanita muscaria var. guessowii` throws away the only
    # answer there is: COL has it, as a variety. Infraspecific names are matched without
    # the filter and accepted at whatever rank below genus they land on.
    below_genus = {"species", "subspecies", "variety", "form", "subvariety", "subform"}
    infraspecific = any(marker in name for marker in (" var. ", " subsp. ", " f. "))
    fields = {"q": name} if infraspecific else {"q": name, "rank": "species"}
    found = get(f"https://api.checklistbank.org/dataset/{COL}/match/nameusage?"
                + urllib.parse.urlencode(fields))
    usage = found.get("usage") or {}
    if usage.get("rank") not in below_genus or not usage.get("id"):
        return None
    row = {"id": usage["id"]}
    if usage.get("status") == "synonym":
        # For a synonym COL returns the accepted name at the head of the classification.
        accepted = (usage.get("classification") or [{}])[0].get("name")
        if accepted and accepted != name:
            row["synonymOf"] = accepted
    return row


def gbif_backbone(name):
    query = urllib.parse.urlencode({"name": name, "kingdom": "Fungi", "strict": "true"})
    found = get(f"https://api.gbif.org/v1/species/match?{query}")
    if found.get("matchType") == "NONE":
        return None
    return found.get("usageKey")


# Species Fungorum Plus on GBIF — the Index Fungorum data, and the only GBIF checklist
# asked here. See gbif_index below for why it is one dataset rather than all of them.
SPECIES_FUNGORUM = "bf3db7c9-5e5d-4fd0-bd5b-94539eaf9598"


def gbif_index(name):
    """Index Fungorum, via GBIF: published fungal names the backbone has not ranked.

    Restricted to one dataset on purpose. Searching every GBIF checklist accepted a hit
    from **NCBI Taxonomy**, which indexes whatever name a sequence was deposited under
    and registers nothing — it is not a nomenclator, and two names in this pack rested on
    it alone. Index Fungorum is one of the three repositories where a fungal name is
    actually published, so a hit here means somebody registered the name.
    """
    query = urllib.parse.urlencode({"q": name, "datasetKey": SPECIES_FUNGORUM, "limit": 20})
    for row in get(f"https://api.gbif.org/v1/species/search?{query}").get("results", []):
        published = row.get("species") or row.get("scientificName") or ""
        if published.lower().startswith(name.lower()):
            return row.get("key")
    return None


def inat(name):
    wanted = name.replace(" var. ", " ")
    query = urllib.parse.urlencode({"q": wanted, "per_page": 5})
    for row in get(f"https://api.inaturalist.org/v1/taxa?{query}").get("results", []):
        if row.get("name", "").lower() == wanted.lower():
            return row.get("id")
    return None


def verify(name):
    """Who says this mushroom exists, and under what identifier."""
    for authority, lookup in (("col", col),
                              ("gbif", gbif_backbone),
                              ("index-fungorum", gbif_index),
                              ("inaturalist", inat)):
        time.sleep(1.1)  # iNaturalist asks for about one request a second.
        try:
            key = lookup(name)
        except Exception:  # noqa: BLE001 - one authority being down is not a verdict
            continue
        if key:
            found = key if isinstance(key, dict) else {"id": key}
            row = {"authority": authority, "id": str(found["id"]), "checked": str(date.today())}
            if found.get("synonymOf"):
                row["synonymOf"] = found["synonymOf"]
            return row
    return None


def main():
    only_new = "--new" in sys.argv
    taxa = json.loads(PACK.read_text())["taxa"]
    RECORD.parent.mkdir(parents=True, exist_ok=True)
    known = json.loads(RECORD.read_text()) if RECORD.exists() else {}

    unverified = []
    for taxon in taxa:
        name = taxon["scientificName"]
        if only_new and name in known:
            continue
        found = verify(name)
        if found:
            known[name] = found
            print(f"  {name:<38} {found['authority']}:{found['id']}")
        else:
            unverified.append(name)
            known.pop(name, None)
            print(f"  {name:<38} NOT FOUND BY ANY AUTHORITY")

    # Only names still in the pack, so a removed taxon does not leave a stale blessing
    # behind that would let the name back in unchecked.
    current = {t["scientificName"] for t in taxa}
    known = {k: v for k, v in sorted(known.items()) if k in current}
    RECORD.write_text(json.dumps(known, indent=2, ensure_ascii=False) + "\n")

    print(f"\n{len(known)} of {len(taxa)} names verified.")
    if unverified:
        print("\nNOT VERIFIED — no authority of the four knows these as a species:")
        for name in unverified:
            print(f"    {name}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

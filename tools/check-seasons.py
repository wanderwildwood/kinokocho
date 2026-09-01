#!/usr/bin/env python3
"""Check the pack's seasons against where the mushrooms are actually recorded.

The months in the pack are not decoration. They decide what a month page shows, and —
because a confusion is only ever named where both mushrooms are in season — whether a
warning the pack holds can be shown at all. A season that is a month too narrow is a
warning nobody sees.

That is not hypothetical either. Meripilus sumstinei was listed as fruiting in July
alone, so the hen of the woods, which is picked in September and October, was never told
what it is confused with. Galerina marginata was listed December to March against a
honey mushroom in September and October, and its own note said "found year-round".

So this asks iNaturalist how the research-grade records for each taxon fall across the
year in North Carolina, and reports months the pack leaves out.

    python3 tools/check-seasons.py            # report
    python3 tools/check-seasons.py --write    # widen the well-evidenced ones

**It only ever widens.** A season that is too narrow hides a mushroom in a month it is
out, and takes any warning attached to it away. A season that is too wide shows one in a
month it may not be out, which is a thing the month page already says plainly of every
row on it — "seasons are a guide and nothing more; fungi do not read calendars". The two
errors are not symmetrical, and the evidence for narrowing is weak anyway: a month with
no records is usually a month with nobody walking.

Observation counts measure observers as much as mushrooms. That is why the test is a
month's share of *that taxon's own* records rather than a raw count, and why thin taxa
are left alone rather than reshaped from a handful of sightings.
"""

import json
import sys
import time
import urllib.parse
import urllib.request
from collections import OrderedDict
from pathlib import Path

PACK = Path(__file__).resolve().parent.parent / (
    "app/src/main/assets/packs/southern-appalachia-v1.json"
)
UA = "kinokocho-season-check (https://github.com/wanderwildwood/kinokocho)"
PLACE = 30  # North Carolina, which is what this pack is for.

# Enough of a taxon's own records to say anything about its season at all.
ENOUGH = 40
# A month belongs to the season if it holds this much of the taxon's records...
SHARE = 0.08
# ...and this many outright, so a share of a small number cannot carry it.
LEAST = 10
# Anything lethal is widened on far less, because the cost of being wrong runs one way.
LEAST_IF_LETHAL = 2


def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def records_by_month(name):
    """NC research-grade observations per month, or None if iNaturalist has no such taxon."""
    wanted = name.replace(" var. ", " ")
    time.sleep(1.1)  # Their published rate limit is about one request a second.
    found = get(
        "https://api.inaturalist.org/v1/taxa?"
        + urllib.parse.urlencode({"q": wanted, "per_page": 5})
    ).get("results", [])
    exact = [t for t in found if t.get("name", "").lower() == wanted.lower()]
    if not exact:
        return None
    time.sleep(1.1)
    histogram = get(
        "https://api.inaturalist.org/v1/observations/histogram?"
        + urllib.parse.urlencode(
            {
                "taxon_id": exact[0]["id"],
                "place_id": PLACE,
                "date_field": "observed",
                "interval": "month_of_year",
                "quality_grade": "research",
            }
        )
    )
    return {int(m): n for m, n in histogram["results"]["month_of_year"].items()}


def main():
    write = "--write" in sys.argv
    pack = json.loads(PACK.read_text(), object_pairs_hook=OrderedDict)

    widened, thin, absent = [], [], []
    for taxon in pack["taxa"]:
        name = taxon["scientificName"]
        listed = set((taxon.get("season") or {}).get("months") or [])
        if not listed:
            continue  # No months means every month; nothing to widen.

        try:
            counts = records_by_month(name)
        except Exception as exc:  # noqa: BLE001 - a lookup failing is not a bad season
            print(f"  ? {name}: {exc}")
            continue
        if counts is None:
            absent.append(name)
            continue

        total = sum(counts.values())
        lethal = (taxon.get("hazard") or {}).get("severity") == "LETHAL"
        floor = LEAST_IF_LETHAL if lethal else LEAST
        if total < ENOUGH and not lethal:
            thin.append((name, total))
            continue

        found_in = {
            month
            for month, n in counts.items()
            if n >= floor and (lethal or n >= total * SHARE)
        }
        gap = sorted(found_in - listed)
        if gap:
            widened.append((name, sorted(listed), gap, {m: counts[m] for m in gap}, lethal))
            if write:
                taxon["season"]["months"] = sorted(listed | set(gap))

    print(f"\n{len(pack['taxa'])} taxa checked against North Carolina records.\n")
    if widened:
        print("Months the pack leaves out, where the mushroom is demonstrably found:")
        for name, listed, gap, detail, lethal in sorted(
            widened, key=lambda w: -sum(w[3].values())
        ):
            mark = " (lethal)" if lethal else ""
            print(f"    {name}{mark}")
            print(f"        listed {listed}")
            print(f"        adds   {gap}   records there: {detail}")
        print()
    if thin:
        print(f"{len(thin)} taxa have too few records to judge a season from, and are")
        print("left alone rather than reshaped from a handful of sightings.\n")
    if absent:
        print("Not resolvable on iNaturalist, so not checked:")
        for name in absent:
            print(f"    {name}")
        print()

    if write and widened:
        PACK.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + "\n")
        print(f"Widened {len(widened)} seasons in the pack.")
    elif widened:
        print("Run again with --write to widen these.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

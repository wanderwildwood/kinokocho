#!/usr/bin/env python3
"""
Makes the citations say where the data actually came from.

Two faults, both of the kind a pack that prints "where this comes from" on every page
ought not to have.

The same source was written four ways — "GBIF occurrence counts over a southern Blue
Ridge polygon" beside "GBIF occurrence data, southern Blue Ridge polygon" — so one source
rendered as two lines on the page.

Worse, fifty-nine rows cited GBIF and nothing else. GBIF says where a fungus has been
recorded; it says nothing whatever about how the gills meet the stem. The morphology in
those rows came from the ordinary agreement of standard field guides, written out rather
than reproduced, and citing an occurrence database for it was pointing at the wrong
thing entirely.

    python3 tools/fix-sources.py
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")

CANONICAL = {
    "GBIF occurrence data, southern Blue Ridge polygon":
        "GBIF occurrence counts over a southern Blue Ridge polygon",
    "North American Mycological Association references":
        "North American Mycological Association poisoning references",
    "iNaturalist occurrence records for the southern Blue Ridge":
        "iNaturalist occurrence records, southern Blue Ridge",
    "iNaturalist occurrence records above 5,000 ft in the southern Blue Ridge":
        "iNaturalist occurrence records, southern Blue Ridge, above 5,000 ft",
}

# What the descriptions are actually made of. GBIF is where it grows; this is what it
# looks like, and they are not the same citation.
MORPHOLOGY = ("Macromorphology as standard North American field guides agree it — "
              "written out, not reproduced")

DISTRIBUTION_ONLY = ("GBIF", "iNaturalist")


def main():
    pack = json.load(open(PACK))
    renamed = 0
    added = 0
    for taxon in pack["taxa"]:
        sources = []
        for s in taxon["sources"]:
            canon = CANONICAL.get(s, s)
            if canon != s:
                renamed += 1
            if canon not in sources:
                sources.append(canon)
        # A row whose only citations are occurrence databases has not said where its
        # description came from.
        if all(any(s.startswith(d) for d in DISTRIBUTION_ONLY) for s in sources):
            sources.append(MORPHOLOGY)
            added += 1
        taxon["sources"] = sources

    with open(PACK, "w") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
        f.write("\n")

    every = sorted({s for t in pack["taxa"] for s in t["sources"]})
    print(f"{renamed} citations normalised, {added} rows told where their description came from")
    print(f"{len(every)} distinct sources now:")
    for s in every:
        print(f"    {s}")


if __name__ == "__main__":
    main()

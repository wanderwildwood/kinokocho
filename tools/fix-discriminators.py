#!/usr/bin/env python3
"""
Makes every "tells them apart" actually tell them apart.

An audit of the eighty-three confusions found sixty-three naming at least one character
that does not separate the pair at all — and two places use those names. The candidate
page prints them as "Tells them apart: ...", which is advice, and the key pulls them
forward ahead of their information gain, which spends questions.

The worst of them were not merely useless. *Tricholoma sejunctum* against the death cap
named the spore print, and both print pale: a reader told to take a print to settle that
pair takes one, learns nothing, and believes they have ruled something out.

Two rules, in order:

1. **Prune.** A character where the two taxa's definite states overlap, or where either
   says nothing, cannot settle anything and is removed.
2. **Refill from the note.** Each confusion carries a hand-written sentence saying how to
   tell them apart, so a character whose own noun appears in that sentence is the one the
   writer meant. Those go first; after them, whatever separates the pair, most reliable
   first. Four at most — a list of eight is not a thing anybody checks.

A pair that nothing separates keeps an empty list and is reported, because that is a hole
in the taxon data rather than something to paper over with a plausible character.

    python3 tools/fix-discriminators.py [--dry-run]
"""

import argparse
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")
SCHEMA = os.path.join(HERE, "..", "app", "src", "main", "assets", "schema",
                      "characters-v1.json")
MOST = 4


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    pack = json.load(open(PACK))
    schema = json.load(open(SCHEMA))
    by = {t["id"]: t for t in pack["taxa"]}
    characters = {c["id"]: c for c in schema["characters"]}

    def definite(taxon, cid):
        return {s["value"] for s in taxon["characters"].get(cid, [])
                if s["frequency"].lower() in ("always", "usually")}

    def separates(a, b, cid):
        x, y = definite(a, cid), definite(b, cid)
        return bool(x) and bool(y) and not (x & y)

    pruned = 0
    added = 0
    empty = []
    for taxon in pack["taxa"]:
        for look in taxon.get("lookalikes", []):
            other = by.get(look["taxon"])
            if not other:
                continue
            kept = [c for c in look["discriminators"] if separates(taxon, other, c)]
            pruned += len(look["discriminators"]) - len(kept)

            able = [c for c in characters if separates(taxon, other, c)]
            # The note already says how to tell them apart, so a character the note names
            # is the character the writer meant.
            note = look.get("note", "").lower()
            named = [c for c in able
                     if characters[c]["noun"].lower() in note and c not in kept]
            rest = sorted([c for c in able if c not in kept and c not in named],
                          key=lambda c: -characters[c]["power"])

            before = len(kept)
            for c in named + rest:
                if len(kept) >= MOST:
                    break
                kept.append(c)
            added += len(kept) - before

            if not kept:
                empty.append(f"{taxon['id']} -> {look['taxon']}")
            look["discriminators"] = kept

    if not args.dry_run:
        with open(PACK, "w") as f:
            json.dump(pack, f, indent=2, ensure_ascii=False)
            f.write("\n")

    print(f"{pruned} removed that separated nothing, {added} put in their place"
          + (" (dry run)" if args.dry_run else ""))
    if empty:
        print(f"\n{len(empty)} pairs that nothing in the data separates — these are holes "
              f"in the taxon rows, not in the list:")
        for e in empty:
            print(f"    {e}")


if __name__ == "__main__":
    main()

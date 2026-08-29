#!/usr/bin/env python3
"""
Writes the work order for the drawings that have to be done by a person.

The app draws its own character states — a gill running down a stem, a sac at the base
— and those are diagrams of one feature, which is what a schema needs. What it cannot
draw is a *mushroom*: a plate of the destroying angel that a reader can hold a mushroom
up against. Those want a hand.

Generated rather than kept, because it was written once against eighty-six taxa and the
pack is a hundred now: a work order that has drifted from the data is worse than none,
since the illustrator draws what it says. Run it again whenever the pack changes.

    python3 tools/drawings-wanted.py [output.txt]

Tiers are computed, not curated:
  1. what can kill or seriously harm, commonest first
  2. what those are confused with, which is the other half of every comparison
  3. one good example of each kind of fungus, so the app's first question has pictures

    python3 tools/drawings-wanted.py ~/Desktop/mushroom-journal-drawings-wanted.txt
"""

import json
import os
import sys
import textwrap

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")
SCHEMA = os.path.join(HERE, "..", "app", "src", "main", "assets", "schema",
                      "characters-v1.json")
DEFAULT_OUT = os.path.expanduser("~/Desktop/mushroom-journal-drawings-wanted.txt")

RULE = "=" * 72
THIN = "-" * 72

HEAD = """MUSHROOM JOURNAL — DRAWINGS WANTED
{rule}

For 茸帳 / Mushroom Journal, an e-ink field journal. Everything here is line work
for a 4.3in black-and-white screen: no colour, no grey, no shading. A field guide
plate rather than a logo.

THE HAND
{thin}
  The reference is the owner's own chanterelle, which travels with this file as
  chanterelle-reference.png. It is not a mood board — it was measured, and these
  five rules came out of it. Please work to them rather than to a general idea of
  a line drawing.

  1. THIN. The pen line is about 0.014 of the width of the thing it draws — on a
     drawing 700px across, a line of about 10px. Our first attempts were nearly
     three times that and read as diagrams.
  2. ONE WEIGHT. A pen does not change nibs halfway through a drawing. What
     separates the cap from the ridges under it is where the lines go, not how
     heavy they are.
  3. FEW LINES. The reference is eight strokes. What is left out is most of why it
     works. If a line is not carrying information, it is in the way.
  4. CROOKED. Its left margin is rolled and the right is not. A symmetrical
     mushroom reads as an ornament; an asymmetrical one reads as a mushroom. Please
     do not tidy this out.
  5. NO BLACK. A solid fill on a page of thin lines is not a darker area, it is a
     hole. Where tone is needed, an open hatch. Small dots are fine.

  None of that is licence to be vague. Accuracy is the point of the whole app —
  thrift that costs accuracy is just a worse drawing.

WHAT TO SHOW
{thin}
  - THREE-QUARTER VIEW wherever there is a cap: tilted so the top and the gills or
    pores underneath are both visible at once. This is the single thing that makes
    a drawing read as a mushroom instead of a shape.
  - THE BASE OF THE STEM, dug up, on anything that has one. It is the most commonly
    missed character and half the point of the app.
  - Legible at about 64 x 64 pixels. A fault invisible at full size is usually
    obvious at that one, so please check small before calling one finished.

FORMAT
{thin}
  SVG preferred, single colour, stroked paths. PNG on white at 1024px is fine too.
  Square framing, a little air around the subject. File named for the species.

  {count} drawings in three tiers. Tier 1 first.
"""

TIER_1 = """

TIER 1 — WHAT CAN HURT YOU ({n}). Draw these first.
{rule}
A reader looking at one of these may be holding the thing. Commonest first.
"""

TIER_2 = """

TIER 2 — WHAT THEY ARE CONFUSED WITH ({n}). Draw these second.
{rule}
A drawing of a dangerous mushroom is only half a comparison. These are what people
actually mistake them for, and the pair is what teaches.
"""

TIER_3 = """

TIER 3 — ONE PER KIND OF FUNGUS ({n}). Draw these third.
{rule}
The app's first question is always "what kind of fungus is it?". These are one good
example of each answer, so that question has real pictures behind it.
"""

SEVERITY = {
    "LETHAL": "[CAN KILL]",
    "SEVERE": "[SERIOUS HARM]",
    "GI": "[MAKES PEOPLE ILL]",
    "INTOXICATION": "[INTOXICATING]",
}
PREVALENCE = {"common": "met on most walks", "occasional": "found regularly",
              "uncommon": "uncommon here"}


def wrap(text, indent):
    return textwrap.fill(text, width=72, initial_indent=indent,
                         subsequent_indent=indent)


def main():
    out_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    pack = json.load(open(PACK))
    schema = json.load(open(SCHEMA))
    by = {t["id"]: t for t in pack["taxa"]}
    labels = {c["id"]: c for c in schema["characters"]}
    colours = {c["id"]: c["label"] for c in schema["colours"]}

    def value_label(cid, vid):
        c = labels.get(cid)
        if not c:
            return vid
        if c.get("valuesFrom") == "colours":
            return colours.get(vid, vid)
        for v in c.get("values", []):
            if v["id"] == vid:
                return v["label"]
        return vid

    # The nouns are short because the app puts a heading above them saying which part
    # of the mushroom they belong to. A flat list has no heading, so "Colour" appeared
    # twice on the same mushroom meaning the cap and then the gills.
    PART = {"cap": "Cap", "gills": "Gill", "stem": "Stem", "flesh": "Flesh"}

    def field(cid):
        c = labels[cid]
        part = PART.get(c["group"])
        return f"{part} {c['noun'].lower()}" if part else c["noun"]

    def applicable(taxon, cid):
        """
        Whether the character means anything for this mushroom.

        The pack records `ring: absent` on things that have no stem, which is true and
        useless: the brief said "Stem ring: No ring" under a carbon ball. The schema
        already knows what depends on what, so it is asked.
        """
        for dep in schema["dependencies"]:
            if dep["character"] != cid:
                continue
            requires = dep["requires"]
            held = {st["value"]
                    for st in taxon["characters"].get(requires["character"], [])}
            if not held & set(requires["anyOf"]):
                return False
        return True

    def describe(taxon, lines):
        """The characters an illustrator has to get right, in reading order."""
        for cid in ["fruitbody_type", "stipe_base", "ring", "gill_attachment",
                    "gill_spacing", "cap_shape", "cap_surface", "cap_margin",
                    "cap_colour", "veil_remnants", "hymenophore_colour",
                    "stipe_surface", "substrate", "growth_habit"]:
            states = taxon["characters"].get(cid)
            if not states or not applicable(taxon, cid):
                continue
            shown = [value_label(cid, st["value"]) for st in states
                     if st["frequency"] != "RARELY"]
            if shown:
                lines.append(f"      {field(cid)}: {', '.join(shown)}")

    prevalence_order = {"common": 0, "occasional": 1, "uncommon": 2}

    tier1 = sorted(
        [t for t in pack["taxa"] if t["hazard"]["severity"] in ("LETHAL", "SEVERE")],
        key=lambda t: (t["hazard"]["severity"] != "LETHAL",
                       prevalence_order[t["prevalence"]], t["scientificName"]),
    )[:10]

    # Whatever those are confused with, either way round: confusion is mutual and the
    # rows are not.
    partners = []
    for t in tier1:
        for look in t.get("lookalikes", []):
            partners.append((look["taxon"], t["id"], look["note"]))
        for other in pack["taxa"]:
            for look in other.get("lookalikes", []):
                if look["taxon"] == t["id"]:
                    partners.append((other["id"], t["id"], look["note"]))
    seen, tier2 = set(), []
    drawn = {t["id"] for t in tier1}
    for tid, against, note in partners:
        if tid in seen or tid in drawn or tid not in by:
            continue
        seen.add(tid)
        tier2.append((by[tid], by[against], note))
    tier2 = sorted(tier2, key=lambda p: prevalence_order[p[0]["prevalence"]])[:10]

    drawn |= {t[0]["id"] for t in tier2}
    tier3 = []
    for value in labels["fruitbody_type"]["values"]:
        if value["id"] == "other":
            continue
        best = [t for t in pack["taxa"]
                if t["id"] not in drawn
                and any(st["value"] == value["id"]
                        for st in t["characters"].get("fruitbody_type", []))]
        if best:
            best.sort(key=lambda t: (prevalence_order[t["prevalence"]],
                                     t["scientificName"]))
            tier3.append((value["label"], best[0]))
            drawn.add(best[0]["id"])

    total = len(tier1) + len(tier2) + len(tier3)
    lines = [HEAD.format(rule=RULE, thin=THIN, count=total)]

    lines.append(TIER_1.format(n=len(tier1), rule=RULE))
    for t in tier1:
        lines.append(f"\n  {t['scientificName']}"
                     + (f" — {t['commonName']}" if t.get("commonName") else ""))
        lines.append(f"    {SEVERITY.get(t['hazard']['severity'], '')} "
                     f"{PREVALENCE[t['prevalence']]}. {t['hazard'].get('note', '')}".rstrip())
        if t.get("note"):
            lines.append(wrap(t["note"], "    "))
        describe(t, lines)

    lines.append(TIER_2.format(n=len(tier2), rule=RULE))
    for t, against, note in tier2:
        lines.append(f"\n  {t['scientificName']}"
                     + (f" — {t['commonName']}" if t.get("commonName") else ""))
        lines.append(f"    Drawn against {against['scientificName']}.")
        lines.append(wrap(note, "    "))
        describe(t, lines)

    lines.append(TIER_3.format(n=len(tier3), rule=RULE))
    for label, t in tier3:
        lines.append(f"\n  {label.upper()}")
        lines.append(f"    {t['scientificName']}"
                     + (f" — {t['commonName']}" if t.get("commonName") else ""))
        if t.get("note"):
            lines.append(wrap(t["note"], "    "))
        describe(t, lines)

    lines.append("\n" + RULE)
    lines.append(wrap(
        f"Generated from the region pack, which holds {len(pack['taxa'])} taxa. "
        f"Re-run tools/drawings-wanted.py after changing it.", ""))

    with open(out_path, "w") as f:
        f.write("\n".join(lines) + "\n")
    print(f"{total} drawings -> {out_path}")
    print(f"  tier 1: {len(tier1)}   tier 2: {len(tier2)}   tier 3: {len(tier3)}")


if __name__ == "__main__":
    main()

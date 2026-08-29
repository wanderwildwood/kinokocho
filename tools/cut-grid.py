#!/usr/bin/env python3
"""
Cuts a generated grid of mushrooms into single drawings, named correctly.

The workflow this exists for: an image generator is asked for six mushrooms in a grid,
and comes back with a sheet — usually with captions on it, often in colour, and always
with the names spelled by a model that has never seen a spore print. None of that
matters if the sheet is cut up here instead: the reading order is known, so the file
names come from the batch list rather than from anything written on the picture.

It also does the part the generator will not reliably do. Colour and grey are stripped to
pure black on white, because the screen this is for is one bit deep and a grey wash
becomes a smear on it. Anything that survives is a line.

    python3 tools/cut-grid.py sheet.png 1            # batch 1, from the prompts file
    python3 tools/cut-grid.py sheet.png 1 --rows 2 --cols 3
    python3 tools/cut-grid.py sheet.png all --rows 6 --cols 6   # one sheet, all of them
    python3 tools/cut-grid.py sheet.png 1 --drop-top 14      # captions above each cell
    python3 tools/cut-grid.py sheet.png 1 --drop-bottom 12   # captions below each cell
    python3 tools/cut-grid.py sheet.png 1 --threshold 200 --keep-grey

Captions are not a problem worth arguing with the generator about — they get cut off
here. Which end they are on changes between runs, so it is told rather than guessed: a
wrong guess quietly beheads a mushroom instead of removing a word.

Writes into tools/reference/plates/ and prints what it wrote. Nothing is installed into
the app by this: look at them first.
"""

import argparse
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "reference", "plates")
BATCH = 6


def every_id():
    """Every drawing in the brief, in the order the prompts file lists them."""
    out = []
    n = 1
    while True:
        batch = batch_ids(n)
        if not batch:
            return out
        out += batch
        n += 1


def batch_ids(n):
    """
    The ids of batch n, in reading order.

    Derived the same way drawings-wanted.py derives them, so the two cannot disagree
    about which cell is which — the whole scheme rests on that agreement, since the
    picture itself carries no reliable name.
    """
    # drawings-wanted.py cannot be imported by that name, so the ordering is recomputed
    # here from the same pack by the same rules. If either changes, change both — the
    # whole scheme rests on the two agreeing about which cell is which.
    pack = json.load(open(os.path.join(
        HERE, "..", "app", "src", "main", "assets", "packs",
        "southern-appalachia-v1.json")))
    schema = json.load(open(os.path.join(
        HERE, "..", "app", "src", "main", "assets", "schema", "characters-v1.json")))
    by = {t["id"]: t for t in pack["taxa"]}
    order = {"common": 0, "occasional": 1, "uncommon": 2}

    tier1 = sorted(
        [t for t in pack["taxa"] if t["hazard"]["severity"] in ("LETHAL", "SEVERE")],
        key=lambda t: (t["hazard"]["severity"] != "LETHAL",
                       order[t["prevalence"]], t["scientificName"]),
    )[:10]

    partners = []
    for t in tier1:
        for look in t.get("lookalikes", []):
            partners.append(look["taxon"])
        for other in pack["taxa"]:
            for look in other.get("lookalikes", []):
                if look["taxon"] == t["id"]:
                    partners.append(other["id"])
    seen, tier2 = set(), []
    drawn = {t["id"] for t in tier1}
    for tid in partners:
        if tid in seen or tid in drawn or tid not in by:
            continue
        seen.add(tid)
        tier2.append(by[tid])
    tier2 = sorted(tier2, key=lambda t: order[t["prevalence"]])[:10]

    drawn |= {t["id"] for t in tier2}
    tier3 = []
    fruitbody = next(c for c in schema["characters"] if c["id"] == "fruitbody_type")
    for value in fruitbody["values"]:
        if value["id"] == "other":
            continue
        best = [t for t in pack["taxa"]
                if t["id"] not in drawn
                and any(st["value"] == value["id"]
                        for st in t["characters"].get("fruitbody_type", []))]
        if best:
            best.sort(key=lambda t: (order[t["prevalence"]], t["scientificName"]))
            tier3.append(best[0])
            drawn.add(best[0]["id"])

    every = [t["id"] for t in tier1 + tier2 + tier3]
    return every[(n - 1) * BATCH: n * BATCH]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("sheet")
    ap.add_argument("batch",
                    help="a batch number from the prompts file, or 'all' for one sheet "
                         "holding every drawing")
    ap.add_argument("--rows", type=int, default=2)
    ap.add_argument("--cols", type=int, default=3)
    ap.add_argument("--threshold", type=int, default=170,
                    help="0-255; lower keeps only the darkest lines")
    ap.add_argument("--keep-grey", action="store_true",
                    help="leave it grey instead of forcing black on white")
    ap.add_argument("--drop-top", type=int, default=0, metavar="PCT",
                    help="cut this %% off the top of each cell, for captions above")
    ap.add_argument("--drop-bottom", type=int, default=0, metavar="PCT",
                    help="cut this %% off the bottom of each cell, for captions below")
    args = ap.parse_args()

    if args.batch == "all":
        ids = every_id()
        if args.rows * args.cols < len(ids):
            sys.exit(f"a {args.cols}x{args.rows} grid holds {args.rows * args.cols}, "
                     f"and there are {len(ids)} drawings. Try --rows 6 --cols 6.")
    else:
        ids = batch_ids(int(args.batch))
        if not ids:
            sys.exit(f"there is no batch {args.batch}")
    os.makedirs(OUT, exist_ok=True)

    size = subprocess.run(["identify", "-format", "%w %h", args.sheet],
                          capture_output=True, text=True, check=True).stdout.split()
    w, h = int(size[0]), int(size[1])
    cw, chh = w // args.cols, h // args.rows
    print(f"{args.sheet} is {w}x{h}; cutting {args.cols}x{args.rows} cells of {cw}x{chh}")

    written = 0
    for i, tid in enumerate(ids):
        r, c = divmod(i, args.cols)
        if r >= args.rows:
            print(f"  ! {tid}: no cell {i + 1} in a {args.cols}x{args.rows} grid")
            continue
        dest = os.path.join(OUT, f"{tid}.png")
        # The caption band, if the sheet has one. Which end it is on depends on the
        # generator and changes between runs, so it is asked for rather than guessed:
        # a wrong guess quietly beheads a mushroom.
        top = chh * args.drop_top // 100
        bottom = chh * args.drop_bottom // 100
        crop = f"{cw}x{chh - top - bottom}+{c * cw}+{r * chh + top}"
        # Trim to the ink, then pad, so a drawing that sits high in its cell is not
        # left sitting high in its file.
        steps = ["convert", args.sheet, "-crop", crop, "+repage"]
        if not args.keep_grey:
            # Colour and grey go. The screen is one bit deep: a grey wash on it is a
            # smear, and a caption the generator added is usually mid-grey and lifts
            # out with everything else that is not a line.
            steps += ["-colorspace", "Gray", "-normalize",
                      "-threshold", f"{args.threshold * 100 // 255}%"]
        steps += ["-fuzz", "8%", "-trim", "+repage",
                  "-bordercolor", "white", "-border", "24", dest]
        subprocess.run(steps, check=True)
        out = subprocess.run(["identify", "-format", "%wx%h", dest],
                             capture_output=True, text=True).stdout
        print(f"  {i + 1}. {tid}.png  ({out})")
        written += 1

    print(f"\n{written} written to {OUT}")
    print("Look at them before anything else. A cell that came back as a duplicate or "
          "in the wrong order is easiest to catch now.")


if __name__ == "__main__":
    main()

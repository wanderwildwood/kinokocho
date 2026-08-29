#!/usr/bin/env python3
"""
Cuts a generated sheet of mushrooms into single drawings, named correctly.

An image generator asked for six mushrooms comes back with a sheet: usually captioned,
sometimes in colour, often with one species drawn twice and one label that is not a
species at all. Arguing with it costs more than fixing the result, and asking for
thirty-two separate images costs more still — an answer that is too much hand-work to
happen is the wrong answer however correct it is.

So the sheet is taken as it comes and everything wrong with it is fixed here.

  Names          The picture's own captions are ignored entirely. You read the sheet and
                 say which cell is which; nothing on the image is trusted, because
                 "Amanita bunnescens" is what a model writes when it is filling a grid.
  Layout         The drawings are found rather than assumed. The first sheet back had
                 four across the top and three across the bottom on different spacings,
                 and cutting it into even cells sliced a mushroom in half.
  Captions       Found as their own regions — short, wide, small-marked — and dropped.
  Colour, grey   Thresholded away. Not a preference the generator kept ignoring: the
                 screen is one bit deep, and a grey wash on it is a smear.

    python3 tools/cut-grid.py sheet.png --batch 1
    python3 tools/cut-grid.py sheet.png --cells a,b,skip,c    # what actually came back
    python3 tools/cut-grid.py sheet.png --list                # just say what you found

Writes into tools/reference/plates/, which is gitignored. Look at them before anything
is installed into the app.
"""

import argparse
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "reference", "plates")
PACK = os.path.join(HERE, "..", "app", "src", "main", "assets", "packs",
                    "southern-appalachia-v1.json")
SCHEMA = os.path.join(HERE, "..", "app", "src", "main", "assets", "schema",
                      "characters-v1.json")
BATCH = 6


def every_id():
    """
    Every drawing in the brief, in the order the prompts file lists them.

    Recomputed here from the same pack by the same rules drawings-wanted.py uses. If
    either changes, change both: the whole scheme rests on the two agreeing about which
    drawing is which.
    """
    pack = json.load(open(PACK))
    schema = json.load(open(SCHEMA))
    by = {t["id"]: t for t in pack["taxa"]}
    order = {"common": 0, "occasional": 1, "uncommon": 2}

    tier1 = sorted(
        [t for t in pack["taxa"] if t["hazard"]["severity"] in ("LETHAL", "SEVERE")],
        key=lambda t: (t["hazard"]["severity"] != "LETHAL",
                       order[t["prevalence"]], t["scientificName"]),
    )[:10]

    partners = []
    for t in tier1:
        partners += [look["taxon"] for look in t.get("lookalikes", [])]
        for other in pack["taxa"]:
            partners += [other["id"] for look in other.get("lookalikes", [])
                         if look["taxon"] == t["id"]]
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

    return [t["id"] for t in tier1 + tier2 + tier3]


def find_drawings(path, threshold, gutter_divisor=60):
    """
    Where the drawings actually are, rather than where a grid says they should be.

    The gaps between drawings on a generated sheet are pure white, so they can simply be
    found. Ink is projected onto the vertical axis to give bands of rows that contain
    anything, then onto the horizontal axis within each band. What comes back is a box
    per drawing, in reading order.

    Captions turn up as their own boxes, being separated by white too. They are short and
    wide and made of small marks, and are dropped on that basis — which is why a caption
    band no longer has to be measured and cut off by hand.
    """
    from PIL import Image
    im = Image.open(path).convert("L")
    w, h = im.size
    px = im.load()
    ink = [[px[x, y] < threshold for x in range(w)] for y in range(h)]

    def bands(has_ink, gap):
        out, start, run = [], None, 0
        for i, v in enumerate(has_ink):
            if v:
                if start is None:
                    start = i
                run = 0
            elif start is not None:
                run += 1
                if run > gap:
                    out.append((start, i - run))
                    start = None
        if start is not None:
            out.append((start, len(has_ink)))
        return out

    boxes = []
    for y0, y1 in bands([any(r) for r in ink], max(8, h // gutter_divisor)):
        cols = [any(ink[y][x] for y in range(y0, y1)) for x in range(w)]
        for x0, x1 in bands(cols, max(8, w // gutter_divisor)):
            bw, bh = x1 - x0, y1 - y0
            if bw < w // 40 or bh < h // 40:
                continue
            # A caption is a wide, short strip. A mushroom is not.
            if bw > bh * 3 and bh < h // 10:
                continue
            boxes.append(drop_caption(ink, x0, y0, x1, y1))
    return boxes


def drop_caption(ink, x0, y0, x1, y1):
    """
    Takes the caption off the bottom of a drawing that has one.

    Captions are separated from their drawing by white, but only by a few pixels — less
    than the gutter between drawings — so they arrive inside the same box rather than as
    boxes of their own. Looking inside the box finds them: a short band at the bottom,
    after a gap, made of marks far wider than they are tall.

    Nothing is removed unless it looks like text. A drawing with a detached part low down
    — a puffball beside its cut half, a bracket with a fallen piece — is taller than a
    line of type and is left alone.
    """
    height = y1 - y0
    rows = [any(ink[y][x] for x in range(x0, x1)) for y in range(y0, y1)]

    # The box as bands of ink. Working on bands rather than walking up from the bottom
    # avoids the trap that a box usually ends in a few blank rows, which made the "gap"
    # zero wide and stopped the search before it had looked at anything.
    bands, cur = [], None
    for i, v in enumerate(rows):
        if v and cur is None:
            cur = i
        elif not v and cur is not None:
            bands.append((cur, i))
            cur = None
    if cur is not None:
        bands.append((cur, len(rows)))
    if not bands:
        return (x0, y0, x1, y1)

    # Drop trailing bands that are a line of type: short, and with the drawing still
    # above them. A drawing with a detached part low down — a puffball beside its cut
    # half — is taller than a line of type and is kept.
    while len(bands) > 1:
        top, bottom = bands[-1]
        if (bottom - top) < height * 0.12 and bands[0][1] - bands[0][0] > height * 0.4:
            bands.pop()
        else:
            break
    return (x0, y0, x1, y0 + bands[-1][1])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("sheet")
    ap.add_argument("--batch", type=int,
                    help="a batch number from the prompts file, if the sheet matches it")
    ap.add_argument("--cells",
                    help="comma-separated taxon ids in the order they appear, with "
                         "'skip' for a duplicate or anything that is not wanted. Use "
                         "this whenever the sheet does not match a batch, which is most "
                         "of the time.")
    ap.add_argument("--list", action="store_true",
                    help="find the drawings and say where they are, writing nothing")
    ap.add_argument("--threshold", type=int, default=170,
                    help="0-255; lower keeps only the darkest lines")
    ap.add_argument("--keep-grey", action="store_true",
                    help="leave it grey instead of forcing black on white")
    args = ap.parse_args()

    boxes = find_drawings(args.sheet, args.threshold)
    print(f"{args.sheet}: {len(boxes)} drawings found")

    if args.list or (not args.batch and not args.cells):
        for i, (x0, y0, x1, y1) in enumerate(boxes, 1):
            print(f"  {i:2}. {x1 - x0:4} x {y1 - y0:4}  at {x0},{y0}")
        if not args.list:
            print("\nPass --batch N or --cells id,id,skip,... to write them out.")
        return

    ids = ([c.strip() for c in args.cells.split(",")] if args.cells
           else every_id()[(args.batch - 1) * BATCH: args.batch * BATCH])
    wanted = [t for t in ids if t not in ("skip", "-", "")]
    if len(boxes) != len(ids):
        print(f"  ! the sheet holds {len(boxes)} drawings and you named {len(ids)} "
              f"cells. Run with --list, look at the sheet, and put a 'skip' in --cells "
              f"for every drawing you do not want.")

    os.makedirs(OUT, exist_ok=True)
    written = 0
    for i, tid in enumerate(ids):
        if tid in ("skip", "-", "") or i >= len(boxes):
            continue
        x0, y0, x1, y1 = boxes[i]
        dest = os.path.join(OUT, f"{tid}.png")
        steps = ["convert", args.sheet,
                 "-crop", f"{x1 - x0}x{y1 - y0}+{x0}+{y0}", "+repage"]
        if not args.keep_grey:
            steps += ["-colorspace", "Gray", "-normalize",
                      "-threshold", f"{args.threshold * 100 // 255}%"]
        steps += ["-fuzz", "8%", "-trim", "+repage",
                  "-bordercolor", "white", "-border", "24", dest]
        subprocess.run(steps, check=True)
        size = subprocess.run(["identify", "-format", "%wx%h", dest],
                              capture_output=True, text=True).stdout
        print(f"  {i + 1:2}. {tid}.png  ({size})")
        written += 1

    print(f"\n{written} written to {OUT}")
    if written != len(wanted):
        print(f"  ! {len(wanted)} were named. Check the sheet against --list.")
    print("Look at them before anything goes into the app.")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Traces one figure from an old coloured plate into the one-bit line drawing the candidate
page shows.

The plates worth having are a century or two old — Peck's New York State reports,
Sowerby, Bresadola, Bulliard — and most of them are coloured, not drawn: a
chromolithograph is flat areas of colour with hardly a line in it, so thresholding one
gives blobs. What survives the trip to a one-ink panel is where one colour meets another
and the darker strokes laid over them, so that is what this keeps:

- the outline of the figure against the paper, the paper being whatever colour the edge
  of the crop is;
- the boundaries between colours inside it, which is cap against gills against stem;
- strokes darker than their surroundings, which is gills, scales and hatching.

    python3 tools/trace-plate.py plate.jpg out.png 0.06 0.33 0.93 0.92
    python3 tools/trace-plate.py plate.jpg out.png 0.06 0.33 0.93 0.92 --ink 20 --edge 45

The four numbers are the crop, as fractions of the image. Anything else in the crop — a
neighbouring figure, a plate number — has to be painted over in paper colour first; the
tracer cannot tell one mushroom from another. Raise --ink when flat areas come out
speckled, raise --edge for a noisy scan, and try --edge 80 with a lower --ink for a real
engraving that already has its lines.

A faint trace is thickened a pixel at a time, at most twice, until its ink covers what
the other plates' do: 7% of the drawing's bounding box, where the plates already in the
app run from 7 to 28. Judge the result at 64px, which is how it is seen.

The output goes to tools/reference/plates/<taxon>.png for install-plates.py, and its
source — work, year, licence, file — into plate-sources.json.
"""

import argparse

import cv2
import numpy as np

# The plates already in the app cover 0.068 to 0.279 of their bounding box; below the
# bottom of that a trace reads as a ghost at 64px.
MIN_INK = 0.07


def trace(src, x0, y0, x1, y1, width, edge, ink, bgtol, hatch):
    im = cv2.imread(src)
    h, w = im.shape[:2]
    im = im[int(y0 * h):int(y1 * h), int(x0 * w):int(x1 * w)]

    # Worked at twice the final size and brought down at the end, so lines stay fine.
    work = width * 2
    im = cv2.resize(im, (work, int(im.shape[0] * work / im.shape[1])), interpolation=cv2.INTER_AREA)
    lab = cv2.cvtColor(im, cv2.COLOR_BGR2LAB).astype(np.float32)

    # The paper is the colour round the edge of the crop.
    border = np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]])
    paper = np.median(border, axis=0)
    figure = (np.linalg.norm(lab - paper, axis=2) > bgtol).astype(np.uint8)
    figure = cv2.morphologyEx(figure, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    figure = cv2.morphologyEx(figure, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))

    # Keep the figure, not the foxing: nothing under 0.2% of the crop.
    n, labels, stats, _ = cv2.connectedComponentsWithStats(figure)
    kept = np.zeros_like(figure)
    for i in range(1, n):
        if stats[i, 4] > 0.002 * figure.size:
            kept[labels == i] = 1
    figure = kept
    outline = cv2.morphologyEx(figure, cv2.MORPH_GRADIENT, np.ones((3, 3), np.uint8))

    # Where one colour meets another inside the figure.
    smooth = cv2.GaussianBlur(lab, (0, 0), 1.5)
    gx = cv2.Sobel(smooth, cv2.CV_32F, 1, 0)
    gy = cv2.Sobel(smooth, cv2.CV_32F, 0, 1)
    magnitude = np.sqrt((gx ** 2 + gy ** 2).sum(axis=2))
    edges = ((magnitude > edge * 4) & (figure > 0)).astype(np.uint8)

    # Strokes darker than what is round them.
    lightness = lab[:, :, 0]
    local = cv2.GaussianBlur(lightness, (0, 0), 4)
    strokes = ((local - lightness) > ink * 2.55 * 0.4) & (figure > 0) if hatch else np.zeros_like(figure, bool)

    art = ((outline > 0) | (edges > 0) | strokes).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(art)
    for i in range(1, n):
        if stats[i, 4] < 12:
            art[labels == i] = 0

    out = 255 - art * 255
    out = cv2.resize(out, (width, int(out.shape[0] * width / work)), interpolation=cv2.INTER_AREA)
    return np.where(out < 170, 0, 255).astype(np.uint8)


def ink_of(dark):
    ys, xs = np.where(dark)
    return dark[ys.min():ys.max() + 1, xs.min():xs.max() + 1].mean() if len(ys) else 0.0


def thicken(out):
    dark = (out < 128).astype(np.uint8)
    for _ in range(2):
        if ink_of(dark) >= MIN_INK:
            break
        dark = cv2.dilate(dark, np.ones((2, 2), np.uint8))
    return ((1 - dark) * 255).astype(np.uint8)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("src")
    ap.add_argument("out")
    for k in ("x0", "y0", "x1", "y1"):
        ap.add_argument(k, type=float)
    ap.add_argument("--width", type=int, default=420)
    ap.add_argument("--edge", type=float, default=40)
    ap.add_argument("--ink", type=float, default=12)
    ap.add_argument("--bgtol", type=float, default=18)
    ap.add_argument("--hatch", type=int, default=1)
    ap.add_argument("--no-thicken", action="store_true")
    a = ap.parse_args()
    out = trace(a.src, a.x0, a.y0, a.x1, a.y1, a.width, a.edge, a.ink, a.bgtol, a.hatch)
    if not a.no_thicken:
        out = thicken(out)
    cv2.imwrite(a.out, out)
    print(f"{a.out}  {out.shape[1]}x{out.shape[0]}  ink {ink_of(out < 128):.3f}")


if __name__ == "__main__":
    main()

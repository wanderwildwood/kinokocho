#!/usr/bin/env python3
"""
Renders the drawables to a contact sheet, so they can be looked at.

Vector drawable XML cannot be viewed without an emulator, which means art gets written
blind and stays wrong. This lifts the paths back out into SVG and renders a labelled
grid — the cheapest way to see that a chanterelle looks like a chanterelle.

    python3 tools/preview-art.py [character ...]

Writes build/art-preview.png. Needs rsvg-convert and ImageMagick.
"""

import glob
import os
import re
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
DRAWABLE = os.path.join(HERE, "..", "app", "src", "main", "res", "drawable")
OUT = os.path.join(HERE, "..", "build", "art-preview.png")

CELL = 150


def paths_of(xml):
    out = []
    for m in re.finditer(r"<path\b(.*?)/>", xml, re.S):
        blob = m.group(1)

        def attr(n, default=""):
            mm = re.search(rf'android:{n}="([^"]*)"', blob)
            return mm.group(1) if mm else default

        out.append({
            "d": attr("pathData"),
            "stroke": attr("strokeColor", "#00000000"),
            "width": attr("strokeWidth", "0"),
            "fill": attr("fillColor", "#00000000"),
            "cap": attr("strokeLineCap", "round"),
        })
    return out


def svg_of(xml):
    body = []
    for p in paths_of(xml):
        stroke = "none" if p["stroke"].endswith("00000000") else "#000"
        fill = "none" if p["fill"].endswith("00000000") else "#000"
        body.append(
            f'<path d="{p["d"]}" stroke="{stroke}" stroke-width="{p["width"]}" '
            f'fill="{fill}" stroke-linecap="{p["cap"]}" stroke-linejoin="round"/>'
        )
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">'
        '<rect width="96" height="96" fill="#fff"/>' + "".join(body) + "</svg>"
    )


def main():
    wanted = sys.argv[1:]
    files = sorted(glob.glob(os.path.join(DRAWABLE, "art_*.xml")))
    if wanted:
        files = [f for f in files
                 if any(os.path.basename(f)[4:].startswith(w) for w in wanted)]
    if not files:
        raise SystemExit("no drawables matched")

    tmp = tempfile.mkdtemp()
    tiles = []
    for f in files:
        name = os.path.basename(f)[4:-4]
        svg = os.path.join(tmp, name + ".svg")
        png = os.path.join(tmp, name + ".png")
        with open(svg, "w") as fh:
            fh.write(svg_of(open(f).read()))
        subprocess.run(["rsvg-convert", "-w", str(CELL), "-h", str(CELL),
                        "-o", png, svg], check=True)
        labelled = os.path.join(tmp, name + "_l.png")
        subprocess.run([
            "convert", png, "-background", "white", "-fill", "black",
            "-pointsize", "13", "-gravity", "south",
            "label:" + name.replace("_", " "), "-append",
            "-bordercolor", "#bbb", "-border", "1", labelled,
        ], check=True)
        tiles.append(labelled)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    subprocess.run(["montage", *tiles, "-tile", "6x", "-geometry", "+6+6",
                    "-background", "white", OUT], check=True)
    print(f"{len(tiles)} drawings -> {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()

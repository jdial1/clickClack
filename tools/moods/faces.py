"""The pack's five character bricks, redrawn in each of the four brick colours.

The mood picker offers every face in its own pack colour or in red, yellow,
green or blue. The pack only ships one colour per face, so this recolours them:
each pixel keeps its brightness relative to the face's body colour and takes
the target's hue. White details (the one-eyed face's ring) stay white, and
features darker than the body are pushed a little darker so they still read
on green and blue.

    python tools/moods/faces.py

Writes extra_character_<a..e>_<colour>.png next to the originals.
"""
import colorsys
from collections import Counter
from pathlib import Path

from PIL import Image

RES = Path(__file__).resolve().parents[2] / "app/src/main/res/drawable-nodpi"
COLOURS = {"red": 0xF54E4F, "yellow": 0xFFB733, "green": 0x4EB162, "blue": 0x108BE2}


def hsv(rgb):
    return colorsys.rgb_to_hsv(*(c / 255 for c in rgb))


def recolour(im, target):
    body = Counter(p[:3] for p in im.getdata() if p[3] == 255).most_common(1)[0][0]
    _, bs, bv = hsv(body)
    th, ts, tv = hsv(((target >> 16) & 255, (target >> 8) & 255, target & 255))
    out = im.copy()
    px = out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = hsv((r, g, b))
            if s < 0.12 and v > 0.8:  # white detail
                continue
            ratio = v / bv
            if ratio < 1:
                ratio **= 1.6
            nv = min(1.0, ratio * tv)
            ns = min(1.0, s * ts / bs) if bs else ts
            px[x, y] = tuple(round(c * 255) for c in colorsys.hsv_to_rgb(th, ns, nv)) + (a,)
    return out


def main():
    for face in "abcde":
        im = Image.open(RES / f"extra_character_{face}.png").convert("RGBA")
        for name, argb in COLOURS.items():
            recolour(im, argb).save(RES / f"extra_character_{face}_{name}.png", optimize=True)


if __name__ == "__main__":
    main()

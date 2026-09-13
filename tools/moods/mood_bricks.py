"""One-stud mood bricks for the weekly sculpture, in each player colour.

Each is the pack's low 1-stud brick with a tiny face pressed in. Moods share
six faces (see Mood.glyph).

    python tools/moods/mood_bricks.py

Writes mood_<glyph>_<colour>.png next to the pack bricks.
"""
import colorsys
from collections import Counter
from pathlib import Path

from PIL import Image, ImageDraw

RES = Path(__file__).resolve().parents[2] / "app/src/main/res/drawable-nodpi"
COLOURS = ["red", "yellow", "green", "blue"]
K = 8  # supersample


def darker(rgb, k):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    return tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s, v * k)) + (255,)


def draw(glyph, d, ink):
    def P(*v):
        return [x * K for x in v]

    def eyes(y=26):
        d.ellipse(P(21, y - 2.5, 26, y + 2.5), fill=ink)
        d.ellipse(P(38, y - 2.5, 43, y + 2.5), fill=ink)

    if glyph == "smile":
        eyes()
        d.arc(P(24, 25, 40, 37), start=20, end=160, fill=ink, width=3 * K)
    elif glyph == "flat":
        eyes()
        d.rounded_rectangle(P(25, 33, 39, 36), radius=1.5 * K, fill=ink)
    elif glyph == "down":
        eyes(27)
        d.arc(P(24, 32, 40, 44), start=200, end=340, fill=ink, width=3 * K)
    elif glyph == "spiky":
        d.line(P(20, 22, 27, 25), fill=ink, width=3 * K)
        d.line(P(44, 22, 37, 25), fill=ink, width=3 * K)
        eyes(28)
        d.line(P(24, 37, 28, 34, 32, 37, 36, 34, 40, 37), fill=ink, width=2 * K, joint="curve")
    elif glyph == "news":
        d.rounded_rectangle(P(29.5, 20, 34.5, 32), radius=2.5 * K, fill=ink)
        d.ellipse(P(29.5, 34, 34.5, 39), fill=ink)
    elif glyph == "heart":
        d.ellipse(P(23, 22, 32.5, 31), fill=ink)
        d.ellipse(P(31.5, 22, 41, 31), fill=ink)
        d.polygon(P(23.3, 28, 40.7, 28, 32, 39), fill=ink)


def main():
    for colour in COLOURS:
        base = Image.open(RES / f"{colour}_brick_low_1.png").convert("RGBA")
        body = Counter(p[:3] for p in base.getdata() if p[3] == 255).most_common(1)[0][0]
        for glyph in ["smile", "flat", "down", "spiky", "news", "heart"]:
            layer = Image.new("RGBA", (base.width * K, base.height * K), (0, 0, 0, 0))
            draw(glyph, ImageDraw.Draw(layer), darker(body, 0.55))
            out = base.copy()
            out.alpha_composite(layer.resize(base.size, Image.LANCZOS))
            out.save(RES / f"mood_{glyph}_{colour}.png", optimize=True)


if __name__ == "__main__":
    main()

"""Icon bricks for the deck themes, each in its theme's brick colour.

    python tools/ui/themes.py path/to/kenney_toy-brick-pack.zip

Writes theme_<name>.png into app/src/main/res/drawable-nodpi.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from icons import B, CX, CY, P, W, line  # noqa: E402  (icons imports the brick renderer)

K = B.K


def arc(d, cx, cy, r, start, end, w):
    d.arc(P(cx - r - w / 2, cy - r - w / 2, cx + r + w / 2, cy + r + w / 2), start=start, end=end, fill=255, width=int(w * K))


def heart(d, cx, cy, s):
    d.ellipse(P(cx - s, cy - s * 0.9, cx, cy + s * 0.1), fill=255)
    d.ellipse(P(cx, cy - s * 0.9, cx + s, cy + s * 0.1), fill=255)
    d.polygon(P(cx - s * 0.98, cy - s * 0.25, cx + s * 0.98, cy - s * 0.25, cx, cy + s * 1.05), fill=255)


def know_me(ink, body):
    # A question mark.
    d = ink.layer(W)
    arc(d, CX, CY - 12, 17, 180, 400, 10)
    line(d, [CX + 13, CY - 1, CX, CY + 8, CX, CY + 16], w=10)
    d.ellipse(P(CX - 7, CY + 26, CX + 7, CY + 40), fill=255)


def everyday(ink, body):
    # A mug with steam.
    d = ink.layer(W)
    d.rounded_rectangle(P(CX - 30, CY - 12, CX + 14, CY + 34), radius=8 * K, fill=255)
    arc(d, CX + 16, CY + 10, 11, -90, 90, 8)
    s = ink.layer(W)
    for x in (CX - 18, CX - 4):
        pts = []
        for i in range(13):
            t = i / 12
            pts += [x + math.sin(t * 2 * math.pi) * 3.5, CY - 20 - t * 20]
        s.line(P(*pts), fill=255, width=5 * K, joint="curve")


def future(ink, body):
    # A rocket.
    d = ink.layer(W)
    d.ellipse(P(CX - 14, CY - 38, CX + 14, CY + 22), fill=255)
    d.polygon(P(CX - 14, CY + 4, CX - 28, CY + 26, CX - 12, CY + 22), fill=255)
    d.polygon(P(CX + 14, CY + 4, CX + 28, CY + 26, CX + 12, CY + 22), fill=255)
    d.polygon(P(CX - 8, CY + 22, CX + 8, CY + 22, CX, CY + 38), fill=255)
    e = ink.layer(B.shade(body, 0.6))
    e.ellipse(P(CX - 6, CY - 16, CX + 6, CY - 4), fill=255)


def playful(ink, body):
    # A die, five face.
    d = ink.layer(W)
    d.rounded_rectangle(P(CX - 30, CY - 30, CX + 30, CY + 30), radius=10 * K, fill=255)
    e = ink.layer(B.shade(body, 0.6))
    for x, y in ((-15, -15), (15, -15), (0, 0), (-15, 15), (15, 15)):
        e.ellipse(P(CX + x - 6, CY + y - 6, CX + x + 6, CY + y + 6), fill=255)


def food(ink, body):
    # Fork and knife.
    d = ink.layer(W)
    for dx in (-6, 0, 6):
        line(d, [CX - 16 + dx, CY - 34, CX - 16 + dx, CY - 14], w=4)
    d.rounded_rectangle(P(CX - 25, CY - 18, CX - 7, CY - 6), radius=5 * K, fill=255)
    line(d, [CX - 16, CY - 8, CX - 16, CY + 36], w=7)
    d.chord(P(CX + 4, CY - 36, CX + 28, CY + 12), start=90, end=270, fill=255)
    d.polygon(P(CX + 16, CY - 36, CX + 22, CY - 36, CX + 22, CY + 4, CX + 16, CY + 4), fill=255)
    line(d, [CX + 18, CY, CX + 18, CY + 36], w=7)


def romantic(ink, body):
    heart(ink.layer(W), CX, CY, 30)


def spicy(ink, body):
    # A flame.
    d = ink.layer(W)
    d.ellipse(P(CX - 24, CY - 8, CX + 24, CY + 36), fill=255)
    d.polygon(P(CX - 23, CY + 10, CX - 4, CY - 38, CX + 6, CY - 14, CX + 16, CY - 28, CX + 23, CY + 10), fill=255)
    e = ink.layer(B.shade(body, 0.7))
    e.ellipse(P(CX - 10, CY + 10, CX + 10, CY + 32), fill=255)
    e.polygon(P(CX - 9, CY + 18, CX + 2, CY - 2, CX + 9, CY + 18), fill=255)


def suitcase(ink, body):
    d = ink.layer(W)
    arc(d, CX, CY - 22, 12, 180, 360, 7)
    line(d, [CX - 12, CY - 22, CX - 12, CY - 14], w=7)
    line(d, [CX + 12, CY - 22, CX + 12, CY - 14], w=7)
    d.rounded_rectangle(P(CX - 34, CY - 16, CX + 34, CY + 32), radius=9 * K, fill=255)
    e = ink.layer(B.shade(body, 0.62))
    for x in (CX - 18, CX + 18):
        e.rounded_rectangle(P(x - 3, CY - 16, x + 3, CY + 32), radius=2 * K, fill=255)


def bubbles(ink, body):
    d = ink.layer(W)
    d.rounded_rectangle(P(CX - 36, CY - 32, CX + 12, CY + 2), radius=12 * K, fill=255)
    d.polygon(P(CX - 26, CY, CX - 30, CY + 14, CX - 14, CY), fill=255)
    e = ink.layer(B.shade(body, 0.62))
    for x in (CX - 24, CX - 12, CX):
        e.ellipse(P(x - 4, CY - 19, x + 4, CY - 11), fill=255)
    f = ink.layer(W)
    f.rounded_rectangle(P(CX - 6, CY + 4, CX + 36, CY + 32), radius=11 * K, fill=255)
    f.polygon(P(CX + 20, CY + 30, CX + 30, CY + 42, CX + 30, CY + 30), fill=255)



THEMES = [
    ("know_me", 0xF54E4F, know_me),
    ("everyday", 0xFFB733, everyday),
    ("future", 0x108BE2, future),
    ("playful", 0x4EB162, playful),
    ("deep", 0x108BE2, bubbles),
    ("food", 0xFFB733, food),
    ("travel", 0x4EB162, suitcase),
    ("romantic", 0xF54E4F, romantic),
    ("spicy", 0xF54E4F, spicy),
]

if __name__ == "__main__":
    for name, colour, fn in THEMES:
        B.render(colour, fn).save(B.RES / f"theme_{name}.png", optimize=True)

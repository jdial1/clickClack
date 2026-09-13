"""Icon bricks for repeated UI actions (settings, back, done, add...).

A white symbol pressed into the neutral slate brick, drawn with the weather
brick renderer so the two sets match.

    python tools/ui/icons.py path/to/kenney_toy-brick-pack.zip

Writes icon_<name>.png into app/src/main/res/drawable-nodpi.
"""
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/weather"))
import bricks as B  # noqa: E402  (reads the pack zip from sys.argv[1])

P = B.P
W = B.WHITE
CX, CY = 64, 72


def line(d, pts, w=8):
    d.line(P(*pts), fill=255, width=w * B.K, joint="curve")
    for x, y in (pts[0:2], pts[-2:]):
        d.ellipse(P(x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=255)


def gear(ink, body):
    d = ink.layer(W)
    for i in range(8):
        a = i * math.pi / 4
        pts = []
        for da, r in ((-0.2, 22), (0.2, 22), (0.16, 34), (-0.16, 34)):
            pts += [CX + math.cos(a + da) * r, CY + math.sin(a + da) * r]
        d.polygon(P(*pts), fill=255)
    d.ellipse(P(CX - 25, CY - 25, CX + 25, CY + 25), fill=255)
    d.ellipse(P(CX - 10, CY - 10, CX + 10, CY + 10), fill=0)


def info(ink, body):
    d = ink.layer(W)
    d.ellipse(P(CX - 7, CY - 34, CX + 7, CY - 20), fill=255)
    d.rounded_rectangle(P(CX - 7, CY - 12, CX + 7, CY + 32), radius=7 * B.K, fill=255)


def back(ink, body):
    d = ink.layer(W)
    line(d, [CX + 28, CY, CX - 26, CY])
    line(d, [CX - 4, CY - 22, CX - 26, CY, CX - 4, CY + 22])


def nxt(ink, body):
    d = ink.layer(W)
    line(d, [CX - 28, CY, CX + 26, CY])
    line(d, [CX + 4, CY - 22, CX + 26, CY, CX + 4, CY + 22])


def check(ink, body):
    line(ink.layer(W), [CX - 28, CY + 2, CX - 8, CY + 22, CX + 28, CY - 20], w=10)


def plus(ink, body):
    d = ink.layer(W)
    line(d, [CX, CY - 28, CX, CY + 28], w=10)
    line(d, [CX - 28, CY, CX + 28, CY], w=10)


def close(ink, body):
    d = ink.layer(W)
    line(d, [CX - 22, CY - 22, CX + 22, CY + 22], w=10)
    line(d, [CX - 22, CY + 22, CX + 22, CY - 22], w=10)


def pencil(ink, body):
    d = ink.layer(W)
    a = math.radians(-45)
    ux, uy = math.cos(a), math.sin(a)
    nx, ny = -uy, ux

    def pt(t, n):
        return [CX + ux * t + nx * n, CY + uy * t + ny * n]
    d.polygon(P(*(pt(-18, -9) + pt(30, -9) + pt(30, 9) + pt(-18, 9))), fill=255)
    d.polygon(P(*(pt(-22, -9) + pt(-38, 0) + pt(-22, 9))), fill=255)
    e = ink.layer(B.shade(body, 0.55))
    e.polygon(P(*(pt(22, -9) + pt(24, -9) + pt(24, 9) + pt(22, 9))), fill=255)


def retake(ink, body):
    d = ink.layer(W)
    r, w = 24, 9
    # PIL draws an arc's stroke inside its box, so pad the box by half the width
    # to keep the stroke centred on r, where the arrowhead sits.
    d.arc(P(CX - r - w / 2, CY - r - w / 2, CX + r + w / 2, CY + r + w / 2), start=-40, end=225, fill=255, width=w * B.K)
    t = math.radians(225)
    ex, ey = CX + math.cos(t) * r, CY + math.sin(t) * r
    tx, ty = -math.sin(t), math.cos(t)  # clockwise tangent on screen
    nx, ny = math.cos(t), math.sin(t)
    d.polygon(P(ex + tx * 15, ey + ty * 15, ex + nx * 12 - tx * 3, ey + ny * 12 - ty * 3, ex - nx * 12 - tx * 3, ey - ny * 12 - ty * 3), fill=255)


def filt(ink, body):
    d = ink.layer(W)
    d.polygon(P(CX - 32, CY - 26, CX + 32, CY - 26, CX + 8, CY + 2, CX + 8, CY + 26, CX - 8, CY + 34, CX - 8, CY + 2), fill=255)


def chevron(ink, body):
    line(ink.layer(W), [CX - 24, CY - 10, CX, CY + 14, CX + 24, CY - 10], w=10)


def bell(ink, body):
    d = ink.layer(W)
    d.chord(P(CX - 24, CY - 34, CX + 24, CY + 14), start=180, end=360, fill=255)
    d.polygon(P(CX - 24, CY - 10, CX + 24, CY - 10, CX + 30, CY + 18, CX - 30, CY + 18), fill=255)
    d.rounded_rectangle(P(CX - 34, CY + 14, CX + 34, CY + 22), radius=4 * B.K, fill=255)
    d.ellipse(P(CX - 8, CY + 22, CX + 8, CY + 36), fill=255)


def calendar(ink, body):
    d = ink.layer(W)
    d.rounded_rectangle(P(CX - 32, CY - 24, CX + 32, CY + 32), radius=8 * B.K, fill=255)
    e = ink.layer(B.shade(body, 0.6))
    for gx in range(3):
        for gy in range(2):
            x, y = CX - 20 + gx * 17, CY - 2 + gy * 16
            e.rounded_rectangle(P(x, y, x + 9, y + 9), radius=2 * B.K, fill=255)
    r = ink.layer(W)
    for x in (CX - 16, CX + 16):
        r.rounded_rectangle(P(x - 4, CY - 34, x + 4, CY - 16), radius=4 * B.K, fill=255)


def signout(ink, body):
    d = ink.layer(W)
    d.rounded_rectangle(P(CX - 34, CY - 30, CX + 2, CY + 30), radius=6 * B.K, fill=255)
    d.rounded_rectangle(P(CX - 26, CY - 22, CX - 6, CY + 22), radius=3 * B.K, fill=0)
    line(d, [CX - 12, CY, CX + 30, CY])
    line(d, [CX + 16, CY - 14, CX + 30, CY, CX + 16, CY + 14])


ICONS = [
    ("Settings", gear, "Us header, Settings"),
    ("Details", info, "Results detail, What is this"),
    ("Back", back, "6 screens"),
    ("Next", nxt, "Decks, tests, setup"),
    ("Done", check, "Done and Save, 10 places"),
    ("Add", plus, "Dates, Someday"),
    ("Close", close, "Remove, Close"),
    ("Write", pencil, "Edit, Write a question"),
    ("Retake", retake, "Advanced, tests"),
    ("Filter", filt, "Deck filters"),
    ("Show more", chevron, "Advanced, pickers"),
    ("Reminder", bell, "Daily reminder"),
    ("Dates", calendar, "Plans, special dates"),
    ("Sign out", signout, "Advanced"),
]

SLATE = 0x5B6384

if __name__ == "__main__":
    for name, fn, _ in ICONS:
        B.render(SLATE, fn).save(B.RES / f"icon_{name.lower().replace(' ', '_')}.png", optimize=True)

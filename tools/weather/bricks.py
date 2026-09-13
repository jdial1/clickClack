"""The weather bricks on Today, drawn onto the pack's plain special brick.

Each is the pack's extra_dirt brick recoloured, with a symbol pressed into the
face the way the character bricks' faces are: flat shapes with a slight inset
shadow. Snowy wears the pack's own snow top.

    python tools/weather/bricks.py path/to/kenney_toy-brick-pack.zip

Writes weather_<name>.png into app/src/main/res/drawable-nodpi.
"""
import colorsys
import io
import math
import sys
import zipfile
from collections import Counter
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

RES = Path(__file__).resolve().parents[2] / "app/src/main/res/drawable-nodpi"
z = zipfile.ZipFile(sys.argv[1])


def sprite(name):
    return Image.open(io.BytesIO(z.read(f"PNG/Double/Special/{name}.png"))).convert("RGBA")


BASE = sprite("extra_dirt")
SNOWCAP = sprite("extra_stone_top")


def hexrgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(rgb, k):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    return tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s, max(0, min(1, v * k))))


def recolour(im, target):
    body = Counter(p[:3] for p in im.getdata() if p[3] == 255).most_common(1)[0][0]
    _, bs, bv = colorsys.rgb_to_hsv(*(c / 255 for c in body))
    th, ts, tv = colorsys.rgb_to_hsv(*(c / 255 for c in target))
    out = im.copy()
    px = out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            nv = min(1, v / bv * tv)
            ns = min(1, s * ts / bs) if bs else ts
            px[x, y] = tuple(round(c * 255) for c in colorsys.hsv_to_rgb(th, ns, nv)) + (a,)
    return out


# Drawing happens at 4x on a 512 canvas, in brick coordinates (0..128).
K = 4


class Ink:
    def __init__(self):
        self.layers = []  # (mask, colour)

    def layer(self, colour):
        m = Image.new("L", (128 * K, 128 * K), 0)
        self.layers.append((m, colour))
        return ImageDraw.Draw(m)


def P(*xy):
    return [v * K for v in xy]


def cloud(d, cx, cy, w):
    """A Kenney-ish rounded cloud: three bumps on a flat-bottomed base."""
    h = w * 0.34
    d.rounded_rectangle(P(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), radius=h / 2 * K, fill=255)
    r1, r2 = w * 0.22, w * 0.3
    d.ellipse(P(cx - w * 0.28 - r1, cy - h / 2 - r1 * 0.9, cx - w * 0.28 + r1, cy - h / 2 + r1 * 1.1), fill=255)
    d.ellipse(P(cx + w * 0.06 - r2, cy - h / 2 - r2 * 1.1, cx + w * 0.06 + r2, cy - h / 2 + r2 * 0.9), fill=255)


def sun(d, cx, cy, r, rays=8, ray=10):
    d.ellipse(P(cx - r, cy - r, cx + r, cy + r), fill=255)
    for i in range(rays):
        a = i * 2 * math.pi / rays
        x1, y1 = cx + math.cos(a) * (r + 5), cy + math.sin(a) * (r + 5)
        x2, y2 = cx + math.cos(a) * (r + 5 + ray), cy + math.sin(a) * (r + 5 + ray)
        d.line(P(x1, y1, x2, y2), fill=255, width=6 * K)
        for x, y in ((x1, y1), (x2, y2)):
            d.ellipse(P(x - 3, y - 3, x + 3, y + 3), fill=255)


def drop(d, cx, cy, s=6):
    d.ellipse(P(cx - s, cy, cx + s, cy + 2 * s), fill=255)
    d.polygon(P(cx - s * 0.95, cy + s * 0.8, cx, cy - s * 1.3, cx + s * 0.95, cy + s * 0.8), fill=255)


def flake(d, cx, cy, r):
    for i in range(3):
        a = i * math.pi / 3
        dx, dy = math.cos(a) * r, math.sin(a) * r
        d.line(P(cx - dx, cy - dy, cx + dx, cy + dy), fill=255, width=4 * K)
    d.ellipse(P(cx - 4, cy - 4, cx + 4, cy + 4), fill=255)


def star(d, cx, cy, r):
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.45
        pts += [cx + math.cos(a) * rr, cy + math.sin(a) * rr]
    d.polygon(P(*pts), fill=255)


def render(body_hex, draw_fn, cap=False):
    body = hexrgb(body_hex)
    brick = recolour(BASE, body)
    ink = Ink()
    draw_fn(ink, body)
    big = brick.resize((128 * K, 128 * K), Image.NEAREST)
    alpha = big.getchannel("A")
    for mask, colour in ink.layers:
        mask = ImageChops.multiply(mask, alpha)
        # Inset shadow a touch below, like the pack's recessed faces.
        shadow = Image.new("L", mask.size, 0)
        shadow.paste(mask, (0, 3 * K))
        shadow = ImageChops.multiply(ImageChops.subtract(shadow, mask), alpha)
        big.paste(Image.new("RGBA", big.size, shade(body, 0.72) + (255,)), (0, 0), shadow)
        big.paste(Image.new("RGBA", big.size, colour + (255,)), (0, 0), mask)
    out = big.resize((128, 128), Image.LANCZOS)
    if cap:
        # The pack's own snow top, laid over the brick.
        white = SNOWCAP.copy()
        px = white.load()
        for y in range(128):
            for x in range(128):
                r, g, b, a = px[x, y]
                if not (a and min(r, g, b) > 200):
                    px[x, y] = (0, 0, 0, 0)
        out.alpha_composite(white)
    return out


WHITE = (250, 252, 255)
PALE_Y = (255, 236, 150)
SUN_Y = (255, 205, 60)
DROP = (190, 228, 255)
GREY = (205, 210, 225)


def sunny(ink, body):
    sun(ink.layer(WHITE), 64, 70, 20, ray=9)


def partly(ink, body):
    sun(ink.layer(SUN_Y), 50, 58, 15, ray=7)
    cloud(ink.layer(WHITE), 72, 86, 62)


def cloudy(ink, body):
    cloud(ink.layer(shade(body, 1.25)), 48, 60, 50)
    cloud(ink.layer(WHITE), 72, 88, 66)


def rainy(ink, body):
    cloud(ink.layer(WHITE), 64, 60, 72)
    d = ink.layer(DROP)
    for x, y in ((44, 86), (64, 96), (84, 86)):
        drop(d, x, y)


def stormy(ink, body):
    cloud(ink.layer(GREY), 64, 58, 76)
    d = ink.layer(SUN_Y)
    d.polygon(P(68, 72, 52, 98, 64, 98, 56, 118, 80, 88, 68, 88, 76, 72), fill=255)


def snowy(ink, body):
    d = ink.layer(WHITE)
    flake(d, 44, 82, 13)
    flake(d, 84, 90, 10)
    flake(d, 66, 108, 7)


def windy(ink, body):
    d = ink.layer(WHITE)
    for y, w, curl in ((56, 70, 12), (78, 86, 0), (100, 58, 10)):
        x0 = 64 - w / 2
        d.line(P(x0, y, x0 + w - curl, y), fill=255, width=7 * K)
        d.ellipse(P(x0 - 3.5, y - 3.5, x0 + 3.5, y + 3.5), fill=255)
        if curl:
            ex = x0 + w - curl
            d.arc(P(ex - curl, y - 2 * curl, ex + curl, y), start=-90, end=90, fill=255, width=7 * K)
        else:
            d.ellipse(P(x0 + w - 3.5, y - 3.5, x0 + w + 3.5, y + 3.5), fill=255)


def foggy(ink, body):
    d = ink.layer(WHITE)
    for y, a, b in ((50, 22, 92), (70, 34, 106), (90, 18, 84), (110, 40, 100)):
        d.rounded_rectangle(P(a, y - 5, b, y + 5), radius=5 * K, fill=255)


def hot(ink, body):
    sun(ink.layer(PALE_Y), 64, 58, 16, ray=7)
    d = ink.layer(WHITE)
    for x in (40, 64, 88):
        pts = []
        for i in range(21):
            t = i / 20
            pts += [x + math.sin(t * 2 * math.pi) * 4, 90 + t * 26]
        d.line(P(*pts), fill=255, width=5 * K, joint="curve")


def cold(ink, body):
    d = ink.layer(WHITE)
    # Icicles hanging from the top edge of the face.
    for x, l in ((22, 30), (40, 20), (58, 38), (78, 24), (96, 32), (112, 18)):
        d.polygon(P(x - 8, 20, x + 8, 20, x, 20 + l), fill=255)
    d.rectangle(P(0, 16, 128, 24), fill=255)
    flake(ink.layer(shade(body, 0.55)), 64, 92, 16)


def night(ink, body):
    d = ink.layer(PALE_Y)
    d.ellipse(P(38, 50, 86, 98), fill=255)
    d.ellipse(P(54, 42, 100, 88), fill=0)
    s = ink.layer(WHITE)
    star(s, 92, 92, 8)
    star(s, 100, 56, 5)


BLOCKS = [
    ("Sunny", 0xFFB733, sunny, False),
    ("Partly cloudy", 0x4FA8EA, partly, False),
    ("Cloudy", 0x9AA3C7, cloudy, False),
    ("Rainy", 0x108BE2, rainy, False),
    ("Stormy", 0x4A4F6B, stormy, False),
    ("Snowy", 0x7FC4EE, snowy, True),
    ("Windy", 0x34C3A0, windy, False),
    ("Foggy", 0xA3A6B8, foggy, False),
    ("Hot", 0xF54E4F, hot, False),
    ("Cold", 0x9ED8F5, cold, False),
    ("Clear night", 0x2E3A7A, night, False),
]


if __name__ == "__main__":
    for name, h, fn, cap in BLOCKS:
        slug = name.lower().replace(" ", "_")
        render(h, fn, cap).save(RES / f"weather_{slug}.png", optimize=True)

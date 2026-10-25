# 16x16 item icons drawn from 8-tone palettes, lightest first, like the vanilla ingots
from PIL import Image

MC = "/workspace/mods/mc261src/assets/minecraft/textures/"
MMS = "/workspace/mods/morestorage-261/common/src/main/resources/assets/muchmorestorage/textures/"

def ramp_of(path):
    im = Image.open(path).convert("RGBA")
    return sorted(set(p[:3] for p in im.getdata() if p[3] > 0), key=lambda p: -sum(p))

def ramp_from(col):
    lift = max(0, 44 - max(col))
    col = tuple(v + lift for v in col)
    return [tuple(min(255, int(v * f)) for v in col) for f in (1.45, 1.25, 1.1, 1.0, 0.88, 0.76, 0.64, 0.42)]

def mean_of(path):
    px = [p[:3] for p in Image.open(path).convert("RGBA").getdata() if p[3] > 0]
    return tuple(sum(p[i] for p in px) // len(px) for i in range(3))

def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def outline(t, col):
    px = t.load()
    add = []
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                a, b = x + dx, y + dy
                if 0 <= a < 16 and 0 <= b < 16 and px[a, b][3]:
                    add.append((x, y))
                    break
    for p in add:
        px[p] = col + (255,)

def sheet(p):
    # square 14x14 plate: dark rim, lit top/left and shaded bottom/right bevel, diagonal shine and a corner glint
    t = blank()
    px = t.load()
    x0, y0, x1, y1 = 2, 2, 13, 13
    n = (x1 - x0) + (y1 - y0)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            s = (x - x0) + (y - y0)
            f = s / n
            k = 2 if f < 0.2 else 3 if f < 0.78 else 4
            c = s - int(n * 0.42)
            if c in (0, 1):
                k = 1
            elif c == 2:
                k = 2
            elif s - int(n * 0.66) == 0:
                k = 2
            if x == x0 or y == y0:
                k = 1
            if x == x1 or y == y1:
                k = 5
            px[x, y] = p[k] + (255,)
    px[x0, y0] = p[0] + (255,)
    px[x0 + 1, y0 + 1] = p[0] + (255,)
    px[x1, y0] = p[3] + (255,)
    px[x0, y1] = p[3] + (255,)
    outline(t, p[7])
    return t

def worktop(p, kind):
    t = blank()
    px = t.load()
    for i in range(6):
        y, l = 3 + i, 6 - i
        r = l + 8
        for x in range(l, r + 1):
            d = x - l + i
            if kind == "wood":
                k = 2 if (x - l) // 2 % 2 == 0 else 4
            elif kind == "metal":
                k = 1 if d in (6, 7) and 0 < i < 5 else 3
            else:
                k = 3
            if i == 0 or x == l:
                k = min(k, 2) if kind != "wood" else 1
            px[x, y] = p[k] + (255,)
        for j in (1, 2):
            px[r + 1, y + j] = p[6] + (255,)
        px[r + 1, y] = p[5] + (255,)
    for x in range(1, 10):
        px[x, 9] = p[4] + (255,)
        px[x, 10] = p[5] + (255,)
    if kind == "wood":
        for x in range(2, 10, 3):
            px[x, 10] = p[6] + (255,)
    outline(t, p[7])
    return t

def panels(p, kind):
    t = blank()
    px = t.load()
    def plate(x0, y0, x1, y1):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                k = 3
                if kind == "wood" and (x - x0) % 3 == 2:
                    k = 4
                if x == x0 or y == y0:
                    k = 1
                if x == x1 or y == y1:
                    k = 5
                if kind == "metal" and (x, y) in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
                    k = 6
                px[x, y] = p[k] + (255,)
    plate(3, 2, 10, 10)
    for x in range(2, 12):
        px[x, 11] = (0, 0, 0, 0)
    plate(6, 5, 13, 13)
    outline(t, p[7])
    return t

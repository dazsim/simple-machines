# Powered tools and the power cell, drawn along the item diagonal like the vanilla tools
from icons import blank, outline

STEEL = [(226, 230, 238), (186, 192, 204), (150, 156, 170), (112, 118, 132), (78, 82, 96)]
IRON = [(220, 220, 220), (180, 180, 180), (140, 140, 144), (100, 100, 106)]
RC = [(244, 120, 92), (200, 60, 52), (126, 30, 34)]
GRIP = [(84, 76, 70), (60, 54, 50), (40, 36, 34)]
OUT = (28, 30, 38)

def fill(t, along, across, shade):
    for y in range(16):
        for x in range(16):
            a, c = x - y, x + y
            if along[0] <= a <= along[1] and across[0] <= c <= across[1]:
                col = shade(a, c)
                if col: t.putpixel((x, y), col + (255,))

def band(pal, lo, hi):
    return lambda a, c: pal[min(len(pal) - 1, (c - lo) * len(pal) // (hi - lo + 1))]

def base(t):
    fill(t, (-13, -6), (14, 16), band(GRIP, 14, 16))
    def body(a, c):
        if a in (-4, -3) and 13 <= c <= 18: return RC[0] if c < 15 else RC[1] if c < 17 else RC[2]
        if a in (-1, 0) and c in (13, 14): return (120, 230, 140)
        return STEEL[min(4, (c - 12) * 5 // 8)]
    fill(t, (-7, 1), (12, 19), body)

def drill():
    t = blank()
    def bit(a, c):
        w = max(0, (14 - a) // 3)
        if abs(c - 15.5) > w + 0.5: return None
        ramp = IRON if (a + c // 2) % 3 else [IRON[2], IRON[3], IRON[3], IRON[3]]
        return ramp[min(3, int(c - 15.5 + w + 0.5) * 4 // (2 * w + 2))]
    fill(t, (2, 14), (10, 21), bit)
    base(t)
    outline(t, OUT)
    return t

def chainsaw():
    t = blank()
    def bar(a, c):
        if c in (12, 19): return (34, 34, 40) if a % 2 == 0 and a < 14 else None
        if a >= 14 and c not in (15, 16): return None
        if c in (13, 18): return (54, 54, 60)
        return IRON[{14: 1, 15: 1, 16: 2, 17: 3}[c]]
    fill(t, (2, 15), (12, 19), bar)
    base(t)
    outline(t, OUT)
    return t

def shovel():
    t = blank()
    fill(t, (2, 7), (14, 17), band([STEEL[1], STEEL[2], STEEL[4]], 14, 17))
    def blade(a, c):
        d = abs(c - 15.5)
        if a <= 8 and d > 2.5: return None
        if a >= 13 and d > 3.5 - (a - 13) * 1.2: return None
        return STEEL[min(4, int(c - 11) * 5 // 10)]
    fill(t, (7, 16), (11, 20), blade)
    base(t)
    outline(t, OUT)
    return t

def cell():
    t = blank()
    for y in range(3, 14):
        for x in range(5, 11):
            t.putpixel((x, y), STEEL[[1, 1, 2, 2, 3, 4][x - 5]] + (255,))
    for x in range(5, 11):
        t.putpixel((x, 3), STEEL[0] + (255,)); t.putpixel((x, 13), STEEL[4] + (255,))
    for y in range(6, 11):
        for x in range(6, 10):
            t.putpixel((x, y), RC[1 if x < 8 else 2] + (255,))
    t.putpixel((6, 6), RC[0] + (255,))
    t.putpixel((7, 2), (236, 146, 104, 255)); t.putpixel((8, 2), (196, 108, 76, 255))
    outline(t, OUT)
    return t

# Machine upgrades: an item icon and a flat card texture for the chassis renderer
PCB = [(46, 120, 84), (34, 92, 64), (24, 66, 48)]
GOLD = [(250, 218, 92), (214, 168, 40)]
CHIP = (30, 30, 36)

def _card(t, x0, y0, x1, y1, face):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (y0, y1)
            t.putpixel((x, y), (PCB[2] if edge else face(x, y)) + (255,))

def _overclock_face(x, y):
    if 5 <= x <= 10 and 5 <= y <= 10:
        return (200, 60, 52) if (x, y) in ((7, 7), (8, 8)) else CHIP
    if y in (3, 12) and x % 2 == 1 or x in (3, 12) and y % 2 == 1:
        return GOLD[0]
    return PCB[0] if (x + y) % 5 else PCB[1]

def _cell_face(x, y):
    if 4 <= x <= 11 and 3 <= y <= 12:
        if x in (4, 11) or y in (3, 12):
            return STEEL[3]
        return RC[0] if y >= 6 and y % 2 == 0 else RC[1] if y >= 6 else STEEL[1]
    if y in (1, 14) and 6 <= x <= 9:
        return GOLD[1]
    return PCB[0] if (x + y) % 5 else PCB[1]

def overclocker():
    t = blank()
    _card(t, 1, 2, 14, 13, _overclock_face)
    for x in (4, 6, 9, 11):
        t.putpixel((x, 14), GOLD[1] + (255,))
    outline(t, OUT)
    return t

def energy_cell():
    t = blank()
    _card(t, 1, 0, 14, 15, _cell_face)
    return t

def card(kind):
    t = blank()
    _card(t, 0, 0, 15, 15, _overclock_face if kind == "overclocker_circuit" else _cell_face)
    return t

#!/usr/bin/env python3
# blueprint manual for Simple Books: pages, template, drawings, recipe and advancement; run from repo root (gen_assets.py calls it)
import shutil
import json, math, os
from PIL import Image, ImageDraw, ImageFont

MOD = "simplemachines"
ROOT = "common/src/main/resources"
A, D = f"{ROOT}/assets/{MOD}", f"{ROOT}/data/{MOD}"
BOOK = f"{A}/simplebooks/books/manual"
TEX = f"{A}/textures/simplebooks/manual"
T = f"{MOD}:textures/simplebooks/manual"
W, H, SS = 640, 400, 3
INK, SOFT, FILL = (255, 255, 255, 255), (200, 220, 240, 255), (29, 79, 140, 255)
FONTS = ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "DejaVuSans-Bold.ttf"]
COND = {"fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": ["simplebooks"]}],
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "simplebooks"}]}
GUIDE = {"simplebooks:book": f"{MOD}:manual", "minecraft:item_model": f"{MOD}:manual"}

def jdump(obj, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")

def font(size):
    for p in FONTS:
        try: return ImageFont.truetype(p, size * SS)
        except OSError: pass
    return ImageFont.load_default(size * SS)

class Sheet:
    def __init__(self):
        self.im = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def s(self, pts): return [(x * SS, y * SS) for x, y in pts]

    def line(self, a, b, w=2.0, c=INK):
        self.d.line(self.s([a, b]), fill=c, width=round(w * SS))
        for x, y in self.s([a, b]):
            r = w * SS / 2
            self.d.ellipse([x - r, y - r, x + r, y + r], fill=c)

    def poly(self, pts, w=2.0, c=INK, fill=FILL):
        if fill: self.d.polygon(self.s(pts), fill=fill)
        for a, b in zip(pts, pts[1:] + pts[:1]): self.line(a, b, w, c)

    def dash(self, a, b, w=1.4, c=SOFT, on=6, off=5):
        L = math.dist(a, b); n = int(L // (on + off)) + 1
        for i in range(n):
            s0, s1 = i * (on + off) / L, min(1, (i * (on + off) + on) / L)
            if s0 >= 1: break
            self.line((a[0] + (b[0] - a[0]) * s0, a[1] + (b[1] - a[1]) * s0), (a[0] + (b[0] - a[0]) * s1, a[1] + (b[1] - a[1]) * s1), w, c)

    def arrow(self, a, b, w=2.0, c=INK, head=9):
        self.line(a, b, w, c)
        ang = math.atan2(b[1] - a[1], b[0] - a[0])
        p1 = (b[0] - head * math.cos(ang - 0.45), b[1] - head * math.sin(ang - 0.45))
        p2 = (b[0] - head * math.cos(ang + 0.45), b[1] - head * math.sin(ang + 0.45))
        self.d.polygon(self.s([b, p1, p2]), fill=c)

    def text(self, xy, t, size=13, c=INK, anchor="mm"):
        self.d.text((xy[0] * SS, xy[1] * SS), t, font=font(size), fill=c, anchor=anchor)

    def tag(self, n, xy, r=11):
        x, y = xy
        self.d.ellipse([(x - r) * SS, (y - r) * SS, (x + r) * SS, (y + r) * SS], fill=FILL, outline=INK, width=2 * SS)
        self.text(xy, str(n), 12)

    def circle(self, xy, r, w=2.0, fill=FILL):
        x, y = xy
        self.d.ellipse([(x - r) * SS, (y - r) * SS, (x + r) * SS, (y + r) * SS], fill=fill, outline=INK, width=round(w * SS))

    def save(self, name):
        os.makedirs(TEX, exist_ok=True)
        self.im.resize((W, H), Image.LANCZOS).save(f"{TEX}/{name}.png")

# isometric: x runs right and down, z runs left and down, y up
def iso(o, k=1.0):
    ox, oy = o
    c, s = math.cos(math.radians(30)) * k, math.sin(math.radians(30)) * k
    return lambda x, y, z: (ox + (x - z) * c, oy + (x + z) * s - y * k)

def box(sh, P, x, y, z, w, h, d, lw=2.0, fill=FILL):
    top = [P(x, y + h, z), P(x + w, y + h, z), P(x + w, y + h, z + d), P(x, y + h, z + d)]
    right = [P(x + w, y, z), P(x + w, y + h, z), P(x + w, y + h, z + d), P(x + w, y, z + d)]
    left = [P(x, y, z + d), P(x + w, y, z + d), P(x + w, y + h, z + d), P(x, y + h, z + d)]
    for f in (top, right, left): sh.poly(f, lw, fill=fill)

def face_rect(sh, P, side, a0, b0, a1, b1, depth, lw=1.6):
    # a rectangle drawn on the +z (left, a=x) or +x (right, a=z) face, b is height
    if side == "z": pts = [P(a0, b0, depth), P(a1, b0, depth), P(a1, b1, depth), P(a0, b1, depth)]
    else: pts = [P(depth, b0, a0), P(depth, b0, a1), P(depth, b1, a1), P(depth, b1, a0)]
    sh.poly(pts, lw, fill=None)

def machine(sh, P, s=1.0, x=0, z=0, port=None):
    box(sh, P, x, 0, z, 100 * s, 82 * s, 100 * s)
    box(sh, P, x - 4 * s, 82 * s, z - 4 * s, 108 * s, 10 * s, 108 * s)
    face_rect(sh, P, "z", x + 22 * s, 18 * s, x + 78 * s, 62 * s, z + 100 * s)
    for i in range(3):
        a = P(x + 30 * s + i * 20 * s, 26 * s, z + 100 * s); b = P(x + 30 * s + i * 20 * s, 54 * s, z + 100 * s)
        sh.line(a, b, 1.4, SOFT)
    for zz in (z + 18 * s, z + 82 * s):
        sh.line(P(x + 100 * s, 6 * s, zz), P(x + 100 * s, 76 * s, zz), 1.4, SOFT)
    if port: face_rect(sh, P, "x", z + 36 * s, 28 * s, z + 64 * s, 54 * s, x + 100 * s, 2.0)

def chest(sh, P, x, z, s=1.0):
    box(sh, P, x, 0, z, 70 * s, 50 * s, 70 * s)
    sh.line(P(x, 38 * s, z + 70 * s), P(x + 70 * s, 38 * s, z + 70 * s), 1.4)
    sh.line(P(x + 70 * s, 38 * s, z), P(x + 70 * s, 38 * s, z + 70 * s), 1.4)

def d_intro():
    sh = Sheet(); P = iso((300, 250), 1.25)
    machine(sh, P)
    sh.text((300, 30), "STEEL CHASSIS MACHINE", 14)
    sh.dash(P(-4, 92, 104), (130, 120)); sh.text((95, 112), "worktop", 12, SOFT)
    sh.dash(P(50, 40, 100), (140, 300)); sh.text((100, 312), "front panel", 12, SOFT)
    sh.dash(P(100, 40, 50), (520, 300)); sh.text((560, 312), "side panels", 12, SOFT)
    sh.dash(P(100, 92, 0), (500, 90)); sh.text((540, 82), "chassis", 12, SOFT)
    sh.save("intro")

def d_metals():
    sh = Sheet(); P = iso((300, 260), 1.15)
    box(sh, P, 0, 0, 0, 100, 100, 100)
    face_rect(sh, P, "z", 30, 14, 70, 44, 100)
    for i in range(4): sh.line(P(4, 60 + i * 9, 100), P(96, 60 + i * 9, 100), 1.2, SOFT)
    sh.arrow((120, 70), P(20, 104, 30)); sh.text((112, 58), "copper", 12)
    sh.arrow((470, 60), P(80, 104, 20)); sh.text((480, 48), "redstone", 12)
    sh.arrow(P(100, 20, 60), (540, 330)); sh.text((560, 345), "redstone copper", 12)
    sh.text((300, 380), "Alloy Furnace: two inputs, fuel below", 12, SOFT)
    sh.save("metals")

def d_chassis():
    sh = Sheet()
    for n, (ox, label) in enumerate([(170, "IRON  x1"), (470, "STEEL  x1.5 speed, x2 energy")]):
        P = iso((ox, 240), 1.0)
        box(sh, P, 0, 0, 0, 100, 8, 100)
        for xx, zz in [(0, 0), (88, 0), (0, 88), (88, 88)]:
            box(sh, P, xx, 8, zz, 12, 92 if n else 80, 12)
        sh.text((ox, 380), label, 12)
    sh.text((320, 30), "CHASSIS TIERS", 14)
    sh.save("chassis")

def d_assembly():
    sh = Sheet(); P = iso((230, 290), 0.7)
    parts = [("chassis", 0, 8), ("assembly", 40, 34), ("control board", 110, 6), ("upgrades", 150, 4), ("panels", 190, 80), ("worktop", 300, 10)]
    for i, (name, y, h) in enumerate(parts):
        if i == 0:
            box(sh, P, 0, 0, 0, 100, 8, 100)
            for xx, zz in [(88, 0), (0, 88), (88, 88)]: box(sh, P, xx, 8, zz, 12, 22, 12)
        elif name == "assembly": box(sh, P, 20, y, 20, 60, h, 60)
        elif name == "control board": box(sh, P, 10, y, 30, 80, h, 40)
        elif name == "upgrades":
            box(sh, P, 22, y, 40, 22, 14, 4); box(sh, P, 56, y, 40, 22, 14, 4)
        elif name == "panels":
            box(sh, P, 100, y - 30, 0, 4, h, 100); box(sh, P, 0, y - 30, 100, 100, h, 4)
        else: box(sh, P, -4, y, -4, 108, h, 108)
        tx, ty = P(110, y + h / 2, 110)
        sh.dash((tx, ty), (430, ty)); sh.tag(i, (444, ty)); sh.text((464, ty), name, 12, INK, "lm")
    sh.dash(P(50, 10, 50), P(50, 300, 50), 1.2)
    sh.save("assembly")

def d_finishes():
    sh = Sheet()
    for i, kind in enumerate(["wood", "concrete", "metal"]):
        P = iso((130 + i * 190, 250), 0.9)
        box(sh, P, 0, 0, 0, 100, 100, 6)
        for j in range(1, 6):
            if kind == "wood": sh.line(P(0, j * 16, 6), P(100, j * 16 + (4 if j % 2 else -3), 6), 1.2, SOFT)
            if kind == "metal":
                for xx in (10, 90): sh.circle(P(xx, j * 16, 6), 2.0, 1.2)
        if kind == "concrete": face_rect(sh, P, "z", 8, 8, 92, 92, 6, 1.2)
        sh.text((130 + i * 190, 330), kind, 13)
    P = iso((320, 110), 0.9); box(sh, P, 0, 0, 0, 120, 12, 120)
    sh.text((320, 30), "PANELS AND WORKTOPS", 14)
    sh.save("finishes")

def d_machines():
    sh = Sheet(); P = iso((300, 215), 1.0)
    a, b = P(-130, 20, 50), P(20, 20, 50)
    for t in (-3, 3): sh.line((a[0], a[1] + t), (b[0], b[1] + t), 2.0)
    sh.text((a[0] - 30, a[1] - 18), "FE in", 12, SOFT)
    machine(sh, P, port=True)
    chest(sh, P, 150, 15)
    sh.arrow(P(104, 40, 50), P(146, 40, 50), 2.4)
    sh.arrow(P(50, 170, 50), P(50, 100, 50), 2.4)
    sh.text(P(50, 190, 50), "input", 12, SOFT)
    sh.dash(P(100, 70, 40), (500, 150)); sh.text((550, 140), "output port", 12, SOFT)
    sh.text((320, 30), "ONE MACHINE, ANY SIDE", 14)
    sh.save("machines")

def d_farm():
    sh = Sheet(); P = iso((400, 110), 0.55)
    for i in range(5):
        for j in range(5):
            x0, z0 = -150 + i * 80, 130 + j * 80
            sh.poly([P(x0, 0, z0), P(x0 + 76, 0, z0), P(x0 + 76, 0, z0 + 76), P(x0, 0, z0 + 76)], 1.4, SOFT)
            for k in (-14, 0, 14): sh.line(P(x0 + 38 + k, 0, z0 + 38), P(x0 + 38 + k, 22, z0 + 38), 1.4)
    machine(sh, P, s=1.0, x=0, z=0)
    sh.text((320, 30), "CROP HARVESTER AND ITS AREA", 14)
    sh.text((320, 375), "5x5 in front of the machine, 9x9 on a steel chassis", 12, SOFT)
    sh.save("farm")

def d_power():
    sh = Sheet(); P = iso((150, 170), 0.62)
    box(sh, P, 0, 0, 0, 70, 70, 70); sh.circle(P(35, 35, 70), 14); sh.text(P(35, 130, 35), "generator", 12)
    a, b = P(70, 10, 35), P(260, 10, 35)
    for t in (-3, 3): sh.line((a[0], a[1] + t), (b[0], b[1] + t), 2.0)
    box(sh, P, 260, 0, 0, 70, 70, 70); face_rect(sh, P, "z", 270, 10, 320, 60, 70); sh.text(P(295, 130, 35), "battery", 12)
    a, b = P(330, 10, 35), P(480, 10, 35)
    for t in (-3, 3): sh.line((a[0], a[1] + t), (b[0], b[1] + t), 2.0)
    machine(sh, iso(P(480, 0, 0), 0.62), 0.8)
    sh.arrow(P(120, 40, 35), P(220, 40, 35)); sh.text(P(170, 50, 35), "FE", 12, SOFT)
    sh.arrow(P(370, 40, 35), P(450, 40, 35)); sh.text(P(410, 50, 35), "FE", 12, SOFT)
    sh.text((320, 380), "Redstone Copper Cable: up to 1000 FE/t", 12, SOFT)
    sh.save("power")

def d_voltage():
    sh = Sheet(); P = iso((110, 190), 0.55)
    box(sh, P, 0, 0, 0, 70, 70, 70); sh.circle(P(35, 35, 70), 14); sh.text(P(35, 130, 35), "generator", 12)
    a, b = P(70, 10, 35), P(220, 10, 35)
    for t in (-3, 3): sh.line((a[0], a[1] + t), (b[0], b[1] + t), 2.0)
    sh.text(P(145, 50, 35), "LV", 12, SOFT)
    box(sh, P, 220, 0, 0, 70, 70, 70); face_rect(sh, P, "x", 15, 15, 55, 55, 290); sh.text(P(255, 130, 35), "LV/MV", 12)
    a, b = P(290, 10, 35), P(450, 10, 35)
    for t in (-4, 0, 4): sh.line((a[0], a[1] + t), (b[0], b[1] + t), 2.0)
    sh.text(P(370, 50, 35), "MV", 12, SOFT)
    box(sh, P, 450, 0, 0, 70, 70, 70); face_rect(sh, P, "x", 15, 15, 55, 55, 450); sh.text(P(485, 130, 35), "MV/LV", 12)
    sh.text((320, 380), "front = high side, other faces = low side, 5% loss", 12, SOFT)
    sh.save("voltage")

def d_solar():
    sh = Sheet(); P = iso((120, 230), 0.62)
    for i in range(4):
        x = i * 130
        box(sh, P, x, 0, 0, 100, 50, 100)
        n = (2, 3, 4, 4)[i]; st = 90 / n
        for k in range(1, n):
            sh.line(P(x + 5 + k * st, 50, 5), P(x + 5 + k * st, 50, 95), 1.2, SOFT)
            sh.line(P(x + 5, 50, 5 + k * st), P(x + 95, 50, 5 + k * st), 1.2, SOFT)
        face_rect(sh, P, "z", x + 20, 12, x + 80, 38, 100)
        sh.text(P(x + 50, -40, 100), ("Iron", "Copper", "Gold", "Diamond")[i], 12)
    for i in range(6):
        sh.line((300 + i * 30 - 60, 40), (300 + i * 30 - 75, 75), 1.4, SOFT)
    sh.circle((560, 50), 18)
    sh.text((320, 380), "Open sky above, daylight. Diamond also works at night", 12, SOFT)
    sh.save("solar")

def d_wind():
    sh = Sheet(); P = iso((300, 330), 0.9)
    box(sh, P, 0, 0, 0, 100, 8, 100)
    box(sh, P, 40, 8, 40, 20, 50, 20)
    for k in range(14):
        y = 60 + k * 10
        import math as _m
        a = _m.radians(k * 25)
        dx, dz = 48 * _m.cos(a), 48 * _m.sin(a)
        sh.line(P(50 - dx, y, 50 - dz), P(50 + dx, y, 50 + dz), 2.0)
    sh.line(P(50, 58, 50), P(50, 200, 50), 2.6)
    sh.dash((150, 70), (150, 330)); sh.text((150, 60), "2 blocks", 12, SOFT)
    sh.text((480, 120), "higher = more", 12); sh.text((480, 145), "open air = more", 12); sh.text((480, 170), "rain, thunder = more", 12)
    sh.text((320, 380), "Up to 50 FE/t at Y 160 and above, LV out of the base", 12, SOFT)
    sh.save("wind")

def d_pipes():
    sh = Sheet()
    sh.line((60, 200), (580, 200), 2.0); sh.line((60, 224), (580, 224), 2.0)
    for x, mode in [(140, "input"), (320, "output"), (500, "disconnected")]:
        sh.poly([(x - 30, 252), (x + 30, 252), (x + 30, 330), (x - 30, 330)])
        sh.text((x, 291), "inv", 12, SOFT)
        if mode == "input": sh.arrow((x, 226), (x, 250))
        elif mode == "output": sh.arrow((x, 250), (x, 226))
        else:
            sh.line((x - 8, 230), (x + 8, 246)); sh.line((x + 8, 230), (x - 8, 246))
        sh.text((x, 350), mode, 13)
    for x in range(80, 580, 60): sh.arrow((x, 212), (x + 30, 212), 1.4, SOFT, 6)
    sh.text((320, 60), "PIPE ENDS: USE A SCREWDRIVER TO CYCLE", 14)
    sh.text((320, 90), "input  >  output  >  disconnected  >  input", 12, SOFT)
    sh.save("pipes")

def d_filters():
    sh = Sheet()
    sh.line((60, 200), (580, 200), 2.0); sh.line((60, 224), (580, 224), 2.0)
    for x, label, sub in [(140, "allow: iron", "gets iron first"), (320, "deny: iron", "anything else"), (500, "no filter", "overflow")]:
        sh.poly([(x - 30, 252), (x + 30, 252), (x + 30, 330), (x - 30, 330)])
        sh.text((x, 291), "inv", 12, SOFT)
        sh.poly([(x - 16, 226), (x + 16, 226), (x + 16, 236), (x - 16, 236)], 1.6)
        if x == 500: sh.poly([(x - 16, 226), (x + 16, 226), (x + 16, 236), (x - 16, 236)], 1.0, SOFT)
        sh.arrow((x, 236), (x, 250))
        sh.text((x, 350), label, 13); sh.text((x, 370), sub, 11, SOFT)
    for x in range(80, 580, 60): sh.arrow((x, 212), (x + 30, 212), 1.4, SOFT, 6)
    sh.text((320, 60), "FILTERS ON INPUT ENDS", 14)
    sh.text((320, 90), "matching filters first, then the rest", 12, SOFT)
    sh.save("filters")

def d_fluids():
    sh = Sheet()
    sh.poly([(60, 150), (160, 150), (160, 290), (60, 290)]); sh.text((110, 310), "tank", 12)
    sh.line((160, 200), (480, 200), 2.0); sh.line((160, 224), (480, 224), 2.0)
    for x in range(180, 470, 60): sh.arrow((x, 212), (x + 30, 212), 1.4, SOFT, 6)
    sh.poly([(300, 204), (340, 204), (340, 220), (300, 220)], 1.2, SOFT)
    sh.text((320, 250), "glass window", 11, SOFT)
    sh.poly([(480, 150), (580, 150), (580, 290), (480, 290)]); sh.text((530, 310), "machine", 12)
    sh.text((320, 60), "FLUID PIPE", 14)
    sh.text((320, 90), "same ends and filters as item pipes", 12, SOFT)
    sh.text((320, 370), "the window shows what is flowing", 12, SOFT)
    sh.save("fluids")

def d_tanks():
    sh = Sheet(); P = iso((320, 215), 1.0)
    h, e = 60, 9
    # pipes behind: into the back faces
    box(sh, P, -12, -12, -h - 70, 24, 24, 70, 1.8)
    box(sh, P, -h - 70, -12, -12, 70, 24, 24, 1.8)
    # chamfered cube, faces seen from (1, 1, 1)
    faces = []
    for a in range(3):
        for sa in (1, -1):
            pts = []
            for u, v in ((1, 1), (1, -1), (-1, -1), (-1, 1)):
                q = [0, 0, 0]; q[a] = sa * h; q[(a + 1) % 3] = u * (h - e); q[(a + 2) % 3] = v * (h - e)
                pts.append(q)
            faces.append(("face", (a, sa), pts))
    for a in range(3):
        b = (a + 1) % 3; c = (a + 2) % 3
        for sa in (1, -1):
            for sb in (1, -1):
                pts = []
                for sc in (1, -1):
                    for k, (x0, x1) in enumerate(((h, h - e), (h - e, h))):
                        q = [0, 0, 0]; q[a] = sa * x0; q[b] = sb * x1; q[c] = sc * (h - e)
                        pts.append(q)
                faces.append(("edge", (a, sa, b, sb), [pts[0], pts[1], pts[3], pts[2]]))
    for sx in (1, -1):
        for sy in (1, -1):
            for sz in (1, -1):
                sg = (sx, sy, sz)
                pts = []
                for a in range(3):
                    q = [sg[i] * (h - e) for i in range(3)]; q[a] = sg[a] * h
                    pts.append(q)
                faces.append(("corner", sg, pts))
    def normal(pts):
        c = [sum(p[i] for p in pts) / len(pts) for i in range(3)]
        return c, [x / max(abs(v) for v in c) for x in c]
    def pr(q): return P(q[0], q[1], q[2])
    for kind, key, pts in faces:
        c, n = normal(pts)
        if n[0] + n[1] + n[2] <= 0.01: continue
        o = pr(c)
        ang = lambda q: math.atan2(pr(q)[1] - o[1], pr(q)[0] - o[0])
        sh.poly([pr(q) for q in sorted(pts, key=ang)], 2.0)
        if kind == "edge":
            a, sa, b, sb = key; cc = 3 - a - b
            for t in (-30, 0, 30):
                q = [0, 0, 0]; q[a] = sa * (h - e / 2); q[b] = sb * (h - e / 2); q[cc] = t
                sh.circle(pr(q), 2.6, 1.2)
        if kind == "face":
            a, sa = key; u, v = (a + 1) % 3, (a + 2) % 3
            def fq(x, y):
                q = [0, 0, 0]; q[a] = sa * h; q[u] = x; q[v] = y
                return pr(q)
            w = h - e - 10
            sh.poly([fq(-w, -w), fq(w, -w), fq(w, w), fq(-w, w)], 1.4, SOFT, None)
            if a != 1:
                for yy in range(-w + 6, 4, 9):
                    if a == 2: sh.line(fq(-w + 4, yy), fq(w - 4, yy), 1.0, SOFT)
                    else: sh.line(fq(yy, -w + 4), fq(yy, w - 4), 1.0, SOFT)
    # pipes in front
    box(sh, P, h, -12, -12, 50, 24, 24, 1.8)
    box(sh, P, -12, -12, h, 24, 24, 50, 1.8)
    sh.text((320, 30), "FLUID TANK", 14)
    sh.text((320, 365), "16 buckets, pipes join on any face", 12, SOFT)
    sh.save("tanks")

def d_output():
    sh = Sheet(); P = iso((210, 200), 0.95)
    machine(sh, P, port=True)
    chest(sh, P, 150, 15)
    sh.arrow(P(104, 40, 50), P(146, 40, 50), 2.4)
    sh.text((320, 40), "AUTO-OUTPUT", 14)
    sh.text((320, 370), "screwdriver on a side: outputs are pushed out of the port", 12, SOFT)
    sh.save("output")

def d_upgrades():
    sh = Sheet()
    sh.poly([(60, 110), (580, 110), (580, 300), (60, 300)])
    for x in range(90, 560, 40): sh.line((x, 120), (x, 140), 1.2, SOFT)
    sh.text((150, 270), "CONTROL BOARD", 12, SOFT)
    for i, (label, sub) in enumerate([("OVERCLOCKER", "+50% speed, +50% FE/t"), ("ENERGY CELL", "+100% storage")]):
        x = 260 + i * 160
        sh.poly([(x, 150), (x + 120, 150), (x + 120, 250), (x, 250)])
        sh.poly([(x + 35, 170), (x + 85, 170), (x + 85, 215), (x + 35, 215)], 1.4)
        for k in range(6): sh.line((x + 10 + k * 20, 250), (x + 10 + k * 20, 240), 1.4)
        sh.text((x + 60, 325), label, 12); sh.text((x + 60, 345), sub, 11, SOFT)
        sh.tag(i + 1, (x + 60, 135))
    sh.text((320, 50), "TWO UPGRADE SLOTS", 14)
    sh.save("upgrades")

def tool_body(sh, x, y):
    sh.poly([(x, y), (x + 150, y), (x + 170, y + 20), (x + 170, y + 50), (x, y + 50)])
    sh.poly([(x + 40, y + 50), (x + 80, y + 50), (x + 70, y + 120), (x + 30, y + 120)])
    sh.poly([(x + 20, y + 12), (x + 70, y + 12), (x + 70, y + 30), (x + 20, y + 30)], 1.4)

def d_tools():
    sh = Sheet()
    tool_body(sh, 60, 120); sh.poly([(230, 136), (250, 140), (250, 160), (230, 164)])
    for cy in (128, 150, 172):
        sh.poly([(250, cy - 10), (282, cy - 4), (282, cy + 4), (250, cy + 10)])
    sh.text((150, 290), "drill", 13)
    x = 330; tool_body(sh, x, 120)
    sh.poly([(x + 170, 125), (x + 300 - 70, 125), (x + 250, 145), (x + 230, 165), (x + 170, 165)])
    sh.text((x + 100, 290), "chainsaw", 13)
    sh.text((320, 40), "POWERED TOOLS", 14); sh.text((320, 340), "built on a Tool Stand, recharge in the Charger", 12, SOFT)
    sh.save("tools")

def textures():
    g = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(g)
    for i in range(0, 64, 16):
        c = (156, 192, 228, 110 if i == 0 else 45)
        d.line([(i, 0), (i, 63)], fill=c); d.line([(0, i), (63, i)], fill=c)
    g.save(f"{TEX}/grid.png")
    c = Image.new("RGBA", (16, 16), (22, 54, 94, 255)); ImageDraw.Draw(c).rectangle([0, 0, 15, 15], outline=(12, 30, 54, 255)); c.save(f"{TEX}/stand_cover.png")
    p = Image.new("RGBA", (16, 16), (29, 79, 140, 255)); d = ImageDraw.Draw(p)
    for i in (0, 8): d.line([(i, 0), (i, 15)], fill=(60, 110, 170, 255)); d.line([(0, i), (15, i)], fill=(60, 110, 170, 255))
    p.save(f"{TEX}/stand_page.png")
    it = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(it)
    d.rectangle([2, 1, 13, 14], fill=(29, 79, 140, 255), outline=(12, 30, 54, 255))
    d.rectangle([3, 13, 13, 14], fill=(230, 236, 244, 255)); d.line([(3, 1), (3, 13)], fill=(12, 30, 54, 255))
    d.rectangle([6, 4, 11, 8], outline=(255, 255, 255, 255)); d.line([(7, 10), (11, 10)], fill=(200, 220, 240, 255))
    d.point((8, 6), fill=(255, 230, 128, 255))
    os.makedirs(f"{A}/textures/item", exist_ok=True); it.save(f"{A}/textures/item/manual.png")

def recipe(name):
    return json.load(open(f"{D}/recipe/{name}.json"))

def out(r):
    res = r["result"]; c = res.get("count", 1)
    return {"item": res["id"], "count": c} if c > 1 else res["id"]

def ing(x):
    i = x["ingredient"]; c = x.get("count", 1)
    return {"item": i, "count": c} if c > 1 else i

# vanilla names that are not just the id in title case, and items standing in for common tags
VANILLA = {"redstone_block": "Block of Redstone", "quartz": "Nether Quartz", "redstone": "Redstone Dust",
           "iron_block": "Block of Iron", "gold_block": "Block of Gold", "lapis_lazuli": "Lapis Lazuli"}
TAGS = {"c:dusts/redstone": "minecraft:redstone", "c:gems/quartz": "minecraft:quartz", "c:glass_blocks": "minecraft:glass",
        "c:ingots/copper": "minecraft:copper_ingot", "c:ingots/gold": "minecraft:gold_ingot", "c:ingots/iron": "minecraft:iron_ingot",
        "c:gems/diamond": "minecraft:diamond", "c:ender_pearls": "minecraft:ender_pearl",
        "c:storage_blocks/iron": "minecraft:iron_block", "c:storage_blocks/gold": "minecraft:gold_block",
        "c:gems/lapis": "minecraft:lapis_lazuli"}

def name_of(ref):
    if ref.startswith("#"):
        t = ref[1:]
        if t in TAGS: ref = TAGS[t]
        else:
            ns, path = t.split(":", 1)
            ref = json.load(open(f"{ROOT}/data/{ns}/tags/item/{path}.json"))["values"][0]
    ns, i = ref.split(":", 1)
    if ns == MOD:
        lang = json.load(open(f"{A}/lang/en_us.json"))
        return lang.get(f"item.{MOD}.{i}") or lang[f"block.{MOD}.{i}"]
    return VANILLA.get(i, i.replace("_", " ").title())

def named(pattern, key):
    counts = {}
    for row in pattern:
        for ch in row:
            if ch in key: counts[ch] = counts.get(ch, 0) + 1
    return ", ".join(f"{n}x {name_of(key[ch])}" for ch, n in counts.items())

def craft(name, title=None):
    r = recipe(name)
    pat = [row.ljust(3) for row in r["pattern"]] + ["   "] * (3 - len(r["pattern"]))
    s = {"kind": "craft_shaped", "pattern": pat, "key": r["key"], "output": out(r), "note": "Crafting Table\n" + named(pat, r["key"])}
    if title: s["title"] = title
    return s

def process(name, appliance, note=None):
    r = recipe(name)
    ins = [ing(r[k]) for k in ("first", "second", "input") if k in r]
    s = {"kind": "process", "inputs": ins, "appliance": f"{MOD}:{appliance}", "output": out(r)}
    if note: s["note"] = note
    return s

def compress(name, appliance="compressor", note=None):
    r = recipe(name)
    n = r.get("input_count", 1); c = r.get("count", 1)
    s = {"kind": "process", "inputs": [{"item": r["ingredient"], "count": n} if n > 1 else r["ingredient"]],
         "appliance": f"{MOD}:{appliance}", "output": {"item": r["result"], "count": c} if c > 1 else r["result"]}
    if note: s["note"] = note
    return s

def text(t): return {"kind": "text", "text": t}

def build(assembly, machine, title):
    return {"kind": "process", "title": title, "inputs": [f"{MOD}:{assembly}"], "on": {"items": [f"{MOD}:iron_chassis", f"{MOD}:steel_chassis"], "name": "Chassis"},
            "output": f"{MOD}:{machine}", "note": "Fit it to a chassis"}

def seq(title, cells, t): return {"kind": "sequence", "title": title, "cells": cells, "text": t}

def page(name, section, sort, title, sub, body, drawing, caption, right_title, steps, **kw):
    p = {"section": section, "sort": sort, "title": title, "subtitle": sub, "text": body,
         "drawing": f"{T}/{drawing}.png", "caption": caption, "right_title": right_title, "steps": steps}
    p.update(kw)
    jdump(p, f"{BOOK}/pages/{name}.json")

def pages():
    shutil.rmtree(f"{BOOK}/pages", ignore_errors=True)
    M = MOD + ":"
    page("intro", "start", 0, "Simple Machines", "Workshop manual",
         "Powered machines built by hand. Every machine starts as a chassis; you fit its parts one at a time and fix each with a screwdriver.\n\nStart with an Alloy Furnace and an Extruder, which burn fuel. They make the steel, redstone copper and rods everything else needs.",
         "intro", "A finished machine", "First tools",
         [craft("screwdriver"), craft("alloy_furnace"), craft("extruder")])
    page("metals", "start", 1, "Metals", "Steel, redstone copper, rods and plates",
         "The Alloy Furnace mixes two inputs. The Extruder turns an iron or copper ingot into 2 rods. Once you have power, the Rolling Mill rolls ingots into rods, or plates into casings; the button in its screen switches between them.\n\nPlates come from the Compressor, one ingot each. Steel rods only come from the Rolling Mill. The Tool Stand and Diamond Drill Bit need them.\n\nCopper, steel and gold follow the iron cards. Plates are tagged c:plates.",
         "metals", "Alloy Furnace, side view", "Recipes",
         [process("alloying/steel_ingot", "alloy_furnace"), process("alloying/redstone_copper_ingot", "alloy_furnace"),
          process("extruding/iron_rod", "extruder"), compress("compressing/iron_plate"),
          process("rolling/steel_rod", "rolling_mill", "Rod mode")])
    page("chassis", "building", 0, "Chassis and Tiers", "Tier 1 iron, tier 2 steel",
         "The chassis is the frame every powered machine is built on, and it sets the tier.\n\nIron is tier 1. Steel is tier 2: the machine runs 1.5x faster, holds twice the energy and some machines reach further, such as the 9x9 Crop Harvester.",
         "chassis", "Iron and steel frames", "Recipes",
         [craft("iron_chassis"), craft("steel_chassis")])
    page("assembly", "building", 1, "Building a Machine", "Fit a part, then screw it in",
         "Place a chassis and right click it with each part in turn. A part sits loose until you use the screwdriver on it.\n\n0 Chassis  1 Assembly  2 Control board  3 Upgrades (optional, up to 2)  4 Panels  5 Worktop\n\nSneak with the screwdriver to take the last part off. Breaking a part-built chassis drops its parts.",
         "assembly", "Exploded view, parts in order", "Order",
         [seq("Parts in order", [M + "steel_chassis", M + "crusher_assembly", M + "control_board", M + "overclocker_circuit", M + "oak_panels", M + "oak_worktop"],
              "Right click with the part, then the screwdriver. Skip the upgrades if you like."),
          craft("control_board"), craft("crusher_assembly", "Assembly (Crusher)")])
    page("finishes", "building", 2, "Panels and Finishes", "Wood, concrete and metal",
         "Panels close the sides and a worktop covers the top. Both come in the twelve woods, the sixteen concrete colours, and iron, copper and steel.\n\nWood panels take four slabs, concrete panels four concrete (makes two). Metal ones use plates from the Compressor; copper and steel follow the iron card. With Much More Storage its worktops are used instead.",
         "finishes", "Panel finishes and a worktop", "Recipes",
         [craft("oak_panels"), craft("white_concrete_panels"), craft("iron_panels"), craft("oak_worktop")])
    page("machines", "machines", 0, "Processing Machines", "Built on a chassis",
         "Electric Furnace: 20 FE/t, normal smelting.\nRolling Mill: 20 FE/t, rods or casings.\nCrusher: 20 FE/t, ores give 2 dusts.\nSawmill: 20 FE/t, a log gives 6 planks.\nAlloy Smelter: 30 FE/t, Alloy Furnace recipes in half the time.\n\nEach machine has its own assembly; the assembly decides what the chassis becomes. Feed FE in on any side. Use the screwdriver on a side to push outputs out of it, or pull them with a pipe; see Auto-output.",
         "machines", "Any side can take FE, items or an output port", "Assemblies",
         [craft("furnace_assembly", "Electric Furnace"), craft("rolling_assembly", "Rolling Mill"), craft("crusher_assembly", "Crusher"),
          craft("saw_assembly", "Sawmill"), craft("alloy_assembly", "Alloy Smelter")])
    page("compressor", "machines", 3, "Compressor", "Plates, diamonds, biofuel and wafers",
         "The Compressor squeezes a stack of one item into something denser. 30 FE/t, faster on steel.\n\n1 ingot: a plate (iron, copper, steel, gold), 5 s.\n64 coal: 1 diamond, 2 minutes.\n32 compostables: a Biofuel Block, 8 s. It burns for 120 s in any furnace or generator.\n4 sand: a Silicon Wafer, 10 s.\n\nThe input slot waits until it holds the full count.",
         "machines", "Any side can take FE, items or an output port", "Recipes",
         [craft("compressor_assembly", "Compressor Assembly"), build("compressor_assembly", "compressor", "Compressor"),
          compress("compressing/iron_plate"), compress("compressing/diamond"), compress("compressing/biofuel_block"), compress("compressing/silicon_wafer")])
    page("casings", "machines", 4, "Casings", "The Rolling Mill's second mode",
         "Switch the Rolling Mill to Casings and feed it plates: one plate rolls into 2 casings. Two casings and a glass block make 8 pipes, the cheap way.\n\nSheet mode has been removed. A mill saved in sheet mode shows a warning and stops; press the mode button and empty both slots to unlock it.",
         "machines", "Any side can take FE, items or an output port", "Recipes",
         [process("rolling/iron_casing", "rolling_mill", "Casing mode"), craft("item_pipe_from_casing", "Item Pipe"), craft("fluid_pipe_from_casing", "Fluid Pipe")])
    page("farm", "machines", 1, "Farm and Generator Machines", "Working the world around them",
         "Crop Harvester: harvests and replants grown crops in a 5x5 area in front (9x9 on steel), 400 FE a crop.\nShearer: shears sheep in the same area, 500 FE a sheep.\nStone Generator: cobblestone from water and lava, 10 FE/t.\nWater Generator: fills buckets beside 2 water sources, 10 FE/t.\nMechanical Hive: honeycomb and honey bottles, 20 FE/t.",
         "farm", "The work area sits in front of the machine", "Assemblies",
         [craft("harvester_assembly", "Crop Harvester"), craft("shearer_assembly", "Shearer"), craft("stone_assembly", "Stone Generator"),
          craft("water_assembly", "Water Generator"), craft("hive_assembly", "Mechanical Hive")])
    page("crafter", "machines", 2, "Auto Crafter", "Any crafting recipe, on power",
         "Click items into the 3x3 pattern; nothing is used up. Right click a slot with an empty hand to clear it, or press x to clear the lot. JEI's + button fills the pattern too.\n\nThe Ingredients row holds one stack per ingredient; tools and other unstackables get one slot each. Pipes and hoppers can only put matching items in.\n\nResults and leftovers such as empty buckets or used tools land in the 4 outputs. It waits when they are full. 20 FE/t for 5 seconds a craft, faster on steel. The redstone button picks always on, run with a signal or stop with one.",
         "machines", "Any side can take FE, items or an output port", "Assembly",
         [craft("crafter_assembly", "Crafter Assembly"), build("crafter_assembly", "auto_crafter", "Auto Crafter")])
    page("power", "power", 0, "Power and Cables", "FE from fuel, stored and carried",
         "The Solid Fuel Generator burns fuel into 40 FE/t (60 on steel). The Battery stores 500k FE (1M on steel): it takes FE on every side but the front and gives it out of the front.\n\nRedstone Copper Cable carries up to 1000 FE/t and connects to anything that takes or gives FE. Use a screwdriver on a cable side to disconnect it and again to reconnect it. Any of the 16 dyes colours a cable. The Charger, built from a Charger Assembly, fills FE items.",
         "power", "Generator, battery, machine", "Power",
         [craft("generator_assembly", "Generator Assembly"), build("generator_assembly", "solid_fuel_generator", "Solid Fuel Generator"),
          dict(craft("battery_assembly", "Battery Assembly"), join=False), build("battery_assembly", "battery", "Battery"),
          dict(craft("charger_assembly", "Charger Assembly"), join=False), build("charger_assembly", "charger", "Charger")])
    page("lava_generator", "power", 2, "Lava Generator", "Burns lava, no ash",
         "The Lava Generator holds 4000 mB of lava. Fill it with lava buckets in its left slot, which come back empty on the right, or pipe lava into any side.\n\nIt burns 1 mB every 10 ticks, so a bucket runs 500 s. 80 FE/t on iron, 120 FE/t on steel. It stores 80k FE and gives out up to 400 FE/t to every side.\n\nThe lava stays in the tank only while the machine stands; breaking it loses the lava.",
         "power", "Generator, battery, machine", "Assembly",
         [craft("lava_assembly", "Lava Assembly"), build("lava_assembly", "lava_generator", "Lava Generator")])
    page("power_parts", "power", 1, "Cables and Cells", "Carrying and storing FE",
         "Each power block is a chassis with its own assembly, built like any machine: fit the assembly, then the control board, panels and worktop. Generators, batteries and machines keep their energy when broken.\n\nThe Power Cell goes into the Battery Module of a powered tool.",
         "power", "Generator, battery, machine", "Recipes",
         [craft("redstone_copper_cable"), craft("power_cell")])
    page("voltage", "power", 3, "Voltage and Transformers", "LV, MV and HV",
         "Cables come in three voltages and only join their own kind.\n\nLV, Redstone Copper Cable (red): 1,000 FE/t.\nMV, Gold Cable (yellow): 8,000 FE/t.\nHV, Diamond Cable (light blue): 64,000 FE/t.\n\nEvery machine, generator and battery is LV, except the Diamond Solar Panel, which gives MV. A Transformer links two voltages: the front, the side facing you when you place it, is the higher one and every other face the lower. FE goes both ways, losing 5%. The LV/MV one moves up to 1,000 FE/t each way, the MV/HV one 8,000.\n\nOvervoltage: feed MV or HV into a lower rated cable end, machine or transformer face and it explodes. Server owners can turn off the terrain damage in config/simplemachines-server.properties.",
         "voltage", "Step up, carry, step down", "Recipes",
         [craft("gold_cable"), craft("diamond_cable"), dict(craft("lv_mv_transformer"), join=False), craft("mv_hv_transformer")])
    page("solar", "power", 4, "Solar Panels", "Four tiers, free power from the sky",
         "A solar panel needs open sky right above it. It makes FE in daylight and gives it out of its sides and bottom.\n\nIron: 4 FE/t, stores 2k.\nCopper: 20 FE/t, stores 10k.\nGold: 100 FE/t, stores 50k.\nDiamond: 1,000 FE/t by day and 500 at night, stores 500k.\n\nEach tier takes four panels of the tier below. The Diamond panel gives MV: use Gold Cable or an LV/MV Transformer front, or LV blocks next to it explode. Right click a panel to read it.",
         "solar", "Iron, Copper, Gold and Diamond", "Recipes",
         [craft("iron_solar_panel"), craft("copper_solar_panel"), craft("gold_solar_panel"), craft("diamond_core"), craft("diamond_solar_panel")])
    page("solar_parts", "power", 5, "Solar Parts", "Cells, blend and cores",
         "PV Cells go into every panel up to Gold. Make them from glass, lapis, a Silicon Wafer and a Redstone Copper Ingot.\n\nThe Diamond panel uses Silicon-Lapis Blend instead, alloyed from a wafer and 3 lapis.\n\nEach tier has its own core: a centre of Redstone Copper Ingot with 4 iron, copper or gold plates, or a diamond with 4 steel plates (the Diamond Core is on the panel page).",
         "solar", "Iron, Copper, Gold and Diamond", "Recipes",
         [craft("pv_cell"), process("alloying/silicon_lapis_blend", "alloy_smelter"), craft("iron_core"), craft("copper_core"),
          craft("gold_core")])
    page("wind", "power", 6, "Helical Wind Turbine", "Two blocks tall, best up high",
         "The turbine needs a free block above where you place it. Breaking either half takes the whole turbine.\n\nOutput is up to 50 FE/t: 50 x height x open air x weather. Height counts from Y 64 (nothing) to Y 160 (full). Open air is the share of air in the 9x9x9 space around the rotor, checked every 5 seconds. Rain gives x1.25 and thunder x1.5.\n\nIt stores 20k FE and gives LV out of the base. Right click it to read it.",
         "wind", "Rotor on top, base below", "Recipe",
         [craft("wind_turbine")])
    page("pipes", "items", 0, "Item Pipes", "Three-way pipe ends",
         "Pipes connect to each other and to anything with an inventory. Use a screwdriver on a pipe end to cycle it:\n\nInput (default): the pipe delivers into that block.\nOutput: the pipe pulls items out every half second and sends them to the other inputs in turn.\nDisconnected: the pipe ignores that block.",
         "pipes", "One pipe, three ends", "Pipe",
         [craft("item_pipe"), text("Machines only accept items they have a recipe for, and only give up their outputs."),
          text("Use any of the 16 dyes on a placed pipe or cable to keep runs apart: two colours never join, plain joins any colour. They drop plain.")])
    page("filters", "items", 2, "Pipe Filters", "Sort what goes where",
         "Use a Pipe Filter on an input end of an item or fluid pipe to fit it. Right click the filter with an empty hand to set it up: put items in the 9 slots as a pattern.\n\nAllow: the end takes only what is listed, and gets it first.\nDeny: the end takes anything not listed.\nTags: match by item tag instead of the exact item.\nData: also match names, enchantments and other data.",
         "filters", "Allow ends first, then the rest", "Filter",
         [craft("pipe_filter"), text("Items go to matching Allow ends in turn. Only when none of them can take more do they go to plain ends and Deny ends that do not list them."),
          text("Breaking the pipe or changing the end with the screwdriver drops the filter with its settings kept.")])
    page("fluids", "items", 3, "Fluid Pipes", "Copper pipes with a window",
         "Fluid pipes work like item pipes but move fluids, 250 mB every half second from each output end. They connect to tanks and machines from other mods, and to cauldrons.\n\nThe glass window shows the fluid while it moves. Filters work the same way: put a bucket of the fluid, or anything holding it, in a slot.",
         "fluids", "Tank to machine", "Pipe",
         [craft("fluid_pipe"), text("Fluid and item pipes never join each other. Dyes keep fluid runs apart in the same way.")])
    page("tanks", "items", 4, "Fluid Tank", "Sixteen buckets of one fluid",
         "A Fluid Tank holds 16 buckets of a single fluid and shows its level through the glass. Pipes join it on any face.\n\nUse a bucket on it to pour in or fill up. Broken tanks keep what is inside.",
         "tanks", "Pipes in, pipes out", "Tank",
         [craft("fluid_tank"), text("Set the pipe end on the tank to output to drain it into the rest of the run.")])
    page("output", "items", 1, "Auto-output", "Push outputs into the next block",
         "Use a screwdriver (without sneaking) on a side of a built machine to make it push its outputs into whatever is on that side, such as a chest or a cupboard.\n\nSides that output or have a pipe on them show a port. All sides are off to start with.",
         "output", "Machine pushing into a chest", "Sides",
         [seq("Set an output side", [M + "screwdriver"], "Use the screwdriver on the side facing the chest. Use it again to turn that side off."),
          text("Hoppers and other mods see the same sides: inputs can be filled, outputs emptied.")])
    page("upgrades", "upgrades", 0, "Upgrades", "Optional step after the control board",
         "With the control board fixed, right click with an upgrade and screw it in. There are two slots and any mix fits.\n\nEach Overclocker adds half the base speed; FE use and intake rise with it. Each Energy Cell adds the base storage again. Two Overclockers run at double speed; two Cells hold triple.\n\nUpgrades stay with the machine and drop back when it is taken apart.",
         "upgrades", "Upgrade cards on the control board", "Recipes",
         [craft("overclocker_circuit"), craft("energy_cell"),
          text("Fitted upgrades show on the machine's tooltip and in its screen.")])
    page("tools", "tools", 0, "Powered Tools", "Built on a Tool Stand",
         "The Tool Stand is about the size of an anvil. Place one and fit the parts in order, using the screwdriver after each one:\n\n1 Frame  2 Core  3 Control board  4 Battery module  5 Upgrades (optional, up to 2)\n\nThe core decides the tool: a Drill Motor makes an Electric Drill (pickaxe and shovel blocks), a Chainsaw Bar a Chainsaw (axe blocks). Any time after the core, a Diamond Drill Bit or Diamond-Tipped Chain can be fitted and screwed on. Take the finished tool off with an empty hand.",
         "tools", "Drill and chainsaw", "Order",
         [seq("Parts in order", [M + "tool_stand", M + "steel_tool_frame", M + "drill_motor", M + "control_board", M + "battery_module", M + "overclocker_circuit"],
              "Right click with the part, then the screwdriver."),
          text("Put a tool back on an empty stand to change it: sneak with the screwdriver to take the last part off. Parts drop back, and the battery module keeps its charge."), craft("tool_stand")])
    page("tool_parts", "tools", 1, "Tool Parts", "Frames, cores and upgrades",
         "A tool holds 50k FE and uses 50 FE a block. When empty it is slow and drops nothing.\n\nThe body sets the speed: a steel frame digs at 1.25x an iron one.\nOverclocker: +50% speed and +50% FE a block, so two give +100% speed at 2x FE.\nEnergy Cell: +15% speed and +50k FE, no extra FE a block.\nThe two together add at most +100% speed.\n\nCharge tools in the Charger. FE on NeoForge, Team Reborn Energy on Fabric.",
         "tools", "Side views", "Recipes",
         [craft("iron_tool_frame"), craft("steel_tool_frame"), craft("drill_motor"), craft("chainsaw_bar"), craft("battery_module")])
    page("tool_tiers", "tools", 2, "Tool Tiers", "Bits, chains and frames",
         "The bit decides what a drill can mine. The body decides how fast.\n\nThe standard bit mines up to diamond ore. Obsidian, crying obsidian, ancient debris and other netherite level blocks throw white-hot sparks and do not break.\n\nA Diamond Drill Bit mines them. A steel body runs at 1.25x; an iron body manages only 40% on those blocks.",
         "tools", "Drill and chainsaw", "Diamond tips",
         [text("Tree felling: a Chainsaw with a steel body and a Diamond-Tipped Chain fells a whole natural tree, top down, for 50 FE per log. Sneak to cut a single log. Built log walls are left alone."),
          craft("diamond_drill_bit"), craft("diamond_tipped_chain"),
          seq("Electric Drill", [M + "electric_drill"], "Pickaxe and shovel blocks."), seq("Chainsaw", [M + "chainsaw"], "Axe blocks."),
          text("With no charge left the motor stays still, digging is slow and nothing drops. Recharge the tool in the Charger.")])

def main():
    os.makedirs(TEX, exist_ok=True)
    for f in [d_intro, d_metals, d_chassis, d_assembly, d_finishes, d_machines, d_farm, d_power, d_voltage, d_solar, d_wind, d_pipes, d_filters, d_fluids, d_tanks, d_output, d_upgrades, d_tools]: f()
    textures()
    jdump({"extends": "simplebooks:text", "vars": {"rule": "#9cc0e4"},
           "styles": {"title": {"color": "$head", "scale": 2, "align": "center"}, "body": {"color": "$ink", "line_height": 20}},
           "slots": {"left.body": {"box": [0, 100, 644, 330]},
                     "left.drawing": {"type": "image_box", "box": [42, 440, 560, 350], "bind": "drawing", "frame": False, "optional": True},
                     "left.caption": {"type": "subtitle", "box": [0, 800, 644, 18], "bind": "caption", "style": "subtitle", "optional": True}}},
          f"{A}/simplebooks/templates/blueprint.json")
    jdump({"title": "book.simplemachines.manual.title", "default_template": f"{MOD}:blueprint", "tabs": True,
           "style": {"page": "#1d4f8c", "page_texture": f"{T}/grid.png", "page_tile": "128", "font": "minecraft:uniform",
                     "ink": "#ffffff", "soft": "#c8dcf0", "head": "#ffffff", "link": "#ffe680", "rule": "#9cc0e4", "arrow": "#ffffff",
                     "cover": "#16365e", "cover_edge": "#0c1e36", "edge": "#9cc0e4",
                     "badge": "#ffffff", "badge_edge": "#9cc0e4", "badge_text": "#1d4f8c", "frame": "#ffffff", "frame_inner": "#1d4f8c",
                     "placeholder": "#245a98", "placeholder_hatch": "#2a64a6", "slot": "#2a64a6", "slot_shadow": "#163e6e", "slot_light": "#9cc0e4",
                     "mark": "#ffffff", "mark_hover": "#ffe680", "mark_line": "#1d4f8c", "tabs": "#ffffff,#d6e6f6,#aecdec",
                     "tab_text": "#1d4f8c", "tab_edge": "#0c1e36", "button_tint": "#c8dcf0", "stand_ink": "#ffffff"},
           "stand_cover": f"{MOD}:simplebooks/manual/stand_cover", "stand_page": f"{MOD}:simplebooks/manual/stand_page",
           "sections": ["contents", {"id": "start", "title": "Getting Started", "icon": f"{MOD}:screwdriver"},
                        {"id": "building", "title": "Building", "icon": f"{MOD}:steel_chassis"},
                        {"id": "machines", "title": "Machines", "icon": f"{MOD}:crusher"},
                        {"id": "power", "title": "Power", "icon": f"{MOD}:redstone_copper_cable"},
                        {"id": "items", "title": "Items", "icon": f"{MOD}:item_pipe"},
                        {"id": "upgrades", "title": "Upgrades", "icon": f"{MOD}:overclocker_circuit"},
                        {"id": "tools", "title": "Tools", "icon": f"{MOD}:electric_drill"}]},
          f"{A}/simplebooks/books/manual.json")
    pages()
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/manual"}}, f"{A}/items/manual.json")
    jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/manual"}}, f"{A}/models/item/manual.json")
    jdump({**COND, "type": f"{MOD}:crafting_shapeless_keep_tool", "category": "misc", "ingredients": ["minecraft:book", "#c:tools/screwdriver", "#c:ingots/copper"],
           "keep": "#c:tools/screwdriver",
           "result": {"id": "simplebooks:guide", "components": GUIDE}}, f"{D}/recipe/manual.json")
    jdump({**COND, "type": "minecraft:advancement_reward", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "simplebooks:guide",
           "functions": [{"function": "minecraft:set_components", "components": GUIDE}]}]}]}, f"{D}/loot_table/manual.json")
    jdump({**COND, "criteria": {"screwdriver": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "#c:tools/screwdriver"}]}}},
           "requirements": [["screwdriver"]], "rewards": {"loot": [f"{MOD}:manual"]}}, f"{D}/advancement/got_manual.json")
    lang = json.load(open(f"{A}/lang/en_us.json"))
    lang["book.simplemachines.manual.title"] = "Simple Machines Manual"
    jdump(lang, f"{A}/lang/en_us.json")

if __name__ == "__main__":
    main()

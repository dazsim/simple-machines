# Pipe filter and fluid pipe assets. Called from gen_assets.main after the item pipe textures exist.
import json, random
from PIL import Image

MOD = "simplemachines"

def h(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

DARK = h("484c58"); HI = h("d6dae2"); LINK = h("767c8a"); LINKHI = h("a2a8b4")
FL = h("5d6270"); MESH_HOLE = h("2e3138"); PIPE_GREY = h("a0a6b2")
CU_DK = h("6d3421"); CU_LO = h("9c4529"); CU = h("c15a36"); CU_MID = h("e77c56"); CU_HI = h("fc9982")
GLASS_DK = h("3a4a5c"); GLASS_LIT = h("8aa2b8"); GLASS_TOP = h("bfd6e6")
STEEL = [(214, 218, 226), (162, 168, 180), (118, 124, 138), (72, 76, 88)]

def main(A, D, save, jdump, shaped, loot_self, model, el, all_faces, gui, rect, pipe_tex):
    tb = A + "/textures/block/"
    rnd = random.Random(7)

    def noisy(c, n=3):
        d = rnd.randint(-n, n)
        return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)

    def tex(name, fn, folder="block"):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                im.putpixel((x, y), fn(x, y))
        save(im, f"{A}/textures/{folder}/{name}.png")
        return im

    def flange(x, y):
        if y == 0 or x == 0: return HI
        if y == 15 or x == 15: return DARK
        if x in (4, 11) and 4 <= y <= 11 or y in (4, 11) and 4 <= x <= 11:
            if (x in (4, 11) and y in (7, 8)) or (y in (4, 11) and x in (7, 8)): return MESH_HOLE
            return DARK if (x == 11 or y == 11) else LINKHI
        return noisy(FL)
    tex("item_pipe_filter", flange)
    flat = lambda c, n=2: (lambda x, y: noisy(c, n))
    tex("item_pipe_filter_sleeve", flat(LINK))
    tex("item_pipe_filter_rib", flat(PIPE_GREY))
    tex("fluid_pipe_filter_sleeve", flat(CU_LO))
    tex("fluid_pipe_filter_rib", flat(CU))

    def flange_cu(x, y):
        if y == 0 or x == 0: return CU_HI
        if y == 15 or x == 15: return CU_DK
        if x in (4, 11) and 4 <= y <= 11 or y in (4, 11) and 4 <= x <= 11:
            if (x in (4, 11) and y in (7, 8)) or (y in (4, 11) and x in (7, 8)): return CU_DK
            return CU_DK if (x == 11 or y == 11) else CU_MID
        return noisy(CU_LO)
    tex("fluid_pipe_filter", flange_cu)

    def lens(x, y):
        if x in (6, 9): return DARK
        if (x, y) in ((7, 3), (7, 13)): return (240, 250, 255, 255)
        return noisy(GLASS_TOP, 2)
    tex("item_pipe_filter_lens", lens)

    # glass window down the middle; the lit texels are where the renderer draws the fluid
    def fluid(x, y):
        if y == 0 or x == 0: return CU_HI
        if y == 15 or x == 15: return CU_LO
        win_row = y in (7, 8) and (2 <= x <= 13) and x not in (7, 8)
        win_col = x in (7, 8) and (2 <= y <= 13) and y not in (7, 8)
        if win_row: return GLASS_DK if y == 7 else noisy(GLASS_LIT, 4)
        if win_col: return noisy(GLASS_LIT, 4) if x == 8 else GLASS_DK
        if (y in (6, 9) and 2 <= x <= 13 and x not in (7, 8)) or (x in (6, 9) and 2 <= y <= 13 and y not in (7, 8)): return CU_LO
        return noisy(CU, 3)

    def fluid_core(x, y):
        if y == 0 or x == 0: return CU_HI
        if y == 15 or x == 15: return CU_LO
        if 7 <= x <= 8 and 7 <= y <= 8: return GLASS_DK if y == 7 else noisy(GLASS_LIT, 4)
        if 6 <= x <= 9 and 6 <= y <= 9: return CU_LO
        return noisy(CU, 3)
    tex("fluid_pipe_core", fluid_core)
    tex("fluid_pipe", fluid)

    link = Image.open(tb + "item_pipe_link.png").convert("RGBA")
    def fluid_link(x, y):
        p = link.getpixel((x, y))
        if p[:3] == DARK[:3]: return CU_DK
        if p[:3] == LINKHI[:3]: return CU_MID
        return noisy(CU_LO, 3)
    tex("fluid_pipe_link", fluid_link)

    def icon_filter(x, y):
        T = (0, 0, 0, 0)
        if not (1 <= x <= 14 and 1 <= y <= 14): return T
        if x in (1, 14) or y in (1, 14):
            if (x in (1, 14) and y in (7, 8)) or (y in (1, 14) and x in (7, 8)): return T
            return DARK
        if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)): return HI
        if (x, y) in ((4, 4), (13, 4), (4, 13), (13, 13)): return DARK
        if 4 <= x <= 11 and 4 <= y <= 11:
            if x in (4, 11) or y in (4, 11): return DARK if (x == 11 or y == 11) else LINKHI
            if 5 <= x <= 10 and 5 <= y <= 10:
                if (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)): return GLASS_TOP if (x, y) != (7, 7) else (240, 250, 255, 255)
                if (y in (5, 10) and x in (6, 9)) or (x in (5, 10) and y in (6, 9)): return PIPE_GREY
                return noisy(LINK, 2)
        if x == 2 or y == 2: return LINKHI
        if x == 13 or y == 13: return h("4f5360")
        return noisy(FL, 2)
    tex("pipe_filter", icon_filter, "item")
    jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/pipe_filter"}}, f"{A}/models/item/pipe_filter.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/pipe_filter"}}, f"{A}/items/pipe_filter.json")
    save(pipe_tex(STEEL, 87, STEEL[3]), tb + "fluid_pipe_extract.png")

    # filter screen: nine ghost slots on the left, the switches are buttons on the right
    t = gui("gen")
    rect(t, 7, 14, 170, 78, (198, 198, 198, 255))
    for i in range(9):
        x, y = 25 + i % 3 * 18, 16 + i // 3 * 18
        rect(t, x, y, x + 17, y + 17, (139, 139, 139, 255))
        rect(t, x, y, x + 16, y, (55, 55, 55, 255)); rect(t, x, y, x, y + 16, (55, 55, 55, 255))
        rect(t, x + 1, y + 17, x + 17, y + 17, (255, 255, 255, 255)); rect(t, x + 17, y + 1, x + 17, y + 17, (255, 255, 255, 255))
    save(t, A + "/textures/gui/pipe_filter.png")

    # fluid pipe, same geometry as the item pipe
    pt = {"core": f"{MOD}:block/fluid_pipe_core", "pipe": f"{MOD}:block/fluid_pipe", "link": f"{MOD}:block/fluid_pipe_link",
          "extract": f"{MOD}:block/fluid_pipe_extract", "particle": f"{MOD}:block/fluid_pipe"}
    a0, a1, c0, c1 = 6.5, 9.5, 5.5, 10.5
    core = el((c0, c0, c0), (c1, c1, c1), all_faces("core"))
    model("fluid_pipe", [core], pt)
    model("fluid_pipe_arm", [el((a0, a0, 0), (a1, a1, c0), all_faces("pipe", ("south",)))], pt)
    model("fluid_pipe_collar", [el((6, 6, 0), (10, 10, 1.5), all_faces("link"))], pt)
    model("fluid_pipe_grab", [el((5.5, 5.5, 0), (10.5, 10.5, 2), all_faces("extract"))], pt)
    rots = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    parts = [{"apply": {"model": f"{MOD}:block/fluid_pipe"}}]
    for d, r in rots.items():
        parts.append({"when": {d: "pipe|link|extract"}, "apply": {"model": f"{MOD}:block/fluid_pipe_arm", **r}})
        parts.append({"when": {d: "link"}, "apply": {"model": f"{MOD}:block/fluid_pipe_collar", **r}})
        parts.append({"when": {d: "extract"}, "apply": {"model": f"{MOD}:block/fluid_pipe_grab", **r}})
    jdump({"multipart": parts}, f"{A}/blockstates/fluid_pipe.json")
    display = json.load(open(f"{A}/models/block/item_pipe_item.json")).get("display")
    item = {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": pt}
    if display:
        item["display"] = display
    item["elements"] = [el((0, a0, a0), (16, a1, a1), all_faces("pipe")), core]
    jdump(item, f"{A}/models/block/fluid_pipe_item.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/fluid_pipe_item"}}, f"{A}/items/fluid_pipe.json")
    loot_self("fluid_pipe")
    shaped("fluid_pipe", ["CGC"], {"C": "#c:plates/copper", "G": "#c:glass_blocks"}, 8)
    shaped("pipe_filter", [" S ", "BCB", " S "], {"S": "#c:plates/iron", "B": "minecraft:iron_bars", "C": f"{MOD}:control_board"})

    tank(A, D, jdump, shaped, model, el, tex, noisy, save)

    tag = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    vals = json.load(open(tag))["values"]
    jdump({"values": vals + [f"{MOD}:fluid_pipe", f"{MOD}:fluid_tank"]}, tag)

    lang = json.load(open(f"{A}/lang/en_us.json"))
    lang.update({
        "block.simplemachines.fluid_pipe": "Fluid Pipe",
        "item.simplemachines.pipe_filter": "Pipe Filter",
        "container.simplemachines.pipe_filter": "Pipe Filter",
        "tooltip.simplemachines.pipe_filter": "Use on a pipe input end, then right click it with an empty hand to set it up",
        "tooltip.simplemachines.filter_tags": "tags on",
        "tooltip.simplemachines.filter_data": "data must match",
        "gui.simplemachines.filter_mode": "Mode: %s",
        "gui.simplemachines.filter_tags": "Tags: %s",
        "gui.simplemachines.filter_data": "Data: %s",
        "gui.simplemachines.filter_allow": "Allow",
        "gui.simplemachines.filter_deny": "Deny",
        "gui.simplemachines.filter_off": "Off",
        "gui.simplemachines.filter_on": "On",
        "gui.simplemachines.filter_ignore": "Ignore",
        "gui.simplemachines.filter_match": "Match",
        "gui.simplemachines.filter_mode_hint": "Allow: only the listed items or fluids come out here. Deny: anything but them.",
        "gui.simplemachines.filter_tags_hint": "On: also anything sharing a tag with a listed item, such as other planks or other iron ingots.",
        "gui.simplemachines.filter_data_hint": "Match: enchantments, damage and other item data must be the same too.",
        "message.simplemachines.filter_no_end": "No input end to fit a filter to",
        "message.simplemachines.filter_present": "That end already has a filter",
        "message.simplemachines.filter_fitted": "Filter fitted",
        "message.simplemachines.pipe_no_fluid": "No tank on that side",
        "block.simplemachines.fluid_tank": "Fluid Tank",
        "tooltip.simplemachines.tank": "%s: %s / %s mB",
        "tooltip.simplemachines.tank_empty": "Empty, holds 16 buckets of one fluid",
    })
    jdump(lang, f"{A}/lang/en_us.json")


TANK_E, TANK_K = 0, 4  # edge bevel and corner cut, in pixels


def tank(A, D, jdump, shaped, model, el, tex, noisy, save):
    # a cube with clipped corners, optionally bevelled edges
    import math
    T = (0, 0, 0, 0)
    E, K, H = TANK_E, TANK_K, 8
    r2, r3 = math.sqrt(2), math.sqrt(3)
    rnd = random.Random(11)

    def mul(c, k):
        return tuple(max(0, min(255, round(v * k))) for v in c[:3]) + (255,)

    def bolt(dx, dy, r=1.0):
        d = (dx * dx + dy * dy) ** 0.5
        if d > r: return None
        if dx + dy < -0.35 * r: return STEEL[0]
        if dx + dy > 0.45 * r: return STEEL[3]
        return STEEL[1] if d < 0.55 * r else STEEL[2]

    planes = []
    for a in range(3):
        for sg in (-1, 1):
            n = [0, 0, 0]; n[a] = sg
            planes.append((n, H, "face"))
    if E:
        for a, b in ((0, 1), (0, 2), (1, 2)):
            for sa in (-1, 1):
                for sb in (-1, 1):
                    n = [0, 0, 0]; n[a] = sa / r2; n[b] = sb / r2
                    planes.append((n, (2 * H - E) / r2, "edge"))
    for sx in (-1, 1):
        for sy in (-1, 1):
            for sz in (-1, 1):
                planes.append(([sx / r3, sy / r3, sz / r3], (3 * H - K) / r3, "corner"))

    dot = lambda a, b: sum(x * y for x, y in zip(a, b))
    sub = lambda a, b: [x - y for x, y in zip(a, b)]
    cross = lambda a, b: [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
    unit = lambda a: [x / math.sqrt(dot(a, a)) for x in a]

    def poly(i):
        n, d, _ = planes[i]
        u = unit(cross([0, 1, 0], n)) if abs(n[1]) < 0.9 else unit(cross([1, 0, 0], n))
        v = cross(n, u)
        c = [x * d for x in n]
        pts = [[c[k] + (u[k] * a + v[k] * b) * 40 for k in range(3)] for a, b in ((-1, -1), (1, -1), (1, 1), (-1, 1))]
        for j, (m, e, _) in enumerate(planes):
            if j == i or len(pts) < 3: continue
            nxt = []
            for k in range(len(pts)):
                a, b = pts[k], pts[(k + 1) % len(pts)]
                da, db = dot(a, m) - e, dot(b, m) - e
                if da <= 1e-6: nxt.append(a)
                if (da <= 1e-6) != (db <= 1e-6):
                    t = da / (da - db)
                    nxt.append([a[q] + (b[q] - a[q]) * t for q in range(3)])
            pts = nxt
        out = []
        for p in pts:
            if not out or math.dist(p, out[-1]) > 1e-4: out.append(p)
        if len(out) > 1 and math.dist(out[0], out[-1]) < 1e-4: out.pop()
        return out

    def light(n):
        return n[0] ** 2 * 0.6 + n[2] ** 2 * 0.8 + n[1] ** 2 * (1.0 if n[1] > 0 else 0.5)

    def inside(pts2, x, y):
        # signed distance to a convex ccw polygon, positive inside
        d = 1e9
        for k in range(len(pts2)):
            (ax, ay), (bx, by) = pts2[k], pts2[(k + 1) % len(pts2)]
            l = math.hypot(bx - ax, by - ay)
            d = min(d, ((bx - ax) * (y - ay) - (by - ay) * (x - ax)) / l)
        return d

    # square faces, drawn in face pixels on a 64px texture
    fpoly = poly(1)
    f2 = [(8 + p[2], 8 - p[1]) for p in fpoly]
    if inside(f2, 8, 8) < 0: f2 = f2[::-1]
    lo = min(x for x, _ in f2)
    N = 64

    def face_tex(kind):
        im = Image.new("RGBA", (N, N))
        cells = {}
        for y in range(N):
            for x in range(N):
                fx, fy = (x + 0.5) / 4, (y + 0.5) / 4
                d = inside(f2, fx, fy)
                if d < 0: continue
                cx, cy = x // 4, y // 4
                if (cx, cy) not in cells:
                    dc = inside(f2, cx + 0.5, cy + 0.5)
                    lit = cx + cy < 15
                    c = None
                    if kind == "bottom":
                        r = math.hypot(cx - 7.5, cy - 7.5)
                        if r < 1.6: c = MESH_HOLE
                        elif r < 2.6: c = STEEL[1] if lit else STEEL[2]
                        elif dc < 1: c = CU_MID if lit else CU_LO
                        elif (min(cx, 15 - cx), min(cy, 15 - cy)) == (3, 3): c = STEEL[0] if lit else STEEL[1]
                        elif dc < 2: c = noisy(CU_LO, 2)
                        else: c = noisy(CU, 3)
                    else:
                        fw = 1 if E else 2
                        along = max(min(cx, 15 - cx), min(cy, 15 - cy))
                        if dc < fw:
                            c = (CU_HI if lit else CU_MID) if dc < 1 else noisy(CU, 3)
                            if fw - 1 <= dc and along in (4, 5) if kind == "top" else False: c = STEEL[0]
                        elif dc < fw + 1:
                            c = STEEL[1] if lit else STEEL[2]
                            if kind == "top" and along == 5: c = STEEL[0]
                        else:
                            c = "glass"
                    cells[(cx, cy)] = c
                c = cells[(cx, cy)]
                if c == "glass":
                    c = T
                    if abs(fx + fy - 9.5) < 0.2 and 4.5 < fx < 6 or abs(fx + fy - 11) < 0.2 and 4.5 < fx < 7.5: c = GLASS_TOP
                if kind == "inner": c = mul(noisy(CU_LO, 4), 0.55)
                if d < 0.3: c = CU_DK
                im.putpixel((x, y), c)
        return im
    tb = A + "/textures/block/"
    for kind in ("side", "top", "bottom", "inner"):
        save(face_tex(kind), tb + f"fluid_tank_{kind}.png")

    # facets on the bevels and corners, each with one shared shape in its own plane
    def frame_of(i):
        n = planes[i][0]
        pts = poly(i)
        ls = [math.dist(pts[k], pts[(k + 1) % len(pts)]) for k in range(len(pts))]
        k0 = max(range(len(pts)), key=lambda k: round(ls[k], 3) * 1000 - k)
        u = unit(sub(pts[(k0 + 1) % len(pts)], pts[k0]))
        v = cross(n, u)
        o = [sum(p[q] for p in pts) / len(pts) for q in range(3)]
        loc = [(dot(sub(p, o), u), dot(sub(p, o), v)) for p in pts]
        return n, u, v, o, loc

    def facet_tex(kind, loc, k, W, Hh):
        xs, ys = [x for x, _ in loc], [y for _, y in loc]
        x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
        im = Image.new("RGBA", (W, Hh))
        cen = (sum(xs) / len(xs), sum(ys) / len(ys))
        inr = inside(loc, *cen)
        cells = {}
        for y in range(Hh):
            for x in range(W):
                lx = x0 + (x + 0.5) / W * (x1 - x0)
                ly = y1 - (y + 0.5) / Hh * (y1 - y0)
                d = inside(loc, lx, ly)
                if d < -0.1: continue
                key = (int(lx * 1.2 + 50), int(ly * 1.2 + 50))
                if key not in cells: cells[key] = noisy(CU, 3)
                c = cells[key]
                if kind == "inner": c = mul(c, 0.5)
                elif d < 0.22: c = DARK
                elif kind == "corner":
                    if d < 0.6: c = STEEL[1] if d < 0.42 else STEEL[2]
                    elif inr > 1.2:
                        b = bolt((lx - cen[0]) / 0.6, (ly - cen[1]) / 0.6)
                        if b: c = b
                else:
                    if d < 0.45: c = CU_HI if ly > cen[1] else CU_LO
                    else:
                        for bx in (-3.0, 0.0, 3.0):
                            b = bolt((lx - bx) / 0.5, (ly - cen[1]) / 0.5)
                            if b: c = b
                im.putpixel((x, y), mul(c, k) if kind != "inner" else c)
        return im

    t = {"side": f"{MOD}:block/fluid_tank_side", "top": f"{MOD}:block/fluid_tank_top", "bottom": f"{MOD}:block/fluid_tank_bottom",
         "inner": f"{MOD}:block/fluid_tank_inner", "particle": f"{MOD}:block/fluid_tank_bottom"}
    dirs = {(0, -1): "west", (0, 1): "east", (1, -1): "down", (1, 1): "up", (2, -1): "north", (2, 1): "south"}
    opp = {"west": "east", "east": "west", "down": "up", "up": "down", "north": "south", "south": "north"}
    elems = []
    made = {}
    hi = 16 - lo
    uvf = [round(lo, 4), round(lo, 4), round(hi, 4), round(hi, 4)]
    for i, (n, d, kind) in enumerate(planes):
        if kind == "face":
            a = [q for q in range(3) if n[q]][0]
            sg = int(n[a])
            out = dirs[(a, sg)]
            tx = "#top" if out == "up" else "#bottom" if out == "down" else "#side"
            back = "#inner" if tx == "#bottom" else tx
            fr = [lo] * 3; to = [hi] * 3; fr[a], to[a] = (15.98, 16) if sg > 0 else (0, 0.02)
            elems.append(el(tuple(round(x, 4) for x in fr), tuple(round(x, 4) for x in to),
                            {out: {"texture": tx, "uv": uvf, "cullface": out}, opp[out]: {"texture": back, "uv": uvf}}))
            continue
        n, u, v, o, loc = frame_of(i)
        W, Hh = (64, 16) if kind == "edge" else ((32, 32) if K - E > 3 else (16, 16))
        b = round(light(n) * 100)
        name = f"fluid_tank_{kind}_{b}"
        if name not in made:
            made[name] = 1
            save(facet_tex(kind, loc, b / 100, W, Hh), tb + name + ".png")
            t[f"{kind}_{b}"] = f"{MOD}:block/{name}"
        if f"{kind}_inner" not in made:
            made[f"{kind}_inner"] = 1
            save(facet_tex("inner", loc, 1, W, Hh), tb + f"fluid_tank_{kind}_inner.png")
            t[f"{kind}_inner"] = f"{MOD}:block/fluid_tank_{kind}_inner"
        R = [[u[q], v[q], n[q]] for q in range(3)]
        be = math.asin(max(-1, min(1, -R[2][0])))
        al = math.atan2(R[2][1], R[2][2])
        ga = math.atan2(R[1][0], R[0][0])
        if abs(R[2][0]) > 0.999: al, ga = math.atan2(-R[1][2], R[1][1]), 0
        g = [x + 8 for x in o]
        xs, ys = [x for x, _ in loc], [y for _, y in loc]
        fr = (g[0] + min(xs), g[1] + min(ys), g[2])
        to = (g[0] + max(xs), g[1] + max(ys), g[2])
        uv = [0, 0, 16, 16]
        e = el(tuple(round(x, 4) for x in fr), tuple(round(x, 4) for x in to),
               {"south": {"texture": f"#{kind}_{b}", "uv": uv}, "north": {"texture": f"#{kind}_inner", "uv": uv}},
               {"origin": [round(x, 4) for x in g], "x": round(math.degrees(al), 4),
                "y": round(math.degrees(be), 4), "z": round(math.degrees(ga), 4)})
        e["shade"] = False
        elems.append(e)
    model("fluid_tank", elems, t)
    jdump({"variants": {"": {"model": f"{MOD}:block/fluid_tank"}}}, f"{A}/blockstates/fluid_tank.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/fluid_tank"}}, f"{A}/items/fluid_tank.json")
    jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
           "conditions": [{"condition": "minecraft:survives_explosion"}],
           "entries": [{"type": "minecraft:item", "name": f"{MOD}:fluid_tank", "functions": [
               {"function": "minecraft:copy_components", "source": "block_entity", "include": [f"{MOD}:tank"]}]}]}],
           "random_sequence": f"{MOD}:blocks/fluid_tank"}, f"{D}/{MOD}/loot_table/blocks/fluid_tank.json")
    shaped("fluid_tank", ["CSC", "GBG", "CSC"], {"C": "#c:plates/copper", "S": "#c:plates/iron",
                                                 "G": "#c:glass_blocks", "B": "minecraft:bucket"})

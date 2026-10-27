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


def tank(A, D, jdump, shaped, model, el, tex, noisy, save):
    # a cuboctahedron: square faces turned 45 degrees on each block face, triangles across the corners
    import math
    T = (0, 0, 0, 0)
    rnd = random.Random(11)

    def mul(c, k):
        return tuple(max(0, min(255, round(v * k))) for v in c[:3]) + (255,)

    def bolt(dx, dy, r=1.0):
        d = (dx * dx + dy * dy) ** 0.5
        if d > r: return None
        if dx + dy < -0.35 * r: return STEEL[0]
        if dx + dy > 0.45 * r: return STEEL[3]
        return STEEL[1] if d < 0.55 * r else STEEL[2]

    def frame(x, y):
        d = min(x, y, 15 - x, 15 - y)
        cx, cy = min(x, 15 - x), min(y, 15 - y)
        if (cx, cy) == (1, 1): return STEEL[0] if (x < 8) == (y < 8) else STEEL[1]
        if (cx, cy) in ((1, 2), (2, 1)): return STEEL[1]
        if (cx, cy) == (2, 2): return STEEL[3]
        if d == 0: return CU_DK
        if d == 1: return CU_HI if x < 8 and x <= y or y < 8 and y < x else CU_MID
        if d == 2: return CU_LO
        return None

    def ring(x, y, hole, w):
        r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if r < hole: return MESH_HOLE
        if r < hole + 0.9: return CU_DK if x + y < 15 else CU_HI
        if r < hole + w: return noisy(CU_MID, 2)
        if r < hole + w + 1.0: return CU_DK
        return None

    def window(x, y):
        f = frame(x, y) or ring(x, y, 0.8, 1.6)
        if f: return f
        if (x, y) in ((4, 6), (5, 5), (6, 4), (3, 6), (6, 3), (11, 12), (12, 11)): return GLASS_TOP
        return T

    def port(x, y):
        if (x, y) in ((7, 3), (3, 7), (12, 8), (8, 12)): return STEEL[0]
        if (x, y) in ((8, 3), (3, 8), (12, 7), (7, 12)): return STEEL[2]
        f = frame(x, y) or ring(x, y, 1.6, 1.9)
        if f: return f
        return noisy(CU, 2)
    port_full = port

    tex("fluid_tank_window", window)
    tex("fluid_tank_port", port_full)
    tex("fluid_tank_inner", lambda x, y: mul(noisy(CU_LO, 4), 0.55))

    # corner triangles, finer so the slanted edges stay clean
    s = 8 * math.sqrt(2)
    hgt = s * math.sqrt(3) / 2
    N = 64
    edges = []
    for (ax, ay), (bx, by) in (((s / 2, 0), (0, hgt)), ((0, hgt), (s, hgt)), ((s, hgt), (s / 2, 0))):
        nx, ny = by - ay, ax - bx
        l = math.hypot(nx, ny)
        edges.append((ax, ay, nx / l, ny / l))
    cen = (s / 2, hgt * 2 / 3)

    def tri(k, inner=False):
        im = Image.new("RGBA", (N, N))
        cell = {}
        for y in range(N):
            for x in range(N):
                px, py = (x + 0.5) / N * s, (y + 0.5) / N * hgt
                ds = [(px - ax) * nx + (py - ay) * ny for ax, ay, nx, ny in edges]
                d = min(ds)
                if d < -0.15: im.putpixel((x, y), T); continue
                key = (x // 4, y // 4)
                if key not in cell: cell[key] = noisy(CU, 3)
                if inner: c = mul(cell[key], 0.55 / k)
                elif d < 0.25: c = DARK
                elif d < 0.85: c = STEEL[1] if d < 0.55 else STEEL[2]
                elif d < 1.1: c = CU_DK
                else:
                    c = cell[key]
                    r = math.hypot(px - cen[0], py - cen[1])
                    if 1.5 < r < 2.0: c = CU_LO
                    elif r <= 1.5: c = noisy(CU_MID, 2)
                    for vx, vy in ((s / 2, 0), (0, hgt), (s, hgt)):
                        bx, by = vx + (cen[0] - vx) * 0.5, vy + (cen[1] - vy) * 0.5
                        b = bolt((px - bx) / 0.6, (py - by) / 0.6)
                        if b: c = b
                    if c == cell[key] and d < 1.5: c = CU_LO
                im.putpixel((x, y), mul(c, k))
        return im
    tb = A + "/textures/block/"
    save(tri(0.9), tb + "fluid_tank_corner.png")
    save(tri(0.66), tb + "fluid_tank_corner_low.png")
    save(tri(1.0, True), tb + "fluid_tank_corner_inner.png")

    t = {"window": f"{MOD}:block/fluid_tank_window", "port": f"{MOD}:block/fluid_tank_port",
         "inner": f"{MOD}:block/fluid_tank_inner", "corner": f"{MOD}:block/fluid_tank_corner",
         "corner_low": f"{MOD}:block/fluid_tank_corner_low", "corner_inner": f"{MOD}:block/fluid_tank_corner_inner",
         "particle": f"{MOD}:block/fluid_tank_port"}
    uv = [0, 0, 16, 16]
    a, b = 8 - s / 2, 8 + s / 2
    sq = []
    for axis, lo, hi, out, inn, tx in (("y", "down", "up", None, None, "#port"), ("z", "north", "south", None, None, "#window"),
                                       ("x", "west", "east", None, None, "#window")):
        for at, face, back in ((0, lo, hi), (16, hi, lo)):
            f = {face: {"texture": tx, "uv": uv, "cullface": face}, back: {"texture": "#inner" if tx == "#port" else tx, "uv": uv}}
            if axis == "y": fr, to, o = (a, at, a), (b, at, b), [8, at, 8]
            elif axis == "z": fr, to, o = (a, a, at), (b, b, at), [8, 8, at]
            else: fr, to, o = (at, a, a), (at, b, b), [at, 8, 8]
            sq.append(el(fr, to, f, {"origin": o, "axis": axis, "angle": 45}))
    tris = []
    for cx in (0, 16):
        for cy in (0, 16):
            for cz in (0, 16):
                c = (cx, cy, cz)
                mids = [tuple(8 if i == j else c[i] for i in range(3)) for j in range(3)]
                n = [(v - 8) / 8 / math.sqrt(3) for v in c]
                g = [sum(m[i] for m in mids) / 3 for i in range(3)]
                A0, B0, C0 = mids
                bd = [B0[i] - A0[i] for i in range(3)]
                if sum(x * x for x in bd) == 0: continue
                l = math.sqrt(sum(x * x for x in bd)); bd = [x / l for x in bd]
                mid = [(A0[i] + B0[i]) / 2 for i in range(3)]
                ad = [C0[i] - mid[i] for i in range(3)]; l = math.sqrt(sum(x * x for x in ad)); ad = [x / l for x in ad]
                cr = [bd[1] * ad[2] - bd[2] * ad[1], bd[2] * ad[0] - bd[0] * ad[2], bd[0] * ad[1] - bd[1] * ad[0]]
                if sum(cr[i] * n[i] for i in range(3)) < 0: bd = [-x for x in bd]; cr = [-x for x in cr]
                R = [[bd[i], ad[i], cr[i]] for i in range(3)]
                be = math.asin(max(-1, min(1, -R[2][0])))
                al = math.atan2(R[2][1], R[2][2])
                ga = math.atan2(R[1][0], R[0][0])
                fr = (g[0] - s / 2, g[1] - hgt / 3, g[2])
                to = (g[0] + s / 2, g[1] + hgt * 2 / 3, g[2])
                tx = "#corner" if cy else "#corner_low"
                faces = {"south": {"texture": tx, "uv": uv}, "north": {"texture": "#corner_inner", "uv": uv}}
                e = el(tuple(round(v, 4) for v in fr), tuple(round(v, 4) for v in to), faces,
                       {"origin": [round(v, 4) for v in g], "x": round(math.degrees(al), 4),
                        "y": round(math.degrees(be), 4), "z": round(math.degrees(ga), 4)})
                e["shade"] = False
                tris.append(e)
    model("fluid_tank", sq + tris, t)
    jdump({"variants": {"": {"model": f"{MOD}:block/fluid_tank"}}}, f"{A}/blockstates/fluid_tank.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/fluid_tank"}}, f"{A}/items/fluid_tank.json")
    jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
           "conditions": [{"condition": "minecraft:survives_explosion"}],
           "entries": [{"type": "minecraft:item", "name": f"{MOD}:fluid_tank", "functions": [
               {"function": "minecraft:copy_components", "source": "block_entity", "include": [f"{MOD}:tank"]}]}]}],
           "random_sequence": f"{MOD}:blocks/fluid_tank"}, f"{D}/{MOD}/loot_table/blocks/fluid_tank.json")
    shaped("fluid_tank", ["CSC", "GBG", "CSC"], {"C": "#c:plates/copper", "S": "#c:plates/iron",
                                                 "G": "#c:glass_blocks", "B": "minecraft:bucket"})

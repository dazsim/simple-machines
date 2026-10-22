#!/usr/bin/env python3
# Writes textures and the json resources. Run from the repo root: python3 tools/gen_assets.py
import json, os, random
from PIL import Image

R = "common/src/main/resources"
A = R + "/assets/simplemachines"
D = R + "/data"
MOD = "simplemachines"

def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)

def jdump(obj, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")

def px(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), c)

def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, c)

def jitter(c, rnd, k=8):
    d = rnd.randint(-k, k)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)

# palettes
BRICK = [(150, 70, 52), (132, 60, 44), (164, 82, 60)]
MORTAR = (122, 108, 96)
IRON = [(196, 196, 196), (170, 170, 170), (138, 138, 138), (96, 96, 100)]
STEEL = [(214, 218, 226), (162, 168, 180), (118, 124, 138), (72, 76, 88)]
COPPER = [(236, 146, 104), (196, 108, 76), (132, 66, 46)]
RCOPPER = [(244, 120, 92), (200, 60, 52), (126, 30, 34)]
DARK = (36, 30, 28)
FIRE = [(255, 220, 120), (255, 160, 48), (220, 90, 24)]

def bricks(seed):
    rnd = random.Random(seed)
    t = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                t.putpixel((x, y), MORTAR + (255,))
            else:
                t.putpixel((x, y), jitter(BRICK[(x + off) // 8 % 3 if rnd.random() < 0.8 else 2], rnd, 6))
    return t

def plate(pal, seed, rivets=True):
    rnd = random.Random(seed)
    t = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t.putpixel((x, y), jitter(pal[1], rnd, 5))
    for i in range(16):
        px(t, i, 0, pal[0] + (255,)); px(t, 0, i, pal[0] + (255,))
        px(t, i, 15, pal[3] + (255,)); px(t, 15, i, pal[3] + (255,))
    if rivets:
        for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
            px(t, x, y, pal[0] + (255,)); px(t, x + 1, y + 1, pal[2] + (255,))
    return t

def copper_trim(t, x0, y0, x1, y1):
    rect(t, x0, y0, x1, y0, COPPER[0] + (255,)); rect(t, x0, y0, x0, y1, COPPER[0] + (255,))
    rect(t, x0, y1, x1, y1, COPPER[2] + (255,)); rect(t, x1, y0, x1, y1, COPPER[2] + (255,))

def alloy_front(lit):
    t = bricks(3)
    rect(t, 3, 7, 12, 14, COPPER[1] + (255,))
    copper_trim(t, 3, 7, 12, 14)
    rect(t, 4, 8, 11, 13, DARK + (255,))
    if lit:
        rnd = random.Random(9)
        for x in range(4, 12):
            h = rnd.randint(2, 5)
            for y in range(13 - h, 14):
                px(t, x, y, FIRE[min(2, (13 - y) * 3 // (h + 1))] + (255,))
    rect(t, 6, 2, 9, 3, COPPER[1] + (255,))
    rect(t, 6, 2, 9, 2, COPPER[0] + (255,))
    return t

def alloy_top():
    t = bricks(5)
    rect(t, 4, 4, 11, 11, COPPER[1] + (255,))
    copper_trim(t, 4, 4, 11, 11)
    rect(t, 6, 6, 9, 9, DARK + (255,))
    return t

def extruder_front(lit):
    t = plate(IRON, 7)
    rect(t, 4, 3, 11, 10, IRON[2] + (255,))
    for (x, y) in [(7, 4), (8, 4), (5, 6), (10, 6), (5, 7), (10, 7), (7, 9), (8, 9)]:
        px(t, x, y, COPPER[1] + (255,))
    rect(t, 6, 5, 9, 8, COPPER[2] + (255,))
    rect(t, 7, 6, 8, 7, DARK + (255,))
    rect(t, 4, 12, 11, 14, DARK + (255,))
    rect(t, 4, 12, 11, 12, IRON[3] + (255,))
    if lit:
        for x in range(5, 11):
            px(t, x, 14, FIRE[1] + (255,)); px(t, x, 13, FIRE[2 if x % 2 else 0] + (255,))
    return t

def extruder_top():
    t = plate(IRON, 11)
    rect(t, 5, 5, 10, 10, IRON[3] + (255,))
    rect(t, 6, 6, 9, 9, DARK + (255,))
    return t

INGOT = [
    "................",
    "................",
    "................",
    "................",
    ".........oooooo.",
    ".......ooHHHHHo.",
    ".....ooHHHHHHMo.",
    "...ooHHHHHHHMLo.",
    ".ooHHHHHHHHMLLo.",
    ".oMMMMMMMMMLLDo.",
    ".oMMMMMMMMMLDDo.",
    ".oLLLLLLLLLDDo..",
    ".oDDDDDDDDDDo...",
    "..oooooooooo....",
    "................",
    "................",
]

ROD = [
    "................",
    "............ooo.",
    "...........oHMo.",
    "..........oHMLo.",
    ".........oHMLo..",
    "........oHMLo...",
    ".......oHMLo....",
    "......oHMLo.....",
    ".....oHMLo......",
    "....oHMLo.......",
    "...oHMLo........",
    "..oHMLo.........",
    ".oHMLo..........",
    ".oMLo...........",
    ".ooo............",
    "................",
]

def sprite(mask, pal):
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out = tuple(max(0, v - 40) for v in pal[-1][:3])
    cols = {"H": pal[0], "M": pal[1], "L": pal[2], "D": pal[-1], "o": out}
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch in cols:
                t.putpixel((x, y), cols[ch][:3] + (255,))
    return t

def gui(inputs):
    t = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    W, H = 176, 166
    rect(t, 1, 1, W - 2, H - 2, (198, 198, 198, 255))
    rect(t, 1, 0, W - 3, 0, (0, 0, 0, 255)); rect(t, 0, 1, 0, H - 3, (0, 0, 0, 255))
    rect(t, 2, H - 1, W - 2, H - 1, (0, 0, 0, 255)); rect(t, W - 1, 2, W - 1, H - 2, (0, 0, 0, 255))
    px(t, W - 2, 1, (0, 0, 0, 255)); px(t, 1, H - 2, (0, 0, 0, 255)); px(t, W - 2, H - 2, (0, 0, 0, 255))
    rect(t, 1, 1, W - 3, 2, (255, 255, 255, 255)); rect(t, 1, 1, 2, H - 3, (255, 255, 255, 255))
    rect(t, 3, H - 3, W - 2, H - 2, (85, 85, 85, 255)); rect(t, W - 3, 3, W - 2, H - 2, (85, 85, 85, 255))
    px(t, W - 2, H - 2, (0, 0, 0, 0)); px(t, W - 3, H - 3, (85, 85, 85, 255))

    def slot(x, y, s=18):
        rect(t, x, y, x + s - 1, y + s - 1, (139, 139, 139, 255))
        rect(t, x, y, x + s - 2, y, (55, 55, 55, 255)); rect(t, x, y, x, y + s - 2, (55, 55, 55, 255))
        rect(t, x + 1, y + s - 1, x + s - 1, y + s - 1, (255, 255, 255, 255)); rect(t, x + s - 1, y + 1, x + s - 1, y + s - 1, (255, 255, 255, 255))

    for r in range(3):
        for c in range(9):
            slot(7 + c * 18, 83 + r * 18)
    for c in range(9):
        slot(7 + c * 18, 141)
    grey = (139, 139, 139, 255)
    if inputs in ("gen", "mill"):
        rect(t, 152, 16, 168, 70, (255, 255, 255, 255))
        rect(t, 152, 16, 167, 69, (55, 55, 55, 255))
        rect(t, 153, 17, 166, 68, (40, 30, 28, 255))
        if inputs == "gen":
            slot(79, 52)
            for (x0, y0, x1, y1) in [(86, 38, 87, 47), (83, 41, 84, 48), (89, 42, 90, 48), (84, 48, 90, 49)]:
                rect(t, x0, y0, x1, y1, grey)
            return t
        slot(55, 34)
        slot(111, 30, 26)
        inputs = 0
    else:
        xs = [46, 64] if inputs == 2 else [55]
        for x in xs:
            slot(x, 16)
        slot(55, 52)
        slot(111, 30, 26)
    for i in range(16):
        rect(t, 80, 41, 80 + min(i, 15), 42, grey)
    for k in range(8):
        rect(t, 95 + k, 35 + k, 95 + k, 48 - k, grey)
    if inputs:
        for (x0, y0, x1, y1) in [(62, 38, 63, 47), (59, 41, 60, 48), (65, 42, 66, 48), (60, 48, 66, 49)]:
            rect(t, x0, y0, x1, y1, grey)
    return t

def main():
    tb, ti = A + "/textures/block/", A + "/textures/item/"
    save(bricks(1), tb + "alloy_furnace_side.png")
    save(alloy_top(), tb + "alloy_furnace_top.png")
    save(alloy_front(False), tb + "alloy_furnace_front.png")
    save(alloy_front(True), tb + "alloy_furnace_front_on.png")
    save(plate(IRON, 2), tb + "extruder_side.png")
    save(extruder_top(), tb + "extruder_top.png")
    save(extruder_front(False), tb + "extruder_front.png")
    save(extruder_front(True), tb + "extruder_front_on.png")
    save(sprite(ROD, IRON), ti + "iron_rod.png")
    save(sprite(ROD, COPPER), ti + "copper_rod.png")
    save(sprite(ROD, STEEL), ti + "steel_rod.png")
    save(gui(2), A + "/textures/gui/alloy_furnace.png")
    save(gui(1), A + "/textures/gui/extruder.png")
    save(gui("gen"), A + "/textures/gui/solid_fuel_generator.png")
    save(gui("mill"), A + "/textures/gui/rolling_mill.png")
    save(gui("mill"), A + "/textures/gui/electric_furnace.png")

    for m in ["alloy_furnace", "extruder"]:
        variants = {}
        for i, f in enumerate(["north", "east", "south", "west"]):
            for lit in ["false", "true"]:
                v = {"model": f"{MOD}:block/{m}" + ("_on" if lit == "true" else "")}
                if i:
                    v["y"] = 90 * i
                variants[f"facing={f},lit={lit}"] = v
        jdump({"variants": variants}, f"{A}/blockstates/{m}.json")
        for on in ["", "_on"]:
            jdump({"parent": "minecraft:block/orientable", "textures": {
                "front": f"{MOD}:block/{m}_front{on}", "side": f"{MOD}:block/{m}_side", "top": f"{MOD}:block/{m}_top"}},
                f"{A}/models/block/{m}{on}.json")
    items = ["steel_ingot", "redstone_copper_ingot", "iron_rod", "copper_rod", "steel_rod"]
    for it in items:
        jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{it}"}}, f"{A}/models/item/{it}.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{it}"}}, f"{A}/items/{it}.json")
    blocks = ["alloy_furnace", "extruder"]
    for b in blocks:
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/{b}"}}, f"{A}/items/{b}.json")
        jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
               "conditions": [{"condition": "minecraft:survives_explosion"}],
               "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}]}],
               "random_sequence": f"{MOD}:blocks/{b}"}, f"{D}/{MOD}/loot_table/blocks/{b}.json")

    jdump({"values": [f"{MOD}:{b}" for b in blocks + BUILT + CHASSIS + ["redstone_copper_cable"]]}, f"{D}/minecraft/tags/block/mineable/pickaxe.json")
    jdump({"values": [f"{MOD}:steel_ingot"]}, f"{D}/c/tags/item/ingots/steel.json")
    for m in ["iron", "copper", "steel"]:
        jdump({"values": [f"{MOD}:{m}_rod"]}, f"{D}/c/tags/item/rods/{m}.json")

    rec = f"{D}/{MOD}/recipe/"
    jdump({"type": f"{MOD}:alloying", "first": {"ingredient": "#c:ingots/copper"}, "second": {"ingredient": "#c:dusts/redstone"},
           "result": {"id": f"{MOD}:redstone_copper_ingot"}, "cooking_time": 200}, rec + "alloying/redstone_copper_ingot.json")
    jdump({"type": f"{MOD}:alloying", "first": {"ingredient": "#c:ingots/iron"}, "second": {"ingredient": "#minecraft:coals", "count": 2},
           "result": {"id": f"{MOD}:steel_ingot"}, "cooking_time": 400}, rec + "alloying/steel_ingot.json")
    for m in ["iron", "copper"]:
        jdump({"type": f"{MOD}:extruding", "input": {"ingredient": f"#c:ingots/{m}"},
               "result": {"id": f"{MOD}:{m}_rod", "count": 2}, "cooking_time": 400}, rec + f"extruding/{m}_rod.json")

    for m in ["iron", "copper", "steel"]:
        jdump({"type": f"{MOD}:rolling", "input": {"ingredient": f"#c:ingots/{m}"},
               "result": {"id": f"{MOD}:{m}_rod", "count": 2}, "cooking_time": 100}, rec + f"rolling/{m}_rod.json")

    def shaped(name, pattern, key, count=1):
        result = {"id": f"{MOD}:{name}"}
        if count > 1:
            result["count"] = count
        jdump({"type": "minecraft:crafting_shaped", "category": "misc", "key": key, "pattern": pattern,
               "result": result}, rec + name + ".json")
    shaped("alloy_furnace", ["BBB", "CFC", "BBB"], {"B": "minecraft:bricks", "C": "#c:ingots/copper", "F": "minecraft:furnace"})
    shaped("extruder", ["III", "IPI", "IFI"], {"I": "#c:ingots/iron", "P": "minecraft:piston", "F": "minecraft:furnace"})
    shaped("steel_chassis", ["IRI", "RRR", "IRI"], {"I": "#c:ingots/steel", "R": "#c:rods/steel"})
    built(shaped, rec)

# ---- assembled machines, chassis, parts and cable ----

BUILT = ["electric_furnace", "rolling_mill", "solid_fuel_generator"]
CHASSIS = ["iron_chassis", "steel_chassis"]
CORES = {"furnace": "electric_furnace", "rolling": "rolling_mill", "generator": "solid_fuel_generator"}
FINISHES = ["wood", "iron", "copper", "steel"]
FACINGS = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
COPPER4 = [(236, 146, 104), (196, 108, 76), (160, 84, 58), (110, 54, 38)]
WOOD = [(190, 152, 98), (162, 130, 78), (134, 106, 64), (98, 78, 46)]
RED = [(255, 76, 52), (196, 36, 30), (110, 20, 18)]
BOARD = (44, 56, 72)
PALETTES = {"iron": IRON, "copper": COPPER4, "steel": STEEL, "wood": WOOD}
# front opening per core as (x0, y0, x1, y1) in model space; the generator and furnace show fire, the mill its rollers
FRONT_HOLE = {"furnace": (4, 4, 12, 11), "rolling": (3, 6, 13, 10), "generator": (5, 3, 11, 10)}
SIDE_HOLE = (6, 6, 10, 10)
TOP_HOLE = {"furnace": (5, 5, 11, 11), "rolling": (4, 6, 12, 10), "generator": None}

def frame(pal, seed):
    rnd = random.Random(seed)
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x < 2 or x > 13 or y < 2 or y > 13:
                t.putpixel((x, y), jitter(pal[1], rnd, 6))
    for i in range(16):
        px(t, i, 0, pal[0] + (255,)); px(t, 0, i, pal[0] + (255,))
        px(t, i, 15, pal[3] + (255,)); px(t, 15, i, pal[3] + (255,))
    for i in range(2, 14):
        px(t, i, 13, pal[2] + (255,)); px(t, 13, i, pal[2] + (255,))
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        px(t, x, y, pal[0] + (255,))
    return t

def core_base(seed):
    t = plate(STEEL, seed, rivets=False)
    for y in range(16):
        for x in range(16):
            r, g, b, _ = t.getpixel((x, y))
            t.putpixel((x, y), (r * 3 // 4, g * 3 // 4, b * 4 // 5, 255))
    return t

def core_front(core, lit):
    t = core_base(51)
    x0, y0, x1, y1 = FRONT_HOLE[core]
    u0, u1, v0, v1 = x0, x1 - 1, 16 - y1, 15 - y0
    rect(t, u0, v0, u1, v1, DARK + (255,))
    if core == "furnace":
        for v in range(v0 + 1, v1, 2):
            rect(t, u0 + 1, v, u1 - 1, v, (FIRE[1] if lit else RED[2]) + (255,))
        if lit:
            rect(t, u0 + 1, v0 + 1, u1 - 1, v0 + 1, FIRE[0] + (255,))
    elif core == "rolling":
        for v in (v0, v1 - 1):
            rect(t, u0 + 1, v, u1 - 1, v + 1, COPPER[1] + (255,))
            rect(t, u0 + 1, v, u1 - 1, v, COPPER[0] + (255,))
        if lit:
            rect(t, u0 + 1, v0 + 2, u1 - 1, v0 + 2, FIRE[1] + (255,))
    else:
        if lit:
            rnd = random.Random(19)
            for u in range(u0, u1 + 1):
                h = rnd.randint(2, 5)
                for v in range(v1 - h, v1 + 1):
                    px(t, u, v, FIRE[min(2, (v1 - v) * 3 // (h + 1))] + (255,))
        for u in range(u0 + 1, u1, 2):
            rect(t, u, v0, u, v1, STEEL[3] + (255,))
    px(t, 12, 3, (RED[0] if lit else RED[2]) + (255,))
    return t

def core_side():
    t = core_base(53)
    rect(t, 6, 6, 9, 9, COPPER[2] + (255,))
    rect(t, 6, 6, 9, 6, COPPER[0] + (255,)); rect(t, 6, 6, 6, 9, COPPER[0] + (255,))
    rect(t, 7, 7, 8, 8, RED[1] + (255,))
    return t

def core_top(core):
    t = core_base(57)
    hole = TOP_HOLE[core]
    if hole:
        x0, z0, x1, z1 = hole
        rect(t, x0, z0, x1 - 1, z1 - 1, DARK + (255,))
        if core == "rolling":
            rect(t, x0 + 1, z0 + 1, x1 - 2, z0 + 1, COPPER[0] + (255,))
            rect(t, x0 + 1, z1 - 2, x1 - 2, z1 - 2, COPPER[1] + (255,))
        else:
            rect(t, x0 + 1, z0 + 1, x1 - 2, z0 + 1, STEEL[2] + (255,))
    else:
        for z in (5, 8, 11):
            rect(t, 4, z, 11, z, DARK + (255,))
    return t

def board_tex():
    t = Image.new("RGBA", (16, 16), BOARD + (255,))
    for x in range(16):
        px(t, x, 0, (64, 78, 96, 255)); px(t, x, 15, (28, 36, 48, 255))
    for (x0, y0, x1, y1) in [(3, 4, 12, 4), (3, 4, 3, 11), (12, 4, 12, 8), (6, 8, 12, 8), (6, 8, 6, 12), (9, 11, 13, 11)]:
        rect(t, x0, y0, x1, y1, RED[1] + (255,))
    for (x, y) in [(3, 11), (12, 8), (6, 12), (13, 11), (9, 11)]:
        px(t, x, y, COPPER[0] + (255,))
    rect(t, 8, 5, 10, 6, (24, 24, 28, 255))
    return t

def sheet_tex(name):
    pal = PALETTES[name]
    t = plate(pal, 61 + len(name), rivets=False)
    for i in range(1, 15):
        px(t, i, 1, pal[0] + (255,))
    return t

def worktop_tex(name):
    pal = PALETTES[name]
    t = plate(pal, 67 + len(name), rivets=False)
    rect(t, 1, 1, 14, 1, pal[0] + (255,)); rect(t, 1, 1, 1, 14, pal[0] + (255,))
    return t

def cable_tex():
    t = Image.new("RGBA", (16, 16))
    rnd = random.Random(71)
    for y in range(16):
        for x in range(16):
            t.putpixel((x, y), jitter(RCOPPER[1], rnd, 6))
    for i in range(16):
        px(t, i, 6, RCOPPER[0] + (255,)); px(t, i, 9, RCOPPER[2] + (255,))
        px(t, 6, i, RCOPPER[0] + (255,)); px(t, 9, i, RCOPPER[2] + (255,))
    return t

SHEET = [
    "................",
    "................",
    "................",
    "................",
    "........oooooo..",
    ".....oooHHHHHHo.",
    "..oooHHHHHHHHMo.",
    ".oHHHHHHHHHMMLo.",
    ".oMHHHHHHMMMLLo.",
    ".oLMMMMMMMMLLDo.",
    ".oDLLLLLLLLLDo..",
    "..ooDDDDDDDDo...",
    "....oooooooo....",
    "................",
    "................",
    "................",
]

PANELS = [
    "................",
    "..oooooooooo....",
    "..oHHHHHHHHo....",
    "..oHMMMMMMLo....",
    "..oHMoooooooooo.",
    "..oHMoHHHHHHHHo.",
    "..oHMoHMMMMMMLo.",
    "..oHMoHMMMMMMLo.",
    "..oHMoHMMMMMMLo.",
    "..oLLoHMMMMMMLo.",
    "..ooooHMMMMMMLo.",
    "......oHMMMMMLo.",
    "......oLLLLLLLo.",
    "......oooooooooo",
    "................",
    "................",
]

WORKTOP = [
    "................",
    "................",
    "................",
    "................",
    "......oooooooo..",
    "....ooHHHHHHHo..",
    "..ooHHHHHHHHMo..",
    ".oHHHHHHHHHMMo..",
    ".oHHHHHHHHMMLo..",
    ".oMMMMMMMMMLDo..",
    ".oLLLLLLLLLDo...",
    ".ooooooooooo....",
    "................",
    "................",
    "................",
    "................",
]

BOARD_ITEM = [
    "................",
    "................",
    "..oooooooooooo..",
    "..oBBBBBBBBBBo..",
    "..oBRRRRRRRCBo..",
    "..oBRBBKKBBRBo..",
    "..oBRBBKKBBRBo..",
    "..oBCBBBBBBRBo..",
    "..oBBBRRRRRRBo..",
    "..oBBBRBBBBBBo..",
    "..oBBBCBBCRRBo..",
    "..oBBBBBBBBBBo..",
    "..oooooooooooo..",
    "....C.C.C.C.....",
    "................",
    "................",
]

def board_item():
    cols = {"o": (24, 30, 40), "B": BOARD, "R": RED[1], "C": COPPER[0], "K": (20, 20, 24)}
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(BOARD_ITEM):
        for x, ch in enumerate(row):
            if ch in cols:
                t.putpixel((x, y), cols[ch] + (255,))
    return t

# ---- model geometry ----

def el(a, b, faces, rot=None):
    e = {"from": list(a), "to": list(b), "faces": faces}
    if rot:
        e["rotation"] = rot
    return e

def all_faces(tex, skip=()):
    return {d: {"texture": "#" + tex} for d in ["north", "south", "east", "west", "up", "down"] if d not in skip}

def frame_elements():
    out = []
    for x in (0, 14):
        for z in (0, 14):
            out.append(el((x, 0, z), (x + 2, 14, z + 2), all_faces("frame")))
    out.append(el((2, 0, 0), (14, 2, 2), all_faces("frame", ("east", "west"))))
    out.append(el((2, 0, 14), (14, 2, 16), all_faces("frame", ("east", "west"))))
    out.append(el((0, 0, 2), (2, 2, 14), all_faces("frame", ("north", "south"))))
    out.append(el((14, 0, 2), (16, 2, 14), all_faces("frame", ("north", "south"))))
    out.append(el((2, 1, 2), (14, 2, 14), {"up": {"texture": "#floor"}}))
    return out

def core_elements(loose):
    faces = {"north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
             "west": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#bottom"}}
    rot = {"origin": [8, 8, 8], "axis": "y", "angle": 22.5} if loose else None
    return [el((3, 2, 3), (13, 12, 13), faces, rot)]

def board_elements(loose):
    rot = {"origin": [13.5, 2, 8], "axis": "z", "angle": -22.5} if loose else None
    return [el((13.25, 3, 4), (14, 12, 12), all_faces("board"), rot)]

def holed(a0, b0, a1, b1, hole):
    """Rectangles covering (a0,b0)-(a1,b1) minus the hole."""
    if not hole:
        return [(a0, b0, a1, b1)]
    h0, k0, h1, k1 = hole
    return [(a0, b0, a1, k0), (a0, k1, a1, b1), (a0, k0, h0, k1), (h1, k0, a1, k1)]

def turn(e, quarter):
    """Rotate an element about the block's vertical axis, north going to east per quarter."""
    for _ in range(quarter):
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        e["from"], e["to"] = [16 - z1, y0, x0], [16 - z0, y1, x1]
    return e

def wall(rects, quarter, out, tex):
    d0, d1 = 0.5 - out, 1.5 - out
    return [turn(el((a0, b0, d0), (a1, b1, d1), all_faces(tex)), quarter) for (a0, b0, a1, b1) in rects]

def front_panel(core, loose):
    x0, y0, x1, y1 = FRONT_HOLE[core]
    return wall(holed(2, 2, 14, 14, (16 - x1, y0, 16 - x0, y1)), 0, 1 if loose else 0, "panel")

def side_panels(loose):
    out = 1 if loose else 0
    side = holed(2, 2, 14, 14, SIDE_HOLE)
    return wall(side, 1, out, "panel") + wall(holed(2, 2, 14, 14, None), 2, out, "panel") + wall(side, 3, out, "panel")

def worktop_elements(core, loose):
    lift = 1.5 if loose else 0
    hole = TOP_HOLE[core]
    rects = holed(0, 0, 16, 16, hole)
    return [el((a0, 14 + lift, b0), (a1, 16 + lift, b1), all_faces("worktop")) for (a0, b0, a1, b1) in rects]

def model(name, elements, textures, parent="minecraft:block/block"):
    jdump({"parent": parent, "ambientocclusion": False, "textures": textures, "elements": elements},
          f"{A}/models/block/{name}.json")
    return f"{MOD}:block/{name}"

def finish_tex(f, part):
    if f == "wood":
        return "minecraft:block/oak_planks" if part == "panel" else "minecraft:block/spruce_planks"
    return f"{MOD}:block/{f}_{part}"

def built(shaped, rec):
    tb, ti = A + "/textures/block/", A + "/textures/item/"
    save(frame(IRON, 13), tb + "iron_chassis.png")
    save(frame([(150, 160, 178), (112, 122, 140), (82, 90, 106), (52, 56, 68)], 13), tb + "steel_chassis.png")
    save(core_base(59), tb + "chassis_floor.png")
    for core in CORES:
        save(core_front(core, False), tb + f"{core}_core_front.png")
        save(core_front(core, True), tb + f"{core}_core_front_on.png")
        save(core_top(core), tb + f"{core}_core_top.png")
    save(core_side(), tb + "core_side.png")
    save(core_base(63), tb + "core_bottom.png")
    save(board_tex(), tb + "control_board.png")
    for f in ["iron", "copper", "steel"]:
        save(sheet_tex(f), tb + f"{f}_panel.png")
        save(worktop_tex(f), tb + f"{f}_worktop.png")
        save(sprite(SHEET, PALETTES[f]), ti + f"{f}_sheet.png")
    for f in FINISHES:
        save(sprite(PANELS, PALETTES[f]), ti + f"{f}_panels.png")
        save(sprite(WORKTOP, PALETTES[f]), ti + f"{f}_worktop.png")
    save(board_item(), ti + "control_board.png")
    save(cable_tex(), tb + "redstone_copper_cable.png")
    save(Image.open("/workspace/mods/morestorage-261/common/src/main/resources/assets/muchmorestorage/textures/item/screwdriver.png"),
         ti + "screwdriver.png")

    frames = {}
    for tier in ["iron", "steel"]:
        frames[tier] = model(f"{tier}_chassis", frame_elements(),
                             {"frame": f"{MOD}:block/{tier}_chassis", "floor": f"{MOD}:block/chassis_floor",
                              "particle": f"{MOD}:block/{tier}_chassis"})
    core_tex = lambda c, on: {"front": f"{MOD}:block/{c}_core_front" + on, "side": f"{MOD}:block/core_side",
                              "top": f"{MOD}:block/{c}_core_top", "bottom": f"{MOD}:block/core_bottom",
                              "particle": f"{MOD}:block/core_side"}
    for c in CORES:
        model(f"{c}_core", core_elements(False), core_tex(c, ""))
        model(f"{c}_core_on", core_elements(False), core_tex(c, "_on"))
        model(f"{c}_core_loose", core_elements(True), core_tex(c, ""))
    board_t = {"board": f"{MOD}:block/control_board", "particle": f"{MOD}:block/control_board"}
    model("control_board", board_elements(False), board_t)
    model("control_board_loose", board_elements(True), board_t)
    for f in FINISHES:
        pt = {"panel": finish_tex(f, "panel"), "particle": finish_tex(f, "panel")}
        wt = {"worktop": finish_tex(f, "worktop"), "particle": finish_tex(f, "worktop")}
        for loose in ["", "_loose"]:
            model(f"{f}_side_panels{loose}", side_panels(bool(loose)), pt)
            for c in CORES:
                model(f"{c}_{f}_front_panel{loose}", front_panel(c, bool(loose)), pt)
                model(f"{c}_{f}_worktop{loose}", worktop_elements(c, bool(loose)), wt)

    def part(m, when, y):
        a = {"apply": {"model": f"{MOD}:block/{m}"}}
        if y:
            a["apply"]["y"] = y
        if when:
            a["when"] = when
        return a

    # chassis while it is being built
    for tier in ["iron", "steel"]:
        parts = [{"apply": {"model": frames[tier]}}]
        for f, y in FACINGS:
            for c in CORES:
                parts.append(part(f"{c}_core_loose", {"facing": f, "core": c, "step": "1"}, y))
                parts.append(part(f"{c}_core", {"facing": f, "core": c, "step": "2|3|4|5|6|7"}, y))
                for fin in FINISHES:
                    parts.append(part(f"{c}_{fin}_front_panel_loose", {"facing": f, "core": c, "panel": fin, "step": "5"}, y))
                    parts.append(part(f"{c}_{fin}_front_panel", {"facing": f, "core": c, "panel": fin, "step": "6|7"}, y))
                    parts.append(part(f"{c}_{fin}_worktop_loose", {"facing": f, "core": c, "worktop": fin, "step": "7"}, y))
            parts.append(part("control_board_loose", {"facing": f, "step": "3"}, y))
            parts.append(part("control_board", {"facing": f, "step": "4|5|6|7"}, y))
            for fin in FINISHES:
                parts.append(part(f"{fin}_side_panels_loose", {"facing": f, "panel": fin, "step": "5"}, y))
                parts.append(part(f"{fin}_side_panels", {"facing": f, "panel": fin, "step": "6|7"}, y))
        jdump({"multipart": parts}, f"{A}/blockstates/{tier}_chassis.json")
        jdump({"model": {"type": "minecraft:model", "model": frames[tier]}}, f"{A}/items/{tier}_chassis.json")
        loot_self(f"{tier}_chassis")

    # finished machines
    for c, block in CORES.items():
        parts = []
        for tier in ["iron", "steel"]:
            parts.append({"when": {"tier": tier}, "apply": {"model": frames[tier]}})
        for f, y in FACINGS:
            parts.append(part(f"{c}_core", {"facing": f, "lit": "false"}, y))
            parts.append(part(f"{c}_core_on", {"facing": f, "lit": "true"}, y))
            for fin in FINISHES:
                parts.append(part(f"{c}_{fin}_front_panel", {"facing": f, "panel": fin}, y))
                parts.append(part(f"{fin}_side_panels", {"facing": f, "panel": fin}, y))
                parts.append(part(f"{c}_{fin}_worktop", {"facing": f, "worktop": fin}, y))
        jdump({"multipart": parts}, f"{A}/blockstates/{block}.json")
        elements = frame_elements() + core_elements(False) + front_panel(c, False) + side_panels(False) + worktop_elements(c, False)
        item = model(f"{block}_item", elements, {"frame": f"{MOD}:block/iron_chassis", "floor": f"{MOD}:block/chassis_floor",
                     **core_tex(c, ""), "panel": finish_tex("wood", "panel"), "worktop": finish_tex("wood", "worktop")})
        jdump({"model": {"type": "minecraft:model", "model": item}}, f"{A}/items/{block}.json")
        jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
               "conditions": [{"condition": "minecraft:survives_explosion"}],
               "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}", "functions": [
                   {"function": "minecraft:copy_state", "block": f"{MOD}:{block}", "properties": ["tier", "panel", "worktop"]},
                   {"function": "minecraft:copy_components", "source": "block_entity", "include": [f"{MOD}:energy"]}]}]}],
               "random_sequence": f"{MOD}:blocks/{block}"}, f"{D}/{MOD}/loot_table/blocks/{block}.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/{c}_core"}}, f"{A}/items/{c}_assembly.json")

    # cable
    ct = {"cable": f"{MOD}:block/redstone_copper_cable", "particle": f"{MOD}:block/redstone_copper_cable"}
    core = el((6, 6, 6), (10, 10, 10), all_faces("cable"))
    arms = {"north": el((6, 6, 0), (10, 10, 6), all_faces("cable", ("south",))),
            "south": el((6, 6, 10), (10, 10, 16), all_faces("cable", ("north",))),
            "west": el((0, 6, 6), (6, 10, 10), all_faces("cable", ("east",))),
            "east": el((10, 6, 6), (16, 10, 10), all_faces("cable", ("west",))),
            "down": el((6, 0, 6), (10, 6, 10), all_faces("cable", ("up",))),
            "up": el((6, 10, 6), (10, 16, 10), all_faces("cable", ("down",)))}
    model("redstone_copper_cable", [core], ct)
    parts = [{"apply": {"model": f"{MOD}:block/redstone_copper_cable"}}]
    for d, e in arms.items():
        parts.append({"when": {d: "true"}, "apply": {"model": model(f"redstone_copper_cable_{d}", [e], ct)}})
    jdump({"multipart": parts}, f"{A}/blockstates/redstone_copper_cable.json")
    cable_item = model("redstone_copper_cable_item", [arms["west"], core, arms["east"]], ct)
    jdump({"model": {"type": "minecraft:model", "model": cable_item}}, f"{A}/items/redstone_copper_cable.json")
    loot_self("redstone_copper_cable")

    flat = ["iron_sheet", "copper_sheet", "steel_sheet", "control_board"] + \
           [f"{f}_{k}" for f in FINISHES for k in ["panels", "worktop"]]
    for it in flat:
        jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{it}"}}, f"{A}/models/item/{it}.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{it}"}}, f"{A}/items/{it}.json")
    jdump({"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/screwdriver"}}, f"{A}/models/item/screwdriver.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/screwdriver"}}, f"{A}/items/screwdriver.json")

    lang = {
        "itemGroup.simplemachines": "Simple Machines",
        "item.simplemachines.steel_ingot": "Steel Ingot",
        "item.simplemachines.redstone_copper_ingot": "Redstone Copper Ingot",
        "item.simplemachines.screwdriver": "Screwdriver",
        "item.simplemachines.control_board": "Control Board",
        "item.simplemachines.furnace_assembly": "Furnace Assembly",
        "item.simplemachines.rolling_assembly": "Rolling Assembly",
        "item.simplemachines.generator_assembly": "Generator Assembly",
        "block.simplemachines.alloy_furnace": "Alloy Furnace",
        "block.simplemachines.extruder": "Extruder",
        "block.simplemachines.solid_fuel_generator": "Solid Fuel Generator",
        "block.simplemachines.rolling_mill": "Rolling Mill",
        "block.simplemachines.electric_furnace": "Electric Furnace",
        "block.simplemachines.iron_chassis": "Iron Chassis",
        "block.simplemachines.steel_chassis": "Steel Chassis",
        "block.simplemachines.redstone_copper_cable": "Redstone Copper Cable",
        "container.simplemachines.alloy_furnace": "Alloy Furnace",
        "container.simplemachines.extruder": "Extruder",
        "container.simplemachines.solid_fuel_generator": "Solid Fuel Generator",
        "container.simplemachines.rolling_mill": "Rolling Mill",
        "container.simplemachines.electric_furnace": "Electric Furnace",
        "gui.simplemachines.rods": "Rods",
        "gui.simplemachines.sheets": "Sheets",
        "message.simplemachines.needs_assembly": "Fit a mechanical assembly first",
        "message.simplemachines.needs_board": "Needs a control board",
        "message.simplemachines.needs_panels": "Needs side panels",
        "message.simplemachines.needs_worktop": "Needs a worktop",
        "message.simplemachines.loose": "Screw in the loose part first",
    }
    for m in ["iron", "copper", "steel"]:
        lang[f"item.simplemachines.{m}_rod"] = f"{m.title()} Rod"
        lang[f"item.simplemachines.{m}_sheet"] = f"{m.title()} Sheet"
    for f in FINISHES:
        name = "Wooden" if f == "wood" else f.title()
        lang[f"item.simplemachines.{f}_panels"] = f"{name} Panels"
        lang[f"item.simplemachines.{f}_worktop"] = f"{name} Worktop"
    jdump(lang, f"{A}/lang/en_us.json")

    for m in ["iron", "copper", "steel"]:
        jdump({"type": f"{MOD}:rolling", "input": {"ingredient": f"#c:ingots/{m}"}, "result": {"id": f"{MOD}:{m}_sheet"},
               "cooking_time": 100, "sheet": True}, rec + f"rolling/{m}_sheet.json")
        jdump({"values": [f"{MOD}:{m}_sheet"]}, f"{D}/c/tags/item/plates/{m}.json")
        shaped(f"{m}_panels", ["SS", "SS"], {"S": f"#c:plates/{m}"})
        shaped(f"{m}_worktop", ["SSS"], {"S": f"#c:plates/{m}"})
    shaped("wood_panels", ["SS", "SS"], {"S": "#minecraft:wooden_slabs"})
    shaped("wood_worktop", ["SSS"], {"S": "#minecraft:wooden_slabs"})
    shaped("iron_chassis", ["IBI", "B B", "IBI"], {"I": "#c:ingots/iron", "B": "minecraft:iron_bars"})
    shaped("screwdriver", ["C", "S"], {"C": "#c:ingots/copper", "S": "minecraft:stick"})
    shaped("control_board", ["RDR", "CCC"], {"R": f"{MOD}:redstone_copper_ingot", "D": "#c:dusts/redstone", "C": "#c:ingots/copper"})
    shaped("furnace_assembly", ["RCR", "RFR"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "F": "minecraft:furnace"})
    shaped("rolling_assembly", ["RCR", "PIP"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot",
                                               "P": "minecraft:piston", "I": "#c:ingots/iron"})
    shaped("generator_assembly", ["RCR", "IFI"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot",
                                                 "I": "#c:ingots/iron", "F": "minecraft:furnace"})
    shaped("redstone_copper_cable", ["RRR"], {"R": f"{MOD}:redstone_copper_ingot"}, 8)

def loot_self(b):
    jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
           "conditions": [{"condition": "minecraft:survives_explosion"}],
           "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}]}],
           "random_sequence": f"{MOD}:blocks/{b}"}, f"{D}/{MOD}/loot_table/blocks/{b}.json")

main()

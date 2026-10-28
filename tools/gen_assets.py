#!/usr/bin/env python3
# Writes textures and the json resources. Run from the repo root: python3 tools/gen_assets.py
import glob, json, os, random, shutil, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
import icons
import powered_icons
import gen_manual
import gen_tools
import gen_pipes

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
    # alloys take the metal as an ingot or as dust, from any mod
    for m in ["iron", "copper"]:
        jdump({"values": [f"#c:ingots/{m}", f"#c:dusts/{m}"]}, f"{D}/{MOD}/tags/item/ingots_or_dusts/{m}.json")
    jdump({"type": f"{MOD}:alloying", "first": {"ingredient": f"#{MOD}:ingots_or_dusts/copper"}, "second": {"ingredient": "#c:dusts/redstone"},
           "result": {"id": f"{MOD}:redstone_copper_ingot"}, "cooking_time": 200}, rec + "alloying/redstone_copper_ingot.json")
    jdump({"type": f"{MOD}:alloying", "first": {"ingredient": f"#{MOD}:ingots_or_dusts/iron"}, "second": {"ingredient": "#minecraft:coals", "count": 2},
           "result": {"id": f"{MOD}:steel_ingot"}, "cooking_time": 400}, rec + "alloying/steel_ingot.json")
    for m in ["iron", "copper"]:
        jdump({"type": f"{MOD}:extruding", "input": {"ingredient": f"#c:ingots/{m}"},
               "result": {"id": f"{MOD}:{m}_rod", "count": 2}, "cooking_time": 400}, rec + f"extruding/{m}_rod.json")

    for m in ["iron", "copper", "steel"]:
        jdump({"type": f"{MOD}:rolling", "input": {"ingredient": f"#c:ingots/{m}"},
               "result": {"id": f"{MOD}:{m}_rod", "count": 2}, "cooking_time": 100}, rec + f"rolling/{m}_rod.json")

    def shaped(name, pattern, key, count=1, cond=False):
        result = {"id": f"{MOD}:{name}"}
        if count > 1:
            result["count"] = count
        r = {}
        if cond:
            r["neoforge:conditions"] = [{"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": "muchmorestorage"}}]
            r["fabric:load_conditions"] = [{"condition": "fabric:not", "value": {"condition": "fabric:all_mods_loaded", "values": ["muchmorestorage"]}}]
        r.update({"type": "minecraft:crafting_shaped", "category": "misc", "key": key, "pattern": pattern, "result": result})
        jdump(r, rec + name + ".json")
    shaped("alloy_furnace", ["BBB", "CFC", "BBB"], {"B": "minecraft:bricks", "C": "#c:ingots/copper", "F": "minecraft:furnace"})
    shaped("extruder", ["III", "IPI", "IFI"], {"I": "#c:ingots/iron", "P": "minecraft:piston", "F": "minecraft:furnace"})
    shaped("steel_chassis", ["IRI", "RRR", "IRI"], {"I": "#c:ingots/steel", "R": "#c:rods/steel"})
    built(shaped, rec)
    machines(shaped, rec)
    powered(shaped)
    upgrades(shaped)
    gen_pipes.main(A, D, save, jdump, shaped, loot_self, model, el, all_faces, gui, rect, pipe_tex)

# ---- assembled machines, chassis, parts and cable ----

NEW = {"battery": "battery", "crusher": "crusher", "harvester": "crop_harvester", "alloy": "alloy_smelter",
       "stone": "stone_generator", "saw": "sawmill", "water": "water_generator", "hive": "mechanical_hive",
       "shearer": "shearer", "charger": "charger", "crafter": "auto_crafter",
       "compressor": "compressor", "lava": "lava_generator", "quarry": "quarry"}
BUILT = ["electric_furnace", "rolling_mill", "solid_fuel_generator"] + list(NEW.values())
CHASSIS = ["iron_chassis", "steel_chassis"]
CORES = {"furnace": "electric_furnace", "rolling": "rolling_mill", "generator": "solid_fuel_generator", **NEW}
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped"]
COLOURS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
           "blue", "brown", "green", "red", "black"]
CONCRETES = [c + "_concrete" for c in COLOURS]
METALS = ["iron", "copper", "steel"]
FINISHES = WOODS + CONCRETES + METALS
FACINGS = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
COPPER4 = [(236, 146, 104), (196, 108, 76), (160, 84, 58), (110, 54, 38)]
RED = [(255, 76, 52), (196, 36, 30), (110, 20, 18)]
BOARD = (44, 56, 72)
PALETTES = {"iron": IRON, "copper": COPPER4, "steel": STEEL}
# front opening per core as (x0, y0, x1, y1) in model space; the generator and furnace show fire, the mill its rollers
FRONT_HOLE = {"furnace": (4, 4, 12, 11), "rolling": (3, 6, 13, 10), "generator": (5, 3, 11, 10),
              "battery": (5, 3, 11, 12), "crusher": (3, 5, 13, 11), "harvester": (3, 4, 13, 10), "alloy": (4, 4, 12, 11),
              "stone": (4, 4, 12, 11), "saw": (3, 6, 13, 10), "water": (5, 4, 11, 11), "hive": (4, 4, 12, 11),
              "shearer": (3, 5, 13, 10), "charger": (5, 3, 11, 10), "crafter": (3, 3, 13, 13),
              "compressor": (3, 3, 13, 12), "lava": (4, 3, 12, 12), "quarry": (3, 3, 13, 13)}
TOP_HOLE = {"furnace": (5, 5, 11, 11), "rolling": (4, 6, 12, 10), "generator": None, "crusher": (4, 4, 12, 12),
            "alloy": (5, 5, 11, 11), "water": (5, 5, 11, 11), "saw": (7, 3, 9, 13), "crafter": (3, 3, 13, 13),
            "lava": (5, 5, 11, 11), "quarry": (6, 6, 10, 10)}
WATER = [(96, 150, 230), (62, 110, 210), (40, 76, 170)]
HONEY = [(255, 214, 96), (236, 168, 40), (176, 110, 22)]
LAVA = [(255, 200, 70), (240, 120, 30), (180, 60, 20)]
GREEN = [(110, 190, 60), (70, 140, 40)]
PLANK = [(196, 158, 100), (160, 124, 76), (112, 84, 50)]

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

def core_front(core, lit, level=None):
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
    elif core in NEW:
        new_front(t, core, lit, u0, v0, u1, v1, level)
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

def new_front(t, core, lit, u0, v0, u1, v1, level=None):
    c = lambda col: col + (255,)
    if core == "battery":
        # charge gauge, five bars lit from the bottom
        n = (5 if lit else 0) if level is None else level
        for i in range(5):
            v = v1 - i * 2
            rect(t, u0 + 1, v, u1 - 1, v, c(RED[0] if i < n else RED[2]))
            if i < n:
                px(t, u1 - 1, v, c(COPPER[0]))
    elif core == "crusher":
        for v in (v0 + 1, v1 - 1):
            for u in range(u0 + 1, u1):
                px(t, u, v, c(STEEL[1] if (u + (v == v0 + 1)) % 2 else STEEL[3]))
        if lit:
            for u in range(u0 + 2, u1 - 1, 3):
                px(t, u, v0 + 3, c((150, 140, 130)))
    elif core == "harvester":
        rect(t, u0 + 1, v1 - 1, u1 - 1, v1 - 1, c(STEEL[1]))
        for u in range(u0 + 1, u1, 2):
            px(t, u, v1 - 2, c(STEEL[0]))
        for u in range(u0 + 2, u1 - 1, 3):
            rect(t, u, v0 + 1, u, v1 - 3, c(GREEN[1]))
            px(t, u, v0 + 1, c(HONEY[0] if lit else GREEN[0]))
    elif core == "alloy":
        mid = (u0 + u1) // 2
        for a, b in ((u0 + 1, mid - 1), (mid + 1, u1 - 1)):
            rect(t, a, v0 + 1, b, v0 + 3, c(COPPER[1]))
            rect(t, a, v0 + 1, b, v0 + 1, c(COPPER[0]))
        rect(t, u0 + 1, v1 - 1, u1 - 1, v1 - 1, c(FIRE[1] if lit else RED[2]))
        if lit:
            rect(t, u0 + 2, v1 - 2, u1 - 2, v1 - 2, c(FIRE[0]))
    elif core == "stone":
        mid = (u0 + u1) // 2
        rect(t, u0, v1 - 2, mid - 1, v1, c(WATER[1]))
        rect(t, mid + 1, v1 - 2, u1, v1, c(LAVA[1] if lit else LAVA[2]))
        rect(t, mid - 1, v0 + 1, mid + 1, v0 + 3, c((128, 128, 128)))
        px(t, mid, v0 + 2, c((96, 96, 96)))
    elif core == "saw":
        rect(t, u0 + 1, v0 + 1, u1 - 1, v1 - 1, c(STEEL[2]))
        for u in range(u0 + 1, u1, 2):
            px(t, u, v0, c(STEEL[0]))
        px(t, (u0 + u1) // 2, (v0 + v1) // 2, c(STEEL[3]))
        if lit:
            px(t, u0 + 2, v0 + 1, c(STEEL[0])); px(t, u1 - 2, v1 - 1, c(STEEL[0]))
    elif core == "water":
        rect(t, u0, v0 + 2, u1, v1, c(WATER[2]))
        for v in range(v0 + 3, v1 + 1, 2):
            rect(t, u0 + (v % 3), v, u1 - 1, v, c(WATER[1]))
        rect(t, u0 + 1, v0 + 2, u1 - 1, v0 + 2, c(WATER[0] if lit else WATER[1]))
    elif core == "hive":
        for v in range(v0, v1 + 1):
            for u in range(u0, u1 + 1):
                k = (u + (v // 2) % 2 * 2) % 4
                if k != 0 and v % 2 == 0 or k in (1, 2):
                    px(t, u, v, c(HONEY[1] if (u + v) % 3 else HONEY[2]))
        if lit:
            px(t, (u0 + u1) // 2, (v0 + v1) // 2, c(HONEY[0]))
    elif core == "shearer":
        for i in range(min(u1 - u0, v1 - v0) + 1):
            px(t, u0 + 1 + i, v0 + i, c(STEEL[0]))
            px(t, u1 - 1 - i, v0 + i, c(STEEL[1]))
        rect(t, u0, v1, u1, v1, c((236, 236, 236) if lit else (190, 190, 190)))
    elif core == "crafter":
        for i in range(3):
            for j in range(3):
                a, b = u0 + 1 + i * 3, v0 + 1 + j * 3
                lit_cell = lit and (i + j) % 2 == 0
                rect(t, a, b, a + 1, b + 1, c(PLANK[1] if not lit_cell else HONEY[0]))
                px(t, a, b, c(PLANK[0] if not lit_cell else (255, 240, 170)))
    elif core == "compressor":
        rect(t, u0 + 1, v0, u1 - 1, v0 + 1, c(STEEL[1])); rect(t, u0 + 1, v0 + 1, u1 - 1, v0 + 1, c(STEEL[2]))
        rect(t, u0 + 2, v0 + 2, u1 - 2, v0 + 2, c(STEEL[0]))
        rect(t, (u0 + u1) // 2 - 2, v1 - 3, (u0 + u1) // 2 + 1, v1 - 1, c((60, 60, 64)))
        px(t, (u0 + u1) // 2 - 1, v1 - 3, c((120, 120, 128)))
        rect(t, u0, v1, u1, v1, c(STEEL[3])); rect(t, u0 + 1, v1, u1 - 1, v1, c(IRON[2]))
        if lit:
            rect(t, u0 + 1, v0 + 3, u1 - 1, v0 + 3, c(RED[0]))
    elif core == "lava":
        rect(t, u0, v0, u1, v1, c((150, 170, 180)))
        rect(t, u0 + 1, v0 + 1, u1 - 1, v1 - 1, c(DARK))
        top = v0 + 3
        for v in range(top, v1):
            for u in range(u0 + 1, u1):
                k = 0 if (u + v * 3) % 7 == 0 else (1 if v < top + 2 else 2)
                px(t, u, v, c(LAVA[k] if lit or k else LAVA[1]))
        px(t, u0 + 1, v0 + 1, c((230, 240, 245))); px(t, u0 + 2, v0 + 2, c((200, 214, 222)))
    elif core == "quarry":
        # hazard stripes over a drill bit pointing down
        for u in range(u0, u1 + 1):
            px(t, u, v0, c(HONEY[1] if (u // 2) % 2 else DARK))
        mid = (u0 + u1) // 2
        for i in range(5):
            rect(t, mid - 2 + i // 2, v0 + 2 + i, mid + 1 - i // 2, v0 + 2 + i, c(STEEL[1] if i % 2 else STEEL[0]))
        rect(t, mid - 3, v0 + 1, mid + 2, v0 + 1, c(STEEL[2]))
        px(t, mid - 1, v1 - 1, c(STEEL[0])); px(t, mid, v1 - 1, c(STEEL[0]))
        if lit:
            for u in (u0 + 1, u1 - 1):
                px(t, u, v1 - 1, c(HONEY[0])); px(t, u, v1 - 2, c(FIRE[1]))
    elif core == "charger":
        bolt = [(3, 0), (2, 1), (2, 2), (1, 3), (2, 3), (3, 3), (3, 4), (2, 5), (2, 6)]
        for (a, b) in bolt:
            px(t, u0 + a, v0 + b, c(HONEY[0] if lit else RED[2]))

def core_side():
    t = core_base(53)
    rect(t, 6, 6, 9, 9, COPPER[2] + (255,))
    rect(t, 6, 6, 9, 6, COPPER[0] + (255,)); rect(t, 6, 6, 6, 9, COPPER[0] + (255,))
    rect(t, 7, 7, 8, 8, RED[1] + (255,))
    return t

def core_top(core):
    t = core_base(57)
    hole = TOP_HOLE.get(core)
    if hole:
        x0, z0, x1, z1 = hole
        rect(t, x0, z0, x1 - 1, z1 - 1, DARK + (255,))
        if core == "crusher":
            for z in range(z0 + 1, z1 - 1):
                for x in range(x0 + 1, x1 - 1):
                    if (x + z) % 3 == 0:
                        px(t, x, z, STEEL[2] + (255,))
        elif core == "saw":
            rect(t, x0, z0 + 1, x1 - 1, z1 - 2, STEEL[1] + (255,))
            for z in range(z0 + 1, z1 - 1, 2):
                px(t, x0, z, STEEL[0] + (255,))
        elif core == "water":
            rect(t, x0, z0, x1 - 1, z1 - 1, WATER[2] + (255,))
            rect(t, x0 + 1, z0 + 1, x1 - 3, z0 + 1, WATER[0] + (255,))
        elif core == "crafter":
            for i in range(3):
                for j in range(3):
                    a, b = x0 + 1 + i * 3, z0 + 1 + j * 3
                    rect(t, a, b, a + 1, b + 1, PLANK[1] + (255,))
                    px(t, a, b, PLANK[0] + (255,))
        elif core == "quarry":
            rect(t, x0, z0, x1 - 1, z1 - 1, STEEL[2] + (255,)); rect(t, x0 + 1, z0 + 1, x1 - 2, z1 - 2, DARK + (255,))
        elif core == "lava":
            rect(t, 5, 5, 10, 10, COPPER[1] + (255,)); rect(t, 6, 6, 9, 9, DARK + (255,)); rect(t, 7, 7, 8, 8, LAVA[1] + (255,))
        elif core == "rolling":
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
    side = holed(2, 2, 14, 14, None)
    return wall(side, 1, out, "panel") + wall(holed(2, 2, 14, 14, None), 2, out, "panel") + wall(side, 3, out, "panel")

def worktop_elements(core, loose):
    lift = 1.5 if loose else 0
    hole = TOP_HOLE.get(core)
    rects = holed(0, 0, 16, 16, hole)
    return [el((a0, 14 + lift, b0), (a1, 16 + lift, b1), all_faces("worktop")) for (a0, b0, a1, b1) in rects]

def model(name, elements, textures, parent="minecraft:block/block"):
    jdump({"parent": parent, "ambientocclusion": False, "textures": textures, "elements": elements},
          f"{A}/models/block/{name}.json")
    return f"{MOD}:block/{name}"

def finish_tex(f, part):
    if part == "worktop" and f in CONCRETES:
        return f"minecraft:block/{f}"
    return f"{MOD}:block/{f}_{part}"

def ramp(f):
    if f in METALS:
        src = icons.MC + f"item/{f}_ingot.png" if f != "steel" else A + "/textures/item/steel_ingot.png"
        return icons.ramp_of(src)
    src = icons.MMS + f"block/{f}_worktop.png" if f in WOODS else icons.MC + f"block/{f}.png"
    return icons.ramp_from(icons.mean_of(src))

def kind(f):
    return "wood" if f in WOODS else "metal" if f in METALS else "concrete"

def built(shaped, rec):
    tb, ti = A + "/textures/block/", A + "/textures/item/"
    save(frame(IRON, 13), tb + "iron_chassis.png")
    save(frame([(150, 160, 178), (112, 122, 140), (82, 90, 106), (52, 56, 68)], 13), tb + "steel_chassis.png")
    save(core_base(59), tb + "chassis_floor.png")
    for core in CORES:
        save(core_front(core, False), tb + f"{core}_core_front.png")
        save(core_front(core, True), tb + f"{core}_core_front_on.png")
        save(core_top(core), tb + f"{core}_core_top.png")
    for n in range(6):
        save(core_front("battery", False, n), tb + f"battery_core_front_{n}.png")
    save(core_side(), tb + "core_side.png")
    save(core_base(63), tb + "core_bottom.png")
    save(board_tex(), tb + "control_board.png")
    for f in ["iron", "copper", "steel"]:
        save(sheet_tex(f), tb + f"{f}_panel.png")
        save(worktop_tex(f), tb + f"{f}_worktop.png")
        save(icons.sheet(ramp(f)), ti + f"{f}_sheet.png")
    # wood and concrete finishes share Much More Storage's textures so the two mods match side by side
    for f in WOODS + CONCRETES:
        shutil.copy(icons.MMS + f"block/{f}_body_side.png", tb + f"{f}_panel.png")
    for f in WOODS:
        shutil.copy(icons.MMS + f"block/{f}_worktop.png", tb + f"{f}_worktop.png")
    for f in FINISHES:
        save(icons.panels(ramp(f), kind(f)), ti + f"{f}_panels.png")
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
    for n in range(6):
        model(f"battery_core_{n}", core_elements(False), core_tex("battery", f"_{n}"))
    board_t = {"board": f"{MOD}:block/control_board", "particle": f"{MOD}:block/control_board"}
    model("control_board", board_elements(False), board_t)
    model("control_board_loose", board_elements(True), board_t)
    for f in FINISHES:
        pt = {"panel": finish_tex(f, "panel"), "particle": finish_tex(f, "panel")}
        for loose in ["", "_loose"]:
            model(f"{f}_side_panels{loose}", side_panels(bool(loose)), pt)
            for c in CORES:
                model(f"{c}_{f}_front_panel{loose}", front_panel(c, bool(loose)), pt)

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
            if c == "battery":
                for n in range(6):
                    parts.append(part(f"battery_core_{n}", {"facing": f, "level": str(n)}, y))
            else:
                parts.append(part(f"{c}_core", {"facing": f, "lit": "false"}, y))
                parts.append(part(f"{c}_core_on", {"facing": f, "lit": "true"}, y))
            for fin in FINISHES:
                parts.append(part(f"{c}_{fin}_front_panel", {"facing": f, "panel": fin}, y))
                parts.append(part(f"{fin}_side_panels", {"facing": f, "panel": fin}, y))
        jdump({"multipart": parts}, f"{A}/blockstates/{block}.json")
        elements = frame_elements() + core_elements(False) + front_panel(c, False) + side_panels(False) + worktop_elements(c, False)
        item = model(f"{block}_item", elements, {"frame": f"{MOD}:block/iron_chassis", "floor": f"{MOD}:block/chassis_floor",
                     **core_tex(c, ""), "panel": finish_tex("oak", "panel"), "worktop": finish_tex("oak", "worktop")})
        jdump({"model": {"type": "minecraft:model", "model": item}}, f"{A}/items/{block}.json")
        jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
               "conditions": [{"condition": "minecraft:survives_explosion"}],
               "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}", "functions": [
                   {"function": "minecraft:copy_state", "block": f"{MOD}:{block}", "properties": ["tier", "panel"]},
                   {"function": "minecraft:copy_components", "source": "block_entity", "include": [f"{MOD}:energy", f"{MOD}:worktop", f"{MOD}:upgrades"]
                    + ([f"{MOD}:crafter_pattern"] if block == "auto_crafter" else [])
                    + ([f"{MOD}:quarry_drill"] if block == "quarry" else [])}]}]}],
               "random_sequence": f"{MOD}:blocks/{block}"}, f"{D}/{MOD}/loot_table/blocks/{block}.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/{c}_core"}}, f"{A}/items/{c}_assembly.json")

    # cables
    for cname in ["redstone_copper_cable", "gold_cable", "diamond_cable"]:
        ct = {"cable": f"{MOD}:block/{cname}", "particle": f"{MOD}:block/{cname}"}
        core = el((6, 6, 6), (10, 10, 10), all_faces("cable"))
        arms = {"north": el((6, 6, 0), (10, 10, 6), all_faces("cable", ("south",))),
                "south": el((6, 6, 10), (10, 10, 16), all_faces("cable", ("north",))),
                "west": el((0, 6, 6), (6, 10, 10), all_faces("cable", ("east",))),
                "east": el((10, 6, 6), (16, 10, 10), all_faces("cable", ("west",))),
                "down": el((6, 0, 6), (10, 6, 10), all_faces("cable", ("up",))),
                "up": el((6, 10, 6), (10, 16, 10), all_faces("cable", ("down",)))}
        model(cname, [core], ct)
        parts = [{"apply": {"model": f"{MOD}:block/{cname}"}}]
        for d, e in arms.items():
            parts.append({"when": {d: "true"}, "apply": {"model": model(f"{cname}_{d}", [e], ct)}})
        jdump({"multipart": parts}, f"{A}/blockstates/{cname}.json")
        cable_item = model(f"{cname}_item", [arms["west"], core, arms["east"]], ct)
        jdump({"model": {"type": "minecraft:model", "model": cable_item}}, f"{A}/items/{cname}.json")
        loot_self(cname)

    flat = ["iron_sheet", "copper_sheet", "steel_sheet", "control_board"] + \
           [f"{f}_panels" for f in FINISHES]
    # worktop items copy Much More Storage's 2 px slab so both mods' tops look the same in inventories
    for f in FINISHES:
        uv = {"up": [0, 0, 16, 16], "down": [0, 0, 16, 16], "north": [0, 0, 16, 2], "south": [0, 14, 16, 16],
              "east": [0, 0, 16, 2], "west": [0, 0, 16, 2]}
        slab = {"from": [0, 7, 0], "to": [16, 9, 16], "faces": {d: {"uv": u, "texture": "#top"} for d, u in uv.items()}}
        jdump({"parent": "minecraft:block/block", "textures": {"top": finish_tex(f, "worktop"), "particle": finish_tex(f, "worktop")},
               "elements": [slab]}, f"{A}/models/item/{f}_worktop.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{f}_worktop"}}, f"{A}/items/{f}_worktop.json")
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
        "gui.simplemachines.casings": "Casings",
        "gui.simplemachines.sheet_removed": "Sheet mode removed",
        "gui.simplemachines.mill_unlock": "Empty the slots to unlock",
        "message.simplemachines.needs_assembly": "Fit a mechanical assembly first",
        "message.simplemachines.needs_board": "Needs a control board",
        "message.simplemachines.needs_panels": "Needs side panels (or fit an upgrade first)",
        "message.simplemachines.needs_worktop": "Needs a worktop",
        "message.simplemachines.loose": "Screw in the loose part first",
    }
    for m in ["iron", "copper", "steel"]:
        lang[f"item.simplemachines.{m}_rod"] = f"{m.title()} Rod"
        lang[f"item.simplemachines.{m}_sheet"] = f"{m.title()} Plate"
        lang[f"tag.item.c.rods.{m}"] = f"{m.title()} Rods"
        lang[f"tag.item.c.plates.{m}"] = f"{m.title()} Plates"
    lang.update({"tag.item.c.ingots.steel": "Steel Ingots", "tag.item.c.tools.screwdriver": "Screwdrivers", "tag.item.c.worktops": "Worktops"})
    for f in FINISHES:
        name = f.replace("_", " ").title()
        lang[f"item.simplemachines.{f}_panels"] = f"{name} Panels"
        lang[f"item.simplemachines.{f}_worktop"] = f"{name} Worktop"
    jdump(lang, f"{A}/lang/en_us.json")

    for m in ["iron", "copper", "steel"]:
        if os.path.exists(rec + f"rolling/{m}_sheet.json"):
            os.remove(rec + f"rolling/{m}_sheet.json")
        jdump({"values": [f"{MOD}:{m}_sheet"]}, f"{D}/c/tags/item/plates/{m}.json")
        shaped(f"{m}_panels", ["SS", "SS"], {"S": f"#c:plates/{m}"})
        shaped(f"{m}_worktop", ["SSS"], {"S": f"#c:plates/{m}"})
    # worktops match Much More Storage's recipes, so with it installed its worktops are used instead
    for f in WOODS:
        shaped(f"{f}_panels", ["SS", "SS"], {"S": f"minecraft:{f}_slab"})
        shaped(f"{f}_worktop", ["SSS"], {"S": f"minecraft:{f}_slab"}, cond=True)
    for f in CONCRETES:
        shaped(f"{f}_panels", ["CC", "CC"], {"C": f"minecraft:{f}"}, 2)
        shaped(f"{f}_worktop", ["CCC"], {"C": f"minecraft:{f}"}, 2, cond=True)
    # with Much More Storage installed a lone worktop turns into its matching one, so old tops stack with new
    for f in WOODS + CONCRETES:
        jdump({"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "muchmorestorage"}],
               "fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": ["muchmorestorage"]}],
               "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [f"{MOD}:{f}_worktop"],
               "result": {"id": f"muchmorestorage:{f}_worktop"}}, rec + f"{f}_worktop_to_mms.json")
    jdump({"values": [f"{MOD}:{f}_worktop" for f in FINISHES]}, f"{D}/c/tags/item/worktops.json")
    jdump({"values": [{"id": f"{m}:screwdriver", "required": False} for m in [MOD, "muchmorestorage", "morelighting"]]},
          f"{D}/c/tags/item/tools/screwdriver.json")
    shaped("iron_chassis", ["IBI", "B B", "IBI"], {"I": "#c:ingots/iron", "B": "minecraft:iron_bars"})
    shaped("screwdriver", ["C", "S"], {"C": "#c:ingots/copper", "S": "minecraft:stick"})
    shaped("control_board", ["RDR", "CCC"], {"R": f"{MOD}:redstone_copper_ingot", "D": "#c:dusts/redstone", "C": "#c:ingots/copper"})
    shaped("furnace_assembly", ["RCR", "RFR"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "F": "minecraft:furnace"})
    shaped("rolling_assembly", ["RCR", "PIP"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot",
                                               "P": "minecraft:piston", "I": "#c:ingots/iron"})
    shaped("generator_assembly", ["RCR", "IFI"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot",
                                                 "I": "#c:ingots/iron", "F": "minecraft:furnace"})
    shaped("redstone_copper_cable", ["RRR"], {"R": f"{MOD}:redstone_copper_ingot"}, 8)

# ---- alpha.5: pipes, dusts, screens, new machine data ----

DUST = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......HH.......",
    ".....HHMMH......",
    "....HMMMLMH.....",
    "...HMMLMMLMM....",
    "..HMLMMMLMMLM...",
    "..MMLMLMMMLMLM..",
    ".oLMMLMLMLMMLLo.",
    ".ooooooooooooo..",
    "................",
    "................",
]

def port_tex():
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(t, 6, 6, 9, 9, STEEL[2] + (255,))
    rect(t, 6, 6, 9, 6, STEEL[0] + (255,)); rect(t, 6, 6, 6, 9, STEEL[1] + (255,))
    rect(t, 7, 7, 8, 8, (20, 18, 18, 255))
    return t

def pipe_tex(pal, seed, ring=None):
    t = Image.new("RGBA", (16, 16))
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            t.putpixel((x, y), jitter(pal[1], rnd, 5))
    for i in range(16):
        px(t, i, 0, pal[0] + (255,)); px(t, i, 15, pal[-1] + (255,))
        px(t, 0, i, pal[0] + (255,)); px(t, 15, i, pal[-1] + (255,))
    if ring:
        for i in range(16):
            px(t, i, 7, ring + (255,)); px(t, 7, i, ring + (255,))
    return t

def powered_gui(layout):
    t = gui("gen")
    rect(t, 70, 30, 100, 70, (198, 198, 198, 255))
    grey = (139, 139, 139, 255)
    def slot(x, y, s=18):
        rect(t, x, y, x + s - 1, y + s - 1, (139, 139, 139, 255))
        rect(t, x, y, x + s - 2, y, (55, 55, 55, 255)); rect(t, x, y, x, y + s - 2, (55, 55, 55, 255))
        rect(t, x + 1, y + s - 1, x + s - 1, y + s - 1, (255, 255, 255, 255)); rect(t, x + s - 1, y + 1, x + s - 1, y + s - 1, (255, 255, 255, 255))
    slots = {"process": [(56, 35), (116, 35)], "lava_generator": [(44, 35), (128, 35)], "alloy_smelter": [(38, 35), (56, 35), (116, 35)], "battery": [],
             "stone_generator": [(116, 35)], "mechanical_hive": [(56, 35), (112, 35), (130, 35)],
             "grid": [(62 + i % 3 * 18, 17 + i // 3 * 18) for i in range(9)]}[layout]
    big = {"process": 1, "alloy_smelter": 2, "stone_generator": 0}.get(layout, -1)
    for i, (x, y) in enumerate(slots):
        if i == big:
            slot(x - 5, y - 5, 26)
        else:
            slot(x - 1, y - 1)
    if layout == "lava_generator":
        rect(t, 71, 16, 104, 69, (55, 55, 55, 255))
        rect(t, 72, 17, 104, 69, (255, 255, 255, 255))
        rect(t, 72, 17, 103, 68, (40, 30, 28, 255))
    elif layout not in ("battery", "grid"):
        for i in range(16):
            rect(t, 80, 41, 80 + min(i, 15), 42, grey)
        for k in range(8):
            rect(t, 95 + k, 35 + k, 95 + k, 48 - k, grey)
    return t

def crafter_gui():
    t = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    W, H = 176, 198
    rect(t, 1, 1, W - 2, H - 2, (198, 198, 198, 255))
    rect(t, 1, 0, W - 3, 0, (0, 0, 0, 255)); rect(t, 0, 1, 0, H - 3, (0, 0, 0, 255))
    rect(t, 2, H - 1, W - 2, H - 1, (0, 0, 0, 255)); rect(t, W - 1, 2, W - 1, H - 2, (0, 0, 0, 255))
    px(t, W - 2, 1, (0, 0, 0, 255)); px(t, 1, H - 2, (0, 0, 0, 255)); px(t, W - 2, H - 2, (0, 0, 0, 255))
    rect(t, 1, 1, W - 3, 2, (255, 255, 255, 255)); rect(t, 1, 1, 2, H - 3, (255, 255, 255, 255))
    rect(t, 3, H - 3, W - 2, H - 2, (85, 85, 85, 255)); rect(t, W - 3, 3, W - 2, H - 2, (85, 85, 85, 255))
    px(t, W - 2, H - 2, (0, 0, 0, 0)); px(t, W - 3, H - 3, (85, 85, 85, 255))

    def slot(x, y, s=18, fill=(139, 139, 139, 255)):
        rect(t, x, y, x + s - 1, y + s - 1, fill)
        rect(t, x, y, x + s - 2, y, (55, 55, 55, 255)); rect(t, x, y, x, y + s - 2, (55, 55, 55, 255))
        rect(t, x + 1, y + s - 1, x + s - 1, y + s - 1, (255, 255, 255, 255)); rect(t, x + s - 1, y + 1, x + s - 1, y + s - 1, (255, 255, 255, 255))
    for i in range(9):
        slot(7 + i % 3 * 18, 16 + i // 3 * 18, fill=(118, 118, 118, 255))
        slot(7 + i * 18, 83)
        slot(7 + i * 18, 173)
    for r in range(3):
        for c in range(9):
            slot(7 + c * 18, 115 + r * 18)
    slot(68, 34, fill=(118, 118, 118, 255))
    for i in range(4):
        slot(115 + i % 2 * 18, 25 + i // 2 * 18)
    grey = (139, 139, 139, 255)
    rect(t, 90, 42, 105, 43, grey)
    for k in range(8):
        rect(t, 105 + k, 36 + k, 105 + k, 49 - k, grey)
    rect(t, 152, 16, 168, 70, (255, 255, 255, 255))
    rect(t, 152, 16, 167, 69, (55, 55, 55, 255))
    rect(t, 153, 17, 166, 68, (40, 30, 28, 255))
    return t

def machines(shaped, rec):
    tb, ti = A + "/textures/block/", A + "/textures/item/"
    save(port_tex(), tb + "port.png")
    save(pipe_tex(STEEL, 81), tb + "item_pipe.png")
    save(pipe_tex(STEEL[1:], 83, STEEL[3]), tb + "item_pipe_link.png")
    save(pipe_tex(COPPER, 85, COPPER[2]), tb + "item_pipe_extract.png")
    for layout in ["process", "alloy_smelter", "battery", "stone_generator", "mechanical_hive", "grid", "lava_generator"]:
        save(powered_gui(layout), A + f"/textures/gui/{layout}.png")
    save(crafter_gui(), A + "/textures/gui/auto_crafter.png")
    dusts = {"iron": icons.ramp_of(icons.MC + "item/iron_ingot.png"), "copper": icons.ramp_of(icons.MC + "item/copper_ingot.png"),
             "gold": icons.ramp_of(icons.MC + "item/gold_ingot.png")}
    for m, r in dusts.items():
        save(sprite(DUST, [r[1], r[3], r[5], r[7]]), ti + f"{m}_dust.png")
        jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{m}_dust"}}, f"{A}/models/item/{m}_dust.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{m}_dust"}}, f"{A}/items/{m}_dust.json")
        jdump({"values": [f"{MOD}:{m}_dust"]}, f"{D}/c/tags/item/dusts/{m}.json")
        for kind_, time in [("smelting", 200), ("blasting", 100)]:
            jdump({"type": f"minecraft:{kind_}", "category": "misc", "cookingtime": time, "experience": 0.35,
                   "ingredient": f"#c:dusts/{m}", "result": {"id": f"minecraft:{m}_ingot"}},
                  rec + f"{m}_ingot_from_{kind_}_dust.json")

    # pipe
    pt = {"pipe": f"{MOD}:block/item_pipe", "link": f"{MOD}:block/item_pipe_link", "extract": f"{MOD}:block/item_pipe_extract",
          "particle": f"{MOD}:block/item_pipe"}
    a0, a1, c0, c1 = 6.5, 9.5, 5.5, 10.5
    core = el((c0, c0, c0), (c1, c1, c1), all_faces("pipe"))
    arm = el((a0, a0, 0), (a1, a1, c0), all_faces("pipe", ("south",)))
    collar = el((6, 6, 0), (10, 10, 1.5), all_faces("link"))
    grab = el((5.5, 5.5, 0), (10.5, 10.5, 2), all_faces("extract"))
    model("item_pipe", [core], pt)
    model("item_pipe_arm", [arm], pt)
    model("item_pipe_collar", [collar], pt)
    model("item_pipe_grab", [grab], pt)
    rots = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    parts = [{"apply": {"model": f"{MOD}:block/item_pipe"}}]
    for d, r in rots.items():
        parts.append({"when": {d: "pipe|link|extract"}, "apply": {"model": f"{MOD}:block/item_pipe_arm", **r}})
        parts.append({"when": {d: "link"}, "apply": {"model": f"{MOD}:block/item_pipe_collar", **r}})
        parts.append({"when": {d: "extract"}, "apply": {"model": f"{MOD}:block/item_pipe_grab", **r}})
    jdump({"multipart": parts}, f"{A}/blockstates/item_pipe.json")
    pipe_item = model("item_pipe_item", [el((0, a0, a0), (16, a1, a1), all_faces("pipe")), core], pt)
    jdump({"model": {"type": "minecraft:model", "model": pipe_item}}, f"{A}/items/item_pipe.json")
    loot_self("item_pipe")
    shaped("item_pipe", ["SGS"], {"S": "#c:plates/iron", "G": "#c:glass_blocks"}, 8)

    R, C, I = "#c:rods/iron", f"{MOD}:redstone_copper_ingot", "#c:ingots/iron"
    shaped("battery_assembly", ["RCR", "DID"], {"R": R, "C": C, "I": I, "D": "minecraft:redstone_block"})
    shaped("crusher_assembly", ["RCR", "IGI"], {"R": R, "C": C, "I": I, "G": "minecraft:grindstone"})
    shaped("harvester_assembly", ["RCR", "HIH"], {"R": R, "C": C, "I": I, "H": "minecraft:iron_hoe"})
    shaped("alloy_assembly", ["RCR", "IAI"], {"R": R, "C": C, "I": I, "A": f"{MOD}:alloy_furnace"})
    shaped("stone_assembly", ["RCR", "WIL"], {"R": R, "C": C, "I": I, "W": "minecraft:water_bucket", "L": "minecraft:lava_bucket"})
    shaped("saw_assembly", ["RCR", "ISI"], {"R": R, "C": C, "I": I, "S": "minecraft:stonecutter"})
    shaped("water_assembly", ["RCR", "WIW"], {"R": R, "C": C, "I": I, "W": "minecraft:water_bucket"})
    shaped("hive_assembly", ["RCR", "IHI"], {"R": R, "C": C, "I": I, "H": "minecraft:beehive"})
    shaped("shearer_assembly", ["RCR", "SIS"], {"R": R, "C": C, "I": I, "S": "minecraft:shears"})
    shaped("charger_assembly", ["RCR", "ILI"], {"R": R, "C": C, "I": I, "L": "minecraft:lightning_rod"})
    shaped("crafter_assembly", ["RCR", "IKI"], {"R": R, "C": C, "I": I, "K": "minecraft:crafter"})
    shaped("compressor_assembly", ["RCR", "PIP"], {"R": R, "C": C, "I": I, "P": "minecraft:piston"})
    shaped("lava_assembly", ["RCR", "MTM"], {"R": R, "C": C, "M": "minecraft:magma_block", "T": f"{MOD}:fluid_tank"})

    def proc(kind_, name, ingredient, result, count=1, bonus=0.0, time=200, tags=(), inputs=1):
        r = {}
        if tags:
            r["neoforge:conditions"] = [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": t}} for t in tags]
            r["fabric:load_conditions"] = [{"condition": "fabric:tags_populated", "values": list(tags)}]
        r.update({"type": f"{MOD}:{kind_}", "ingredient": ingredient, "result": result})
        if count > 1:
            r["count"] = count
        if bonus:
            r["bonus"] = bonus
        if inputs > 1:
            r["input_count"] = inputs
        r["cooking_time"] = time
        jdump(r, rec + f"{kind_}/{name}.json")
    for m in ["iron", "copper", "gold", "tin", "lead", "silver", "nickel", "zinc", "aluminum", "osmium", "uranium", "platinum"]:
        own = m in dusts
        for src, n, b in [("ores", 2, 0.1), ("raw_materials", 2, 0.1), ("ingots", 1, 0.0)]:
            tags = () if own else (f"c:{src}/{m}", f"c:dusts/{m}")
            proc("crushing", f"{m}_dust_from_{src}", f"#c:{src}/{m}", f"#c:dusts/{m}", n, b, 200 if n > 1 else 100, tags)
    proc("crushing", "gravel", "minecraft:cobblestone", "minecraft:gravel", time=100)
    proc("crushing", "sand", "minecraft:gravel", "minecraft:sand", time=100)
    for w in ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"]:
        proc("sawing", f"{w}_planks", f"#minecraft:{w}_logs", f"minecraft:{w}_planks", 6, time=100)
    for w in ["crimson", "warped"]:
        proc("sawing", f"{w}_planks", f"#minecraft:{w}_stems", f"minecraft:{w}_planks", 6, time=100)
    proc("sawing", "bamboo_planks", "#minecraft:bamboo_blocks", "minecraft:bamboo_planks", 3, time=100)
    proc("sawing", "sticks", "#minecraft:planks", "minecraft:stick", 3, time=50)
    alpha6(shaped, rec, proc)
    milestone2(shaped)
    milestone3(shaped, rec)
    milestone4(shaped, rec)

    lang = json.load(open(f"{A}/lang/en_us.json"))
    names = {"battery": "Battery", "crusher": "Crusher", "crop_harvester": "Crop Harvester", "alloy_smelter": "Alloy Smelter",
             "stone_generator": "Stone Generator", "sawmill": "Sawmill", "water_generator": "Water Generator",
             "mechanical_hive": "Mechanical Hive", "shearer": "Shearer", "charger": "Charger", "auto_crafter": "Auto Crafter",
             "compressor": "Compressor", "lava_generator": "Lava Generator", "quarry": "Quarry"}
    for core, block in NEW.items():
        lang[f"block.simplemachines.{block}"] = names[block]
        lang[f"container.simplemachines.{block}"] = names[block]
        lang[f"item.simplemachines.{core}_assembly"] = names[block].replace("Crop ", "").replace("Mechanical ", "") + " Assembly"
    lang["item.simplemachines.alloy_assembly"] = "Alloy Assembly"
    lang["item.simplemachines.stone_assembly"] = "Stone Assembly"
    lang["item.simplemachines.water_assembly"] = "Water Assembly"
    lang["item.simplemachines.saw_assembly"] = "Saw Assembly"
    lang["item.simplemachines.crafter_assembly"] = "Crafter Assembly"
    lang["item.simplemachines.lava_assembly"] = "Lava Assembly"
    lang.update({
        "gui.simplemachines.ingredients": "Ingredients",
        "gui.simplemachines.crafter_clear": "Clear pattern",
        "gui.simplemachines.redstone": "Redstone mode",
        "gui.simplemachines.redstone_always": "Always on",
        "gui.simplemachines.redstone_with_signal": "Run with signal",
        "gui.simplemachines.redstone_without_signal": "Stop with signal",
        "gui.simplemachines.redstone_click": "Click to change",
        "gui.simplemachines.crafter_idle": "Set a pattern",
        "gui.simplemachines.crafter_no_recipe": "No recipe matches this pattern",
        "gui.simplemachines.crafter_working": "Crafting",
        "gui.simplemachines.crafter_missing": "Waiting for ingredients",
        "gui.simplemachines.crafter_full": "Outputs full",
        "gui.simplemachines.crafter_no_power": "No power",
        "gui.simplemachines.crafter_off": "Stopped by redstone",
    })
    lang["block.simplemachines.item_pipe"] = "Item Pipe"
    for m in dusts:
        lang[f"item.simplemachines.{m}_dust"] = f"{m.title()} Dust"
    for d in ["north", "south", "east", "west", "up", "down"]:
        lang[f"direction.simplemachines.{d}"] = d.title()
    lang["message.simplemachines.output_on"] = "Auto-output on: %s"
    lang["message.simplemachines.output_off"] = "Auto-output off: %s"
    lang.pop("message.simplemachines.pipe_deliver", None)
    lang["message.simplemachines.pipe_link"] = "Pipe end: input"
    lang["message.simplemachines.pipe_extract"] = "Pipe end: output"
    lang["message.simplemachines.pipe_off"] = "Pipe end: disconnected"
    lang["message.simplemachines.pipe_no_inventory"] = "No inventory on that side"
    lang["message.simplemachines.cable_on"] = "Cable side connected: %s"
    lang["message.simplemachines.cable_off"] = "Cable side disconnected: %s"
    jdump(lang, f"{A}/lang/en_us.json")
    tag = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    vals = json.load(open(tag))["values"]
    jdump({"values": vals + [f"{MOD}:item_pipe"]}, tag)

# ---- 2.1 milestone 1: compressor, lava generator, casings, plates, wafer, biofuel ----

COMPOSTER = "/tmp/mcsrc/net/minecraft/world/level/block/ComposterBlock.java"

def casing_tex(r):
    t = icons.blank()
    for y in range(3, 13):
        rect(t, 4, y, 11, y, r[3] + (255,))
        px(t, 4, y, r[5] + (255,)); px(t, 11, y, r[5] + (255,)); px(t, 5, y, r[1] + (255,))
    rect(t, 4, 2, 11, 2, r[1] + (255,)); rect(t, 5, 3, 10, 3, (40, 36, 34, 255))
    rect(t, 4, 13, 11, 13, r[6] + (255,))
    for y in (6, 10):
        rect(t, 4, y, 11, y, r[5] + (255,))
    return t

def wafer_tex():
    t = icons.blank()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 6.6:
                k = (x + y) % 5 == 0
                t.putpixel((x, y), ((96, 110, 150) if k else (52, 60, 92)) + (255,))
            elif d < 7.4:
                t.putpixel((x, y), (150, 160, 180, 255))
    rect(t, 6, 1, 9, 1, (0, 0, 0, 0))
    px(t, 5, 4, (200, 210, 240, 255)); px(t, 4, 5, (170, 180, 220, 255))
    return t

def biofuel_tex():
    t = Image.new("RGBA", (16, 16))
    rnd = random.Random(77)
    pal = [(118, 140, 52), (92, 112, 40), (70, 84, 32), (110, 84, 44)]
    for y in range(16):
        for x in range(16):
            t.putpixel((x, y), jitter(pal[rnd.choice((0, 1, 1, 2, 3))], rnd, 6))
    for i in range(16):
        px(t, i, 0, (60, 70, 28, 255)); px(t, i, 15, (52, 60, 24, 255))
        px(t, 0, i, (60, 70, 28, 255)); px(t, 15, i, (52, 60, 24, 255))
    for y in (5, 10):
        rect(t, 1, y, 14, y, (64, 76, 30, 255))
    return t

def alpha6(shaped, rec, proc):
    tb, ti = A + "/textures/block/", A + "/textures/item/"
    gold, iron, copper = (icons.ramp_of(icons.MC + f"item/{m}_ingot.png") for m in ("gold", "iron", "copper"))
    flat_item("gold_sheet", icons.sheet(gold))
    flat_item("iron_casing", casing_tex(iron))
    flat_item("copper_casing", casing_tex(copper))
    flat_item("silicon_wafer", wafer_tex())
    jdump({"values": [f"{MOD}:gold_sheet"]}, f"{D}/c/tags/item/plates/gold.json")
    save(biofuel_tex(), tb + "biofuel_block.png")
    jdump({"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/biofuel_block"}}, f"{A}/models/block/biofuel_block.json")
    jdump({"variants": {"": {"model": f"{MOD}:block/biofuel_block"}}}, f"{A}/blockstates/biofuel_block.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/biofuel_block"}}, f"{A}/items/biofuel_block.json")
    loot_self("biofuel_block")
    jdump({"values": {f"{MOD}:biofuel_block": {"burn_time": 2400}}}, f"{D}/neoforge/data_maps/item/furnace_fuels.json")
    jdump({"values": [f"{MOD}:biofuel_block"]}, f"{D}/minecraft/tags/block/mineable/hoe.json")
    # every vanilla compostable, read from the composter source when it is around
    if os.path.exists(COMPOSTER):
        import re
        ids = sorted(set(m.lower() for m in re.findall(r"add\([0-9.]+F, Items\.([A-Z0-9_]+)\)", open(COMPOSTER).read())))
        jdump({"values": [{"id": f"minecraft:{i}", "required": False} for i in ids]}, f"{D}/{MOD}/tags/item/compostables.json")
    for m in ["iron", "copper", "steel", "gold"]:
        proc("compressing", f"{m}_plate", f"#c:ingots/{m}", f"{MOD}:{m}_sheet", time=100)
    proc("compressing", "diamond", "minecraft:coal", "minecraft:diamond", time=2400, inputs=64)
    proc("compressing", "biofuel_block", f"#{MOD}:compostables", f"{MOD}:biofuel_block", time=160, inputs=32)
    proc("compressing", "silicon_wafer", "#c:sands", f"{MOD}:silicon_wafer", time=200, inputs=4)
    for m in ["iron", "copper"]:
        jdump({"type": f"{MOD}:rolling", "input": {"ingredient": f"#c:plates/{m}"}, "result": {"id": f"{MOD}:{m}_casing", "count": 2},
               "cooking_time": 100, "mode": "casings"}, rec + f"rolling/{m}_casing.json")
    shaped("item_pipe_from_casing", ["CGC"], {"C": f"{MOD}:iron_casing", "G": "#c:glass_blocks"}, 8)
    shaped("fluid_pipe_from_casing", ["CGC"], {"C": f"{MOD}:copper_casing", "G": "#c:glass_blocks"}, 8)
    for f in ("item_pipe_from_casing", "fluid_pipe_from_casing"):
        r = json.load(open(rec + f + ".json"))
        r["result"]["id"] = f"{MOD}:" + f.replace("_from_casing", "")
        jdump(r, rec + f + ".json")
    lang = json.load(open(f"{A}/lang/en_us.json"))
    lang.update({
        "item.simplemachines.gold_sheet": "Gold Plate", "tag.item.c.plates.gold": "Gold Plates",
        "item.simplemachines.iron_casing": "Iron Casing", "item.simplemachines.copper_casing": "Copper Casing",
        "item.simplemachines.silicon_wafer": "Silicon Wafer", "block.simplemachines.biofuel_block": "Biofuel Block",
        "tag.item.simplemachines.compostables": "Compostables",
        "gui.simplemachines.lava_tank": "%s / %s mB Lava",
    })
    jdump(lang, f"{A}/lang/en_us.json")

# ---- 2.1 milestone 2: MV and HV cables, transformers, overvoltage ----

LV_RING = [(255, 120, 100), (214, 44, 36), (130, 20, 20)]
MV_RING = [(255, 244, 150), (246, 204, 40), (168, 118, 10)]
HV_RING = [(214, 248, 255), (96, 212, 242), (30, 120, 160)]

def tier_cable_tex(seed, jacket, stripe, band):
    t = Image.new("RGBA", (16, 16))
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            t.putpixel((x, y), jitter(jacket[1], rnd, 4))
    for i in range(16):
        px(t, i, 6, jacket[0] + (255,)); px(t, i, 9, jacket[2] + (255,))
        px(t, i, 7, stripe[0] + (255,)); px(t, i, 8, stripe[1] + (255,))
        px(t, 6, i, jacket[0] + (255,)); px(t, 9, i, jacket[2] + (255,))
    for x in (3, 12):
        rect(t, x, 0, x, 15, band[1] + (255,)); px(t, x, 6, band[0] + (255,)); px(t, x, 9, band[2] + (255,))
    return t

def ring_tex(ring, seed):
    t = plate(STEEL, seed)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 6.2:
                if d > 4.6:
                    k = 0 if (x + y) < 13 else (2 if (x + y) > 17 else 1)
                    px(t, x, y, ring[k] + (255,))
                elif d > 3.6:
                    px(t, x, y, DARK + (255,))
                elif d > 1.6:
                    px(t, x, y, (COPPER[1] if int(d * 2) % 2 else COPPER[2]) + (255,))
                else:
                    px(t, x, y, (40, 40, 46, 255))
    return t

def transformer_assets(name, front, low):
    els = []
    for x in (0, 14):
        for z in (0, 14):
            els.append(el((x, 0, z), (x + 2, 16, z + 2), all_faces("frame")))
    for y in (0, 14):
        for z in (0, 14):
            els.append(el((2, y, z), (14, y + 2, z + 2), all_faces("frame", ("east", "west"))))
        for x in (0, 14):
            els.append(el((x, y, 2), (x + 2, y + 2, 14), all_faces("frame", ("north", "south"))))
    els.append(el((1, 1, 1), (15, 15, 15), {d: {"texture": "#front" if d == "north" else "#low", "uv": [1, 1, 15, 15]}
                                            for d in ("north", "south", "east", "west", "up", "down")}))
    m = model(name, els, {"frame": f"{MOD}:block/steel_chassis", "particle": f"{MOD}:block/steel_chassis",
                          "front": f"{MOD}:block/ring_{front}", "low": f"{MOD}:block/ring_{low}"})
    rot = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    jdump({"variants": {f"facing={f}": {"model": m, **r} for f, r in rot.items()}}, f"{A}/blockstates/{name}.json")
    jdump({"model": {"type": "minecraft:model", "model": m}}, f"{A}/items/{name}.json")
    loot_self(name)

def milestone2(shaped):
    tb = A + "/textures/block/"
    gold = [(255, 236, 120), (232, 190, 40), (160, 116, 16)]
    diam = [(224, 252, 255), (150, 228, 240), (74, 182, 212)]
    save(tier_cable_tex(72, [(176, 180, 190), (130, 134, 146), (80, 82, 92)], MV_RING, gold), tb + "gold_cable.png")
    save(tier_cable_tex(73, [(150, 156, 170), (96, 100, 114), (56, 58, 68)], HV_RING, diam), tb + "diamond_cable.png")
    for n, r in (("lv", LV_RING), ("mv", MV_RING), ("hv", HV_RING)):
        save(ring_tex(r, 100 + len(n) + ord(n[0])), tb + f"ring_{n}.png")
    transformer_assets("lv_mv_transformer", "mv", "lv")
    transformer_assets("mv_hv_transformer", "hv", "mv")
    tag = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    vals = json.load(open(tag))["values"]
    jdump({"values": vals + [f"{MOD}:{b}" for b in ("gold_cable", "diamond_cable", "lv_mv_transformer", "mv_hv_transformer")]}, tag)
    S = "#c:plates/steel"
    shaped("gold_cable", ["TTT", "SGS", "TTT"], {"T": f"{MOD}:redstone_copper_cable", "S": S, "G": "#c:ingots/gold"}, 6)
    shaped("diamond_cable", ["TTT", "DED", "TTT"], {"T": f"{MOD}:gold_cable", "D": "#c:gems/diamond", "E": "#c:ender_pearls"}, 6)
    shaped("lv_mv_transformer", ["SRS", "LIM", "SSS"], {"S": S, "R": f"{MOD}:redstone_copper_ingot", "L": f"{MOD}:redstone_copper_cable",
                                                         "I": "#c:storage_blocks/iron", "M": f"{MOD}:gold_cable"})
    shaped("mv_hv_transformer", ["SDS", "MGH", "SSS"], {"S": S, "D": "#c:gems/diamond", "M": f"{MOD}:gold_cable",
                                                         "G": "#c:storage_blocks/gold", "H": f"{MOD}:diamond_cable"})
    lang = json.load(open(f"{A}/lang/en_us.json"))
    lang.update({
        "block.simplemachines.gold_cable": "Gold Cable", "block.simplemachines.diamond_cable": "Diamond Cable",
        "block.simplemachines.lv_mv_transformer": "LV/MV Transformer", "block.simplemachines.mv_hv_transformer": "MV/HV Transformer",
        "message.simplemachines.transformer": "Front %1$s, other faces %2$s. Waiting: %3$s FE up, %4$s FE down (of %5$s)",
        "jei.simplemachines.info.redstone_copper_cable": "LV cable, red. Carries up to 1,000 FE per tick for a whole connected run. Joins only other LV cables.",
        "jei.simplemachines.info.gold_cable": "MV cable, yellow. Carries up to 8,000 FE per tick. Joins only other MV cables.",
        "jei.simplemachines.info.diamond_cable": "HV cable, light blue. Carries up to 64,000 FE per tick. Joins only other HV cables.",
        "jei.simplemachines.info.lv_mv_transformer": "The front (yellow ring) is MV and faces you when placed. The other faces (red rings) are LV. Power goes both ways, up to 1,000 FE per tick each way, losing 5%.",
        "jei.simplemachines.info.mv_hv_transformer": "The front (light blue ring) is HV and faces you when placed. The other faces (yellow rings) are MV. Power goes both ways, up to 8,000 FE per tick each way, losing 5%.",
        "jei.simplemachines.info.overvoltage": "Every machine and battery is LV, the Diamond Solar Panel MV. Feeding a higher voltage into a lower rated block makes it explode.",
    })
    jdump(lang, f"{A}/lang/en_us.json")

GOLD4 = [(255, 236, 110), (232, 190, 50), (170, 120, 20), (110, 70, 10)]
COP4 = [(236, 146, 104), (196, 108, 76), (150, 80, 56), (110, 54, 40)]
DIAM4 = [(224, 252, 255), (150, 228, 240), (74, 182, 212), (34, 110, 140)]
SOLAR_PAL = {1: IRON, 2: COP4, 3: GOLD4, 4: DIAM4}
SOLAR_CELLS = {1: [(90, 130, 220), (44, 70, 150), (28, 44, 100)], 2: [(80, 110, 210), (36, 56, 130), (22, 34, 86)],
               3: [(110, 90, 210), (40, 36, 110), (24, 20, 70)], 4: [(150, 250, 255), (24, 74, 104), (12, 40, 64)]}
SOLAR_LED = {1: LV_RING, 2: MV_RING, 3: MV_RING, 4: HV_RING}
SOLAR_NAMES = {1: "iron", 2: "copper", 3: "gold", 4: "diamond"}

def solid_tex(col, seed, amt=4):
    t = Image.new("RGBA", (16, 16)); rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            t.putpixel((x, y), jitter(col, rnd, amt))
    return t

def solar_cell_tex(tier, n):
    pal, cell = SOLAR_PAL[tier], SOLAR_CELLS[tier]
    t = solid_tex(pal[2], 30 + tier, 3); rnd = random.Random(40 + tier)
    for i in range(16):
        px(t, i, 0, pal[0] + (255,)); px(t, 0, i, pal[0] + (255,)); px(t, i, 15, pal[3] + (255,)); px(t, 15, i, pal[3] + (255,))
    s = 14 // n
    for gy in range(n):
        for gx in range(n):
            x0, y0 = 1 + gx * s + (14 - s * n) // 2, 1 + gy * s + (14 - s * n) // 2
            for y in range(y0, y0 + s - 1):
                for x in range(x0, x0 + s - 1):
                    t.putpixel((x, y), jitter(cell[1], rnd, 4))
            px(t, x0, y0, cell[0] + (255,)); px(t, x0 + s - 2, y0 + s - 2, cell[2] + (255,))
            if tier == 4:
                px(t, x0 + 1, y0, cell[0] + (255,))
    return t

def solar_assets(tier):
    pal, name = SOLAR_PAL[tier], f"{SOLAR_NAMES[tier]}_solar_panel"
    tb = A + "/textures/block/"
    save(solar_cell_tex(tier, (2, 3, 4, 4)[tier - 1]), tb + f"{name}_top.png")
    f = plate(STEEL, 80 + tier, rivets=False)
    rect(f, 3, 3, 12, 6, DARK + (255,))
    for i in range(4):
        rect(f, 4 + i * 2, 4, 4 + i * 2, 5, (SOLAR_LED[tier][1] if i < tier else (50, 50, 56)) + (255,))
    rect(f, 3, 9, 12, 12, pal[1] + (255,)); rect(f, 3, 9, 12, 9, pal[0] + (255,)); rect(f, 3, 12, 12, 12, pal[3] + (255,))
    save(f, tb + f"{name}_front.png")
    els = []
    for x in (0, 14):
        for z in (0, 14):
            els.append(el((x, 0, z), (x + 2, 8, z + 2), all_faces("frame")))
    for z in (0, 14):
        els.append(el((2, 0, z), (14, 2, z + 2), all_faces("frame", ("east", "west"))))
        els.append(el((2, 6, z), (14, 8, z + 2), all_faces("frame", ("east", "west"))))
    for x in (0, 14):
        els.append(el((x, 0, 2), (x + 2, 2, 14), all_faces("frame", ("north", "south"))))
        els.append(el((x, 6, 2), (x + 2, 8, 14), all_faces("frame", ("north", "south"))))
    els.append(el((1, 2, 1), (15, 7, 15), {"north": {"texture": "#front", "uv": [1, 5, 15, 10]}, "south": {"texture": "#body", "uv": [1, 9, 15, 14]},
                                          "east": {"texture": "#body", "uv": [1, 9, 15, 14]}, "west": {"texture": "#body", "uv": [1, 9, 15, 14]},
                                          "down": {"texture": "#body"}}))
    els.append(el((2, 7, 2), (14, 7.5, 14), {"up": {"texture": "#top", "uv": [2, 2, 14, 14]}}))
    if tier == 4:
        for (x, z) in ((0, 0), (14, 0), (0, 14), (14, 14)):
            els.append(el((x, 8, z), (x + 2, 9, z + 2), all_faces("gem")))
    fr = f"{MOD}:block/{'steel' if tier >= 3 else 'iron'}_chassis"
    m = model(name, els, {"frame": fr, "front": f"{MOD}:block/{name}_front", "body": f"{MOD}:block/core_side",
                          "top": f"{MOD}:block/{name}_top", "gem": "minecraft:block/diamond_block", "particle": fr})
    rot = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}}
    jdump({"variants": {f"facing={k}": {"model": m, **r} for k, r in rot.items()}}, f"{A}/blockstates/{name}.json")
    jdump({"model": {"type": "minecraft:model", "model": m}}, f"{A}/items/{name}.json")
    loot_self(name)

def helix_assets():
    tb = A + "/textures/block/"
    b = solid_tex((226, 230, 236), 91, 3)
    for x in range(16):
        px(b, x, 0, COPPER[1] + (255,)); px(b, x, 15, COPPER[2] + (255,))
    save(b, tb + "wind_turbine_blade.png")
    save(plate(IRON, 92), tb + "wind_turbine_base.png")
    post = solid_tex(STEEL[1], 93, 3)
    for y in range(16):
        px(post, 0, y, STEEL[0] + (255,)); px(post, 15, y, STEEL[3] + (255,))
    save(post, tb + "wind_turbine_post.png")
    def box(e):
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        size = {"north": (x1 - x0, y1 - y0), "south": (x1 - x0, y1 - y0), "east": (z1 - z0, y1 - y0),
                "west": (z1 - z0, y1 - y0), "up": (x1 - x0, z1 - z0), "down": (x1 - x0, z1 - z0)}
        for d, f in e["faces"].items():
            if "uv" not in f:
                w, h = (min(16, v) for v in size[d])
                f["uv"] = [0, 0, w, h]
        return e
    display = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
               "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
               "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
               "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
               "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}
    tex = {"base": f"{MOD}:block/wind_turbine_base", "bolt": "minecraft:block/copper_block", "post": f"{MOD}:block/wind_turbine_post",
           "blade": f"{MOD}:block/wind_turbine_blade", "particle": f"{MOD}:block/wind_turbine_base"}
    # base: plate, housing and a stub of the shaft that the blades sit on
    els = [el((1, 0, 1), (15, 1, 15), all_faces("base"))]
    for (x, z) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        els.append(el((x, 1, z), (x + 1, 1.5, z + 1), all_faces("bolt")))
    els += [el((5.5, 1, 5.5), (10.5, 2, 10.5), all_faces("bolt")),
            el((4, 2, 4), (12, 9, 12), all_faces("base")),
            el((5.5, 9, 5.5), (10.5, 10, 10.5), all_faces("bolt")),
            el((6.5, 10, 6.5), (9.5, 15, 9.5), all_faces("post")),
            el((7.5, 15, 7.5), (8.5, 16, 8.5), all_faces("post"))]
    jdump({"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex,
           "elements": [box(e) for e in els], "display": display}, f"{A}/models/block/wind_turbine.json")
    # loose blade: half a turn of helix, drawn still; in a stack the base draws it turning
    steps = [(0, 0), (0, 22.5), (0, 45), (1, -22.5), (1, 0), (1, 22.5), (1, 45), (0, -22.5)]
    els = [el((7.5, 0, 7.5), (8.5, 16, 8.5), all_faces("post"))]
    for i in range(16):
        axis, ang = steps[i // 2]
        a, b2 = (0.5, 15.5), (7, 9)
        x0, x1, z0, z1 = (a[0], a[1], b2[0], b2[1]) if axis == 0 else (b2[0], b2[1], a[0], a[1])
        rot = {"origin": [8, i, 8], "axis": "y", "angle": ang} if ang else None
        e = box(el((x0, i, z0), (x1, i + 1, z1), {d: {"texture": "#blade"} for d in ("north", "south", "east", "west", "up", "down")}, rot))
        for d in ("north", "south", "east", "west"):
            e["faces"][d]["uv"] = [0, 15 - i, e["faces"][d]["uv"][2], 16 - i]
        els.append(e)
    jdump({"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex,
           "elements": [box(e) for e in els], "display": display}, f"{A}/models/block/turbine_blade.json")
    jdump({"textures": {"particle": f"{MOD}:block/wind_turbine_base"}}, f"{A}/models/block/wind_turbine_top.json")
    jdump({"textures": {"particle": f"{MOD}:block/wind_turbine_blade"}}, f"{A}/models/block/turbine_blade_spinning.json")
    jdump({"variants": {"half=lower": {"model": f"{MOD}:block/wind_turbine"}, "half=upper": {"model": f"{MOD}:block/wind_turbine_top"}}},
          f"{A}/blockstates/wind_turbine.json")
    jdump({"variants": {f"stack={i}": {"model": f"{MOD}:block/turbine_blade" + ("" if i == 0 else "_spinning")} for i in range(6)}},
          f"{A}/blockstates/turbine_blade.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/wind_turbine"}}, f"{A}/items/wind_turbine.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/turbine_blade"}}, f"{A}/items/turbine_blade.json")
    jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
           "conditions": [{"condition": "minecraft:survives_explosion"},
                          {"condition": "minecraft:block_state_property", "block": f"{MOD}:wind_turbine", "properties": {"half": "lower"}}],
           "entries": [{"type": "minecraft:item", "name": f"{MOD}:wind_turbine"}]}],
           "random_sequence": f"{MOD}:blocks/wind_turbine"}, f"{D}/{MOD}/loot_table/blocks/wind_turbine.json")
    jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:survives_explosion"}],
           "entries": [{"type": "minecraft:item", "name": f"{MOD}:turbine_blade"}]}],
           "random_sequence": f"{MOD}:blocks/turbine_blade"}, f"{D}/{MOD}/loot_table/blocks/turbine_blade.json")

def core_icon(arm, centre):
    t = Image.new("RGBA", (16, 16)); rnd = random.Random(sum(arm[0]) + sum(centre[0]))
    for (x0, y0, x1, y1) in ((5, 0, 10, 4), (5, 11, 10, 15), (0, 5, 4, 10), (11, 5, 15, 10)):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                t.putpixel((x, y), jitter(arm[1], rnd, 4))
        rect(t, x0, y0, x1, y0, arm[0] + (255,)); rect(t, x0, y1, x1, y1, arm[-1] + (255,))
        rect(t, x0, y0, x0, y1, arm[0] + (255,)); rect(t, x1, y0, x1, y1, arm[-1] + (255,))
    for y in range(4, 12):
        for x in range(4, 12):
            t.putpixel((x, y), jitter(centre[1], rnd, 5))
    rect(t, 4, 4, 11, 4, centre[0] + (255,)); rect(t, 4, 4, 4, 11, centre[0] + (255,))
    rect(t, 4, 11, 11, 11, centre[2] + (255,)); rect(t, 11, 4, 11, 11, centre[2] + (255,))
    px(t, 6, 6, (255, 255, 255, 200))
    return t

def pv_icon():
    t = Image.new("RGBA", (16, 16)); rnd = random.Random(150)
    rect(t, 1, 2, 14, 13, (170, 174, 184, 255))
    for gy in range(3):
        for gx in range(3):
            x0, y0 = 2 + gx * 4, 3 + gy * 3 + (gy > 0)
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 4):
                    if x < 14 and y < 13:
                        t.putpixel((x, y), jitter((44, 70, 150), rnd, 5))
            px(t, x0, y0, (110, 150, 235, 255))
    rect(t, 1, 13, 14, 13, RCOPPER[1] + (255,)); rect(t, 1, 2, 14, 2, (210, 214, 222, 255))
    return t

def blend_icon():
    t = Image.new("RGBA", (16, 16)); rnd = random.Random(151)
    for y in range(5, 15):
        w = min(7, (y - 4) + 1)
        for x in range(8 - w, 8 + w):
            k = rnd.random()
            col = (38, 70, 170) if k < 0.55 else (70, 100, 200) if k < 0.8 else (120, 126, 150)
            t.putpixel((x, y), jitter(col, rnd, 6))
    for x in range(2, 14):
        px(t, x, 14, (24, 40, 110, 255))
    return t

def milestone3(shaped, rec):
    ti = A + "/textures/item/"
    for n, arm, centre in (("iron_core", IRON, RCOPPER), ("copper_core", COP4, RCOPPER),
                           ("gold_core", GOLD4, RCOPPER), ("diamond_core", STEEL, DIAM4)):
        flat_item(n, core_icon(arm, centre))
    flat_item("pv_cell", pv_icon())
    flat_item("silicon_lapis_blend", blend_icon())
    for t in range(1, 5):
        solar_assets(t)
    helix_assets()
    tag = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    vals = json.load(open(tag))["values"]
    jdump({"values": vals + [f"{MOD}:{SOLAR_NAMES[t]}_solar_panel" for t in range(1, 5)] + [f"{MOD}:wind_turbine", f"{MOD}:turbine_blade"]}, tag)
    RC = f"{MOD}:redstone_copper_ingot"
    shaped("iron_core", [" P ", "PRP", " P "], {"P": "#c:plates/iron", "R": RC})
    shaped("copper_core", [" P ", "PRP", " P "], {"P": "#c:plates/copper", "R": RC})
    shaped("gold_core", [" P ", "PRP", " P "], {"P": "#c:plates/gold", "R": RC})
    shaped("diamond_core", [" P ", "PDP", " P "], {"P": "#c:plates/steel", "D": "#c:gems/diamond"})
    shaped("pv_cell", ["GGG", "LWL", " R "], {"G": "#c:glass_blocks", "L": "#c:gems/lapis", "W": f"{MOD}:silicon_wafer", "R": RC}, 2)
    jdump({"type": f"{MOD}:alloying", "first": {"ingredient": f"{MOD}:silicon_wafer"}, "second": {"ingredient": "#c:gems/lapis", "count": 3},
           "result": {"id": f"{MOD}:silicon_lapis_blend"}, "cooking_time": 200}, rec + "alloying/silicon_lapis_blend.json")
    PV = f"{MOD}:pv_cell"
    shaped("iron_solar_panel", ["GGG", "VVV", "PCP"], {"G": "#c:glass_blocks", "V": PV, "P": "#c:plates/iron", "C": f"{MOD}:iron_core"})
    shaped("copper_solar_panel", ["SVS", "VCV", "SVS"], {"S": f"{MOD}:iron_solar_panel", "V": PV, "C": f"{MOD}:copper_core"})
    shaped("gold_solar_panel", ["SVS", "VCV", "SVS"], {"S": f"{MOD}:copper_solar_panel", "V": PV, "C": f"{MOD}:gold_core"})
    shaped("diamond_solar_panel", ["SBS", "BCB", "SBS"], {"S": f"{MOD}:gold_solar_panel", "B": f"{MOD}:silicon_lapis_blend", "C": f"{MOD}:diamond_core"})
    shaped("wind_turbine", [" R ", "CXC", "III"], {"C": "#c:plates/copper", "R": "#c:rods/steel", "I": "#c:plates/iron", "X": RC})
    shaped("turbine_blade", ["S  ", "CRC", "  S"], {"S": "#c:plates/steel", "C": "#c:plates/copper", "R": "#c:rods/steel"})
    lang = json.load(open(f"{A}/lang/en_us.json"))
    lang.update({
        "item.simplemachines.iron_core": "Iron Core", "item.simplemachines.copper_core": "Copper Core",
        "item.simplemachines.gold_core": "Gold Core", "item.simplemachines.diamond_core": "Diamond Core",
        "item.simplemachines.pv_cell": "PV Cell", "item.simplemachines.silicon_lapis_blend": "Silicon-Lapis Blend",
        "block.simplemachines.iron_solar_panel": "Iron Solar Panel", "block.simplemachines.copper_solar_panel": "Copper Solar Panel",
        "block.simplemachines.gold_solar_panel": "Gold Solar Panel", "block.simplemachines.diamond_solar_panel": "Diamond Solar Panel",
        "block.simplemachines.wind_turbine": "Wind Turbine Base",
        "block.simplemachines.turbine_blade": "Turbine Blade",
        "tooltip.simplemachines.wind_base": "Stack 1 to 5 Turbine Blades on top. Stores the power, LV output",
        "tooltip.simplemachines.blade_1": "Up to 50 FE/t per blade on a Wind Turbine Base",
        "tooltip.simplemachines.blade_2": "Only the first 5 blades in a stack turn",
        "message.simplemachines.wind_no_blades": "No blades: stack 1 to 5 Turbine Blades on top. Stored %1$s / %2$s FE",
        "message.simplemachines.wind_spare": "%1$s more above do nothing: only %2$s blades count.",
        "message.simplemachines.blade_idle": "Idle: a blade needs a Wind Turbine Base under its stack, and only the first %1$s blades turn.",
        "message.simplemachines.solar": "Making %1$s FE/t (%2$s). Stored %3$s / %4$s FE",
        "message.simplemachines.wind": "Making %1$s FE/t with %2$s of %3$s blades (height %4$s%%, open air %5$s%%). Stored %6$s / %7$s FE",
        "jei.simplemachines.info.iron_solar_panel": "4 FE per tick in daylight with open sky above. Stores 2,000 FE. LV output from the sides and bottom.",
        "jei.simplemachines.info.copper_solar_panel": "20 FE per tick in daylight. Stores 10,000 FE. LV output.",
        "jei.simplemachines.info.gold_solar_panel": "100 FE per tick in daylight. Stores 50,000 FE. LV output.",
        "jei.simplemachines.info.diamond_solar_panel": "1,000 FE per tick in daylight and 500 at night. Stores 500,000 FE. MV output: connect it with Gold Cable or an LV/MV Transformer front, or LV blocks next to it explode.",
        "jei.simplemachines.info.wind_turbine": "Holds the power of a wind turbine: stack 1 to 5 Turbine Blades on top. Each blade adds up to 50 FE per tick, so 5 blades make up to 250. Output is measured at the top blade: more the higher it is (full at Y 160), the more open air around it (9x9x9) and in rain (x1.25) or thunder (x1.5). Stores 50,000 FE, LV output from the base.",
        "jei.simplemachines.info.turbine_blade": "Half a turn of helix. Stack up to 5 on a Wind Turbine Base and they join into one helix that turns faster as it makes more power. A blade with no base under its stack, or a 6th blade or higher, does nothing.",
        "jei.simplemachines.info.iron_core": "Core of the Iron Solar Panel.",
        "jei.simplemachines.info.copper_core": "Core of the Copper Solar Panel.",
        "jei.simplemachines.info.gold_core": "Core of the Gold Solar Panel.",
        "jei.simplemachines.info.diamond_core": "Core of the Diamond Solar Panel.",
        "jei.simplemachines.info.pv_cell": "Solar cell. Goes into every solar panel up to Gold.",
        "jei.simplemachines.info.silicon_lapis_blend": "Made in the Alloy Smelter from a Silicon Wafer and 3 Lapis. Goes into the Diamond Solar Panel.",
    })
    jdump(lang, f"{A}/lang/en_us.json")

def hazard_tex(seed, hollow):
    rnd = random.Random(seed)
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if hollow and 3 <= x <= 12 and 3 <= y <= 12:
                continue
            band = ((x + y) // 3) % 2
            t.putpixel((x, y), jitter(HONEY[1] if band else (40, 40, 44), rnd, 5))
    if hollow:
        for i in range(3, 13):
            px(t, i, 3 + (i - 3), STEEL[2] + (255,)); px(t, i, 12 - (i - 3), STEEL[2] + (255,))
    for i in range(16):
        px(t, i, 0, STEEL[0] + (255,)); px(t, 0, i, STEEL[0] + (255,)); px(t, i, 15, STEEL[3] + (255,)); px(t, 15, i, STEEL[3] + (255,))
    return t

def milestone4(shaped, rec):
    tb = A + "/textures/block/"
    save(hazard_tex(101, True), tb + "quarry_frame.png")
    model("quarry_frame", [el((0, 0, 0), (16, 16, 16), all_faces("all"))], {"all": f"{MOD}:block/quarry_frame", "particle": f"{MOD}:block/quarry_frame"})
    jdump({"variants": {"": {"model": f"{MOD}:block/quarry_frame"}}}, f"{A}/blockstates/quarry_frame.json")
    save(plate(STEEL, 102), tb + "quarry_drill_head.png")
    bit = solid_tex(STEEL[0], 103, 3)
    for y in range(16):
        px(bit, (y * 2) % 16, y, STEEL[2] + (255,))
    save(bit, tb + "quarry_drill_bit.png")
    model("quarry_drill_head", [el((3, 8, 3), (13, 16, 13), all_faces("head")), el((5, 6, 5), (11, 8, 11), all_faces("head")),
                                el((6, 0, 6), (10, 6, 10), all_faces("bit")), el((7, -2, 7), (9, 0, 9), all_faces("bit"))],
          {"head": f"{MOD}:block/quarry_drill_head", "bit": f"{MOD}:block/quarry_drill_bit", "particle": f"{MOD}:block/quarry_drill_head"})
    jdump({"variants": {"": {"model": f"{MOD}:block/quarry_drill_head"}}}, f"{A}/blockstates/quarry_drill_head.json")
    post = solid_tex((236, 236, 236), 104, 3)
    for y in range(0, 16):
        if (y // 3) % 2:
            for x in range(16):
                px(post, x, y, jitter(RED[1], random.Random(y), 4))
    save(post, tb + "landmark.png")
    model("landmark", [el((6, 0, 6), (10, 10, 10), all_faces("post")), el((5.5, 10, 5.5), (10.5, 12, 10.5), all_faces("lamp"))],
          {"post": f"{MOD}:block/landmark", "lamp": "minecraft:block/redstone_block", "particle": f"{MOD}:block/landmark"})
    jdump({"variants": {"": {"model": f"{MOD}:block/landmark"}}}, f"{A}/blockstates/landmark.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/landmark"}}, f"{A}/items/landmark.json")
    loot_self("landmark")
    tag = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    vals = json.load(open(tag))["values"]
    jdump({"values": vals + [f"{MOD}:landmark", f"{MOD}:quarry_frame", f"{MOD}:quarry_drill_head"]}, tag)
    jdump({"type": f"{MOD}:crafting_shaped_quarry", "category": "misc",
           "key": {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "S": "#c:plates/steel", "D": f"{MOD}:electric_drill"},
           "pattern": ["RCR", "SDS"], "result": {"id": f"{MOD}:quarry_assembly"}}, rec + "quarry_assembly.json")
    shaped("landmark", ["T", "R", "C"], {"T": "minecraft:redstone_torch", "R": "#c:rods/steel", "C": "#c:ingots/copper"}, 2)
    lang = json.load(open(f"{A}/lang/en_us.json"))
    lang.update({
        "block.simplemachines.landmark": "Landmark", "block.simplemachines.quarry_frame": "Quarry Frame",
        "block.simplemachines.quarry_drill_head": "Quarry Drill Head",
        "tooltip.simplemachines.quarry_drill": "Drill: %1$s, %2$s, +%3$s%% speed",
        "tooltip.simplemachines.frame_iron": "iron body", "tooltip.simplemachines.frame_steel": "steel body",
        "tooltip.simplemachines.tip_standard": "standard bit", "tooltip.simplemachines.tip_diamond": "diamond bit",
        "message.simplemachines.quarry": "Quarry %1$sx%2$s, at Y %3$s, %4$s blocks dug",
        "message.simplemachines.quarry_filter_on": "Filter fitted (%1$s of %2$s)",
        "message.simplemachines.quarry_filter_off": "Filter removed",
        "message.simplemachines.quarry_filter_full": "All %s filter slots are full. Sneak with an empty hand to take one out",
        "jei.simplemachines.info.quarry": "Runs on MV at 8000 FE a block. Digs out the area beside it down to bedrock, skipping air and fluids. Place Landmarks up to 64 blocks away in front of it and to one side to set the size, otherwise 16x16. Speed comes from the Electric Drill built into its assembly: iron 1 block per second, steel chassis x1.5, steel drill body x1.25, plus the drill's upgrades (up to x2). A diamond bit digs obsidian and ancient debris. Fit up to 3 Pipe Filters to choose what it keeps; the rest is voided. Deny filters always void what they list, and with any allow filter fitted, only what one of them allows is kept.",
        "jei.simplemachines.info.quarry_assembly": "Crafted with an Electric Drill, which keeps its body, bit and upgrades inside the quarry.",
        "jei.simplemachines.info.landmark": "Marks a far corner of a quarry: one in line with its front, one to its side, up to 64 blocks away.",
    })
    jdump(lang, f"{A}/lang/en_us.json")

# ---- powered tools ----

TOOLS = {"electric_drill": ("Electric Drill", powered_icons.drill, ["pickaxes", "shovels"]),
         "chainsaw": ("Chainsaw", powered_icons.chainsaw, ["axes"])}

def flat_item(name, tex, parent="minecraft:item/generated"):
    save(tex, f"{A}/textures/item/{name}.png")
    jdump({"parent": parent, "textures": {"layer0": f"{MOD}:item/{name}"}}, f"{A}/models/item/{name}.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}}, f"{A}/items/{name}.json")

def powered(shaped):
    lang = json.load(open(f"{A}/lang/en_us.json"))
    flat_item("power_cell", powered_icons.cell())
    lang["item.simplemachines.power_cell"] = "Power Cell"
    tags = {}
    for name, (title, draw, ts) in TOOLS.items():
        lang[f"item.simplemachines.{name}"] = title
        for t in ts: tags.setdefault(t, []).append(f"{MOD}:{name}")
        lang[f"subtitles.simplemachines.{name}"] = f"{title} runs"
    for t, v in tags.items(): jdump({"values": v}, f"{D}/minecraft/tags/item/{t}.json")
    jdump({f"item.{n}.run": {"subtitle": f"subtitles.simplemachines.{n}", "sounds": [{"name": f"{MOD}:item/{n}", "stream": False}]}
           for n in TOOLS}, f"{A}/sounds.json")
    lang["tooltip.simplemachines.charge"] = "Charge: %s / %s FE"
    lang["tooltip.simplemachines.no_charge"] = "Out of charge: recharge it in a Charger"
    jdump(lang, f"{A}/lang/en_us.json")
    S, R, P, B = "#c:plates/steel", "#c:rods/steel", f"{MOD}:power_cell", f"{MOD}:control_board"
    shaped("power_cell", ["SRS", "SDS", "SRS"], {"S": "#c:plates/copper", "R": f"{MOD}:redstone_copper_ingot", "D": "minecraft:redstone_block"})
    gen_tools.main(A, D, save, jdump, shaped, flat_item, loot_self)

def upgrades(shaped):
    lang = json.load(open(f"{A}/lang/en_us.json"))
    for name, title, draw in [("overclocker_circuit", "Overclocker Circuit", powered_icons.overclocker),
                              ("energy_cell", "Energy Cell", powered_icons.energy_cell)]:
        flat_item(name, draw())
        save(powered_icons.card(name), f"{A}/textures/block/{name}.png")
        lang[f"item.simplemachines.{name}"] = title
    lang["tooltip.simplemachines.overclocker_circuit"] = "Machine upgrade: +50% speed and FE use"
    lang["tooltip.simplemachines.energy_cell"] = "Machine upgrade: +100% energy storage"
    lang["tooltip.simplemachines.upgrade_fit"] = "Fit after a machine's control board, or on a finished tool on its stand, then screw in (2 slots)"
    lang["tooltip.simplemachines.upgrades"] = "Upgrades: %s"
    lang["upgrade.simplemachines.overclocker_circuit"] = "Overclock"
    lang["upgrade.simplemachines.energy_cell"] = "Cell"
    lang["upgrade.simplemachines.times"] = "%s x%s"
    lang["upgrade.simplemachines.and"] = "%s + %s"
    lang["message.simplemachines.upgrade_early"] = "Fix the control board before fitting upgrades"
    lang["message.simplemachines.upgrade_late"] = "Upgrades go in before the side panels"
    lang["message.simplemachines.upgrades_full"] = "Both upgrade slots are full"
    lang["message.simplemachines.upgrade_fitted"] = "Upgrade fitted (%s/%s)"
    # recipe viewer pages
    for k, v in {
        "alloying": "Alloying", "rolling": "Rolling", "extruding": "Extruding", "crushing": "Crushing",
        "sawing": "Sawing", "generator": "Generator Fuel", "stone_generator": "Stone Generator",
        "water_generator": "Water Generator", "mechanical_hive": "Mechanical Hive",
        "line": "%s: %s", "powered": "%s s, %s FE", "per_tick": "%s FE per tick",
        "steel_time": "Steel chassis: %s s, %s FE", "fuel_time": "%s s, burns fuel",
        "mode_rods": "Set the mill to Rods", "mode_casings": "Set the mill to Casings",
        "compressing": "Compressing", "lava_generator": "Lava Generator",
        "lava_note": "1 mB per 10 ticks, a bucket lasts %s s",
        "mode_tip": "Click the mode button in the rolling mill screen",
        "bonus": "Steel chassis: %s%% chance of +1", "bonus_tip": "Only on a steel chassis",
        "burns": "Burns %s s", "iron_chassis": "Iron", "steel_chassis": "Steel",
        "makes": "%s chassis: %s FE", "generator_note": "Pauses while its buffer is full",
        "beside": "Next to the machine, not inside it",
        "stone_note": "Water and lava beside it",
        "water_note": "2 water sources beside it",
        "hive_note": "Honey only if given bottles",
    }.items():
        lang[f"jei.simplemachines.{k}"] = v
    jdump(lang, f"{A}/lang/en_us.json")
    B = f"{MOD}:control_board"
    shaped("overclocker_circuit", ["RGR", "QBQ", "RGR"], {"R": f"{MOD}:redstone_copper_ingot", "G": "#c:ingots/gold", "Q": "#c:gems/quartz", "B": B})
    shaped("energy_cell", [" S ", "PBP", " S "], {"S": "#c:plates/steel", "P": f"{MOD}:power_cell", "B": B})

def loot_self(b):
    jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
           "conditions": [{"condition": "minecraft:survives_explosion"}],
           "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}]}],
           "random_sequence": f"{MOD}:blocks/{b}"}, f"{D}/{MOD}/loot_table/blocks/{b}.json")

# items that are not ingredients but should still show a recipe
EXTRA_UNLOCKS = {"fluid_tank": [f"{MOD}:fluid_pipe"], "pipe_filter": [f"{MOD}:item_pipe"]}

def recipe_unlocks():
    # recipe book unlock per recipe: holding any ingredient (or knowing the recipe) grants it
    rec = f"{D}/{MOD}/recipe"
    out = f"{D}/{MOD}/advancement/recipes"
    shutil.rmtree(out, ignore_errors=True)
    for f in sorted(glob.glob(f"{rec}/**/*.json", recursive=True)):
        r = json.load(open(f))
        name = os.path.relpath(f, rec)[:-5]
        ings = list(r.get("key", {}).values()) + r.get("ingredients", []) + [r.get("ingredient")]
        ings += [r.get(k, {}).get("ingredient") for k in ("first", "second", "input")]
        seen = []
        for i in ings:
            if i and i not in seen:
                seen.append(i)
        seen += [i for i in EXTRA_UNLOCKS.get(name, []) if i not in seen]
        if name.endswith("_assembly"):
            # a chassis in hand shows everything that can go on it
            seen += [f"{MOD}:iron_chassis", f"{MOD}:steel_chassis"]
        crit = {"has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"{MOD}:{name}"}}}
        for n, i in enumerate(seen):
            crit[f"has_{n}"] = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": i}]}}
        adv = {k: v for k, v in r.items() if k in ("neoforge:conditions", "fabric:load_conditions")}
        adv.update({"parent": "minecraft:recipes/root", "criteria": crit, "requirements": [list(crit)],
                    "rewards": {"recipes": [f"{MOD}:{name}"]}})
        jdump(adv, f"{out}/{r.get('category', 'misc')}/{name}.json")

main()
gen_manual.main()
recipe_unlocks()

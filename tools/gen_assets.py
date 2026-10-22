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

def steel_frame():
    rnd = random.Random(13)
    t = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x < 2 or x > 13 or y < 2 or y > 13:
                t.putpixel((x, y), jitter(STEEL[1], rnd, 6))
    for i in range(16):
        px(t, i, 0, STEEL[0] + (255,)); px(t, 0, i, STEEL[0] + (255,))
        px(t, i, 15, STEEL[3] + (255,)); px(t, 15, i, STEEL[3] + (255,))
    for i in range(2, 14):
        px(t, i, 13, STEEL[2] + (255,)); px(t, 13, i, STEEL[2] + (255,))
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        px(t, x, y, STEEL[0] + (255,))
    return t

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "........ooo.....",
    "......ooHHHo....",
    "....ooHHHMMo....",
    "..ooHHHMMMMLo...",
    ".oHHMMMMMMLLo...",
    ".oMMMMMMLLLLo...",
    ".oLMMMLLLLLDo...",
    "..oLLLLLLDDo....",
    "...ooLLDDoo.....",
    ".....oooo.......",
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
    xs = [46, 64] if inputs == 2 else [55]
    for x in xs:
        slot(x, 16)
    slot(55, 52)
    slot(111, 30, 26)
    grey = (139, 139, 139, 255)
    for i in range(16):
        rect(t, 80, 41, 80 + min(i, 15), 42, grey)
    for k in range(8):
        rect(t, 95 + k, 35 + k, 95 + k, 48 - k, grey)
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
    save(steel_frame(), tb + "steel_chassis.png")
    save(sprite(INGOT, STEEL), ti + "steel_ingot.png")
    save(sprite(INGOT, RCOPPER), ti + "redstone_copper_ingot.png")
    save(sprite(ROD, IRON), ti + "iron_rod.png")
    save(sprite(ROD, COPPER), ti + "copper_rod.png")
    save(sprite(ROD, STEEL), ti + "steel_rod.png")
    save(gui(2), A + "/textures/gui/alloy_furnace.png")
    save(gui(1), A + "/textures/gui/extruder.png")
    save(alloy_front(True).resize((64, 64), Image.NEAREST), R + "/simplemachines_logo.png")

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
    jdump({"variants": {"": {"model": f"{MOD}:block/steel_chassis"}}}, f"{A}/blockstates/steel_chassis.json")

    # 12 bars, 2px thick; faces take their uv from position, which lands on the frame border
    bars = []
    for a in range(3):
        for p in [0, 14]:
            for q in [0, 14]:
                lo, hi = [0, 0, 0], [16, 16, 16]
                b, c = [k for k in range(3) if k != a]
                lo[b], hi[b] = p, p + 2
                lo[c], hi[c] = q, q + 2
                bars.append({"from": lo, "to": hi, "faces": {d: {"texture": "#frame"} for d in
                             ["north", "south", "east", "west", "up", "down"]}})
    jdump({"parent": "minecraft:block/block", "ambientocclusion": False,
           "textures": {"frame": f"{MOD}:block/steel_chassis", "particle": f"{MOD}:block/steel_chassis"},
           "elements": bars}, f"{A}/models/block/steel_chassis.json")

    items = ["steel_ingot", "redstone_copper_ingot", "iron_rod", "copper_rod", "steel_rod"]
    for it in items:
        jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{it}"}}, f"{A}/models/item/{it}.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{it}"}}, f"{A}/items/{it}.json")
    blocks = ["alloy_furnace", "extruder", "steel_chassis"]
    for b in blocks:
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/{b}"}}, f"{A}/items/{b}.json")
        jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
               "conditions": [{"condition": "minecraft:survives_explosion"}],
               "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}]}],
               "random_sequence": f"{MOD}:blocks/{b}"}, f"{D}/{MOD}/loot_table/blocks/{b}.json")

    jdump({
        "itemGroup.simplemachines": "Simple Machines",
        "item.simplemachines.steel_ingot": "Steel Ingot",
        "item.simplemachines.redstone_copper_ingot": "Redstone Copper Ingot",
        "item.simplemachines.iron_rod": "Iron Rod",
        "item.simplemachines.copper_rod": "Copper Rod",
        "item.simplemachines.steel_rod": "Steel Rod",
        "block.simplemachines.alloy_furnace": "Alloy Furnace",
        "block.simplemachines.extruder": "Extruder",
        "block.simplemachines.steel_chassis": "Steel Chassis",
        "container.simplemachines.alloy_furnace": "Alloy Furnace",
        "container.simplemachines.extruder": "Extruder",
    }, f"{A}/lang/en_us.json")

    jdump({"values": [f"{MOD}:{b}" for b in blocks]}, f"{D}/minecraft/tags/block/mineable/pickaxe.json")
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

    def shaped(name, pattern, key):
        jdump({"type": "minecraft:crafting_shaped", "category": "misc", "key": key, "pattern": pattern,
               "result": {"id": f"{MOD}:{name}"}}, rec + name + ".json")
    shaped("alloy_furnace", ["BBB", "CFC", "BBB"], {"B": "minecraft:bricks", "C": "#c:ingots/copper", "F": "minecraft:furnace"})
    shaped("extruder", ["III", "IPI", "IFI"], {"I": "#c:ingots/iron", "P": "minecraft:piston", "F": "minecraft:furnace"})
    shaped("steel_chassis", ["IRI", "RRR", "IRI"], {"I": "#c:ingots/steel", "R": "#c:rods/steel"})

main()

#!/usr/bin/env python3
# Ender devices and their parts (2.2.0). Run from the repo root after gen_assets.py: python3 tools/gen_ender.py
import json, os, shutil, sys
from PIL import Image

g = {"__file__": os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_assets.py"), "__name__": "gen_assets"}
exec(open(g["__file__"]).read().rsplit("\nmain()", 1)[0], g)
A, D, MOD, rect, px, save, jdump, model = g["A"], g["D"], g["MOD"], g["rect"], g["px"], g["save"], g["jdump"], g["model"]
PLAN = "/workspace/mods/simplemachines-dist/2.2-plan/textures/"
DARK = (36, 30, 28, 255)
TEAL = [(44, 205, 177), (52, 153, 136), (24, 86, 76)]
AMBER = [(232, 190, 50), (120, 92, 30)]
KEYS = [(200, 206, 224), (150, 156, 176)]
CORES = {"programmer": "ender_programmer", "buffer": "ender_buffer", "tank": "ender_tank"}
LINKED = ("buffer", "tank")
g["FRONT_HOLE"].update({"programmer": (3, 3, 13, 13), "buffer": (3, 3, 13, 13), "tank": (4, 2, 12, 14)})

def front(lit):
    # the plan's screen and keypad, packed into the 10x10 window the front panel leaves open
    t = g["core_base"](51)
    rect(t, 3, 3, 12, 12, DARK)
    screen = TEAL[0] if lit else TEAL[2]
    rect(t, 4, 4, 11, 4, screen + (255,))
    for u in range(4, 12, 2):
        px(t, u, 5, screen + (255,))
    for v in (7, 9, 11):
        for u in range(5, 11):
            px(t, u, v, KEYS[(u - 5) % 2] + (255,))
    if lit:
        px(t, 10, 11, TEAL[0] + (255,))
    px(t, 12, 3, (AMBER[0] if lit else AMBER[1]) + (255,))
    return t

def top():
    t = g["core_top"]("programmer")
    px(t, 12, 3, TEAL[1] + (255,))
    return t

def panel(W, H):
    t = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    rect(t, 1, 1, W - 2, H - 2, (198, 198, 198, 255))
    rect(t, 1, 0, W - 3, 0, (0, 0, 0, 255)); rect(t, 0, 1, 0, H - 3, (0, 0, 0, 255))
    rect(t, 2, H - 1, W - 2, H - 1, (0, 0, 0, 255)); rect(t, W - 1, 2, W - 1, H - 2, (0, 0, 0, 255))
    px(t, W - 2, 1, (0, 0, 0, 255)); px(t, 1, H - 2, (0, 0, 0, 255)); px(t, W - 2, H - 2, (0, 0, 0, 255))
    rect(t, 1, 1, W - 3, 2, (255, 255, 255, 255)); rect(t, 1, 1, 2, H - 3, (255, 255, 255, 255))
    rect(t, 3, H - 3, W - 2, H - 2, (85, 85, 85, 255)); rect(t, W - 3, 3, W - 2, H - 2, (85, 85, 85, 255))
    px(t, W - 2, H - 2, (0, 0, 0, 0)); px(t, W - 3, H - 3, (85, 85, 85, 255))
    return t

def inset(t, x0, y0, x1, y1, fill=(139, 139, 139, 255)):
    rect(t, x0, y0, x1, y1, fill)
    rect(t, x0, y0, x1 - 1, y0, (55, 55, 55, 255)); rect(t, x0, y0, x0, y1 - 1, (55, 55, 55, 255))
    rect(t, x0 + 1, y1, x1, y1, (255, 255, 255, 255)); rect(t, x1, y0 + 1, x1, y1, (255, 255, 255, 255))

def inventory(t, y):
    for r in range(3):
        for c in range(9):
            inset(t, 7 + c * 18, y + r * 18, 24 + c * 18, y + 17 + r * 18)
    for c in range(9):
        inset(t, 7 + c * 18, y + 58, 24 + c * 18, y + 75)

def buffer_gui():
    t = panel(176, 195)
    for r in range(3):
        for c in range(9):
            inset(t, 7 + c * 18, 17 + r * 18, 24 + c * 18, 34 + r * 18)
    inset(t, 7, 87, 168, 94)
    inventory(t, 112)
    return t

def tank_gui():
    t = panel(176, 166)
    inset(t, 39, 19, 56, 36)
    inset(t, 39, 43, 56, 60)
    inset(t, 79, 16, 96, 69)
    inset(t, 152, 16, 167, 69)
    inventory(t, 83)
    return t

def gui_tex():
    t = g["gui"]("gen")
    bg = (198, 198, 198, 255)
    rect(t, 60, 16, 150, 80, bg)
    def inset(x0, y0, x1, y1, fill=(139, 139, 139, 255)):
        rect(t, x0, y0, x1, y1, fill)
        rect(t, x0, y0, x1 - 1, y0, (55, 55, 55, 255)); rect(t, x0, y0, x0, y1 - 1, (55, 55, 55, 255))
        rect(t, x0 + 1, y1, x1, y1, (255, 255, 255, 255)); rect(t, x1, y0 + 1, x1, y1, (255, 255, 255, 255))
    inset(8, 19, 100, 32)
    inset(119, 40, 136, 57)
    return t

def part(m, when, y):
    a = {"apply": {"model": f"{MOD}:block/{m}"}}
    if y:
        a["apply"]["y"] = y
    a["when"] = when
    return a

def lang_update():
    p = f"{A}/lang/en_us.json"
    lang = json.load(open(p))
    lang.update({
        "item.simplemachines.ender_ingot": "Ender Ingot",
        "item.simplemachines.ender_wire": "Ender Wire",
        "item.simplemachines.ender_coil": "Ender Coil",
        "item.simplemachines.link_card": "Link Card",
        "item.simplemachines.programmer_assembly": "Programmer Assembly",
        "block.simplemachines.ender_programmer": "Ender Programmer",
        "container.simplemachines.ender_programmer": "Ender Programmer",
        "gui.simplemachines.ender_code": "Code",
        "gui.simplemachines.ender_random": "Random",
        "gui.simplemachines.ender_write": "Write",
        "gui.simplemachines.ender_private": "Private",
        "gui.simplemachines.ender_public": "Public",
        "gui.simplemachines.ender_private_hint": "Private codes belong to you alone and never clash with anyone else's",
        "gui.simplemachines.ender_wrote": "Wrote %s to the card.",
        "gui.simplemachines.ender_no_card": "Put a Link Card in the slot.",
        "gui.simplemachines.ender_no_power": "Not enough power.",
        "gui.simplemachines.ender_not_owner": "That card holds someone else's code.",
        "gui.simplemachines.ender_no_code": "Type a code first.",
        "gui.simplemachines.ender_cost": "%s FE per write",
        "tooltip.simplemachines.ender_code": "Code %s, %s",
        "tooltip.simplemachines.ender_public": "public",
        "tooltip.simplemachines.ender_private": "private",
        "tooltip.simplemachines.link_card_blank": "Blank: write a code in an Ender Programmer",
        "tooltip.simplemachines.link_card": "Right-click an ender device to set its code, sneak to copy it",
        "tooltip.simplemachines.uses_write": "Uses %s FE per write",
        "message.simplemachines.ender_set": "Code set to %s",
        "message.simplemachines.ender_copied": "Copied code %s to the card",
        "message.simplemachines.ender_not_owner": "This code belongs to someone else",
        "message.simplemachines.ender_now_private": "Code %s is now private",
        "message.simplemachines.ender_now_public": "Code %s is now public",
        "item.simplemachines.buffer_assembly": "Buffer Assembly",
        "item.simplemachines.tank_assembly": "Tank Assembly",
        "item.simplemachines.ender_backpack": "Ender Backpack",
        "block.simplemachines.ender_buffer": "Ender Buffer",
        "block.simplemachines.ender_tank": "Ender Tank",
        "container.simplemachines.ender_buffer": "Ender Buffer",
        "container.simplemachines.ender_tank": "Ender Tank",
        "container.simplemachines.ender_backpack": "Ender Backpack",
        "gui.simplemachines.ender_no_card": "Put a Link Card or Backpack in the slot.",
        "gui.simplemachines.ender_not_owner": "That holds someone else's code.",
        "gui.simplemachines.ender_wrote": "Wrote %s.",
        "gui.simplemachines.ender_code_label": "Code %s",
        "gui.simplemachines.ender_no_link": "No code: right-click with a Link Card",
        "gui.simplemachines.ender_offline": "No power: not sharing",
        "gui.simplemachines.ender_owner_public": "Public. Owner: %s",
        "gui.simplemachines.ender_owner_private": "Private. Owner: %s",
        "gui.simplemachines.ender_someone": "someone else",
        "gui.simplemachines.ender_fe": "%s / %s FE",
        "gui.simplemachines.ender_tank_empty": "Empty",
        "gui.simplemachines.ender_tank_amount": "%s / %s B",
        "tooltip.simplemachines.backpack_blank": "Blank: write a code in an Ender Programmer",
        "tooltip.simplemachines.backpack": "Right-click to open the Ender Buffer on its code",
        "tooltip.simplemachines.uses_open": "Uses %s FE per open",
        "message.simplemachines.backpack_no_code": "This backpack has no code yet",
        "message.simplemachines.backpack_no_power": "The backpack needs charging",
    })
    jdump(lang, p)

def recipes():
    rec = f"{D}/{MOD}/recipe/"
    def shaped(name, pattern, key, count=1):
        result = {"id": f"{MOD}:{name}"}
        if count > 1:
            result["count"] = count
        jdump({"type": "minecraft:crafting_shaped", "category": "misc", "key": key, "pattern": pattern, "result": result}, rec + name + ".json")
    jdump({"type": f"{MOD}:alloying", "first": {"ingredient": f"{MOD}:redstone_copper_ingot"}, "second": {"ingredient": "#c:ender_pearls"},
           "result": {"id": f"{MOD}:ender_ingot"}, "cooking_time": 400}, rec + "alloying/ender_ingot.json")
    jdump({"type": f"{MOD}:rolling", "input": {"ingredient": f"{MOD}:ender_ingot"},
           "result": {"id": f"{MOD}:ender_wire", "count": 2}, "cooking_time": 100}, rec + "rolling/ender_wire.json")
    W = f"{MOD}:ender_wire"
    shaped("ender_coil", [" W ", "WSW", " W "], {"W": W, "S": "#c:rods/steel"})
    shaped("link_card", ["PWP"], {"P": "#c:plates/iron", "W": W}, 2)
    shaped("programmer_assembly", ["RCR", "WIW"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "W": W, "I": f"{MOD}:ender_ingot"})
    E = f"{MOD}:ender_coil"
    shaped("buffer_assembly", ["RCR", "EXE"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "E": E, "X": "minecraft:chest"})
    shaped("tank_assembly", ["RCR", "EXE"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "E": E, "X": f"{MOD}:fluid_tank"})
    shaped("ender_backpack", ["LWL", "LEL", "LPL"], {"L": "minecraft:leather", "W": W, "E": E, "P": f"{MOD}:power_cell"})

def device(CORE, BLOCK):
    tb = A + "/textures/block/"
    if CORE == "programmer":
        save(front(False), tb + f"{CORE}_core_front.png")
        save(front(True), tb + f"{CORE}_core_front_on.png")
        save(top(), tb + f"{CORE}_core_top.png")
        save(gui_tex(), A + f"/textures/gui/{BLOCK}.png")
    else:
        for suffix in ["front", "front_on", "top"]:
            shutil.copy(PLAN + f"{BLOCK}_core_{suffix}.png", tb + f"{CORE}_core_{suffix}.png")
        save(buffer_gui() if CORE == "buffer" else tank_gui(), A + f"/textures/gui/{BLOCK}.png")

    core_tex = lambda on: {"front": f"{MOD}:block/{CORE}_core_front" + on, "side": f"{MOD}:block/core_side",
                           "top": f"{MOD}:block/{CORE}_core_top", "bottom": f"{MOD}:block/core_bottom", "particle": f"{MOD}:block/core_side"}
    model(f"{CORE}_core", g["core_elements"](False), core_tex(""))
    model(f"{CORE}_core_on", g["core_elements"](False), core_tex("_on"))
    model(f"{CORE}_core_loose", g["core_elements"](True), core_tex(""))
    for f in g["FINISHES"]:
        pt = {"panel": g["finish_tex"](f, "panel"), "particle": g["finish_tex"](f, "panel")}
        for loose in ["", "_loose"]:
            model(f"{CORE}_{f}_front_panel{loose}", g["front_panel"](CORE, bool(loose)), pt)

    # the chassis blockstates were written for every other core already
    for tier in ["iron", "steel"]:
        p = f"{A}/blockstates/{tier}_chassis.json"
        parts = [x for x in json.load(open(p))["multipart"] if x.get("when", {}).get("core") != CORE]
        for f, y in g["FACINGS"]:
            parts.append(part(f"{CORE}_core_loose", {"facing": f, "core": CORE, "step": "1"}, y))
            parts.append(part(f"{CORE}_core", {"facing": f, "core": CORE, "step": "2|3|4|5|6|7"}, y))
            for fin in g["FINISHES"]:
                parts.append(part(f"{CORE}_{fin}_front_panel_loose", {"facing": f, "core": CORE, "panel": fin, "step": "5"}, y))
                parts.append(part(f"{CORE}_{fin}_front_panel", {"facing": f, "core": CORE, "panel": fin, "step": "6|7"}, y))
        jdump({"multipart": parts}, p)

    parts = [{"when": {"tier": t}, "apply": {"model": f"{MOD}:block/{t}_chassis"}} for t in ["iron", "steel"]]
    for f, y in g["FACINGS"]:
        parts.append(part(f"{CORE}_core", {"facing": f, "lit": "false"}, y))
        parts.append(part(f"{CORE}_core_on", {"facing": f, "lit": "true"}, y))
        for fin in g["FINISHES"]:
            parts.append(part(f"{CORE}_{fin}_front_panel", {"facing": f, "panel": fin}, y))
            parts.append(part(f"{fin}_side_panels", {"facing": f, "panel": fin}, y))
    jdump({"multipart": parts}, f"{A}/blockstates/{BLOCK}.json")
    elements = g["frame_elements"]() + g["core_elements"](False) + g["front_panel"](CORE, False) + g["side_panels"](False) + g["worktop_elements"](CORE, False)
    item = model(f"{BLOCK}_item", elements, {"frame": f"{MOD}:block/iron_chassis", "floor": f"{MOD}:block/chassis_floor",
                 **core_tex(""), "panel": g["finish_tex"]("oak", "panel"), "worktop": g["finish_tex"]("oak", "worktop")})
    jdump({"model": {"type": "minecraft:model", "model": item}}, f"{A}/items/{BLOCK}.json")
    jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:block/{CORE}_core"}}, f"{A}/items/{CORE}_assembly.json")
    keep = [f"{MOD}:energy", f"{MOD}:worktop", f"{MOD}:upgrades"] + ([f"{MOD}:ender_link"] if CORE in LINKED else [])
    jdump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
           "conditions": [{"condition": "minecraft:survives_explosion"}],
           "entries": [{"type": "minecraft:item", "name": f"{MOD}:{BLOCK}", "functions": [
               {"function": "minecraft:copy_state", "block": f"{MOD}:{BLOCK}", "properties": ["tier", "panel"]},
               {"function": "minecraft:copy_components", "source": "block_entity", "include": keep}]}]}],
           "random_sequence": f"{MOD}:blocks/{BLOCK}"}, f"{D}/{MOD}/loot_table/blocks/{BLOCK}.json")
    p = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    tag = json.load(open(p))
    if f"{MOD}:{BLOCK}" not in tag["values"]:
        tag["values"].append(f"{MOD}:{BLOCK}")
    jdump(tag, p)

def main():
    ti = A + "/textures/item/"
    for it in ["ender_ingot", "ender_wire", "ender_coil", "link_card", "ender_backpack"]:
        shutil.copy(PLAN + it + ".png", ti + it + ".png")
        jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{it}"}}, f"{A}/models/item/{it}.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{it}"}}, f"{A}/items/{it}.json")
    for core, block in CORES.items():
        device(core, block)
    lang_update()
    recipes()
    g["recipe_unlocks"]()

if __name__ == "__main__":
    main()

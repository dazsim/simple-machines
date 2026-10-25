# Tool Stand, tool parts and the 3D drill and chainsaw item models
import json, math, os, random
from PIL import Image
import powered_icons

MOD = "simplemachines"
UPS = ["overclocker_circuit", "energy_cell"]
COMBOS = [""] + [",".join(c) for c in [[UPS[0]], [UPS[1]], [UPS[0]] * 2, UPS, [UPS[1]] * 2]]
BITS = {"frame": 1, "core": 2, "board": 4, "battery": 8, "upgrades": 16, "tip": 32}
SPIN = 6

def tex(base, seed, k=6, f=None):
    rnd = random.Random(seed)
    t = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = f(x, y) if f else None
            c = c or base
            d = rnd.randint(-k, k)
            t.putpixel((x, y), tuple(max(0, min(255, v + d)) for v in c) + (255,))
    return t

def anim(base, seed, k, f, n):
    """n stacked 16x16 frames of tex(); f gets the frame index too"""
    t = Image.new("RGBA", (16, 16 * n))
    for i in range(n):
        t.paste(tex(base, seed, k, lambda x, y: f(x, y, i)), (0, 16 * i))
    return t

def textures(save, A):
    iron, steel = [(200, 200, 200), (170, 170, 172), (120, 120, 126)], [(150, 158, 174), (108, 114, 128), (70, 74, 88)]
    def plate(p):
        return lambda x, y: p[0] if x == 0 or y == 0 else p[2] if x == 15 or y == 15 else p[0] if (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)] else None
    t = {
        "tool_iron": tex(iron[1], 1, 5, plate(iron)),
        "tool_steel": tex(steel[1], 2, 4, plate(steel)),
        "tool_grip": tex((52, 48, 46), 3, 4, lambda x, y: (34, 32, 32) if y % 3 == 0 else None),
        "tool_motor": tex((232, 182, 40), 4, 6, lambda x, y: (70, 56, 24) if y in (5, 9) and 3 <= x <= 12 and x % 2 else (255, 224, 120) if y == 0 else (150, 110, 24) if y == 15 else None),
        "tool_bitsteel": tex((92, 96, 108), 15, 4, lambda x, y: (130, 136, 150) if y == 0 or x == 0 else (60, 62, 72) if y == 15 else None),
        "tool_panel": tex((44, 46, 54), 16, 3, lambda x, y: (90, 94, 106) if x in (0, 15) or y in (0, 15) else None),
        "tool_engine": tex((200, 60, 52), 5, 6, lambda x, y: (244, 120, 92) if y in (0, 1) else (90, 30, 30) if y == 15 else None),
        "tool_chuck": tex((70, 72, 80), 6, 5, lambda x, y: (120, 124, 134) if x % 4 == 0 else None),
        "tool_bit": tex((186, 190, 200), 7, 4, lambda x, y: (110, 114, 126) if (x + y) % 4 < 2 else None),
        "tool_bar": tex((180, 184, 192), 8, 4, lambda x, y: ((40, 40, 46) if x % 2 == 0 else (90, 90, 96)) if y < 3 or y > 12 else (140, 144, 152) if y in (3, 12) else None),
        "tool_circuit": tex((40, 110, 60), 9, 6, lambda x, y: (30, 30, 34) if 4 <= x <= 9 and 5 <= y <= 10 else (220, 180, 70) if (x + y) % 5 == 0 else None),
        "tool_battery": tex((54, 58, 66), 10, 4, lambda x, y: (200, 60, 52) if y in (6, 7) else (120, 230, 140) if y == 11 and 11 <= x <= 13 else None),
        "tool_cell": tex((60, 64, 74), 11, 4, lambda x, y: (90, 180, 230) if y in (6, 7) else (150, 230, 255) if y == 11 and 2 <= x <= 4 else None),
        "tool_fins": tex((214, 218, 226), 12, 3, lambda x, y: (120, 126, 140) if x % 3 == 0 else None),
        "tool_stand": tex((86, 90, 100), 13, 4, plate([(120, 124, 136), (86, 90, 100), (50, 52, 60)])),
        "tool_stand_top": tex((60, 56, 52), 14, 4, lambda x, y: (40, 38, 36) if y % 4 == 0 else None),
    }
    # running variants: the bit's flutes turn and the chain runs round the bar
    t["tool_bit_spin"] = anim((186, 190, 200), 7, 4, lambda x, y, i: (110, 114, 126) if (x + y + i) % 4 < 2 else (230, 232, 238) if (x + y + i) % 4 == 2 and y % 8 == 0 else None, 4)
    def chain(x, y, i):
        if y < 3: return (40, 40, 46) if (x + i) % 4 < 2 else (96, 96, 104)
        if y > 12: return (40, 40, 46) if (x - i) % 4 < 2 else (96, 96, 104)
        return (140, 144, 152) if y in (3, 12) else None
    t["tool_bar_run"] = anim((180, 184, 192), 8, 4, chain, 4)
    def dchain(x, y, i):
        if y < 3 or y > 12:
            k = (x + i) % 4 if y < 3 else (x - i) % 4
            return (110, 230, 240) if k == 0 and y in (0, 15) else (40, 40, 46) if k < 2 else (96, 96, 104)
        return (140, 144, 152) if y in (3, 12) else None
    t["tool_bar_diamond"] = tex((180, 184, 192), 8, 4, lambda x, y: dchain(x, y, 0))
    t["tool_bar_diamond_run"] = anim((180, 184, 192), 8, 4, dchain, 4)
    def thread(x, y, i):
        return (70, 72, 80) if (x + y // 4 + i) % 3 == 0 else (200, 204, 212) if (x + y // 4 + i) % 3 == 1 else None
    t["tool_thread"] = tex((150, 154, 164), 17, 3, lambda x, y: thread(x, y, 0))
    t["tool_thread_spin"] = anim((150, 154, 164), 17, 3, thread, 3)
    # cones: rows of carbide or diamond teeth that roll while the bit turns
    for kind, tooth, glint in (("std", (196, 198, 206), (240, 240, 246)), ("diamond", (90, 220, 236), (210, 255, 255))):
        def cone(x, y, i, tooth=tooth, glint=glint):
            yy = (y + i) % 16
            if yy % 4 == 3: return (36, 38, 44)
            if x % 4 in (1, 2) and yy % 4 in (1, 2): return glint if (x % 4, yy % 4) == (1, 1) else tooth
            return None
        t[f"tool_cone_{kind}"] = tex((74, 76, 84), 18, 4, lambda x, y, cone=cone: cone(x, y, 0))
        t[f"tool_cone_{kind}_spin"] = anim((74, 76, 84), 18, 4, cone, 4)
    t["tool_fins_hot"] = tex((255, 150, 60), 12, 6, lambda x, y: (255, 230, 150) if x % 3 == 1 else (200, 60, 20) if x % 3 == 0 else None)
    spark = Image.new("RGBA", (8, 8))
    for y in range(8):
        for x in range(8):
            d = abs(x - 3.5) + abs(y - 3.5)
            if d < 1.5: spark.putpixel((x, y), (255, 255, 255, 255))
            elif d < 2.5: spark.putpixel((x, y), (225, 225, 225, 255))
    save(spark, f"{A}/textures/particle/hot_spark.png")
    os.makedirs(f"{A}/particles", exist_ok=True)
    open(f"{A}/particles/hot_spark.json", "w").write(json.dumps({"textures": [f"{MOD}:hot_spark"]}, indent=2) + "\n")
    open(f"{A}/particles/white_spark.json", "w").write(json.dumps({"textures": [f"{MOD}:hot_spark"]}, indent=2) + "\n")
    for n in ("tool_bit_spin", "tool_bar_run", "tool_bar_diamond_run", "tool_thread_spin", "tool_cone_std_spin", "tool_cone_diamond_spin"):
        open(f"{A}/textures/item/{n}.png.mcmeta", "w").write(json.dumps({"animation": {"frametime": 1}}, indent=2) + "\n")
    for n, img in t.items():
        save(img, f"{A}/textures/item/{n}.png" if n != "tool_stand" and n != "tool_stand_top" else f"{A}/textures/block/{n}.png")

def box(a, b, t, skip=()):
    return {"from": list(a), "to": list(b), "faces": {d: {"texture": "#" + t} for d in ["north", "south", "east", "west", "up", "down"] if d not in skip}}

def fbox(a, b, t, **kw):
    """box with every face showing the whole texture"""
    e = {"from": list(a), "to": list(b), "faces": {d: {"texture": "#" + t, "uv": [0, 0, 16, 16]} for d in ["north", "south", "east", "west", "up", "down"]}}
    e.update(kw)
    return e

def rx(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0], [0, c, -s], [0, s, c]]

def rz(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]

def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]

def euler(r):
    """ZYX euler angles in degrees for an element rotation (matrix = Rz Ry Rx)"""
    y = -math.asin(max(-1, min(1, r[2][0])))
    return {"x": round(math.degrees(math.atan2(r[2][1], r[2][2])), 3), "y": round(math.degrees(y), 3), "z": round(math.degrees(math.atan2(r[1][0], r[0][0])), 3)}

AXIS = (9.75, 8)  # y, z of the drill axis

def tricone(kind, frame, running):
    """legs and three toothed roller cones at the drill tip, turned frame * 20 degrees about the axis"""
    cy, cz = AXIS
    cone_t = f"{MOD}:item/tool_cone_{kind}" + ("_spin" if running else "")
    els = []
    for i in range(3):
        beta = math.radians(120 * i + 20 * frame)
        leg = fbox((16.25, cy + 1.1, cz - 0.85), (18.75, cy + 2.6, cz + 0.85), "leg")
        leg["rotation"] = {"origin": [16.25, cy, cz], "x": round(math.degrees(beta), 3), "y": 0, "z": 0}
        els.append(leg)
        m = mul(rx(beta), rz(-math.radians(32)))
        bx, by, bz = 18.0, cy + 1.45 * math.cos(beta), cz + 1.45 * math.sin(beta)
        for x0, x1, h in ((0, 1.2, 2.6), (1.2, 2.2, 2.0), (2.2, 3.0, 1.3), (3.0, 3.5, 0.6)):
            e = fbox((bx + x0, by - h / 2, bz - h / 2), (bx + x1, by + h / 2, bz + h / 2), "cone")
            e["rotation"] = dict({"origin": [round(bx, 4), round(by, 4), round(bz, 4)]}, **euler(m))
            els.append(e)
    for e in els:
        for k in ("from", "to"):
            e[k] = [round(v, 4) for v in e[k]]
    return els, {"cone": cone_t, "leg": f"{MOD}:item/tool_bitsteel"}

def drill_core(running):
    cy, cz = AXIS
    return ([box((1.5, 7.5, 6.5), (10, 12, 9.5), "motor"), box((10, 8, 6.75), (12, 11.5, 9.25), "chuck"),
             fbox((12, cy - 1.25, cz - 1.25), (14.5, cy + 1.25, cz + 1.25), "thread"),
             fbox((14.5, cy - 1.75, cz - 1.75), (15.25, cy + 1.75, cz + 1.75), "steel"),
             fbox((15.25, cy - 2, cz - 2), (17, cy + 2, cz + 2), "steel")],
            {"motor": f"{MOD}:item/tool_motor", "chuck": f"{MOD}:item/tool_chuck",
             "thread": f"{MOD}:item/tool_thread" + ("_spin" if running else ""), "steel": f"{MOD}:item/tool_bitsteel"})

def saw_core():
    return ([box((1.5, 7.5, 6.5), (10, 12.5, 9.5), "engine"),
             box((5, 12.5, 7.25), (6, 15, 8.75), "engine"), box((5, 14, 7.25), (11, 15, 8.75), "grip"),
             box((10, 12.5, 7.25), (11, 14, 8.75), "engine")],
            {"engine": f"{MOD}:item/tool_engine", "grip": f"{MOD}:item/tool_grip"})

def saw_bar(tex_name):
    return [box((10, 8, 7.6), (22, 11, 8.4), "bar")], {"bar": f"{MOD}:item/{tex_name}"}

# the tool lies along X, nose at +X, grip down; both tools share the frame, battery, board and upgrade spots
PARTS = {
    "frame": lambda m: ([box((3, 1, 6.5), (6, 8, 9.5), "grip"),
                         box((1.5, 7, 5.75), (10.5, 10, 6.5), m), box((1.5, 7, 9.5), (10.5, 10, 10.25), m),
                         box((1, 6.5, 6.5), (1.5, 10.5, 9.5), m), box((6, 2.5, 7), (8, 3.5, 9), m)],
                        {"grip": f"{MOD}:item/tool_grip", m: f"{MOD}:item/tool_{m}"}),
    "board": lambda: ([box((2.75, 9.9, 9.5), (8.25, 11.8, 9.8), "c"),
                       box((2.25, 9.6, 9.8), (2.75, 12, 10.05), "p"), box((8.25, 9.6, 9.8), (8.75, 12, 10.05), "p"),
                       box((2.75, 11.7, 9.8), (8.25, 12, 10.05), "p"), box((4.5, 9.9, 9.8), (5, 11.7, 9.95), "p")],
                      {"c": f"{MOD}:item/tool_circuit", "p": f"{MOD}:item/tool_panel"}),
    "battery": lambda: ([box((2.5, -1, 6), (7, 1, 10), "b")], {"b": f"{MOD}:item/tool_battery"}),
    "cell": lambda n: ([box((2.5, -1 - 1.5 * n, 6.25), (7, -1, 9.75), "c")], {"c": f"{MOD}:item/tool_cell"}),
    "fins": lambda n, hot=False: ([dict(box((2 + i, 12, 6.75), (2.5 + i, 13.5 + 0.5 * n, 9.25), "f"), **({"light_emission": 15} if hot else {}))
                                   for i in [0, 1.25, 2.5] + ([3.75] if n > 1 else [])],
                                  {"f": f"{MOD}:item/tool_fins" + ("_hot" if hot else "")}),
}

DISPLAY = {
    "gui": {"rotation": [20, -30, 0], "translation": [0, 2, 0], "scale": [0.7, 0.7, 0.7]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_righthand": {"rotation": [90, -90, 0], "translation": [0, 2, 1.5], "scale": [0.7, 0.7, 0.7]},
    "thirdperson_lefthand": {"rotation": [90, -90, 0], "translation": [0, 2, 1.5], "scale": [0.7, 0.7, 0.7]},
    "firstperson_righthand": {"rotation": [0, 80, 5], "translation": [1, 2, -1], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, -80, -5], "translation": [1, 2, -1], "scale": [0.4, 0.4, 0.4]},
}

def m(name):
    return {"type": "minecraft:model", "model": f"{MOD}:item/tool/{name}"}

EMPTY = {"type": "minecraft:empty"}

def gated(bit, inner):
    without = [v for v in range(64) if not v & bit]
    return {"type": "minecraft:select", "property": "minecraft:component", "component": f"{MOD}:tool_parts",
            "cases": [{"when": without, "model": EMPTY}], "fallback": inner}

def by_upgrades(name, up, hot=False):
    cases = []
    for n in (1, 2):
        w = [c for c in COMBOS if c.split(",").count(up) == n]
        inner = m(f"{name}_{n}")
        if hot: inner = {"type": "minecraft:condition", "property": f"{MOD}:tool_running", "on_true": m(f"{name}_{n}_hot"), "on_false": inner}
        cases.append({"when": w, "model": inner})
    return {"type": "minecraft:select", "property": "minecraft:component", "component": f"{MOD}:upgrades",
            "cases": cases, "fallback": EMPTY}

def running(core):
    return {"type": "minecraft:condition", "property": f"{MOD}:tool_running",
            "on_true": m(core + "_running"), "on_false": m(core)}

def spin(kind):
    return {"type": "minecraft:range_dispatch", "property": f"{MOD}:spin",
            "entries": [{"threshold": f, "model": m(f"bit_{kind}_run_{f}")} for f in range(SPIN)], "fallback": m(f"bit_{kind}_run_0")}

def tip_model(saw):
    if saw:
        std = running_pair("bar_std")
        dia = running_pair("bar_diamond")
    else:
        std = {"type": "minecraft:condition", "property": f"{MOD}:tool_running", "on_true": spin("std"), "on_false": m("bit_std")}
        dia = {"type": "minecraft:condition", "property": f"{MOD}:tool_running", "on_true": spin("diamond"), "on_false": m("bit_diamond")}
    return {"type": "minecraft:select", "property": "minecraft:component", "component": f"{MOD}:tool_tip",
            "cases": [{"when": "diamond", "model": dia}, {"when": "none", "model": EMPTY}], "fallback": std}

def running_pair(name):
    return {"type": "minecraft:condition", "property": f"{MOD}:tool_running", "on_true": m(name + "_running"), "on_false": m(name)}

def tool_model(core):
    frame = {"type": "minecraft:select", "property": "minecraft:component", "component": f"{MOD}:tool_frame",
             "cases": [{"when": "steel", "model": m("frame_steel")}], "fallback": m("frame_iron")}
    without_tip = [v for v in range(64) if not v & 34]
    tip = {"type": "minecraft:select", "property": "minecraft:component", "component": f"{MOD}:tool_parts",
           "cases": [{"when": without_tip, "model": EMPTY}], "fallback": tip_model(core == "chainsaw_core")}
    return {"model": {"type": "minecraft:composite", "models": [
        gated(1, frame), gated(2, running(core)), tip, gated(4, m("board")), gated(8, m("battery")),
        gated(16, by_upgrades("cell", UPS[1])), gated(16, by_upgrades("fins", UPS[0], True))]}}

LANG = {
    "block.simplemachines.tool_stand": "Tool Stand",
    "item.simplemachines.iron_tool_frame": "Iron Tool Frame",
    "item.simplemachines.steel_tool_frame": "Steel Tool Frame",
    "item.simplemachines.drill_motor": "Drill Motor",
    "item.simplemachines.chainsaw_bar": "Chainsaw Bar",
    "item.simplemachines.battery_module": "Battery Module",
    "tooltip.simplemachines.stored": "Stored: %s FE",
    "tooltip.simplemachines.tool_part": "Tool part: build tools on a Tool Stand",
    "tooltip.simplemachines.iron_frame": "Iron frame",
    "tooltip.simplemachines.steel_frame": "Steel frame: digs 25% faster",
    "item.simplemachines.diamond_drill_bit": "Diamond Drill Bit",
    "item.simplemachines.diamond_tipped_chain": "Diamond-Tipped Chain",
    "tooltip.simplemachines.diamond_bit_part": "Drill bit: mines obsidian and netherite level blocks",
    "tooltip.simplemachines.diamond_chain_part": "Chainsaw chain: a steel chainsaw with it fells whole trees",
    "tooltip.simplemachines.standard_bit": "Standard bit: up to diamond ore",
    "tooltip.simplemachines.diamond_bit": "Diamond bit: mines obsidian and netherite level blocks",
    "tooltip.simplemachines.standard_chain": "Standard chain",
    "tooltip.simplemachines.diamond_chain": "Diamond-tipped chain",
    "tooltip.simplemachines.hard_slow": "Iron frame: obsidian level blocks at 40% speed",
    "tooltip.simplemachines.fells_trees": "Fells whole trees (sneak for one log)",
    "tooltip.simplemachines.upgrade_speed": "Upgrades: +%s%% speed (Overclocker +50%%, Energy Cell +15%%, max +100%%)",
    "message.simplemachines.wrong_tip": "That tip does not fit this tool",
    "message.simplemachines.tip_fitted": "A diamond tip is already fitted",
    "message.simplemachines.tip_screwed": "Diamond tip fitted",
    "message.simplemachines.needs_tool_frame": "Needs a tool frame",
    "message.simplemachines.needs_tool_core": "Needs a drill motor or chainsaw bar",
    "message.simplemachines.needs_battery_module": "Needs a battery module",
    "message.simplemachines.tool_ready": "Tool ready: take it off with an empty hand",
    "message.simplemachines.tool_unfinished": "The tool is not finished yet",
    "message.simplemachines.stand_busy": "The stand already holds a tool",
    "message.simplemachines.tool_upgrade_early": "Finish the tool before fitting upgrades",
}

def main(A, D, save, jdump, shaped, flat_item, loot_self):
    lang = json.load(open(f"{A}/lang/en_us.json"))
    lang.update(LANG)
    jdump(lang, f"{A}/lang/en_us.json")
    textures(save, A)
    jdump({"parent": "minecraft:item/handheld", "gui_light": "side", "display": DISPLAY}, f"{A}/models/item/tool/base.json")
    def part(name, built):
        els, tx = built
        tx = dict(tx, particle=list(tx.values())[0])
        jdump({"parent": f"{MOD}:item/tool/base", "textures": tx, "elements": els}, f"{A}/models/item/tool/{name}.json")
    part("frame_iron", PARTS["frame"]("iron"))
    part("frame_steel", PARTS["frame"]("steel"))
    part("drill_core", drill_core(False))
    part("drill_core_running", drill_core(True))
    part("chainsaw_core", saw_core())
    part("chainsaw_core_running", saw_core())
    for kind in ("std", "diamond"):
        part(f"bit_{kind}", tricone(kind, 0, False))
        for f in range(SPIN):
            part(f"bit_{kind}_run_{f}", tricone(kind, f, True))
    part("bar_std", saw_bar("tool_bar"))
    part("bar_std_running", saw_bar("tool_bar_run"))
    part("bar_diamond", saw_bar("tool_bar_diamond"))
    part("bar_diamond_running", saw_bar("tool_bar_diamond_run"))
    part("board", PARTS["board"]())
    part("battery", PARTS["battery"]())
    for n in (1, 2):
        part(f"cell_{n}", PARTS["cell"](n))
        part(f"fins_{n}", PARTS["fins"](n))
        part(f"fins_{n}_hot", PARTS["fins"](n, True))
    jdump(tool_model("drill_core"), f"{A}/items/electric_drill.json")
    jdump(tool_model("chainsaw_core"), f"{A}/items/chainsaw.json")

    # part items keep flat icons
    for name, img in [("iron_tool_frame", powered_icons.tool_frame(False)), ("steel_tool_frame", powered_icons.tool_frame(True)),
                      ("drill_motor", powered_icons.drill_motor()), ("chainsaw_bar", powered_icons.chainsaw_bar()),
                      ("battery_module", powered_icons.battery_module()),
                      ("diamond_drill_bit", powered_icons.diamond_drill_bit()), ("diamond_tipped_chain", powered_icons.diamond_tipped_chain())]:
        flat_item(name, img)

    # stand: anvil sized, a wide foot, a waist and a long cradle the tool sits on
    st, top = f"{MOD}:block/tool_stand", f"{MOD}:block/tool_stand_top"
    jdump({"parent": "minecraft:block/block", "textures": {"particle": st, "s": st, "t": top}, "elements": [
        box((2, 0, 2), (14, 4, 14), "s"), box((4, 4, 3), (12, 5, 13), "s"), box((6, 5, 4), (10, 10, 12), "s"),
        box((1, 10, 3), (15, 12, 13), "s"), box((0, 12, 3), (16, 14, 13), "t")]}, f"{A}/models/block/tool_stand.json")
    jdump({"variants": {f"facing={f}": ({"model": st, "y": r} if r else {"model": st})
                        for f, r in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]}}, f"{A}/blockstates/tool_stand.json")
    jdump({"model": {"type": "minecraft:model", "model": st}}, f"{A}/items/tool_stand.json")
    loot_self("tool_stand")

    S, R, I = "#c:plates/steel", "#c:rods/steel", "#c:ingots/iron"
    shaped("iron_tool_frame", ["II ", "IRI", " RI"], {"I": I, "R": "#c:rods/iron"})
    shaped("steel_tool_frame", ["SS ", "SRS", " RS"], {"S": S, "R": R})
    shaped("drill_motor", [" C ", "RMR", " T "], {"C": f"{MOD}:redstone_copper_ingot", "R": "#c:rods/copper", "M": "minecraft:piston", "T": R})
    shaped("chainsaw_bar", ["  T", " TC", "SR "], {"T": S, "C": "minecraft:iron_bars", "S": f"{MOD}:redstone_copper_ingot", "R": "minecraft:piston"})
    shaped("battery_module", ["CPC"], {"C": "#c:ingots/copper", "P": f"{MOD}:power_cell"})
    shaped("tool_stand", ["SSS", " R ", "BBB"], {"R": R, "S": "minecraft:smooth_stone_slab", "B": "minecraft:smooth_stone"})
    shaped("diamond_drill_bit", [" DS", "DSD", "TD "], {"D": "minecraft:diamond", "S": S, "T": R})
    shaped("diamond_tipped_chain", ["DCD", "C C", "DCD"], {"D": "minecraft:diamond", "C": "minecraft:iron_chain"})
    tag = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
    vals = json.load(open(tag))["values"]
    jdump({"values": vals + [f"{MOD}:tool_stand"]}, tag)

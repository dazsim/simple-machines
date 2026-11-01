#!/usr/bin/env python3
# Entangled parts, the programmer and the linked devices (2.2.0). Run from the repo root after gen_assets.py: python3 tools/gen_entangled.py
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
CORES = {"entangled_programmer": "entangled_programmer", "entangled_buffer": "entangled_buffer", "entangled_tank": "entangled_tank",
         "entangled_relay_lv": "entangled_relay_lv", "entangled_relay_mv": "entangled_relay_mv", "entangled_relay_hv": "entangled_relay_hv",
         "entangled_pad": "entangled_pad"}
LINKED = ("entangled_buffer", "entangled_tank", "entangled_relay_lv", "entangled_relay_mv", "entangled_relay_hv", "entangled_pad")
TIERS = {"lv": (0xF4, 0x78, 0x5C), "mv": (0xE8, 0xBE, 0x28), "hv": (0x96, 0xE4, 0xF0)}
g["FRONT_HOLE"].update({"entangled_programmer": (3, 3, 13, 13), "entangled_buffer": (3, 3, 13, 13), "entangled_tank": (4, 2, 12, 14),
                        **{f"entangled_relay_{t}": (4, 3, 12, 13) for t in TIERS}, "entangled_pad": (3, 3, 13, 13)})
# the pad shows through the worktop, so you can see where to jump
g["TOP_HOLE"]["entangled_pad"] = (3, 3, 13, 13)

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
    t = g["core_top"]("entangled_programmer")
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
    t = panel(176, 212)
    for r in range(3):
        for c in range(9):
            inset(t, 7 + c * 18, 17 + r * 18, 24 + c * 18, 34 + r * 18)
    # the three module slots show what is fitted, they are filled at the worktop
    for i in range(3):
        inset(t, 114 + i * 18, 105, 131 + i * 18, 122)
    inset(t, 7, 106, 78, 113)
    inventory(t, 129)
    return t

def tank_gui():
    t = panel(176, 180)
    inset(t, 39, 19, 56, 36)
    inset(t, 39, 43, 56, 60)
    inset(t, 79, 16, 96, 69)
    inset(t, 152, 16, 167, 69)
    inventory(t, 97)
    return t

def relay_front(t, lit):
    # the plan's tier pip sits behind the front panel, so the tier also runs along the bottom of the window
    im = Image.open(PLAN + f"entangled_relay_{t}_core_front" + ("_on" if lit else "") + ".png").convert("RGBA")
    c = TIERS[t] if lit else tuple(x * 11 // 20 for x in TIERS[t])
    for u in range(5, 11):
        px(im, u, 12, c + (255,))
    return im

def relay_gui():
    t = panel(176, 106)
    inset(t, 152, 16, 167, 69)
    return t

def pad_gui():
    t = panel(176, 104)
    inset(t, 152, 16, 167, 69)
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

def backpack_worn():
    # chest slot layer on the 64x32 humanoid sheet: the pack on the back of the body, straps on the front and shoulders
    t = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    EDGE, DEEP, MID, HI, LIGHT = (61, 28, 16, 255), (84, 39, 22, 255), (158, 73, 42, 255), (198, 92, 53, 255), (215, 107, 67, 255)
    STEEL, STEEL_D = (137, 147, 176, 255), (110, 114, 130, 255)
    rect(t, 32, 20, 39, 31, EDGE); rect(t, 33, 21, 38, 30, MID)
    rect(t, 33, 21, 38, 21, STEEL); rect(t, 33, 22, 38, 22, STEEL_D)
    rect(t, 33, 23, 38, 23, LIGHT); rect(t, 33, 24, 38, 24, HI)
    rect(t, 34, 25, 37, 28, STEEL); rect(t, 35, 26, 36, 26, (44, 205, 177, 255)); rect(t, 35, 27, 36, 27, (16, 94, 81, 255))
    rect(t, 33, 30, 38, 30, DEEP); px(t, 37, 29, (80, 220, 90, 255))
    for x in (21, 26):
        rect(t, x, 16, x, 31, EDGE)
    px(t, 21, 26, STEEL); px(t, 26, 26, STEEL)
    for x in (16, 17, 30, 31):
        rect(t, x, 20, x, 31, DEEP)
    save(t, A + "/textures/entity/equipment/humanoid/entangled_backpack.png")
    jdump({"layers": {"humanoid": [{"texture": f"{MOD}:entangled_backpack"}]}}, f"{A}/equipment/entangled_backpack.json")

def lang_update():
    p = f"{A}/lang/en_us.json"
    lang = json.load(open(p))
    lang.update({
        "item.simplemachines.entangled_ingot": "Entangled Ingot",
        "item.simplemachines.entangled_wire": "Entangled Wire",
        "item.simplemachines.entangled_coil": "Entangled Coil",
        "item.simplemachines.link_card": "Link Card",
        "item.simplemachines.entangled_programmer_assembly": "Entangled Programmer Assembly",
        "block.simplemachines.entangled_programmer": "Entangled Programmer",
        "container.simplemachines.entangled_programmer": "Entangled Programmer",
        "gui.simplemachines.entangled_code": "Code",
        "gui.simplemachines.entangled_random": "Random",
        "gui.simplemachines.entangled_write": "Write",
        "gui.simplemachines.entangled_private": "Private",
        "gui.simplemachines.entangled_public": "Public",
        "gui.simplemachines.entangled_private_hint": "Private codes belong to you alone and never clash with anyone else's",
        "gui.simplemachines.entangled_wrote": "Wrote %s to the card.",
        "gui.simplemachines.entangled_no_card": "Put a Link Card in the slot.",
        "gui.simplemachines.entangled_no_power": "Not enough power.",
        "gui.simplemachines.entangled_not_owner": "That card holds someone else's code.",
        "gui.simplemachines.entangled_no_code": "Type a code first.",
        "gui.simplemachines.entangled_cost": "%s FE per write",
        "tooltip.simplemachines.entangled_code": "Code %s, %s",
        "tooltip.simplemachines.entangled_public": "public",
        "tooltip.simplemachines.entangled_private": "private",
        "tooltip.simplemachines.link_card_blank": "Blank: write a code in an Entangled Programmer",
        "tooltip.simplemachines.link_card": "Right-click an entangled device to set its code, sneak to copy it",
        "tooltip.simplemachines.uses_write": "Uses %s FE per write",
        "message.simplemachines.entangled_set": "Code set to %s",
        "message.simplemachines.entangled_copied": "Copied code %s to the card",
        "message.simplemachines.entangled_not_owner": "This code belongs to someone else",
        "message.simplemachines.entangled_now_private": "Code %s is now private",
        "message.simplemachines.entangled_now_public": "Code %s is now public",
        "item.simplemachines.entangled_buffer_assembly": "Entangled Buffer Assembly",
        "item.simplemachines.entangled_tank_assembly": "Entangled Tank Assembly",
        "item.simplemachines.entangled_backpack": "Entangled Backpack",
        "block.simplemachines.entangled_buffer": "Entangled Buffer",
        "block.simplemachines.entangled_tank": "Entangled Tank",
        "container.simplemachines.entangled_buffer": "Entangled Buffer",
        "container.simplemachines.entangled_tank": "Entangled Tank",
        "container.simplemachines.entangled_backpack": "Backpack",
        "gui.simplemachines.entangled_no_card": "Put a Link Card or Backpack in the slot.",
        "gui.simplemachines.entangled_not_owner": "That holds someone else's code.",
        "gui.simplemachines.entangled_wrote": "Wrote %s.",
        "gui.simplemachines.entangled_code_label": "Code %s",
        "gui.simplemachines.entangled_no_link": "No code: right-click with a Link Card",
        "gui.simplemachines.entangled_offline": "No power: not sharing",
        "gui.simplemachines.entangled_owner_public": "Public. Owner: %s",
        "gui.simplemachines.entangled_owner_private": "Private. Owner: %s",
        "gui.simplemachines.entangled_someone": "someone else",
        "gui.simplemachines.entangled_fe": "%s / %s FE",
        "gui.simplemachines.entangled_tank_empty": "Empty",
        "gui.simplemachines.entangled_tank_amount": "%s / %s B",
        "tooltip.simplemachines.backpack_blank": "Blank: write a code in an Entangled Programmer",
        "tooltip.simplemachines.backpack": "Right-click to open the Entangled Buffer on its code",
        "tooltip.simplemachines.uses_open": "Uses %s FE per open",
        "message.simplemachines.backpack_no_code": "This backpack has no code yet",
        "message.simplemachines.backpack_no_power": "Not enough charge: opening takes %s FE and it has %s FE. Charge it in a Charger",
        "message.simplemachines.backpack_not_worn": "Wear an Entangled Backpack in your chest slot to open it with this key",
        "key.simplemachines.open_backpack": "Open Entangled Backpack",
        "tooltip.simplemachines.backpack_worn": "Or wear it in the chest slot and press %s",
        **{k: v for t in ("LV", "MV", "HV") for k, v in {
            f"block.simplemachines.entangled_relay_{t.lower()}": f"Entangled Relay {t}",
            f"item.simplemachines.entangled_relay_{t.lower()}_assembly": f"Entangled Relay {t} Assembly",
        }.items()},
        "jei.simplemachines.info.entangled_relay_lv": "LV relay. Sends up to 1,000 FE per tick to every relay on its code, in any dimension, losing 10% on the way. Higher voltage on its input blows it.",
        "jei.simplemachines.info.entangled_relay_mv": "MV relay. Sends up to 8,000 FE per tick to every relay on its code, in any dimension, losing 10% on the way. Higher voltage on its input blows it.",
        "jei.simplemachines.info.entangled_relay_hv": "HV relay. Sends up to 64,000 FE per tick to every relay on its code, in any dimension, losing 10% on the way.",
        "gui.simplemachines.relay_send": "Send",
        "gui.simplemachines.relay_receive": "Receive",
        "gui.simplemachines.relay_sender": "%s sender",
        "gui.simplemachines.relay_senders": "%s senders",
        "gui.simplemachines.relay_receiver": "%s receiver",
        "gui.simplemachines.relay_receivers": "%s receivers",
        "gui.simplemachines.relay_in": "In %s FE/t",
        "gui.simplemachines.relay_in_after": "In %s FE/t (after 10%%)",
        "gui.simplemachines.relay_out": "Out %s FE/t",
        "tooltip.simplemachines.relay_sends": "Sends %s on the receive side, 10%% loss",
        "tooltip.simplemachines.entangled_private_cables": "Private: only your Simple Machines cables connect",
        "block.simplemachines.entangled_pad": "Entangled Pad",
        "item.simplemachines.entangled_pad_assembly": "Entangled Pad Assembly",
        "jei.simplemachines.info.entangled_pad": "Give two pads the same code with a Link Card, then jump on one to teleport to the other, in any dimension. "
            "The pad you jump from pays 2,000 FE plus 20 FE per block of x and z distance, at most 50,000 FE. Two pads per code. "
            "A private pad only teleports its owner; a public one teleports anyone. Higher voltage than MV on its input blows it.",
        "tooltip.simplemachines.uses_teleport": "Uses %s FE + %s FE per block per teleport (max %s)",
        "tooltip.simplemachines.pad_no_code": "No code yet: right-click with a Link Card",
        "tooltip.simplemachines.pad_private": "Private: only you can teleport, only your Simple Machines cables connect",
        "tooltip.simplemachines.pad_public": "Public: anyone can teleport, any cable connects",
        "tooltip.simplemachines.pad_assembly": "Build into a chassis to make an Entangled Pad",
        "tooltip.simplemachines.pad_assembly_codes": "Two pads on one code: jump on one to teleport to the other",
        "tooltip.simplemachines.pad_assembly_private": "Private code: only you can teleport. Public code: anyone can",
        "gui.simplemachines.pad_linked": "Linked: %s",
        "gui.simplemachines.pad_dim_overworld": "Overworld",
        "gui.simplemachines.pad_dim_nether": "Nether",
        "gui.simplemachines.pad_dim_end": "The End",
        "gui.simplemachines.pad_dim_other": "another dimension",
        "gui.simplemachines.pad_trip": "Trip: %s FE",
        "gui.simplemachines.pad_ready": "Ready. Jump to go.",
        "gui.simplemachines.pad_alone": "No other pad yet",
        "gui.simplemachines.pad_full": "Code full: 2 pads",
        "gui.simplemachines.pad_low": "Needs %s FE to go",
        "message.simplemachines.pad_not_owner": "This pad is private: only its owner can teleport",
        "message.simplemachines.pad_no_code": "This pad has no code yet",
        "message.simplemachines.pad_alone": "No other pad on this code",
        "message.simplemachines.pad_full": "This code already has %s pads",
        "message.simplemachines.pad_low": "Not enough power: this trip needs %s FE",
        "message.simplemachines.pad_cooldown": "Wait a moment before teleporting again",
        "message.simplemachines.pad_gone": "The other pad is gone",
        "message.simplemachines.pad_blocked": "Something is blocking the other pad",
        "item.simplemachines.chunk_loader_module": "Chunk Loader Module",
        "item.simplemachines.range_module": "Range Module",
        "tooltip.simplemachines.chunk_loader_module": "Keeps the chunk it sits in loaded",
        "tooltip.simplemachines.range_module": "Widens a Chunk Loader by one chunk each way",
        "tooltip.simplemachines.chunk_loader_cost": "1x1 area: %s FE/t, plus %s FE/t device upkeep",
        "tooltip.simplemachines.range_module_cost": "3x3 area: %s FE/t, 5x5 with two: %s FE/t",
        "tooltip.simplemachines.module_cap": "Up to %s per player, set by the server",
        "tooltip.simplemachines.chunk_loader_module_fit": "Fit after the control board of an Entangled Buffer, Tank or Relay, one per device",
        "tooltip.simplemachines.range_module_fit": "Fit next to a Chunk Loader, two at most",
        "tooltip.simplemachines.modules": "Modules: %s",
        "tooltip.simplemachines.loader_area": "Loads %sx%s (%s) for %s FE/t",
        "tooltip.simplemachines.upgrade_fit": "Fit after a machine's control board, or on a finished tool on its stand, then screw in (2 slots, 3 on entangled devices)",
        "message.simplemachines.upgrades_full": "The upgrade slots are full",
        "message.simplemachines.no_overclock_entangled": "Entangled devices can't take an Overclocker",
        "message.simplemachines.module_no_loader": "This device can't load chunks",
        "message.simplemachines.module_one_loader": "One Chunk Loader per device",
        "message.simplemachines.module_needs_loader": "Fit a Chunk Loader first",
        "message.simplemachines.module_range_full": "Two Range modules at most",
        "gui.simplemachines.loader_none": "No Chunk Loader module",
        "gui.simplemachines.loader_off": "Loader off",
        "gui.simplemachines.loader_disabled": "Chunk loading is off on this server",
        "gui.simplemachines.loader_no_code": "Loader: needs a code",
        "gui.simplemachines.loader_active": "Loader %sx%s, %s FE/t",
        "gui.simplemachines.loader_away": "Loader paused: owner offline",
        "gui.simplemachines.loader_grace": "No power: unloads in %s s",
        "gui.simplemachines.loader_no_power": "Needs %s FE to restart",
        "gui.simplemachines.loader_over": "Over limit: %s max",
        "gui.simplemachines.loader_chunks": "%s, you use %s/%s",
        "gui.simplemachines.loader_chunks_uncapped": "%s, you use %s",
        "gui.simplemachines.chunk_count": "%s chunks",
        "gui.simplemachines.chunk_count_one": "%s chunk",
        "gui.simplemachines.loader_switch_on": "On",
        "gui.simplemachines.loader_switch_off": "Off",
        "gui.simplemachines.loader_switch_hint": "Turn the Chunk Loader on or off",
        "gui.simplemachines.module_slots_hint": "Module slots: fit a Chunk Loader and Range modules after the control board",
        "jei.simplemachines.info.chunk_loader_module": "Fit into an Entangled Buffer, Tank or Relay at the worktop, with a code set. "
            "It keeps its own chunk loaded for 32 FE/t, even with nobody near and the owner offline. "
            "Out of power it waits 10 s, then lets the chunks go. Each player can load 75 chunks; the server can change that.",
        "jei.simplemachines.info.range_module": "Fit next to a Chunk Loader. One makes it load 3x3 chunks for 288 FE/t, two make it 5x5 for 800 FE/t.",
        "jei.simplemachines.info.entangled_ingot": "Made in the Alloy Smelter from a Redstone Copper Ingot and an Ender Pearl. The Rolling Mill in rod mode turns it into 2 Entangled Wire.",
        "jei.simplemachines.info.entangled_wire": "Rolled from an Entangled Ingot in rod mode. Wound on a Steel Rod it makes an Entangled Coil, and it goes into Link Cards, the programmer, the backpack and the modules.",
        "jei.simplemachines.info.entangled_coil": "4 Entangled Wire wound on a Steel Rod. Every Entangled Buffer, Tank, Relay, Pad and Backpack and the Chunk Loader Module is built with it.",
        "jei.simplemachines.info.link_card": "Write a code onto it in an Entangled Programmer, then right-click an entangled device to give it that code. Sneak right-click copies a device's code onto the card. Cards are not used up.",
        "jei.simplemachines.info.entangled_programmer": "Writes codes onto Link Cards and Entangled Backpacks for 500 FE each. Type up to 6 digits or press Random. A private code belongs to you alone; a public code is shared by everyone.",
        "jei.simplemachines.info.entangled_buffer": "Every buffer on its code shares 27 slots, in any dimension. Uses 8 FE/t. A private buffer only connects to its owner's Simple Machines pipes and cables; hoppers and other mods' pipes need a public one.",
        "jei.simplemachines.info.entangled_tank": "Every tank on its code shares 16 buckets of one fluid, in any dimension. Uses 8 FE/t. A private tank only connects to its owner's Simple Machines pipes and cables; other mods' pipes need a public one.",
        "jei.simplemachines.info.entangled_backpack": "Write a code onto it in an Entangled Programmer, then right-click while holding it, or wear it in the chest slot and press the Open Entangled Backpack key (B), to open the Entangled Buffer on that code from anywhere. Each open uses 1,000 FE of its 50,000 FE; charge it in a Charger. It won't open without charge.",
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
           "result": {"id": f"{MOD}:entangled_ingot"}, "cooking_time": 400}, rec + "alloying/entangled_ingot.json")
    jdump({"type": f"{MOD}:rolling", "input": {"ingredient": f"{MOD}:entangled_ingot"},
           "result": {"id": f"{MOD}:entangled_wire", "count": 2}, "cooking_time": 100}, rec + "rolling/entangled_wire.json")
    W = f"{MOD}:entangled_wire"
    shaped("entangled_coil", [" W ", "WSW", " W "], {"W": W, "S": "#c:rods/steel"})
    shaped("link_card", ["PWP"], {"P": "#c:plates/iron", "W": W}, 2)
    shaped("entangled_programmer_assembly", ["RCR", "WIW"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "W": W, "I": f"{MOD}:entangled_ingot"})
    E = f"{MOD}:entangled_coil"
    shaped("entangled_buffer_assembly", ["RCR", "EXE"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "E": E, "X": "minecraft:chest"})
    shaped("entangled_tank_assembly", ["RCR", "EXE"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "E": E, "X": f"{MOD}:fluid_tank"})
    for t, cable in (("lv", "redstone_copper_cable"), ("mv", "gold_cable"), ("hv", "diamond_cable")):
        shaped(f"entangled_relay_{t}_assembly", ["RCR", "EXE"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "E": E, "X": f"{MOD}:{cable}"})
    shaped("entangled_pad_assembly", ["RCR", "EXE"], {"R": "#c:rods/iron", "C": f"{MOD}:redstone_copper_ingot", "E": E, "X": "minecraft:ender_eye"})
    P = "#c:plates/steel"
    shaped("chunk_loader_module", ["PCP", "WDW", "PBP"], {"P": P, "C": E, "W": W, "D": "#c:gems/diamond", "B": f"{MOD}:control_board"})
    shaped("range_module", ["PWP", "WEW", "PWP"], {"P": P, "W": W, "E": "#c:ender_pearls"})
    shaped("entangled_backpack", ["LWL", "LEL", "LPL"], {"L": "minecraft:leather", "W": W, "E": E, "P": f"{MOD}:power_cell"})

def device(CORE, BLOCK):
    tb = A + "/textures/block/"
    if CORE == "entangled_programmer":
        save(front(False), tb + f"{CORE}_core_front.png")
        save(front(True), tb + f"{CORE}_core_front_on.png")
        save(top(), tb + f"{CORE}_core_top.png")
        save(gui_tex(), A + f"/textures/gui/{BLOCK}.png")
    elif CORE.startswith("entangled_relay_"):
        tier = CORE.rsplit("_", 1)[1]
        save(relay_front(tier, False), tb + f"{CORE}_core_front.png")
        save(relay_front(tier, True), tb + f"{CORE}_core_front_on.png")
        shutil.copy(PLAN + "entangled_relay_core_top.png", tb + f"{CORE}_core_top.png")
        save(relay_gui(), A + "/textures/gui/entangled_relay.png")
    elif CORE == "entangled_pad":
        for suffix in ["front", "front_on", "top", "top_on"]:
            shutil.copy(PLAN + f"{BLOCK}_core_{suffix}.png", tb + f"{CORE}_core_{suffix}.png")
        save(pad_gui(), A + f"/textures/gui/{BLOCK}.png")
    else:
        for suffix in ["front", "front_on", "top"]:
            shutil.copy(PLAN + f"{BLOCK}_core_{suffix}.png", tb + f"{CORE}_core_{suffix}.png")
        save(buffer_gui() if CORE == "entangled_buffer" else tank_gui(), A + f"/textures/gui/{BLOCK}.png")

    core_tex = lambda on: {"front": f"{MOD}:block/{CORE}_core_front" + on, "side": f"{MOD}:block/core_side",
                           "top": f"{MOD}:block/{CORE}_core_top" + (on if CORE == "entangled_pad" else ""), "bottom": f"{MOD}:block/core_bottom", "particle": f"{MOD}:block/core_side"}
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
    keep = [f"{MOD}:energy", f"{MOD}:worktop", f"{MOD}:upgrades"] + ([f"{MOD}:entangled_link"] if CORE in LINKED else [])
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
    for it in ["entangled_ingot", "entangled_wire", "entangled_coil", "link_card", "entangled_backpack", "chunk_loader_module", "range_module"]:
        shutil.copy(PLAN + it + ".png", ti + it + ".png")
        jdump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{it}"}}, f"{A}/models/item/{it}.json")
        jdump({"model": {"type": "minecraft:model", "model": f"{MOD}:item/{it}"}}, f"{A}/items/{it}.json")
    for it in ["chunk_loader_module", "range_module"]:
        # the worktop draws fitted upgrades from the block atlas
        shutil.copy(PLAN + it + ".png", A + f"/textures/block/{it}.png")
    for core, block in CORES.items():
        device(core, block)
    backpack_worn()
    lang_update()
    recipes()
    g["recipe_unlocks"]()

if __name__ == "__main__":
    main()

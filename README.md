# Simple Machines

Machines, metals and parts for Minecraft 26.1.x, NeoForge and Fabric. Early alpha.

Contents so far:

- Alloy Furnace: two inputs, fuel and one output. Copper ingot and redstone make a Redstone Copper Ingot, iron ingot and 2 coal or charcoal make a Steel Ingot. Burns fuel and has no hopper sides.
- Extruder: one input, fuel and one output. One iron or copper ingot makes 2 rods. Slow, burns fuel, no hopper sides, and only takes items it has a recipe for.
- Iron Chassis (tier 1) and Steel Chassis (tier 2): the base every powered machine is built on. Steel machines run 1.5x faster and hold twice the energy.
- Solid Fuel Generator: burns fuel into FE, 40 FE/t on iron, 60 FE/t on steel.
- Rolling Mill: 20 FE/t, rolls iron, copper and steel ingots into 2 rods or one sheet. The button in its screen switches between rods and sheets.
- Electric Furnace: 20 FE/t, normal smelting recipes, a bit faster than a furnace.
- Redstone Copper Cable: carries up to 1000 FE/t, connects to anything that takes or gives FE.
- Parts: furnace, rolling and generator assemblies, control board, wood, iron, copper and steel panels and worktops, screwdriver.
- Steel ingot, Redstone Copper Ingot, iron, copper and steel rods and sheets.

Powered machines keep their stored energy when broken, and a comparator reads how full they are.

## Building a machine

Place a chassis, then right click it with each part in turn and use the screwdriver to fix it:

1. A mechanical assembly, which decides the machine (furnace, rolling or generator).
2. A control board.
3. Panels for the sides.
4. A worktop on the open top.

Each part sits loose until it is screwed in. Wood panels and worktop are cheap, metal ones come from sheets made in the Rolling Mill. Sneak and use the screwdriver to take the last part off again. Breaking a part-built chassis drops its parts.

Hoppers: top feeds the inputs, sides feed fuel first and then the inputs, bottom takes the output.

Recipes use the types `simplemachines:alloying`, `simplemachines:extruding` and `simplemachines:rolling`, see `data/simplemachines/recipe`.

Build with `./gradlew build` (Java 25). Textures and json files come from `python3 tools/gen_assets.py`.

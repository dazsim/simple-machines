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
- Parts: an assembly for each machine, control board, panels and worktops, screwdriver.
- Finishes for panels and worktops: the twelve woods, the sixteen concrete colours, iron, copper and steel.
- Steel ingot, Redstone Copper Ingot, iron, copper and steel rods and sheets.
- Battery: stores 500k FE on iron, 1M FE on steel. Takes FE on every side but the front and gives it out of the front, up to 1000 FE/t (2000 on steel). Keeps its charge as an item, so a charged battery can be carried and placed.
- Crusher: 20 FE/t. Ores and raw ores give 2 dusts (a steel crusher has a 10% chance of a third), ingots give 1 dust, cobblestone gives gravel and gravel gives sand. Dusts smelt or blast back into ingots. Works for any metal whose `c:ores`, `c:raw_materials` and `c:dusts` tags are filled by some mod; iron, copper and gold dusts are included.
- Sawmill: 20 FE/t. A log or stem gives 6 planks, a bamboo block 3, planks give 3 sticks.
- Alloy Smelter: 30 FE/t, the powered Alloy Furnace. Same recipes in half the time, no fuel.
- Crop Harvester: harvests grown crops in a 5x5 area in front of it (9x9 on steel) and replants them, 400 FE a crop.
- Shearer: shears sheep in the same area, 500 FE a sheep.
- Stone Generator: makes cobblestone when water and lava touch it, 10 FE/t.
- Water Generator: fills buckets when it touches 2 water sources, 10 FE/t.
- Mechanical Hive: 20 FE/t, makes honeycomb, and honey bottles from glass bottles.
- Charger: charges FE items placed in it, 500 FE/t (1000 on steel).
- Item Pipe: moves items between inventories, see below.

Powered machines keep their stored energy when broken, and a comparator reads how full they are.

## Building a machine

Place a chassis, then right click it with each part in turn and use the screwdriver to fix it:

1. A mechanical assembly, which decides the machine.
2. A control board.
3. Panels for the sides.
4. A worktop on the open top.

Each part sits loose until it is screwed in. Wood panels come from four slabs, concrete panels from four concrete (makes two), worktops from three slabs or three concrete (makes two). Metal ones come from sheets made in the Rolling Mill. Sneak and use the screwdriver to take the last part off again. Breaking a part-built chassis drops its parts.

## Pipes and output sides

Item pipes connect to each other and to anything with an inventory. Use any screwdriver on a pipe end next to an inventory to cycle it through input (the default; the pipe delivers into it), output (the pipe pulls items out every half second and sends them round the network in turn to every other input on it) and disconnected (the pipe ignores that block, for example a cabinet sitting under a pipe meant for the machines either side). Machines only accept items they have a recipe for, and only give up their outputs.

Use a screwdriver (without sneaking) on a side of a built machine to make it push its outputs into whatever is on that side, such as a chest or a cupboard. Sides that output or have a pipe on them show a port. All sides are off to start with. Hoppers and other mods see inputs and outputs the same way: inputs can be filled, outputs can be emptied.

## Much More Storage and other mods

- Worktops are tagged `c:worktops`. Any worktop in that tag fits a chassis, and Simple Machines worktops fit Much More Storage carcasses. Neither mod needs the other.
- With Much More Storage installed, the wood and concrete worktop recipes here are turned off and its worktops are used instead. They look the same.
- Screwdrivers are tagged `c:tools/screwdriver`. Any of them builds a machine or a cupboard, and sneaking takes parts off.

## 0.1.0-alpha.6

- Pipe ends cycle through input, output and disconnected with a screwdriver. Old extract ends load as output.

## 0.1.0-alpha.5

- Item pipes and per-side auto-output.
- New machines: Battery, Crusher, Sawmill, Alloy Smelter, Crop Harvester, Shearer, Stone Generator, Water Generator, Mechanical Hive and Charger.
- Iron, copper and gold dusts.
- Machine inventories are sided for hoppers and other mods.

## 0.1.0-alpha.4

- Copper, iron and steel sheets redrawn as flat sheets.
- Panels and worktops in all twelve woods and sixteen concrete colours, drawn to match Much More Storage.
- Worktop sits on the machine as its own piece and shows the chosen finish.
- Shared worktop and screwdriver tags, see above.
- Breaking change: the old single `wood` finish is gone, so worlds from alpha.3 with wood panels or worktops will lose them.

Hoppers: top feeds the inputs, sides feed fuel first and then the inputs, bottom takes the output.

Recipes use the types `simplemachines:alloying`, `simplemachines:extruding` and `simplemachines:rolling`, see `data/simplemachines/recipe`.

Build with `./gradlew build` (Java 25). Textures and json files come from `python3 tools/gen_assets.py`.

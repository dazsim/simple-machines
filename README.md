# Simple Machines

Machines, metals and parts for Minecraft 26.1.x, NeoForge and Fabric. Early alpha.

Contents so far:

- Alloy Furnace: two inputs, fuel and one output. Copper ingot and redstone make a Redstone Copper Ingot, iron ingot and 2 coal or charcoal make a Steel Ingot. Burns fuel and has no hopper sides.
- Extruder: one input, fuel and one output. One iron or copper ingot makes 2 rods. Slow, burns fuel, no hopper sides, and only takes items it has a recipe for.
- Iron Chassis (tier 1) and Steel Chassis (tier 2): the base every powered machine is built on. Steel machines run 1.5x faster and hold twice the energy.
- Solid Fuel Generator: burns fuel into FE, 40 FE/t on iron, 60 FE/t on steel.
- Rolling Mill: 20 FE/t, rolls iron, copper and steel ingots into 2 rods, or iron and copper into casings. The button in its screen switches between rods and casings.
- Electric Furnace: 20 FE/t, normal smelting recipes, a bit faster than a furnace.
- Redstone Copper Cable: carries up to 1000 FE/t, connects to anything that takes or gives FE.
- Parts: an assembly for each machine, control board, panels and worktops, screwdriver.
- Finishes for panels and worktops: the twelve woods, the sixteen concrete colours, iron, copper and steel.
- Steel ingot, Redstone Copper Ingot, iron, copper and steel rods, iron, copper, gold and steel plates, iron and copper casings.
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
- Electric Drill and Chainsaw, see below.
- Overclocker Circuit and Energy Cell machine upgrades, see below.
- Compressor, Auto Crafter, Lava Generator, Solar Panels, Helical Wind Turbine, Fluid Tank, fluid pipes, pipe filters, MV and HV cables, transformers and the Quarry, see below.

Powered machines keep their stored energy when broken, and a comparator reads how full they are.

## Building a machine

Place a chassis, then right click it with each part in turn and use the screwdriver to fix it:

1. A mechanical assembly, which decides the machine.
2. A control board.
3. Up to two upgrades (optional).
4. Panels for the sides.
5. A worktop on the open top.

Each part sits loose until it is screwed in. Wood panels come from four slabs, concrete panels from four concrete (makes two), worktops from three slabs or three concrete (makes two). Metal ones come from sheets made in the Rolling Mill. Sneak and use the screwdriver to take the last part off again. Breaking a part-built chassis drops its parts.

## Pipes and output sides

Item pipes connect to each other and to anything with an inventory. Use any screwdriver on a pipe end next to an inventory to cycle it through input (the default; the pipe delivers into it), output (the pipe pulls items out every half second and sends them round the network in turn to every other input on it) and disconnected (the pipe ignores that block, for example a cabinet sitting under a pipe meant for the machines either side). Machines only accept items they have a recipe for, and only give up their outputs.

Use a screwdriver (without sneaking) on a side of a built machine to make it push its outputs into whatever is on that side, such as a chest or a cupboard. Sides that output or have a pipe on them show a port. All sides are off to start with. Hoppers and other mods see inputs and outputs the same way: inputs can be filled, outputs can be emptied.

Use a screwdriver on a side of a Redstone Copper Cable to disconnect that side, or connect it again. It works on sides facing other cables and sides facing machines, so two runs laid next to each other stay separate networks.

Right-click a placed cable or item pipe with any dye to colour it (survival uses one dye). Coloured cables and pipes only join others of the same colour or uncoloured ones, so parallel runs in different colours never mix. Colour has no effect on connections to machines and inventories. Colour lives in the world only: a broken cable or pipe drops as the plain item.

## Upgrades

After the control board is screwed in, right click with an Overclocker Circuit or Energy Cell and screw it in. A machine has two upgrade slots and any mix fits; the step can be skipped. Each Overclocker adds half the base speed, and FE use and intake rise with it, so two run at double speed. Each Energy Cell adds the base storage again, so two hold triple. Upgrades stay in the machine when it is broken and carried, show in its tooltip and screen, and drop back when it is taken apart.

## Powered tools

The Electric Drill (pickaxe and shovel blocks) and Chainsaw (axe blocks) are built on a Tool Stand rather than crafted. Place the stand and fit the parts in order, right clicking with each and then with a screwdriver:

1. Tool frame (iron or steel)
2. Core: a Drill Motor makes a drill, a Chainsaw Bar a chainsaw
3. Control board
4. Battery module
5. Upgrades, optional, up to two: Overclocker Circuit or Energy Cell

Any time after the core, a Diamond Drill Bit (drill) or Diamond-Tipped Chain (chainsaw) can be fitted and screwed on in place of the standard one.

Take the finished tool off with an empty hand. To change one, put it back on an empty stand and sneak with the screwdriver: the last part comes off and drops, so you can strip it down and swap the core, frame or upgrades. The battery module keeps its charge. Breaking the stand drops it and whatever is on it.

A tool holds 50k FE and uses 50 FE a block. When empty it is slow and drops nothing. Charge it in the Charger. FE on NeoForge, Team Reborn Energy on Fabric.

Speed and tiers:

| | Effect |
|---|---|
| Steel frame | x1.25 speed |
| Overclocker | +50% speed and +50% FE per block each |
| Energy Cell | +50k FE and +15% speed each, no extra FE |
| Upgrades together | capped at +100% speed |
| Standard bit | mines up to diamond ore; obsidian and netherite level blocks are refused with white-hot sparks |
| Diamond Drill Bit | mines obsidian and netherite level blocks: full speed on a steel frame, 40% on an iron frame |
| Diamond-Tipped Chain | on a steel frame, the chainsaw fells a whole natural tree, top down, 50 FE per log, logs drop in the world; sneak to cut one log |

The parts show on the tool: a yellow drill with a tricone bit (standard or diamond), steel or iron frame, the control board behind a side panel, a heat sink per Overclocker, a bigger battery pack per Cell.

While digging, the bit's three cones turn, the chainsaw chain runs, the Overclocker fins glow, a motor sound loops (with subtitles), long streaks of hot steel sparks fly off stone and ore, and dirt, sand, gravel and wood throw bits of the block.

## Manual (Simple Books)

With Simple Books installed, getting your first screwdriver (from any mod) gives you the Simple Machines Manual, a blueprint style guide to every machine, the assembly steps with recipes, tiers, finishes, cables, pipes, auto-output, upgrades and tools. Another copy is crafted from a book, any screwdriver and a copper ingot; the screwdriver is given back. Simple Books is optional; without it the manual, its recipe and advancement are simply not loaded.

## Much More Storage and other mods

- Worktops are tagged `c:worktops`. Any worktop in that tag fits a chassis, and Simple Machines worktops fit Much More Storage carcasses. Neither mod needs the other.
- With Much More Storage installed, the wood and concrete worktop recipes here are turned off and its worktops are used instead. They look the same.
- Screwdrivers are tagged `c:tools/screwdriver`. Any of them builds a machine or a cupboard, and sneaking takes parts off.

## Compressor and Auto Crafter

- Compressor: 30 FE/t. An ingot makes a plate (iron, copper, gold and steel), 4 sand make a Silicon Wafer, 32 compostables make a Biofuel Block (burns for 2400 ticks), and 64 coal make a diamond.
- Auto Crafter: 20 FE/t. Set a crafting pattern in its screen and it crafts from the ingredients fed into its buffer, into 4 output slots. A redstone button picks always on, on with a signal or on without one. The screen says what it is waiting for.

## Fluids

- Fluid Tank: holds 16 buckets of one fluid and keeps it when broken. Right click with a bucket to fill or empty it.
- Fluid pipes move 250 mB at a time and use the same input, output and disconnected ends as item pipes.
- Pipe filters fit on an input end. Right click it with an empty hand to set the list. Allow lets only the listed items or fluids out there, Deny lets everything else. Tags on also matches anything sharing a tag, data Match also checks enchantments and damage. Pipes send matching items to filtered ends first.
- Item and fluid pipes are also crafted from casings and glass.

## Generators

- Lava Generator: 80 FE/t (120 on steel), burns 1 mB of lava every 10 ticks from a 4 bucket tank. Fill it with a bucket or a fluid pipe. It keeps its lava when broken.
- Solar Panels, half height with a gauge, made from a PV Cell and a core:

| Tier | Day | Night | Output |
|---|---|---|---|
| Iron | 4 FE/t | 0 | LV |
| Copper | 20 FE/t | 0 | LV |
| Gold | 100 FE/t | 0 | LV |
| Diamond | 1000 FE/t | 500 FE/t | MV |

- Helical Wind Turbine: up to 50 FE/t. Output grows with height (full at Y 160), open air around the top and rain or thunder.

## Voltage

Redstone Copper Cable is LV (1000 FE/t), Gold Cable MV (8000 FE/t) and Diamond Cable HV (64000 FE/t). Cables lose nothing. Machines take LV and the Quarry takes MV.

The LV/MV and MV/HV Transformers step power up or down. The front face is the high side and the other faces are the low side; use a screwdriver on a face to make it the high side, or sneak to pick the face opposite. Each step loses 5%.

Wiring a higher voltage into something rated lower makes it explode.

## Quarry

The Quarry Assembly is made with an Electric Drill, and the drill's frame, bit and upgrades carry over into the quarry. Build the Quarry on a chassis like any machine.

It digs a 16x16 area with the quarry at one corner, out from its front, down to bedrock. Landmarks placed in line with it, up to 64 blocks away in front and to one side, set the size instead. It runs on MV at 8000 FE a block and holds 200k FE (400k on steel).

Once powered, it builds a raised frame one piece at a time, then the gantry moves the drill head over the area layer by layer. Blocks in the way of the frame are dug out first, and their drops (or a chest's contents) go to its output. If something it can't break is in the way, the screen says so and it waits. If any frame piece is broken, it stops and rebuilds the frame before digging again.

Fit a pipe filter to choose what it keeps; sneak with an empty hand to take it back. A diamond bit lets it dig obsidian.

## Changelog

### 2.1.0

- New: Compressor, Auto Crafter, Lava Generator, Solar Panels in four tiers, Helical Wind Turbine, Fluid Tank, fluid pipes, pipe filters, MV and HV cables, transformers, Quarry and Landmarks.
- Plates come from the Compressor now; the Rolling Mill makes rods and casings.
- Iron and copper dusts work in the steel and redstone alloy recipes.
- JEI recipe pages for every machine (optional).
- The Battery shows its charge on a 5 bar gauge, in the world and as an item. Tank items show their fluid.
- Generators take turns on which side they power first.
- Switched-off cable sides can be clicked again with a screwdriver.
- Recipe unlocks are checked again on login.
- Version numbers are plain from 2.0.1 on.

### 2.0.0-release.1

First 26.1 release. Same content as 0.1.0-alpha.16.

### 0.1.0-alpha.14

- Square metal plate icons.

### 0.1.0-alpha.13

- The chainsaw now uses the drill's yellow housing and the same greys, and the manual covers spin-up, sparks, tool tiers, upgrades, tree felling, cables, dyes and pipe ends.
- The manual is crafted from a book, any screwdriver and a copper ingot, and the screwdriver is given back. Any screwdriver now triggers the free first copy.
- Every recipe unlocks in the recipe book once you hold one of its ingredients, and the `c:` tags have names in JEI and EMI.
- The chainsaw bar no longer shows a stray texture past its tip, and battery and cell faces stay on their own texture.
- The manual has cards for steel rods, the Charger and Power Cell, and all 11 assemblies.

### 0.1.0-alpha.12

- Hold attack and the drill or chainsaw spins up with a rising motor pitch, keeps spinning from block to block, and only winds down (pitch falling) about half a second after you stop mining.

### 0.1.0-alpha.11

- Drill sparks are back at the crosshair, and the drill is held aimed forward in first person so the bit tip sits on the crosshair.

### 0.1.0-alpha.10

- Drill sparks now fly from the tip of the bit instead of the middle of the block, sprayed back off the face. Other players see them too.

### 0.1.0-alpha.9

- New drill bit: a chunky tricone roller-cone bit on a threaded shank. The cones turn while mining. The drill body is yellow.
- Tool tiers. The standard bit mines up to diamond ore; obsidian and netherite level blocks throw white-hot sparks and do not break. The new Diamond Drill Bit mines them, at full speed on a steel frame and 40% on an iron frame.
- Diamond-Tipped Chain: a steel chainsaw with it fells whole natural trees, 50 FE per log, logs drop in the world. Sneak to cut one log.
- Energy Cells also add 15% speed each. Overclockers add 50% each; together capped at +100%. The tooltip shows the total.
- Overclocker fins glow only while the tool is in use.
- The control board sits on the side behind a panel.
- Taller Tool Stand, about the size of an anvil. New recipe: smooth stone slabs, a steel rod and smooth stone.
- Bigger, more numerous sparks with longer streaks.
- No arm swing or recoil while mining.
- Manual: new Tool Tiers page.

### 0.1.0-alpha.8

- Drill and Chainsaw are assembled from parts on the new Tool Stand: frame, core, control board, battery module and up to two upgrades, each screwed in. Sneak with a screwdriver to strip or swap parts. The finished tool recipes are gone; frames, cores, battery module and stand have recipes.
- 3D tool models that show their parts. The drill bit spins and the chain runs while digging.
- Hot orange steel sparks when tools cut stone and ore.
- Drill and Chainsaw sounds and mining particles.
- The Electric Shovel is merged into the Electric Drill, which now mines shovel blocks too. Old shovels vanish from worlds.
- Worktop items are now the same 3D slab as Much More Storage's, so the two mods' tops look identical in inventories. With Much More Storage installed, a Simple Machines wood or concrete worktop crafts on its own into the matching Much More Storage one so they stack. Tops from either mod still fit either mod's cabinets and machines.
- Screwdriver disconnects or reconnects a cable side.
- Cables and item pipes can be dyed in the world; different colours stay apart.

### 0.1.0-alpha.7

- Electric Drill, Chainsaw and Electric Shovel, charged in the Charger.
- Overclocker Circuit and Energy Cell upgrades, an optional assembly step after the control board.
- Simple Machines Manual for Simple Books (optional dependency).

### 0.1.0-alpha.6

- Pipe ends cycle through input, output and disconnected with a screwdriver. Old extract ends load as output.

### 0.1.0-alpha.5

- Item pipes and per-side auto-output.
- New machines: Battery, Crusher, Sawmill, Alloy Smelter, Crop Harvester, Shearer, Stone Generator, Water Generator, Mechanical Hive and Charger.
- Iron, copper and gold dusts.
- Machine inventories are sided for hoppers and other mods.

### 0.1.0-alpha.4

- Copper, iron and steel sheets redrawn as flat sheets.
- Panels and worktops in all twelve woods and sixteen concrete colours, drawn to match Much More Storage.
- Worktop sits on the machine as its own piece and shows the chosen finish.
- Shared worktop and screwdriver tags, see above.
- Breaking change: the old single `wood` finish is gone, so worlds from alpha.3 with wood panels or worktops will lose them.

Hoppers: top feeds the inputs, sides feed fuel first and then the inputs, bottom takes the output.

Recipes use the types `simplemachines:alloying`, `simplemachines:extruding` and `simplemachines:rolling`, see `data/simplemachines/recipe`.

Build with `./gradlew build` (Java 25). Textures and json files come from `python3 tools/gen_assets.py`.

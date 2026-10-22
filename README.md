# Simple Machines

Fuel-burning machines, metals and parts for Minecraft 26.1.x, NeoForge and Fabric. Early alpha.

Contents so far:

- Alloy Furnace: two inputs, fuel and one output. Copper ingot and redstone make a Redstone Copper Ingot, iron ingot and 2 coal or charcoal make a Steel Ingot.
- Extruder: one input, fuel and one output. One iron or copper ingot makes 2 rods. Slow.
- Steel Chassis: decorative for now.
- Steel ingot, Redstone Copper Ingot, iron, copper and steel rods.

Hoppers: top feeds the inputs, sides feed fuel first and then the inputs, bottom takes the output.

Recipes use the types `simplemachines:alloying` and `simplemachines:extruding`, see `data/simplemachines/recipe`.

Build with `./gradlew build` (Java 25). Textures and json files come from `python3 tools/gen_assets.py`.

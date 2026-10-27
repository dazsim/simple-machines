package com.workshopcraft.simplemachines;

import com.workshopcraft.simplemachines.assembly.Upgrade;
import com.workshopcraft.simplemachines.item.MachineItem;
import com.workshopcraft.simplemachines.item.PoweredToolItem;
import com.workshopcraft.simplemachines.item.UpgradeItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvent;

import com.workshopcraft.simplemachines.assembly.ChassisBlock;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Finish;
import com.workshopcraft.simplemachines.assembly.ChassisBlockEntity;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.machine.AlloyFurnaceBlock;
import com.workshopcraft.simplemachines.machine.ElectricFurnaceBlock;
import com.workshopcraft.simplemachines.machine.ElectricFurnaceBlockEntity;
import com.workshopcraft.simplemachines.machine.AlloyFurnaceBlockEntity;
import com.workshopcraft.simplemachines.machine.ExtruderBlock;
import com.workshopcraft.simplemachines.machine.ExtruderBlockEntity;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import com.workshopcraft.simplemachines.machine.GeneratorBlock;
import com.workshopcraft.simplemachines.machine.GeneratorBlockEntity;
import com.workshopcraft.simplemachines.machine.RollingMillBlock;
import com.workshopcraft.simplemachines.machine.RollingMillBlockEntity;
import com.workshopcraft.simplemachines.machine.MachineMenu;
import com.workshopcraft.simplemachines.recipe.AlloyingRecipe;
import com.workshopcraft.simplemachines.recipe.ExtrudingRecipe;
import com.workshopcraft.simplemachines.recipe.RollingRecipe;
import com.workshopcraft.simplemachines.recipe.KeepToolRecipe;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import com.workshopcraft.simplemachines.machine.*;
import com.workshopcraft.simplemachines.pipe.ItemPipeBlock;
import com.workshopcraft.simplemachines.recipe.ProcessRecipe;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class SimpleMachines {
    public static final String MODID = "simplemachines";

    public static final RecipeType<AlloyingRecipe> ALLOYING = type("alloying");
    public static final RecipeType<ExtrudingRecipe> EXTRUDING = type("extruding");
    public static final RecipeType<RollingRecipe> ROLLING = type("rolling");
    public static final RecipeSerializer<AlloyingRecipe> ALLOYING_SERIALIZER = new RecipeSerializer<>(AlloyingRecipe.MAP_CODEC, AlloyingRecipe.STREAM_CODEC);
    public static final RecipeSerializer<ExtrudingRecipe> EXTRUDING_SERIALIZER = new RecipeSerializer<>(ExtrudingRecipe.MAP_CODEC, ExtrudingRecipe.STREAM_CODEC);
    public static final RecipeSerializer<RollingRecipe> ROLLING_SERIALIZER = new RecipeSerializer<>(RollingRecipe.MAP_CODEC, RollingRecipe.STREAM_CODEC);
    public static final RecipeType<ProcessRecipe> CRUSHING = type("crushing");
    public static final RecipeType<ProcessRecipe> SAWING = type("sawing");
    public static final RecipeSerializer<ProcessRecipe> CRUSHING_SERIALIZER = new RecipeSerializer<>(
            ProcessRecipe.mapCodec(ProcessRecipe.Kind.CRUSHING), ProcessRecipe.streamCodec(ProcessRecipe.Kind.CRUSHING));
    public static final RecipeSerializer<ProcessRecipe> SAWING_SERIALIZER = new RecipeSerializer<>(
            ProcessRecipe.mapCodec(ProcessRecipe.Kind.SAWING), ProcessRecipe.streamCodec(ProcessRecipe.Kind.SAWING));
    public static final RecipeType<ProcessRecipe> COMPRESSING = type("compressing");
    public static final RecipeSerializer<ProcessRecipe> COMPRESSING_SERIALIZER = new RecipeSerializer<>(
            ProcessRecipe.mapCodec(ProcessRecipe.Kind.COMPRESSING), ProcessRecipe.streamCodec(ProcessRecipe.Kind.COMPRESSING));
    public static final List<Layout> LAYOUTS = List.of(Layout.PROCESS, Layout.ALLOY, Layout.BATTERY, Layout.STONE, Layout.HIVE, Layout.GRID, Layout.LAVA);
    public static final Map<Layout, MenuType<PoweredMenu>> MENUS = new HashMap<>();
    /** Machines the chassis can become, by core. */
    public static final Map<Core, Block> MACHINES = new EnumMap<>(Core.class);
    public static final Map<Core, BlockEntityType<?>> MACHINE_ENTITIES = new EnumMap<>(Core.class);
    /** Every block entity type built on WorkBlockEntity, for the loaders' item and energy capabilities. */
    public static final List<BlockEntityType<? extends WorkBlockEntity>> WORK_ENTITIES = new ArrayList<>();
    /** The machines added in 0.1.0-alpha.5, all plain WorkMachineBlocks. */
    public static final List<Core> NEW_CORES = List.of(Core.BATTERY, Core.CRUSHER, Core.HARVESTER, Core.ALLOY, Core.STONE,
            Core.SAW, Core.WATER, Core.HIVE, Core.SHEARER, Core.CHARGER, Core.CRAFTER, Core.COMPRESSOR, Core.LAVA);
    public static final MenuType<MachineMenu> ALLOY_MENU = new MenuType<>(MachineMenu::alloy, FeatureFlags.VANILLA_SET);
    public static final MenuType<MachineMenu> EXTRUDER_MENU = new MenuType<>(MachineMenu::extruder, FeatureFlags.VANILLA_SET);
    public static final MenuType<EnergyMenu> GENERATOR_MENU = new MenuType<>(EnergyMenu::generator, FeatureFlags.VANILLA_SET);
    public static final MenuType<EnergyMenu> ROLLING_MILL_MENU = new MenuType<>(EnergyMenu::rollingMill, FeatureFlags.VANILLA_SET);
    public static final MenuType<EnergyMenu> ELECTRIC_FURNACE_MENU = new MenuType<>(EnergyMenu::electricFurnace, FeatureFlags.VANILLA_SET);
    public static final MenuType<com.workshopcraft.simplemachines.pipe.FilterMenu> FILTER_MENU =
            new MenuType<>(com.workshopcraft.simplemachines.pipe.FilterMenu::client, FeatureFlags.VANILLA_SET);
    public static final DataComponentType<com.workshopcraft.simplemachines.pipe.PipeFilter> FILTER =
            DataComponentType.<com.workshopcraft.simplemachines.pipe.PipeFilter>builder()
                    .persistent(com.workshopcraft.simplemachines.pipe.PipeFilter.CODEC)
                    .networkSynchronized(com.workshopcraft.simplemachines.pipe.PipeFilter.STREAM_CODEC).build();
    public static final DataComponentType<com.workshopcraft.simplemachines.pipe.TankContents> TANK =
            DataComponentType.<com.workshopcraft.simplemachines.pipe.TankContents>builder()
                    .persistent(com.workshopcraft.simplemachines.pipe.TankContents.CODEC)
                    .networkSynchronized(com.workshopcraft.simplemachines.pipe.TankContents.STREAM_CODEC).build();
    public static final MenuType<com.workshopcraft.simplemachines.machine.AutoCrafterMenu> CRAFTER_MENU =
            new MenuType<>(com.workshopcraft.simplemachines.machine.AutoCrafterMenu::client, FeatureFlags.VANILLA_SET);
    public static final DataComponentType<net.minecraft.world.item.component.ItemContainerContents> CRAFTER_PATTERN =
            DataComponentType.<net.minecraft.world.item.component.ItemContainerContents>builder()
                    .persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                    .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC).cacheEncoding().build();
    public static final DataComponentType<Integer> ENERGY = DataComponentType.<Integer>builder()
            .persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
    public static final DataComponentType<String> WORKTOP = DataComponentType.<String>builder()
            .persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();
    public static final DataComponentType<String> UPGRADES_FITTED = DataComponentType.<String>builder()
            .persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();
    public static final DataComponentType<String> TOOL_FRAME = DataComponentType.<String>builder()
            .persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();
    /** "diamond" for a diamond drill bit or diamond-tipped chain; absent for the standard one, "none" while the stand shows it loose. */
    public static final DataComponentType<String> TOOL_TIP = DataComponentType.<String>builder()
            .persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();
    /** Which parts a tool model shows, as ToolStandRenderer bits; only set on the stacks the stand draws. */
    public static final DataComponentType<Integer> TOOL_PARTS = DataComponentType.<Integer>builder()
            .persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
    public static final SoundEvent DRILL_SOUND = SoundEvent.createVariableRangeEvent(id("item.electric_drill.run"));
    public static final net.minecraft.core.particles.SimpleParticleType HOT_SPARK = new net.minecraft.core.particles.SimpleParticleType(false) {
    };
    public static final net.minecraft.core.particles.SimpleParticleType WHITE_SPARK = new net.minecraft.core.particles.SimpleParticleType(false) {
    };
    public static final SoundEvent CHAINSAW_SOUND = SoundEvent.createVariableRangeEvent(id("item.chainsaw.run"));
    public static final Map<Upgrade, Item> UPGRADES = new EnumMap<>(Upgrade.class);
    public static final Map<Core, Item> ASSEMBLIES = new EnumMap<>(Core.class);
    public static final Map<Finish, Item> PANELS = new EnumMap<>(Finish.class);
    public static final Map<Finish, Item> WORKTOPS = new EnumMap<>(Finish.class);

    public static Block alloyFurnace;
    public static Block generator;
    public static Block rollingMill;
    public static Block extruder;
    public static Block electricFurnace;
    public static Block ironChassis;
    public static Block steelChassis;
    public static Block cable;
    public static Block goldCable;
    public static Block diamondCable;
    public static Block lvMvTransformer;
    public static Block mvHvTransformer;
    public static final List<Block> CABLES = new ArrayList<>();
    public static BlockEntityType<com.workshopcraft.simplemachines.cable.TransformerBlockEntity> transformerEntity;
    public static Block itemPipe;
    public static Block fluidPipe;
    public static Item pipeFilter;
    public static Block tank;
    public static Block biofuelBlock;
    public static Item biofuelItem;
    public static Item screwdriver;
    public static Item controlBoard;
    public static Item ironToolFrame;
    public static Item steelToolFrame;
    public static Item drillMotor;
    public static Item chainsawBar;
    public static Item batteryModule;
    public static Item diamondDrillBit;
    public static Item diamondChain;
    public static Block toolStand;
    public static BlockEntityType<com.workshopcraft.simplemachines.assembly.ToolStandBlockEntity> toolStandEntity;
    public static Item powerCell;
    public static final List<Item> TOOLS = new ArrayList<>();
    public static BlockEntityType<AlloyFurnaceBlockEntity> alloyFurnaceEntity;
    public static BlockEntityType<ExtruderBlockEntity> extruderEntity;
    public static BlockEntityType<GeneratorBlockEntity> generatorEntity;
    public static BlockEntityType<RollingMillBlockEntity> rollingMillEntity;
    public static BlockEntityType<ElectricFurnaceBlockEntity> electricFurnaceEntity;
    public static BlockEntityType<ChassisBlockEntity> chassisEntity;
    public static BlockEntityType<com.workshopcraft.simplemachines.cable.ConduitBlockEntity> conduitEntity;
    public static BlockEntityType<com.workshopcraft.simplemachines.pipe.TankBlockEntity> tankEntity;
    private static final List<Item> TAB = new ArrayList<>();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static <T extends Recipe<?>> RecipeType<T> type(String name) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return MODID + ":" + name;
            }
        };
    }

    private static BlockBehaviour.Properties machine(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)))
                .mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.STONE)
                .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0);
    }

    private static BlockBehaviour.Properties built(String name) {
        return machine(name).mapColor(MapColor.METAL).sound(SoundType.METAL).noOcclusion()
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false);
    }

    public static MenuType<PoweredMenu> menuFor(Layout layout) {
        return MENUS.get(layout);
    }

    @SuppressWarnings("unchecked")
    private static MenuType<PoweredMenu> menu(Layout layout) {
        final MenuType<PoweredMenu>[] holder = new MenuType[1];
        holder[0] = new MenuType<>((id, inv) -> new PoweredMenu(holder[0], id, inv, new SimpleContainer(layout.slots().length),
                new SimpleContainerData(EnergyMenu.DATA_COUNT), layout), FeatureFlags.VANILLA_SET);
        return holder[0];
    }

    static {
        for (Layout l : LAYOUTS) MENUS.put(l, menu(l));
    }

    private static BlockBehaviour.Properties chassis(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)))
                .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0F).sound(SoundType.METAL)
                .noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false);
    }

    public static void registerBlocks(BiConsumer<Identifier, Block> r) {
        alloyFurnace = new AlloyFurnaceBlock(machine("alloy_furnace"));
        r.accept(id("alloy_furnace"), alloyFurnace);
        extruder = new ExtruderBlock(machine("extruder").mapColor(MapColor.METAL).sound(SoundType.METAL));
        r.accept(id("extruder"), extruder);
        generator = new GeneratorBlock(built("solid_fuel_generator"));
        r.accept(id("solid_fuel_generator"), generator);
        rollingMill = new RollingMillBlock(built("rolling_mill"));
        r.accept(id("rolling_mill"), rollingMill);
        electricFurnace = new ElectricFurnaceBlock(built("electric_furnace"));
        r.accept(id("electric_furnace"), electricFurnace);
        ironChassis = new ChassisBlock(Tier.IRON, chassis("iron_chassis"));
        r.accept(id("iron_chassis"), ironChassis);
        steelChassis = new ChassisBlock(Tier.STEEL, chassis("steel_chassis"));
        r.accept(id("steel_chassis"), steelChassis);
        cable = cable(r, "redstone_copper_cable", MapColor.COLOR_RED, com.workshopcraft.simplemachines.cable.Voltage.LV);
        goldCable = cable(r, "gold_cable", MapColor.COLOR_YELLOW, com.workshopcraft.simplemachines.cable.Voltage.MV);
        diamondCable = cable(r, "diamond_cable", MapColor.COLOR_LIGHT_BLUE, com.workshopcraft.simplemachines.cable.Voltage.HV);
        lvMvTransformer = transformer(r, "lv_mv_transformer", com.workshopcraft.simplemachines.cable.Voltage.MV);
        mvHvTransformer = transformer(r, "mv_hv_transformer", com.workshopcraft.simplemachines.cable.Voltage.HV);
        itemPipe = new ItemPipeBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("item_pipe")))
                .mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.COPPER).noOcclusion());
        r.accept(id("item_pipe"), itemPipe);
        fluidPipe = new com.workshopcraft.simplemachines.pipe.FluidPipeBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id("fluid_pipe"))).mapColor(MapColor.COLOR_ORANGE).strength(0.5F)
                .sound(SoundType.COPPER).noOcclusion());
        r.accept(id("fluid_pipe"), fluidPipe);
        tank = new com.workshopcraft.simplemachines.pipe.TankBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id("fluid_tank"))).mapColor(MapColor.METAL).strength(1.5F)
                .sound(SoundType.COPPER).noOcclusion().isViewBlocking((st, l, p) -> false).isRedstoneConductor((st, l, p) -> false)
                .isSuffocating((st, l, p) -> false));
        r.accept(id("fluid_tank"), tank);
        toolStand = new com.workshopcraft.simplemachines.assembly.ToolStandBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id("tool_stand"))).mapColor(MapColor.METAL).strength(2.0F)
                .sound(SoundType.METAL).noOcclusion());
        r.accept(id("tool_stand"), toolStand);
        biofuelBlock = new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("biofuel_block")))
                .mapColor(MapColor.COLOR_GREEN).strength(1.0F).sound(SoundType.GRASS));
        r.accept(id("biofuel_block"), biofuelBlock);
        MACHINES.put(Core.FURNACE, electricFurnace);
        MACHINES.put(Core.ROLLING, rollingMill);
        MACHINES.put(Core.GENERATOR, generator);
        for (Core core : NEW_CORES) {
            final Block b = core == Core.BATTERY ? new com.workshopcraft.simplemachines.machine.BatteryBlock(built(core.block).lightLevel(st -> 0))
                    : new WorkMachineBlock(built(core.block), core);
            r.accept(id(core.block), b);
            MACHINES.put(core, b);
        }
    }

    private static Block cable(BiConsumer<Identifier, Block> r, String name, MapColor colour, int tier) {
        final Block b = new CableBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)))
                .mapColor(colour).strength(0.5F).sound(SoundType.COPPER).noOcclusion(), tier);
        r.accept(id(name), b);
        CABLES.add(b);
        return b;
    }

    private static Block transformer(BiConsumer<Identifier, Block> r, String name, int high) {
        final Block b = new com.workshopcraft.simplemachines.cable.TransformerBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id(name))).mapColor(MapColor.METAL).strength(3.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), high);
        r.accept(id(name), b);
        return b;
    }

    public static void registerItems(BiConsumer<Identifier, Item> r) {
        for (String name : List.of("steel_ingot", "redstone_copper_ingot", "iron_rod", "copper_rod", "steel_rod",
                "iron_sheet", "copper_sheet", "steel_sheet", "gold_sheet", "iron_casing", "copper_casing", "silicon_wafer")) {
            add(r, name, new Item(props(name)));
        }
        screwdriver = add(r, "screwdriver", new Item(props("screwdriver").stacksTo(1)));
        add(r, "alloy_furnace", new BlockItem(alloyFurnace, props("alloy_furnace").useBlockDescriptionPrefix()));
        add(r, "extruder", new BlockItem(extruder, props("extruder").useBlockDescriptionPrefix()));
        add(r, "iron_chassis", new BlockItem(ironChassis, props("iron_chassis").useBlockDescriptionPrefix()));
        add(r, "steel_chassis", new BlockItem(steelChassis, props("steel_chassis").useBlockDescriptionPrefix()));
        for (Core core : Core.values()) {
            if (core == Core.NONE) continue;
            final String name = core.getSerializedName() + "_assembly";
            ASSEMBLIES.put(core, add(r, name, new Item(props(name))));
        }
        controlBoard = add(r, "control_board", new Item(props("control_board")));
        for (Finish finish : Finish.values()) {
            if (finish == Finish.NONE) continue;
            final String panels = finish.getSerializedName() + "_panels";
            final String worktop = finish.getSerializedName() + "_worktop";
            PANELS.put(finish, add(r, panels, new Item(props(panels))));
            WORKTOPS.put(finish, add(r, worktop, new Item(props(worktop))));
        }
        add(r, "redstone_copper_cable", new BlockItem(cable, props("redstone_copper_cable").useBlockDescriptionPrefix()));
        add(r, "gold_cable", new BlockItem(goldCable, props("gold_cable").useBlockDescriptionPrefix()));
        add(r, "diamond_cable", new BlockItem(diamondCable, props("diamond_cable").useBlockDescriptionPrefix()));
        add(r, "lv_mv_transformer", new BlockItem(lvMvTransformer, props("lv_mv_transformer").useBlockDescriptionPrefix()));
        add(r, "mv_hv_transformer", new BlockItem(mvHvTransformer, props("mv_hv_transformer").useBlockDescriptionPrefix()));
        add(r, "solid_fuel_generator", new MachineItem(generator, props("solid_fuel_generator").useBlockDescriptionPrefix()));
        add(r, "rolling_mill", new MachineItem(rollingMill, props("rolling_mill").useBlockDescriptionPrefix()));
        add(r, "electric_furnace", new MachineItem(electricFurnace, props("electric_furnace").useBlockDescriptionPrefix()));
        for (Core core : NEW_CORES) add(r, core.block, new MachineItem(MACHINES.get(core), props(core.block).useBlockDescriptionPrefix()));
        add(r, "item_pipe", new BlockItem(itemPipe, props("item_pipe").useBlockDescriptionPrefix()));
        add(r, "fluid_pipe", new BlockItem(fluidPipe, props("fluid_pipe").useBlockDescriptionPrefix()));
        add(r, "fluid_tank", new com.workshopcraft.simplemachines.pipe.TankItem(tank, props("fluid_tank").useBlockDescriptionPrefix()));
        biofuelItem = add(r, "biofuel_block", new BlockItem(biofuelBlock, props("biofuel_block").useBlockDescriptionPrefix()));
        pipeFilter = add(r, "pipe_filter", new com.workshopcraft.simplemachines.pipe.PipeFilterItem(props("pipe_filter")));
        for (String name : List.of("iron_dust", "copper_dust", "gold_dust")) add(r, name, new Item(props(name)));
        for (Upgrade u : Upgrade.values()) UPGRADES.put(u, add(r, u.id, new UpgradeItem(u, props(u.id).stacksTo(16))));
        powerCell = add(r, "power_cell", new Item(props("power_cell")));
        add(r, "tool_stand", new BlockItem(toolStand, props("tool_stand").useBlockDescriptionPrefix()));
        ironToolFrame = add(r, "iron_tool_frame", new Item(props("iron_tool_frame")));
        steelToolFrame = add(r, "steel_tool_frame", new Item(props("steel_tool_frame")));
        drillMotor = add(r, "drill_motor", new Item(props("drill_motor")));
        chainsawBar = add(r, "chainsaw_bar", new Item(props("chainsaw_bar")));
        diamondDrillBit = add(r, "diamond_drill_bit", new com.workshopcraft.simplemachines.item.ToolTipItem(props("diamond_drill_bit"), "tooltip.simplemachines.diamond_bit_part"));
        diamondChain = add(r, "diamond_tipped_chain", new com.workshopcraft.simplemachines.item.ToolTipItem(props("diamond_tipped_chain"), "tooltip.simplemachines.diamond_chain_part"));
        batteryModule = add(r, "battery_module", new com.workshopcraft.simplemachines.item.BatteryModuleItem(props("battery_module").stacksTo(1)));
        TOOLS.add(add(r, "electric_drill", new PoweredToolItem(props("electric_drill"), DRILL_SOUND,
                List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_SHOVEL), 5.0F, -2.8F)));
        TOOLS.add(add(r, "chainsaw", new PoweredToolItem(props("chainsaw"), CHAINSAW_SOUND, List.of(BlockTags.MINEABLE_WITH_AXE), 8.0F, -3.0F)));
    }

    private static Item.Properties props(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    private static Item add(BiConsumer<Identifier, Item> r, String name, Item item) {
        r.accept(id(name), item);
        TAB.add(item);
        return item;
    }

    public static void registerSounds(BiConsumer<Identifier, SoundEvent> r) {
        r.accept(DRILL_SOUND.location(), DRILL_SOUND);
        r.accept(CHAINSAW_SOUND.location(), CHAINSAW_SOUND);
    }

    public static void registerParticles(BiConsumer<Identifier, net.minecraft.core.particles.ParticleType<?>> r) {
        r.accept(id("hot_spark"), HOT_SPARK);
        r.accept(id("white_spark"), WHITE_SPARK);
    }

    public static void registerComponents(BiConsumer<Identifier, DataComponentType<?>> r) {
        r.accept(id("energy"), ENERGY);
        r.accept(id("worktop"), WORKTOP);
        r.accept(id("upgrades"), UPGRADES_FITTED);
        r.accept(id("tool_frame"), TOOL_FRAME);
        r.accept(id("tool_parts"), TOOL_PARTS);
        r.accept(id("tool_tip"), TOOL_TIP);
        r.accept(id("filter"), FILTER);
        r.accept(id("tank"), TANK);
        r.accept(id("crafter_pattern"), CRAFTER_PATTERN);
    }

    public static void registerTabs(BiConsumer<Identifier, CreativeModeTab> r) {
        r.accept(id("main"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.simplemachines"))
                .icon(() -> new ItemStack(alloyFurnace))
                .displayItems((params, out) -> {
                    TAB.forEach(out::accept);
                    TOOLS.forEach(t -> out.accept(PoweredToolItem.charged(t)));
                })
                .build());
    }

    public static void registerBlockEntities(BiConsumer<Identifier, BlockEntityType<?>> r) {
        alloyFurnaceEntity = new BlockEntityType<>(AlloyFurnaceBlockEntity::new, Set.of(alloyFurnace));
        r.accept(id("alloy_furnace"), alloyFurnaceEntity);
        extruderEntity = new BlockEntityType<>(ExtruderBlockEntity::new, Set.of(extruder));
        r.accept(id("extruder"), extruderEntity);
        generatorEntity = new BlockEntityType<>(GeneratorBlockEntity::new, Set.of(generator));
        r.accept(id("solid_fuel_generator"), generatorEntity);
        rollingMillEntity = new BlockEntityType<>(RollingMillBlockEntity::new, Set.of(rollingMill));
        r.accept(id("rolling_mill"), rollingMillEntity);
        electricFurnaceEntity = new BlockEntityType<>(ElectricFurnaceBlockEntity::new, Set.of(electricFurnace));
        r.accept(id("electric_furnace"), electricFurnaceEntity);
        entity(r, Core.BATTERY, BatteryBlockEntity::new);
        entity(r, Core.CRUSHER, (p, st) -> new ProcessBlockEntity(MACHINE_ENTITIES.get(Core.CRUSHER), p, st, CRUSHING, "crusher"));
        entity(r, Core.SAW, (p, st) -> new ProcessBlockEntity(MACHINE_ENTITIES.get(Core.SAW), p, st, SAWING, "sawmill"));
        entity(r, Core.COMPRESSOR, (p, st) -> new ProcessBlockEntity(MACHINE_ENTITIES.get(Core.COMPRESSOR), p, st, COMPRESSING, "compressor",
                ProcessBlockEntity.COMPRESSOR_CAPACITY, ProcessBlockEntity.MAX_IN, ProcessBlockEntity.COMPRESSOR_PER_TICK));
        entity(r, Core.LAVA, LavaGeneratorBlockEntity::new);
        entity(r, Core.HARVESTER, HarvesterBlockEntity::new);
        entity(r, Core.ALLOY, AlloySmelterBlockEntity::new);
        entity(r, Core.STONE, (p, st) -> new SourceBlockEntity(p, st, SourceBlockEntity.Kind.STONE));
        entity(r, Core.WATER, (p, st) -> new SourceBlockEntity(p, st, SourceBlockEntity.Kind.WATER));
        entity(r, Core.HIVE, (p, st) -> new SourceBlockEntity(p, st, SourceBlockEntity.Kind.HIVE));
        entity(r, Core.SHEARER, ShearerBlockEntity::new);
        entity(r, Core.CHARGER, ChargerBlockEntity::new);
        entity(r, Core.CRAFTER, com.workshopcraft.simplemachines.machine.AutoCrafterBlockEntity::new);
        WORK_ENTITIES.add(generatorEntity);
        WORK_ENTITIES.add(rollingMillEntity);
        WORK_ENTITIES.add(electricFurnaceEntity);
        chassisEntity = new BlockEntityType<>(ChassisBlockEntity::new, Set.of(ironChassis, steelChassis));
        r.accept(id("chassis"), chassisEntity);
        conduitEntity = new BlockEntityType<>(com.workshopcraft.simplemachines.cable.ConduitBlockEntity::new, Set.of(cable, goldCable, diamondCable, itemPipe, fluidPipe));
        r.accept(id("conduit"), conduitEntity);
        transformerEntity = new BlockEntityType<>(com.workshopcraft.simplemachines.cable.TransformerBlockEntity::new, Set.of(lvMvTransformer, mvHvTransformer));
        r.accept(id("transformer"), transformerEntity);
        tankEntity = new BlockEntityType<>(com.workshopcraft.simplemachines.pipe.TankBlockEntity::new, Set.of(tank));
        r.accept(id("fluid_tank"), tankEntity);
        toolStandEntity = new BlockEntityType<>(com.workshopcraft.simplemachines.assembly.ToolStandBlockEntity::new, Set.of(toolStand));
        r.accept(id("tool_stand"), toolStandEntity);
    }

    private static <T extends WorkBlockEntity> void entity(BiConsumer<Identifier, BlockEntityType<?>> r, Core core,
                                                           BlockEntityType.BlockEntitySupplier<T> factory) {
        final BlockEntityType<T> type = new BlockEntityType<>(factory, Set.of(MACHINES.get(core)));
        MACHINE_ENTITIES.put(core, type);
        WORK_ENTITIES.add(type);
        r.accept(id(core.block), type);
    }

    public static void registerMenus(BiConsumer<Identifier, MenuType<?>> r) {
        for (Layout l : LAYOUTS) r.accept(id("powered_" + l.name()), MENUS.get(l));
        r.accept(id("alloy_furnace"), ALLOY_MENU);
        r.accept(id("extruder"), EXTRUDER_MENU);
        r.accept(id("solid_fuel_generator"), GENERATOR_MENU);
        r.accept(id("rolling_mill"), ROLLING_MILL_MENU);
        r.accept(id("electric_furnace"), ELECTRIC_FURNACE_MENU);
        r.accept(id("pipe_filter"), FILTER_MENU);
        r.accept(id("auto_crafter"), CRAFTER_MENU);
    }

    public static void registerRecipeTypes(BiConsumer<Identifier, RecipeType<?>> r) {
        r.accept(id("alloying"), ALLOYING);
        r.accept(id("extruding"), EXTRUDING);
        r.accept(id("rolling"), ROLLING);
        r.accept(id("crushing"), CRUSHING);
        r.accept(id("sawing"), SAWING);
        r.accept(id("compressing"), COMPRESSING);
    }

    public static void registerRecipeSerializers(BiConsumer<Identifier, RecipeSerializer<?>> r) {
        r.accept(id("alloying"), ALLOYING_SERIALIZER);
        r.accept(id("extruding"), EXTRUDING_SERIALIZER);
        r.accept(id("rolling"), ROLLING_SERIALIZER);
        r.accept(id("crushing"), CRUSHING_SERIALIZER);
        r.accept(id("sawing"), SAWING_SERIALIZER);
        r.accept(id("compressing"), COMPRESSING_SERIALIZER);
        r.accept(id("crafting_shapeless_keep_tool"), KeepToolRecipe.SERIALIZER);
    }
}

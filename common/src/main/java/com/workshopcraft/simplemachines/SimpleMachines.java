package com.workshopcraft.simplemachines;

import com.workshopcraft.simplemachines.assembly.ChassisBlock;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Finish;
import com.workshopcraft.simplemachines.assembly.ScrewdriverItem;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.EnumMap;
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
    public static final MenuType<MachineMenu> ALLOY_MENU = new MenuType<>(MachineMenu::alloy, FeatureFlags.VANILLA_SET);
    public static final MenuType<MachineMenu> EXTRUDER_MENU = new MenuType<>(MachineMenu::extruder, FeatureFlags.VANILLA_SET);
    public static final MenuType<EnergyMenu> GENERATOR_MENU = new MenuType<>(EnergyMenu::generator, FeatureFlags.VANILLA_SET);
    public static final MenuType<EnergyMenu> ROLLING_MILL_MENU = new MenuType<>(EnergyMenu::rollingMill, FeatureFlags.VANILLA_SET);
    public static final MenuType<EnergyMenu> ELECTRIC_FURNACE_MENU = new MenuType<>(EnergyMenu::electricFurnace, FeatureFlags.VANILLA_SET);
    public static final DataComponentType<Integer> ENERGY = DataComponentType.<Integer>builder()
            .persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
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
    public static Item screwdriver;
    public static Item controlBoard;
    public static BlockEntityType<AlloyFurnaceBlockEntity> alloyFurnaceEntity;
    public static BlockEntityType<ExtruderBlockEntity> extruderEntity;
    public static BlockEntityType<GeneratorBlockEntity> generatorEntity;
    public static BlockEntityType<RollingMillBlockEntity> rollingMillEntity;
    public static BlockEntityType<ElectricFurnaceBlockEntity> electricFurnaceEntity;
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
        cable = new CableBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("redstone_copper_cable")))
                .mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.COPPER).noOcclusion());
        r.accept(id("redstone_copper_cable"), cable);
    }

    public static void registerItems(BiConsumer<Identifier, Item> r) {
        for (String name : List.of("steel_ingot", "redstone_copper_ingot", "iron_rod", "copper_rod", "steel_rod",
                "iron_sheet", "copper_sheet", "steel_sheet")) {
            add(r, name, new Item(props(name)));
        }
        screwdriver = add(r, "screwdriver", new ScrewdriverItem(props("screwdriver").stacksTo(1)));
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
        add(r, "solid_fuel_generator", new BlockItem(generator, props("solid_fuel_generator").useBlockDescriptionPrefix()));
        add(r, "rolling_mill", new BlockItem(rollingMill, props("rolling_mill").useBlockDescriptionPrefix()));
        add(r, "electric_furnace", new BlockItem(electricFurnace, props("electric_furnace").useBlockDescriptionPrefix()));
    }

    private static Item.Properties props(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    private static Item add(BiConsumer<Identifier, Item> r, String name, Item item) {
        r.accept(id(name), item);
        TAB.add(item);
        return item;
    }

    public static void registerComponents(BiConsumer<Identifier, DataComponentType<?>> r) {
        r.accept(id("energy"), ENERGY);
    }

    public static void registerTabs(BiConsumer<Identifier, CreativeModeTab> r) {
        r.accept(id("main"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.simplemachines"))
                .icon(() -> new ItemStack(alloyFurnace))
                .displayItems((params, out) -> TAB.forEach(out::accept))
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
    }

    public static void registerMenus(BiConsumer<Identifier, MenuType<?>> r) {
        r.accept(id("alloy_furnace"), ALLOY_MENU);
        r.accept(id("extruder"), EXTRUDER_MENU);
        r.accept(id("solid_fuel_generator"), GENERATOR_MENU);
        r.accept(id("rolling_mill"), ROLLING_MILL_MENU);
        r.accept(id("electric_furnace"), ELECTRIC_FURNACE_MENU);
    }

    public static void registerRecipeTypes(BiConsumer<Identifier, RecipeType<?>> r) {
        r.accept(id("alloying"), ALLOYING);
        r.accept(id("extruding"), EXTRUDING);
        r.accept(id("rolling"), ROLLING);
    }

    public static void registerRecipeSerializers(BiConsumer<Identifier, RecipeSerializer<?>> r) {
        r.accept(id("alloying"), ALLOYING_SERIALIZER);
        r.accept(id("extruding"), EXTRUDING_SERIALIZER);
        r.accept(id("rolling"), ROLLING_SERIALIZER);
    }
}

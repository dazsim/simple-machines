package com.workshopcraft.simplemachines;

import com.workshopcraft.simplemachines.machine.AlloyFurnaceBlock;
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
import net.minecraft.core.registries.Registries;
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
import java.util.List;
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

    public static Block alloyFurnace;
    public static Block generator;
    public static Block rollingMill;
    public static Block extruder;
    public static Block steelChassis;
    public static BlockEntityType<AlloyFurnaceBlockEntity> alloyFurnaceEntity;
    public static BlockEntityType<ExtruderBlockEntity> extruderEntity;
    public static BlockEntityType<GeneratorBlockEntity> generatorEntity;
    public static BlockEntityType<RollingMillBlockEntity> rollingMillEntity;
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

    public static void registerBlocks(BiConsumer<Identifier, Block> r) {
        alloyFurnace = new AlloyFurnaceBlock(machine("alloy_furnace"));
        r.accept(id("alloy_furnace"), alloyFurnace);
        extruder = new ExtruderBlock(machine("extruder").mapColor(MapColor.METAL).sound(SoundType.METAL));
        r.accept(id("extruder"), extruder);
        generator = new GeneratorBlock(machine("solid_fuel_generator").mapColor(MapColor.METAL).sound(SoundType.METAL));
        r.accept(id("solid_fuel_generator"), generator);
        rollingMill = new RollingMillBlock(machine("rolling_mill").mapColor(MapColor.METAL).sound(SoundType.METAL));
        r.accept(id("rolling_mill"), rollingMill);
        steelChassis = new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id("steel_chassis")))
                .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0F).sound(SoundType.METAL)
                .noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
        r.accept(id("steel_chassis"), steelChassis);
    }

    public static void registerItems(BiConsumer<Identifier, Item> r) {
        for (String name : List.of("steel_ingot", "redstone_copper_ingot", "iron_rod", "copper_rod", "steel_rod")) {
            add(r, name, new Item(props(name)));
        }
        add(r, "alloy_furnace", new BlockItem(alloyFurnace, props("alloy_furnace").useBlockDescriptionPrefix()));
        add(r, "extruder", new BlockItem(extruder, props("extruder").useBlockDescriptionPrefix()));
        add(r, "solid_fuel_generator", new BlockItem(generator, props("solid_fuel_generator").useBlockDescriptionPrefix()));
        add(r, "rolling_mill", new BlockItem(rollingMill, props("rolling_mill").useBlockDescriptionPrefix()));
        add(r, "steel_chassis", new BlockItem(steelChassis, props("steel_chassis").useBlockDescriptionPrefix()));
    }

    private static Item.Properties props(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    private static void add(BiConsumer<Identifier, Item> r, String name, Item item) {
        r.accept(id(name), item);
        TAB.add(item);
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
    }

    public static void registerMenus(BiConsumer<Identifier, MenuType<?>> r) {
        r.accept(id("alloy_furnace"), ALLOY_MENU);
        r.accept(id("extruder"), EXTRUDER_MENU);
        r.accept(id("solid_fuel_generator"), GENERATOR_MENU);
        r.accept(id("rolling_mill"), ROLLING_MILL_MENU);
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

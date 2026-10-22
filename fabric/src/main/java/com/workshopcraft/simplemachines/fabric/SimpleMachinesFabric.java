package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.function.BiConsumer;

public class SimpleMachinesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMachines.registerBlocks(direct(BuiltInRegistries.BLOCK));
        SimpleMachines.registerItems(direct(BuiltInRegistries.ITEM));
        SimpleMachines.registerTabs(direct(BuiltInRegistries.CREATIVE_MODE_TAB));
        SimpleMachines.registerBlockEntities(direct(BuiltInRegistries.BLOCK_ENTITY_TYPE));
        SimpleMachines.registerMenus(direct(BuiltInRegistries.MENU));
        SimpleMachines.registerRecipeTypes(direct(BuiltInRegistries.RECIPE_TYPE));
        SimpleMachines.registerRecipeSerializers(direct(BuiltInRegistries.RECIPE_SERIALIZER));
        ItemStorage.SIDED.registerForBlockEntity((be, side) -> ContainerStorage.of(be, side), SimpleMachines.alloyFurnaceEntity);
        ItemStorage.SIDED.registerForBlockEntity((be, side) -> ContainerStorage.of(be, side), SimpleMachines.extruderEntity);
    }

    private static <T> BiConsumer<Identifier, T> direct(Registry<T> registry) {
        return (id, value) -> Registry.register(registry, id, value);
    }
}

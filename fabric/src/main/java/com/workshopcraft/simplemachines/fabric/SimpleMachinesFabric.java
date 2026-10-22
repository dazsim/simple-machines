package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;

public class SimpleMachinesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMachines.registerComponents(direct(BuiltInRegistries.DATA_COMPONENT_TYPE));
        SimpleMachines.registerBlocks(direct(BuiltInRegistries.BLOCK));
        SimpleMachines.registerItems(direct(BuiltInRegistries.ITEM));
        SimpleMachines.registerTabs(direct(BuiltInRegistries.CREATIVE_MODE_TAB));
        SimpleMachines.registerBlockEntities(direct(BuiltInRegistries.BLOCK_ENTITY_TYPE));
        SimpleMachines.registerMenus(direct(BuiltInRegistries.MENU));
        SimpleMachines.registerRecipeTypes(direct(BuiltInRegistries.RECIPE_TYPE));
        SimpleMachines.registerRecipeSerializers(direct(BuiltInRegistries.RECIPE_SERIALIZER));
        ItemStorage.SIDED.registerForBlockEntity((be, side) -> ContainerStorage.of(be, side), SimpleMachines.generatorEntity);
        ItemStorage.SIDED.registerForBlockEntity((be, side) -> ContainerStorage.of(be, side), SimpleMachines.rollingMillEntity);
        EnergyStorage.SIDED.registerForBlockEntity((be, side) -> new BufferEnergyStorage(be.energy()), SimpleMachines.generatorEntity);
        EnergyStorage.SIDED.registerForBlockEntity((be, side) -> new BufferEnergyStorage(be.energy()), SimpleMachines.rollingMillEntity);
        ItemStorage.SIDED.registerForBlockEntity((be, side) -> ContainerStorage.of(be, side), SimpleMachines.electricFurnaceEntity);
        EnergyStorage.SIDED.registerForBlockEntity((be, side) -> new BufferEnergyStorage(be.energy()), SimpleMachines.electricFurnaceEntity);
        EnergyStorage.SIDED.registerForBlocks((level, pos, state, be, side) ->
                level instanceof ServerLevel server ? new CableEnergyStorage(server, pos, side) : null, SimpleMachines.cable);
        EnergyBuffer.lookup = (level, pos, side) -> EnergyStorage.SIDED.find(level, pos, side) != null;
        EnergyBuffer.pusher = (level, pos, side, amount) -> {
            final EnergyStorage s = EnergyStorage.SIDED.find(level, pos, side);
            if (s == null || !s.supportsInsertion()) return 0;
            try (Transaction tx = Transaction.openOuter()) {
                final long n = s.insert(amount, tx);
                tx.commit();
                return (int) n;
            }
        };
    }

    private static <T> BiConsumer<Identifier, T> direct(Registry<T> registry) {
        return (id, value) -> Registry.register(registry, id, value);
    }
}

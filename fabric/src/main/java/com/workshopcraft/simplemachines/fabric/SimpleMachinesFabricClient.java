package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.client.EnergyScreen;
import com.workshopcraft.simplemachines.client.MachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.workshopcraft.simplemachines.client.ToolEffects;
import com.workshopcraft.simplemachines.client.WorktopRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.client.PoweredScreen;
import com.workshopcraft.simplemachines.machine.PoweredMenu;
import com.workshopcraft.simplemachines.machine.WorkBlockEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class SimpleMachinesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ToolEffects::tick);
        MenuScreens.register(SimpleMachines.ALLOY_MENU, MachineScreen::new);
        MenuScreens.register(SimpleMachines.EXTRUDER_MENU, MachineScreen::new);
        MenuScreens.register(SimpleMachines.GENERATOR_MENU, EnergyScreen::new);
        MenuScreens.register(SimpleMachines.ROLLING_MILL_MENU, EnergyScreen::new);
        MenuScreens.register(SimpleMachines.ELECTRIC_FURNACE_MENU, EnergyScreen::new);
        for (MenuType<PoweredMenu> m : SimpleMachines.MENUS.values()) MenuScreens.register(m, PoweredScreen::new);
        for (Core core : SimpleMachines.NEW_CORES) renderer(SimpleMachines.MACHINE_ENTITIES.get(core));
        BlockEntityRenderers.register(SimpleMachines.chassisEntity, WorktopRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.conduitEntity, com.workshopcraft.simplemachines.client.ConduitRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.generatorEntity, WorktopRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.rollingMillEntity, WorktopRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.electricFurnaceEntity, WorktopRenderer::new);
    }

    @SuppressWarnings("unchecked")
    private static <T extends WorkBlockEntity> void renderer(BlockEntityType<?> type) {
        BlockEntityRenderers.register((BlockEntityType<T>) type, WorktopRenderer::new);
    }
}

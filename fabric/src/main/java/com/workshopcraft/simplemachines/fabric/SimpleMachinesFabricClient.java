package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.client.EnergyScreen;
import com.workshopcraft.simplemachines.client.MachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class SimpleMachinesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(SimpleMachines.ALLOY_MENU, MachineScreen::new);
        MenuScreens.register(SimpleMachines.EXTRUDER_MENU, MachineScreen::new);
        MenuScreens.register(SimpleMachines.GENERATOR_MENU, EnergyScreen::new);
        MenuScreens.register(SimpleMachines.ROLLING_MILL_MENU, EnergyScreen::new);
        MenuScreens.register(SimpleMachines.ELECTRIC_FURNACE_MENU, EnergyScreen::new);
    }
}

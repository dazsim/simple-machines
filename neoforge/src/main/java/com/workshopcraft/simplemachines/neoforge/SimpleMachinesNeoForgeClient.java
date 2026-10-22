package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.client.EnergyScreen;
import com.workshopcraft.simplemachines.client.MachineScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = SimpleMachines.MODID, dist = Dist.CLIENT)
public class SimpleMachinesNeoForgeClient {
    public SimpleMachinesNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(SimpleMachines.ALLOY_MENU, MachineScreen::new);
            e.register(SimpleMachines.EXTRUDER_MENU, MachineScreen::new);
            e.register(SimpleMachines.GENERATOR_MENU, EnergyScreen::new);
            e.register(SimpleMachines.ROLLING_MILL_MENU, EnergyScreen::new);
            e.register(SimpleMachines.ELECTRIC_FURNACE_MENU, EnergyScreen::new);
        });
    }
}

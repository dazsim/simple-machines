package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

@Mod(SimpleMachines.MODID)
public class SimpleMachinesNeoForge {
    public SimpleMachinesNeoForge(IEventBus modBus) {
        modBus.addListener(SimpleMachinesNeoForge::onRegister);
        modBus.addListener(SimpleMachinesNeoForge::onCapabilities);
    }

    private static void onRegister(RegisterEvent e) {
        e.register(Registries.BLOCK, h -> SimpleMachines.registerBlocks(h::register));
        e.register(Registries.ITEM, h -> SimpleMachines.registerItems(h::register));
        e.register(Registries.CREATIVE_MODE_TAB, h -> SimpleMachines.registerTabs(h::register));
        e.register(Registries.BLOCK_ENTITY_TYPE, h -> SimpleMachines.registerBlockEntities(h::register));
        e.register(Registries.MENU, h -> SimpleMachines.registerMenus(h::register));
        e.register(Registries.RECIPE_TYPE, h -> SimpleMachines.registerRecipeTypes(h::register));
        e.register(Registries.RECIPE_SERIALIZER, h -> SimpleMachines.registerRecipeSerializers(h::register));
    }

    private static void onCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.Item.BLOCK, SimpleMachines.alloyFurnaceEntity, WorldlyContainerWrapper::new);
        e.registerBlockEntity(Capabilities.Item.BLOCK, SimpleMachines.extruderEntity, WorldlyContainerWrapper::new);
    }
}

package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.client.EnergyScreen;
import com.workshopcraft.simplemachines.client.MachineScreen;
import net.neoforged.api.distmarker.Dist;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.client.PoweredScreen;
import com.workshopcraft.simplemachines.machine.PoweredMenu;
import com.workshopcraft.simplemachines.machine.WorkBlockEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.Minecraft;
import com.workshopcraft.simplemachines.client.ToolEffects;
import net.neoforged.fml.common.Mod;
import com.workshopcraft.simplemachines.client.WorktopRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = SimpleMachines.MODID, dist = Dist.CLIENT)
public class SimpleMachinesNeoForgeClient {
    public SimpleMachinesNeoForgeClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ToolEffects.tick(Minecraft.getInstance()));
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            for (MenuType<PoweredMenu> m : SimpleMachines.MENUS.values()) e.register(m, PoweredScreen::new);
            e.register(SimpleMachines.ALLOY_MENU, MachineScreen::new);
            e.register(SimpleMachines.EXTRUDER_MENU, MachineScreen::new);
            e.register(SimpleMachines.GENERATOR_MENU, EnergyScreen::new);
            e.register(SimpleMachines.ROLLING_MILL_MENU, EnergyScreen::new);
            e.register(SimpleMachines.ELECTRIC_FURNACE_MENU, EnergyScreen::new);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            for (Core core : SimpleMachines.NEW_CORES) renderer(e, SimpleMachines.MACHINE_ENTITIES.get(core));
            e.registerBlockEntityRenderer(SimpleMachines.chassisEntity, WorktopRenderer::new);
            e.registerBlockEntityRenderer(SimpleMachines.conduitEntity, com.workshopcraft.simplemachines.client.ConduitRenderer::new);
            e.registerBlockEntityRenderer(SimpleMachines.generatorEntity, WorktopRenderer::new);
            e.registerBlockEntityRenderer(SimpleMachines.rollingMillEntity, WorktopRenderer::new);
            e.registerBlockEntityRenderer(SimpleMachines.electricFurnaceEntity, WorktopRenderer::new);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends WorkBlockEntity> void renderer(EntityRenderersEvent.RegisterRenderers e, BlockEntityType<?> type) {
        e.registerBlockEntityRenderer((BlockEntityType<T>) type, WorktopRenderer::new);
    }
}

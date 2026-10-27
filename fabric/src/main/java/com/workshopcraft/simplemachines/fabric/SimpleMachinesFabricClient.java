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
        com.workshopcraft.simplemachines.compat.jei.ViewerRecipes.source = SimpleMachinesFabricClient::synced;
        com.workshopcraft.simplemachines.client.ToolRunning.registerDirect(SimpleMachines.id("tool_running"));
        net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(SimpleMachines.HOT_SPARK,
                com.workshopcraft.simplemachines.client.HotSparkParticle::provider);
        net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(SimpleMachines.WHITE_SPARK,
                com.workshopcraft.simplemachines.client.HotSparkParticle::whiteProvider);
        com.workshopcraft.simplemachines.client.ToolSpin.registerDirect(SimpleMachines.id("spin"));
        com.workshopcraft.simplemachines.client.TankItemRenderer.Unbaked.registerDirect(SimpleMachines.id("tank_fluid"));
        MenuScreens.register(SimpleMachines.ALLOY_MENU, MachineScreen::new);
        MenuScreens.register(SimpleMachines.FILTER_MENU, com.workshopcraft.simplemachines.client.FilterScreen::new);
        MenuScreens.register(SimpleMachines.CRAFTER_MENU, com.workshopcraft.simplemachines.client.AutoCrafterScreen::new);
        MenuScreens.register(SimpleMachines.EXTRUDER_MENU, MachineScreen::new);
        MenuScreens.register(SimpleMachines.GENERATOR_MENU, EnergyScreen::new);
        MenuScreens.register(SimpleMachines.ROLLING_MILL_MENU, EnergyScreen::new);
        MenuScreens.register(SimpleMachines.ELECTRIC_FURNACE_MENU, EnergyScreen::new);
        for (MenuType<PoweredMenu> m : SimpleMachines.MENUS.values()) MenuScreens.register(m, PoweredScreen::new);
        for (Core core : SimpleMachines.NEW_CORES) renderer(SimpleMachines.MACHINE_ENTITIES.get(core));
        BlockEntityRenderers.register(SimpleMachines.chassisEntity, WorktopRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.conduitEntity, com.workshopcraft.simplemachines.client.ConduitRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.tankEntity, com.workshopcraft.simplemachines.client.TankRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.toolStandEntity, com.workshopcraft.simplemachines.client.ToolStandRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.generatorEntity, WorktopRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.rollingMillEntity, WorktopRenderer::new);
        BlockEntityRenderers.register(SimpleMachines.electricFurnaceEntity, WorktopRenderer::new);
    }

    @SuppressWarnings("unchecked")
    private static <T extends WorkBlockEntity> void renderer(BlockEntityType<?> type) {
        BlockEntityRenderers.register((BlockEntityType<T>) type, WorktopRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static java.util.Collection<? extends net.minecraft.world.item.crafting.RecipeHolder<?>> synced(net.minecraft.world.item.crafting.RecipeType<?> type) {
        final var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return java.util.List.of();
        return level.recipeAccess().getSynchronizedRecipes().getAllOfType((net.minecraft.world.item.crafting.RecipeType) type);
    }
}

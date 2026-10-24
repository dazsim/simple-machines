package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.workshopcraft.simplemachines.assembly.Assembly;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@Mod(SimpleMachines.MODID)
public class SimpleMachinesNeoForge {
    public SimpleMachinesNeoForge(IEventBus modBus) {
        modBus.addListener(SimpleMachinesNeoForge::onRegister);
        modBus.addListener(SimpleMachinesNeoForge::onCapabilities);
        NeoForge.EVENT_BUS.addListener(SimpleMachinesNeoForge::onUseBlock);
        EnergyBuffer.pusher = (level, pos, side, amount) -> {
            final EnergyHandler h = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (h == null) return 0;
            try (Transaction tx = Transaction.openRoot()) {
                final int n = h.insert(amount, tx);
                tx.commit();
                return n;
            }
        };
        EnergyBuffer.lookup = (level, pos, side) -> level.getCapability(Capabilities.Energy.BLOCK, pos, side) != null;
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (Assembly.screwdriver(e.getLevel(), e.getPos(), e.getEntity(), e.getItemStack())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onRegister(RegisterEvent e) {
        e.register(Registries.DATA_COMPONENT_TYPE, h -> SimpleMachines.registerComponents(h::register));
        e.register(Registries.BLOCK, h -> SimpleMachines.registerBlocks(h::register));
        e.register(Registries.ITEM, h -> SimpleMachines.registerItems(h::register));
        e.register(Registries.CREATIVE_MODE_TAB, h -> SimpleMachines.registerTabs(h::register));
        e.register(Registries.BLOCK_ENTITY_TYPE, h -> SimpleMachines.registerBlockEntities(h::register));
        e.register(Registries.MENU, h -> SimpleMachines.registerMenus(h::register));
        e.register(Registries.RECIPE_TYPE, h -> SimpleMachines.registerRecipeTypes(h::register));
        e.register(Registries.RECIPE_SERIALIZER, h -> SimpleMachines.registerRecipeSerializers(h::register));
    }

    private static void onCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.Item.BLOCK, SimpleMachines.generatorEntity, WorldlyContainerWrapper::new);
        e.registerBlockEntity(Capabilities.Item.BLOCK, SimpleMachines.rollingMillEntity, WorldlyContainerWrapper::new);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, SimpleMachines.generatorEntity, (be, side) -> new BufferEnergyHandler(be.energy()));
        e.registerBlockEntity(Capabilities.Energy.BLOCK, SimpleMachines.rollingMillEntity, (be, side) -> new BufferEnergyHandler(be.energy()));
        e.registerBlockEntity(Capabilities.Item.BLOCK, SimpleMachines.electricFurnaceEntity, WorldlyContainerWrapper::new);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, SimpleMachines.electricFurnaceEntity, (be, side) -> new BufferEnergyHandler(be.energy()));
        e.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) ->
                level instanceof ServerLevel server ? new CableEnergyHandler(server, pos, side) : null, SimpleMachines.cable);
    }
}

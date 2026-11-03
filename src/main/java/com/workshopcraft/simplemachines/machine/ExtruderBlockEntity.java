package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.recipe.ExtrudingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class ExtruderBlockEntity extends MachineBlockEntity<ExtrudingRecipe> {
    public ExtruderBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.extruderEntity, pos, state, 1, SimpleMachines.EXTRUDING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.extruder");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new MachineMenu(SimpleMachines.EXTRUDER_MENU, id, inventory, this, data(), 1);
    }
}

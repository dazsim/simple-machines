package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.recipe.AlloyingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class AlloyFurnaceBlockEntity extends MachineBlockEntity<AlloyingRecipe> {
    public AlloyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.alloyFurnaceEntity, pos, state, 2, SimpleMachines.ALLOYING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.alloy_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new MachineMenu(SimpleMachines.ALLOY_MENU, id, inventory, this, data(), 2);
    }
}

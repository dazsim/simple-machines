package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** 27 slots that live on the channel, not in the block, so every buffer on the code sees the same items. */
public class EnderBufferBlockEntity extends EnderDeviceBlockEntity {
    public EnderBufferBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.BUFFER), pos, state, EnderData.SLOTS, EnderData.BUFFER);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        final EnderData.Channel c = channel();
        return c == null ? items : c.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return online && EnderBackpackItem.fitsBuffer(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return online;
    }

    /** The channel's items stay on the channel when the block goes. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.ender_buffer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EnderBufferMenu(id, inventory, this, menuData);
    }
}

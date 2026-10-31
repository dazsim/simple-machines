package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** 27 slots that live on the channel, not in the block, so every buffer on the code sees the same items. */
public class EntangledBufferBlockEntity extends EnderDeviceBlockEntity {
    public EntangledBufferBlockEntity(BlockPos pos, BlockState state) {
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

    /** The block never keeps items of its own: with no channel to put them on they fall out. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && level instanceof ServerLevel server && channel() == null) {
            spill(server, worldPosition, stack);
            return;
        }
        super.setItem(slot, stack);
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        super.work(level, pos, state, tier);
        final EnderData.Channel c = channel();
        if (c == null) return;
        boolean moved = false;
        for (int i = 0; i < items.size(); i++) {
            final ItemStack stray = items.get(i);
            if (stray.isEmpty()) continue;
            items.set(i, ItemStack.EMPTY);
            moved = true;
            final ItemStack left = EntangledBackpackItem.fitsBuffer(stray) ? merge(c.items, stray) : stray;
            if (!left.isEmpty()) spill(level, pos, left);
        }
        if (moved) setChanged();
    }

    private static ItemStack merge(NonNullList<ItemStack> into, ItemStack stack) {
        for (ItemStack s : into) {
            if (stack.isEmpty()) break;
            if (s.isEmpty() || !ItemStack.isSameItemSameComponents(s, stack)) continue;
            final int n = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
            if (n > 0) {
                s.grow(n);
                stack.shrink(n);
            }
        }
        for (int i = 0; i < into.size() && !stack.isEmpty(); i++) if (into.get(i).isEmpty()) {
            into.set(i, stack.copy());
            stack.setCount(0);
        }
        return stack;
    }

    private static void spill(ServerLevel level, BlockPos pos, ItemStack stack) {
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return online && EntangledBackpackItem.fitsBuffer(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return online && exposed();
    }

    /** The channel's items stay on the channel when the block goes; anything left in the block itself drops. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) Containers.dropContents(level, pos, items);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.entangled_buffer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EntangledBufferMenu(id, inventory, this, menuData);
    }
}

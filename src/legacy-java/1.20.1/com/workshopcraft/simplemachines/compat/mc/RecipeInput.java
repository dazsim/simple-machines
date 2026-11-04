package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Stand-in for the 1.21 RecipeInput: a read-only Container for 1.20.1 recipes. */
public interface RecipeInput extends Container {
    ItemStack getItem(int slot);

    int size();

    default int getContainerSize() {
        return size();
    }

    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }

    default ItemStack removeItem(int slot, int count) {
        return ItemStack.EMPTY;
    }

    default ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    default void setItem(int slot, ItemStack stack) {
    }

    default void setChanged() {
    }

    default boolean stillValid(Player player) {
        return true;
    }

    default void clearContent() {
    }
}

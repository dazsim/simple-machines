package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Stand-in for the 1.21 SingleRecipeInput. */
public class SingleRecipeInput extends SimpleContainer {
    public SingleRecipeInput(ItemStack stack) {
        super(stack);
    }

    public ItemStack item() {
        return getItem(0);
    }

    public int size() {
        return getContainerSize();
    }
}

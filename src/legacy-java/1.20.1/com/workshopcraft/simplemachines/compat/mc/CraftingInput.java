package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 1.21 CraftingInput as a detached 1.20.1 crafting grid. */
public class CraftingInput extends TransientCraftingContainer {
    private CraftingInput(int width, int height, NonNullList<ItemStack> items) {
        super(null, width, height, items);
    }

    public static CraftingInput of(int width, int height, List<ItemStack> items) {
        final NonNullList<ItemStack> list = NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int i = 0; i < list.size() && i < items.size(); i++) list.set(i, items.get(i));
        return new CraftingInput(width, height, list);
    }

    public int size() {
        return getContainerSize();
    }

    public int width() {
        return getWidth();
    }

    public int height() {
        return getHeight();
    }

    public ItemStack getItem(int x, int y) {
        return getItem(x + y * getWidth());
    }

    public int ingredientCount() {
        int n = 0;
        for (ItemStack s : getItems()) if (!s.isEmpty()) n++;
        return n;
    }
}

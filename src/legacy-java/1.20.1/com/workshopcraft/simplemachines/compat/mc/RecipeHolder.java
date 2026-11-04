package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

/** Stand-in for the 1.20.2+ RecipeHolder over a 1.20.1 recipe, which carries its own id. */
public record RecipeHolder<T extends Recipe<?>>(ResourceLocation id, T value) {
    public RecipeHolder(T value) {
        this(value.getId(), value);
    }
}

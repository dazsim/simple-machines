package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class CrafterBlock {
    private CrafterBlock() {
    }

    public static Optional<RecipeHolder<CraftingRecipe>> getPotentialResults(Level level, CraftingInput input) {
        return RecipeCheck.find(level.getRecipeManager(), RecipeType.CRAFTING, input, level);
    }
}

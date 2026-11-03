package com.workshopcraft.simplemachines.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;

import java.util.List;

public interface MachineRecipe extends Recipe<MachineInput> {
    int cookTime();

    /** The ingredient for each input slot, in slot order, for the given input. */
    List<CountedIngredient> matched(MachineInput input);

    List<CountedIngredient> ingredients();

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.FURNACE_MISC;
    }

    default boolean usesItem(ItemStack stack) {
        for (CountedIngredient c : ingredients()) {
            if (c.ingredient().test(stack)) return true;
        }
        return false;
    }
}

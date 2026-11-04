package com.workshopcraft.simplemachines.compat.mc.jei;

import com.workshopcraft.simplemachines.compat.mc.RecipeHolder;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public final class JeiTypes {
    private JeiTypes() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Recipe<?>> RecipeType<RecipeHolder<T>> holder(ResourceLocation id) {
        return new RecipeType<>(id, (Class) RecipeHolder.class);
    }
}

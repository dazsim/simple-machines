package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.recipe.ExtrudingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.world.item.crafting.RecipeHolder;

class ExtrudingCategory extends Base<RecipeHolder<ExtrudingRecipe>> {
    static final IRecipeHolderType<ExtrudingRecipe> TYPE = IRecipeHolderType.create(SimpleMachines.id("extruding"));

    ExtrudingCategory(IGuiHelper gui) {
        super(TYPE, "extruding", SimpleMachines.extruder, gui, 140, 1);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<ExtrudingRecipe> holder, IFocusGroup focuses) {
        input(b, 1, holder.value().input());
        output(b, 66).add(holder.value().result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, RecipeHolder<ExtrudingRecipe> holder, IFocusGroup focuses) {
        final int t = holder.value().cookTime() * 5 / 4;
        arrow(b, 28, t);
        line(b, 0, JeiText.line(SimpleMachines.extruder.getName(), JeiText.fuel(t)));
    }
}

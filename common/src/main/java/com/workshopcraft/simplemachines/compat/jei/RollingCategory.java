package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.RollingMillBlockEntity;
import com.workshopcraft.simplemachines.recipe.RollingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

class RollingCategory extends Base<RecipeHolder<RollingRecipe>> {
    static final IRecipeHolderType<RollingRecipe> TYPE = IRecipeHolderType.create(SimpleMachines.id("rolling"));

    RollingCategory(IGuiHelper gui) {
        super(TYPE, "rolling", SimpleMachines.rollingMill, gui, 150, 2);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<RollingRecipe> holder, IFocusGroup focuses) {
        input(b, 1, holder.value().input());
        output(b, 66).add(holder.value().result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, RecipeHolder<RollingRecipe> holder, IFocusGroup focuses) {
        final RollingRecipe r = holder.value();
        final int pt = RollingMillBlockEntity.PER_TICK;
        arrow(b, 28, r.cookTime());
        line(b, 0, Component.translatable(r.sheet() ? "jei.simplemachines.mode_sheets" : "jei.simplemachines.mode_rods"),
                Component.translatable("jei.simplemachines.mode_tip"));
        line(b, 1, JeiText.powered(SimpleMachines.rollingMill.getName(), r.cookTime(), pt), JeiText.perTick(pt), JeiText.steel(r.cookTime(), pt));
    }
}

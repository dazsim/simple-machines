package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.machine.AlloySmelterBlockEntity;
import com.workshopcraft.simplemachines.recipe.AlloyingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.world.item.crafting.RecipeHolder;

class AlloyingCategory extends Base<RecipeHolder<AlloyingRecipe>> {
    static final IRecipeHolderType<AlloyingRecipe> TYPE = IRecipeHolderType.create(SimpleMachines.id("alloying"));

    AlloyingCategory(IGuiHelper gui) {
        super(TYPE, "alloying", SimpleMachines.alloyFurnace, gui, 176, 2);
    }

    static int furnaceTicks(AlloyingRecipe r) {
        return r.cookTime() * 5 / 4;
    }

    static int smelterTicks(AlloyingRecipe r) {
        return Math.max(1, r.cookTime() / 2);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
        final AlloyingRecipe r = holder.value();
        input(b, 1, r.first());
        input(b, 19, r.second());
        output(b, 84).add(r.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
        final AlloyingRecipe r = holder.value();
        arrow(b, 46, furnaceTicks(r));
        line(b, 0, JeiText.line(SimpleMachines.alloyFurnace.getName(), JeiText.fuel(furnaceTicks(r))));
        final int t = smelterTicks(r);
        final int pt = AlloySmelterBlockEntity.PER_TICK;
        line(b, 1, JeiText.powered(SimpleMachines.MACHINES.get(Core.ALLOY).getName(), t, pt), JeiText.perTick(pt), JeiText.steel(t, pt));
    }
}

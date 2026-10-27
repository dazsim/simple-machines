package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Only loaded by JEI, so the mod runs without it. */
@JeiPlugin
public class SimpleMachinesJei implements IModPlugin {
    private static final List<Core> SOURCES = List.of(Core.STONE, Core.WATER, Core.HIVE);

    @Override
    public Identifier getPluginUid() {
        return SimpleMachines.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration r) {
        final IGuiHelper gui = r.getJeiHelpers().getGuiHelper();
        r.addRecipeCategories(new AlloyingCategory(gui), new RollingCategory(gui), new ExtrudingCategory(gui),
                new ProcessCategory(gui, ProcessCategory.CRUSHING, "crushing", Core.CRUSHER),
                new ProcessCategory(gui, ProcessCategory.SAWING, "sawing", Core.SAW),
                new GeneratorCategory(gui));
        for (Core c : SOURCES) r.addRecipeCategories(new SourceCategory(gui, SourceCategory.type(c), c));
    }

    @Override
    public void registerRecipes(IRecipeRegistration r) {
        r.addRecipes(AlloyingCategory.TYPE, ViewerRecipes.get(SimpleMachines.ALLOYING));
        r.addRecipes(RollingCategory.TYPE, ViewerRecipes.get(SimpleMachines.ROLLING));
        r.addRecipes(ExtrudingCategory.TYPE, ViewerRecipes.get(SimpleMachines.EXTRUDING));
        r.addRecipes(ProcessCategory.CRUSHING, ViewerRecipes.get(SimpleMachines.CRUSHING).stream().filter(h -> !h.value().output().isEmpty()).toList());
        r.addRecipes(ProcessCategory.SAWING, ViewerRecipes.get(SimpleMachines.SAWING).stream().filter(h -> !h.value().output().isEmpty()).toList());
        r.addRecipes(GeneratorCategory.TYPE, GeneratorCategory.fuels());
        for (Core c : SOURCES) r.addRecipes(SourceCategory.type(c), List.of(SourceCategory.make(c)));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
        r.addCraftingStation(AlloyingCategory.TYPE, SimpleMachines.alloyFurnace, SimpleMachines.MACHINES.get(Core.ALLOY));
        r.addCraftingStation(RollingCategory.TYPE, SimpleMachines.rollingMill);
        r.addCraftingStation(ExtrudingCategory.TYPE, SimpleMachines.extruder);
        r.addCraftingStation(ProcessCategory.CRUSHING, SimpleMachines.MACHINES.get(Core.CRUSHER));
        r.addCraftingStation(ProcessCategory.SAWING, SimpleMachines.MACHINES.get(Core.SAW));
        r.addCraftingStation(GeneratorCategory.TYPE, SimpleMachines.generator);
        for (Core c : SOURCES) r.addCraftingStation(SourceCategory.type(c), SimpleMachines.MACHINES.get(c));
        r.addCraftingStation(RecipeTypes.SMELTING, SimpleMachines.electricFurnace);
    }
}

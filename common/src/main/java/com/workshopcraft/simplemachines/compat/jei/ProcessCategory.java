package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.machine.ProcessBlockEntity;
import com.workshopcraft.simplemachines.recipe.ProcessRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Crusher and sawmill: one input, one output and a chance of one more on a steel chassis. */
class ProcessCategory extends Base<RecipeHolder<ProcessRecipe>> {
    static final IRecipeHolderType<ProcessRecipe> CRUSHING = IRecipeHolderType.create(SimpleMachines.id("crushing"));
    static final IRecipeHolderType<ProcessRecipe> SAWING = IRecipeHolderType.create(SimpleMachines.id("sawing"));
    private final Core core;

    ProcessCategory(IGuiHelper gui, IRecipeHolderType<ProcessRecipe> type, String key, Core core) {
        super(type, key, SimpleMachines.MACHINES.get(core), gui, 176, 2);
        this.core = core;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<ProcessRecipe> holder, IFocusGroup focuses) {
        final ProcessRecipe r = holder.value();
        b.addInputSlot(1, ROW).setStandardSlotBackground().add(r.ingredient());
        final ItemStack out = r.output();
        output(b, 66).add(out);
        if (r.bonus() > 0 && !out.isEmpty()) {
            final String pct = percent(r.bonus());
            b.addOutputSlot(100, ROW).setStandardSlotBackground().add(out.copyWithCount(1))
                    .addRichTooltipCallback((view, tip) -> {
                        tip.add(Component.translatable("jei.simplemachines.bonus", pct));
                        tip.add(Component.translatable("jei.simplemachines.bonus_tip"));
                    });
        }
    }

    static String percent(float chance) {
        final float p = chance * 100;
        return p == Math.round(p) ? Integer.toString(Math.round(p)) : String.format("%.1f", p);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, RecipeHolder<ProcessRecipe> holder, IFocusGroup focuses) {
        final ProcessRecipe r = holder.value();
        final int pt = ProcessBlockEntity.PER_TICK;
        arrow(b, 28, r.cookTime());
        if (r.bonus() > 0) {
            b.addText(Component.literal(percent(r.bonus()) + "%"), 28, 10).setColor(TEXT).setPosition(121, ROW + 4)
                    .setTooltip(Component.translatable("jei.simplemachines.bonus_tip"));
        }
        line(b, 0, JeiText.powered(SimpleMachines.MACHINES.get(core).getName(), r.cookTime(), pt), JeiText.perTick(pt), JeiText.steel(r.cookTime(), pt));
        if (r.bonus() > 0) line(b, 1, Component.translatable("jei.simplemachines.bonus", percent(r.bonus())), Component.translatable("jei.simplemachines.bonus_tip"));
    }
}

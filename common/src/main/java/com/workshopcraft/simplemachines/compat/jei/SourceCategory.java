package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Stone generator, water generator and mechanical hive: they make things from what is around them. */
class SourceCategory extends Base<SourceCategory.Make> {
    record Make(List<ItemStack> inputs, List<ItemStack> outputs, int ticks, int perTick, String note) {
    }

    private final Core core;

    SourceCategory(IGuiHelper gui, IRecipeType<Make> type, Core core) {
        super(type, core.block, SimpleMachines.MACHINES.get(core), gui, 176, 2);
        this.core = core;
    }

    static IRecipeType<Make> type(Core core) {
        return IRecipeType.create(SimpleMachines.id(core.block), Make.class);
    }

    static Make make(Core core) {
        return switch (core) {
            case STONE -> new Make(List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.LAVA_BUCKET)),
                    List.of(new ItemStack(Items.COBBLESTONE)), 20, 10, "stone_note");
            case WATER -> new Make(List.of(new ItemStack(Items.BUCKET)), List.of(new ItemStack(Items.WATER_BUCKET)), 40, 10, "water_note");
            default -> new Make(List.of(new ItemStack(Items.GLASS_BOTTLE)),
                    List.of(new ItemStack(Items.HONEYCOMB), new ItemStack(Items.HONEY_BOTTLE)), 400, 20, "hive_note");
        };
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Make m, IFocusGroup focuses) {
        final boolean beside = core == Core.STONE;
        for (int i = 0; i < m.inputs().size(); i++) {
            final var slot = beside ? b.addSlot(mezz.jei.api.recipe.RecipeIngredientRole.CRAFTING_STATION, 1 + i * 18, ROW)
                    : b.addInputSlot(1 + i * 18, ROW);
            slot.setStandardSlotBackground().add(m.inputs().get(i));
            if (beside) slot.addRichTooltipCallback((v, tip) -> tip.add(Component.translatable("jei.simplemachines.beside")));
        }
        for (int i = 0; i < m.outputs().size(); i++) {
            (i == 0 ? b.addOutputSlot(76, ROW).setOutputSlotBackground() : b.addOutputSlot(104, ROW).setStandardSlotBackground())
                    .add(m.outputs().get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Make m, IFocusGroup focuses) {
        arrow(b, 42, m.ticks());
        line(b, 0, JeiText.powered(SimpleMachines.MACHINES.get(core).getName(), m.ticks(), m.perTick()),
                JeiText.perTick(m.perTick()), JeiText.steel(m.ticks(), m.perTick()));
        line(b, 1, Component.translatable("jei.simplemachines." + m.note()));
    }
}

package com.workshopcraft.simplemachines.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import com.workshopcraft.simplemachines.recipe.CountedIngredient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/** Slots along the top, an arrow, then lines of text underneath. */
abstract class Base<T> extends AbstractRecipeCategory<T> {
    static final int TEXT = 0xFF404040;
    static final int ROW = 5;
    static final int LINES = 30;

    Base(IRecipeType<T> type, String key, ItemLike icon, IGuiHelper gui, int width, int lines) {
        super(type, Component.translatable("jei.simplemachines." + key), gui.createDrawableItemLike(icon), width, LINES + lines * 10);
    }

    static IRecipeSlotBuilder input(IRecipeLayoutBuilder b, int x, CountedIngredient c) {
        return b.addInputSlot(x, ROW).setStandardSlotBackground().addItemStacks(stacks(c));
    }

    static List<ItemStack> stacks(CountedIngredient c) {
        return c.ingredient().items().map(h -> new ItemStack(h, c.count())).toList();
    }

    static IRecipeSlotBuilder output(IRecipeLayoutBuilder b, int x) {
        return b.addOutputSlot(x, ROW).setOutputSlotBackground();
    }

    static void arrow(IRecipeExtrasBuilder b, int x, int ticks) {
        b.addAnimatedRecipeArrowWidget(Math.max(20, ticks)).setPosition(x, ROW);
    }

    /** One line of text per row under the slots, each with an optional tooltip. */
    void line(IRecipeExtrasBuilder b, int row, Component text, Component... tooltip) {
        final var w = b.addText(text, getWidth(), 10).setColor(TEXT).setPosition(0, LINES + row * 10);
        if (tooltip.length > 0) w.setTooltip(List.of(tooltip));
    }
}

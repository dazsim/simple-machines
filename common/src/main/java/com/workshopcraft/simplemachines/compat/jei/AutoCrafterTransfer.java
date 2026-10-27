package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.client.AutoCrafterScreen;
import com.workshopcraft.simplemachines.machine.AutoCrafterMenu;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Copies a JEI crafting recipe into the ghost grid; nothing is moved. */
public class AutoCrafterTransfer implements IRecipeTransferHandler<AutoCrafterMenu, RecipeHolder<CraftingRecipe>> {
    @Override
    public Class<? extends AutoCrafterMenu> getContainerClass() {
        return AutoCrafterMenu.class;
    }

    @Override
    public Optional<MenuType<AutoCrafterMenu>> getMenuType() {
        return Optional.of(SimpleMachines.CRAFTER_MENU);
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(AutoCrafterMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                                         Player player, boolean maxTransfer, boolean doTransfer) {
        final List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
        if (inputs.size() != 9) return null;
        if (!doTransfer) return null;
        for (int i = 0; i < 9; i++) {
            final ItemStack stack = inputs.get(i).getDisplayedIngredient().flatMap(t -> t.getIngredient(VanillaTypes.ITEM_STACK)).orElse(ItemStack.EMPTY);
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, AutoCrafterMenu.setButton(i, stack));
        }
        return null;
    }

    /** Drag an item from the JEI list onto a ghost slot. */
    public static class Ghosts implements IGhostIngredientHandler<AutoCrafterScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(AutoCrafterScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
            final List<Target<I>> targets = new ArrayList<>();
            if (ingredient.getIngredient(VanillaTypes.ITEM_STACK).isEmpty()) return targets;
            for (int i = 0; i < 9; i++) {
                final int pos = i;
                final Rect2i area = new Rect2i(screen.left() + 8 + 18 * (i % 3), screen.top() + 17 + 18 * (i / 3), 16, 16);
                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return area;
                    }

                    @Override
                    public void accept(I value) {
                        if (value instanceof ItemStack stack) screen.press(AutoCrafterMenu.setButton(pos, stack));
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }
}

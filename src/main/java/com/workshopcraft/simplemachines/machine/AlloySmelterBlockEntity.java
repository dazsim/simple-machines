package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.recipe.AlloyingRecipe;
import com.workshopcraft.simplemachines.recipe.CountedIngredient;
import com.workshopcraft.simplemachines.recipe.MachineInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The alloy furnace's recipes on FE, in half the time and with no fuel. */
public class AlloySmelterBlockEntity extends PoweredBlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int PER_TICK = 30;
    public static final int MAX_IN = 300;

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.ALLOY), pos, state, 2, CAPACITY, MAX_IN, PER_TICK);
    }

    @Override
    protected @Nullable Job find(ServerLevel level, List<ItemStack> inputs) {
        final MachineInput in = new MachineInput(List.copyOf(inputs));
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (!(holder.value() instanceof AlloyingRecipe r)) continue;
            final List<CountedIngredient> used = r.matched(in);
            if (used.isEmpty()) continue;
            final int[] counts = new int[used.size()];
            for (int i = 0; i < counts.length; i++) counts[i] = used.get(i).count();
            return new Job(r.result().create(), counts, Math.max(1, r.cookTime() / 2), ItemStack.EMPTY, 0);
        }
        return null;
    }

    @Override
    protected boolean accepts(ServerLevel level, ItemStack stack) {
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof AlloyingRecipe r && r.usesItem(stack)) return true;
        }
        return false;
    }

    /** Keeps pipes from filling both slots with the same thing: the second slot only takes a partner for the first. */
    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        final ItemStack current = items.get(slot);
        if (!current.isEmpty()) return ItemStack.isSameItemSameComponents(current, stack);
        final ItemStack other = items.get(1 - slot);
        if (other.isEmpty()) return accepts(level, stack);
        if (ItemStack.isSameItemSameComponents(other, stack)) return false;
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (!(holder.value() instanceof AlloyingRecipe r)) continue;
            final var a = r.first().ingredient();
            final var b = r.second().ingredient();
            if (a.test(stack) && b.test(other) || b.test(stack) && a.test(other)) return true;
        }
        return false;
    }

    @Override
    public Layout layout() {
        return Layout.ALLOY;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.alloy_smelter");
    }
}

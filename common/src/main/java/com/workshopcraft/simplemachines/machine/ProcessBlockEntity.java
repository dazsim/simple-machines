package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.recipe.ProcessRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Crusher, sawmill and compressor: one input, one output from a {@link ProcessRecipe}; a steel crusher can roll a bonus item. */
public class ProcessBlockEntity extends PoweredBlockEntity {
    public static final int CAPACITY = 10_000;
    public static final int PER_TICK = 20;
    public static final int MAX_IN = 200;
    public static final int COMPRESSOR_CAPACITY = 20_000;
    public static final int COMPRESSOR_PER_TICK = 30;

    private final RecipeType<ProcessRecipe> type;
    private final String name;

    public ProcessBlockEntity(BlockEntityType<?> be, BlockPos pos, BlockState state, RecipeType<ProcessRecipe> type, String name) {
        this(be, pos, state, type, name, CAPACITY, MAX_IN, PER_TICK);
    }

    public ProcessBlockEntity(BlockEntityType<?> be, BlockPos pos, BlockState state, RecipeType<ProcessRecipe> type, String name,
                              int capacity, int maxIn, int perTick) {
        super(be, pos, state, capacity, maxIn, perTick);
        this.type = type;
        this.name = name;
    }

    @Override
    protected @Nullable Job find(ServerLevel level, ItemStack input) {
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof ProcessRecipe r && r.getType() == type && r.ingredient().test(input)) {
                final ItemStack out = r.output();
                if (out.isEmpty()) continue;
                final float chance = tier() == Tier.STEEL ? r.bonus() : 0;
                return new Job(out, new int[]{r.inputCount()}, r.cookTime(), chance > 0 ? out.copyWithCount(1) : ItemStack.EMPTY, chance);
            }
        }
        return null;
    }

    @Override
    protected boolean accepts(ServerLevel level, ItemStack stack) {
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof ProcessRecipe r && r.getType() == type && r.usesItem(stack)) return true;
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines." + name);
    }
}

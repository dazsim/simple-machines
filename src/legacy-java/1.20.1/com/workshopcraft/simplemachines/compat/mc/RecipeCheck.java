package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 1.20.1 recipe lookups returning RecipeHolder stand-ins. */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class RecipeCheck<C extends Container, T extends Recipe<?>> {
    private final RecipeManager.CachedCheck check;

    private RecipeCheck(RecipeType<T> type) {
        this.check = RecipeManager.createCheck((RecipeType) type);
    }

    public static <C extends Container, T extends Recipe<?>> RecipeCheck<C, T> create(RecipeType<T> type) {
        return new RecipeCheck<>(type);
    }

    public Optional<RecipeHolder<T>> getRecipeFor(C input, Level level) {
        return ((Optional<T>) check.getRecipeFor(input, level)).map(RecipeHolder::new);
    }

    public static <C extends Container, T extends Recipe<?>> Optional<RecipeHolder<T>> find(RecipeManager manager, RecipeType<T> type, C input, Level level) {
        return ((Optional<T>) manager.getRecipeFor((RecipeType) type, input, level)).map(RecipeHolder::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Collection<RecipeHolder<?>> all(RecipeManager manager) {
        return (List) manager.getRecipes().stream().map(r -> new RecipeHolder(r)).toList();
    }
}

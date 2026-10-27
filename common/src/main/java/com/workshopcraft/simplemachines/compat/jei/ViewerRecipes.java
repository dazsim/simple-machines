package com.workshopcraft.simplemachines.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Recipes the client got from the server, for recipe viewers. Each loader sets the source. */
public final class ViewerRecipes {
    public static Function<RecipeType<?>, Collection<? extends RecipeHolder<?>>> source = t -> List.of();

    private ViewerRecipes() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Recipe<?>> List<RecipeHolder<T>> get(RecipeType<T> type) {
        final List<RecipeHolder<T>> out = new ArrayList<>();
        for (RecipeHolder<?> h : source.apply(type)) {
            if (h.value().getType() == type) out.add((RecipeHolder<T>) h);
        }
        if (out.isEmpty()) {
            final MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                for (RecipeHolder<?> h : server.getRecipeManager().getRecipes()) {
                    if (h.value().getType() == type) out.add((RecipeHolder<T>) h);
                }
            }
        }
        out.sort(Comparator.comparing(h -> h.id().identifier().toString()));
        return out;
    }
}

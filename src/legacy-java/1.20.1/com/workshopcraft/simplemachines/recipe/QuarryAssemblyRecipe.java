package com.workshopcraft.simplemachines.recipe;

import com.google.gson.JsonObject;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.compat.mc.Comps;
import com.workshopcraft.simplemachines.quarry.QuarryDrill;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** 1.20.1 copy: shaped crafting that carries the electric drill's frame, tip and upgrades into the quarry assembly. */
public class QuarryAssemblyRecipe extends ShapedRecipe {
    public static final RecipeSerializer<QuarryAssemblyRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public QuarryAssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new QuarryAssemblyRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public QuarryAssemblyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new QuarryAssemblyRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, QuarryAssemblyRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    };

    public QuarryAssemblyRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        final ItemStack out = super.assemble(input, registries);
        for (int i = 0; i < input.getContainerSize(); i++) {
            final ItemStack in = input.getItem(i);
            if (SimpleMachines.TOOLS.size() > 0 && in.is(SimpleMachines.TOOLS.get(0))) {
                Comps.set(out, SimpleMachines.QUARRY_DRILL, QuarryDrill.of(in).join());
                break;
            }
        }
        return out;
    }
}

package com.workshopcraft.simplemachines.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.quarry.QuarryDrill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** Shaped crafting that carries the electric drill's frame, tip and upgrades into the quarry assembly. */
public class QuarryAssemblyRecipe extends ShapedRecipe {
    public static final MapCodec<QuarryAssemblyRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
    ).apply(i, QuarryAssemblyRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuarryAssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            QuarryAssemblyRecipe::new);
    public static final RecipeSerializer<QuarryAssemblyRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public QuarryAssemblyRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
                                ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        final ItemStack out = super.assemble(input);
        for (int i = 0; i < input.size(); i++) {
            final ItemStack in = input.getItem(i);
            if (SimpleMachines.TOOLS.size() > 0 && in.is(SimpleMachines.TOOLS.getFirst())) {
                out.set(SimpleMachines.QUARRY_DRILL, QuarryDrill.of(in).join());
                break;
            }
        }
        return out;
    }
}

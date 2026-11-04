package com.workshopcraft.simplemachines.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.compat.mc.Registration;
import com.workshopcraft.simplemachines.quarry.QuarryDrill;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** 1.21.1 copy: shaped crafting that carries the electric drill's frame, tip and upgrades into the quarry assembly. */
public class QuarryAssemblyRecipe extends ShapedRecipe {
    public static final MapCodec<QuarryAssemblyRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(o -> o.result)
    ).apply(i, QuarryAssemblyRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuarryAssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStack.STREAM_CODEC, o -> o.result,
            QuarryAssemblyRecipe::new);
    public static final RecipeSerializer<QuarryAssemblyRecipe> SERIALIZER = Registration.serializer(MAP_CODEC, STREAM_CODEC);

    private final ItemStack result;

    public QuarryAssemblyRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result) {
        super(group, category, pattern, result);
        this.result = result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        final ItemStack out = super.assemble(input, registries);
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

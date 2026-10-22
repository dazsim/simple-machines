package com.workshopcraft.simplemachines.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/** Two inputs in either slot order, one output. */
public record AlloyingRecipe(CountedIngredient first, CountedIngredient second, ItemStackTemplate result, int cookTime) implements MachineRecipe {
    public static final MapCodec<AlloyingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CountedIngredient.CODEC.fieldOf("first").forGetter(AlloyingRecipe::first),
            CountedIngredient.CODEC.fieldOf("second").forGetter(AlloyingRecipe::second),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
            Codec.intRange(1, 72000).optionalFieldOf("cooking_time", 200).forGetter(AlloyingRecipe::cookTime)
    ).apply(i, AlloyingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            CountedIngredient.STREAM_CODEC, AlloyingRecipe::first,
            CountedIngredient.STREAM_CODEC, AlloyingRecipe::second,
            ItemStackTemplate.STREAM_CODEC, AlloyingRecipe::result,
            ByteBufCodecs.VAR_INT, AlloyingRecipe::cookTime,
            AlloyingRecipe::new);

    @Override
    public List<CountedIngredient> matched(MachineInput in) {
        if (first.test(in.getItem(0)) && second.test(in.getItem(1))) return List.of(first, second);
        if (second.test(in.getItem(0)) && first.test(in.getItem(1))) return List.of(second, first);
        return List.of();
    }

    @Override
    public List<CountedIngredient> ingredients() {
        return List.of(first, second);
    }

    @Override
    public boolean matches(MachineInput in, Level level) {
        return in.size() == 2 && !matched(in).isEmpty();
    }

    @Override
    public ItemStack assemble(MachineInput in) {
        return result.create();
    }

    @Override
    public RecipeSerializer<AlloyingRecipe> getSerializer() {
        return SimpleMachines.ALLOYING_SERIALIZER;
    }

    @Override
    public RecipeType<AlloyingRecipe> getType() {
        return SimpleMachines.ALLOYING;
    }
}

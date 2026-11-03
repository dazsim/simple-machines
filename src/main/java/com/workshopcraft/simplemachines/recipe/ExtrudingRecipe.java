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

public record ExtrudingRecipe(CountedIngredient input, ItemStackTemplate result, int cookTime) implements MachineRecipe {
    public static final MapCodec<ExtrudingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CountedIngredient.CODEC.fieldOf("input").forGetter(ExtrudingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ExtrudingRecipe::result),
            Codec.intRange(1, 72000).optionalFieldOf("cooking_time", 400).forGetter(ExtrudingRecipe::cookTime)
    ).apply(i, ExtrudingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtrudingRecipe> STREAM_CODEC = StreamCodec.composite(
            CountedIngredient.STREAM_CODEC, ExtrudingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, ExtrudingRecipe::result,
            ByteBufCodecs.VAR_INT, ExtrudingRecipe::cookTime,
            ExtrudingRecipe::new);

    @Override
    public List<CountedIngredient> matched(MachineInput in) {
        return input.test(in.getItem(0)) ? List.of(input) : List.of();
    }

    @Override
    public List<CountedIngredient> ingredients() {
        return List.of(input);
    }

    @Override
    public boolean matches(MachineInput in, Level level) {
        return in.size() == 1 && input.test(in.getItem(0));
    }

    @Override
    public ItemStack assemble(MachineInput in) {
        return result.create();
    }

    @Override
    public RecipeSerializer<ExtrudingRecipe> getSerializer() {
        return SimpleMachines.EXTRUDING_SERIALIZER;
    }

    @Override
    public RecipeType<ExtrudingRecipe> getType() {
        return SimpleMachines.EXTRUDING;
    }
}

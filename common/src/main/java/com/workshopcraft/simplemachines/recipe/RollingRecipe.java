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

/** Rods by default; recipes marked sheet run when the mill is switched to sheets. */
public record RollingRecipe(CountedIngredient input, ItemStackTemplate result, int cookTime, boolean sheet) implements MachineRecipe {
    public static final MapCodec<RollingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CountedIngredient.CODEC.fieldOf("input").forGetter(RollingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(RollingRecipe::result),
            Codec.intRange(1, 72000).optionalFieldOf("cooking_time", 100).forGetter(RollingRecipe::cookTime),
            Codec.BOOL.optionalFieldOf("sheet", false).forGetter(RollingRecipe::sheet)
    ).apply(i, RollingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RollingRecipe> STREAM_CODEC = StreamCodec.composite(
            CountedIngredient.STREAM_CODEC, RollingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, RollingRecipe::result,
            ByteBufCodecs.VAR_INT, RollingRecipe::cookTime,
            ByteBufCodecs.BOOL, RollingRecipe::sheet,
            RollingRecipe::new);

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
    public RecipeSerializer<RollingRecipe> getSerializer() {
        return SimpleMachines.ROLLING_SERIALIZER;
    }

    @Override
    public RecipeType<RollingRecipe> getType() {
        return SimpleMachines.ROLLING;
    }
}

package com.workshopcraft.simplemachines.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One input, one output by item id or by "#tag" (the first item in the tag, so other mods' dusts are used).
 * A recipe whose tag is empty does nothing. bonus is the chance of one extra on a steel chassis.
 */
public record ProcessRecipe(Kind kind, Ingredient ingredient, String result, int count, float bonus, int cookTime, int inputCount) implements MachineRecipe {
    public enum Kind {
        CRUSHING, SAWING, COMPRESSING
    }

    public static MapCodec<ProcessRecipe> mapCodec(Kind kind) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(ProcessRecipe::ingredient),
                Codec.STRING.fieldOf("result").forGetter(ProcessRecipe::result),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ProcessRecipe::count),
                Codec.floatRange(0, 1).optionalFieldOf("bonus", 0F).forGetter(ProcessRecipe::bonus),
                Codec.intRange(1, 72000).optionalFieldOf("cooking_time", 200).forGetter(ProcessRecipe::cookTime),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(ProcessRecipe::inputCount)
        ).apply(i, (a, b, c, d, e, f) -> new ProcessRecipe(kind, a, b, c, d, e, f)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe> streamCodec(Kind kind) {
        return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ProcessRecipe::ingredient,
                ByteBufCodecs.STRING_UTF8, ProcessRecipe::result,
                ByteBufCodecs.VAR_INT, ProcessRecipe::count,
                ByteBufCodecs.FLOAT, ProcessRecipe::bonus,
                ByteBufCodecs.VAR_INT, ProcessRecipe::cookTime,
                ByteBufCodecs.VAR_INT, ProcessRecipe::inputCount,
                (a, b, c, d, e, f) -> new ProcessRecipe(kind, a, b, c, d, e, f));
    }

    /** The result stack, or empty when the id is unknown or the tag has no items. */
    public ItemStack output() {
        if (result.startsWith("#")) {
            final TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(result.substring(1)));
            for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) return new ItemStack(h, count);
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(result)).map(i -> new ItemStack(i, count)).orElse(ItemStack.EMPTY);
    }

    @Override
    public List<CountedIngredient> matched(MachineInput in) {
        return ingredient.test(in.getItem(0)) ? List.of(new CountedIngredient(ingredient, inputCount)) : List.of();
    }

    @Override
    public List<CountedIngredient> ingredients() {
        return List.of(new CountedIngredient(ingredient, inputCount));
    }

    @Override
    public boolean usesItem(ItemStack stack) {
        return ingredient.test(stack) && !output().isEmpty();
    }

    @Override
    public boolean matches(MachineInput in, Level level) {
        return in.size() == 1 && ingredient.test(in.getItem(0)) && !output().isEmpty();
    }

    @Override
    public ItemStack assemble(MachineInput in) {
        return output();
    }

    @Override
    public RecipeSerializer<ProcessRecipe> getSerializer() {
        return switch (kind) {
            case CRUSHING -> SimpleMachines.CRUSHING_SERIALIZER;
            case SAWING -> SimpleMachines.SAWING_SERIALIZER;
            case COMPRESSING -> SimpleMachines.COMPRESSING_SERIALIZER;
        };
    }

    @Override
    public RecipeType<ProcessRecipe> getType() {
        return switch (kind) {
            case CRUSHING -> SimpleMachines.CRUSHING;
            case SAWING -> SimpleMachines.SAWING;
            case COMPRESSING -> SimpleMachines.COMPRESSING;
        };
    }
}

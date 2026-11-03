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

/** mode is "rods" (default) or "casings". The old sheet flag still reads, as mode "sheets", which no mill runs. */
public record RollingRecipe(CountedIngredient input, ItemStackTemplate result, int cookTime, String mode) implements MachineRecipe {
    public static final String RODS = "rods", CASINGS = "casings", SHEETS = "sheets";
    public static final MapCodec<RollingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CountedIngredient.CODEC.fieldOf("input").forGetter(RollingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(RollingRecipe::result),
            Codec.intRange(1, 72000).optionalFieldOf("cooking_time", 100).forGetter(RollingRecipe::cookTime),
            Codec.STRING.optionalFieldOf("mode", RODS).forGetter(RollingRecipe::mode),
            Codec.BOOL.optionalFieldOf("sheet", false).forGetter(r -> SHEETS.equals(r.mode()))
    ).apply(i, (a, b, c, m, sheet) -> new RollingRecipe(a, b, c, sheet ? SHEETS : m)));
    public static final StreamCodec<RegistryFriendlyByteBuf, RollingRecipe> STREAM_CODEC = StreamCodec.composite(
            CountedIngredient.STREAM_CODEC, RollingRecipe::input,
            ItemStackTemplate.STREAM_CODEC, RollingRecipe::result,
            ByteBufCodecs.VAR_INT, RollingRecipe::cookTime,
            ByteBufCodecs.STRING_UTF8, RollingRecipe::mode,
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

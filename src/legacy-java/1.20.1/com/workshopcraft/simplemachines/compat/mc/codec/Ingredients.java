package com.workshopcraft.simplemachines.compat.mc.codec;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Ingredient;

/** Codecs for 1.20.1's json-only Ingredient. */
public final class Ingredients {
    public static final Codec<Ingredient> CODEC = Codec.PASSTHROUGH.comapFlatMap(d -> {
        try {
            return DataResult.success(Ingredient.fromJson(d.convert(JsonOps.INSTANCE).getValue()));
        } catch (RuntimeException e) {
            return DataResult.error(e::getMessage);
        }
    }, i -> new com.mojang.serialization.Dynamic<JsonElement>(JsonOps.INSTANCE, i.toJson()));
    public static final Codec<Ingredient> CODEC_NONEMPTY = CODEC;
    public static final StreamCodec<Object, Ingredient> STREAM_CODEC = StreamCodec.of(Ingredient::fromNetwork, (b, i) -> i.toNetwork(b));

    private Ingredients() {
    }
}

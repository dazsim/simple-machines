package com.workshopcraft.simplemachines.compat.mc;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.Supplier;

/** Stand-in for the 26.x SavedDataType: codec based saved data kept in the overworld's storage. */
public record SavedDataType<T extends CodecSavedData>(ResourceLocation id, Supplier<T> constructor, Codec<T> codec, DataFixTypes fix) {
    public T get(MinecraftServer server) {
        final SavedData.Factory<T> factory = new SavedData.Factory<>(() -> attach(constructor.get()),
                (tag, registries) -> attach(codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("data"))
                        .result().orElseGet(constructor)), fix);
        return server.overworld().getDataStorage().computeIfAbsent(factory, id.getNamespace() + "_" + id.getPath());
    }

    private T attach(T data) {
        data.codec = codec;
        return data;
    }

    static CompoundTag save(CodecSavedData data, CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        @SuppressWarnings("unchecked") final Codec<CodecSavedData> c = (Codec<CodecSavedData>) data.codec;
        c.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), data).result().ifPresent(t -> tag.put("data", t));
        return tag;
    }
}

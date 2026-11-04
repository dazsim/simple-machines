package com.workshopcraft.simplemachines.compat.mc;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;

import java.util.function.Supplier;

/** Stand-in for the 26.x SavedDataType: codec based saved data kept in the overworld's storage. */
public record SavedDataType<T extends CodecSavedData>(ResourceLocation id, Supplier<T> constructor, Codec<T> codec, DataFixTypes fix) {
    public T get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                tag -> attach(codec.parse(NbtOps.INSTANCE, tag.get("data")).result().orElseGet(constructor)),
                () -> attach(constructor.get()), id.getNamespace() + "_" + id.getPath());
    }

    private T attach(T data) {
        data.codec = codec;
        return data;
    }

    static CompoundTag save(CodecSavedData data, CompoundTag tag) {
        @SuppressWarnings("unchecked") final Codec<CodecSavedData> c = (Codec<CodecSavedData>) data.codec;
        c.encodeStart(NbtOps.INSTANCE, data).result().ifPresent(t -> tag.put("data", t));
        return tag;
    }
}

package com.workshopcraft.simplemachines.compat.mc;

import com.workshopcraft.simplemachines.compat.mc.component.DataComponentType;

import org.jetbrains.annotations.Nullable;

/** Stand-in for the 26.x DataComponentGetter. */
public interface DataComponentGetter {
    @Nullable <T> T get(DataComponentType<T> type);

    default <T> T getOrDefault(DataComponentType<T> type, T def) {
        final T v = get(type);
        return v != null ? v : def;
    }

    default boolean has(DataComponentType<?> type) {
        return get(type) != null;
    }
}

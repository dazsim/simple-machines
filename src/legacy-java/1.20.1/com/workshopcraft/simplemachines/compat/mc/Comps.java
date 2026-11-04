package com.workshopcraft.simplemachines.compat.mc;

import com.workshopcraft.simplemachines.compat.mc.item.BlockItemStateProperties;
import com.workshopcraft.simplemachines.compat.mc.component.DataComponentMap;
import com.workshopcraft.simplemachines.compat.mc.component.DataComponentType;
import com.workshopcraft.simplemachines.compat.mc.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Item component access for 1.20.1, stored under the stack's SimpleMachines compound. */
public final class Comps {
    public static final String KEY = "SimpleMachines";

    private Comps() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(ItemStack stack, DataComponentType<T> type) {
        if (type == DataComponents.DYE) return stack.getItem() instanceof DyeItem d ? (T) d.getDyeColor() : null;
        final CompoundTag tag = stack.getTag();
        if (type == DataComponents.BLOCK_STATE) {
            return tag != null && tag.contains("BlockStateTag", Tag.TAG_COMPOUND) ? (T) new BlockItemStateProperties(tag.getCompound("BlockStateTag")) : null;
        }
        return read(tag == null ? null : tag.getCompound(KEY), type);
    }

    @Nullable
    public static <T> T read(@Nullable CompoundTag data, DataComponentType<T> type) {
        if (data == null || type.codec() == null || !data.contains(type.name())) return null;
        return type.codec().parse(NbtOps.INSTANCE, data.get(type.name())).result().orElse(null);
    }

    public static <T> T getOrDefault(ItemStack stack, DataComponentType<T> type, T def) {
        final T v = get(stack, type);
        return v != null ? v : def;
    }

    public static boolean has(ItemStack stack, DataComponentType<?> type) {
        return get(stack, type) != null;
    }

    @Nullable
    public static <T> T set(ItemStack stack, DataComponentType<T> type, @Nullable T value) {
        final T old = get(stack, type);
        if (value == null) {
            remove(stack, type);
            return old;
        }
        if (type == DataComponents.BLOCK_STATE) {
            stack.getOrCreateTag().put("BlockStateTag", ((BlockItemStateProperties) value).tag().copy());
            return old;
        }
        if (type.codec() == null) return old;
        final CompoundTag data = stack.getOrCreateTagElement(KEY);
        type.codec().encodeStart(NbtOps.INSTANCE, value).result().ifPresent(t -> data.put(type.name(), t));
        return old;
    }

    @Nullable
    public static <T> T remove(ItemStack stack, DataComponentType<T> type) {
        final T old = get(stack, type);
        if (type == DataComponents.BLOCK_STATE) {
            stack.removeTagKey("BlockStateTag");
            return old;
        }
        final CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY, Tag.TAG_COMPOUND)) {
            final CompoundTag data = tag.getCompound(KEY);
            data.remove(type.name());
            if (data.isEmpty()) stack.removeTagKey(KEY);
        }
        return old;
    }

    public static void write(CompoundTag data, DataComponentMap.Builder components) {
        for (Map.Entry<DataComponentType<?>, Object> e : components.values().entrySet()) put(data, e.getKey(), e.getValue());
    }

    @SuppressWarnings("unchecked")
    private static <T> void put(CompoundTag data, DataComponentType<T> type, Object value) {
        if (type.codec() != null && type.name() != null) {
            type.codec().encodeStart(NbtOps.INSTANCE, (T) value).result().ifPresent(t -> data.put(type.name(), t));
        }
    }

    public static DataComponentGetter getter(ItemStack stack) {
        return new DataComponentGetter() {
            public <T> T get(DataComponentType<T> type) {
                return Comps.get(stack, type);
            }
        };
    }

    public static DataComponentGetter getter(CompoundTag data) {
        return new DataComponentGetter() {
            public <T> T get(DataComponentType<T> type) {
                return Comps.read(data, type);
            }
        };
    }
}

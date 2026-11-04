package com.workshopcraft.simplemachines.compat.mc.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.Property;

import org.jetbrains.annotations.Nullable;

/** Stand-in for 1.20.5+ BlockItemStateProperties over the BlockStateTag compound. */
public record BlockItemStateProperties(CompoundTag tag) {
    public static final BlockItemStateProperties EMPTY = new BlockItemStateProperties(new CompoundTag());

    @Nullable
    public <T extends Comparable<T>> T get(Property<T> property) {
        return tag.contains(property.getName()) ? property.getValue(tag.getString(property.getName())).orElse(null) : null;
    }

    public <T extends Comparable<T>> BlockItemStateProperties with(Property<T> property, T value) {
        final CompoundTag t = tag.copy();
        t.putString(property.getName(), property.getName(value));
        return new BlockItemStateProperties(t);
    }

    public boolean isEmpty() {
        return tag.isEmpty();
    }
}

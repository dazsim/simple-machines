package com.workshopcraft.simplemachines.compat.mc;

/** Block entities that take item components when placed. */
public interface ComponentHolder {
    void applyComponentsFrom(DataComponentGetter components);
}

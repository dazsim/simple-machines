package com.workshopcraft.simplemachines.pipe;

import net.minecraft.world.level.material.Fluid;

/** One fluid slot in droplets, as the loaders' fluid handlers see it. */
public interface FluidStore {
    Fluid fluid();

    long amount();

    long capacity();

    long room(Fluid f);

    void set(Fluid f, long a);

    void changed();

    default boolean drainable() {
        return true;
    }
}

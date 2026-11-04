package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;

public final class Buckets {
    private Buckets() {
    }

    public static Fluid fluid(BucketItem b) {
        return b.getFluid();
    }
}
